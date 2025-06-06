/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.viewmodels

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.fragments.createsample.CreateSampleFragment
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.utils.DateUtils
import java.lang.Integer.min
import java.util.*
import kotlin.math.roundToInt

class SamplingViewModel : ViewModel()
{
    private lateinit var mapboxManager: MapboxManager
    private lateinit var pointAnnotationManager: PointAnnotationManager

    private var _currentStudy : MutableLiveData<Study>? = null
    private var _currentConfig : MutableLiveData<Config>? = null
    private var _currentEnumArea : MutableLiveData<EnumArea>? = null
    private var _currentSampledItemsForSampling : ArrayList<EnumerationItem> = ArrayList()

    private var allPointAnnotations = java.util.ArrayList<PointAnnotation>()

    var currentFragment : Fragment? = null

    var currentConfig : LiveData<Config>?
        get(){
            return _currentConfig
        }
        set(value){
            _currentConfig = MutableLiveData(value?.value)
        }

    var currentStudy : LiveData<Study>?
        get(){
          return _currentStudy
        }
        set(value){
            _currentStudy = MutableLiveData(value?.value)
        }

    var currentEnumArea : LiveData<EnumArea>?
        get(){
            return _currentEnumArea
        }
        set(value){
            _currentEnumArea = MutableLiveData(value?.value)
        }

    val samplingMethod: String
        get()
        {
            currentStudy?.value?.let{study ->
                currentFragment?.let{fragment ->
                    return SamplingMethodConverter.internationalString(study.samplingMethod, fragment)
                }

                return study.samplingMethod.format
            }
            return ""
        }

    fun setSampleAreasForMap(mapboxManager: MapboxManager, pointAnnotationManager: PointAnnotationManager) : SamplingState
    {
        this.mapboxManager = mapboxManager
        this.pointAnnotationManager = pointAnnotationManager

        for (pointAnnotation in allPointAnnotations)
        {
            pointAnnotationManager.delete( pointAnnotation )
        }

        allPointAnnotations.clear()

        currentConfig?.value?.let { config ->
            for (enumArea in config.enumAreas)
            {
                if (enumArea.uuid != config.selectedEnumAreaUuid)
                {
                    continue
                }

                for (location in enumArea.locations)
                {
                    if (!location.isLandmark && location.enumerationItems.isNotEmpty())
                    {
                        var resourceId: Int
                        if (location.enumerationItems.size == 1)
                        {
                            val sampledItem = location.enumerationItems[0]

                            if (sampledItem.enumerationState == EnumerationState.Enumerated)
                            {
                                resourceId = R.drawable.home_green

                                if (sampledItem.samplingState == SamplingState.Sampled)
                                {
                                    resourceId = R.drawable.home_light_blue
                                }

                                val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                                val pointAnnotation = mapboxManager.addMarker( point, resourceId )
                                pointAnnotation?.let { pointAnnotation ->
                                    allPointAnnotations.add( pointAnnotation )
                                }
                            }
                        }
                        else
                        {
                            resourceId = R.drawable.multi_home_green

                            for (sampledItem in location.enumerationItems)
                            {
                                if (sampledItem.samplingState == SamplingState.Sampled)
                                {
                                    resourceId = R.drawable.multi_home_light_blue
                                }
                            }

                            val point = com.mapbox.geojson.Point.fromLngLat(location.longitude, location.latitude )
                            val pointAnnotation = mapboxManager.addMarker( point, resourceId )
                            pointAnnotation?.let { pointAnnotation ->
                                allPointAnnotations.add( pointAnnotation )
                            }
                        }
                    }
                }
            }
        }

        return SamplingState.None
    }

