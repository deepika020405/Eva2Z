//package com.developer.ev2z.ui.components
//
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material.icons.filled.Description
//import androidx.compose.material3.Card
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.SegmentedButtonDefaults.Icon
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.developer.ev2z.data.local.model.VaultDocument
//
//@Composable
//fun DocumentCard(
//    doc: VaultDocument,
//    onView: () -> Unit,
//    onDelete: () -> Unit
//) {
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 6.dp),
//        shape = RoundedCornerShape(16.dp)
//    ) {
//
//        Row(
//            modifier = Modifier
//                .padding(16.dp)
//        ) {
//
//            Icon(
//                Icons.Default.Description,
//                contentDescription = null,
//                modifier = Modifier.size(36.dp)
//            )
//
//            Spacer(Modifier.width(12.dp))
//
//            Column(
//                modifier = Modifier.weight(1f)
//            ) {
//
//                Text(
//                    doc.name,
//                    fontWeight = FontWeight.Bold
//                )
//
//                doc.expiryDate?.let {
//                    Text(
//                        "Expires $it",
//                        fontSize = 12.sp
//                    )
//                }
//
//                Spacer(Modifier.height(6.dp))
//
//                DocumentStatusChip(doc.status)
//
//            }
//
//            IconButton(
//                onClick = onDelete
//            ) {
//                Icon(Icons.Default.Delete, null)
//            }
//
//        }
//
//    }
//
//}