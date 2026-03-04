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

// Global objects
MedicineStorage medicineStorage;
DisplayManager display;
LidServo lidServo;
StepperManager stepper;
ButtonManager nextButton(NEXT_BUTTON);
ButtonManager lidButton(LID_BUTTON);
BuzzerManager buzzer(BUZZER_PIN);
WiFiManager wifi(WIFI_AP_SSID, WIFI_AP_PASSWORD);
#define TIMEZONE_OFFSET 28800  // 8 hours in seconds (UTC+8)
// State variables
int currentContainer = 0;
bool lidOpen = false;
bool pendingHome = false;
bool alarmAcknowledged = false;
bool autoMovePending = false;      // true when auto‑move started, cleared after stepper done and lid opened
bool lidAutoOpened = false;        // true after lid opened automatically (waiting for user to close)
bool pendingAutoHome = false;      // true when we need to home after lid close

// Time tracking
unsigned long lastTimeUpdate = 0;
int currentHour = 0;
int currentMinute = 0;

// Function prototypes
void updateDisplay();
void moveToNextContainer();
void toggleLid();
void acknowledgeAlarm();
void performHomeAfterWrap();
void updateTime();
void onDataReceived(const JsonDocument& doc);
void saveMedicineData(const JsonDocument& doc);
void loadMedicineData();
void saveTime(time_t t);
time_t loadTime();
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
    time_t savedTime = loadTime();
    if (savedTime > 0) {
        setTime(savedTime);
        Serial.println("Restored saved time");
    } else {
        setTime(8, 0, 0, 1, 1, 2024);
    }
    currentHour = hour();
    currentMinute = minute();

    stepper.begin();
    lidServo.begin();
    nextButton.begin();
    lidButton.begin();
    buzzer.begin();
    buzzer.setMedicineStorage(&medicineStorage);
    wifi.begin();
    wifi.setDataCallback(onDataReceived);

    stepper.home();
    updateDisplay();
}

void loop() {
    stepper.update();
    lidServo.update();
    nextButton.update();
    lidButton.update();
    buzzer.update();
    wifi.loop();

    // Time update every second
    if (millis() - lastTimeUpdate >= 1000) {
        lastTimeUpdate = millis();
        updateTime();
        updateDisplay();

        if (!alarmAcknowledged) {
            buzzer.checkScheduledAlarm(currentHour, currentMinute);
        }

        // Check for automatic rotation once per minute
        static int lastAutoCheckMinute = -1;
        if (currentMinute != lastAutoCheckMinute) {
            lastAutoCheckMinute = currentMinute;
            if (!stepper.isMoving() && !pendingHome && !pendingAutoHome) {
                int duePillbox = medicineStorage.getDuePillbox(currentHour, currentMinute);
                if (duePillbox != 0) {
                    int targetContainer = duePillbox - 1;
                    if (targetContainer != currentContainer) {
                        stepper.moveToContainer(targetContainer, currentContainer, pendingHome);
                        autoMovePending = true;   // start auto‑move sequence
                        updateDisplay();
                    }
                }
            }
        }
    }

    // After auto‑move completes (stepper stopped), open lid
    if (autoMovePending && !stepper.isMoving() && !pendingHome) {
        autoMovePending = false;        // movement done
        lidServo.open();
        lidOpen = true;
        lidAutoOpened = true;           // remember that lid was opened by device
        updateDisplay();
    }

    // Button: Next
    if (nextButton.wasPressed()) {
        // Manual move cancels any pending auto‑move
        if (lidAutoOpened) {
            lidAutoOpened = false;       // user manually moved, so no auto‑home needed
        }
        stepper.moveToNextContainer(currentContainer, pendingHome);
        updateDisplay();
    }

    // Button: Lid
    if (lidButton.wasPressed()) {
        toggleLid();
        if (buzzer.isAlarming()) {
            acknowledgeAlarm();
        }
        // If lid was closed and it was previously opened automatically, schedule homing
        if (!lidOpen && lidAutoOpened) {
            pendingAutoHome = true;
            lidAutoOpened = false;       // reset flag
        }
    }

    // Homing after wrap‑around (from next button when at last container)
    if (pendingHome && !stepper.isMoving()) {
        performHomeAfterWrap();
    }

    // Homing after auto‑move and lid close
    if (pendingAutoHome && !stepper.isMoving() && !pendingHome) {
        AccelStepper& s = stepper.getStepper();
        s.setSpeed(-200);
        while (digitalRead(LIMIT_SWITCH) == HIGH) {
            s.runSpeed();
            lidServo.update();
            buzzer.update();
        }
        s.stop();
        s.setCurrentPosition(0);
        pendingAutoHome = false;
        stepper.begin();
        s.disableOutputs();
        updateDisplay();
    }

    // Update display on alarm state change
    static bool lastAlarmState = false;
    if (buzzer.isAlarming() != lastAlarmState) {
        lastAlarmState = buzzer.isAlarming();
        updateDisplay();
    }
}

void updateTime() {
    static unsigned long lastSecond = 0;
    if (millis() - lastSecond >= 1000) {
        lastSecond = millis();
        adjustTime(1);                     // advance one second (UTC)
        // Compute local time (UTC+8)
        time_t utc = now();                 // get current UTC time_t
        time_t local = utc + TIMEZONE_OFFSET;
        // Use gmtime to break down the local time (since local + offset is still a UTC timestamp, but we treat it as local)
        struct tm* tm_local = gmtime(&local);
        currentHour = tm_local->tm_hour;
        currentMinute = tm_local->tm_min;
    }
}

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
    stepper.resetTargetPosition();   // <-- new: reset stored target
    currentContainer = 0;             // <-- important: update container index
    stepper.begin();
    s.disableOutputs();
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

// ---------- WiFi Callback ----------
void onDataReceived(const JsonDocument& doc) {
    Serial.println("Processing received data...");

    if (doc["unixTime"].is<long>()) {
        long unixTime = doc["unixTime"];          // UTC seconds
        setTime(unixTime);                         // store as UTC
        saveTime(unixTime);
        Serial.printf("Time set to UTC: %s", asctime(gmtime(&unixTime)));
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

// ---------- Persistent Storage ----------
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

void saveTime(time_t t) {
    File file = LittleFS.open("/time.txt", FILE_WRITE);
    if (!file) {
        Serial.println("Failed to open time.txt for writing");
        return;
    }
    file.println(t);
    file.close();
    Serial.println("Time saved");
}

time_t loadTime() {
    if (!LittleFS.exists("/time.txt")) return 0;
    File file = LittleFS.open("/time.txt", FILE_READ);
    if (!file) return 0;
    String content = file.readStringUntil('\n');
    file.close();
    return content.toInt();
}