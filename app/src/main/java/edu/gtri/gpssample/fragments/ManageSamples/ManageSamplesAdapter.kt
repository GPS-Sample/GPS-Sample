package edu.gtri.gpssample.fragments.ManageStudies

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.Sample
import edu.gtri.gpssample.database.models.Study

class ManageSamplesAdapter(var samples: List<Sample>?) : RecyclerView.Adapter<ManageSamplesAdapter.ViewHolder>()
{
    override fun getItemCount() = samples!!.size

    private var mContext: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectSample: ((sample: Sample) -> Unit)
    lateinit var shouldDeleteSample: ((sample: Sample) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateSamples( samples: List<Sample> )
    {
        this.samples = samples
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val sample = samples!!.get(holder.adapterPosition)

        holder.nameTextView.setText( sample.name )

        holder.itemView.setOnClickListener {
            didSelectSample(sample)
        }

        holder.deleteImageView.setOnClickListener {
            shouldDeleteSample(sample)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view);
        val deleteImageView: ImageView = itemView.findViewById<ImageView>(R.id.image_view)
    }
}