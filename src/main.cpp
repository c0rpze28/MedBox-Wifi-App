#include <Arduino.h>
#include <WiFi.h>
#include <WebServer.h>

// Access Point credentials
const char* ssid = "ESP32-Hotspot";
const char* password = "12345678";

// LED pin (built-in LED on most ESP32 boards is GPIO2)
const int ledPin = 2;
bool ledState = false;

// Create a web server object that listens on port 80
WebServer server(80);

// HTML page for the control interface
const char index_html[] PROGMEM = R"rawliteral(
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>ESP32 Control</title>
    <style>
        body { font-family: Arial; text-align: center; margin-top: 50px; }
        button { padding: 15px 30px; font-size: 18px; margin: 10px; }
        .on { background-color: #4CAF50; color: white; }
        .off { background-color: #f44336; color: white; }
    </style>
</head>
<body>
    <h1>ESP32 Control Panel</h1>
    <p><a href="/on"><button class="on">LED ON</button></a></p>
    <p><a href="/off"><button class="off">LED OFF</button></a></p>
    <p>Status: <span id="status">Loading...</span></p>

    <script>
        function fetchStatus() {
            fetch('/status')
                .then(response => response.text())
                .then(data => {
                    document.getElementById('status').innerText = data;
                });
        }
        fetchStatus();
        setInterval(fetchStatus, 2000);
    </script>
</body>
</html>
)rawliteral";

// Handle root URL: serve the HTML page
void handleRoot() {
  server.send(200, "text/html", index_html);
}

// Handle /on: turn LED on
void handleOn() {
  ledState = true;
  digitalWrite(ledPin, HIGH);
  server.send(200, "text/plain", "LED is ON");
}

// Handle /off: turn LED off
void handleOff() {
  ledState = false;
  digitalWrite(ledPin, LOW);
  server.send(200, "text/plain", "LED is OFF");
}

// Handle /status: return current LED state
void handleStatus() {
  String state = ledState ? "ON" : "OFF";
  server.send(200, "text/plain", "LED is " + state);
}

// Handle 404 - Not found
void handleNotFound() {
  server.send(404, "text/plain", "404: Not Found");
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println();

  // Configure LED pin as output
  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, LOW); // start with LED off

  // Set up Access Point
  WiFi.softAP(ssid, password);
  IPAddress apIP = WiFi.softAPIP();
  Serial.print("AP IP address: ");
  Serial.println(apIP);

  // Define server routes
  server.on("/", handleRoot);
  server.on("/on", handleOn);
  server.on("/off", handleOff);
  server.on("/status", handleStatus);
  server.onNotFound(handleNotFound);

  // Start server
  server.begin();
  Serial.println("HTTP server started");
}

void loop() {
  server.handleClient(); // handle incoming client requests
}