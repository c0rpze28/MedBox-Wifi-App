#ifndef STEPPER_MANAGER_H
#define STEPPER_MANAGER_H

#include <AccelStepper.h>
#include "Config.h"

class StepperManager {
public:
    StepperManager();
    void begin();
    void home();
    void moveToNextContainer(int &currentContainer, bool &pendingHome);
    void update();          // must be called frequently in loop
    bool isMoving() const;
    void stopAndDisable();

private:
    AccelStepper stepper;
    long targetPosition;
    bool moving;
};

#endif