package edu.gtri.gpssample.fragments.ManageStudy

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.models.NetworkUser

class ManageStudyAdapter(var networkUsers: List<NetworkUser>?) : RecyclerView.Adapter<ManageStudyAdapter.ViewHolder>()
{
    override fun getItemCount() = networkUsers!!.size

    private var mContext: Context? = null
    private var allHolders = ArrayList<ViewHolder>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_online_status, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateUsers( networkUsers: List<NetworkUser> )
    {
        this.networkUsers = networkUsers
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        val networkUser = networkUsers!!.get(holder.adapterPosition)

        holder.itemView.isSelected = networkUser.isOnline

        holder.checkBox.setText( networkUser.name )
        holder.checkBox.isChecked = networkUser.isOnline;

//        holder.itemView.setOnClickListener {
//            selectedItemCallback.invoke(user, true)
//        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val checkBox: CheckBox = itemView.findViewById(R.id.check_box);
    }
}