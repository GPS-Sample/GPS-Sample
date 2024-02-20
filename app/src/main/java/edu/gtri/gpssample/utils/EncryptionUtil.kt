package edu.gtri.gpssample.utils

import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtil {

    const val kSecretKey = "B08DC45F248BA92F28AA35AC6C157BBD9CC8A3DE"
    const val kSalt = "c2xTT0lua0ZWbHdvZWlTTGs="
    const val kIv = "Y2Jma25sRkRMS1NHekNWTA=="

    fun createKeys()
    {
        val secret = "GPS_SAMPLE CDC GTRI"
        val digest = MessageDigest.getInstance("SHA-1")
        val result = digest.digest(secret.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder()
        for (b in result) {
            sb.append(String.format("%02X", b))
        }
        val hashedPassword = sb.toString()

        val saltString = "slSOInkFVlwoeiSLk"
        val encodedSaltString: String = Base64.encodeToString(saltString.toByteArray(), 0)

        val ivString = "cbfknlFDLKSGzCVL"
        val encodedIvString: String = Base64.encodeToString(ivString.toByteArray(), 0)

        Log.d("XXXXXXX", "secret key ${hashedPassword}")
        Log.d("XXXXXXX","iv key ${encodedIvString}")
        Log.d("XXXXXXX","salt string ${encodedSaltString}")

        Log.d("XXXXXXXXXX", "---------- BIG TEST ")
        val encrypted = Encrypt("ENCRYPT TEST","")
        Log.d("XXXXXXXXXX", "Encrypted $encrypted")
        val decrypted = Decrypt(encrypted,"")
        Log.d("XXXXXXXXXX", "Decrypted $decrypted")
        Log.d("XXXXXXXXXX", "DONE!!")
    }


    fun Encrypt(strToEncrypt: String, password: String) :  String
    {
        try {
            val secretKey = if (password.isNotEmpty()) password else kSecretKey
            val ivParameterSpec = IvParameterSpec(Base64.decode(kIv, Base64.DEFAULT))
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec = PBEKeySpec(secretKey.toCharArray(), Base64.decode(kSalt, Base64.DEFAULT), 10000, 128)

            val tmp = factory.generateSecret(spec)
            val secretKeySpec = SecretKeySpec(tmp.encoded, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)

            return Base64.encodeToString(
                cipher.doFinal(strToEncrypt.toByteArray(Charsets.UTF_8)),
                Base64.DEFAULT
            )
        } catch (e: Exception) {
            Log.d("XXXXX", "ERROR ${e.message}")
        }

        return ""
    }

    fun Decrypt(strToDecrypt : String, password: String) : String? {
        try
        {
            val secretKey = if (password.isNotEmpty()) password else kSecretKey

            val ivParameterSpec =  IvParameterSpec(Base64.decode(kIv, Base64.DEFAULT))
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec =  PBEKeySpec(secretKey.toCharArray(), Base64.decode(kSalt, Base64.DEFAULT), 10000, 128)
            val tmp = factory.generateSecret(spec);
            val secretKeySpec =  SecretKeySpec(tmp.encoded, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            return  String(cipher.doFinal(Base64.decode(strToDecrypt, Base64.DEFAULT)))
        }
        catch (e : Exception) {
            println("Error while decrypting: $e");
        }

        return null
    }
}