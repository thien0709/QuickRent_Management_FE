package com.bxt.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bxt.viewmodel.WelcomeViewModel

@Composable
fun WelcomeScreen(
    onCompleteWelcome: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel()
)
{
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { uiState.pages.size })

    LaunchedEffect(uiState.currentPage) {
        pagerState.animateScrollToPage(uiState.currentPage)
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            if (pagerState.currentPage > uiState.currentPage)
                viewModel.nextPage()
            else
                viewModel.previousPage()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFFF8F6F0))
            .systemBarsPadding()
    ) {

        if (!uiState.isLastPage) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    viewModel.skipOnboarding()
                    onCompleteWelcome()
                }) {
                    Text("Bỏ qua")
                }
            }
        } else {
            Spacer(modifier = Modifier.height(48.dp))
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { index ->
            val page = uiState.pages[index]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Fake image
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📱", fontSize = 64.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    page.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    page.description,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        }

        // Indicator
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            uiState.pages.forEachIndexed { index, _ ->
                val size = if (index == uiState.currentPage) 12.dp else 8.dp
                val color = if (index == uiState.currentPage) MaterialTheme.colorScheme.primary
                else Color.Gray.copy(alpha = 0.5f)
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(size)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        // Buttons
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (uiState.currentPage > 0) {
                OutlinedButton(onClick = { viewModel.previousPage() }) {
                    Text("Quay lại")
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            Button(onClick = {
                if (uiState.isLastPage) {
                    viewModel.completeOnboarding()
                    onCompleteWelcome()
                } else {
                    viewModel.nextPage()
                }
            }) {
                Text(if (uiState.isLastPage) "Bắt đầu" else "Tiếp theo")
            }
        }
    }
}
