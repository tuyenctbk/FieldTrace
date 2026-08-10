# Privacy Policy for FieldTrace

**Effective Date:** July 31, 2026  
**Last Updated:** July 31, 2026  

FieldTrace ("we," "our," or "us") is committed to protecting your privacy. This Privacy Policy describes how FieldTrace collects, uses, and discloses information when you use our mobile application (the "App").

---

## 1. Information We Collect and Process

FieldTrace is designed to operate primarily on-device using your smartphone's built-in sensors. We collect and process the following types of data:

### A. Sensor & Technical Data
* **Magnetometer Data:** Raw 3-axis magnetic field values ($\text{EMF}_X, \text{EMF}_Y, \text{EMF}_Z$) measured in Microteslas ($\mu\text{T}$) to compute real-time magnetic flux and render 2D spatial heatmaps.
* **Location Data (GPS):** Precise and coarse geographic coordinates (latitude and longitude) are accessed **only when you record a spatial trace session** to tag coordinate nodes on your local heatmaps. Location data is processed strictly on your device and stored in your local Room database.
* **Device Hardware Information:** Device model, operating system version, and sensor hardware capabilities to calibrate magnetometer sample rates and baseline offsets.

### B. Analytical & Diagnostic Data (Non-PII)
* **Crash Reports & Performance Metrics:** We utilize **Firebase Crashlytics** and **Firebase Analytics** to collect anonymous diagnostic logs, crash trace stacks, and aggregate usage statistics (e.g., screen visits, feature adoption). This information contains **no personally identifiable information (PII)** and is strictly used to resolve technical bugs and improve application performance.

---

## 2. How We Use Your Information

We use the collected information for the following purposes:
* To provide real-time electromagnetic field (EMF) scanning, audio sonification synthesis, and 2D spatial heatmap rendering.
* To allow you to record, inspect, and export spatial trace logs locally in CSV, JSON, and GPX formats.
* To perform 3D magnetometer figure-eight baseline calibration.
* To diagnose app crashes, technical bugs, and optimize app responsiveness.

---

## 3. Data Storage & Security

* **100% Local Storage:** All recorded spatial field traces, diagnostic notes, and custom session logs are stored locally on your device in an encrypted Room SQLite database.
* **No Cloud Uploads of Personal Records:** Your recorded heatmaps, trace points, and custom notes are **never uploaded to external servers** or sold to third parties.
* **User Data Erasure:** You retain full ownership and control of your data. You can delete individual trace recordings or clear your entire history at any time using the "Purge" button inside the Spatial Archive Vault tab.

---

## 4. Permissions Requested

FieldTrace requests the following permissions solely to enable core functionality:
* `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: Required to attach GPS latitude and longitude coordinates to spatial heatmaps during active scan sessions.
* `VIBRATE`: Required to trigger tactile haptic alerts when magnetic flux exceeds safety thresholds.

You may revoke these permissions at any time via your Android device settings, though revoking location access will disable spatial heatmap coordinate tagging.

---

## 5. Children's Privacy

FieldTrace does not knowingly collect or solicit personal information from children under the age of 13. The application does not require user account registration, login credentials, or personal identity submission.

---

## 6. Third-Party Services

The App integrates the following trusted third-party services:
* **Google Play Services:** Used for location services and app licensing.
* **Firebase Analytics & Crashlytics:** Used for anonymized crash reporting and usage telemetry.

For more information on Google's privacy practices, please visit the [Google Privacy Policy](https://policies.google.com/privacy).

---

## 7. Changes to This Privacy Policy

We may update our Privacy Policy from time to time. Any changes will be posted on this page with an updated "Effective Date." We encourage you to review this Privacy Policy periodically.

---

## 8. Contact Us

If you have any questions or suggestions regarding this Privacy Policy, please contact us at:

* **Email:** support@soloprono.com / tuyenctbk@gmail.com  
* **Application Name:** FieldTrace  
* **Package Name:** `com.soloprono.fieldtrace`
