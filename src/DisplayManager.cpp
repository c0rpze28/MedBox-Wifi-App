#include "DisplayManager.h"

DisplayManager::DisplayManager() : display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1) {}

bool DisplayManager::begin() {
    return display.begin(SSD1306_SWITCHCAPVCC, 0x3C);
}

void DisplayManager::showStartupMessage() {
    display.clearDisplay();
    display.setTextSize(2);
    display.setCursor(0, 20);
    display.println("Starting...");
    display.display();
}

void DisplayManager::update(int containerIndex, bool lidOpen, const String& medicineName, 
                           const String& expiration, const String& intakeTime,
                           bool alarming, int hour, int minute) {
    display.clearDisplay();
    display.setTextColor(WHITE);

    // Time in top right corner
    display.setTextSize(1);
    char timeStr[6];
    sprintf(timeStr, "%02d:%02d", hour, minute);
    int16_t x1, y1;
    uint16_t w, h;
    display.getTextBounds(timeStr, 0, 0, &x1, &y1, &w, &h);
    display.setCursor(SCREEN_WIDTH - w - 2, 0);
    display.print(timeStr);

    // Alarm indicator
    if (alarming) {
        display.setCursor(0, 0);
        display.print("🔔");
        display.fillCircle(10, 5, 3, WHITE);
    }

    // Container number
    display.setCursor(0, 12);
    display.print("C:");
    display.print(containerIndex + 1);
    display.print("/");
    display.print(NUMBER_OF_CONTAINERS);

    // Medicine name (truncate if too long)
    display.setCursor(0, 24);
    display.print(medicineName.substring(0, 10));
    if (medicineName.length() > 10) display.print("...");

    // Intake time (new line)
    display.setCursor(0, 36);
    if (intakeTime.length() > 0) {
        display.print("Take: ");
        display.println(intakeTime);
    } else {
        display.println("Take: --:--");
    }

    // Expiration
    display.setCursor(0, 48);
    display.println(expiration);

    // Lid status
    display.setCursor(0, 56);  // adjusted to fit at bottom
    display.print("Lid: ");
    display.println(lidOpen ? "OPEN" : "CLOSED");

    // Visual alarm indicator in bottom right
    if (alarming) {
        display.fillRect(SCREEN_WIDTH - 12, SCREEN_HEIGHT - 12, 10, 10, WHITE);
        display.fillRect(SCREEN_WIDTH - 10, SCREEN_HEIGHT - 14, 6, 12, BLACK);
    }

    display.display();
}

void DisplayManager::showMessage(const String& message, int durationMs) {
    display.clearDisplay();
    display.setTextSize(1);
    display.setCursor(0, 20);
    display.println(message);
    display.display();
    delay(durationMs);
    // Restore normal display after delay
}