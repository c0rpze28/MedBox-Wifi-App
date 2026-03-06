#include "ButtonManager.h"

ButtonManager::ButtonManager(uint8_t pin, unsigned long debounceDelay)
    : pin(pin), debounceDelay(debounceDelay), lastDebounceTime(0),
      lastReading(HIGH), state(HIGH), pressedFlag(false) {}

void ButtonManager::begin() {
    pinMode(pin, INPUT_PULLUP);
}

void ButtonManager::update() {
    bool reading = digitalRead(pin);
    if (reading != lastReading) {
        lastDebounceTime = millis();
    }
    if ((millis() - lastDebounceTime) > debounceDelay) {
        if (reading != state) {
            state = reading;
            if (state == LOW) {
                pressedFlag = true;
            }
        }
    }
    lastReading = reading;
}

bool ButtonManager::wasPressed() {
    if (pressedFlag) {
        pressedFlag = false;
        return true;
    }
    return false;
}

bool ButtonManager::isPressed() {
    return state == LOW;
}

unsigned long ButtonManager::getPressDuration() {
    if (state == LOW) {
        return millis() - (lastDebounceTime + debounceDelay);
    }
    return 0;
}