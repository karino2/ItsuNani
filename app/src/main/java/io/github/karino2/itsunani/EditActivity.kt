package io.github.karino2.itsunani

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import java.text.SimpleDateFormat
import java.util.*

class EditActivity : AppCompatActivity() {

    var entryId = -1L

    val editText by lazy { findViewById<EditText>(R.id.editText) }
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        intent?.let {
            entryId = intent.getLongExtra("ENTRY_ID", -1)
            val (date, body) = dbHolder.getEntry(entryId)
            editText.setText(body)
            supportActionBar!!.title = sdf.format(Date(date))
        }

        editText.setOnEditorActionListener { v, actionId, event ->
            when(actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    dbHolder.updateEntry(entryId, v.text.toString())
                    finish()
                    true
                }
                else -> false
            }
        }

    }

    val dbHolder by lazy { DatabaseHolder(this) }
    override fun onDestroy() {
        dbHolder.close()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("ENTRY_ID", entryId)
        outState.putString("TITLE", supportActionBar!!.title.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        entryId = savedInstanceState.getLong("ENTRY_ID")
        supportActionBar!!.title = savedInstanceState.getString("TITLE")
    }



}
