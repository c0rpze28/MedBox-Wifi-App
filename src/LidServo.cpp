#include "LidServo.h"

LidServo::LidServo() {}

void LidServo::begin() {
    servo.attach(SERVO_PIN, 500, 2400);
    servo.setEasingType(EASE_CUBIC_IN_OUT);
    servo.setSpeed(60);
    servo.startEaseTo(CLOSED_ANGLE);
}

void LidServo::open() {
    servo.startEaseTo(OPEN_ANGLE);
}

void LidServo::close() {
    servo.startEaseTo(CLOSED_ANGLE);
}

void LidServo::toggle(bool &lidOpen) {
    if (lidOpen) {
        close();
        lidOpen = false;
    } else {
        open();
        lidOpen = true;
    }
}

void LidServo::update() {
    servo.update();
}