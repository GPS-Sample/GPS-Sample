package edu.gtri.gpssample.fragments.ManageEnumerationArea

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.Team

class TeamsAdapter(var teams: List<Team>?) : RecyclerView.Adapter<TeamsAdapter.ViewHolder>()
{
    override fun getItemCount() = teams!!.size

    private lateinit var context: Context
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectTeam: ((team: Team) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateTeams( teams: List<Team> )
    {
        this.teams = teams
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        holder.itemView.isSelected = false

        val team = teams!!.get(holder.adapterPosition)

        holder.nameTextView.setText( team.name )
        holder.imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.qr_code_white))

        holder.itemView.setOnClickListener {
            didSelectTeam(team)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view);
        val imageView: ImageView = itemView.findViewById<ImageView>(R.id.image_view)
    }
}