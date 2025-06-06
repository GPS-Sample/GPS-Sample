/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.utils

import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtil {

    const val kSalt = "c2xTT0lua0ZWbHdvZWlTTGs="
    const val kIv = "Y2Jma25sRkRMS1NHekNWTA=="

    fun Encrypt(strToEncrypt: String, password: String) :  String
    {
        try
        {
            val ivParameterSpec = IvParameterSpec(Base64.decode(kIv, Base64.DEFAULT))
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec = PBEKeySpec(password.toCharArray(), Base64.decode(kSalt, Base64.DEFAULT), 10000, 128)

            val tmp = factory.generateSecret(spec)
            val secretKeySpec = SecretKeySpec(tmp.encoded, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)

            return Base64.encodeToString(
                cipher.doFinal(strToEncrypt.toByteArray(Charsets.UTF_8)),
                Base64.DEFAULT
            )
        }
        catch (e: Exception)
        {
            Log.d("xxx", e.stackTraceToString())
        }

        return ""
    }

    fun Decrypt(strToDecrypt : String, password: String) : String? {
        try
        {
            val ivParameterSpec =  IvParameterSpec(Base64.decode(kIv, Base64.DEFAULT))
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec =  PBEKeySpec(password.toCharArray(), Base64.decode(kSalt, Base64.DEFAULT), 10000, 128)
            val tmp = factory.generateSecret(spec);
            val secretKeySpec =  SecretKeySpec(tmp.encoded, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            return  String(cipher.doFinal(Base64.decode(strToDecrypt, Base64.DEFAULT)))
        }
        catch (e : Exception)
        {
            Log.d( "xxx",e.stackTraceToString());
        }

        return null
    }
}