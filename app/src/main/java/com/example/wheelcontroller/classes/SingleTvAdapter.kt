package com.example.wheelcontroller.classes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.wheelcontroller.R

class SingleTvAdapter(list: Array<String>): RecyclerView.Adapter<SingleTvAdapter.ViewHolder>() {

    private val list:Array<String>
    init {
        this.list = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view:View = LayoutInflater.from(parent.context).inflate(R.layout.single_tv_layout,parent,false);
        return ViewHolder(view);
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvText.text = list[position]
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvText:TextView;

        init {
            tvText = itemView.findViewById(R.id.tvText);
        }
    }


}