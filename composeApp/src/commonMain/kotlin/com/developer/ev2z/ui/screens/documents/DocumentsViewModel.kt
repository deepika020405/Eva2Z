package com.developer.ev2z.ui.screens.documents

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.developer.ev2z.data.local.StorageKeys
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.VaultDocumentDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
}