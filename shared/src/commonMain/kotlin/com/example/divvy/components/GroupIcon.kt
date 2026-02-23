package com.example.divvy.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
enum class GroupIcon(val imageVector: ImageVector) {
    Home(Icons.Rounded.Home),
    Flight(Icons.Rounded.Flight),
    Restaurant(Icons.Rounded.Restaurant),
    Work(Icons.Rounded.Work),
    ShoppingBag(Icons.Rounded.ShoppingBag),
    Grocery(Icons.Rounded.LocalGroceryStore),
    Car(Icons.Rounded.DirectionsCar),
    School(Icons.Rounded.School),
    Movie(Icons.Rounded.Movie),
    Music(Icons.Rounded.MusicNote),
    Pets(Icons.Rounded.Pets),
    Celebration(Icons.Rounded.Celebration),
    Gaming(Icons.Rounded.SportsEsports),
    Bank(Icons.Rounded.AccountBalance),
    Group(Icons.Rounded.Group)
}

@Composable
fun GroupIcon(
    icon: GroupIcon,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Icon(
        imageVector = icon.imageVector,
        contentDescription = icon.name,
        modifier = modifier,
        tint = tint
    )
}
