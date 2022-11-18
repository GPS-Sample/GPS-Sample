package edu.gtri.gpssample

import android.app.Application
import edu.gtri.gpssample.models.FieldModel
import edu.gtri.gpssample.models.StudyModel
import edu.gtri.gpssample.models.ConfigurationModel

class MainApplication : Application()
{
    var fields = mutableListOf<FieldModel>()
    var studies = mutableListOf<StudyModel>()
    var configurations = mutableListOf<ConfigurationModel>()
}
