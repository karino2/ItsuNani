package io.github.karino2.itsunani

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.net.Uri
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
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), CoroutineScope {
    companion object {
        const val PERMISSION_REQUEST_RECORD_AUDIO_ID = 1
        const val REQUEST_OPEN_FILE_ID = 2

        const val LAST_URI_KEY = "last_uri_path"
    }

    val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("ITSU_NANI", MODE_PRIVATE)
    }

    val lastUri : Uri?
    get()
    {
        sharedPreferences.getString(LAST_URI_KEY, null)?.let {
            return Uri.parse(it)
        }
        return null
    }

    fun writeLastUri(uri : Uri) = sharedPreferences
        .edit()
        .putString(LAST_URI_KEY, uri.toString())
        .commit()

    fun resetLastUri() = sharedPreferences
            .edit()
            .putString(LAST_URI_KEY, null)
            .commit()

    lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job


    val sensorManager by lazy {
        getSystemService(Activity.SENSOR_SERVICE) as SensorManager
    }

    fun showMessage(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    fun requestOpen() {
        showMessage("Choose markdown file to store")
        try {
            Intent(Intent.ACTION_OPEN_DOCUMENT)
                .apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    // text/markdown seems not known mime-type.
                    type = "*/*"
                }
                .also{
                startActivityForResult(it, REQUEST_OPEN_FILE_ID)
            }
        } catch(e: ActivityNotFoundException) {
            showMessage("No activity for open document!")
        }
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

        lastUri ?: return requestOpen()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            REQUEST_OPEN_FILE_ID-> {
                if (resultCode == RESULT_OK) {
                    data?.data?.also {
                        contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        writeLastUri(it)

                    }
                }
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    val handler by lazy { Handler() }

    override fun onStart() {
        job = Job()
        super.onStart()
        lastUri ?: return notifyVoiceNotReady()

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

    fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

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

    override fun onDestroy() {
        recognizer.destroy()
        super.onDestroy()
    }

    var saving = false

    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm")

    val now : String
    get()
    {
        return sdf.format(Date())
    }

    fun saveMessage(body: String) {
        val newline = "- $now $body\n"
        val uri = lastUri ?: return showMessage("No markdown path specified...")
        contentResolver.openOutputStream(uri, "wa").use {
            BufferedWriter(OutputStreamWriter(it)).use { bw->
                bw.write(newline)
            }
        }
    }

    private fun saveInputsAndFinish(input:String) {
        showToast(input)
        saving = true
        launch {
            withContext(Dispatchers.IO) {
                saveMessage(input)
            }
            finish()
        }
    }

    val retryButton by lazy { findViewById<Button>(R.id.buttonRetry) }


    fun startVoiceInput() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        {
            recognizer.startListening(RecognizerIntent.getVoiceDetailsIntent(this))
            retryButton.isEnabled = false
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO),PERMISSION_REQUEST_RECORD_AUDIO_ID)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            PERMISSION_REQUEST_RECORD_AUDIO_ID -> {
                if(grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startVoiceInput()
                }
            }
        }
    }
}
