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
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Rule

class CreateFilterAdapter(var filterRules: List<Rule>?) : RecyclerView.Adapter<CreateFilterAdapter.ViewHolder>()
{
    override fun getItemCount() = filterRules!!.size

    private var context: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var shouldEditFilterRule: ((rule: Rule) -> Unit)
    lateinit var shouldDeleteFilterRule: ((rule: Rule, previousRule : Rule?) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context
        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_filter, parent, false))

        viewHolder.itemView.isSelected = false

        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateRules( rule: Rule? )
    {
        var rulesArray = ArrayList<Rule>()
        rule?.let{rule->
            var looprRule : Rule? = rule

            // we loop til we don't find a rule.
            // maybe do this with recursion
            while(looprRule != null)
            {
                 if(!rulesArray.contains(looprRule))
                 {
                     rulesArray.add(looprRule)
                 }
                 val op = looprRule.filterOperator
                 op?.let{ op ->
                     looprRule = op.rule
                 }?: run{ looprRule = null }


            }
        }

        this.filterRules = rulesArray
        notifyDataSetChanged()
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        var titleTextView: TextView = itemView.findViewById(R.id.title_text_view);
        val deleteButton: ImageView = itemView.findViewById(R.id.image_view);
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val rule = filterRules!!.get(holder.adapterPosition)
        var previousRule : Rule? = null
        if(holder.adapterPosition > 0)
        {
            previousRule = filterRules!!.get(holder.adapterPosition - 1)
        }

        rule?.let { rule ->
            if (position == 0 && rule.filterOperator == null)
            {
                holder.titleTextView.text = rule.name
            }
            else
            {
                // TODO: use resource holder
                rule.filterOperator?.let{operator->
                    holder.titleTextView.text = rule.name + " " + operator.connector.format    //rule!!.connector.format + " " + rule.name
                }?: run{
                    holder.titleTextView.text = rule.name
                }

            }
        }

        holder.deleteButton.setOnClickListener {
            shouldDeleteFilterRule( rule, previousRule )
        }
    }
}