#ifndef CONFIG_H
#define CONFIG_H

// OLED
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64

// Stepper motor pins
#define STEPPER_IN1 16
#define STEPPER_IN2 17
#define STEPPER_IN3 18
#define STEPPER_IN4 19

// Inputs
#define LIMIT_SWITCH 32
#define NEXT_BUTTON 33
#define LID_BUTTON 25

// Servo
#define SERVO_PIN 26

// System constants
#define NUMBER_OF_CONTAINERS 6
#define STEPS_PER_REV_TRAY 17824
#define STEPS_PER_CONTAINER 2730

#endif