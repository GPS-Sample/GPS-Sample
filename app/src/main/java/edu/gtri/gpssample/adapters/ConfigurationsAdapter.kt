package edu.gtri.gpssample.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.models.ConfigurationModel

class ConfigurationsAdapter(var configurations: List<ConfigurationModel>?) : RecyclerView.Adapter<ConfigurationsAdapter.ViewHolder>()
{
    override fun getItemCount() = configurations!!.size

    private var mContext: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var selectedItemCallback: ((configurationModel: ConfigurationModel, shouldDismissKeyboard: Boolean) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_configuration, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val configurationModel = configurations!!.get(holder.adapterPosition)

        holder.nameTextView.setText( configurationModel.name )

        holder.itemView.setOnClickListener {
            selectedItemCallback.invoke(configurationModel, true)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view);
    }
}