package io.github.karino2.itsunani

import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

class SearchActivity : AppCompatActivity(), CoroutineScope {
    lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onStart() {
        job = Job()
        super.onStart()
        launch {
            val query = async(Dispatchers.IO) {
                queryCursor()
            }
            val curs = query.await()
            Log.d("ItsuNani", "count = ${curs.count}")
            entryAdapter.cursor = curs
            entryAdapter.notifyDataSetChanged()
        }
    }

    val SELECT_FIELDS = arrayOf("_id", "BODY", "DATE")
    val ORDER_SENTENCE = "DATE DESC, _id DESC"

    private fun queryCursor(): Cursor {
        return database.query(DatabaseHolder.ENTRY_TABLE_NAME) {
            select(*SELECT_FIELDS)
            order(ORDER_SENTENCE)
        }
    }

    override fun onStop() {
        super.onStop()
        job.cancel()
    }
    fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        recyclerView.adapter = entryAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupActionMode()

       searchEditText.apply {
           setOnEditorActionListener { v, actionId, event ->
               when(actionId) {
                   EditorInfo.IME_ACTION_SEARCH -> {
                       launch {
                           val newCursor = async(Dispatchers.IO) {
                               database.query(DatabaseHolder.ENTRY_TABLE_NAME) {
                                   select(*SELECT_FIELDS)
                                   val word = v.text.toString()
                                   if(!word.isEmpty())
                                        where("BODY like ?", "%"+v.text.toString()+"%")
                                   order(ORDER_SENTENCE)
                               }
                           }
                           entryAdapter.cursor = newCursor.await()
                           entryAdapter.notifyDataSetChanged()
                       }
                       true
                   }
                   else -> false
               }
           }
       }
    }

    val searchEditText by lazy {
        findViewById<EditText>(R.id.editTextSearch)
    }

    private fun setupActionMode() {
        entryAdapter.actionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                val inflater = mode.menuInflater
                inflater.inflate(R.menu.search_context_menu, menu)
                entryAdapter.isSelecting = true
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.delete_item -> {
                        launch {
                            val newCursor = async(Dispatchers.IO) {
                                database.deleteEntries(entryAdapter.selectedIds)
                                queryCursor()
                            }
                            entryAdapter.cursor = newCursor.await()
                            // To cancel selection, we always call notifyDataSetChanged in onDestroy
                            // entryAdapter.notifyDataSetChanged()
                            entryAdapter.isSelecting = false
                            mode.finish()
                        }

                    }
                }
                return false
            }


            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                entryAdapter.isSelecting = false
                entryAdapter.notifyDataSetChanged()
            }

        }
    }

    val recyclerView by lazy {
        findViewById<RecyclerView>(R.id.recyclerView)
    }

    val entryAdapter = EntryAdapter(null)

    val database by lazy { DatabaseHolder(this) }

    override fun onDestroy() {
        super.onDestroy()
        database.close()
    }


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var longClickListener = {_:View -> false}

        val dateTV = itemView.findViewById<TextView>(R.id.textViewDate)
        val bodyTV = itemView.findViewById<TextView>(R.id.textViewBody)
    }

    class EntryAdapter(var cursor: Cursor?) : RecyclerView.Adapter<ViewHolder>() {
        var actionModeCallback : ActionMode.Callback? = null
        var isSelecting = false

        val selectedIds = arrayListOf<Long>()

        fun toggleSelect(item: View) {
            val id = item.tag as Long
            if(item.isActivated) {
                selectedIds.remove(id)
                item.isActivated = false
            } else {
                selectedIds.add(id)
                item.isActivated = true
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ViewHolder(inflater.inflate(R.layout.search_item, parent, false)).apply {
                itemView.setOnLongClickListener {view->
                    actionModeCallback?.let {
                        (view.context as AppCompatActivity).startSupportActionMode(it)
                        toggleSelect(view)
                        true
                    } ?: false
                }
                itemView.setOnClickListener {
                    if(isSelecting)
                        toggleSelect(it)
                }
            }
        }

        override fun getItemId(position: Int): Long {
            return cursor?.let {
                it.moveToPosition(position)
                // I assume _id is  columnIndex 0. It's defacto.
                return it.getLong(0)
            } ?: 0
        }

        override fun getItemCount(): Int {
            return cursor?.count ?: 0
        }

        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm")

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val curs = cursor
            if(curs == null)
                throw IllegalStateException("onViewViewHolder called when cursor is null. What's situation?")
            curs.moveToPosition(position)
            holder.dateTV.text = sdf.format(Date(curs.getLong(2)))
            holder.bodyTV.text = curs.getString(1)
            holder.itemView.tag = curs.getLong(0)
            holder.itemView.isActivated = false
        }

    }

}
