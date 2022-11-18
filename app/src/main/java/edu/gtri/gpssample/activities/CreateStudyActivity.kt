package edu.gtri.gpssample.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import edu.gtri.gpssample.MainApplication
import edu.gtri.gpssample.R
import edu.gtri.gpssample.adapters.FieldsAdapter
import edu.gtri.gpssample.adapters.StudiesAdapter
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.constants.ResultCode
import edu.gtri.gpssample.databinding.ActivityCreateStudyBinding
import edu.gtri.gpssample.models.FieldModel
import edu.gtri.gpssample.models.StudyModel

class CreateStudyActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityCreateStudyBinding
    private lateinit var fieldsAdapter: FieldsAdapter

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fieldsAdapter = FieldsAdapter((application as MainApplication).fields)
        fieldsAdapter.selectedItemCallback = this::onItemSelected

        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = fieldsAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this )

        binding.nextButton.setOnClickListener {

            if (binding.studyNameEditText.text.toString().length > 0)
            {
                val intent = Intent(this, GenerateBarcodeActivity::class.java)
                intent.putExtra( Key.StudyName.toString(), binding.studyNameEditText.text.toString())
                startActivityForResult( intent, 0 )
                overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
            }
        }

        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
        }
    }

    override fun onResume()
    {
        super.onResume()

        fieldsAdapter.updateFields((application as MainApplication).fields)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == ResultCode.GenerateBarcode.value)
        {
            finish()
            this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
        }
    }

    fun onItemSelected(fieldModel: FieldModel, shouldDismissKeyboard: Boolean )
    {
//        val intent = Intent(this, ManageStudiesActivity::class.java)
//        startActivity( intent )
//        overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_create_field, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_create_field -> {
                val intent = Intent(this, CreateFieldActivity::class.java)
                startActivity( intent )
                overridePendingTransition(R.animator.slide_from_right, R.animator.slide_to_left)
            }
        }

        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()

        this.overridePendingTransition(R.animator.slide_from_left, R.animator.slide_to_right)
    }
}