#include <Wire.h>
#include <TimeLib.h>
#include <LittleFS.h>
#include "Config.h"
#include "MedicineStorage.h"
#include "DisplayManager.h"
#include "LidServo.h"
#include "StepperManager.h"
#include "ButtonManager.h"
#include "BuzzerManager.h"
#include "WiFiManager.h"
#include "RTCManager.h"

// Global objects
MedicineStorage medicineStorage;
DisplayManager display;
LidServo lidServo;
StepperManager stepper;
ButtonManager nextButton(NEXT_BUTTON);
ButtonManager lidButton(LID_BUTTON);
BuzzerManager buzzer(BUZZER_PIN);
WiFiManager wifi(WIFI_AP_SSID, WIFI_AP_PASSWORD);
RTCManager rtc;

// State variables
int currentContainer = 0;
bool lidOpen = false;
bool pendingHome = false;          // true after wrap‑around (needs homing)
bool alarmAcknowledged = false;
bool autoMovePending = false;      // true during auto‑move to due container
bool lidAutoOpened = false;        // true after lid opened automatically

// Time (local, updated from RTC)
int currentHour = 0;
int currentMinute = 0;

// Button long‑press detection for WiFi wake
unsigned long nextButtonPressStart = 0;
bool nextButtonPressed = false;

// Function prototypes
void updateDisplay();
void moveToNextContainer();
void toggleLid();
void acknowledgeAlarm();
void performHomeAfterWrap();
void onDataReceived(const JsonDocument& doc);
void saveMedicineData(const JsonDocument& doc);
void loadMedicineData();
String formatExpiry(uint64_t timestamp_ms);

void setup() {
    Serial.begin(115200);
    Wire.begin(21, 22);

    if (!LittleFS.begin(true)) {
        Serial.println("LittleFS mount failed");
    }

    if (!display.begin()) {
        while (1);
    }
    display.showStartupMessage();
    delay(1000);

    loadMedicineData();

    rtc.begin();
    if (!rtc.isRunning()) {
        Serial.println("RTC not running – will set on phone sync");
    }

    stepper.begin();
    lidServo.begin();
    nextButton.begin();
    lidButton.begin();
    buzzer.begin();
    buzzer.setMedicineStorage(&medicineStorage);

    wifi.setTimeoutMinutes(15);
    wifi.setDataCallback(onDataReceived);
    wifi.begin();

    stepper.home();
    updateDisplay();
}

void loop() {
    // Update non‑blocking modules
    stepper.update();
    lidServo.update();
    nextButton.update();
    lidButton.update();
    buzzer.update();
    wifi.loop();

    // ---- Read current time from RTC once per second ----
    static unsigned long lastRtcRead = 0;
    if (millis() - lastRtcRead >= 1000) {
        lastRtcRead = millis();
        currentHour = rtc.getHour();
        currentMinute = rtc.getMinute();
        updateDisplay();

        if (!alarmAcknowledged) {
            buzzer.checkScheduledAlarm(currentHour, currentMinute);
        }

        // Check for automatic rotation to due medicine (once per minute)
        static int lastAutoCheckMinute = -1;
        if (currentMinute != lastAutoCheckMinute) {
            lastAutoCheckMinute = currentMinute;
            if (!stepper.isMoving() && !pendingHome) {
                int duePillbox = medicineStorage.getDuePillbox(currentHour, currentMinute);
                if (duePillbox != 0) {
                    int targetContainer = duePillbox - 1;
                    if (targetContainer != currentContainer) {
                        stepper.moveToContainer(targetContainer, currentContainer, pendingHome);
                        autoMovePending = true;   // mark that we are moving for alarm
                        updateDisplay();
                    }
                }
            }
        }
    }

    // ---- After auto‑move completes, open the lid ----
    if (autoMovePending && !stepper.isMoving() && !pendingHome) {
        autoMovePending = false;        // movement done
        lidServo.open();
        lidOpen = true;
        lidAutoOpened = true;           // remember lid was opened automatically
        updateDisplay();
    }

    // ---- Button: Next (manual move + long press for WiFi wake) ----
    if (nextButton.wasPressed()) {
        // Short press: manual move
        if (lidAutoOpened) {
            lidAutoOpened = false;       // user manually moved, cancel auto‑open flag
        }
        stepper.moveToNextContainer(currentContainer, pendingHome);
        updateDisplay();

        // Reset WiFi timeout (client activity)
        wifi.handleClientActivity();
    }

    // Long press detection for WiFi wake
    if (nextButton.isPressed()) {
        if (!nextButtonPressed) {
            nextButtonPressed = true;
            nextButtonPressStart = millis();
        } else if (millis() - nextButtonPressStart >= 5000) {
            if (!wifi.isAPActive()) {
                Serial.println("Long press: waking WiFi");
                wifi.startAP();
                display.showMessage("WiFi ON", 1500);
            }
            nextButtonPressed = false;   // prevent repeated triggering
        }
    } else {
        nextButtonPressed = false;
    }

    // ---- Button: Lid ----
    if (lidButton.wasPressed()) {
        toggleLid();
        if (buzzer.isAlarming()) {
            acknowledgeAlarm();
        }
        // If lid was closed after an automatic open, just clear the flag (no homing)
        if (!lidOpen && lidAutoOpened) {
            lidAutoOpened = false;       // lid closed, stay at current container
        }

        // Reset WiFi timeout (client activity)
        wifi.handleClientActivity();
    }

    // ---- Homing after wrap‑around (from next button when at last container) ----
    if (pendingHome && !stepper.isMoving()) {
        performHomeAfterWrap();
    }

    // ---- Update display on alarm state change ----
    static bool lastAlarmState = false;
    if (buzzer.isAlarming() != lastAlarmState) {
        lastAlarmState = buzzer.isAlarming();
        updateDisplay();
    }
}

