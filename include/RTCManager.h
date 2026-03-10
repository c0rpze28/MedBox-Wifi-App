#ifndef RTC_MANAGER_H
#define RTC_MANAGER_H

#include <Arduino.h>
#include <ThreeWire.h>
#include <RtcDS1302.h>

// Your actual pin assignments
#define RTC_RST 4  // CE/RST
#define RTC_DAT 23  // IO
#define RTC_CLK 14  // SCLK

class RTCManager {
public:
    RTCManager();
    void begin();

    // Getters for individual time components (local time)
    int getYear();
    int getMonth();
    int getDay();
    int getHour();
    int getMinute();
    int getSecond();
    int getDayOfWeek(); // 1=Monday ... 7=Sunday

    // Get time as Unix timestamp (UTC)
    time_t getUnixTime();

    // Set time from Unix timestamp (UTC)
    void setUnixTime(time_t t);

    // Set time manually (local time)
    void setDateTime(int year, int month, int day, int hour, int minute, int second);

    // Check if RTC is running
    bool isRunning();

    // Print current time to Serial (debug)
    void printTime();

private:
    ThreeWire _wire;
    RtcDS1302<ThreeWire> _rtc;
    unsigned long _lastReadMillis;
    RtcDateTime _lastReadTime;

    void updateCache();
};

#endif