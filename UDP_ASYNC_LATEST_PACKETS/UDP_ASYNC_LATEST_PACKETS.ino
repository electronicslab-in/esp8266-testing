#include <ESP8266WiFi.h>
//library can be added from  https://github.com/me-no-dev/ESPAsyncUDP

//ASYNC UDP - does not blocks main thread
// Only processed the latest data in main loop fetched from buffer at 50hz i.e. after 20 mseconds that too if the received data is fresh
#include <ESPAsyncUDP.h>

const int CONNECTION_LED = LED_BUILTIN; // Onboard LED (GPIO2, also called D2 active LOW)

const char *ssid = "ESP8266_AP";
const char *password = "12345678";

AsyncUDP udp;

// Global buffer for latest joystick data
String latestPacket = "";
volatile bool newPacketAvailable = false;

void setup() {
  //Wifi Connection indicator LED
  pinMode(CONNECTION_LED, OUTPUT);
  digitalWrite(CONNECTION_LED, HIGH);  // solid OFF

  //wifi AP setup
  Serial.begin(115200);
  WiFi.mode(WIFI_AP);
  WiFi.softAP(ssid, password);

  Serial.print("AP IP: ");
  Serial.println(WiFi.softAPIP());

  if (udp.listen(4210)) {
    Serial.println("UDP Listening on port 4210");

    udp.onPacket([](AsyncUDPPacket packet) {
      // Just store latest data, overwrite older
      latestPacket = String((char*)packet.data()).substring(0, packet.length());
      newPacketAvailable = true;
    });
  }
}

void loop() {

  //---------wifi-connection test loop ---------------------------------
  int clients = WiFi.softAPgetStationNum(); 
  static int lastClients = 0;
  static unsigned long lastConnectionTestLoop = 0;

  if (millis() - lastConnectionTestLoop >= 1000) { // Test Connection after 1 seconds
      lastConnectionTestLoop = millis();

      if (clients != lastClients) {  
        // Only run when client count changes
        lastClients = clients;

        if (clients > 0) {
          digitalWrite(CONNECTION_LED, LOW);  // solid ON
          Serial.println("Device Connected");
          Serial.print("Access Point IP Address: ");
          Serial.println(WiFi.softAPIP());
        } else {
          digitalWrite(CONNECTION_LED, HIGH);  // solid OFF
          Serial.println("No device connected...");
          Serial.print("Access Point IP Address: ");
          Serial.println(WiFi.softAPIP());
        }
      }
  }

  //---------------------------------------------------------------------------------



  // ---------------- main control loop at fixed rate ----------------------
  static unsigned long lastMainLoop = 0;
  if (millis() - lastMainLoop >= 20) { // 50 Hz main control loop
    lastMainLoop = millis();

    if (newPacketAvailable) {
      newPacketAvailable = false;
      Serial.print("Using joystick data: ");
      Serial.println(latestPacket);
      // parse joystick → update PID target here
    } else {
      // No new packet → keep last command
    }
  }

  


}
