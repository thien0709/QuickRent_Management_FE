package com.bxt.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme

// Data classes đơn giản
data class FoodCategory(
    val id: String,
    val name: String,
    val icon: String,
    val color: Color = Color.White
)

data class PopularItem(
    val id: String,
    val name: String,
    val restaurant: String,
    val rating: Float,
    val price: String,
    val imageUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    // Search state
    var searchText by remember { mutableStateOf("") }




    // Static data categories (có thể thay bằng data từ API sau)
    val categories = remember {
        listOf(
            FoodCategory("1", "Steak", "🥩", Color(0xFFFFE0E0)),
            FoodCategory("2", "Sushi", "🍣", Color(0xFFE0F0FF)),
            FoodCategory("3", "Ramen", "🍜", Color(0xFFFFE0B0)),
            FoodCategory("4", "Burgers", "🍔", Color(0xFFE0FFE0)),
            FoodCategory("5", "Salad", "🥗", Color(0xFFE0FFF0)),
            FoodCategory("6", "Rice", "🍚", Color(0xFFF0E0FF))
        )
    }

    // Static popular items (có thể thay bằng data từ API sau)
    val popularItems = remember {
        listOf(
            PopularItem(
                id = "1",
                name = "Roasted Bone Marrow",
                restaurant = "by Inquisitive",
                rating = 4.8f,
                price = "₹ 600.0",
                imageUrl = "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=200"
            ),
            PopularItem(
                id = "2",
                name = "Grilled Salmon",
                restaurant = "by Ocean Delight",
                rating = 4.6f,
                price = "₹ 750.0",
                imageUrl = "https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=200"
            ),
            PopularItem(
                id = "3",
                name = "Wagyu Steak",
                restaurant = "by Premium Grill",
                rating = 4.9f,
                price = "₹ 1200.0",
                imageUrl = "https://images.unsplash.com/photo-1558030006-450675393462?w=200"
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F6F0)) // Cream background giống hình
            .padding(16.dp)
    ) {
        // Top Bar với Profile và Notification
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Image
            AsyncImage(
                model = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop&crop=face",
                contentDescription = "Profile",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .clickable { /* Handle profile click */ },
                contentScale = ContentScale.Crop
            )

            // Notification Icon
            IconButton(onClick = { /* Handle notification */ }) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Greeting Text
        Text(
            text = "Hey Shubham,",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = "What would you like to eat today?",
            fontSize = 16.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Search Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = {
                    Text("Search", color = Color(0xFF999999))
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF999999)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor =  Color.White,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Filter Button
            Card(
                modifier = Modifier
                    .size(56.dp)
                    .clickable { /* Handle filter */ },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Filter",
                        tint = Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Categories Section
        Text(
            text = "CATEGORIES",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow (
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(100.dp) // Chiều cao phù hợp cho 1 hàng
        ) {
            items(categories) { category ->
                CategoryCard(
                    category = category,
                    onClick = { /* Handle category click */ }
                )
            }
        }


        Spacer(modifier = Modifier.height(32.dp))

        // Popular Today Section
        Text(
            text = "POPULAR TODAY",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Popular Items List
        LazyColumn {
            items(popularItems) { item ->
                PopularItemCard(
                    item = item,
                    onClick = { /* Handle item click */ }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: FoodCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = category.icon,
                fontSize = 36.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun PopularItemCard(
    item: PopularItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Food Image
            AsyncImage(
                model = item.imageUrl,
                contentDescription = "Food Image",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Food Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = item.restaurant,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = item.rating.toString(),
                        fontSize = 14.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Text(
                    text = item.price,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Arrow Icon
            Card(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onClick() },
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "View Details",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}