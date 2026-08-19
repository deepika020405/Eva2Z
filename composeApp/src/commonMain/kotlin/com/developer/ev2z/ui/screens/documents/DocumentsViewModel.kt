package com.developer.ev2z.ui.screens.documents

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.developer.ev2z.data.local.StorageKeys
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.VaultDocumentDetail
import com.developer.ev2z.response.VaultDocumentDto
import com.developer.ev2z.response.VaultUploadResponse
import com.developer.ev2z.util.crypto.randomHex
import com.developer.ev2z.util.crypto.sha256
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object VaultSessionCache {
    val docs = mutableListOf<VaultDocumentDto>()
    val images = mutableMapOf<String, ByteArray>()

    fun put(doc: VaultDocumentDto, imageBytes: ByteArray?) {
        docs.removeAll { it.id == doc.id }
        docs.add(0, doc)
        if (imageBytes != null) images[doc.id] = imageBytes
    }

    fun remove(documentId: String) {
        docs.removeAll { it.id == documentId }
        images.remove(documentId)
    }
}

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

    // Last list returned by the server.
    private var serverDocs: List<VaultDocumentDto> = emptyList()

    init {
        // Show any docs uploaded earlier this session right away, even before
        // the first network load completes (and even on a fresh ViewModel after
        // navigating back into the screen).
        publishMerged()
    }

    private fun publishMerged() {
        val serverIds = serverDocs.map { it.id }.toSet()
        _documents.value = serverDocs + VaultSessionCache.docs.filter { it.id !in serverIds }
    }


    fun addOrUpdateLocal(doc: VaultDocumentDto, imageBytes: ByteArray? = null) {
        VaultSessionCache.put(doc, imageBytes)
        publishMerged()
    }

    // ---------------- LOAD DOCUMENTS ----------------

    fun loadDocuments() {
        viewModelScope.launch {
            try {
                _loading.value = true
                fetchDocuments()
            } catch (e: Exception) {
                _error.value = "Failed to load documents"
            } finally {
                _loading.value = false
            }
        }
    }


    private fun refreshDocumentsSilently() {
        viewModelScope.launch {
            try {
                fetchDocuments()
            } catch (e: Exception) {
                _error.value = "Failed to load documents"
            }
        }
    }

    private suspend fun fetchDocuments() {
        val vaultToken = storageManager.getString(StorageKeys.VAULT_TOKEN, "")
        val authToken = storageManager.getString("token", "")

        if (vaultToken.isEmpty()) {
            _error.value = "Vault session expired"
            return
        }

        val result = withContext(Dispatchers.IO) {
            apiService.getVaultDocuments(vaultToken, authToken)
        }

        serverDocs = result
        publishMerged()
        _error.value = null
    }

    // ---------------- DOCUMENT DETAIL ----------------

    fun loadDocumentDetail(
        documentId: String,
        onResult: (VaultDocumentDetail?) -> Unit
    ) {
        viewModelScope.launch {
            val vaultToken = storageManager.getString(StorageKeys.VAULT_TOKEN, "")
            val authToken = storageManager.getString("token", "")

            val detail = withContext(Dispatchers.IO) {
                apiService.getVaultDocumentDetail(documentId, vaultToken, authToken)
            }
            onResult(detail)
        }
    }

    // ---------------- OPEN DOCUMENT ----------------

    fun openDocument(
        documentId: String,
        onError: () -> Unit = {},
        onResult: (ByteArray) -> Unit
    ) {
        // Show the locally cached image instantly for docs uploaded this session.
        VaultSessionCache.images[documentId]?.let { cached ->
            onResult(cached)
            return
        }

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
                onError()
            } finally {
                _loading.value = false
            }
        }
    }

    // ---------------- DELETE DOCUMENT ----------------

    fun deleteDocument(documentId: String) {
        // Optimistically drop the document so its card disappears from the list
        // immediately — no full-screen spinner, no waiting for the round-trip.
        serverDocs = serverDocs.filterNot { it.id == documentId }
        VaultSessionCache.remove(documentId)
        publishMerged()

        viewModelScope.launch {
            try {
                val vaultToken = storageManager.getString(StorageKeys.VAULT_TOKEN, "")
                val authToken = storageManager.getString("token", "")

                withContext(Dispatchers.IO) {
                    apiService.deleteVaultDocument(
                        documentId,
                        vaultToken,
                        authToken
                    )
                }

                // Reconcile with the server (silently, keeping the list visible).
                refreshDocumentsSilently()

            } catch (e: Exception) {
                // Delete failed — restore the real server state and surface it.
                _error.value = "Delete failed"
                e.printStackTrace()
                refreshDocumentsSilently()
            }
        }
    }

    // ---------------- UPLOAD DOCUMENT ----------------

    suspend fun uploadDocument(
        fileName: String,
        imageBytes: ByteArray
    ): VaultUploadResponse {
        val vaultToken = storageManager.getString(StorageKeys.VAULT_TOKEN, "")
        val authToken = storageManager.getString("token", "")

        val salt = randomHex(16)
        val iv = randomHex(16)
        val authTag = randomHex(16)
        val checksum = sha256(imageBytes)

        return withContext(Dispatchers.IO) {
            apiService.uploadVaultDocument(
                vaultToken = vaultToken,
                authToken = authToken,
                encryptedFile = imageBytes,
                fileName = fileName,
                fileType = "image/jpeg",
                salt = salt,
                iv = iv,
                authTag = authTag,
                checksum = checksum
            )
        }
    }

    suspend fun updateDocument(
        documentId: String,
        fileName: String,
        imageBytes: ByteArray?
    ): VaultUploadResponse {
        val vaultToken = storageManager.getString(StorageKeys.VAULT_TOKEN, "")
        val authToken = storageManager.getString("token", "")

        // vault/update/{id} handles both cases — with new bytes it replaces the
        // file, without them it renames. (The old vault reupload route is dead
        // server-side: VaultController::reupload does not exist → 500.)
        val salt = randomHex(16)
        val iv = randomHex(16)
        val authTag = randomHex(16)
        val checksum = sha256(imageBytes ?: fileName.toByteArray())

        return withContext(Dispatchers.IO) {
            apiService.updateVaultDocument(
                documentId = documentId,
                vaultToken = vaultToken,
                authToken = authToken,
                encryptedFile = imageBytes,
                fileName = fileName,
                fileType = "image/jpeg",
                salt = salt,
                iv = iv,
                authTag = authTag,
                checksum = checksum
            )
        }
    }
}
