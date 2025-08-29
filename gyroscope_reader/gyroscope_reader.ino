#include <Wire.h>

// Define Gyroscope related variables
float RatePitch, RateRoll, RateYaw;
float RateCalibrationPitch, RateCalibrationRoll, RateCalibrationYaw;
int RateCalibrationNumber;


// Gyro sensor reader function
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

void setup() {

  //start serial communication
  Serial.begin(115200);

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
  
}




void loop() {

  // ---------------- main control loop at fixed rate ----------------------
  static unsigned long lastMainLoop = 0;
  if (millis() - lastMainLoop >= 20) { // 50 Hz main control loop
    lastMainLoop = millis();

    // ----------- read Gyroscope Values------------
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


  // ---------------------------------------------------------------------------

}







