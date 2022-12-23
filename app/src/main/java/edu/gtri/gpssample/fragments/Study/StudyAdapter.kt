package edu.gtri.gpssample.fragments.Study

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.models.UserModel

class StudyAdapter(var users: List<UserModel>?) : RecyclerView.Adapter<StudyAdapter.ViewHolder>()
{
    override fun getItemCount() = users!!.size

    private var mContext: Context? = null
    private var allHolders = ArrayList<ViewHolder>()
//    lateinit var selectedItemCallback: ((user: UserModel, shouldDismissKeyboard: Boolean) -> Unit)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        this.mContext = parent.context

        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_online_status, parent, false))

        viewHolder.itemView.isSelected = false
        allHolders.add(viewHolder)

        return viewHolder
    }

    fun updateUsers( users: List<UserModel> )
    {
        this.users = users
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
    {
        val user = users!!.get(holder.adapterPosition)

        holder.itemView.isSelected = user.isOnline

        holder.checkBox.setText( user.name )
        holder.checkBox.isChecked = user.isOnline;

//        holder.itemView.setOnClickListener {
//            selectedItemCallback.invoke(user, true)
//        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val checkBox: CheckBox = itemView.findViewById(R.id.check_box);
    }
}