package com.example.attempt3

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BikeScooter
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NoAdultContent
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SmokeFree
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.ThumbUp

val habitIconMap = mapOf(
    "Book" to Icons.Default.Book,
    "DirectionsRun" to Icons.AutoMirrored.Default.DirectionsRun,
    "Email" to Icons.Default.Email,
    "Face" to Icons.Default.Face,
    "Favorite" to Icons.Default.Favorite,
    "FitnessCenter" to Icons.Default.FitnessCenter,
    "Healing" to Icons.Default.Healing,
    "Lightbulb" to Icons.Default.Lightbulb,
    "Restaurant" to Icons.Default.Restaurant,
    "School" to Icons.Default.School,
    "SelfImprovement" to Icons.Default.SelfImprovement,
    "ThumbUp" to Icons.Default.ThumbUp,
    "People" to Icons.Default.People,
    "Person" to Icons.Default.Person,
    "Public" to Icons.Default.Public,
    "Spa" to Icons.Default.Spa,
    "Palette" to Icons.Default.Palette,
    "MusicNote" to Icons.Default.MusicNote,
    "Hiking" to Icons.Default.Hiking,
    "BikeScooter" to Icons.Default.BikeScooter,
    "Edit" to Icons.Default.Edit,
    "Bedtime" to Icons.Default.Bedtime,
    "SmokeFree" to Icons.Default.SmokeFree,
    "Labs" to Icons.Default.Science, // Using Science for Labs
    "MonitorHeart" to Icons.Default.MonitorHeart, // For Cardio Load
    "Dining" to Icons.Default.Dining, // For Dine In
    "Exclamation" to Icons.Default.Error, // Using Error for Exclamation
    "Science" to Icons.Default.Science,
    "NoAdultContent" to Icons.Default.NoAdultContent,
    "Psychology" to Icons.Default.Psychology,
    "Relax" to Icons.Default.HotTub,
    "Heartbroken" to Icons.Default.SentimentVeryDissatisfied
)

const val defaultHabitIconKey = "SelfImprovement"