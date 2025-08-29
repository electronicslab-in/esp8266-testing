#include <Wire.h>                     // I2C Communication Library
#include <ESP8266WiFi.h>              // ESP2866 WIFI Library
#include <ESPAsyncUDP.h>              // Asynch UDP Data listener / Sender Library -> https://github.com/me-no-dev/ESPAsyncUDP


// Step A - Define Wifi realted variables
const int CONNECTION_LED = LED_BUILTIN; // For checking device connection status -> Onboard LED (GPIO2, also called D2 active LOW)
const char *ssid = "ESP8266_AP";        //Wifi Access point variables
const char *password = "12345678";

AsyncUDP udp;                          // Creating UDP Connection Instance


// Step B - Joystick Receiver realted variables
String latestPacket = "";                         //  Global buffer for latest joystick data
volatile bool newPacketAvailable = false;         // flag for updated joystick UDP PORT data



// Step C - Define Gyroscope related variables
float RatePitch, RateRoll, RateYaw;
float RateCalibrationPitch, RateCalibrationRoll, RateCalibrationYaw;
int RateCalibrationNumber;

// Step D - Define PID related variables
float DesiredRatePitch, DesiredRateRoll, DesiredRateYaw;          // remote controller recevied values are normalized to convert to these values  -> more like the user joystick input in degree / seconds
float ErrorRatePitch, ErrorRateRoll, ErrorRateYaw;                // current Error = Desired - Measured for kth cycle
float InputThrottle, InputPitch, InputRoll, InputYaw;             // calculated PID outpur
float PrevErrorRatePitch, PrevErrorRateRoll, PrevErrorRateYaw;    // previous Error -> (k - 1)th cycle
float PrevIntermRatePitch, PrevIntermRateRoll, PrevIntermRateYaw;
float PIDReturn[] = {0, 0, 0};

float PRatePitch = 0.6; float IRatePitch = 3.5; float DRatePitch = 0.03;
float PRateRoll = 0.6; float IRateRoll = 3.5; float DRateRoll = 2;
float PRateYaw = 2; float IRateYaw = 12; float DRateYaw = 0;

// Step E - ESC input (brused or brusheless)  variables
float MotorInput1, MotorInput2, MotorInput3, MotorInput4;




// Define Gyro sensor reader function
void gyro_signals(void) {
  
  // switch on the low pass filter
  Wire.beginTransmission(0x68);   //Start I2C communication with Gyroscope at address 0x68
  Wire.write(0x1A);             // 0x1A -> register address of low pass filter 
  Wire.write(0x05);             //, 0x05 -> low pass filter cutoff frequency of 10Hz
  Wire.endTransmission();       // End transmission

  // set the sensitivity scale of sensor
  Wire.beginTransmission(0x68);   //Start I2C communication with Gyroscope at address 0x68
  Wire.write(0x1B);             // 0x1A -> register address of sensitivity scale 
  Wire.write(0x8);             // 0x8 -> value of sensitivity scale => LSB Value -> 65.5 LSB per degree per seconds
  Wire.endTransmission();       // End transmission

  // read the measurement value of gyro
  Wire.beginTransmission(0x68);   //Start I2C communication with Gyroscope at address 0x68
  Wire.write(0x43);               //0x43 + 0x44 ->  Gyro_XOUT[15:8] + Gyro_XOUT[7:0] (similarly 0x45 + 0x46 ->  Gyro_YOUT[15:8] + Gyro_YOUT[7:0] , 0x47 + 0x48 ->  Gyro_ZOUT[15:8] + Gyro_ZOUT[7:0] )
  Wire.endTransmission();       // End transmission

  // Request 6 bytes
  Wire.requestFrom(0x68,6);
  int16_t GyroX = Wie.read()<<8 | Wire.read(); // Read -> left shift -> read -> OR 
  int16_t GyroY = Wie.read()<<8 | Wire.read(); // Read -> left shift -> read -> OR 
  int16_t GyroZ = Wie.read()<<8 | Wire.read(); // Read -> left shift -> read -> OR 

  // Normalize based on sensitivity factor
  RateRoll = (float) GyroX / 65.5;          // convert the measurement units to degree/second
  RatePitch = (float) GyroY / 65.5;          // convert the measurement units to degree/second
  RateYaw = (float) GyroZ / 65.5;          // convert the measurement units to degree/second
}


// Define PID Controller Function
void PID_Controller(float Error, float PrevError, float P, float I, float D, float PrevIterm ) {
  // Kp * (Desired - Actual) = Kp * Error
  float Pterm = P * Error;

  // Ki * Time Integration of Error = del_Error * del_T
  // Now, del_Error(k) / del_t(k) = [Error(k) - Error(k-1)] / [t(k) - t(k-1)] 
  // Error(k) =  Error(k-1) + del_Error(k) * [t(k) - t(k-1)]
  // del_t = T = 4 milliseconds = 0.004
  float Iterm = PrevIterm + I * ( Error + PrevError) * 0.004 / 2;

  if (Iterm > 400) Iterm = 400;
  else if(Iterm < -400) Iterm = -400;


  float Dterm = D * ( Error - PrevError ) / 0.004;

  float PIDoutput = Pterm + Iterm + Dterm;

  if (PIDoutput > 400) PIDoutput = 400;
  else if (PIDoutput < -400) PIDoutput = -400;

  PIDReturn[0] = PIDoutput;
  PIDReturn[1] = Error;
  PIDReturn[2] = Iterm;

}

