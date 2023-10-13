package edu.gtri.gpssample.viewmodels.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.database.models.CollectionTeam
import edu.gtri.gpssample.database.models.EnumerationTeam

class TeamViewModel
{
    private var _currentEnumerationTeam : MutableLiveData<EnumerationTeam>? = null
    private var _currentCollectionTeam : MutableLiveData<CollectionTeam>? = null

    var currentEnumerationTeam : LiveData<EnumerationTeam>? = _currentEnumerationTeam
    var currentCollectionTeam : LiveData<CollectionTeam>? = _currentCollectionTeam

    fun setCurrentEnumerationTeam(enumerationTeam: EnumerationTeam)
    {
        _currentEnumerationTeam = MutableLiveData(enumerationTeam)
        currentEnumerationTeam = _currentEnumerationTeam
    }

    fun setCurrentCollectionTeam(collectionTeam: CollectionTeam)
    {
        _currentCollectionTeam = MutableLiveData(collectionTeam)
        currentCollectionTeam = _currentCollectionTeam
    }
}