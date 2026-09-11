package com.example.securedroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.securedroid.ui.theme.SecureDroidTheme
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SecureDroidTheme {
                SecureDroidScreen(
                    onScan = { endpoint, onResult ->
                        executor.execute {
                            val result = performScan(endpoint)
                            runOnUiThread {
                                onResult(result)
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}

private fun performScan(endpoint: String): ScanResult {
    var connection: HttpURLConnection? = null

    return try {
        val url = URL(endpoint)
        connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.setRequestProperty("Accept", "application/json")

        val responseCode = connection.responseCode

        if (responseCode != HttpURLConnection.HTTP_OK) {
            return ScanResult(
                success = false,
                message = "Backend returned HTTP $responseCode"
            )
        }

        val response = connection.inputStream
            .bufferedReader()
            .use { it.readText() }

        val json = JSONObject(response)

        val ports = json.optJSONArray("listening_tcp_ports")
        val portList = mutableListOf<String>()

        if (ports != null) {
            for (index in 0 until ports.length()) {
                portList.add(ports.getInt(index).toString())
            }
        }

        val recommendations = json.optJSONArray("recommendations")
        val recommendationList = mutableListOf<String>()

        if (recommendations != null) {
            for (index in 0 until recommendations.length()) {
                recommendationList.add(
                    recommendations.getString(index)
                )
            }
        }

        ScanResult(
            success = true,
            message = "Scan completed successfully",
            host = json.optString("host", "Unknown"),
            operatingSystem = json.optString(
                "operating_system",
                "Unknown"
            ),
            osRelease = json.optString(
                "os_release",
                "Unknown"
            ),
            pythonVersion = json.optString(
                "python_version",
                "Unknown"
            ),
            listeningPorts = portList,
            recommendations = recommendationList
        )
    } catch (exception: Exception) {
        ScanResult(
            success = false,
            message = "Connection failed: ${exception.message}"
        )
    } finally {
        connection?.disconnect()
    }
}

private data class ScanResult(
    val success: Boolean,
    val message: String,
    val host: String = "",
    val operatingSystem: String = "",
    val osRelease: String = "",
    val pythonVersion: String = "",
    val listeningPorts: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
)

@androidx.compose.runtime.Composable
private fun SecureDroidScreen(
    onScan: (
        String,
        (ScanResult) -> Unit
    ) -> Unit
) {
    var endpoint by remember {
        mutableStateOf("http://192.168.0.8:8000/scan")
    }

    var result by remember {
        mutableStateOf<ScanResult?>(null)
    }

    var isScanning by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "SecureDroid",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Authorized Android Security Laboratory",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = endpoint,
            onValueChange = { endpoint = it },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Backend /scan URL")
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                isScanning = true
                result = null

                onScan(endpoint) { scanResult ->
                    result = scanResult
                    isScanning = false
                }
            },
            enabled = !isScanning && endpoint.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(20.dp)
                        .height(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Run Security Scan")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        result?.let { scanResult ->

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (scanResult.success) {
                            "Scan Result"
                        } else {
                            "Scan Error"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(scanResult.message)

                    if (scanResult.success) {
                        Spacer(modifier = Modifier.height(16.dp))

                        ResultRow("Host", scanResult.host)

                        ResultRow(
                            "Operating System",
                            scanResult.operatingSystem
                        )

                        ResultRow(
                            "OS Release",
                            scanResult.osRelease
                        )

                        ResultRow(
                            "Python",
                            scanResult.pythonVersion
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Listening TCP Ports",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            if (scanResult.listeningPorts.isEmpty()) {
                                "None detected"
                            } else {
                                scanResult.listeningPorts.joinToString(
                                    separator = ", "
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Recommendations",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        scanResult.recommendations.forEach { recommendation ->
                            Text(
                                text = "• $recommendation",
                                modifier = Modifier.padding(
                                    vertical = 3.dp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ResultRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(130.dp)
        )

        Text(text = value)
    }
}
