/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package edu.gtri.gpssample.database.models

import android.util.Log
import edu.gtri.gpssample.constants.SampleType
import edu.gtri.gpssample.constants.SamplingMethod
import edu.gtri.gpssample.utils.EncryptionUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
data class Study(
    var uuid : String,
    var creationDate: Long,
    var name: String,
    var samplingMethod: SamplingMethod,
    var sampleSize: Int,
    var sampleType : SampleType,
    var subsetSampleName : String,
    var subsetSampleSize : Int,
    var subsetSampleType : SampleType,
    var stratas: ArrayList<Strata>,
    var fields : ArrayList<Field>,
    var primaryRules : ArrayList<Rule>,
    var primaryFilters : ArrayList<Filter>,
    var subsetRules : ArrayList<Rule>,
    var subsetFilters : ArrayList<Filter>,
    var version : String )
{
    constructor(name: String, samplingMethod: SamplingMethod, sampleSize: Int, sampleType: SampleType)
            : this(UUID.randomUUID().toString(), Date().time, name, samplingMethod, sampleSize, sampleType, "", 0, SampleType.None,ArrayList<Strata>(), ArrayList<Field>(), ArrayList<Rule>(),ArrayList<Filter>(),ArrayList<Rule>(), ArrayList<Filter>(), UUID.randomUUID().toString())

    constructor(uuid: String, creationDate: Long, name: String, samplingMethod: SamplingMethod, sampleSize: Int, sampleType: SampleType, subsetSampleName: String, subsetSampleSize: Int, subsetSampleType: SampleType )
            : this(uuid, creationDate, name, samplingMethod, sampleSize, sampleType, subsetSampleName, subsetSampleSize, subsetSampleType,ArrayList<Strata>(), ArrayList<Field>(), ArrayList<Rule>(), ArrayList<Filter>(), ArrayList<Rule>(), ArrayList<Filter>(), UUID.randomUUID().toString())

    fun pack(password: String) : String
    {
        val jsonString = Json.encodeToString( this )
        return  EncryptionUtil.Encrypt(jsonString,password)
    }

    companion object
    {
        fun unpack( message: String, password: String ) : Study?
        {
            try
            {
                val decrypted = EncryptionUtil.Decrypt(message,password)
                decrypted?.let {decrypted ->
                    return Json.decodeFromString<Study>( decrypted )
                }
            }
            catch (ex: Exception)
            {
                Log.d( "xxXXx", ex.stackTrace.toString())
            }
            return null
        }
    }
}