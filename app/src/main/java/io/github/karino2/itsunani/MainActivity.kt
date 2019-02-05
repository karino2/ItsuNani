package io.github.karino2.itsunani

import android.app.Activity
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import kotlin.coroutines.CoroutineContext
import android.widget.ImageView
import kotlinx.coroutines.*
import java.util.ArrayList


class MainActivity : AppCompatActivity(), CoroutineScope {

    lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job


    val sensorManager by lazy {
        getSystemService(Activity.SENSOR_SERVICE) as SensorManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<EditText>(R.id.editTextEntry).apply {
            setOnEditorActionListener { v, actionId, event ->
                when(actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                       saveInputsAndFinish(v.text.toString())
                        true
                    }
                    else -> false
                }
            }
        }

        retryButton.setOnClickListener { _ ->
            startVoiceInput()
        }
    }

    val handler by lazy { Handler() }

    override fun onStart() {
        job = Job()
        super.onStart()
        handler.postDelayed({ startVoiceInput() }, 200)
    }

    override fun onStop() {
        super.onStop()
        job.cancel()
    }

    private fun notifyVoiceNotReady() {
        setResourceToVoiceState(R.drawable.voice_not_ready)
        retryButton.isEnabled = true
    }

    private fun notifyVoiceReady() {
        setResourceToVoiceState(R.drawable.voice_ready)
    }

    private fun setResourceToVoiceState(rsid: Int) {
        val iv = findViewById<ImageView>(R.id.imageViewVoiceState)
        if (iv.isAttachedToWindow)
            iv.setImageResource(rsid)
    }

    val recognizer by lazy {
        val recogn = SpeechRecognizer.createSpeechRecognizer(this)
        recogn.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                notifyVoiceReady()
            }

            override fun onRmsChanged(rmsdB: Float) {
            }

            override fun onBufferReceived(buffer: ByteArray?) {
            }

            override fun onPartialResults(partialResults: Bundle?) {
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
            }

            override fun onBeginningOfSpeech() {
            }

            override fun onEndOfSpeech() {
            }

            override fun onError(error: Int) {
                notifyVoiceNotReady()
            }

            override fun onResults(results: Bundle) {
                notifyVoiceNotReady()
                if(saving)
                    return
                val inputs = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!
                if(inputs.isEmpty())
                    return
                saveInputsAndFinish(inputs[0])
            }
        })
        recogn
    }

    val dbHolder by lazy { DatabaseHolder(this) }
    override fun onDestroy() {
        dbHolder.close()
        recognizer.destroy()
        super.onDestroy()
    }

    var saving = false

    private fun saveInputsAndFinish(input:String) {
        saving = true
        launch {
            val executing = async(Dispatchers.IO) {
                dbHolder.insertEntry(input)
            }
            executing.await()
            finish()
        }
    }

    val retryButton by lazy { findViewById<Button>(R.id.buttonRetry) }


    fun startVoiceInput() {
        recognizer.startListening(RecognizerIntent.getVoiceDetailsIntent(this))
        retryButton.isEnabled = false
    }
}
