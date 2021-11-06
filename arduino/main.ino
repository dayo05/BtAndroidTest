#include <SoftwareSerial.h>
#include <DHT.h>

#define DHTPIN 2
#define DHTTYPE DHT22

SoftwareSerial btSerial(3, 4);
SoftwareSerial pmsSerial(5, 6);
unsigned char pms[32];
int pm_1_0, pm_2_5, pm_10_0;
DHT dht(DHTPIN, DHTTYPE);
int hum;
int temp;
void setup() {
  pinMode(13, OUTPUT);
  Serial.begin(9600);
  btSerial.begin(9600);
  pmsSerial.begin(9600);
  dht.begin();
  delay(2000);
}

void loop() {
    while(!btSerial.available()){delay(100);}
    digitalWrite(13, HIGH);
    Serial.println(btSerial.readString());
    hum = dht.readHumidity() * 100;
    temp = dht.readTemperature() * 100;

    if(pmsSerial.available() < 32) Serial.println("error");
    for(int i = 0; i < 32; i++)
        pms[i] = pmsSerial.read();
    
    if(pms[0] != 0x42 || pms[1] != 0x4d) Serial.println("0101");
    char* d = new char[100];
    sprintf(d, "%d|%d|%d|%d|%d", hum, temp, (pms[10] << 8) | pms[11], (pms[12] << 8) | pms[13], (pms[14] << 8) | pms[15]);
    Serial.println(d);
    btSerial.println(d);
    btSerial.flush();
    digitalWrite(13, LOW);
    delay(5000);
}
