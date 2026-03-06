#ifndef DISPLAY_MANAGER_H
#define DISPLAY_MANAGER_H

#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include "Config.h"

class DisplayManager {
public:
    DisplayManager();
    bool begin();
    void update(int containerIndex, bool lidOpen, const String& medicineName, 
                const String& expiration, const String& intakeTime,
                bool alarming = false, int hour = 0, int minute = 0);
    void showStartupMessage();
    void showMessage(const String& message, int durationMs);

private:
    Adafruit_SSD1306 display;
};

#endif