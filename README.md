
# E-Sentinel  
**SOS Emergency Alert Application**

E-Sentinel is an SOS emergency alert application designed to respond to voice commands during critical situations such as accidents, war zones, or natural disasters. It works **entirely offline** for voice recognition using local language models, while still providing real-time location tracking and automated WhatsApp/SMS alerts.

---

## Key Features

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

---

## Setup & Installation

### 1. Clone the Repository
```bash
git clone https://github.com/<your-username>/E-Sentinel.git
cd E-Sentinel
````

### 2. Add Offline Voice Models

Download Vosk models from [alphacephei.com/vosk/models](https://alphacephei.com/vosk/models) and extract them into:

```
models/src/main/assets/
```

Example:

```
models/src/main/assets/model-small-en-in
```

### 3. Map the Model in `MainActivity.kt`

```kotlin
private val modelMap = mapOf(
    "English India (Small)" to "model-small-en-in",
    "Hindi (Small)" to "model-small-hi",
    "Telugu (Small)" to "model-small-te"
)
```

**Note:** These models run **locally on the device**, ensuring offline functionality for emergency voice recognition.

---

## How It Works

1. User triggers SOS by:

   * Saying a distress phrase
   * Falling (detected via accelerometer)
   * Pressing volume buttons twice
   * Pressing the SOS button

2. App fetches user location via `FusedLocationProviderClient`.

3. Alert is sent via Twilio WhatsApp or SMS API with:

   * User’s live location
   * Preset emergency message

4. Optional features:

   * Shortest route to nearest hospital
   * Local news and weather data

---

## Tech Stack

* **Android (Kotlin)**
* **Vosk Offline Speech Recognition**
* **Google Maps & Directions APIs**
* **Twilio API (SMS/WhatsApp)**
* **OpenWeather API**
* **NewsData.io API**

---


## Contributors

Developed by **Harsh Patel** and **B. Jaison Edward**.
For contributions or issues — please open a Pull Request or Issue.

---

**E-Sentinel** — *Emergency response that works offline and in real-time.*

