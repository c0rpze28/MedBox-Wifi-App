#ifndef DISPLAY_MANAGER_H
#define DISPLAY_MANAGER_H

#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include "Config.h"

class DisplayManager {
public:
    DisplayManager();
    bool begin();
    void update(int containerIndex, bool lidOpen, const String& medicineName, const String& expiration);
    void showStartupMessage();

private:
    Adafruit_SSD1306 display;
};

#endif