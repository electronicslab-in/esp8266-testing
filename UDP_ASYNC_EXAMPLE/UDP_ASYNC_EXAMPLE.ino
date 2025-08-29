//library can be added from  https://github.com/me-no-dev/ESPAsyncUDP

//ASYNC UDP - does not blocks main thread

#include <ESP8266WiFi.h>
#include <ESPAsyncUDP.h>

const char *ssid = "ESP8266_AP";
const char *password = "12345678";

const int CONNECTION_LED = LED_BUILTIN; // Onboard LED (GPIO2, also called D2 active LOW)

AsyncUDP udp;

void setup() {
  //Wifi Connection indicator LED
  pinMode(CONNECTION_LED, OUTPUT);
  digitalWrite(CONNECTION_LED, HIGH);  // solid OFF

  //start serial communication
  Serial.begin(115200);

  // Start WiFi in Access Point mode
  WiFi.mode(WIFI_AP);
  WiFi.softAP(ssid, password);

  IPAddress myIP = WiFi.softAPIP();
  Serial.print("Access Point IP: ");
  Serial.println(myIP);

  // Start UDP listener on port 4210
  if (udp.listen(4210)) {
    Serial.println("UDP Listening on port 4210");
    
    // Register callback for packet reception
    udp.onPacket([](AsyncUDPPacket packet) {
      Serial.print("UDP Packet received from: ");
      Serial.print(packet.remoteIP());
      Serial.print(":");
      Serial.println(packet.remotePort());

      Serial.print("Data: ");
      Serial.write(packet.data(), packet.length());
      Serial.println();

      // Optional: send a reply back
      packet.printf("Got %u bytes of data", packet.length());
    });
  }
}

void loop() {
  int clients = WiFi.softAPgetStationNum(); 

  static int lastClients = 0;
  // Serially print when a device connects or disconnects
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

  delay(1000);
}
