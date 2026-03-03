#ifndef WIFI_MANAGER_H
#define WIFI_MANAGER_H

#include <Arduino.h>
#include <WiFi.h>
#include <WebServer.h>
#include <ArduinoJson.h>

// Callback function type to be called when new medicine data is received
typedef std::function<void(const JsonDocument& doc)> DataReceivedCallback;

class WiFiManager {
public:
    WiFiManager(const char* apSSID, const char* apPassword);
    void begin();
    void loop();                     // call in main loop to handle clients
    void setDataCallback(DataReceivedCallback cb);

private:
    const char* _ssid;
    const char* _password;
    WebServer _server;
    DataReceivedCallback _dataCallback;

    void handleRoot();
    void handleUpdate();
    void handleNotFound();
};

#endif