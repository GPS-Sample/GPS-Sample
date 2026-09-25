package edu.gtri.gpssample.fragments.strata_sample

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.*
import java.util.ArrayList

class StrataSampleAdapter2(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>()
{
    companion object
    {
        private const val TYPE_TITLE = 0
        private const val TYPE_HEADER = 1
        private const val TYPE_ITEM = 2
        private const val GROUP_STRATAS = 0
        private const val GROUP_FIELDS = 1
        private const val GROUP_RULES = 2
        private const val GROUP_FILTERS = 3
        private const val GROUP_COLLECTION_FIELDS = 4
    }

    lateinit var didSelectStrata: (Strata) -> Unit
    lateinit var didSelectField: (Field) -> Unit
    lateinit var didSelectRule: (Rule) -> Unit
    lateinit var didSelectFilter: (Filter) -> Unit
    lateinit var didSelectCollectionField: (Field) -> Unit
    lateinit var shouldAddStrata: () -> Unit
    lateinit var shouldAddField: () -> Unit
    lateinit var shouldAddRule: () -> Unit
    lateinit var shouldAddFilter: () -> Unit
    lateinit var shouldAddCollectionField: () -> Unit

    var stratas = ArrayList<Strata>()
    var fields = arrayListOf<Field>()
    var rules = arrayListOf<Rule>()
    var filters = arrayListOf<Filter>()
    var collectionFields = arrayListOf<Field>()

    sealed class PrimaryRow
    {
        data class Title(
            val text: String,
            val topSpacing: Int = 0
        ) : PrimaryRow()

        data class Header(
            val group: Int,
            var expanded: Boolean = true
        ) : PrimaryRow()

        data class StrataRow(
            val strata: Strata
        ) : PrimaryRow()

        data class FieldRow(
            val field: Field
        ) : PrimaryRow()

        data class RuleRow(
            val rule: Rule
        ) : PrimaryRow()

        data class FilterRow(
            val filter: Filter
        ) : PrimaryRow()

        data class CollectionFieldRow(
            val field: Field
        ) : PrimaryRow()
    }

    private val rows = arrayListOf<PrimaryRow>()

    private val expandedStates = booleanArrayOf(true, true, true, true, true)

    fun updateStudy(study: Study)
    {
        fields.clear()
        collectionFields.clear()

        for (field in study.fields)
        {
            fields.add(field)
            field.fields?.let {
                fields.addAll(it)
            }
        }

        for (field in study.collectionFields)
        {
            collectionFields.add(field)
            field.fields?.let {
                collectionFields.addAll(it)
            }
        }

        stratas = study.stratas
        rules = ArrayList(study.rules)
        filters = ArrayList(study.filters)

        rebuildRows()
    }

    private fun rebuildRows()
    {
        rows.clear()

        // ---------------------------------------------------------
        // Enumeration Fields
        // ---------------------------------------------------------

        rows.add(PrimaryRow.Title(text = context.getString(
            R.string.enumeration_fields
        )))

        // Stratas
        rows.add(PrimaryRow.Header(group = GROUP_STRATAS, expanded = expandedStates[GROUP_STRATAS]))

        if (expandedStates[GROUP_STRATAS])
        {
            stratas.forEach {
                rows.add(PrimaryRow.StrataRow(it))
            }
        }

        // Fields
        rows.add(PrimaryRow.Header(group = GROUP_FIELDS, expanded = expandedStates[GROUP_FIELDS]))

        if (expandedStates[GROUP_FIELDS])
        {
            fields.forEach {
                rows.add(PrimaryRow.FieldRow(it))
            }
        }

        // Rules
        rows.add(PrimaryRow.Header(group = GROUP_RULES, expanded = expandedStates[GROUP_RULES]))

        if (expandedStates[GROUP_RULES])
        {
            rules.forEach {
                rows.add(PrimaryRow.RuleRow(it))
            }
        }

        // Filters
        rows.add(PrimaryRow.Header(group = GROUP_FILTERS, expanded = expandedStates[GROUP_FILTERS]))

        if (expandedStates[GROUP_FILTERS])
        {
            filters.forEach {
                rows.add(PrimaryRow.FilterRow(it))
            }
        }

        // ---------------------------------------------------------
        // Collection Fields
        // ---------------------------------------------------------

        rows.add(PrimaryRow.Title(text = context.getString(
            R.string.collection_fields
        ), topSpacing = 40))

        // Fields
        rows.add(PrimaryRow.Header(group = GROUP_COLLECTION_FIELDS, expanded = expandedStates[GROUP_COLLECTION_FIELDS]))

        if (expandedStates[GROUP_COLLECTION_FIELDS])
        {
            collectionFields.forEach {
                rows.add(PrimaryRow.CollectionFieldRow(it))
            }
        }

        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int
    {
        return when (rows[position])
        {
            is PrimaryRow.Title -> TYPE_TITLE
            is PrimaryRow.Header -> TYPE_HEADER
            else -> TYPE_ITEM
        }
    }

    override fun getItemCount(): Int
    {
        return rows.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
    {
        return when (viewType)
        {
            TYPE_TITLE ->
            {
                TitleHolder(LayoutInflater.from(context).inflate(R.layout.list_item_section_title, parent, false))
            }

            TYPE_HEADER ->
            {
                HeaderHolder(LayoutInflater.from(context).inflate(R.layout.list_item_group, parent, false))
            }

            else ->
            {
                ItemHolder(LayoutInflater.from(context).inflate(R.layout.list_item, parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)
    {
        when (val row = rows[position])
        {
            is PrimaryRow.Title -> bindTitle(holder as TitleHolder, row)
            is PrimaryRow.Header -> bindHeader(holder as HeaderHolder, row)
            is PrimaryRow.StrataRow -> bindStrata(holder as ItemHolder, row.strata)
            is PrimaryRow.FieldRow -> bindField(holder as ItemHolder, row.field)
            is PrimaryRow.RuleRow -> bindRule(holder as ItemHolder, row.rule)
            is PrimaryRow.FilterRow -> bindFilter(holder as ItemHolder, row.filter)
            is PrimaryRow.CollectionFieldRow -> bindCollectionField(holder as ItemHolder, row.field)
        }
    }

    private fun bindTitle(holder: TitleHolder, title: PrimaryRow.Title)
    {
        holder.title.text = title.text

        val layoutParams = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams

        layoutParams.topMargin = dpToPx(title.topSpacing)

        holder.itemView.layoutParams = layoutParams
    }

    private fun bindHeader(holder: HeaderHolder, header: PrimaryRow.Header)
    {
        holder.title.text = when (header.group)
        {
            GROUP_STRATAS -> context.getString(R.string.strata)
            GROUP_FIELDS -> context.getString(R.string.fields)
            GROUP_RULES -> context.getString(R.string.rules)
            GROUP_FILTERS -> context.getString(R.string.filters)
            GROUP_COLLECTION_FIELDS -> context.getString(R.string.fields)
            else -> ""
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
                GROUP_STRATAS -> shouldAddStrata()
                GROUP_FIELDS -> shouldAddField()
                GROUP_RULES -> shouldAddRule()
                GROUP_FILTERS -> shouldAddFilter()
                GROUP_COLLECTION_FIELDS -> shouldAddCollectionField()
            }
        }
    }

    private fun bindStrata(holder: ItemHolder, strata: Strata)
    {
        holder.date.visibility = View.GONE
        holder.name.text = strata.name

        holder.itemView.setOnClickListener {
            didSelectStrata(strata)
        }
    }

    private fun bindField(holder: ItemHolder, field: Field)
    {
        holder.date.visibility = View.GONE

        if (field.parentUUID == null)
        {
            holder.name.text = "${field.index}. ${field.name}"
        }
        else
        {
            val parentField = fields.firstOrNull { it.uuid == field.parentUUID }

            holder.name.text = "    ${parentField?.index ?: 0}.${field.index}. ${field.name}"
        }

        holder.itemView.setOnClickListener {
            didSelectField(field)
        }
    }

    private fun bindRule(holder: ItemHolder, rule: Rule)
    {
        holder.date.visibility = View.GONE
        holder.name.text = rule.name

        holder.itemView.setOnClickListener {
            didSelectRule(rule)
        }
    }

    private fun bindFilter(holder: ItemHolder, filter: Filter)
    {
        holder.date.visibility = View.GONE
        holder.name.text = filter.name

        holder.itemView.setOnClickListener {
            didSelectFilter(filter)
        }
    }

    private fun bindCollectionField(holder: ItemHolder, field: Field)
    {
        holder.date.visibility = View.GONE

        if (field.parentUUID == null)
        {
            holder.name.text = "${field.index}. ${field.name}"
        }
        else
        {
            val parentField = collectionFields.firstOrNull { it.uuid == field.parentUUID }

            holder.name.text = "    ${parentField?.index ?: 0}.${field.index}. ${field.name}"
        }

        holder.itemView.setOnClickListener {
            didSelectCollectionField(field)
        }
    }

    fun moveField(from: Int, to: Int)
    {
        val fromField = rows[from] as? PrimaryRow.FieldRow ?: return

        val toField = rows[to] as? PrimaryRow.FieldRow ?: return

        val fromIndex = fields.indexOf(fromField.field)

        val toIndex = fields.indexOf(toField.field)

        java.util.Collections.swap(
            fields,
            fromIndex,
            toIndex
        )

        var index = 1
        var groupUuid = ""
        var primaryIndex = 0

        fields.forEach { field ->
            if (field.parentUUID != null)
            {
                if (groupUuid.isEmpty())
                {
                    index = 1
                    groupUuid = field.parentUUID!!
                }
                else
                {
                    index++
                }

                field.index = index
            }
            else
            {
                groupUuid = ""
                primaryIndex++
                field.index = primaryIndex
            }
        }

        rebuildRows()
    }

    fun moveCollectionField(from: Int, to: Int)
    {
        val fromField = rows[from] as? PrimaryRow.CollectionFieldRow ?: return

        val toField = rows[to] as? PrimaryRow.CollectionFieldRow ?: return

        val fromIndex = collectionFields.indexOf(fromField.field)

        val toIndex = collectionFields.indexOf(toField.field)

        java.util.Collections.swap(
            collectionFields,
            fromIndex,
            toIndex
        )

        var index = 1
        var groupUuid = ""
        var primaryIndex = 0

        collectionFields.forEach { field ->
            if (field.parentUUID != null)
            {
                if (groupUuid.isEmpty())
                {
                    index = 1
                    groupUuid = field.parentUUID!!
                }
                else
                {
                    index++
                }

                field.index = index
            }
            else
            {
                groupUuid = ""
                primaryIndex++
                field.index = primaryIndex
            }
        }

        rebuildRows()
    }

    fun isFieldRow(position: Int): Boolean
    {
        return rows.getOrNull(position) is PrimaryRow.FieldRow
    }

    fun isCollectionFieldRow(position: Int): Boolean
    {
        return rows.getOrNull(position) is PrimaryRow.CollectionFieldRow
    }

    private fun dpToPx(dp: Int): Int
    {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    class TitleHolder(view: View) : RecyclerView.ViewHolder(view)
    {
        val title = view.findViewById<TextView>(R.id.sectionTitle)
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