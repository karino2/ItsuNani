package io.github.karino2.itsunani

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import java.util.*

class EditActivity : AppCompatActivity() {

    var entryId = -1

    val editText by lazy { findViewById<EditText>(R.id.editText)!! }

    val lines by lazy { Lines.loadLines(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        intent?.let {
            entryId = intent.getIntExtra("ENTRY_ID", -1)
            val (_, date, body) = lines.itsuNaniLines.getById(entryId)
            editText.setText(body)
            supportActionBar!!.title = date
        }

        editText.setOnEditorActionListener { v, actionId, event ->
            when(actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    lines.itsuNaniLines.updateEntry(entryId, v.text.toString())
                    Lines.saveLines(this, lines)
                    finish()
                    true
                }
                else -> false
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("ENTRY_ID", entryId)
        outState.putString("TITLE", supportActionBar!!.title.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        entryId = savedInstanceState.getInt("ENTRY_ID")
        supportActionBar!!.title = savedInstanceState.getString("TITLE")
    }



}
