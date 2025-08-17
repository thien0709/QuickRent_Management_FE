package com.bxt.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePager(
    photos: List<String>,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { max(1, photos.size) })

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFFF2F2F2))
    ) {
        if (photos.isNotEmpty()) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photos[page])
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            if (photos.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(photos.size) { i ->
                        val selected = pagerState.currentPage == i
                        Box(
                            modifier = Modifier
                                .size(if (selected) 10.dp else 8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Không có hình ảnh")
            }
        }
    }
}