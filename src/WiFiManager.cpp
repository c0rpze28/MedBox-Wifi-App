#include "WiFiManager.h"

WiFiManager::WiFiManager(const char* apSSID, const char* apPassword)
    : _ssid(apSSID), _password(apPassword), _server(80), _dataCallback(nullptr) {}

void WiFiManager::begin() {
    WiFi.softAP(_ssid, _password);
    IPAddress IP = WiFi.softAPIP();
    Serial.print("AP IP address: ");
    Serial.println(IP);

    _server.on("/", std::bind(&WiFiManager::handleRoot, this));
    _server.on("/update", HTTP_POST, std::bind(&WiFiManager::handleUpdate, this));
    _server.onNotFound(std::bind(&WiFiManager::handleNotFound, this));
    _server.begin();
    Serial.println("HTTP server started");
}

void WiFiManager::loop() {
    _server.handleClient();
}

void WiFiManager::setDataCallback(DataReceivedCallback cb) {
    _dataCallback = cb;
}

void WiFiManager::handleRoot() {
    _server.send(200, "text/plain", "ESP32 MedBox Ready");
}

void WiFiManager::handleUpdate() {
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

    // Send JSON success response
    _server.send(200, "application/json", "{\"status\":\"ok\"}");
}

void WiFiManager::handleNotFound() {
    _server.send(404, "text/plain", "Not found");
}