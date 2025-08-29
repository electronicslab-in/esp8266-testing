#include <ESP8266WiFi.h>
#include <WiFiUdp.h>

const char *ssid = "ESP8266_AP";
const char *password = "12345678";

const int CONNECTION_LED = LED_BUILTIN; // Onboard LED (GPIO2, also called D2 active LOW)

WiFiUDP udp;
unsigned int localPort = 4210; // Port to listen on
char incomingPacket[255];      // Buffer for incoming data
char replyPacket[] = "Hello from ESP8266"; // Reply message

void setup() {

  //Wifi Connection indicator LED
  pinMode(CONNECTION_LED, OUTPUT);
  digitalWrite(CONNECTION_LED, HIGH);  // solid OFF

  //start serial communication
  Serial.begin(115200);
  Serial.println();
  Serial.println("Starting ESP8266 UDP Listener...");

  // Start WiFi in Access Point mode
  WiFi.mode(WIFI_AP);
  WiFi.softAP(ssid, password);

  IPAddress myIP = WiFi.softAPIP();
  Serial.print("Access Point IP: ");
  Serial.println(myIP);

  //start UDP listener
  udp.begin(localPort);
  Serial.printf("Now listening for UDP packets at port %d\n", localPort);

}

void loop() {

  //test is wifi device is connected or not
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

  int packetSize = udp.parsePacket();
  if (packetSize) {
    // Read packet
    int len = udp.read(incomingPacket, 255);
    if (len > 0) {
      incomingPacket[len] = '\0'; // Null-terminate string
    }

    // Print received info
    Serial.printf("Received packet from %s:%d\n", udp.remoteIP().toString().c_str(), udp.remotePort());
    Serial.printf("Packet contents: %s\n", incomingPacket);

    delay(100);
  }

}