// Defind Reset Function for PID variables
void reset_PID(void) {
  PrevErrorRatePitch = 0;
  PrevErrorRateRoll = 0;
  PrevErrorRateYaw = 0;

  PrevIntermRatePitch = 0;
  PrevIntermRateRoll = 0;
  PrevIntermRateYaw = 0;
}


void setup {

  // ---------------------- Serial Monitor Related Setup ------------------------------------ 
  Serial.begin(115200);           // selecting baud rate
  // ---------------------- Serial Monitor Related Setup Ends ------------------------------------


  // ---------------------- WIFI Access Point Related Setup Starts --------------------------------------
  Serial.begin(115200);
  WiFi.mode(WIFI_AP);
  WiFi.softAP(ssid, password);

  Serial.print("AP IP: ");
  Serial.println(WiFi.softAPIP());

  pinMode(CONNECTION_LED, OUTPUT);
  digitalWrite(CONNECTION_LED, HIGH);  // solid OFF - Wifi Connection indicator LED - initially onboard LED is kept off (can be modified to blink)


  // ---------------------- WIFI Access Point Related Setup ENDS --------------------------------------


  // ------------------------ UDP Asynchronous Listener Setup -------------------------------------

  if (udp.listen(4210)) {
    Serial.println("UDP Listening on port 4210");

    udp.onPacket([](AsyncUDPPacket packet) {
      // Just store latest data, overwrite older
      latestPacket = String((char*)packet.data()).substring(0, packet.length());
      newPacketAvailable = true;
    });
  }

  // ------------------------------- UDP Asynchronous Listener Setup ENDS ------------------------------------------



  //--------------------------- Gyroscope Setup Starts ----------------------------------------------------

  //Start I2C Communication
  pinMode(13, OUTPUT);          // I2C Connection PIN - VERIFY
  digitalWrite(13, HIGH);
  Wire.setClock(400000);        // I2C Clock Speed -> 400kHz
  Wire.begin();
  delay(250);

  Wire.beginTransmission(0x68);   //Start I2C communication with Gyroscope at address 0x68
  Wire.write(0x6B);             // 0x6B -> register address of Power Management
  Wire.write(0x00);             // 0x00 -> set power management register to start
  Wire.endTransmission();       // End transmission

 // Gyroscope calibration by taking average of initial 200 values and deleting it from the next measured values; 
  for(RateCalibrationNumber = 0; RateCalibrationNumber < 2000; RateCalibrationNumber ++) {
    gyro_signals();

    RateCalibrationRoll = RateCalibrationRoll + RateRoll;
    RateCalibrationPitch = RateCalibrationPitch + RatePitch;
    RateCalibrationYaw = RateCalibrationYaw + RateYaw;
    delay(1);

  }

  RateCalibrationRoll = RateCalibrationRoll / 2000;   // averaging the calibration errors
  RateCalibrationPitch = RateCalibrationPitch / 2000;
  RateCalibrationYaw = RateCalibrationYaw / 2000;

  //--------------------------- Gyroscope Setup Ends ----------------------------------------------------











// ------------------------ 


}




Void loop {

  //---------wifi client (REMOTE) device connection test loop ---------------------------------
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

  

  // ---------------- main control loop at fixed rate ----------------------
  static unsigned long lastMainLoop = 0;
  if (millis() - lastMainLoop >= 20) { // 50 Hz main control loop
    lastMainLoop = millis();

    // ---------- step 1. Update the recived UDP Inputs ----------------
    if (newPacketAvailable) {
      newPacketAvailable = false;
      Serial.print("Using joystick data: ");
      Serial.println(latestPacket);
      // parse joystick → update PID target here
    } else {
      // No new packet → keep last command
    }
  


    // -------------  step 1. read Gyroscope Values --------------------
    gyro_signals();
    RateRoll = RateRoll - RateCalibrationRoll; //remove the calculated Average calibration error
    RatePitch = RatePitch - RateCalibrationPitch;
    RateYaw = RateYaw - RateCalibrationYaw;

    Serial.print("RatePitch: ");
    Serial.print(RatePitch, 2);   // 2 = decimal places
    Serial.print(" [°/s], RateRoll: ");
    Serial.print(RateRoll, 2);
    Serial.print(" [°/s], RateYaw: ");
    Serial.print(RateYaw, 2);
    Serial.println(" [°/s]");

  }





}

