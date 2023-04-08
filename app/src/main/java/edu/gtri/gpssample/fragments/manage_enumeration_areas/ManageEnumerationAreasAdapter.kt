package edu.gtri.gpssample.fragments.manage_enumeration_areas

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.EnumArea

class ManageEnumerationAreasAdapter(var enumAreas: List<EnumArea>?) : RecyclerView.Adapter<ManageEnumerationAreasAdapter.ViewHolder>()
{
    override fun getItemCount() : Int {
        enumAreas?.let {enumAreas ->
            return enumAreas.count()
        }
        return 0
    }

    private lateinit var context: Context
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectEnumArea: ((enumArea: EnumArea) -> Unit)

    fun updateEnumAreas( areas: List<EnumArea>? )
    {

        this.enumAreas = areas
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val enumArea = enumAreas!!.get(holder.adapterPosition)

        holder.nameTextView.setText( enumArea.name )
        holder.imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.qr_code_white))

        holder.itemView.setOnClickListener {
            didSelectEnumArea(enumArea)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view);
        val imageView: ImageView = itemView.findViewById<ImageView>(R.id.image_view)
    }
}