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
    lateinit var didSelectField: ((field: Field) -> Unit)
    lateinit var didSelectRule: ((rule: Rule) -> Unit)
    lateinit var didSelectFilter: ((filter: Filter) -> Unit)
    lateinit var shouldAddField: (() -> Unit)
    lateinit var shouldAddRule: (() -> Unit)
    lateinit var shouldAddFilter: (() -> Unit)

    fun updateFieldsRulesFilters( fields: List<Field>, rules: List<Rule>, filters: List<Filter> )
    {
        this.fields = fields
        this.rules = rules
        this.filters = filters

        notifyDataSetChanged()
    }

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

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, childView: View?, viewGroup: ViewGroup?): View
    {
        var view: View

        if (childView == null)
        {
            view = LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false )
        }
        else
        {
            view = childView
        }

        val nameTextView = view!!.findViewById<View>(R.id.name_text_view) as TextView

        when( groupPosition )
        {
            0 -> nameTextView.text = fields[childPosition].name
            1 -> nameTextView.text = rules[childPosition].name
            2 -> nameTextView.text = filters[childPosition].name
        }

        view.setOnClickListener {
            when( groupPosition )
            {
                0 -> didSelectField( fields[childPosition] )
                1 -> didSelectRule( rules[childPosition] )
                2 -> didSelectFilter( filters[childPosition] )
            }
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

    override fun getGroupView( groupPosition: Int, isExpanded: Boolean, childView: View?, viewGroup: ViewGroup?): View
    {
        var view: View

        if (childView == null)
        {
            view = LayoutInflater.from(context).inflate(R.layout.list_group, viewGroup, false )
        }
        else
        {
            view = childView!!
        }

        val listTitleTextView = view.findViewById<TextView>(R.id.listGroupTitle)

        when( groupPosition )
        {
            0 -> listTitleTextView.text = "Fields"
            1 -> listTitleTextView.text = "Rules"
            2 -> listTitleTextView.text = "Filters"
        }

        val downImageView = view.findViewById<View>(R.id.arrow_down_image_view) as ImageView
        val upImageView = view.findViewById<View>(R.id.arrow_up_image_view) as ImageView

        if (isExpanded)
        {
            upImageView.visibility = View.VISIBLE
            downImageView.visibility = View.GONE
        }
        else
        {
            upImageView.visibility = View.GONE
            downImageView.visibility = View.VISIBLE
        }

        val addButton = view.findViewById<ImageView>(R.id.add_button)

        addButton.setOnClickListener {
            when( groupPosition )
            {
                0 -> shouldAddField()
                1 -> shouldAddRule()
                2 -> shouldAddFilter()
            }
        }

        return view
    }

    override fun isChildSelectable(p0: Int, p1: Int): Boolean
    {
        return true
    }
}