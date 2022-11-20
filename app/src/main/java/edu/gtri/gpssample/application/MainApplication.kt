package edu.gtri.gpssample.application

import android.app.Application
import android.graphics.Bitmap
import edu.gtri.gpssample.models.FieldModel
import edu.gtri.gpssample.models.StudyModel
import edu.gtri.gpssample.models.ConfigurationModel

class MainApplication : Application()
{
    var barcodeBitmap : Bitmap? = null

    var fields = mutableListOf<FieldModel>()
    var studies = mutableListOf<StudyModel>()
    var configurations = mutableListOf<ConfigurationModel>()
}