    fun validateRule(rule : Rule, fieldData : FieldData) : Boolean
    {
        var validRule = false

        DAO.fieldDAO.getField( fieldData.fieldUuid )?.let { field ->
            when (field.type) {
                FieldType.Checkbox ->
                {
                    validRule = true

                    rule.operator.let { operator ->
                        when (operator)
                        {
                            Operator.Contains ->
                            {
                                for (ruleOption in rule.fieldDataOptions)
                                {
                                    for (fieldOption in fieldData.fieldDataOptions)
                                    {
                                        if (ruleOption.value && ruleOption.name == fieldOption.name)
                                        {
                                            if (validRule)
                                            {
                                                validRule = fieldOption.value
                                            }
                                            break
                                        }
                                    }
                                }
                            }

                            Operator.Equal ->
                            {
                                var actualRuleName = ""
                                var expectedRuleName = ""

                                // find the single ruleOption that was checked
                                for (ruleOption in rule.fieldDataOptions)
                                {
                                    if (ruleOption.value)
                                    {
                                        expectedRuleName = ruleOption.name
                                        break
                                    }
                                }

                                // find the field options that were checked
                                // if more than one, then set not valid
                                for (fieldOption in fieldData.fieldDataOptions)
                                {
                                    if (fieldOption.value)
                                    {
                                        if (actualRuleName.isEmpty())
                                        {
                                            actualRuleName = fieldOption.name
                                        }
                                        else
                                        {
                                            validRule = false
                                        }
                                    }
                                }

                                // if we found a single rule that was checked
                                // verify that it's the expected rule
                                if (validRule && actualRuleName != expectedRuleName)
                                {
                                    validRule = false
                                }
                            }

                            Operator.NotEqual ->
                            {
                                var actualRuleName = ""
                                var expectedRuleName = ""

                                // find the single ruleOption that was checked
                                for (ruleOption in rule.fieldDataOptions)
                                {
                                    if (ruleOption.value)
                                    {
                                        expectedRuleName = ruleOption.name
                                        break
                                    }
                                }

                                // find the field option that matches the expected
                                // verify that the field option is Not checked
                                for (fieldOption in fieldData.fieldDataOptions)
                                {
                                    if (fieldOption.name == expectedRuleName)
                                    {
                                        if (fieldOption.value)
                                        {
                                            validRule = false
                                        }
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                }

                FieldType.Number, FieldType.Date -> {
                    // we allow for all of the rule operators
                    // convert string to int
                    try {
                        var ruleNumber = rule.value.toDouble()

                        if (field.type == FieldType.Date)
                        {
                            // unix time is stored as a string in the rule.value property
                            val ruleUnixTime = ruleNumber.toLong()

                            // default to use both date & time fields of the Date for comparison
                            ruleNumber = ruleUnixTime.toDouble()

                            val ruleDate = Date( ruleUnixTime )

                            // if date not checked, clear the date so that comparison is based only on time
                            if (!field.date)
                            {
                                ruleNumber = DateUtils.clearDate(( ruleDate )).time.toDouble()
                            }

                            // if time not checked, clear the time so that comparison is based only on date
                            if (!field.time)
                            {
                                ruleNumber = DateUtils.clearTime(( ruleDate )).time.toDouble()
                            }

                            fieldData.dateValue?.let { fieldUnixTime ->
                                // this is sort of a hack, the fieldUnixTime is saved in the fieldData.numberValue
                                // so that the code below this block works as is.  should probably make a fun out of the
                                // code block below and pass the number value
                                fieldData.numberValue = fieldUnixTime.toDouble()

                                val fieldDate = Date( fieldUnixTime )

                                if (!field.date)
                                {
                                    fieldData.numberValue = DateUtils.clearDate(( fieldDate )).time.toDouble()
                                }
                                if (!field.time)
                                {
                                    fieldData.numberValue = DateUtils.clearTime(( fieldDate )).time.toDouble()
                                }
                            }
                        }

                        // this is the code block mentioned above
                        fieldData.numberValue?.let { number ->
                            rule.operator?.let { operator ->
                                when (operator) {
                                    Operator.Equal -> {
                                        val epsilon = 0.000001
                                        validRule = Math.abs(number - ruleNumber) < epsilon
                                    }
                                    Operator.GreaterThan -> {
                                        validRule = (number > ruleNumber)
                                    }
                                    Operator.GreaterThanOrEqual -> {
                                        validRule = (number >= ruleNumber)
                                    }
                                    Operator.LessThanOrEqual -> {
                                        validRule = (number <= ruleNumber)
                                    }
                                    Operator.LessThan -> {
                                        validRule = (number < ruleNumber)
                                    }
                                    Operator.NotEqual -> {
                                        validRule = (number != ruleNumber)
                                    }
                                    else->{
                                        validRule = false
                                    }
                                }
                            }
                        }
                    }catch (ex : Exception){
                        Log.d("XXXXXX", ex.toString())
                    }
                }

                FieldType.Text ->
                {
                    rule.operator?.let { operator ->
                        if (operator == Operator.Equal)
                        {
                            validRule = fieldData.textValue.equals(rule.value) ?: false
                        }
                        else if (operator == Operator.NotEqual)
                        {
                            validRule = !(fieldData.textValue.equals(rule.value) ?: false)
                        }
                        else if (operator == Operator.Contains)
                        {
                            validRule = rule.value.contains( fieldData.textValue)
                        }
                    }
                }

                FieldType.Dropdown ->
                {
                    rule.operator?.let { operator ->
                        if (operator == Operator.Equal)
                        {
                            validRule = fieldData.textValue.equals(rule.value) ?: false
                        }
                        else if (operator == Operator.NotEqual)
                        {
                            validRule = !(fieldData.textValue.equals(rule.value) ?: false)
                        }
                    }
                }

                else -> {
                    validRule = fieldData.textValue?.equals(rule.value) ?: false
                }
            }
        }

        return validRule
    }
    fun beginSampling(view : View)
    {
        currentStudy?.value?.let { study ->
            when( study.samplingMethod )
            {
                SamplingMethod.SimpleRandom ->
                {
                    _currentSampledItemsForSampling.clear()

                    currentConfig?.value?.let { config ->
                        for (enumArea in config.enumAreas)
                        {
                            for (location in enumArea.locations)
                            {
                                if (!location.isLandmark && location.enumerationItems.isNotEmpty())
                                {
                                    for (enumerationItem in location.enumerationItems)
                                    {
                                        if (!_currentSampledItemsForSampling.contains(enumerationItem))
                                        {
                                            _currentSampledItemsForSampling.add(enumerationItem)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    performSimpleRandomSampling()
                }

                SamplingMethod.Cluster ->
                {
                    // TODO: init currentSampledItemsForSampling
                    performClusterSampling()
                }
                else -> {}
            }

            setSampleAreasForMap(mapboxManager,pointAnnotationManager)
        }
    }

    fun performSimpleRandomSampling()
    {
        val validSamples : ArrayList<EnumerationItem> = ArrayList()

        currentStudy?.value?.let { study ->
            for (sampleItem in _currentSampledItemsForSampling)
            {
                sampleItem.samplingState = SamplingState.NotSampled
                sampleItem.enumerationEligibleForSampling = false
                sampleItem.syncCode = sampleItem.syncCode + 1

                // find and remove items that are not valid
                if (sampleItem.enumerationState == EnumerationState.Enumerated)
                {
                    if (study.filters.isEmpty())
                    {
                        sampleItem.enumerationEligibleForSampling = true
                        validSamples.add(sampleItem)
                        continue
                    }

                    var validSample = true

                    for (filter in study.filters)
                    {
                        var validRule = false

                        filter.rule?.let { rule ->
                            DAO.fieldDAO.getField( rule.fieldUuid )?.let { field ->
                                for (fieldData in sampleItem.fieldDataList) {
                                    DAO.fieldDAO.getField( fieldData.fieldUuid )?.let { f ->
                                        if (field.name.equals( f.name )) {
                                            validRule = validateRule( rule, fieldData )
                                        }
                                    }
                                    if (validRule)
                                    {
                                        break
                                    }
                                }
                            }

                            var filterOperator = rule.filterOperator

                            while (filterOperator != null)
                            {
                                filterOperator.rule?.let { nextRule ->
                                    DAO.fieldDAO.getField( nextRule.fieldUuid )?.let { nextField ->
                                        for (fieldData in sampleItem.fieldDataList)
                                        {
                                            DAO.fieldDAO.getField( fieldData.fieldUuid )?.let { f ->
                                                if (nextField.name.equals( f.name ))
                                                {
                                                    val nextRuleValid = validateRule( nextRule, fieldData )

                                                    when (filterOperator!!.connector)
                                                    {
                                                        Connector.AND-> {
                                                            validRule = (validRule && nextRuleValid)
                                                        }
                                                        Connector.OR-> {
                                                            validRule = (validRule || nextRuleValid)
                                                        }
                                                        Connector.NOT-> {
                                                            validRule = (validRule && !nextRuleValid)
                                                        }
                                                        else-> {
                                                            validRule = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    filterOperator = nextRule.filterOperator
                                }
                            }
                        }

                        if (!validRule)
                        {
                            validSample = false
                            break;
                        }
                    }

                    if (validSample)
                    {
                        sampleItem.enumerationEligibleForSampling = true
                        validSamples.add(sampleItem)
                    }
                }
            }

            currentStudy?.value?.let { study ->
                var sampleSize = 0

                when (study.sampleType)
                {
                    SampleType.NumberHouseholds ->
                    {
                        sampleSize =  min(study.sampleSize,validSamples.size)
                    }
                    SampleType.PercentHouseholds ->
                    {
                        sampleSize = (study.sampleSize.toDouble() / 100.0 * validSamples.size.toDouble()).roundToInt()
                    }
                    SampleType.PercentTotal ->
                    {
                        sampleSize = (study.sampleSize.toDouble() / 100.0 * totalPopulation().toDouble()).roundToInt()
                    }
                    else -> {}
                }

                if (sampleSize == 0)
                {
                    val fragment = currentFragment as? CreateSampleFragment
                    fragment?.let { fragment ->
                        Toast.makeText( fragment.activity!!.applicationContext, "${fragment.activity!!.getString(R.string.no_eligible_households)}", Toast.LENGTH_SHORT).show()
                    }
                }
                else
                {
                    val sampledIndices = ArrayList<Int>()

                    for (i in 0 until sampleSize)
                    {
                        var rnds = (0 until validSamples.size).random()

                        while(sampledIndices.contains(rnds))
                        {
                            rnds = (0 until validSamples.size).random()
                        }

                        sampledIndices.add(rnds)
                        validSamples[rnds].syncCode = validSamples[rnds].syncCode + 1
                        validSamples[rnds].samplingState = SamplingState.Sampled
                    }

                    val fragment = currentFragment as? CreateSampleFragment
                    fragment?.sampleGenerated()
                }
            }
        }
    }

    fun totalPopulation() : Int
    {
        var total = 0

        currentConfig?.value?.let { config ->
            for (enumArea in config.enumAreas)
            {
                for (location in enumArea.locations)
                {
                    if (!location.isLandmark && location.enumerationItems.isNotEmpty())
                    {
                        for (enumerationItem in location.enumerationItems)
                        {
                            if (enumerationItem.enumerationState == EnumerationState.Enumerated)
                            {
                                for (fieldData in enumerationItem.fieldDataList)
                                {
                                    DAO.fieldDAO.getField( fieldData.fieldUuid )?.let { field ->
                                        if (field.type == FieldType.Number && field.numberOfResidents)
                                        {
                                            total += 1
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return total
    }

    fun performClusterSampling()
    {
        currentEnumArea?.value?.let { enumArea ->

            _currentSampledItemsForSampling.clear()

            for (location in enumArea.locations)
            {
                if (!location.isLandmark && location.enumerationItems.isNotEmpty())
                {
                    for (sampledItem in location.enumerationItems)
                    {
                        if(!_currentSampledItemsForSampling.contains(sampledItem))
                        {
                            _currentSampledItemsForSampling.add(sampledItem)
                        }
                    }
                }
            }

            // generate sample this cluster

            performSimpleRandomSampling()
        }
    }
}