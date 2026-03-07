//package com.developer.ev2z.repository
//
//import androidx.compose.runtime.mutableStateListOf
//import com.developer.ev2z.data.local.model.VaultDocument
//
//class DocumentRepository {
//
//    private val documents = mutableStateListOf<VaultDocument>()
//
//    fun getDocuments(): List<VaultDocument> {
//        return documents
//    }
//
//    fun addDocument(doc: VaultDocument) {
//        documents.add(doc)
//    }
//
//    fun deleteDocument(id: String) {
//        documents.removeAll { it.id == id }
//    }
//
//}