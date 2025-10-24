package com.example.e_sentinel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.*
import okhttp3.Credentials
import android.widget.EditText
import org.json.JSONObject
import java.io.IOException
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatDelegate
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.android.RecognitionListener
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.StorageService


class MainActivity : AppCompatActivity(), RecognitionListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val client = OkHttpClient()
    private var lastVolumeButtonTime = 0L
    private val DOUBLE_PRESS_INTERVAL = 800 // milliseconds
    private lateinit var sensorManager: SensorManager
    private var fallDetected = false
    private var fallThreshold = 25.0f // Adjust based on testing



    private var isSpeechSectionVisible = false
    companion object {
        private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
    }

    private var model: Model? = null
    private var speechService: SpeechService? = null

    private lateinit var statusView: TextView
    private lateinit var partialView: TextView
    private lateinit var finalView: TextView
    private lateinit var modelSpinner: Spinner
    private lateinit var micToggle: ToggleButton
    private lateinit var pauseToggle: ToggleButton
    private lateinit var themeToggle: ToggleButton
    private lateinit var modelLabel: TextView
    private lateinit var statusLabel: TextView
    private lateinit var partialLabel: TextView
    private lateinit var finalLabel: TextView

    private val modelMap = mapOf(
//        "English India (Large)" to "model-large-en-in",
        "English India (Small)" to "model-small-en-in",
//        "English US (Small)" to "model-small-en-us",
//        "Hindi (Small)" to "model-small-hi",
//        "Telugu (Small)" to "model-small-te"
    )

    // List of possible distress keywords/phrases
    val distressKeywords = listOf(
        "help",
        "help me",
        "save me",
        "emergency",
        "please help",
        "i need help",
        "i'm in danger",
        "i am in danger",
        "call the police",
        "sos",
        "attack",
        "kidnap",
        "rape",
        "fire",
        "someone is following me",
        "danger",
        "please rescue me"
    )

    private lateinit var spinnerAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

