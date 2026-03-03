#ifndef LID_SERVO_H
#define LID_SERVO_H

#include <ServoEasing.h>
#include "Config.h"

class LidServo {
public:
    LidServo();
    void begin();
    void open();
    void close();
    void toggle(bool &lidOpen);  // toggles and returns new state
    void update();               // must be called frequently in loop

private:
    ServoEasing servo;
    static const int CLOSED_ANGLE = 80;
    static const int OPEN_ANGLE = 0;
};

#endif