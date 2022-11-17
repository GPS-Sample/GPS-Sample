package edu.gtri.gpssample

import android.app.Application
import edu.gtri.gpssample.models.ConfigurationModel

class MainApplication : Application()
{
    var configurations = mutableListOf<ConfigurationModel>()
}
