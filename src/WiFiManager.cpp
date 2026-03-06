#include "WiFiManager.h"

WiFiManager::WiFiManager(const char* apSSID, const char* apPassword)
    : _ssid(apSSID), _password(apPassword), _server(80), 
      _dataCallback(nullptr), _apActive(false), _lastClientActivity(0),
      _timeoutMinutes(15), _wakePressStartTime(0), _wakeButtonPressed(false) {}

void WiFiManager::begin() {
    startAP();  // Start AP on boot
}

void WiFiManager::loop() {
    if (_apActive) {
        _server.handleClient();
        
        // Check for client activity timeout
        if (_lastClientActivity > 0 && millis() - _lastClientActivity > _timeoutMinutes * 60 * 1000UL) {
            // No activity for timeout period - turn off AP
            Serial.println("WiFi timeout - turning off AP");
            stopAP();
        }
    }
}

void WiFiManager::startAP() {
    if (!_apActive) {
        WiFi.softAP(_ssid, _password);
        IPAddress IP = WiFi.softAPIP();
        Serial.print("AP started - IP: ");
        Serial.println(IP);
        
        _server.on("/", std::bind(&WiFiManager::handleRoot, this));
        _server.on("/update", HTTP_POST, std::bind(&WiFiManager::handleUpdate, this));
        _server.onNotFound(std::bind(&WiFiManager::handleNotFound, this));
        _server.begin();
        
        _apActive = true;
        _lastClientActivity = millis();  // Start timeout timer
    }
}

void WiFiManager::stopAP() {
    if (_apActive) {
        _server.stop();
        WiFi.softAPdisconnect(true);
        Serial.println("AP stopped");
        _apActive = false;
        _lastClientActivity = 0;
    }
}

bool WiFiManager::isAPActive() {
    return _apActive;
}

void WiFiManager::handleClientActivity() {
    if (_apActive) {
        _lastClientActivity = millis();  // Reset timeout
        Serial.println("Client activity detected - timeout reset");
    }
}

void WiFiManager::setTimeoutMinutes(int minutes) {
    _timeoutMinutes = minutes;
}

void WiFiManager::checkWakeCondition() {
    // This will be called from main loop with button state
    // Actual button logic is handled in main.cpp, this just tracks state
}

void WiFiManager::updateClientActivity() {
    // Call this whenever a client connects or data is received
    if (_apActive && WiFi.softAPgetStationNum() > 0) {
        handleClientActivity();
    }
}

// Web server handlers
void WiFiManager::handleRoot() {
    updateClientActivity();
    _server.send(200, "text/plain", "ESP32 MedBox Ready");
}

void WiFiManager::handleUpdate() {
    updateClientActivity();
    
    if (!_server.hasArg("plain")) {
        _server.send(400, "application/json", "{\"error\":\"Body not received\"}");
        return;
    }

    String body = _server.arg("plain");
    Serial.println("Received JSON:");
    Serial.println(body);

    JsonDocument doc;
    DeserializationError error = deserializeJson(doc, body);

    if (error) {
        Serial.print("JSON parse failed: ");
        Serial.println(error.c_str());
        _server.send(400, "application/json", "{\"error\":\"Invalid JSON\"}");
        return;
    }

    if (_dataCallback) {
        _dataCallback(doc);
    }

    _server.send(200, "application/json", "{\"status\":\"ok\"}");
}

void WiFiManager::handleNotFound() {
    updateClientActivity();
    _server.send(404, "text/plain", "Not found");
}

void WiFiManager::setDataCallback(DataReceivedCallback cb) {
    _dataCallback = cb;
}