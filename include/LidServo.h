#ifndef LID_SERVO_H
#define LID_SERVO_H

#include <Arduino.h>
#include "Config.h"

// Forward declaration – no full definition needed here
class ServoEasing;

class LidServo {
public:
    LidServo();
    ~LidServo();                     // needed to delete the pointer
    void begin();
    void open();
    void close();
    void reAttach();
    void toggle(bool &lidOpen);      // updates lidOpen state
    void update();                   // must be called regularly

private:
    ServoEasing* servo;               // pointer to the real servo object
    static const int CLOSED_ANGLE = 80;
    static const int OPEN_ANGLE = 0;
};

#endif