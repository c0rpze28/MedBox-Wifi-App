#ifndef BUZZER_MANAGER_H
#define BUZZER_MANAGER_H

#include <Arduino.h>
#include "Config.h"

class MedicineStorage; // forward declaration

class BuzzerManager {
public:
    BuzzerManager(uint8_t pin);
    void begin();
    void update();                     // call frequently in loop
    void triggerAlarm(int durationSeconds = 30);
    void stopAlarm();
    bool isAlarming() const;
    void setMedicineStorage(MedicineStorage* storage);
    void checkScheduledAlarm(int currentHour, int currentMinute);

private:
    uint8_t pin;
    bool alarming;
    unsigned long alarmStartTime;
    int alarmDuration;                 // seconds
    MedicineStorage* medicineStorage;

    // Non‑blocking tone pattern state
    enum ToneState { TONE_IDLE, TONE_BEEP1, TONE_PAUSE1, TONE_BEEP2, TONE_PAUSE2 };
    ToneState toneState;
    unsigned long lastToneChange;
    int beepDuration;                  // ms
    int pauseDuration;                 // ms

    void startPattern();
    void updatePattern();
    void toneOn(int frequency);
    void toneOff();
};

#endif