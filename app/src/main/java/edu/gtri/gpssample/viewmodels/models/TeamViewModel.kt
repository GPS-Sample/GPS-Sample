package edu.gtri.gpssample.viewmodels.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.database.models.Team

class TeamViewModel
{
    private var _currentTeam : MutableLiveData<Team>? = null

    var currentTeam : LiveData<Team>? = _currentTeam

    fun setCurrentTeam(team: Team)
    {
        _currentTeam = MutableLiveData(team)
        currentTeam = _currentTeam
    }
}