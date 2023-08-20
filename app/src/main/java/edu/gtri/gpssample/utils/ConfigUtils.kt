package edu.gtri.gpssample.utils

import android.content.res.Resources
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.User

data class ConfigValicationResult(
    var success: Boolean,
    var error: String)

object ConfigUtils
{
    fun validateConfig(resources: Resources, user: User, config: Config) : ConfigValicationResult
    {
        if (user.role == Role.Enumerator.toString())
        {
            if (config.teamId < 0)
            {
                return ConfigValicationResult(false, resources.getString(R.string.missing_team))
            }
        }

        return ConfigValicationResult(true,"")
    }
}