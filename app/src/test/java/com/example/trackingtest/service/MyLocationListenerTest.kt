package com.example.trackingtest.service

import android.location.Location
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

class MyLocationListenerTest {

    @Test
    fun formatDistanceForDisplay() {
        val listener = MyLocationListener(
            mutableListOf(),
            selectedAccuracy = "",
            interval = 0,
            mutableFloatStateOf(0F),
            mutableStateOf(Location("")),
            isServiceRunning = { true },
            )

        val result = listener.formatDistanceForDisplay(14125.367F)
        val expected = "14.13"
        assertEquals(expected, result)
    }
}