package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.FilterRule
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import java.util.*
import kotlin.collections.ArrayList

class SelectRuleDialog
{
    private lateinit var sharedViewModel : ConfigurationViewModel
    interface SelectRuleDialogDelegate
    {
        fun didDismissSelectRuleDialog()
    }

    constructor()
    {
    }

    constructor(context: Context, vm : ConfigurationViewModel, filter_uuid: String?, filterRule: FilterRule?, delegate: SelectRuleDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        sharedViewModel = vm
        val view = inflater.inflate(R.layout.dialog_select_rule, null)

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Select Rule").setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()


//
//        val rules = DAO.ruleDAO.getRules( study_id )
//
//        val ruleNames = ArrayList<String>()
//
//        for (rule in rules)
//        {
//            ruleNames.add( rule.name )
//        }
//
//        val filterRules = DAO.filterRuleDAO.getFilterRules( study_id, filter_uuid )
//
//        val connectorTextView = view!!.findViewById<TextView>(R.id.connector_text_view)
//        val connectorFrameLayout = view!!.findViewById<FrameLayout>(R.id.connector_frame_layout)
//
//        if (filterRules.isEmpty())
//        {
//            connectorTextView.visibility = View.GONE
//            connectorFrameLayout.visibility = View.GONE
//        }
//
//        val ruleSpinner = view!!.findViewById<Spinner>(R.id.rule_spinner)
//        ruleSpinner.adapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, ruleNames )
//
//        val connectorSpinner = view!!.findViewById<Spinner>(R.id.connector_spinner)
//        ArrayAdapter.createFromResource(context, R.array.connectors, android.R.layout.simple_spinner_item)
//            .also { adapter ->
//                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//                connectorSpinner.adapter = adapter
//            }
//
//        filterRule?.let { filterRule ->
//            for (i in rules.indices)
//            {
//                if (filterRule.rule_uuid == rules[i].uuid)
//                {
//                    ruleSpinner.setSelection( i )
//                    break
//                }
//            }
//
//            when (filterRule.connector)
//            {
//                "AND" -> connectorSpinner.setSelection(0)
//                "OR" -> connectorSpinner.setSelection(1)
//                "NOT" -> connectorSpinner.setSelection(2)
//                "" -> {
//                    connectorTextView.visibility = View.GONE
//                    connectorFrameLayout.visibility = View.GONE
//                }
//            }
//        }
//
//        val cancelButton = view!!.findViewById<Button>(R.id.cancel_button)
//        cancelButton.setOnClickListener {
//            alertDialog.dismiss()
//        }
//
//        val saveButton = view!!.findViewById<Button>(R.id.save_button)
//        saveButton.setOnClickListener {
//
//            val rule = rules[ruleSpinner.selectedItemPosition]
//            val connector = if (connectorTextView.visibility == View.VISIBLE) connectorSpinner.selectedItem.toString() else ""
//
//            if (filterRule != null)
//            {
//                filterRule!!.rule_uuid = rule.uuid
//                filterRule!!.connector = connector
//                DAO.filterRuleDAO.updateFilterRule( filterRule )
//            }
//            else
//            {
//                val filterRule = FilterRule( null, UUID.randomUUID().toString(), study_id, filter_uuid, rule.uuid, connector )
//                DAO.filterRuleDAO.createFilterRule( filterRule )
//            }
//
//            alertDialog.dismiss()
//            delegate.didDismissSelectRuleDialog()
//        }
    }
}