// ---------- Helper functions ----------

void moveToNextContainer() {
    stepper.moveToNextContainer(currentContainer, pendingHome);
    updateDisplay();
}

void toggleLid() {
    lidServo.toggle(lidOpen);
    updateDisplay();
}

void acknowledgeAlarm() {
    buzzer.stopAlarm();
    alarmAcknowledged = true;
}

void performHomeAfterWrap() {
    AccelStepper& s = stepper.getStepper();
    s.setSpeed(-200);
    while (digitalRead(LIMIT_SWITCH) == HIGH) {
        s.runSpeed();
        lidServo.update();
        buzzer.update();
    }
    s.stop();
    s.setCurrentPosition(0);
    pendingHome = false;
    stepper.resetTargetPosition();
    stepper.begin();
    s.disableOutputs();
    currentContainer = 0;          // update container index
    updateDisplay();
}

void updateDisplay() {
    const MedicineEntry* med = medicineStorage.getByPillbox(currentContainer + 1);
    String medName = med ? med->brandName : "Unknown";
    String exp = med ? (med->expiryDate > 0 ? formatExpiry(med->expiryDate) : "No expiry") : "No expiry";
    String intake = med ? med->intakeTime : "";
    display.update(currentContainer, lidOpen, medName, exp, intake,
                   buzzer.isAlarming(), currentHour, currentMinute);
}

String formatExpiry(uint64_t timestamp_ms) {
    time_t t = timestamp_ms / 1000;
    struct tm* tm_info = localtime(&t);
    char buffer[12];
    strftime(buffer, sizeof(buffer), "%b %Y", tm_info);
    return String(buffer);
}

// ---------- WiFi Data Callback ----------
void onDataReceived(const JsonDocument& doc) {
    Serial.println("Processing received data...");

    if (doc["unixTime"].is<long>()) {
        long unixTime = doc["unixTime"];
        unixTime += 8 * 3600;          // UTC+8
        rtc.setUnixTime(unixTime);
        // Immediately update local time variables
        currentHour = rtc.getHour();
        currentMinute = rtc.getMinute();
        Serial.printf("RTC set to local time: %02d:%02d\n", currentHour, currentMinute);
    }

    saveMedicineData(doc);

    medicineStorage.clear();
    JsonArrayConst medicines = doc["medicines"].as<JsonArrayConst>();
    for (JsonObjectConst obj : medicines) {
        MedicineEntry med;
        med.id = obj["id"] | 0;
        med.brandName = obj["brandName"] | "";
        med.genericName = obj["genericName"] | "";
        med.dosage = obj["dosage"] | "";
        med.quantity = obj["quantity"] | 0;
        med.expiryDate = obj["expiryDate"].as<unsigned long long>() | 0ULL;
        med.intakeTime = obj["intakeTime"] | "";
        med.remindersEnabled = obj["remindersEnabled"] | false;
        med.notes = obj["notes"] | "";
        med.pillboxNumber = obj["pillboxNumber"] | 0;
        med.timestamp = obj["timestamp"].as<unsigned long long>() | 0ULL;
        medicineStorage.addOrUpdate(med);
    }
    medicineStorage.printAll();
    updateDisplay();
}

// ---------- Persistent Storage Functions ----------
void saveMedicineData(const JsonDocument& doc) {
    File file = LittleFS.open("/medicines.json", FILE_WRITE);
    if (!file) {
        Serial.println("Failed to open medicines.json for writing");
        return;
    }
    if (serializeJson(doc, file) == 0) {
        Serial.println("Failed to write JSON to file");
    } else {
        Serial.println("Medicine data saved");
    }
    file.close();
}

void loadMedicineData() {
    if (!LittleFS.exists("/medicines.json")) {
        Serial.println("No saved medicine data");
        return;
    }
    File file = LittleFS.open("/medicines.json", FILE_READ);
    if (!file) {
        Serial.println("Failed to open medicines.json for reading");
        return;
    }
    String content = file.readString();
    file.close();

    JsonDocument doc;
    DeserializationError error = deserializeJson(doc, content);
    if (error) {
        Serial.print("Failed to parse saved JSON: ");
        Serial.println(error.c_str());
        return;
    }

    medicineStorage.clear();
    JsonArrayConst medicines = doc["medicines"].as<JsonArrayConst>();
    for (JsonObjectConst obj : medicines) {
        MedicineEntry med;
        med.id = obj["id"] | 0;
        med.brandName = obj["brandName"] | "";
        med.genericName = obj["genericName"] | "";
        med.dosage = obj["dosage"] | "";
        med.quantity = obj["quantity"] | 0;
        med.expiryDate = obj["expiryDate"].as<unsigned long long>() | 0ULL;
        med.intakeTime = obj["intakeTime"] | "";
        med.remindersEnabled = obj["remindersEnabled"] | false;
        med.notes = obj["notes"] | "";
        med.pillboxNumber = obj["pillboxNumber"] | 0;
        med.timestamp = obj["timestamp"].as<unsigned long long>() | 0ULL;
        medicineStorage.addOrUpdate(med);
    }
    Serial.println("Loaded saved medicine data");
    medicineStorage.printAll();
}