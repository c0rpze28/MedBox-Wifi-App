#include "LidServo.h"
#include <ServoEasing.hpp>            // full definition only here

LidServo::LidServo() : servo(nullptr) {}

LidServo::~LidServo() {
    if (servo) delete servo;
}

void LidServo::begin() {
    servo = new ServoEasing();
    servo->attach(SERVO_PIN, 500, 2400);
    servo->setEasingType(EASE_CUBIC_IN_OUT);
    servo->setSpeed(60);
    servo->startEaseTo(CLOSED_ANGLE);
}

void LidServo::open() {
    if (servo) servo->startEaseTo(OPEN_ANGLE);
}

void LidServo::close() {
    if (servo) servo->startEaseTo(CLOSED_ANGLE);
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
    if (servo) servo->update();
}