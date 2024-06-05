package edu.gtri.gpssample.fragments.perform_collection

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.maps.extension.style.expressions.dsl.generated.distance
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.Location
import java.util.*
import kotlin.collections.ArrayList

class PerformCollectionAdapter(var enumerationItems: List<EnumerationItem>, val enumAreaName: String) : RecyclerView.Adapter<PerformCollectionAdapter.ViewHolder>()
{
    override fun getItemCount() = enumerationItems.size

    private var mContext: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectEnumerationItem: ((enumerationItem: EnumerationItem) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        val viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_collection, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateEnumerationItems( enumerationItems: List<EnumerationItem> )
    {
        this.enumerationItems = enumerationItems
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val enumerationItem = enumerationItems.get(holder.adapterPosition)

        holder.secondTextView.setText( enumerationItem.uuid )

        if (enumerationItem.subAddress.isNotEmpty())
        {
            holder.firstTextView.setText( "${enumAreaName} : ${enumerationItem.subAddress}" )
        }

        holder.thirdTextView.setText("")

        if (enumerationItem.distance > 0)
        {
            holder.thirdTextView.setText("Distance: ${String.format( "%.1f", enumerationItem.distance )} ${enumerationItem.distanceUnits}")
        }

        if (enumerationItem.collectionState == CollectionState.Complete)
        {
            holder.checkImageView.visibility = View.VISIBLE
        }
        else
        {
            holder.checkImageView.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            didSelectEnumerationItem( enumerationItem )
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val firstTextView: TextView = itemView.findViewById(R.id.first_text_view);
        val secondTextView: TextView = itemView.findViewById(R.id.second_text_view);
        val thirdTextView: TextView = itemView.findViewById(R.id.third_text_view);
        val checkImageView: ImageView = itemView.findViewById(R.id.check_image_view)
    }
}