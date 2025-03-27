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

class SelectMapTilesDialog : SelectMapTilesDialogAdapter.SelectMapTilesDialogAdapterDelegate
{
    interface SelectMapTilesDialogDelegate
    {
        fun selectMapTilesDialogDidSelectSaveButton( selection: String )
    }

    constructor()
    {
    }

    lateinit var alertDialog: AlertDialog
    lateinit var delegate: SelectMapTilesDialogDelegate

    constructor( context: Context?, items: List<String>, delegate: SelectMapTilesDialogDelegate )
    {
        this.delegate = delegate

        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.dialog_select_map_tiles, null)

        val selectMapTilesDialogAdapter = SelectMapTilesDialogAdapter( items, this )

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)

        recyclerView.adapter = selectMapTilesDialogAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.layoutManager = LinearLayoutManager(context)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        alertDialog = builder.create()

        alertDialog.setCancelable(true)
        alertDialog.show()

        val cancelButton = view.findViewById<Button>(R.id.cancel_button)

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }
    }

    override fun didSelectMapTiles( selection: String )
    {
        delegate.selectMapTilesDialogDidSelectSaveButton( selection )
        alertDialog.dismiss()
    }
}