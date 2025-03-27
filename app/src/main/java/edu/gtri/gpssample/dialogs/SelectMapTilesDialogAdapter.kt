package edu.gtri.gpssample.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.fragments.perform_enumeration.PerformEnumerationAdapter.ViewHolder

class SelectMapTilesDialogAdapter(var items: List<String>, var delegate: SelectMapTilesDialogAdapterDelegate) : RecyclerView.Adapter<SelectMapTilesDialogAdapter.ViewHolder>()
{
    interface SelectMapTilesDialogAdapterDelegate
    {
        fun didSelectMapTiles( selection: String )
    }

    override fun getItemCount() = items.size

    var selection = ""

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_text, parent, false))

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        val item = items.get(holder.adapterPosition)

        holder.textView.text = item

        holder.textView.setOnClickListener {
            selection = item
            delegate.didSelectMapTiles( selection )
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val textView: TextView = itemView.findViewById(R.id.text_view)
    }
}