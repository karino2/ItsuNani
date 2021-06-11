package io.github.karino2.itsunani

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import io.github.karino2.itsunani.Lines
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SetupActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_NEW_FILE=1
        const val REQUEST_OPEN_FILE=2
    }

    fun showMessage(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    fun writeLastUri(uri: Uri) = Lines.writeLastUri(this, uri)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        findViewById<Button>(R.id.buttonNew).setOnClickListener { _ ->
            try {
                Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/*"
                    }
                    .also{
                        startActivityForResult(it, REQUEST_NEW_FILE)
                    }
            } catch(e: ActivityNotFoundException) {
                showMessage("No activity for create document!")
            }
        }
        findViewById<Button>(R.id.buttonOpen).setOnClickListener { _ ->
            try {
                Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        // with text/*, I can't choose .md ...
                        type = "*/*"
                    }
                    .also{
                        startActivityForResult(it, REQUEST_OPEN_FILE)
                    }
            } catch(e: ActivityNotFoundException) {
                showMessage("No activity for open document!")
            }

        }
    }


    fun createEmptyContent(uri: Uri) {
        contentResolver.openFileDescriptor(uri, "w")!!.use {desc->
            val fos = FileOutputStream(desc.fileDescriptor)
            fos.use {
                it.write("# ItsuNani\n\n".toByteArray())
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        when(requestCode) {
            REQUEST_NEW_FILE -> {
                data?.data?.also {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    createEmptyContent(it)
                    writeLastUri(it)
                    setResult(RESULT_OK)
                    finish()
                    return
                }

            }
            REQUEST_OPEN_FILE -> {
                data?.data?.also {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    writeLastUri(it)
                    setResult(RESULT_OK)
                    finish()
                    return
                }

            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}