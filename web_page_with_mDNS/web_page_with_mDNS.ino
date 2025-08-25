#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>

const char* ssid = "Drone-WiFi";
const char* password = "12345678";

const int CONNECTION_LED = LED_BUILTIN; // Onboard LED (GPIO2, active LOW)

unsigned long previousMillis = 0;
const long interval = 250; // Blink interval (ms)
bool ledState = LOW;

// Global variables to store mode and motor duty cycles
String mode = "none";
int motor1 = 0, motor2 = 0, motor3 = 0, motor4 = 0;

// Web server running on port 80
ESP8266WebServer server(80);

void handleConfig() {
  // If form data is received, update variables
  if (server.hasArg("mode")) {
    mode = server.arg("mode");
  }
  if (server.hasArg("motor1")) motor1 = server.arg("motor1").toInt();
  if (server.hasArg("motor2")) motor2 = server.arg("motor2").toInt();
  if (server.hasArg("motor3")) motor3 = server.arg("motor3").toInt();
  if (server.hasArg("motor4")) motor4 = server.arg("motor4").toInt();

  // Build HTML page
  String html = "<!DOCTYPE html><html><head><title>ESP Config</title>";
  html += "<style>body{font-family:Arial;margin:20px;} .slider{width:300px;} </style>";
  html += "</head><body>";
  html += "<h1>Drone Configuration</h1>";

  // Show current mode
  html += "<p><b>Current Mode:</b> " + mode + "</p>";

  // Config form
  html += "<form action='/config' method='GET'>";

  // Checkbox for manual mode
  html += "<p><input type='checkbox' name='mode' value='manual_motor_pwm_control'";
  if (mode == "manual_motor_pwm_control") html += " checked";
  html += "> Enable Manual Motor PWM Control</p>";

  // Sliders (0–100)
  html += "<h3>Motor Duty Cycles (0–100)</h3>";
  html += "Motor 1: <input class='slider' type='range' name='motor1' min='0' max='100' value='" + String(motor1) + "' oninput='m1out.value=this.value'> <output id='m1out'>" + String(motor1) + "</output>%<br>";
  html += "Motor 2: <input class='slider' type='range' name='motor2' min='0' max='100' value='" + String(motor2) + "' oninput='m2out.value=this.value'> <output id='m2out'>" + String(motor2) + "</output>%<br>";
  html += "Motor 3: <input class='slider' type='range' name='motor3' min='0' max='100' value='" + String(motor3) + "' oninput='m3out.value=this.value'> <output id='m3out'>" + String(motor3) + "</output>%<br>";
  html += "Motor 4: <input class='slider' type='range' name='motor4' min='0' max='100' value='" + String(motor4) + "' oninput='m4out.value=this.value'> <output id='m4out'>" + String(motor4) + "</output>%<br>";

  // Submit button
  html += "<br><input type='submit' value='Save Settings'>";
  html += "</form>";

  html += "</body></html>";

  server.send(200, "text/html", html);
}


void setup() {
  Serial.begin(115200);
  pinMode(CONNECTION_LED, OUTPUT);

  // Start an access point
  WiFi.mode(WIFI_AP);               
  WiFi.softAP(ssid, password);      

  Serial.println("Access Point Started");
  Serial.print("AP IP address: ");
  Serial.println(WiFi.softAPIP());

  // Setup web routes
  server.on("/config", handleConfig);
  server.begin();
  Serial.println("Web server started");
}

void loop() {

  static int lastClients = 0;
  int clients = WiFi.softAPgetStationNum();

  // Serially print when a device connects or disconnects
  if (clients != lastClients) {  
    // Only run when client count changes
    lastClients = clients;

    if (clients > 0) {
      digitalWrite(CONNECTION_LED, LOW);  // solid ON
      Serial.println("Device Connected");
    } else {
      Serial.println("No device connected...");
      Serial.print("Access Point IP Address: ");
      Serial.println(WiFi.softAPIP());
    }
  }

  // Blink LED only if no clients
  if (clients == 0) {
    unsigned long currentMillis = millis();
    if (currentMillis - previousMillis >= interval) {
      previousMillis = currentMillis;
      ledState = !ledState;
      digitalWrite(CONNECTION_LED, ledState ? LOW : HIGH);
    }
  }

  // Handle incoming HTTP requests
  server.handleClient();
  //delay(500); // Slow down serial output

  // Print current settings every 2 seconds
  static unsigned long lastPrint = 0;
  if (millis() - lastPrint > 5000) {
    lastPrint = millis();

    Serial.println("========== Drone Config ==========");
    Serial.print("Mode: ");
    Serial.println(mode);

    Serial.print("Motor 1: ");
    Serial.print(motor1);
    Serial.println("%");

    Serial.print("Motor 2: ");
    Serial.print(motor2);
    Serial.println("%");

    Serial.print("Motor 3: ");
    Serial.print(motor3);
    Serial.println("%");

    Serial.print("Motor 4: ");
    Serial.print(motor4);
    Serial.println("%");

    Serial.println("=================================");
  }

}