//        textView = findViewById(R.id.textView)
//        textView.text = "Choose an option below"
//        / Initialize labels
        modelLabel = findViewById(R.id.label_model)
        statusLabel = findViewById(R.id.label_status)
        partialLabel = findViewById(R.id.label_partial)
        finalLabel = findViewById(R.id.label_final)

        // Initialize text views and buttons
        statusView = findViewById(R.id.status_text)
        partialView = findViewById(R.id.partial_text)
        finalView = findViewById(R.id.final_text)
        modelSpinner = findViewById(R.id.model_spinner)
        micToggle = findViewById(R.id.toggle_mic)
        pauseToggle = findViewById(R.id.pause)
        themeToggle = findViewById(R.id.toggle_theme)

        partialView.movementMethod = ScrollingMovementMethod()
        finalView.movementMethod = ScrollingMovementMethod()

        LibVosk.setLogLevel(LogLevel.INFO)

        val btnSpeechDet = findViewById<Button>(R.id.btnSpeechDet)
        val speechSection = findViewById<LinearLayout>(R.id.speechSection)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        btnSpeechDet.setOnClickListener {
            isSpeechSectionVisible = !isSpeechSectionVisible

            if (isSpeechSectionVisible) {
                speechSection.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate().alpha(1f).setDuration(200).start()
                }
            } else {
                speechSection.animate().alpha(0f).setDuration(200)
                    .withEndAction { speechSection.visibility = View.GONE }
                    .start()
            }
        }

        // Spinner setup
        val models = mutableListOf("None")
        models.addAll(modelMap.keys)
        spinnerAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, models) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(if (themeToggle.isChecked) 0xFFE0E0E0.toInt() else 0xFF000000.toInt())
                return view
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setBackgroundColor(if (themeToggle.isChecked) 0xFF2A2A2A.toInt() else 0xFFFFFFFF.toInt())
                view.setTextColor(if (themeToggle.isChecked) 0xFFE0E0E0.toInt() else 0xFF000000.toInt())
                return view
            }
        }
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelSpinner.adapter = spinnerAdapter
        modelSpinner.setSelection(0)

        modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = models[position]
                if (selected == "None") {
                    model?.close(); model = null
                    speechService?.stop(); speechService = null
                    statusView.text = getString(R.string.no_model_selected)
                } else {
                    loadModel(modelMap[selected]!!)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Microphone toggle
        micToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) startDetection() else stopDetection()
        }

        // Pause toggle
        pauseToggle.setOnCheckedChangeListener { _, isChecked ->
            speechService?.setPause(isChecked)
            statusView.text = if (isChecked) getString(R.string.detection_paused) else getString(R.string.detection_resumed)
        }

        // Theme toggle: dark/light
        themeToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                window.decorView.setBackgroundColor(0xFF1E1E1E.toInt())
                statusView.setBackgroundColor(0xFF2C2C2C.toInt())
                statusView.setTextColor(0xFFE0E0E0.toInt())
                partialView.setBackgroundColor(0xFF2A2A2A.toInt())
                partialView.setTextColor(0xFFE0E0E0.toInt())
                finalView.setBackgroundColor(0xFF2A2A2A.toInt())
                finalView.setTextColor(0xFFE0E0E0.toInt())

                modelLabel.setTextColor(0xFFFFFFFF.toInt())
                statusLabel.setTextColor(0xFFFFFFFF.toInt())
                partialLabel.setTextColor(0xFFFFFFFF.toInt())
                finalLabel.setTextColor(0xFFFFFFFF.toInt())
            } else {
                window.decorView.setBackgroundColor(0xFFFFFFFF.toInt())
                statusView.setBackgroundColor(0xFFCCCCFF.toInt())
                statusView.setTextColor(0xFF000000.toInt())
                partialView.setBackgroundColor(0xFFEEEEEE.toInt())
                partialView.setTextColor(0xFF000000.toInt())
                finalView.setBackgroundColor(0xFFDDFFDD.toInt())
                finalView.setTextColor(0xFF000000.toInt())

                modelLabel.setTextColor(0xFF000000.toInt())
                statusLabel.setTextColor(0xFF000000.toInt())
                partialLabel.setTextColor(0xFF000000.toInt())
                finalLabel.setTextColor(0xFF000000.toInt())
            }
            spinnerAdapter.notifyDataSetChanged()
        }

        // Request audio permission
        val permissionCheck = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO),
                com.example.e_sentinel.MainActivity.Companion.PERMISSIONS_REQUEST_RECORD_AUDIO
            )
        } else {
            statusView.text = getString(R.string.waiting_permission)
        }




        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    // Calculate acceleration magnitude
                    val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()

                    if (acceleration > fallThreshold && !fallDetected) {
                        fallDetected = true

                        runOnUiThread {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Fall Detected")
                                .setMessage("It looks like you may have fallen. Do you need help?")
                                .setPositiveButton("Yes, Send SOS") { _, _ ->
                                    getLocation { lat, lon ->
                                        sendSOS(lat, lon)
                                        Toast.makeText(
                                            this@MainActivity,
                                            "🚨 SOS Triggered due to Fall!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                .setNegativeButton("No, I'm Fine") { _, _ ->
                                    fallDetected = false
                                }
                                .setCancelable(false)
                                .show()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)




//        // SOS Button -> Send SMS via Twilio
//        val btnSOS: Button = findViewById(R.id.btnSOS)
//        btnSOS.setOnClickListener {
//            val phoneNumber = "phone_number"  // replace with target number
//            val latitude = location.latitude
//            val longitude = location.longitude
//            val mapsLink = "https://www.google.com/maps?q=$latitude,$longitude"
//            val message = "🚨 SOS Alert! Please help me! My live location: $mapsLink"
//            val intent = Intent(Intent.ACTION_VIEW)
//            intent.data = Uri.parse(
//                "https://wa.me/${phoneNumber.replace("+", "")}?text=${Uri.encode(message)}"
//            )
//
//            try {
//                startActivity(intent)
//            } catch (e: Exception) {
//                Toast.makeText(this, "WhatsApp not installed!", Toast.LENGTH_SHORT).show()
//            }
//        }


        val btnSOS: Button = findViewById(R.id.btnSOS)
        btnSOS.setOnClickListener {
            val options = arrayOf("Send via SMS (Twilio)", "Send via WhatsApp (Twilio)")

            AlertDialog.Builder(this)
                .setTitle("Choose SOS Option")
                .setItems(options) { _, which ->
                    getLocation { lat, lon ->
                        val mapsLink = "https://www.google.com/maps?q=$lat,$lon"
                        val message = "🚨 SOS Alert! Please help me! My live location: $mapsLink"

                        when (which) {
                            // 1️⃣ SMS via Twilio
                            0 -> {
                                Thread {
                                    try {
                                        val sid = BuildConfig.TWILIO_ACCOUNT_SID
                                        val auth = BuildConfig.TWILIO_AUTH_TOKEN
                                        val fromPhone = "phone_number"   // Twilio SMS number
                                        val toPhone = "phone_number" // target number (your real phone)

                                        val formBody = FormBody.Builder()
                                            .add("From", fromPhone)
                                            .add("To", toPhone)
                                            .add("Body", message)
                                            .build()

                                        val request = Request.Builder()
                                            .url("https://api.twilio.com/2010-04-01/Accounts/$sid/Messages.json")
                                            .post(formBody)
                                            .header("Authorization", Credentials.basic(sid, auth))
                                            .build()

                                        val response = OkHttpClient().newCall(request).execute()
                                        runOnUiThread {
                                            if (response.isSuccessful) {
                                                Toast.makeText(this, "SOS Sent via SMS!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(this, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        runOnUiThread {
                                            Toast.makeText(this, "Error sending SMS", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }.start()
                            }

                            // 2️⃣ WhatsApp via Twilio
                            1 -> {
                                Thread {
                                    try {
                                        val sid = BuildConfig.TWILIO_ACCOUNT_SID
                                        val auth = BuildConfig.TWILIO_AUTH_TOKEN
                                        val fromWhatsapp = "whatsapp:phone_number"  // Twilio WhatsApp number
                                        val toWhatsapp = "whatsapp:phone_number"   // target WhatsApp number

                                        val formBody = FormBody.Builder()
                                            .add("From", fromWhatsapp)
                                            .add("To", toWhatsapp)
                                            .add("Body", message)
                                            .build()

                                        val request = Request.Builder()
                                            .url("https://api.twilio.com/2010-04-01/Accounts/$sid/Messages.json")
                                            .post(formBody)
                                            .header("Authorization", Credentials.basic(sid, auth))
                                            .build()

                                        val response = OkHttpClient().newCall(request).execute()
                                        runOnUiThread {
                                            if (response.isSuccessful) {
                                                Toast.makeText(this, "SOS Sent via WhatsApp!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(this, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        runOnUiThread {
                                            Toast.makeText(this, "Error sending WhatsApp", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }.start()
                            }
                        }
                    }
                }
                .show()
        }

        // Maps Button -> Open Google Maps
        val btnMaps: Button = findViewById(R.id.btnMaps)
        btnMaps.setOnClickListener {
            val gmmIntentUri = Uri.parse("geo:0,0?q=hospital")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }

        // Roads Button -> Shortest Path via Directions API
        // Roads Button -> Optimal Route with traffic
        // Roads Button -> Optimal Route with traffic
        val btnRoads: Button = findViewById(R.id.btnRoads)
        btnRoads.setOnClickListener {
            getLocation { lat, lon ->
                // Ask user for destination
                val input = EditText(this)
                AlertDialog.Builder(this)
                    .setTitle("Enter Destination Address")
                    .setView(input)
                    .setPositiveButton("Go") { _, _ ->
                        val destination = input.text.toString().trim()
                        if (destination.isNotEmpty()) {
                            Thread {
                                try {
                                    val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
                                    // Directions API URL with traffic info
                                    val url =
                                        "https://maps.googleapis.com/maps/api/directions/json?" +
                                                "origin=$lat,$lon&destination=${Uri.encode(destination)}" +
                                                "&mode=driving&departure_time=now&traffic_model=best_guess&key=$apiKey"

                                    val request = Request.Builder().url(url).build()
                                    val response = OkHttpClient().newCall(request).execute()
                                    val json = JSONObject(response.body?.string() ?: "{}")

                                    val routes = json.optJSONArray("routes")
                                    if (routes != null && routes.length() > 0) {
                                        val legs = routes.getJSONObject(0).getJSONArray("legs")
                                        val leg = legs.getJSONObject(0)
                                        val distance = leg.getJSONObject("distance").getString("text")
                                        val duration = leg.optJSONObject("duration_in_traffic")?.getString("text")
                                            ?: leg.getJSONObject("duration").getString("text")
                                        val startAddress = leg.getString("start_address")
                                        val endAddress = leg.getString("end_address")

                                        runOnUiThread {
                                            AlertDialog.Builder(this)
                                                .setTitle("Optimal Route")
                                                .setMessage(
                                                    "From: $startAddress\nTo: $endAddress\nDistance: $distance\nEstimated Time: $duration"
                                                )
                                                .setPositiveButton("Open in Maps") { _, _ ->
                                                    val gmmIntentUri =
                                                        Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$lat,$lon&destination=${Uri.encode(destination)}&travelmode=driving")
                                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                                    mapIntent.setPackage("com.google.android.apps.maps")
                                                    startActivity(mapIntent)
                                                }
                                                .setNegativeButton("Close", null)
                                                .show()
                                        }
                                    } else {
                                        runOnUiThread {
                                            Toast.makeText(this, "No route found", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    runOnUiThread {
                                        Toast.makeText(this, "Error fetching route", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.start()
                        } else {
                            Toast.makeText(this, "Destination cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        // News Button -> Local News
        val btnNews: Button = findViewById(R.id.btnNews)
        btnNews.setOnClickListener {
            getLocation { lat, lon ->
                // Step 1: Get nearest city name
                Thread {
                    try {
                        val geoApiKey = BuildConfig.GOOGLE_MAPS_API_KEY
                        val geoUrl = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lon&key=$geoApiKey"

                        val geoRequest = Request.Builder().url(geoUrl).build()
                        val geoResponse = OkHttpClient().newCall(geoRequest).execute()
                        val geoJson = JSONObject(geoResponse.body?.string() ?: "{}")
                        val results = geoJson.optJSONArray("results")

                        var cityName = "Vellore" // fallback
                        if (results != null && results.length() > 0) {
                            val addressComponents = results.getJSONObject(0).getJSONArray("address_components")
                            for (i in 0 until addressComponents.length()) {
                                val component = addressComponents.getJSONObject(i)
                                val types = component.getJSONArray("types")
                                for (j in 0 until types.length()) {
                                    if (types.getString(j) == "locality") {
                                        cityName = component.getString("long_name")
                                        break
                                    }
                                }
                            }
                        }

                        // Step 2: Fetch local news using NewsData API
                        val newsApiKey = BuildConfig.NEWS_API_KEY
                        val newsUrl = "https://newsdata.io/api/1/news?apikey=$newsApiKey&country=in&language=en&q=$cityName"

                        val newsRequest = Request.Builder().url(newsUrl).build()
                        val newsResponse = OkHttpClient().newCall(newsRequest).execute()
                        val newsJson = JSONObject(newsResponse.body?.string() ?: "{}")
                        val articles = newsJson.optJSONArray("results")

                        runOnUiThread {
                            if (articles != null && articles.length() > 0) {
                                val firstArticle = articles.getJSONObject(0)
                                val title = firstArticle.optString("title", "No Title")
                                val description = firstArticle.optString("description", "No Description Available")
                                val link = firstArticle.optString("link", "")

                                AlertDialog.Builder(this)
                                    .setTitle("Local News ($cityName)")
                                    .setMessage("$title\n\n$description")
                                    .setPositiveButton("Read More") { _, _ ->
                                        if (link.isNotEmpty()) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                            startActivity(intent)
                                        }
                                    }
                                    .setNegativeButton("Close", null)
                                    .show()
                            } else {
                                Toast.makeText(this, "No local news found for $cityName", Toast.LENGTH_SHORT).show()
                            }
                        }


                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this, "Error fetching news", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
        }

        val btnWeather: Button = findViewById(R.id.btnWeather)
        btnWeather.setOnClickListener {
            getLocation { lat, lon ->
                fetchLocalWeather(lat, lon)
            }
        }


        // Gemini Button -> Open Gemini
        val btnGemini: Button = findViewById(R.id.btnGemini)
        btnGemini.setOnClickListener {
            val uri = Uri.parse("https://gemini.google.com")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        // Twilio Button -> Open Twilio
//        val btnTwilio: Button = findViewById(R.id.btnTwilio)
//        btnTwilio.setOnClickListener {
//            val uri = Uri.parse("https://www.twilio.com")
//            startActivity(Intent(Intent.ACTION_VIEW, uri))
//        }
    }

    private fun fetchLocalWeather(latitude: Double, longitude: Double) {
        Thread {
            try {
                val apiKey = "45de9914eb33b41a0ff441d821a62629"  // Your OpenWeather API Key
                val url =
                    "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&units=metric&appid=$apiKey"

                val request = Request.Builder().url(url).build()
                val response = OkHttpClient().newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")

                val weatherArray = json.optJSONArray("weather")
                val mainObject = json.optJSONObject("main")

                val description = weatherArray?.getJSONObject(0)?.getString("description") ?: "N/A"
                val temp = mainObject?.getDouble("temp") ?: 0.0

                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("Local Weather")
                        .setMessage("🌡️ Temperature: $temp °C\n🌥️ Condition: $description")
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error fetching weather info", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(null as SensorEventListener?)
    }

    override fun onResume() {
        super.onResume()
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(null as SensorEventListener?, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val currentTime = System.currentTimeMillis()

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (currentTime - lastVolumeButtonTime <= DOUBLE_PRESS_INTERVAL) {
                lastVolumeButtonTime = 0L

                getLocation { lat, lon ->
                    sendSOS(lat, lon)
                }

                Toast.makeText(this, "SOS Activated by Volume Button!", Toast.LENGTH_SHORT).show()
            } else {
                lastVolumeButtonTime = currentTime
            }

            return true
        }

        return super.onKeyDown(keyCode, event)
    }



    // -------------------- FUNCTIONS --------------------
    private fun loadModel(modelName: String) {
        speechService?.let { it.stop(); it.shutdown(); speechService = null }
        model?.close(); model = null
        System.gc()
        statusView.text = getString(R.string.loading_model, modelName)
        partialView.text = ""; finalView.text = ""

        StorageService.unpack(
            this, modelName, "model",
            { unpackedModel -> model = unpackedModel; statusView.text = getString(R.string.model_loaded, modelName) },
            { exception -> statusView.text = getString(R.string.failed_load_model, exception.message) }
        )
    }

    private fun startDetection() {
        if (model == null) { statusView.text = getString(R.string.select_model_first); micToggle.isChecked = false; return }
        try {
            val rec = Recognizer(model, 16000.0f)
            speechService = SpeechService(rec, 16000.0f).apply { startListening(this@MainActivity) }
            statusView.text = getString(R.string.listening)
        } catch (e: IOException) {
            statusView.text = getString(R.string.mic_error, e.message)
            micToggle.isChecked = false
        }
    }

    private fun stopDetection() {
        speechService?.stop();
        speechService = null;
        statusView.text = getString(R.string.detection_stopped)
    }

    override fun onPartialResult(hypothesis: String) {
        runOnUiThread {
            partialView.text = try {
                JSONObject(hypothesis).optString("partial")
            } catch (e: Exception) {
                hypothesis
            }
        }
    }

    override fun onResult(hypothesis: String) {
        runOnUiThread {
            try {
                val text = JSONObject(hypothesis).optString("text");
                if (text.isNotEmpty()) {

                    finalView.text = text;
                    val spokenText = text.lowercase()

                    val distressDetected = distressKeywords.any { keyword ->
                        spokenText.contains(keyword)
                    }

                    if (distressDetected) {
                        runOnUiThread {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Voice-Based SOS Detected")
                                .setMessage("You said something that sounds like a distress call. Should I send an SOS alert?")
                                .setPositiveButton("Yes, Send SOS") { _, _ ->
                                    getLocation { lat, lon ->
                                        sendSOS(lat, lon)
                                        Toast.makeText(
                                            this@MainActivity,
                                            " SOS Triggered via Voice Command!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                .setNegativeButton("No, I'm Fine") { _, _ ->
                                    fallDetected = false
                                }
                                .setCancelable(false)
                                .show()
                        }
                    }
                }

            } catch (e: Exception) {
                finalView.text = hypothesis;
            }
        }
    }

    override fun onFinalResult(hypothesis: String) {
        runOnUiThread {
            try {
                val text = JSONObject(hypothesis).optString("text");
                if (text.isNotEmpty()){
                    finalView.text = text;
                }

                partialView.text = ""
            }
            catch (e: Exception) {
                finalView.text = hypothesis;
                partialView.text = ""
            }
        }
    }



    override fun onError(e: Exception) { runOnUiThread { statusView.text = getString(R.string.error, e.message) } }
    override fun onTimeout() {}

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == com.example.e_sentinel.MainActivity.Companion.PERMISSIONS_REQUEST_RECORD_AUDIO && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            statusView.text = getString(R.string.permission_granted)
        } else finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechService?.let { it.stop(); it.shutdown() }
    }

    private fun getLocation(callback: (Double, Double) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    callback(it.latitude, it.longitude)
                } ?: Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendSOS(latitude: Double, longitude: Double) {
        Thread {
            try {
                val sid = BuildConfig.TWILIO_ACCOUNT_SID
                val auth = BuildConfig.TWILIO_AUTH_TOKEN
                val fromPhone = "phone_number" // Replace with your Twilio number
                val toPhone = "phone_number" // Replace with destination number
                val message =
                    "🚨 SOS Alert! Location: https://maps.google.com/?q=$latitude,$longitude"

                val formBody = FormBody.Builder()
                    .add("From", fromPhone)
                    .add("To", toPhone)
                    .add("Body", message)
                    .build()

                val request = Request.Builder()
                    .url("https://api.twilio.com/2010-04-01/Accounts/$sid/Messages.json")
                    .post(formBody)
                    .header("Authorization", Credentials.basic(sid, auth))
                    .build()

                val response = client.newCall(request).execute()
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this, "SOS Sent!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Error: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error sending SOS", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun fetchLocalNews(latitude: Double, longitude: Double) {
        Thread {
            try {
                val apiKey = BuildConfig.NEWS_API_KEY
                val url =
                    "https://newsdata.io/api/1/news?apikey=$apiKey&country=in&language=en&lat=$latitude&lon=$longitude"

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")

                val articles = json.optJSONArray("results")
                runOnUiThread {
                    if (articles != null && articles.length() > 0) {
                        val firstNews = articles.getJSONObject(0).getString("title")
                        Toast.makeText(this, "Latest News: $firstNews", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "No local news found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error fetching news", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }


    private fun fetchShortestPath(latitude: Double, longitude: Double, destination: String) {
        Thread {
            try {
                val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
                val url =
                    "https://maps.googleapis.com/maps/api/directions/json?origin=$latitude,$longitude&destination=$destination&mode=driving&key=$apiKey"

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")

                val routes = json.optJSONArray("routes")
                if (routes != null && routes.length() > 0) {
                    val legs = routes.getJSONObject(0).getJSONArray("legs")
                    val distance = legs.getJSONObject(0).getJSONObject("distance").getString("text")
                    val duration = legs.getJSONObject(0).getJSONObject("duration").getString("text")
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Shortest Path: $distance ($duration)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "No route found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error fetching path", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
