package edu.gtri.gpssample.fragments.CreateSample

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.NavPlan

class CreateSampleAdapter(var navPlans: List<NavPlan>?) : RecyclerView.Adapter<CreateSampleAdapter.ViewHolder>()
{
    override fun getItemCount() = navPlans!!.size

    private var context: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectNavPlan: ((navPlan: NavPlan) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_filter, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateNavPlans( navPlans: List<NavPlan> )
    {
        this.navPlans = navPlans
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val titleTextView: TextView = itemView.findViewById(R.id.title_text_view)
        val editButton: ImageView = itemView.findViewById(R.id.edit_image_view)
        val deleteButton: ImageView = itemView.findViewById(R.id.image_view)
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false
        holder.editButton.visibility = View.GONE
        holder.deleteButton.visibility = View.GONE

        val navPlan = navPlans!!.get(holder.adapterPosition)

        holder.titleTextView.setText( navPlan.name )

        holder.itemView.setOnClickListener {
            didSelectNavPlan(navPlan)
        }
    }
}
