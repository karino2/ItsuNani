package io.github.karino2.itsunani

import android.app.Activity
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
                val inputs = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!
                if(inputs.isEmpty())
                    return
                saveInputsAndFinish(inputs)
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

    private fun saveInputsAndFinish(inputs: ArrayList<String>) {
        launch {
            val executing = async(Dispatchers.IO) {
                dbHolder.insertEntry(inputs[0])
            }
            executing.await()
            finish()
        }
    }


    fun startVoiceInput() {
        recognizer.startListening(RecognizerIntent.getVoiceDetailsIntent(this))
    }
}
