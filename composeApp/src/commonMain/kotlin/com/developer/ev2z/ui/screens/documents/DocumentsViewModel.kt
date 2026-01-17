package com.developer.ev2z.ui.screens.documents

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.developer.ev2z.data.local.StorageKeys
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.VaultDocumentDto
import com.developer.ev2z.util.Base64Util
import com.developer.ev2z.util.crypto.AesDecryptor
import com.developer.ev2z.util.crypto.KeyDeriver
import com.developer.ev2z.util.crypto.randomHex
import com.developer.ev2z.util.crypto.sha256
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class DocumentsViewModel(
    private val apiService: ApiService,
    private val storageManager: StorageManager
) {

    private val _documents = mutableStateOf<List<VaultDocumentDto>>(emptyList())
    val documents: State<List<VaultDocumentDto>> = _documents

    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    fun loadDocuments() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                _loading.value = true

                val vaultToken =
                    storageManager.getString(StorageKeys.VAULT_TOKEN, "")
                val authToken =
                    storageManager.getString("token", "")

                if (vaultToken.isEmpty()) {
                    _error.value = "Vault session expired"
                    return@launch
                }

                val result = apiService.getVaultDocuments(
                    vaultToken = vaultToken,
                    authToken = authToken
                )

                _documents.value = result
                _error.value = null

            } catch (e: Exception) {
                _error.value = "Failed to load documents"
            } finally {
                _loading.value = false
            }
        }
    }


fun openDocument(documentId: String, onResult: (ByteArray) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val vaultToken = storageManager.getString(StorageKeys.VAULT_TOKEN, "")
            val authToken = storageManager.getString("token", "")

            val fileBytes = apiService.getVaultDocumentFile(
                documentId = documentId,
                vaultToken = vaultToken,
                authToken = authToken
            )

            // 🧪 File signature check
            val signature = fileBytes.take(4)
                .joinToString(" ") {
                    it.toInt().and(0xFF).toString(16).padStart(2, '0')
                }

            println("📄 File signature = $signature")

            // ✅ DIRECT USE — NO DECRYPTION
            onResult(fileBytes)

        } catch (e: Exception) {
            println("❌ Document open failed")
            e.printStackTrace()
        }
    }
}


    fun deleteDocument(documentId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val vaultToken =
                    storageManager.getString(StorageKeys.VAULT_TOKEN, "")
                val authToken =
                    storageManager.getString("token", "")

                apiService.deleteVaultDocument(
                    documentId = documentId,
                    vaultToken = vaultToken,
                    authToken = authToken
                )

                // ✅ refresh list after delete
                loadDocuments()

            } catch (e: Exception) {
                println("❌ Delete failed")
                e.printStackTrace()
            }
        }
    }



    suspend fun uploadDocument(
        fileName: String,
        imageBytes: ByteArray
    ) {
        val vaultToken = storageManager.getString(StorageKeys.VAULT_TOKEN, "")
        val authToken = storageManager.getString("token", "")

        val salt = randomHex(16)
        val iv = randomHex(16)
        val authTag = randomHex(16)
        val checksum = sha256(imageBytes)

        apiService.uploadVaultDocument(
            vaultToken = vaultToken,
            authToken = authToken,
            encryptedFile = imageBytes,
            fileName = fileName,
            fileType = fileName,
            salt = salt,
            iv = iv,
            authTag = authTag,
            checksum = checksum
        )
    }
}