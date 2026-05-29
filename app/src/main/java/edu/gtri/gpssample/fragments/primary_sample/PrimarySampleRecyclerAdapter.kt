package edu.gtri.gpssample.fragments.primary_sample

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.*

class PrimarySampleRecyclerAdapter( private val context: Context ) : RecyclerView.Adapter<RecyclerView.ViewHolder>()
{
    companion object
    {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    lateinit var didSelectField: (Field) -> Unit
    lateinit var didSelectRule: (Rule) -> Unit
    lateinit var didSelectFilter: (Filter) -> Unit

    lateinit var shouldAddField: () -> Unit
    lateinit var shouldAddRule: () -> Unit
    lateinit var shouldAddFilter: () -> Unit

    var fields = arrayListOf<Field>()
    var rules = arrayListOf<Rule>()
    var filters = arrayListOf<Filter>()

    sealed class PrimaryRow
    {
        data class Header(val group: Int, var expanded: Boolean = true) : PrimaryRow()
        data class FieldRow(val field: Field) : PrimaryRow()
        data class RuleRow(val rule: Rule) : PrimaryRow()
        data class FilterRow(val filter: Filter) : PrimaryRow()
    }

    private val rows = arrayListOf<PrimaryRow>()

    private val expandedStates = booleanArrayOf(true, true, true)

    fun updateStudy(study: Study)
    {
        fields.clear()

        for (field in study.fields)
        {
            fields.add(field)
            field.fields?.let { fields.addAll(it) }
        }

        rules = ArrayList(study.primaryRules)
        filters = ArrayList(study.primaryFilters)

        rebuildRows()
    }

    private fun rebuildRows()
    {
        rows.clear()

        for (group in 0..2)
        {
            rows.add(PrimaryRow.Header(group, expandedStates[group]))

            if (!expandedStates[group]) continue

            when (group)
            {
                0 -> fields.forEach { rows.add(PrimaryRow.FieldRow(it)) }
                1 -> rules.forEach { rows.add(PrimaryRow.RuleRow(it)) }
                2 -> filters.forEach { rows.add(PrimaryRow.FilterRow(it)) }
            }
        }

        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int
    {
        return if (rows[position] is PrimaryRow.Header) TYPE_HEADER else TYPE_ITEM
    }

    override fun getItemCount() = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
    {
        return if (viewType == TYPE_HEADER)
        {
            HeaderHolder(LayoutInflater.from(context).inflate(R.layout.list_item_group, parent, false))
        }
        else
        {
            ItemHolder(LayoutInflater.from(context).inflate(R.layout.list_item, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)
    {
        when (val row = rows[position])
        {
            is PrimaryRow.Header -> bindHeader(holder as HeaderHolder, row)
            is PrimaryRow.FieldRow -> bindField(holder as ItemHolder, row.field)
            is PrimaryRow.RuleRow -> bindRule(holder as ItemHolder, row.rule)
            is PrimaryRow.FilterRow -> bindFilter(holder as ItemHolder, row.filter)
        }
    }

    private fun bindHeader(holder: HeaderHolder, header: PrimaryRow.Header)
    {
        holder.title.text = when (header.group)
        {
            0 -> context.getString(R.string.fields)
            1 -> context.getString(R.string.rules)
            else -> context.getString(R.string.filters)
        }

        holder.up.visibility = if (header.expanded) View.VISIBLE else View.GONE
        holder.down.visibility = if (header.expanded) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener {
            expandedStates[header.group] = !expandedStates[header.group]
            rebuildRows()
        }

        holder.addButton.setOnClickListener {
            when (header.group)
            {
                0 -> shouldAddField()
                1 -> shouldAddRule()
                2 -> shouldAddFilter()
            }
        }
    }

    private fun bindField(holder: ItemHolder, field: Field)
    {
        holder.date.visibility = View.GONE

        holder.name.text =
            if (field.parentUUID == null)
            {
                "${field.index}. ${field.name}"
            }
            else
            {
                val parent = fields.firstOrNull { it.uuid == field.parentUUID }
                "    ${parent?.index ?: 0}.${field.index}. ${field.name}"
            }

        holder.itemView.setOnClickListener { didSelectField(field) }
    }

    private fun bindRule(holder: ItemHolder, rule: Rule)
    {
        holder.date.visibility = View.GONE
        holder.name.text = rule.name
        holder.itemView.setOnClickListener { didSelectRule(rule) }
    }

    private fun bindFilter(holder: ItemHolder, filter: Filter)
    {
        holder.date.visibility = View.GONE
        holder.name.text = filter.name
        holder.itemView.setOnClickListener { didSelectFilter(filter) }
    }

    fun moveField(from: Int, to: Int)
    {
        val fromField = rows[from] as? PrimaryRow.FieldRow ?: return
        val toField = rows[to] as? PrimaryRow.FieldRow ?: return

        val fromIndex = fields.indexOf(fromField.field)
        val toIndex = fields.indexOf(toField.field)

        java.util.Collections.swap(fields, fromIndex, toIndex)

        var index = 1
        var groupUuid = ""
        var primaryIndex = 0

        fields.forEachIndexed { i, field ->
            if (field.parentUUID != null)
            {
                if (groupUuid.isEmpty())
                {
                    index = 1
                    groupUuid = field.parentUUID!!
                }
                else
                {
                    index = index + 1
                }
                field.index = index
            }
            else
            {
                groupUuid = ""
                primaryIndex += 1
                field.index = primaryIndex
            }
        }

        rebuildRows()
    }

    fun isFieldRow(position: Int): Boolean
    {
        return rows.getOrNull(position) is PrimaryRow.FieldRow
    }

    class HeaderHolder(view: View) : RecyclerView.ViewHolder(view)
    {
        val title = view.findViewById<TextView>(R.id.listGroupTitle)
        val up = view.findViewById<ImageView>(R.id.arrow_up_image_view)
        val down = view.findViewById<ImageView>(R.id.arrow_down_image_view)
        val addButton = view.findViewById<ImageView>(R.id.add_button)
    }

    class ItemHolder(view: View) : RecyclerView.ViewHolder(view)
    {
        val name = view.findViewById<TextView>(R.id.name_text_view)
        val date = view.findViewById<TextView>(R.id.date_text_view)
    }
}