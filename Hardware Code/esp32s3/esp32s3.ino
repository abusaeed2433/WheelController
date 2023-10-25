#include<WiFi.h>
#include<Firebase_ESP_Client.h>
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"

#define WIFI_SSID "NR-WIFI"
#define WIFI_PASSWORD "NaiNaiNai"
#define API_KEY "AIzaSyDys-uAXF0uniP2Bl5sjStXivnLC3ILx4I"
#define DATABASE_URL "https://wheelcontroller-25adc-default-rtdb.firebaseio.com/"

FirebaseData fbdo_s1, fbdo_s2;
FirebaseAuth auth;
FirebaseConfig config;

bool signupOK = false;
int value = 0;
int timer = 0;

void setup() {
  Serial.begin(115200);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(300);
  }
  Serial.println();
  Serial.print("Connected with IP: ");
  Serial.println(WiFi.localIP());
  Serial.println();

  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  if (Firebase.signUp(&config, &auth, "", "")) {
    Serial.println("SignUp OK");
    signupOK = true;
  } else {
    Serial.printf("%s\n", config.signer.signupError.message.c_str());
  }

  config.token_status_callback = tokenStatusCallback;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  if (!Firebase.RTDB.beginStream(&fbdo_s1, "/commands/123456/my_command/command")) {
    Serial.printf("stream 1 begin error, %s\n\n", fbdo_s1.errorReason().c_str());
  } else {
    Serial.println("OK1");
  }

  if (!Firebase.RTDB.beginStream(&fbdo_s2, "/commands/123456/my_command/time")) {
    Serial.printf("stream 2 begin error, %s\n\n", fbdo_s2.errorReason().c_str());
  } else {
    Serial.println("OK2");
  }

}

void loop() {
  if (Firebase.ready() && signupOK) {
    //Serial.println("OK3");
    if (!Firebase.RTDB.readStream(&fbdo_s1)) {
      Serial.printf("stream 1 read error, %s\n\n", fbdo_s1.errorReason().c_str());
    }
    if (!Firebase.RTDB.readStream(&fbdo_s2)) {
      Serial.printf("stream 2 read error, %s\n\n", fbdo_s2.errorReason().c_str());
    }

    if (fbdo_s2.streamAvailable()) {
      if (fbdo_s1.dataType() == "int" && fbdo_s2.dataType() == "int") {
        timer = fbdo_s2.intData();
        value = fbdo_s1.intData();
        Serial.println("Successful READ from " + fbdo_s2.dataPath() + ": " + timer + " (" + fbdo_s2.dataType() + ")");
        Serial.println("Successful READ from " + fbdo_s1.dataPath() + ": " + value + " (" + fbdo_s1.dataType() + ")");
        if(value == 1){
          Serial.println("Left");
        }
        else if(value == 2){
          Serial.println("Right");
        }
        else if(value == 3){
          Serial.println("Up");
        }
        else if(value == 4){
          Serial.println("Down");
        }
        else if(value == 5){
          Serial.println("Stop");
        }
        else{
          Serial.println("Invalid Input");
        }
      }
    }
  }
}
