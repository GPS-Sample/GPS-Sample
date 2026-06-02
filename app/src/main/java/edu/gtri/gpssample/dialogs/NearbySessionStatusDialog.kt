package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import edu.gtri.gpssample.R

class NearbySessionStatusDialog
{
    constructor()
    {
    }

    private lateinit var view: View
    private lateinit var alertDialog: AlertDialog

    constructor( context: Context?, title: String, completion: (()->Unit) )
    {
        val inflater = LayoutInflater.from(context)

        view = inflater.inflate(R.layout.dialog_nearby_session_status, null)

        val builder = AlertDialog.Builder(context)
        builder.setView(view)

        val titleView = view.findViewById<TextView>( R.id.title_text_view)
        titleView.setText( title )

        alertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

        val doneButton = view.findViewById<Button>(R.id.done_button)

        doneButton.setOnClickListener {
            alertDialog.dismiss()
            completion()
        }
    }

    fun dismiss()
    {
        alertDialog.dismiss()
    }

    fun setStatus( status: String )
    {
        val doneButton = view.findViewById<Button>(R.id.done_button)
        doneButton.visibility = View.VISIBLE

        val statusLinearLayout = view.findViewById<LinearLayout>( R.id.status_linear_layout )
        statusLinearLayout.visibility = View.VISIBLE

        val statusTextView = view.findViewById<TextView>( R.id.status_text_view)
        statusTextView.setText( status )

        val imageView = view.findViewById<ImageView>( R.id.image_view)
        imageView.visibility = View.GONE
    }

    fun showQrCode( bitmap: Bitmap )
    {
        val doneButton = view.findViewById<Button>(R.id.done_button)
        doneButton.visibility = View.VISIBLE

        val statusLinearLayout = view.findViewById<LinearLayout>( R.id.status_linear_layout )
        statusLinearLayout.visibility = View.GONE

        val imageView = view.findViewById<ImageView>( R.id.image_view)
        imageView.visibility = View.VISIBLE
        imageView.setImageBitmap( bitmap )
    }
}