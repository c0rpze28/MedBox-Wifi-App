#include "RTCManager.h"
#include <TimeLib.h>

RTCManager::RTCManager() 
    : _wire(RTC_DAT, RTC_CLK, RTC_RST),  // constructor order: DAT, CLK, RST
      _rtc(_wire),
      _lastReadMillis(0) {
    _lastReadTime = RtcDateTime(2000, 1, 1, 0, 0, 0); // default
}

void RTCManager::begin() {
    _rtc.Begin();
    
    // Check if RTC is running
    if (!_rtc.GetIsRunning()) {
        Serial.println("RTC not running - will set on phone sync");
    }
    
    updateCache();
}

void RTCManager::updateCache() {
    if (millis() - _lastReadMillis > 500) {
        _lastReadTime = _rtc.GetDateTime();
        _lastReadMillis = millis();
    }
}

int RTCManager::getYear()    { updateCache(); return _lastReadTime.Year(); }
int RTCManager::getMonth()   { updateCache(); return _lastReadTime.Month(); }
int RTCManager::getDay()     { updateCache(); return _lastReadTime.Day(); }
int RTCManager::getHour()    { updateCache(); return _lastReadTime.Hour(); }
int RTCManager::getMinute()  { updateCache(); return _lastReadTime.Minute(); }
int RTCManager::getSecond()  { updateCache(); return _lastReadTime.Second(); }
int RTCManager::getDayOfWeek() { updateCache(); return _lastReadTime.DayOfWeek(); }

time_t RTCManager::getUnixTime() {
    RtcDateTime now = _rtc.GetDateTime();
    _lastReadTime = now;
    _lastReadMillis = millis();
    
    struct tm tm_struct;
    tm_struct.tm_year = now.Year() - 1900;
    tm_struct.tm_mon = now.Month() - 1;
    tm_struct.tm_mday = now.Day();
    tm_struct.tm_hour = now.Hour();
    tm_struct.tm_min = now.Minute();
    tm_struct.tm_sec = now.Second();
    tm_struct.tm_isdst = 0;
    return mktime(&tm_struct);
}

void RTCManager::setUnixTime(time_t t) {
    struct tm* tm_struct = localtime(&t);
    
    RtcDateTime newTime(
        tm_struct->tm_year + 1900,
        tm_struct->tm_mon + 1,
        tm_struct->tm_mday,
        tm_struct->tm_hour,
        tm_struct->tm_min,
        tm_struct->tm_sec
    );
    
    _rtc.SetDateTime(newTime);
    _lastReadTime = newTime;
    _lastReadMillis = millis();
}

void RTCManager::setDateTime(int year, int month, int day, int hour, int minute, int second) {
    RtcDateTime newTime(year, month, day, hour, minute, second);
    _rtc.SetDateTime(newTime);
    _lastReadTime = newTime;
    _lastReadMillis = millis();
}

bool RTCManager::isRunning() {
    return _rtc.GetIsRunning();
}

void RTCManager::printTime() {
    RtcDateTime now = _rtc.GetDateTime();
    Serial.printf("RTC: %04d-%02d-%02d %02d:%02d:%02d\n",
                  now.Year(), now.Month(), now.Day(),
                  now.Hour(), now.Minute(), now.Second());
}