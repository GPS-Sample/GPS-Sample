package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.fragments.add_multi_household.AddMultiHouseholdAdapter
import io.github.dellisd.spatialk.geojson.Feature

class CheckboxDialog
{
    interface CheckboxDialogDelegate
    {
        fun checkboxDialogDidSelectCancelButton()
        fun checkboxDialogDidSelectSaveButton( text: String, selections: ArrayList<String> )
    }

    constructor()
    {
    }

    constructor( context: Context?, title: String?, items: List<String>, text: String, feature: Feature, delegate: CheckboxDialogDelegate )
    {
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_checkbox, null)

        val checkboxDialogAdapter = CheckboxDialogAdapter( items )

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)

        recyclerView.adapter = checkboxDialogAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.layoutManager = LinearLayoutManager(context)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(title).setView(view)

        val alertDialog = builder.create()

        alertDialog.setCancelable(true)
        alertDialog.show()

        val firstButton = view.findViewById<Button>(R.id.left_button)
        val secondButton = view.findViewById<Button>(R.id.right_button)

        firstButton.setOnClickListener {
            alertDialog.dismiss()
        }

        secondButton.setOnClickListener {
            delegate.checkboxDialogDidSelectSaveButton( text, checkboxDialogAdapter.selections )
            alertDialog.dismiss()
        }
    }
}