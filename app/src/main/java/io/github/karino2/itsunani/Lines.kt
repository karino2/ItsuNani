package io.github.karino2.itsunani

import android.content.Context
import android.net.Uri
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.OutputStreamWriter
import java.io.Writer
import kotlin.collections.ArrayList


class Lines(val srcLines: List<String>) {
    constructor(src: String) : this(src.split("\n"))

    // context related IO. Only in companion object.
    companion object {
        const val LAST_URI_KEY = "last_uri_path"

        fun sharedPreferences(ctx: Context) = ctx.getSharedPreferences("ITSU_NANI", Context.MODE_PRIVATE)!!
        fun lastUri(ctx: Context) = sharedPreferences(ctx).getString(LAST_URI_KEY, null)?.let {
            Uri.parse(it)
        }

        fun writeLastUri(ctx:Context, uri : Uri) = sharedPreferences(ctx)
            .edit()
            .putString(LAST_URI_KEY, uri.toString())
            .commit()

        fun loadLines(context: Context): Lines {
            val text = context.contentResolver.openFileDescriptor(lastUri(context), "r")!!.use { desc ->
                val fis = FileInputStream(desc.fileDescriptor)
                fis.bufferedReader().use { it.readText() }
            }
            return Lines(text)
        }

        fun saveLines(context: Context, lines: Lines) {
            context.contentResolver.openOutputStream(lastUri(context), "wt").use {
                BufferedWriter(OutputStreamWriter(it)).use { bw ->
                    lines.save(bw)
                }
            }
        }
    }

    val isItsuNaniLine = srcLines.map { it.startsWith("- ") }

    val itsuNaniLines = ItsuNaniLines(srcLines.filterIndexed {i, _ -> isItsuNaniLine[i] })
    val otherLines = srcLines.filterIndexed {i, _ -> !isItsuNaniLine[i] }

    fun deleteLines( itsuNaniIndices: ArrayList<Int>) = itsuNaniLines.deleteLines(itsuNaniIndices)

    val allLines: Sequence<String>
        get()
        {
            val itsuNaniCopy = LineQueue().apply { addAll(itsuNaniLines.lines) }
            val otherLinesCopy = LineQueue().apply { addAll(otherLines) }
            return sequence {
                srcLines.forEachIndexed { index, s ->
                    if (isItsuNaniLine[index]) {
                        if (!itsuNaniCopy.isEmpty)
                            yield(itsuNaniCopy.poll())
                    } else {
                        yield(otherLinesCopy.poll())
                    }
                }
                while(!itsuNaniCopy.isEmpty)
                    yield(itsuNaniCopy.poll())
            }
        }

    fun save(writer: Writer) {
        writer.write(allLines.toList().joinToString("\n"))
    }
}

class LineQueue {
    val lines = ArrayList<String>()
    fun addAll(c: Collection<String>) = lines.addAll(c)

    val isEmpty : Boolean
    get() = lines.size == 0

    fun poll() : String
    {
        val top = lines[0]
        lines.removeAt(0)
        return top
    }
}

class ItsuNaniLines(originals: List<String>) {
    val lines : ArrayList<String> = ArrayList<String>().apply{ addAll( originals ) }

    // fun createAdapter(context: Context) : ArrayAdapter<String> = ArrayAdapter(context, R.layout.search_item, _textList)

    fun deleteLines( itsuNaniIndices: ArrayList<Int>)
    {
        var delCount = 0
        itsuNaniIndices.forEach { idx ->
            lines.removeAt( idx - delCount )
            delCount++
        }
    }

    // format is as following.
    // - 2021-05-17 14:32 BODY
    val dateTemplate = "- 2021-05-17 14:32"

    fun at(pos: Int) : ItsuNaniLine {
        val line = lines[pos]
        val dt = line.substring(2, dateTemplate.length)
        val body = line.substring(dateTemplate.length+1)

        return ItsuNaniLine(dt, body)
    }

    fun updateEntry(pos: Int, body: String) {
        val oldLine = lines[pos]
        val newLine = oldLine.substring(0, dateTemplate.length+1) + body
        lines.removeAt(pos)
        lines.add(pos, newLine)
    }

    val count: Int
        get() = lines.size

}

data class ItsuNaniLine(val date: String, val body: String)