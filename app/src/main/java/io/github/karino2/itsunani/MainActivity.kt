package io.github.karino2.itsunani

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    companion object {
        const val PERMISSION_REQUEST_RECORD_AUDIO_ID = 1
    }

    val lastUri : Uri?
    get() = Lines.lastUri(this)

    fun showMessage(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    fun gotoSetup()
    {
        Intent(this, SetupActivity::class.java)
            .also { startActivity(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.let {
            it.setDisplayShowHomeEnabled(true)
            it.setIcon(R.mipmap.ic_launcher)
            it.title = "  " + getString(R.string.app_name)
        }

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

        lastUri ?: return gotoSetup()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_item_list -> {
                Intent(this, ListActivity::class.java)
                    .also { startActivity(it) }
                return true
            }
            R.id.menu_item_preferences -> {
                gotoSetup()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    val handler by lazy { Handler() }

    override fun onStart() {
        super.onStart()
        lastUri ?: return notifyVoiceNotReady()

        handler.postDelayed({ startVoiceInput() }, 200)
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
        val recogn = SpeechRecognizer.createSpeechRecognizer(this)!!
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
                saveInputsAndFinish(inputs[0])
            }
        })
        recogn
    }

    override fun onDestroy() {
        recognizer.destroy()
        super.onDestroy()
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm")

    val now : String
    get()
    {
        return sdf.format(Date())
    }

    fun appendMessageWithWA(body: String) {
        val newline = "- $now $body\n"
        val uri = lastUri ?: return showMessage("No markdown path specified...")
        appendLineWithWA(uri, newline)
    }

    private fun appendLineWithWA(uri: Uri, newline: String) {
        contentResolver.openOutputStream(uri, "wa").use {
            BufferedWriter(OutputStreamWriter(it)).use { bw ->
                bw.write(newline)
            }
        }
    }

    fun appendMessageWithW(body: String) {
        val newline = "- $now $body\n"
        val uri = lastUri ?: return showMessage("No markdown path specified...")
        appendLineWithW(uri, newline)
    }

    private fun appendLineWithW(uri: Uri, newline: String) {
        val org = contentResolver.openFileDescriptor(uri, "r")!!.use { desc ->
            val fis = FileInputStream(desc.fileDescriptor)
            fis.bufferedReader().use { it.readText() }
        }

        contentResolver.openOutputStream(uri, "w").use {
            BufferedWriter(OutputStreamWriter(it)).use { bw ->
                bw.write(org)
                bw.write(newline)
            }
        }
    }

    private fun saveInputsAndFinish(input:String) {
        showToast(input)
        try {
            appendMessageWithWA(input)
            finish()
        } catch(_: FileNotFoundException) {
            // In google drive, mode wa is not supported and return FileNotFoundException.
            // I can't distinguish file is really not found and wa is not supported, so always retry with w mode.
            // (neither wt is supported in GoogleDrive...)
            try {
                appendMessageWithW(input)
                finish()
            } catch(e: FileNotFoundException) {
                showMessage(getString(R.string.msg_file_not_found))
                gotoSetup()
            }
        } catch( _:  SecurityException) {
            // For Google Drive, after device reboot, open uri cause SeurityException.
            // I couldn't find any solution for this, so goto setup in this case too.
            showMessage( "Can't open file...")
            gotoSetup()
        }
    }

    val retryButton by lazy { findViewById<Button>(R.id.buttonRetry)!! }


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
            else ->  super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
