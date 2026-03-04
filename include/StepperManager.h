#ifndef STEPPER_MANAGER_H
#define STEPPER_MANAGER_H

#include <AccelStepper.h>
#include "Config.h"

class StepperManager {
public:
    StepperManager();
    void begin();
    void home();                     // initial homing (blocks)
    void homeSlow();                 // slow homing for after wrap (blocks)
    void moveToNextContainer(int &currentContainer, bool &pendingHome);
    void moveToContainer(int containerIndex, int &currentContainer, bool &pendingHome); // new
    void update();                    // call frequently
    bool isMoving() const;
    void stopAndDisable();
    void resetTargetPosition();
    
    AccelStepper& getStepper();

private:
    AccelStepper stepper;
    long targetPosition;
    bool moving;
};

#endif