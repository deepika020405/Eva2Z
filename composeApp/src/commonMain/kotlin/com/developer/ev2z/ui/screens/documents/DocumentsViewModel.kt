package com.developer.ev2z.ui.screens.documents

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.developer.ev2z.data.local.StorageKeys
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.VaultDocumentDto
import com.developer.ev2z.util.crypto.randomHex
import com.developer.ev2z.util.crypto.sha256
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentsViewModel(
    private val apiService: ApiService,
    private val storageManager: StorageManager
) {

    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _documents = mutableStateOf<List<VaultDocumentDto>>(emptyList())
    val documents: State<List<VaultDocumentDto>> = _documents

    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    // ---------------- LOAD DOCUMENTS ----------------

    fun loadDocuments() {
        viewModelScope.launch {
            try {
                _loading.value = true

                val vaultToken = storageManager.getString(StorageKeys.VAULT_TOKEN, "")
                val authToken = storageManager.getString("token", "")

                if (vaultToken.isEmpty()) {
                    _error.value = "Vault session expired"
                    return@launch
                }

                val result = withContext(Dispatchers.IO) {
                    apiService.getVaultDocuments(vaultToken, authToken)
                }

                _documents.value = result
                _error.value = null

            } catch (e: Exception) {
                _error.value = "Failed to load documents"
            } finally {
                _loading.value = false
            }
        }
    }

    // ---------------- OPEN DOCUMENT ----------------

    fun openDocument(
        documentId: String,
        onResult: (ByteArray) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _loading.value = true

                val vaultToken = storageManager.getString(StorageKeys.VAULT_TOKEN, "")
                val authToken = storageManager.getString("token", "")

                val fileBytes = withContext(Dispatchers.IO) {
                    apiService.getVaultDocumentFile(
                        documentId,
                        vaultToken,
                        authToken
                    )
                }

                // 🔍 debug signature
                val signature = fileBytes.take(4)
                    .joinToString(" ") {
                        it.toInt().and(0xFF).toString(16).padStart(2, '0')
                    }
                println("📄 File signature = $signature")

                onResult(fileBytes) // ✅ MAIN THREAD SAFE

            } catch (e: Exception) {
                _error.value = "Unable to open document"
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    // ---------------- DELETE DOCUMENT ----------------

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true

                val vaultToken = storageManager.getString(StorageKeys.VAULT_TOKEN, "")
                val authToken = storageManager.getString("token", "")

                withContext(Dispatchers.IO) {
                    apiService.deleteVaultDocument(
                        documentId,
                        vaultToken,
                        authToken
                    )
                }

                loadDocuments()

            } catch (e: Exception) {
                _error.value = "Delete failed"
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    // ---------------- UPLOAD DOCUMENT ----------------

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

        withContext(Dispatchers.IO) {
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
}
