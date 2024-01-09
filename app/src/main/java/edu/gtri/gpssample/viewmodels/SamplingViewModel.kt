package edu.gtri.gpssample.viewmodels

import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.managers.MapboxManager
import edu.gtri.gpssample.utils.DateUtils
import java.lang.Integer.min
import java.util.*
import kotlin.math.roundToInt

class SamplingViewModel : ViewModel()
{
    private lateinit var mapboxManager: MapboxManager
    private lateinit var pointAnnotationManager: PointAnnotationManager

    private var _currentFragment : Fragment? = null
    private var _currentStudy : MutableLiveData<Study>? = null
    private var _currentConfig : MutableLiveData<Config>? = null
    private var _currentEnumArea : MutableLiveData<EnumArea>? = null
    private var _currentSampledItemsForSampling : ArrayList<EnumerationItem> = ArrayList()

    private var allPointAnnotations = java.util.ArrayList<PointAnnotation>()

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
                Log.d( "xxx", study.samplingMethod.format)
                _currentFragment?.let{fragment ->
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
                for (location in enumArea.locations)
                {
                    if (!location.isLandmark && location.enumerationItems.isNotEmpty())
                    {
                        var resourceId: Int
                        var isMultiFamily = false

                        location.isMultiFamily?.let {
                            isMultiFamily = it
                        }

                        if (!isMultiFamily)
                        {
                            val sampledItem = location.enumerationItems[0]

                            resourceId = when(sampledItem.samplingState)
                            {
                                SamplingState.None       -> R.drawable.home_black
                                SamplingState.NotSampled -> R.drawable.home_green
                                SamplingState.Sampled    -> R.drawable.home_blue
                                SamplingState.Resampled  -> R.drawable.home_blue
                                SamplingState.Invalid    -> R.drawable.home_red
                            }
                        }
                        else
                        {
                            resourceId = R.drawable.multi_home_green

                            for (sampledItem in location.enumerationItems)
                            {
                                if (sampledItem.samplingState == SamplingState.Sampled)
                                {
                                    resourceId = R.drawable.multi_home_blue
                                }
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

        return SamplingState.None
    }

    fun validateRule(rule : Rule, fieldData : FieldData) : Boolean
    {
        var validRule = false

        fieldData.field?.let{field->
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
                    currentConfig?.value?.let { config ->
                        for (enumArea in config.enumAreas) {
                            for (location in enumArea.locations) {
                                if (!location.isLandmark && location.enumerationItems.isNotEmpty()) {
                                    var isMultiFamily = false

                                    location.isMultiFamily?.let {
                                        isMultiFamily = it
                                    }

                                    for (enumerationItem in location.enumerationItems)
                                    {
                                        if (!_currentSampledItemsForSampling.contains(enumerationItem)) {
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

                // find and remove items that are not valid
                if (sampleItem.enumerationState == EnumerationState.Enumerated)
                {
                    if (study.filters.isEmpty())
                    {
                        validSamples.add(sampleItem)
                        continue
                    }

                    // add in filters
                    var validSample = false
                    for (filter in study.filters)
                    {
                        var validRule = false
                        var validFilterOperator = false
                        filter.rule?.let{rule ->
                            rule.field?.let{field ->
                                Log.d("XXXXXXXX", "the field ${field.name}")
                                for (fieldData in sampleItem.fieldDataList)
                                {
                                    Log.d("XXXXX", "field name ${field.name} == fielddata name ${fieldData.field?.name}")
                                    if( fieldData.field?.name.equals( field.name))
                                    {
                                        validRule = validateRule(rule, fieldData)
                                    }
                                }
                            }

                            var filterOperator : FilterOperator? = rule.filterOperator

                            if (filterOperator == null)
                            {
                                if (validRule)
                                {
                                    validSamples.add(sampleItem)
                                }
                            }
                            else
                            {
                                while(filterOperator != null)
                                {
                                    var validNextRule = false

                                    // check each rule
                                    filterOperator?.let{fo ->
                                        fo.rule?.let{nextRule ->
                                            // check next rule
                                            nextRule.field?.let{field ->
                                                Log.d("XXXXXXXX", "the field ${field.name}")
                                                for (fieldData in sampleItem.fieldDataList)
                                                {
                                                    if( fieldData.field?.name.equals( field.name))
                                                    {
                                                        validNextRule = validateRule(nextRule, fieldData)
                                                        // check the operator
                                                        if(validNextRule)
                                                        {
                                                            when(fo.conenctor)
                                                            {
                                                                Connector.AND->{
                                                                    validFilterOperator = (validRule && validNextRule)
                                                                }
                                                                Connector.OR->{
                                                                    validFilterOperator = (validRule || validNextRule)
                                                                }
                                                                Connector.NOT->{
                                                                    validFilterOperator = (validRule && !validNextRule)
                                                                }
                                                                else->{
                                                                    validFilterOperator = false
                                                                }
                                                            }
                                                        }
                                                        if(!validNextRule || !validFilterOperator)
                                                        {
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                            // check constraint
                                            filterOperator = nextRule.filterOperator
                                        }
                                    }?:run{
                                        filterOperator = null
                                    }
                                }
                            }
                        }

                        validSample = validRule && validFilterOperator

                        if(!validSample)
                        {
                            break;
                        }
                    }

                    if(validSample)
                    {
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
                        sampleSize = (study.sampleSize.toDouble() / 100.0 * study.totalPopulationSize.toDouble()).roundToInt()
                    }
                    else -> {}
                }

                if (sampleSize > 0)
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
                        validSamples[rnds].samplingState = SamplingState.Sampled
                    }
                }
            }
        }
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