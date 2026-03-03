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

void DisplayManager::update(int containerIndex, bool lidOpen, const String& medicineName, const String& expiration) {
    display.clearDisplay();
    display.setTextColor(WHITE);

    // Container number
    display.setTextSize(1);
    display.setCursor(0, 0);
    display.print("Container: ");
    display.println(containerIndex + 1);

    // Medicine name
    display.setCursor(0, 15);
    display.print("Med: ");
    display.println(medicineName);

    // Expiration
    display.setCursor(0, 28);
    display.println(expiration);

    // Lid status
    display.setCursor(0, 45);
    display.print("Lid: ");
    display.println(lidOpen ? "OPEN" : "CLOSED");

    display.display();
}