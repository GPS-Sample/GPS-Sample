package edu.gtri.gpssample.fragments.CreateStudy

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageView
import android.widget.TextView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.Rule

class CreateStudyAdapter(var context: Context, var fields: List<Field>, var rules: List<Rule>, var filters: List<Filter>) : BaseExpandableListAdapter()
{
    lateinit var selectedItemCallback: ((fieldModel: Field, shouldDismissKeyboard: Boolean) -> Unit)

//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
//    {
//        this.mContext = parent.context
//
//        var viewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false))
//
//        viewHolder.itemView.isSelected = false
//        allHolders.add(viewHolder)
//
//        return viewHolder
//    }

    fun updateFields( fields: List<Field> )
    {
        this.fields = fields
        notifyDataSetChanged()
    }

    fun updateRules( rules: List<Rule> )
    {
        this.rules = rules
        notifyDataSetChanged()
    }

    fun updateFilters( filters: List<Filter> )
    {
        this.filters = filters
        notifyDataSetChanged()
    }

//    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int)
//    {
//        holder.itemView.isSelected = false
//
//        val field = fields!![holder.adapterPosition]
//
//        holder.nameTextView.text = field.name
//
//        holder.itemView.setOnClickListener {
//            selectedItemCallback.invoke(field, true)
//        }
//    }

//    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
//    {
//        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view);
//    }

    override fun getGroupCount(): Int
    {
        return 3
    }

    override fun getChildrenCount(p0: Int): Int
    {
        when( p0 )
        {
            0 -> return fields.size
            1 -> return rules.size
            2 -> return filters.size
        }

        return 0
    }

    override fun getChild(p0: Int, p1: Int): Any
    {
        when( p0 )
        {
            0-> return fields[p1]
            1-> return rules[p1]
            2-> return filters[p1]
        }

        return Any()
    }

    override fun getChildId(p0: Int, p1: Int): Long
    {
        return p1.toLong()
    }

    override fun getChildView(p0: Int, p1: Int, p2: Boolean, p3: View?, p4: ViewGroup?): View
    {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item, p4, false )

        val nameTextView = view.findViewById<View>(R.id.name_text_view) as TextView

        when( p0 )
        {
            0 -> nameTextView.text = fields[p1].name
            1 -> nameTextView.text = rules[p1].name
            2 -> nameTextView.text = filters[p1].name
        }

        return view
    }

    override fun getGroup(p0: Int): Any
    {
        when( p0 )
        {
            0 -> return fields
            1 -> return rules
            2 -> return filters
        }

        return Any()
    }

    override fun getGroupId(p0: Int): Long
    {
        return p0.toLong()
    }

    override fun hasStableIds(): Boolean
    {
        return true
    }

    override fun getGroupView(p0: Int, p1: Boolean, p2: View?, p3: ViewGroup?): View
    {
        var view: View

        if (p2 == null)
        {
            view = LayoutInflater.from(context).inflate(R.layout.list_group, p3, false )
        }
        else
        {
            view = p2!!
        }

        val addButton = view.findViewById<ImageView>(R.id.add_button)

        addButton.setOnClickListener {
            Log.d( "xxx", "addButton Pressed" )
        }

        val listTitleTextView = view.findViewById<TextView>(R.id.listGroupTitle)

        when( p0 )
        {
            0 -> listTitleTextView.text = "Fields"
            1 -> listTitleTextView.text = "Rules"
            2 -> listTitleTextView.text = "Filters"
        }
        val downImageView = view.findViewById<View>(R.id.arrow_down_image_view) as ImageView
        val upImageView = view.findViewById<View>(R.id.arrow_up_image_view) as ImageView

        if (p1)
        {
            upImageView.visibility = View.VISIBLE
            downImageView.visibility = View.GONE
        }
        else
        {
            upImageView.visibility = View.GONE
            downImageView.visibility = View.VISIBLE
        }

        return view
    }

    override fun isChildSelectable(p0: Int, p1: Int): Boolean
    {
        return true
    }
}