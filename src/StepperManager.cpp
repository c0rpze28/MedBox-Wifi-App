#include "StepperManager.h"

StepperManager::StepperManager() 
    : stepper(AccelStepper::HALF4WIRE, STEPPER_IN1, STEPPER_IN3, STEPPER_IN2, STEPPER_IN4),
      targetPosition(0), moving(false) {}

void StepperManager::begin() {
    stepper.setMaxSpeed(900);
    stepper.setAcceleration(400);
    pinMode(LIMIT_SWITCH, INPUT_PULLUP);
}

void StepperManager::home() {
    stepper.setSpeed(-300);
    while (digitalRead(LIMIT_SWITCH) == HIGH) {
        stepper.runSpeed();
        // Note: We'll rely on external update of servo in main loop
    }
    stepper.stop();
    stepper.setCurrentPosition(0);
    targetPosition = 0;
    stepper.disableOutputs();
    moving = false;
}

void StepperManager::moveToNextContainer(int &currentContainer, bool &pendingHome) {
    if (currentContainer == NUMBER_OF_CONTAINERS - 1) {
        // Wrapping around: will home after reaching target
        currentContainer = 0;
        targetPosition -= STEPS_PER_CONTAINER;
        stepper.enableOutputs();
        stepper.moveTo(targetPosition);
        moving = true;
        pendingHome = true;
    } else {
        currentContainer++;
        targetPosition -= STEPS_PER_CONTAINER;
        stepper.enableOutputs();
        stepper.moveTo(targetPosition);
        moving = true;
        pendingHome = false;
    }
}

void StepperManager::update() {
    if (moving) {
        stepper.run();
        if (stepper.distanceToGo() == 0) {
            moving = false;
            stepper.disableOutputs();
        }
    }
}

bool StepperManager::isMoving() const {
    return moving;
}

void StepperManager::stopAndDisable() {
    stepper.stop();
    stepper.disableOutputs();
    moving = false;
}

void StepperManager::homeSlow() {
    stepper.setSpeed(-200);
    while (digitalRead(LIMIT_SWITCH) == HIGH) {
        stepper.runSpeed();
        // Note: This blocks. In the after‑wrap case we call it from a loop
        // that already updates the servo and buzzer.
    }
    stepper.stop();
    stepper.setCurrentPosition(0);
    stepper.disableOutputs();
}

AccelStepper& StepperManager::getStepper() {
    return stepper;
}

void StepperManager::moveToContainer(int containerIndex, int &currentContainer, bool &pendingHome) {
    // containerIndex: 0..5
    if (containerIndex < 0 || containerIndex >= NUMBER_OF_CONTAINERS) return;
    if (containerIndex == currentContainer) return; // already there

    long newTarget = -(containerIndex * STEPS_PER_CONTAINER);
    targetPosition = newTarget;
    stepper.enableOutputs();
    stepper.moveTo(targetPosition);
    moving = true;

    // Determine if we need to home after reaching target (i.e., crossing the zero point)
    // This happens when moving from a higher container to a lower one, because we go further negative.
    // For simplicity, we set pendingHome if moving to container 0 (home) and not already there.
    pendingHome = (containerIndex == 0 && currentContainer != 0);

    currentContainer = containerIndex; // update immediately for display
}