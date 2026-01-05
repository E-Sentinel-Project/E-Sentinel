# E-Sentinel
**SOS Emergency Alert Application**

E-Sentinel is an SOS emergency alert application designed to respond to voice commands during critical situations such as accidents, war zones, or natural disasters. It works **entirely offline** for voice recognition using local language models, while still providing real-time location tracking and automated WhatsApp/SMS alerts.

---

## Table of Contents

- [Key Features](#key-features)
- [Setup & Installation](#setup--installation)
- [How It Works](#how-it-works)
- [Sample Code Snippets Analysis](#sample-code-snippets-analysis)
    - [Neutrosophic Fall Detection](#neutrosophic-fall-detection)
    - [Motion Feature Extraction](#motion-feature-extraction)
    - [Keyword-Based SOS Trigger](#keyword-based-sos-trigger)
    - [SOS Alert Dispatch](#sos-alert-dispatch)
    - [Volume Button–Based SOS Trigger](#volume-buttonbased-sos-trigger)
    - [Medical Emergency Navigation](#medical-emergency-navigation)
    - [Traffic-Aware Route Calculation](#traffic-aware-route-calculation)
    - [Location-Based News Aggregation](#location-based-news-aggregation)
    - [Local Weather Monitoring](#local-weather-monitoring)
- [Tech Stack](#tech-stack)
- [Pending Works](#pending-works)
- [Contributors](#contributors)

---
<a id="key-features"></a>
# Key Features

- **Offline Voice Command Recognition**  
  Detects emergency phrases like *“call doctor”*, *“help me”*, or *“emergency situation”* using **local Vosk speech models**—no internet required for speech recognition.

- **Real-time Location Tracking**  
  Finds and maps the nearest hospitals or safe zones using Google Maps APIs.

- **Twilio WhatsApp & SMS Integration**  
  Sends instant alerts containing the user’s live location to emergency contacts.

- **Multiple SOS Triggers**
    - Voice command
    - Fall detection
    - Double-tap (volume button)
    - Manual SOS button

- **Fall Detection System**  
  Uses the phone’s accelerometer to detect rapid acceleration followed by sudden inactivity.

- **Multilingual Support**  
  Recognizes multiple languages — English, Hindi, and Telugu — through offline models.

- **Customizable Emergency Contacts**  
  **Update your mobile numbers** in the app before use to ensure alerts reach the correct people.

---

<a id="setup-installation"></a>

# Setup & Installation

### 1. Clone the Repository

```bash
git clone https://github.com/E-Sentinel-Project/E-Sentinel.git
cd E-Sentinel
```

---

### 2. Update API Keys & Phone Numbers

Open the **app-level Gradle file**:

```
app/build.gradle.kts
```

Locate the `buildConfigField` entries and replace the placeholder values with your actual **API keys** and **phone numbers**:

```kotlin
buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"YOUR_API_KEY\"")
buildConfigField("String", "GOOGLE_ROADS_API_KEY", "\"YOUR_API_KEY\"")
buildConfigField("String", "NEWS_API_KEY", "\"YOUR_API_KEY\"")
buildConfigField("String", "GEMINI_API_KEY", "\"YOUR_API_KEY\"")
buildConfigField("String", "TWILIO_ACCOUNT_SID", "\"YOUR_API_KEY\"")
buildConfigField("String", "TWILIO_AUTH_TOKEN", "\"YOUR_API_KEY\"")
buildConfigField("String", "OPEN_WEATHER_MAP_API_KEY", "\"YOUR_API_KEY\"")

buildConfigField("String", "TWILIO_PHONE_NUMBER", "\"YOUR_TWILIO_PHONE_NUMBER\"")
buildConfigField("String", "ALERT_PHONE_NUMBER", "\"YOUR_ALERT_PHONE_NUMBER\"")
```

**Important Notes:**

* Replace all `YOUR_API_KEY` values with valid keys from the respective services.
* Update `TWILIO_PHONE_NUMBER` with your Twilio-registered number.
* Update `ALERT_PHONE_NUMBER` with the emergency contact number that should receive alerts.
* These values are compiled into `BuildConfig` and accessed securely at runtime.

---

### 3. Add Offline Voice Models

Download Vosk models from:
[https://alphacephei.com/vosk/models](https://alphacephei.com/vosk/models)

Extract the models into:

```
models/src/main/assets/
```

**Example:**

```
models/src/main/assets/model-small-en-in
```

---

### 4. Map the Model in `MainActivity.kt`

```kotlin
private val modelMap = mapOf(
    "English India (Small)" to "model-small-en-in",
    "Hindi (Small)" to "model-small-hi",
    "Telugu (Small)" to "model-small-te"
)
```

**Note:**
These voice models run **entirely offline on the device**, ensuring reliable emergency voice recognition even without internet connectivity.

---
<a id="how-it-works"></a>
# How It Works

1. User triggers SOS by:

    * Saying a distress phrase
    * Falling (detected via accelerometer)
    * Pressing volume buttons twice
    * Pressing the SOS button

2. App fetches user location via `FusedLocationProviderClient`.

3. Alert is sent via Twilio WhatsApp or SMS API with:

    * User’s live location
    * Preset emergency message
    * **Custom emergency contacts** (update mobile numbers in the code before use)

4. Optional features:

    * Shortest route to nearest hospital
    * Local news and weather data

---

<a id="sample-code-snippets-analysis"></a>
# Sample Code Snippets Analysis

This section presents representative code snippets that demonstrate how the core **safety-critical logic** of the system operates.

The **complete project source code** (including UI, networking, and platform-specific components) is available in the GitHub repository.
The examples shown here are intentionally simplified to improve readability and focus on decision logic rather than concurrency, lifecycle management, or error handling.

---
<a id="neutrosophic-fall-detection"></a>
## Neutrosophic Fall Detection

The neutrosophic fall detection logic is implemented as a **standalone, offline routine** that processes real-time accelerometer data.
It does **not require model training**, runs continuously on mobile devices, and is optimized for **low power consumption**.

The algorithm uses three compact motion features:

* **Filtered acceleration magnitude**
* **Jerk (rate of change of acceleration)**
* **Orientation angle**

These features are mapped to neutrosophic values:

* **Truth (T)** – evidence supporting a fall
* **Indeterminacy (I)** – uncertainty from ambiguous motion
* **Falsity (F)** – evidence supporting non-fall activity

### Feature Normalization Ranges

| Feature                | Range      | Purpose                 |
| ---------------------- | ---------- | ----------------------- |
| Acceleration magnitude | 15–30 m/s² | Detect impact impulse   |
| Jerk                   | 10–35 m/s³ | Detect abrupt motion    |
| Orientation angle      | 35°–85°    | Detect abnormal posture |

These thresholds were derived empirically through pilot testing on fall and non-fall activities.

### Design Characteristics

* **Soft decision boundaries** (not hard thresholds)
* Weighted evidence combination:

    * Acceleration: **0.55**
    * Jerk: **0.30**
    * Orientation: **0.15**
* Indeterminacy capped at **0.30**
* Falsity enforced at a minimum of **0.10**
* Fully **configurable parameters**

A fall is suspected only when **Truth significantly exceeds both Indeterminacy and Falsity**, reducing false alarms.

### Core Detection Logic

```kotlin
private fun detectFallNeutrosophic(
    filteredAccel: Float,
    jerk: Float,
    angle: Float
): Boolean {

    // Truth (T): evidence supporting a fall
    val T = (
            normalize(filteredAccel, 15f, 30f) * 0.55f +
                    normalize(jerk, 10f, 35f) * 0.30f +
                    normalize(angle, 35f, 85f) * 0.15f
            ).coerceIn(0f, 1f)

    // Indeterminacy (I): uncertainty from ambiguous motion
    val I = (
            normalize(jerk, 3f, 12f) * 0.4f +
                    normalize(angle, 10f, 35f) * 0.6f
            ).coerceIn(0f, 0.3f)

    // Falsity (F): strong evidence of non-fall activity
    val F = (
            normalizeStableGravity(filteredAccel) * 0.6f +
                    normalizeSmallJerk(jerk) * 0.4f
            ).coerceIn(0.1f, 1f)

    // Fall-suspected decision
    return (!fallDetected && (T - F > I) && T > 0.6f)
}
```

---
<a id="motion-feature-extraction"></a>
## Motion Feature Extraction

Raw accelerometer data is transformed into **compact motion descriptors** using a lightweight preprocessing pipeline:

* **Kalman-filtered acceleration magnitude**
* **Jerk (finite difference with timestamps)**
* **Orientation angle from gravity vector**

This module is designed for **real-time execution** with minimal computational overhead.

```kotlin
// Previous accelerometer state
private var lastAcc = FloatArray(3) { 0f }
private var lastTs = 0L
private val kFilter = Kalman1D()
private fun extractMotionFeatures(event: SensorEvent): FloatArray {
    val x = event.values[0]
    val y = event.values[1]
    val z = event.values[2]

    val rawAccel = Math.sqrt( // Raw acceleration magnitude
        (x * x + y * y + z * z).toDouble()
    ).toFloat()
    // Kalman-filtered acceleration
    val filteredAccel = kFilter.update(rawAccel)

    val timestamp = event.timestamp // Time difference
    val dt = if (lastTs == 0L) 0.0 else
        (timestamp - lastTs) / 1_000_000_000.0
    // Jerk computation
    val jerk = if (dt > 0) {
        val diff = Math.sqrt(
            ((x - lastAcc[0]) * (x - lastAcc[0]) +
                    (y - lastAcc[1]) * (y - lastAcc[1]) +
                    (z - lastAcc[2]) * (z - lastAcc[2])).toDouble()
        ).toFloat()
        diff / dt.toFloat()
    } else 0f

    val angle = Math.toDegrees( // Orientation angle
        Math.acos((z / filteredAccel).coerceIn(-1f, 1f).toDouble())
    ).toFloat()

    // Update state
    lastTs = timestamp
    lastAcc[0] = x; lastAcc[1] = y; lastAcc[2] = z

    return floatArrayOf(filteredAccel, jerk, angle)
}
```

---
<a id="keyword-based-sos-trigger"></a>
## Keyword-Based SOS Trigger

The system includes an **offline, speech-driven SOS mechanism** using on-device speech recognition.

### Key Characteristics

* Works without internet connectivity
* Lightweight keyword matching
* Human-in-the-loop confirmation
* Tolerant to recognition noise

### Distress Keyword Registry

```kotlin
val distressKeywords = listOf(
    "help", "help me", "save me", "emergency", "please help",
    "i need help", "i'm in danger", "i am in danger",
    "call the police", "sos", "attack", "kidnap",
    "rape", "fire", "someone is following me",
    "danger", "please rescue me"
)
```

### Speech Recognition Callback

```kotlin
// Vosk Listener Callback
override fun onResult(hypothesis: String) {
    Thread{ // run on a separate thread
        val text = JSONObject(hypothesis).optString("text");
        if (text.isNotEmpty()) {
            finalView.text = text; // update ui text-view
            val spokenText = text.lowercase()

            // check for match
            val distressDetected =
                distressKeywords.any { keyword ->
                    spokenText.contains(keyword)
                }

            if (distressDetected) {
                // Show a pop-up for confirmation
            }
        }
    }.start()}
```

---
<a id="sos-alert-dispatch"></a>
## SOS Alert Dispatch

Once an emergency is confirmed, the system sends an SOS alert containing a **Google Maps location link** using the **Twilio SMS API**.

### Design Features

* HTTPS-based secure communication
* Background-thread execution
* Modular dispatch independent of trigger source
* Immediate user feedback

```kotlin
private fun sendSOS(latitude: Double, longitude: Double) {
    Thread{ // run on a separate thread
        // load credentials and phone numbers
        val sid = BuildConfig.TWILIO_ACCOUNT_SID
        val auth = BuildConfig.TWILIO_AUTH_TOKEN
        val fromPhone = "phone_number"
        val toPhone = "phone_number"
        val message =
            "SOS Alert! Location: https://maps.google.com/?q=$latitude,$longitude"

        // build the message structure
        val formBody = FormBody.Builder()
            .add("From", fromPhone)
            .add("To", toPhone)
            .add("Body", message)
            .build()

        // build an authenticated request to Twilio
        val request = Request.Builder()
            .url(
                "https://api.twilio.com/2010-04-01/Accounts/$sid/Messages.json"
            )
            .post(formBody)
            .header("Authorization", Credentials.basic(sid, auth))
            .build()

        // send the sos message
        val response = client.newCall(request).execute()

        // wait for response on a separate thread
        runOnUiThread {
            if (response.isSuccessful) { // message sent
                Toast.makeText(
                    this,
                    "SOS Sent!",
                    Toast.LENGTH_LONG
                ).show()
            } else { // message not sent
                Toast.makeText(
                    this,
                    "Error: ${response.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }.start()}
```

---
<a id="volume-button-sos-trigger"></a>
## Volume Button–Based SOS Trigger

This feature lets the user trigger an SOS alert by **double-pressing either the Volume Up or Volume Down button**. It is designed for situations where touching the screen or using voice commands is not possible.

The app listens for volume button key events and checks the time between two presses. If the second press happens within a fixed interval, it is treated as an intentional SOS action. This helps avoid accidental triggers during normal volume adjustment.

When the SOS is activated, the app fetches the user’s current location and sends it using the existing SOS dispatch logic. The key event is consumed so the system volume does not change, and a short on-screen message confirms that the SOS was triggered.

---

### Configuration Parameters

```kotlin
private var lastVolumeButtonTime = 0L
private const val DOUBLE_PRESS_INTERVAL = 600L // milliseconds
```

| Parameter               | Description                            |
| ----------------------- | -------------------------------------- |
| `DOUBLE_PRESS_INTERVAL` | Maximum allowed time between presses   |
| `lastVolumeButtonTime`  | Tracks timing of previous button press |

> The interval can be adjusted to balance **responsiveness vs. false triggers**.

---

```kotlin
override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    val currentTime = System.currentTimeMillis()

    // Check for volume button press
    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
        keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {

        // Detect double press within the defined interval
        if (currentTime - lastVolumeButtonTime <= DOUBLE_PRESS_INTERVAL) {

            // Reset timer to avoid repeated triggers
            lastVolumeButtonTime = 0L

            // Fetch current location and send SOS
            getLocation { lat, lon ->
                sendSOS(lat, lon)
            }

            // User feedback
            Toast.makeText(
                this,
                "SOS Activated by Volume Button!",
                Toast.LENGTH_SHORT
            ).show()

        } else {
            // Store timestamp of first press
            lastVolumeButtonTime = currentTime
        }

        // Consume the event so system volume doesn't change
        return true
    }

    return super.onKeyDown(keyCode, event)
}

```

---
<a id="medical-emergency-navigation"></a>
## Medical Emergency Navigation

This module launches **native map navigation** to nearby hospitals using a geo-URI.

```kotlin
//fetch the 'btnMaps' button from XML
val btnMaps: Button = findViewById(R.id.btnMaps)
btnMaps.setOnClickListener {
    // Construct URI (Uniform Resource Identifier) to search for nearby hospitals
    val gmmIntentUri = Uri.parse("geo:0,0?q=hospital")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

    // Open Google Maps with the requested URI
    mapIntent.setPackage("com.google.android.apps.maps")
    startActivity(mapIntent)
}
```

---
<a id="traffic-aware-route-calculation"></a>
## Traffic-Aware Route Calculation

Uses the **Google Directions API** with real-time traffic parameters:

* `departure_time=now`
* `traffic_model=best_guess`

Returns:

* Distance
* Travel duration (traffic-adjusted)
* Start and end addresses

```kotlin
// fetch the button from XML tree
val btnRoads: Button = findViewById(R.id.btnRoads)
btnRoads.setOnClickListener {
    // first fetch phone's current location
    getLocation { lat, lon ->
        Thread{ // run on a separate thread
            // create a pop-up and ask user for destination
            val input = EditText(this)
            val destination = input.text.toString().trim()
            // close pop-up and fetch optimal route

            if (destination.isNotEmpty()) {
                // load api key and api query link
                val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
                val url =
                    "https://maps.googleapis.com/maps/api/directions/json?" +
                            "origin=$lat,$lon&destination=${Uri.encode(destination)}" +
                            "&mode=driving&departure_time=now&traffic_model=best_guess&key=$apiKey"

                // perform the request to the api
                val request = Request.Builder().url(url).build()

                // query's response
                val response = OkHttpClient().newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "{}")

                // fetch array of routes
                val routes = json.optJSONArray("routes")

                // valid route found
                if (routes != null && routes.length() > 0) {
                    // A leg is a section of a route between waypoints.
                    // take the first route and take its first leg
                    val leg = routes.getJSONObject(0).getJSONArray("legs")
                        .getJSONObject(0)

                    // fetch the route's distance
                    val distance = leg.getJSONObject("distance").getString("text")

                    //fetch the duration
                    val duration = leg.optJSONObject("duration_in_traffic")
                        ?.getString("text")
                        ?: leg.getJSONObject("duration")
                            .getString("text")


                    // current location of the user
                    val startAddress = leg.getString("start_address")

                    // destination
                    val endAddress = leg.getString("end_address")

                    // create pop-up and display results

                } else { // no route found }
                } else { // invalid destination } 
                }.start()}
        }
```

---
<a id="location-based-news-aggregation"></a>
## Location-Based News Aggregation

Delivers **geographically relevant news** using:

1. Reverse geocoding (lat/lon → city)
2. Location-based news query

Includes background-thread execution.

```kotlin
// fetch the button from XML tree
val btnNews: Button = findViewById(R.id.btnNews)
btnNews.setOnClickListener {
    getLocation { lat, lon ->
        Thread{ // run on a separate thread
            // firstly, get nearest city name
            val geoApiKey = BuildConfig.GOOGLE_MAPS_API_KEY
            val geoUrl =
                "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lon&key=$geoApiKey"

            // send a request and get a response
            val geoRequest = Request.Builder().url(geoUrl).build()
            val geoResponse = OkHttpClient().newCall(geoRequest).execute()

            // parse the response to obtain results
            val geoJson = JSONObject(geoResponse.body?.string() ?: "{}")
            val results = geoJson.optJSONArray("results")

            var cityName: String? = null

            // recover the city name from the response
            if (results != null && results.length() > 0) {
                val addressComponents = results.getJSONObject(0)
                    .getJSONArray("address_components")
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

            // fetch local news using NewsData API using local city name info
            val newsApiKey = BuildConfig.NEWS_API_KEY
            val newsUrl =
                "https://newsdata.io/api/1/news?apikey=$newsApiKey&country=in&language=en&q=$cityName"

            // send a request and get a response
            val newsRequest = Request.Builder().url(newsUrl).build()
            val newsResponse = OkHttpClient().newCall(newsRequest).execute()
            val newsJson = JSONObject(newsResponse.body?.string() ?: "{}")
            val articles = newsJson.optJSONArray("results")

            if (articles != null && articles.length() > 0) {
                // parse response and obtain news
                val firstArticle = articles.getJSONObject(0)
                val title = firstArticle.optString(
                    "title",
                    "No Title"
                )
                val description = firstArticle.optString(
                    "description",
                    "No Description Available"
                )
                val link = firstArticle.optString("link", "")

                // display the news

            } else {
                // no news found
            }
        }.start()}
}
```

---
<a id="local-weather-monitoring"></a>
## Local Weather Monitoring

Fetches real-time weather using the user's **current geographic coordinates**.

```kotlin
val btnWeather: Button = findViewById(R.id.btnWeather)
btnWeather.setOnClickListener {
    // fetch the current location of user
    getLocation { lat, lon ->
        fetchLocalWeather(lat, lon)
    }
}

private fun fetchLocalWeather(latitude: Double, longitude: Double) {
    Thread{ // run on a separate thread
        // load credentials and links
        val apiKey = BuildConfig.OPEN_WEATHER_MAP_API_KEY
        val url =
            "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&units=metric&appid=$apiKey"

        // send a request and receive a response
        val request = Request.Builder().url(url).build()
        val response = OkHttpClient().newCall(request).execute()
        val json = JSONObject(response.body?.string() ?: "{}")

        val weatherArray = json.optJSONArray("weather")
        val mainObject = json.optJSONObject("main")

        val description = weatherArray
            ?.getJSONObject(0)
            ?.getString("description")
            ?: "N/A"

        val temp = mainObject?.getDouble("temp") ?: 0.0

        // display obtained weather info
    }.start()}
```

---
<a id="tech-stack"></a>
## Tech Stack

* **Android (Kotlin)**
* **Vosk Offline Speech Recognition**
* **Google Maps & Directions APIs**
* **Twilio API (SMS/WhatsApp)**
* **OpenWeather API**
* **NewsData.io API**

---
<a id="pending-works"></a>
## Pending Works
* Settings page to edit mobile numbers (For now the mobile numbers are hard-coded)
* Running the VOSK model as a background service


## Contributors
<a id="contributors"></a>
Developed by **Harsh Patel**, **B. Jaison Edward** and **Dr. Ilanthenral Kandasamy**.

For contributions or issues — please open a Pull Request or Issue.

---

**E-Sentinel** — *Emergency response that works offline and in real-time.*
