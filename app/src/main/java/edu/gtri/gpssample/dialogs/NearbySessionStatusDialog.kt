package edu.gtri.gpssample.dialogs

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import edu.gtri.gpssample.R
import edu.gtri.gpssample.managers.NearbySessionState

class NearbySessionStatusDialog
{
    constructor()
    {
    }

    private lateinit var view: View
    private lateinit var doneButtonText: String
    private lateinit var cancelButtonText: String
    private lateinit var alertDialog: AlertDialog

    constructor( context: Context, title: String, completion: (()->Unit) )
    {
        this.doneButtonText = context.resources.getString(R.string.done )
        this.cancelButtonText = context.resources.getString( R.string.cancel )

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

    fun showDoneButton()
    {
        view.findViewById<Button>(R.id.done_button ).text = doneButtonText
    }

    fun showCancelButton()
    {
        view.findViewById<Button>(R.id.done_button ).text = cancelButtonText
    }

    fun setStatus( status: String )
    {
        val doneButton = view.findViewById<Button>(R.id.done_button)
        doneButton.visibility = View.VISIBLE

        val statusLinearLayout = view.findViewById<LinearLayout>( R.id.status_linear_layout )
        statusLinearLayout.visibility = View.VISIBLE

        val statusTextView = view.findViewById<TextView>( R.id.status_text_view)
        statusTextView.setText( status )

        val imageView = view.findViewById<ImageView>( R.id.qr_image_view)
        imageView.visibility = View.GONE
    }

    fun showQrCode( sessionId: String )
    {
        val qrgEncoder = QRGEncoder(sessionId, null, QRGContents.Type.TEXT, 500 )
        qrgEncoder.colorBlack = Color.WHITE;
        qrgEncoder.colorWhite = Color.BLACK;

        val doneButton = view.findViewById<Button>(R.id.done_button)
         doneButton.text = doneButtonText
        doneButton.visibility = View.VISIBLE

        val statusLinearLayout = view.findViewById<LinearLayout>( R.id.status_linear_layout )
        statusLinearLayout.visibility = View.GONE

        val imageView = view.findViewById<ImageView>( R.id.qr_image_view)
        imageView.visibility = View.VISIBLE
        imageView.setImageBitmap( qrgEncoder.bitmap )
    }

    fun updateState( state: NearbySessionState )
    {
        when (state)
        {
            NearbySessionState.Connecting -> {
                setStatus("Connecting...")
                showCancelButton()
            }

            NearbySessionState.Connected -> {
                setStatus("Connected.")
                showCancelButton()
            }

            NearbySessionState.SendingConfig -> {
                setStatus("Sending Config...")
            }

            is NearbySessionState.Message -> {
                setStatus(state.message)
            }

            NearbySessionState.SendingImage -> {
                setStatus("Sending Image...")
            }

            NearbySessionState.ReceivingConfig -> {
                setStatus("Receiving Config...")
            }

            NearbySessionState.ReceivingEnumerationAreas -> {
                setStatus("Receiving EnumerationAreas...")
            }

            NearbySessionState.ReceivingImages -> {
                setStatus("Receiving Images...")
            }

            NearbySessionState.Done -> {
                setStatus("Done.")
            }

            NearbySessionState.Idle -> {
            }

            is NearbySessionState.Error -> {
                setStatus(state.message)
            }

            NearbySessionState.Closed -> {
                setStatus("Closed")
            }

            is NearbySessionState.Advertising -> {
                showQrCode( state.sessionId )
            }
        }
    }
}