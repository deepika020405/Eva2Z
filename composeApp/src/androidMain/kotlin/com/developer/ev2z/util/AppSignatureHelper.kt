//package com.developer.ev2z.util
//
//import android.content.Context
//import android.content.pm.PackageManager
//import android.os.Build
//import android.util.Base64
//import android.util.Log
//import java.nio.charset.StandardCharsets
//import java.security.MessageDigest
//import java.util.*
//
//class AppSignatureHelper(private val context: Context) {
//
//    companion object {
//        private const val TAG = "AppSignatureHelper"
//        private const val HASH_TYPE = "SHA-256"
//        private const val NUM_HASHED_BYTES = 9
//        private const val NUM_BASE64_CHAR = 11
//    }
//
//    fun getAppSignatures(): ArrayList<String> {
//        val appCodes = ArrayList<String>()
//
//        try {
//            val packageName = context.packageName
//            val packageManager = context.packageManager
//
//            // ✅ Get app signatures based on Android version
//            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                // For Android 9 (API 28) and above
//                val packageInfo = packageManager.getPackageInfo(
//                    packageName,
//                    PackageManager.GET_SIGNING_CERTIFICATES
//                )
//                val signingInfo = packageInfo.signingInfo // ✅ Fixed null safety
//
//                if (signingInfo != null) {
//                    if (signingInfo.hasMultipleSigners()) {
//                        signingInfo.apkContentsSigners
//                    } else {
//                        signingInfo.signingCertificateHistory
//                    }
//                } else {
//                    arrayOf() // Return empty array if signingInfo is null
//                }
//            } else {
//                // For Android 8.1 (API 27) and below
//                @Suppress("DEPRECATION")
//                packageManager.getPackageInfo(
//                    packageName,
//                    PackageManager.GET_SIGNATURES
//                ).signatures
//            }
//
//            // ✅ Generate hash for each signature
//            signatures?.forEach { signature ->
//                val hash = hash(packageName, signature.toCharsString())
//                if (hash != null) {
//                    appCodes.add(hash)
//                    Log.d(TAG, "📱 Hash Code: $hash")
//                }
//            }
//
//        } catch (e: Exception) {
//            Log.e(TAG, "❌ Error getting app signatures: ${e.message}", e)
//        }
//
//        return appCodes
//    }
//
//    private fun hash(packageName: String, signature: String): String? {
//        val appInfo = "$packageName $signature"
//
//        return try {
//            val messageDigest = MessageDigest.getInstance(HASH_TYPE)
//            messageDigest.update(appInfo.toByteArray(StandardCharsets.UTF_8))
//
//            // Get first 9 bytes of the hash
//            var hashSignature = messageDigest.digest()
//            hashSignature = Arrays.copyOfRange(hashSignature, 0, NUM_HASHED_BYTES)
//
//            // Encode to Base64 and get first 11 characters
//            var base64Hash = Base64.encodeToString(
//                hashSignature,
//                Base64.NO_PADDING or Base64.NO_WRAP
//            )
//            base64Hash = base64Hash.substring(0, NUM_BASE64_CHAR)
//
//            Log.d(TAG, "✅ Package: $packageName -> Hash: $base64Hash")
//            base64Hash
//
//        } catch (e: Exception) {
//            Log.e(TAG, "❌ Error hashing: ${e.message}", e)
//            null
//        }
//    }
//
//    fun printAppSignaturesToLog() {
//        val hashes = getAppSignatures()
//
//        if (hashes.isEmpty()) {
//            Log.e(TAG, "❌ No hashes found!")
//        } else {
//            Log.d(TAG, "=".repeat(50))
//            Log.d(TAG, "📱 APP SIGNATURE HASHES FOR SMS RETRIEVER API")
//            Log.d(TAG, "=".repeat(50))
//
//            hashes.forEachIndexed { index, hash ->
//                Log.d(TAG, "Hash ${index + 1}: $hash")
//                println("📱 Hash ${index + 1}: $hash")
//            }
//
//            Log.d(TAG, "=".repeat(50))
//            Log.d(TAG, "✅ COPY THIS HASH TO YOUR BACKEND SMS TEMPLATE")
//            Log.d(TAG, "=".repeat(50))
//            Log.d(TAG, "SMS Format should be:")
//            Log.d(TAG, "<#> Your OTP is {otp}. Don't share this code.")
//            Log.d(TAG, "${hashes[0]}")
//            Log.d(TAG, "")
//            Log.d(TAG, "EVA2Z GPS")
//            Log.d(TAG, "=".repeat(50))
//        }
//    }
//}