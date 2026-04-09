package com.example.zamgavocafront.pvp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.zamgavocafront.R
import com.example.zamgavocafront.model.WordData

class PvpWordAdapter(
    private var words: List<WordData>,
    private val onClick: (WordData) -> Unit
) : RecyclerView.Adapter<PvpWordAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvWord: TextView = view.findViewById(R.id.tv_word)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_pvp_word, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val word = words[position]
        holder.tvWord.text = word.word
        holder.itemView.setOnClickListener { onClick(word) }
    }

    override fun getItemCount() = words.size

    fun updateWords(newWords: List<WordData>) {
        words = newWords
        notifyDataSetChanged()
    }
}
