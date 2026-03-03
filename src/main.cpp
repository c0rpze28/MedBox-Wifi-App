#include <Wire.h>
#include "Config.h"
#include "MedicineData.h"
#include "DisplayManager.h"
#include "LidServo.h"
#include "StepperManager.h"
#include "ButtonManager.h"

// Global objects
MedicineData medicine;
DisplayManager display;
LidServo lidServo;
StepperManager stepper;
ButtonManager nextButton(NEXT_BUTTON);
ButtonManager lidButton(LID_BUTTON);

// State variables
int currentContainer = 0;
bool lidOpen = false;
bool pendingHome = false;   // true if we need to home after current move

void setup() {
    Serial.begin(115200);
    Wire.begin(21, 22);

    if (!display.begin()) {
        while (1); // hang if display fails
    }
    display.showStartupMessage();
    delay(1000);

    // Initialize peripherals
    stepper.begin();
    lidServo.begin();
    nextButton.begin();
    lidButton.begin();

    // Home the stepper
    stepper.home();
    updateDisplay();
}

void loop() {
    // Update non-blocking components
    stepper.update();
    lidServo.update();
    nextButton.update();
    lidButton.update();

    // Handle button presses
    if (nextButton.wasPressed()) {
        moveToNextContainer();
    }
    if (lidButton.wasPressed()) {
        toggleLid();
    }

    // Handle homing after wrap-around move
    if (pendingHome && !stepper.isMoving()) {
        performHomeAfterWrap();
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

void performHomeAfterWrap() {
    // Disable stepper outputs if still enabled
    stepper.stopAndDisable();

    // Slow homing sequence
    stepper.begin();  // re-initialize speeds (optional, but we need to set slow speed)
    // We'll home manually here
    while (digitalRead(LIMIT_SWITCH) == HIGH) {
        // Use AccelStepper's runSpeed for continuous motion
        stepper.setSpeed(-200);
        stepper.runSpeed();
        lidServo.update();  // keep servo updating
    }
    stepper.stop();
    stepper.setCurrentPosition(0);
    pendingHome = false;
    stepper.begin();  // restore normal speeds
    stepper.disableOutputs();

    updateDisplay();
}

void updateDisplay() {
    display.update(currentContainer, lidOpen,
                   medicine.getName(currentContainer),
                   medicine.getExpiration(currentContainer));
}