package edu.gtri.gpssample.fragments.ManageStudies

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.FilterRule

class CreateFilterAdapter(var filterRules: List<FilterRule>?) : RecyclerView.Adapter<CreateFilterAdapter.ViewHolder>()
{
    override fun getItemCount() = filterRules!!.size

    private var context: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var shouldEditFilterRule: ((filterRule: FilterRule) -> Unit)
    lateinit var shouldDeleteFilterRule: ((filterRule: FilterRule) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_filter, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateFilterRules( filterRules: List<FilterRule> )
    {
        this.filterRules = filterRules
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val ruleTextView: TextView = itemView.findViewById(R.id.rule_text_view);
        val connectorTextView: TextView = itemView.findViewById(R.id.connector_text_view);
        val editButton: Button = itemView.findViewById(R.id.edit_button);
        val deleteButton: Button = itemView.findViewById(R.id.delete_button);
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val filterRule = filterRules!!.get(holder.adapterPosition)

        val rule = DAO.ruleDAO.getRule( filterRule.rule_uuid )

        if (rule != null)
        {
            holder.ruleTextView.setText( rule.name )
        }

        if (position == 0)
        {
            holder.connectorTextView.visibility = View.GONE
        }
        else
        {
            holder.connectorTextView.setText( filterRule.connector )
        }

        holder.editButton.setOnClickListener {
            shouldEditFilterRule( filterRule )
        }

        holder.deleteButton.setOnClickListener {
            shouldDeleteFilterRule( filterRule )
        }
    }
}