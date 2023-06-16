package edu.gtri.gpssample.fragments.manage_enumeration_teams

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
import java.util.*
import kotlin.collections.ArrayList

class ManageEnumerationTeamsAdapter(var teams: List<Team>?) : RecyclerView.Adapter<ManageEnumerationTeamsAdapter.ViewHolder>()
{
    override fun getItemCount() = teams!!.size

    private lateinit var context: Context
    private var allHolders = ArrayList<ViewHolder>()
    lateinit var didSelectTeam: ((team: Team) -> Unit)
    lateinit var shouldDeleteTeam: ((team: Team) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.context = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_team, parent, false))

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
        holder.imageView.visibility = View.VISIBLE
        holder.dateTextView.setText( Date(team.creationDate).toString())

        holder.imageView.setOnClickListener {
            shouldDeleteTeam(team)
        }

        holder.itemView.setOnClickListener {
            didSelectTeam(team)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view)
        val dateTextView: TextView = itemView.findViewById(R.id.date_text_view)
        val imageView: ImageView = itemView.findViewById(R.id.check_image_view)
    }
}