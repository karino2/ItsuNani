package io.github.karino2.itsunani

import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
                database.query(DatabaseHolder.ENTRY_TABLE_NAME) {
                    select("_id", "BODY", "DATE")
                    order("DATE DESC, _id DESC")
                }
            }
            val curs = query.await()
            Log.d("ItsuNani", "count = ${curs.count}")
            entryAdapter.cursor = curs
            entryAdapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        job.cancel()
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        recyclerView.adapter = entryAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
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

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val dateTV = view.findViewById<TextView>(R.id.textViewDate)
        val bodyTV = view.findViewById<TextView>(R.id.textViewBody)
    }

    class EntryAdapter(var cursor: Cursor?) : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ViewHolder(inflater.inflate(R.layout.search_item, parent, false))
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
        }

    }

}
