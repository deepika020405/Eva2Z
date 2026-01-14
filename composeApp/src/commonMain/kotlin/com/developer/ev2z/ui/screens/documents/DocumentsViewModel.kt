package com.developer.ev2z.ui.screens.documents

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.developer.ev2z.data.local.StorageKeys
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.VaultDocumentDto
import com.developer.ev2z.util.crypto.AesDecryptor
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

                val encryptedBytes = apiService.getVaultDocumentFile(
                    documentId = documentId,
                    vaultToken = vaultToken,
                    authToken = authToken
                )

                // 🔑 Example key/iv (backend se confirm karo)
                val key = vaultToken.take(16).toByteArray()
                val iv = ByteArray(16) { 0 }

                val decrypted = AesDecryptor.decrypt(
                    encrypted = encryptedBytes,
                    secretKey = key,
                    iv = iv
                )

                onResult(decrypted)

            } catch (e: Exception) {
                println("❌ Decryption failed: ${e.message}")
            }
        }
    }
}