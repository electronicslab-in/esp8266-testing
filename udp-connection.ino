#include <ESP8266WiFi.h>
#include <WiFiUdp.h>

const char* ssid = "DroneAP";
const char* password = "12345678";

WiFiUDP udp;
unsigned int localPort = 4210;  

char packetBuffer[255];

void setup() {
  Serial.begin(115200);
  WiFi.softAP(ssid, password);
  udp.begin(localPort);

  Serial.println("UDP Server Ready");
  Serial.print("IP Address: ");
  Serial.println(WiFi.softAPIP());
}

void loop() {
  int packetSize = udp.parsePacket();
  if (packetSize) {
    int len = udp.read(packetBuffer, 254);
    if (len > 0) packetBuffer[len] = 0;

    Serial.print("Raw UDP: ");
    Serial.println(packetBuffer);

    // Split by commas
    char *token;
    token = strtok(packetBuffer, ",");
    int values[4];
    int idx = 0;

    while (token != NULL && idx < 4) {
      values[idx] = atoi(token); // convert string to int
      token = strtok(NULL, ",");
      idx++;
    }

    // Print joystick values
    if (idx == 4) {
      Serial.print("LEFT_X = "); Serial.println(values[0]);
      Serial.print("LEFT_Y = "); Serial.println(values[1]);
      Serial.print("RIGHT_X = "); Serial.println(values[2]);
      Serial.print("RIGHT_Y = "); Serial.println(values[3]);
    } else {
      Serial.println("Error: Did not receive 4 values!");
    }
  }
}
