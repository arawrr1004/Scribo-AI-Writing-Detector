package com.example.scribo // Sesuaikan jika package name kamu berbeda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// 1. Model Data (Harus sesuai dengan Python)
data class PredictRequest(val teks: String)
data class PredictResponse(val hasil: String, val skor: Float)

// 2. Interface API
interface ApiService {
    @POST("/predict")
    fun cekTeks(@Body request: PredictRequest): Call<PredictResponse>
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AiDetectorScreen()
            }
        }
    }
}

// 3. Komponen Speedometer
@Composable
fun SpeedometerGauge(skor: Float) {
    val animatedSkor by animateFloatAsState(
        targetValue = skor,
        animationSpec = tween(durationMillis = 1000),
        label = "SpeedometerAnimation"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
        Canvas(modifier = Modifier.size(200.dp)) {
            // Background Abu-abu
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 25.dp.toPx(), cap = StrokeCap.Round)
            )
            // Progress Warna (Hijau ke Merah)
            drawArc(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Green, Color.Yellow, Color.Red)
                ),
                startAngle = 180f,
                sweepAngle = 180f * animatedSkor,
                useCenter = false,
                style = Stroke(width = 25.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(animatedSkor * 100).toInt()}%",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = "AI-Generated Possibilities", fontSize = 14.sp, color = Color.Gray)
        }
    }
}

// 4. Tampilan Utama
@Composable
fun AiDetectorScreen() {
    var teksInput by remember { mutableStateOf("") }
    var skorAI by remember { mutableStateOf(0f) }
    var hasilTeks by remember { mutableStateOf("Input Text to Analyze") }

    // GANTI IP DI SINI
    val ipLaptop = "192.168.100.20"

    val retrofit = remember {
        Retrofit.Builder()
            .baseUrl("http://$ipLaptop:8000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val apiService = remember { retrofit.create(ApiService::class.java) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "AI Writing Detector",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )

        // Tampilkan Speedometer
        SpeedometerGauge(skor = skorAI)

// TAMBAHKAN INI UNTUK DEBUG
        Text(text = "DEBUG SKOR: $skorAI", color = Color.Red, fontSize = 10.sp)

        // Hasil Teks
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = hasilTeks,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                // LOGIKA WARNA DINAMIS:
                color = when {
                    skorAI > 0.75f -> Color.Red          // Merah kalau pasti AI
                    skorAI > 0.40f -> Color(0xFFFBC02D)  // Kuning/Orange kalau meragukan (49% masuk sini)
                    else -> Color(0xFF388E3C)            // Hijau kalau manusia
                }
            )
        }

        // Input Teks
        OutlinedTextField(
            value = teksInput,
            onValueChange = { teksInput = it },
            label = { Text("Type Here...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )

        // Tombol Analisis
        Button(
            onClick = {
                if (teksInput.isBlank()) return@Button
                hasilTeks = "Analyzing Text"

                apiService.cekTeks(PredictRequest(teksInput)).enqueue(object : Callback<PredictResponse> {
                    override fun onResponse(call: Call<PredictResponse>, response: Response<PredictResponse>) {
                        if (response.isSuccessful) {
                            val data = response.body()

                            // Update teks hasil
                            hasilTeks = data?.hasil ?: "Selesai"

                            // Update skor untuk gerakkan speedometer
                            skorAI = data?.skor ?: 0.0f

                            // Tambahkan baris ini untuk cek di Logcat Android Studio
                            println("DEBUG SKOR DI ANDROID: ${data?.skor}")

                            skorAI = data?.skor ?: 0f
                            hasilTeks = data?.hasil ?: "Selesai"
                        } else {
                            hasilTeks = "Server Error: ${response.code()}"
                        }
                    }
                    override fun onFailure(call: Call<PredictResponse>, t: Throwable) {
                        hasilTeks = "Gagal terhubung: ${t.message}"
                    }
                })
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("CHECK", fontWeight = FontWeight.Bold)
        }
    }
}