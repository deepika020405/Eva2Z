@Composable
fun TrackingPreviewCard() {

    val images = listOf(
        Res.drawable.homebanner,
        Res.drawable.homebanner1
    )

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(190.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState()) // ✅ scroll enable
        ) {
            images.forEach { image ->
                Image(
                    painter = painterResource(image),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(320.dp)     // ✅ show full image width
                        .fillMaxHeight()
                )
            }
        }
    }
}