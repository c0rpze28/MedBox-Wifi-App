#ifndef BUTTON_MANAGER_H
#define BUTTON_MANAGER_H

#include <Arduino.h>
#include "Config.h"

class ButtonManager {
public:
    ButtonManager(uint8_t pin, unsigned long debounceDelay = 50);
    void begin();
    void update();  // call in loop
    bool wasPressed(); // returns true if button was pressed since last call
    bool isPressed();           // returns current button state
    unsigned long getPressDuration(); // how long button has been held

private:
    uint8_t pin;
    unsigned long debounceDelay;
    unsigned long lastDebounceTime;
    bool lastReading;
    bool state;
    bool pressedFlag;  // set when press detected, cleared by wasPressed()
};

#endif