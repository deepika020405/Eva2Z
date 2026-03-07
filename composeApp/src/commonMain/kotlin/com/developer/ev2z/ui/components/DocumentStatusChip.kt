//package com.developer.ev2z.ui.components
//
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.developer.ev2z.data.local.model.DocumentStatus
//import com.developer.ev2z.ui.screens.DriverScore.Eva2zColors.Surface
//
//@Composable
//fun DocumentStatusChip(status: DocumentStatus) {
//
//    val color = when (status) {
//        DocumentStatus.ACTIVE -> Color(0xFF4CAF50)
//        DocumentStatus.EXPIRING -> Color(0xFFFFA000)
//        DocumentStatus.EXPIRED -> Color.Red
//        DocumentStatus.VERIFIED -> Color(0xFF2196F3)
//    }
//
//    Surface(
//        shape = RoundedCornerShape(50),
//        color = color.copy(alpha = 0.1f)
//    ) {
//
//        Text(
//            text = status.name,
//            color = color,
//            modifier = Modifier.padding(
//                horizontal = 10.dp,
//                vertical = 4.dp
//            ),
//            fontSize = 12.sp
//        )
//
//    }
//
//}