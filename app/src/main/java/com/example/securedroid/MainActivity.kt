package com.example.securedroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                SecureDroidScreen()
            }
        }
    }
}

@Composable
fun SecureDroidScreen() {

    var status by remember {
        mutableStateOf("Ready to connect")
    }

    var response by remember {
        mutableStateOf("")
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "SecureDroid",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black
            )

            Text(
                text = "My own Android security lab",
                modifier = Modifier.padding(top = 12.dp),
                color = Color.Black
            )

            Text(
                text = status,
                modifier = Modifier.padding(top = 20.dp),
                color = Color.Black
            )

            Button(
                onClick = {

                    status = "Connecting to Kali..."
                    response = ""

                    scope.launch {

                        try {

                            response = withContext(Dispatchers.IO) {
                                runKaliScan()
                            }

                            status = "Kali scan completed"

                        } catch (error: Exception) {

                            status = "Connection failed"

                            response = error.message
                                ?: "Unknown error"
                        }
                    }
                },

                modifier = Modifier.padding(top = 20.dp)
            ) {

                Text("Run Kali Scan")
            }

            if (response.isNotEmpty()) {

                Text(
                    text = response,

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),

                    color = Color.Black
                )
            }
        }
    }
}

private fun runKaliScan(): String {

    val url = URL("http://192.168.0.9:8000/scan")

    val connection = url.openConnection() as HttpURLConnection

    connection.requestMethod = "GET"
    connection.connectTimeout = 5000
    connection.readTimeout = 10000

    return try {

        connection.connect()

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {

            connection.inputStream
                .bufferedReader()
                .use { it.readText() }

        } else {

            "Kali returned HTTP ${connection.responseCode}"
        }

    } finally {

        connection.disconnect()
    }
}