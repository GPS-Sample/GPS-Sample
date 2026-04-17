package edu.gtri.gpssample.fragments.subset_sample

import android.content.Context
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
import edu.gtri.gpssample.database.models.Study
import java.util.ArrayList

class SubsetSampleAdapter(var context: Context) : BaseExpandableListAdapter()
{
    lateinit var didSelectRule: ((rule: Rule) -> Unit)
    lateinit var didSelectFilter: ((filter: Filter) -> Unit)
    lateinit var shouldAddRule: (() -> Unit)
    lateinit var shouldAddFilter: (() -> Unit)

    var rules = ArrayList<Rule>()
    var filters = ArrayList<Filter>()

    fun updateStudy( study: Study)
    {
        rules = study.subsetRules
        filters = study.subsetFilters

        notifyDataSetChanged()
    }

    override fun getGroupCount(): Int
    {
        return 2
    }

    override fun getChildrenCount(p0: Int): Int
    {
        when( p0 )
        {
            0-> return rules.size
            1-> return filters.size
        }

        return 0
    }

    override fun getChild(p0: Int, p1: Int): Any
    {
        when( p0 )
        {
            0-> return rules[p1]
            1-> return filters[p1]
        }

        return Any()
    }

    override fun getChildId(p0: Int, p1: Int): Long
    {
        return p1.toLong()
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, childView: View?, viewGroup: ViewGroup?): View
    {
        val view: View

        if (childView == null)
        {
            view = LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false )
        }
        else
        {
            view = childView
        }

        val nameTextView = view.findViewById<View>(R.id.name_text_view) as TextView
        val dateTextView = view.findViewById<View>(R.id.date_text_view) as TextView
        dateTextView.visibility = View.GONE

        when( groupPosition )
        {
            0 -> nameTextView.text = rules[childPosition].name
            1 -> nameTextView.text = filters[childPosition].name
        }

        view.setOnClickListener {
            when( groupPosition )
            {
                0 -> didSelectRule( rules[childPosition] )
                1 -> didSelectFilter( filters[childPosition] )
            }
        }

        return view
    }

    override fun getGroup(p0: Int): Any
    {
        when( p0 )
        {
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

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, childView: View?, viewGroup: ViewGroup?): View
    {
        val view: View

        if (childView == null)
        {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_group, viewGroup, false )
        }
        else
        {
            view = childView
        }

        val listTitleTextView = view.findViewById<TextView>(R.id.listGroupTitle)

        when( groupPosition )
        {
            0 -> listTitleTextView.text = context.getString(R.string.rules)
            1 -> listTitleTextView.text = context.getString(R.string.filters)
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
                0 -> shouldAddRule()
                1 -> shouldAddFilter()
            }
        }

        return view
    }

    override fun isChildSelectable(p0: Int, p1: Int): Boolean
    {
        return true
    }
}