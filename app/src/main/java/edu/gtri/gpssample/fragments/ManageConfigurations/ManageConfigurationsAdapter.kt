package edu.gtri.gpssample.fragments.ManageConfigurations

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.Config

class ManageConfigurationsAdapter(var configurations: List<Config>?) : RecyclerView.Adapter<ManageConfigurationsAdapter.ViewHolder>()
{
    override fun getItemCount() = configurations!!.size

    private var mContext: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var selectedItemCallback: ((configurationModel: Config, shouldDismissKeyboard: Boolean) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateConfigurations( configurations: List<Config> )
    {
        this.configurations = configurations
        notifyDataSetChanged()
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