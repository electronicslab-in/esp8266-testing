#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>

const char* ssid = "Drone-WiFi";
const char* password = "12345678";

const int CONNECTION_LED = LED_BUILTIN; // Onboard LED (GPIO2, also called D2 active LOW)

unsigned long previousMillis = 0;
const long interval = 250; // Blink interval (ms)
bool ledState = LOW;

// Global variables to store mode and motor duty cycles
String mode = "none";
int motor1 = 0, motor2 = 0, motor3 = 0, motor4 = 0;

// Assign motor pins (choose GPIOs that support PWM)
const int motorPin1 = D1;  // GPIO5
const int motorPin2 = D2;  // GPIO4
const int motorPin3 = D5;  // GPIO14
const int motorPin4 = D6;  // GPIO12

// Global PID variables
float pitch_P_value = 1.2, roll_P_value = 1.2, yaw_P_value = 1.2;
float pitch_I_value = 0.0, roll_I_value = 0.0, yaw_I_value = 0.0;
float pitch_D_value = 0.0, roll_D_value = 0.0, yaw_D_value = 0.0;

// Web server running on port 80
ESP8266WebServer server(80);

// webpage for manual pwm control GUI
void manualPWMControlHTTP() {
  // If form data is received, update variables
  if (server.hasArg("mode")) {
    mode = server.arg("mode");
  }
  if (server.hasArg("motor1")) motor1 = server.arg("motor1").toInt();
  if (server.hasArg("motor2")) motor2 = server.arg("motor2").toInt();
  if (server.hasArg("motor3")) motor3 = server.arg("motor3").toInt();
  if (server.hasArg("motor4")) motor4 = server.arg("motor4").toInt();

  // Master control option
  bool masterEnabled = false;
  int masterValue = 0;
  if (server.hasArg("master_enable")) masterEnabled = true;
  if (server.hasArg("master")) masterValue = server.arg("master").toInt();

  if (masterEnabled) {
    motor1 = motor2 = motor3 = motor4 = masterValue;
  }

  // Build HTML page
  String html = "<!DOCTYPE html><html><head><title>ESP Config</title>";
  html += "<style>body{font-family:Arial;margin:20px;} .slider{width:300px;} </style>";
  html += "</head><body>";
  html += "<h1>Drone Configuration</h1>";

  // Show current mode
  html += "<p><b>Current Mode:</b> " + mode + "</p>";

  // Config form
  html += "<form action='/manual-pwm-control' method='GET'>";

  // Checkbox for manual mode
  html += "<p><input type='checkbox' name='mode' value='manual_motor_pwm_control'";
  if (mode == "manual_motor_pwm_control") html += " checked";
  html += "> Enable Manual Motor PWM Control</p>";

  // Master control checkbox + slider
  html += "<h3>Master Control</h3>";
  html += "<p><input type='checkbox' name='master_enable' value='1'";
  if (masterEnabled) html += " checked";
  html += "> Enable Master Slider (controls all motors)</p>";
  html += "Master: <input class='slider' type='range' name='master' min='0' max='100' value='" + String(masterValue) + "' oninput='mout.value=this.value'> <output id='mout'>" + String(masterValue) + "</output>%<br>";

  // Individual motor sliders
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


// webpage for PID GUI
void PIDControlHTTP() {
  // If form data is received, update variables
  if (server.hasArg("mode")) {
    mode = server.arg("mode");
  } else if (mode == "pid_control") {
    // if form didn’t send "mode" (checkbox unchecked), reset
    mode = "none";
  }

  if (server.hasArg("pitch_P_value")) pitch_P_value = server.arg("pitch_P_value").toFloat();
  if (server.hasArg("roll_P_value"))  roll_P_value  = server.arg("roll_P_value").toFloat();
  if (server.hasArg("yaw_P_value"))   yaw_P_value   = server.arg("yaw_P_value").toFloat();

  if (server.hasArg("pitch_I_value")) pitch_I_value = server.arg("pitch_I_value").toFloat();
  if (server.hasArg("roll_I_value"))  roll_I_value  = server.arg("roll_I_value").toFloat();
  if (server.hasArg("yaw_I_value"))   yaw_I_value   = server.arg("yaw_I_value").toFloat();

  if (server.hasArg("pitch_D_value")) pitch_D_value = server.arg("pitch_D_value").toFloat();
  if (server.hasArg("roll_D_value"))  roll_D_value  = server.arg("roll_D_value").toFloat();
  if (server.hasArg("yaw_D_value"))   yaw_D_value   = server.arg("yaw_D_value").toFloat();

  // Build HTML page
  String html = "<!DOCTYPE html><html><head><title>PID Config</title>";
  html += "<style>body{font-family:Arial;margin:20px;} input{margin:5px;}</style>";
  html += "</head><body>";
  html += "<h1>PID Control Configuration</h1>";

  // Form starts
  html += "<form action='/pid-control' method='GET'>";

  // Mode selection checkbox (INSIDE form now)
  html += "<p><input type='checkbox' name='mode' value='pid_control'";
  if (mode == "pid_control") html += " checked";
  html += "> Enable PID Control</p>";

  // P values
  html += "<h3>P Values</h3>";
  html += "Enter Pitch P value (current = " + String(pitch_P_value) + ") = <input type='text' name='pitch_P_value' value='" + String(pitch_P_value) + "'><br>";
  html += "Enter Roll  P value (current = " + String(roll_P_value)  + ") = <input type='text' name='roll_P_value' value='" + String(roll_P_value) + "'><br>";
  html += "Enter Yaw   P value (current = " + String(yaw_P_value)   + ") = <input type='text' name='yaw_P_value' value='" + String(yaw_P_value) + "'><br>";

  // I values
  html += "<h3>I Values</h3>";
  html += "Enter Pitch I value (current = " + String(pitch_I_value) + ") = <input type='text' name='pitch_I_value' value='" + String(pitch_I_value) + "'><br>";
  html += "Enter Roll  I value (current = " + String(roll_I_value)  + ") = <input type='text' name='roll_I_value' value='" + String(roll_I_value) + "'><br>";
  html += "Enter Yaw   I value (current = " + String(yaw_I_value)   + ") = <input type='text' name='yaw_I_value' value='" + String(yaw_I_value) + "'><br>";

  // D values
  html += "<h3>D Values</h3>";
  html += "Enter Pitch D value (current = " + String(pitch_D_value) + ") = <input type='text' name='pitch_D_value' value='" + String(pitch_D_value) + "'><br>";
  html += "Enter Roll  D value (current = " + String(roll_D_value)  + ") = <input type='text' name='roll_D_value' value='" + String(roll_D_value) + "'><br>";
  html += "Enter Yaw   D value (current = " + String(yaw_D_value)   + ") = <input type='text' name='yaw_D_value' value='" + String(yaw_D_value) + "'><br>";

  // Submit button
  html += "<br><input type='submit' value='Save PID Settings'>";
  html += "</form>";

  html += "</body></html>";

  server.send(200, "text/html", html);
}


// Webpage for Home GUI
void handleHomePageHTTP() {
  String html = "<!DOCTYPE html><html><head><title>ESP Drone Control</title>";
  html += "<style>body{font-family:Arial;margin:20px;} a{display:block;margin:10px 0;font-size:18px;}</style>";
  html += "</head><body>";
  html += "<h1>Drone Control Home</h1>";
  html += "<p>Select a configuration page:</p>";
  
  html += "<a href='/manual-pwm-control' target='_blank'>Manual PWM Control</a>";
  html += "<a href='/pid-control' target='_blank'>PID Control</a>";
  
  html += "</body></html>";
  
  server.send(200, "text/html", html);
}


// brushed ESC function
void brushedESCPWMControl() {
  if (mode == "manual_motor_pwm_control") {
    analogWrite(motorPin1, map(motor1, 0, 100, 0, 1023));
    analogWrite(motorPin2, map(motor2, 0, 100, 0, 1023));
    analogWrite(motorPin3, map(motor3, 0, 100, 0, 1023));
    analogWrite(motorPin4, map(motor4, 0, 100, 0, 1023));
  } else {
    analogWrite(motorPin1, 0);
    analogWrite(motorPin2, 0);
    analogWrite(motorPin3, 0);
    analogWrite(motorPin4, 0);
  }
}


void setup() {
  //Initialize Serial Communication
  Serial.begin(115200);
  
  //Wifi Connection indicator LED
  pinMode(CONNECTION_LED, OUTPUT);

  // Motor pins as output for PWM
  pinMode(motorPin1, OUTPUT);
  pinMode(motorPin2, OUTPUT);
  pinMode(motorPin3, OUTPUT);
  pinMode(motorPin4, OUTPUT);

  analogWriteRange(1023);      // default; keep if mapping 0–1023
  analogWriteFreq(5000);       // e.g., 5 kHz to reduce audible whine

  // Start an access point
  WiFi.mode(WIFI_AP);               
  WiFi.softAP(ssid, password);      

  Serial.println("Access Point Started");
  Serial.print("AP IP address: ");
  Serial.println(WiFi.softAPIP());

  // Setup web routes
  server.on("/", handleHomePageHTTP);
  server.on("/manual-pwm-control", manualPWMControlHTTP);
  server.on("/pid-control", PIDControlHTTP);


  server.begin();
  Serial.println("Web server started");
}

void loop() {

  //check the count of connected devices
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

    Serial.print("  pitch_P_value: "); Serial.print(pitch_P_value); Serial.print("  pitch_I_value: "); Serial.print(pitch_I_value); Serial.print("  pitch_D_value: "); Serial.println(pitch_D_value);
    Serial.print("  roll_P_value: "); Serial.print(roll_P_value); Serial.print("  roll_I_value: "); Serial.print(roll_I_value); Serial.print("  roll_D_value: "); Serial.println(roll_D_value);
    Serial.print("  yaw_P_value: "); Serial.print(yaw_P_value); Serial.print("  yaw_I_value: "); Serial.print(yaw_I_value); Serial.print("  yaw_D_value: "); Serial.println(yaw_D_value);
    
    Serial.println("=================================");
  }

  //Handle Brushed ESC Motor PWM Control
  brushedESCPWMControl();

   // Slow down the loop
   delay(100);


}



