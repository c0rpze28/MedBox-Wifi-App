#include "StepperManager.h"

StepperManager::StepperManager() 
    : stepper(AccelStepper::HALF4WIRE, STEPPER_IN1, STEPPER_IN3, STEPPER_IN2, STEPPER_IN4),
      targetPosition(0), moving(false) {}

void StepperManager::begin() {
    stepper.setMaxSpeed(900);
    stepper.setAcceleration(400);
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