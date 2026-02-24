/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.fragments.configuration

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
import edu.gtri.gpssample.database.models.Strata
import edu.gtri.gpssample.database.models.Study
import java.util.ArrayList

class CreateStrataAdapter(var context: Context) : BaseExpandableListAdapter()
{
    lateinit var didSelectStrata: ((strata: Strata) -> Unit)
    lateinit var didSelectField: ((field: Field) -> Unit)
    lateinit var didSelectRule: ((rule: Rule) -> Unit)
    lateinit var didSelectFilter: ((filter: Filter) -> Unit)

    lateinit var shouldAddStrata: (() -> Unit)
    lateinit var shouldAddField: (() -> Unit)
    lateinit var shouldAddRule: (() -> Unit)
    lateinit var shouldAddFilter: (() -> Unit)

    private var stratas = ArrayList<Strata>()
    private var fields = ArrayList<Field>()
    private var rules = ArrayList<Rule>()
    private var filters = ArrayList<Filter>()

    fun updateStudy( study: Study )
    {
        stratas = study.stratas
        fields = ArrayList<Field>()
        rules = study.rules
        filters = study.filters

        for (field in study.fields)
        {
            fields.add( field )
            field.fields?.let { childFields ->
                fields.addAll( childFields )
            }
        }

        notifyDataSetChanged()
    }

    override fun getGroupCount(): Int
    {
        return 4
    }

    override fun getChildTypeCount(): Int {
        return 2   // special first child + normal children
    }

    override fun getChildType(groupPosition: Int, childPosition: Int): Int {
        return if (groupPosition == 0) 0 else 1
    }

    override fun getChildrenCount(p0: Int): Int
    {
        when( p0 )
        {
            0-> return stratas.size
            1-> return fields.size
            2-> return rules.size
            3-> return filters.size
        }

        return 0
    }

    override fun getChild(p0: Int, p1: Int): Any
    {
        when( p0 )
        {
            0-> return stratas[p1]
            1-> return fields[p1]
            2-> return rules[p1]
            3-> return filters[p1]
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
        val type = getChildType(groupPosition, childPosition)

        if (childView == null)
        {
            val layout = if (type == 0)
                R.layout.list_item_strata
            else
                R.layout.list_item

            view = LayoutInflater.from(context).inflate( layout, viewGroup, false )
        }
        else
        {
            view = childView
        }

        if (type == 0)
        {
            val strata = stratas[childPosition]

            val strataNameTextView = view.findViewById<View>(R.id.strata_name_text_view) as TextView
            val sampleSizeTextView = view.findViewById<View>(R.id.sample_size_text_view) as TextView
            val sampleTypeTextView = view.findViewById<View>(R.id.sample_type_text_view) as TextView

            strataNameTextView.text = strata.name
            sampleSizeTextView.text = strata.sampleSize.toString()
            sampleTypeTextView.text = strata.sampleType.format
        }
        else
        {
            val nameTextView = view.findViewById<View>(R.id.name_text_view) as TextView
            val dateTextView = view.findViewById<View>(R.id.date_text_view) as TextView
            dateTextView.visibility = View.GONE

            when( groupPosition )
            {
                1 -> nameTextView.text = "${fields[childPosition].index}. ${fields[childPosition].name}"
                2 -> nameTextView.text = rules[childPosition].name
                3 -> nameTextView.text = filters[childPosition].name
            }

            if (groupPosition == 1)
            {
                val field = fields[childPosition]

                if (field.parentUUID != null)
                {
                    var parentIndex = 0

                    for (f in fields)
                    {
                        if (f.uuid == field.parentUUID)
                        {
                            parentIndex = f.index
                        }
                    }

                    nameTextView.text = "    ${parentIndex}.${fields[childPosition].index}. ${fields[childPosition].name}"
                }
            }
        }

        view.setOnClickListener {
            when( groupPosition )
            {
                0 -> didSelectStrata( stratas[childPosition] )
                1 -> didSelectField( fields[childPosition] )
                2 -> didSelectRule( rules[childPosition] )
                3 -> didSelectFilter( filters[childPosition] )
            }
        }

        return view
    }

    override fun getGroup(p0: Int): Any
    {
        when( p0 )
        {
            0 -> return stratas
            1 -> return fields
            2 -> return rules
            3 -> return filters
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
            0 -> listTitleTextView.text = context.getString(R.string.stratas)
            1 -> listTitleTextView.text = context.getString(R.string.fields)
            2 -> listTitleTextView.text = context.getString(R.string.rules)
            3 -> listTitleTextView.text = context.getString(R.string.filters)
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
                0 -> shouldAddStrata()
                1 -> shouldAddField()
                2 -> shouldAddRule()
                3 -> shouldAddFilter()
            }
        }

        return view
    }

    override fun isChildSelectable(p0: Int, p1: Int): Boolean
    {
        return true
    }
}