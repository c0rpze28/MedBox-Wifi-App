#include "BuzzerManager.h"
#include "MedicineStorage.h"

BuzzerManager::BuzzerManager(uint8_t pin)
    : pin(pin), alarming(false), alarmStartTime(0), alarmDuration(0),
      medicineStorage(nullptr), toneState(TONE_IDLE), lastToneChange(0),
      beepDuration(200), pauseDuration(200) {}

void BuzzerManager::begin() {
    pinMode(pin, OUTPUT);
    digitalWrite(pin, LOW);   // ensure pin is low at start
}

void BuzzerManager::setMedicineStorage(MedicineStorage* storage) {
    medicineStorage = storage;
}

void BuzzerManager::triggerAlarm(int durationSeconds) {
    if (!alarming) {
        alarming = true;
        alarmStartTime = millis();
        alarmDuration = durationSeconds;
        startPattern();
    }
}

void BuzzerManager::stopAlarm() {
    alarming = false;
    toneState = TONE_IDLE;
    noTone(pin);              // stop any PWM
    digitalWrite(pin, LOW);   // force pin low
}

bool BuzzerManager::isAlarming() const {
    return alarming;
}

void BuzzerManager::update() {
    if (alarming) {
        if (millis() - alarmStartTime >= alarmDuration * 1000UL) {
            stopAlarm();
            return;
        }
        updatePattern();
    }
}

void BuzzerManager::checkScheduledAlarm(int currentHour, int currentMinute) {
    static int lastCheckedMinute = -1;
    if (currentMinute == lastCheckedMinute) return;
    lastCheckedMinute = currentMinute;

    if (medicineStorage && medicineStorage->hasMedicineDueAt(currentHour, currentMinute)) {
        triggerAlarm(45);
    }
}

// ----- Non‑blocking tone pattern -----
void BuzzerManager::startPattern() {
    toneState = TONE_BEEP1;
    lastToneChange = millis();
    toneOn(880); // start first beep
}

void BuzzerManager::updatePattern() {
    unsigned long now = millis();
    switch (toneState) {
        case TONE_BEEP1:
            if (now - lastToneChange >= beepDuration) {
                toneOff();
                toneState = TONE_PAUSE1;
                lastToneChange = now;
            }
            break;
        case TONE_PAUSE1:
            if (now - lastToneChange >= pauseDuration) {
                toneOn(880);
                toneState = TONE_BEEP2;
                lastToneChange = now;
            }
            break;
        case TONE_BEEP2:
            if (now - lastToneChange >= beepDuration) {
                toneOff();
                toneState = TONE_PAUSE2;
                lastToneChange = now;
            }
            break;
        case TONE_PAUSE2:
            if (now - lastToneChange >= 1000) {
                toneState = TONE_BEEP1;
                lastToneChange = now;
                toneOn(880);
            }
            break;
        default:
            break;
    }
}

void BuzzerManager::toneOn(int frequency) {
    ::tone(pin, frequency);
}

void BuzzerManager::toneOff() {
    ::noTone(pin);
    digitalWrite(pin, LOW);   // ensure pin is low after tone stops
}