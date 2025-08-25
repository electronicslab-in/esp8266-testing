#include <ESP8266WiFi.h>

const char* ssid = "Drone-WiFi";
const char* password = "12345678";

const int LED_PIN = LED_BUILTIN; //pin no 2 -  Onboard LED (GPIO2)

unsigned long previousMillis = 0;
const long interval = 250; // Blink interval (ms)
bool ledState = LOW;

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  delay(2000);
  WiFi.softAP(ssid, password);
  Serial.println("Access Point Started");
  Serial.print("IP Address: ");
  Serial.println(WiFi.softAPIP());
}

void loop() {

  static int lastClients = 0;
  int clients = WiFi.softAPgetStationNum();

  if (clients != lastClients) {  
    // Only run when client count changes
    lastClients = clients;

    if (clients > 0) {
      digitalWrite(CONNECTION_LED, LOW);  // solid ON
      Serial.println("Device Connected");
    } else {
      Serial.println("No device connected...");
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
}
  int clients = WiFi.softAPgetStationNum();

  if (clients > 0) {
    // Solid ON when client connected
    digitalWrite(LED_PIN, LOW);  // active LOW
  } else {
    // Blink when no client connected
    unsigned long currentMillis = millis();
    if (currentMillis - previousMillis >= interval) {
      previousMillis = currentMillis;
      ledState = !ledState;
      digitalWrite(LED_PIN, ledState ? LOW : HIGH);
    }
  }
}
