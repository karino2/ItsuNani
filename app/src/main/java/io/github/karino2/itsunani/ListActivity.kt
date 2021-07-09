package io.github.karino2.itsunani

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

class ListActivity : AppCompatActivity(), CoroutineScope {

    lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onStart() {
        job = Job()
        super.onStart()
        launch {
            val data = withContext(Dispatchers.IO) {
                loadData()
            }
            lines = data
            entryAdapter.swapData(data.itsuNaniLines)
        }
    }

    private fun loadData() = Lines.loadLines(this)

    var lines: Lines? = null

    override fun onStop() {
        super.onStop()
        job.cancel()
    }
    fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        supportActionBar?.let {
           it.setDisplayShowHomeEnabled(true)
           it.setIcon(R.mipmap.ic_launcher)
           it.title = "  " + getString(R.string.app_name)
        }

        recyclerView.adapter = entryAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupActionMode()

    }

    private var _mode: ActionMode?
        get() = entryAdapter._mode
        set(newMode) {
            entryAdapter._mode = newMode
        }

    private fun setupActionMode() {
        entryAdapter.actionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                _mode = mode
                val inflater = mode.menuInflater
                inflater.inflate(R.menu.list_context_menu, menu)
                entryAdapter.isSelecting = true
                entryAdapter.selectedIds.clear()
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.delete_item -> {
                        launch {
                            val selectedIds = entryAdapter.selectedIds
                            if (selectedIds.isNotEmpty()) {
                                withContext(Dispatchers.IO) {
                                    lines?.let { ls->
                                        ls.deleteLines(selectedIds)
                                        Lines.saveLines(this@ListActivity, ls)
                                    }
                                }
                                entryAdapter.swapData(lines?.itsuNaniLines)
                            }
                            entryAdapter.isSelecting = false
                            mode.finish()
                            _mode = null
                        }

                    }
                }
                return false
            }


            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                entryAdapter.isSelecting = true
                entryAdapter.selectedIds.clear()
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                entryAdapter.isSelecting = false
                entryAdapter.selectedIds.clear()
                _mode = null
                entryAdapter.notifyDataSetChanged()
            }

        }
    }

    val recyclerView by lazy {
        findViewById<RecyclerView>(R.id.recyclerView)!!
    }

    val entryAdapter = EntryAdapter(this)


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTV = itemView.findViewById<TextView>(R.id.textViewDate)
        val bodyTV = itemView.findViewById<TextView>(R.id.textViewBody)
    }

    class EntryAdapter(val context: Context) : RecyclerView.Adapter<ViewHolder>() {
        var _mode: ActionMode? = null
        var actionModeCallback : ActionMode.Callback? = null
        var isSelecting = false
        private var lines: ItsuNaniLines? = null

        val selectedIds = arrayListOf<Int>()
        fun swapData(newLines: ItsuNaniLines?) {
            lines = newLines
            newLines?.let {
                notifyDataSetChanged()
            }
        }

        fun toggleSelect(item: View) {
            val id = item.tag as Int
            if(item.isActivated) {
                selectedIds.remove(id)
                item.isActivated = false
                if (selectedIds.isEmpty()) {
                    _mode?.let {
                        it.finish()
                    }
                }
            } else {
                selectedIds.add(id)
                item.isActivated = true
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ViewHolder(inflater.inflate(R.layout.list_item, parent, false)).apply {
                itemView.setOnLongClickListener {view->
                    actionModeCallback?.let {
                        if (!isSelecting) {
                            (view.context as AppCompatActivity).startSupportActionMode(it)
                            toggleSelect(view)
                        }
                        true
                    } ?: false
                }
                itemView.setOnClickListener {
                    if(isSelecting)
                        toggleSelect(it)
                    else
                        editItem(it.tag as Int)
                }
            }
        }

        private fun editItem(id: Int) {
            val intent = Intent(context, EditActivity::class.java)
            intent.putExtra("ENTRY_ID", id)
            context.startActivity(intent)
        }

        override fun getItemId(position: Int): Long {
            return lines?.at(position)?.id?.toLong() ?: 0
        }

        override fun getItemCount(): Int {
            return lines?.count ?: 0
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val ls = lines
                ?: throw IllegalStateException("onViewViewHolder called when cursor is null. What's situation?")

            val line = ls.at(position)
            holder.dateTV.text = line.date
            holder.bodyTV.text = line.body
            holder.itemView.tag = line.id
            holder.itemView.isActivated = false
        }

    }

}
