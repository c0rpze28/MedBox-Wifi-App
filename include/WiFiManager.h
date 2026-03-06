#ifndef WIFI_MANAGER_H
#define WIFI_MANAGER_H

#include <Arduino.h>
#include <WiFi.h>
#include <WebServer.h>
#include <ArduinoJson.h>
#include <functional> 

// Callback function type
typedef std::function<void(const JsonDocument& doc)> DataReceivedCallback;

class WiFiManager {
public:
    WiFiManager(const char* apSSID, const char* apPassword);
    void begin();
    void loop();                     // call in main loop
    void setDataCallback(DataReceivedCallback cb);
    
    // New methods for timeout management
    void startAP();                  // turn AP on
    void stopAP();                   // turn AP off
    bool isAPActive();               // check if AP is running
    void handleClientActivity();      // call when client connects/disconnects
    void setTimeoutMinutes(int minutes); // set inactivity timeout
    
    // For button wake detection
    void checkWakeCondition();        // called from main loop

private:
    const char* _ssid;
    const char* _password;
    WebServer _server;
    DataReceivedCallback _dataCallback;
    
    // Timeout management
    bool _apActive;
    unsigned long _lastClientActivity;
    int _timeoutMinutes;
    unsigned long _wakePressStartTime;
    bool _wakeButtonPressed;
    
    void handleRoot();
    void handleUpdate();
    void handleNotFound();
    
    // Track client connections
    void updateClientActivity();
};

#endif