#include <Arduino_FreeRTOS.h>
#include <Servo.h>
#include <SimpleDHT.h>

//***********************************************************
//*******We're using FreeRTOS multi-tasking in Arduino*******
//*******Dwayne Hoang - Android Things Andy Robot************
//**************Published at 2017-11-21**********************
//***********************************************************

//Pan Tilt's Parameters
Servo myservo1;
Servo myservo2;
int data = 1;
int pos = 1;

//Ultrasonic Sensor's Parameters
const int trigPin = 10;
const int echoPin = 11;
//Variables for the durantion and the distance
long duration;
int distance;

//The Flame Module 's Parameters
boolean isCheck = true;
int isFlamePin = 2;  // This is our input pin
int isFlame = HIGH;  // HIGH MEANS NO FLAME

//The Gas Sensor MQ-2's Parameters
int smokeA0 = A4;
int sensorThres = 400;

//The temperature sensor DHT11
int pinDHT11 = 4;
SimpleDHT11 dht11;

//There are the values which will decide when alternately perform 
//rotating  Pantilt from 15-165 degree or move pantilt dely on
// the coordinates was received from RPI
int w = 1;
int h = 1;

//***********************************************************
//********************DEFINES THE TASKS**********************
//***********************************************************
void TaskFLame( void *pvParameters );  
void TaskTemperature( void *pvParameters );
void TaskUltrasonic( void *pvParameters);


//***********************************************************
//**********THE SETUP FUNCTIONS RUN ONCE WHEN****************
//**********YOU PRESS RESET OR POWER ON BOARD****************
//***********************************************************
void setup() {

  // initialize serial communication at 115200 bits per second:
  Serial.begin(115200);

  while (!Serial) {
    ; // wait for serial port to connect. Needed for native USB, on LEONARDO, MICRO, YUN, and other 32u4 based boards.
  }

  // Now set up 3 tasks to run independently.
  xTaskCreate(
    TaskFLame
    ,  (const portCHAR *)"FlameSensor"   // A name just for humans or another name you want to write.
    ,  128  // This stack size can be checked & adjusted by reading the Stack Highwater
    ,  NULL
    ,  3 // Priority, with 3 (configMAX_PRIORITIES - 1) being the highest, and 0 being the lowest.
    ,  NULL );

  xTaskCreate(
    TaskTemperature
    ,  (const portCHAR *) "TemperatureSensor"
    ,  128  // Stack size
    ,  NULL
    ,  2 // Priority
    ,  NULL );

  xTaskCreate(
    TaskUltrasonic
    ,  (const portCHAR *) "Ultrasonic"
    ,  128  // Stack size
    ,  NULL
    , 1 // Priority
    ,  NULL );

  // Now the task scheduler, which takes over control of scheduling individual tasks, is automatically started.
}

void loop()
{
  // Empty. Things are done in Tasks.
}

/*--------------------------------------------------*/
/*--------------FLAME SENSOR Tasks -----------------*/
/*--------------------------------------------------*/

void TaskFLame(void *pvParameters)  // This is a task.
{
  (void) pvParameters;

  // initialize digital LED_BUILTIN on pin 13 as an output.
  pinMode(isFlamePin, INPUT);
  pinMode(smokeA0, INPUT);

  for (;;) // A Task shall never return or exit.
  {
    // THE FLAME SENSOR VS THE GAS SENSOR
      isFlame = digitalRead(isFlamePin);
      int isGasSmoke = analogRead(smokeA0);

      if (isFlame== LOW|| isGasSmoke > sensorThres)
      {
        while(isCheck == true)
        {
         
               Serial.print("FLAME");   
               isCheck = false; 
          
               Serial.print("GAS");   
               isCheck = false; 
       
        }
      }else{ isCheck = true;}
      
       vTaskDelay(3000 / portTICK_PERIOD_MS);
     }
}


/*--------------------------------------------------*/
/*----TEMPERATURE AND HUMIDITY SENSOR Tasks --------*/
/*--------------------------------------------------*/
void TaskTemperature(void *pvParameters)  // This is a task.
{
  (void) pvParameters;

  for (;;)
  {
    byte temperature = 0;
    byte humidity = 0;
    int err = SimpleDHTErrSuccess;
    
    if ((err = dht11.read(pinDHT11, &temperature, &humidity, NULL)) != SimpleDHTErrSuccess)
    {
      Serial.print("Read DHT11 failed, err="); 
      Serial.println(err);
      vTaskDelay(10000 / portTICK_PERIOD_MS);
      return;
    }
  
    Serial.print((int)temperature);Serial.print("|");Serial.print((int)humidity); 
  
    // DHT11 sampling rate is 1HZ.
    vTaskDelay(10000 / portTICK_PERIOD_MS);

 }
}

/*--------------------------------------------------*/
/*--------PANTILT, ULTRASONIC SENSOR Tasks ---------*/
/*--------------------------------------------------*/

void TaskUltrasonic(void *pvParameters)
{
  (void) pvParameters;
 
  // initialize Ultrasonic Sensor
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);
  myservo1.attach(5);
  myservo2.attach(6);

  for (;;) // A Task shall never return or exit.
  {

  //For Pantil's XY coordinates
  if(h != 0 && w != 0){
         while (Serial.available() > 0) //Check if byte available from USB Port
        {
            int width = Serial.parseInt();
            int height = Serial.parseInt();
 
            if (Serial.read() == '\n') 
            {
                  int posH = map(width, 1, 640, 15, 165);
                  int posW = map(height, 1, 480, 15, 165);
                  myservo1.write(posH);
                  myservo2.write(posW);
                  w = width;
                  h = height;         
            }
        }
  }

    //For Regularly Rotating Pantils from 15 - 165 degree and opposite.
    if(h == 0 && w == 0){
      // rotates the servo motor from 15 to 165 degrees
      for(int i=15;i<=165;i++){  
        recievedDataListener();  //Check whether have any value is being sended or not whilt Pantil is regularly running
          myservo1.write(60); //Keep for Servo1 step-motor located at a position only
          myservo2.write(i);
          vTaskDelay(90 / portTICK_PERIOD_MS);
          distance = calculateDistance();// Calls a function for calculating the distance measured by the Ultrasonic sensor for each degree
  
          Serial.print(i); // Sends the current degree into the Serial Port
          Serial.print(","); // Sends addition character right next to the previous value needed later in the Processing IDE for indexing
          Serial.print(distance); // Sends the distance value into the Serial Port
          Serial.print("."); // Sends addition character right next to the previous value needed later in the Processing IDE for indexing
      }
    }
     if(h == 0 && w == 0){
      // Repeats the previous lines from 165 to 15 degrees
      for(int i=165;i>15;i--){
        recievedDataListener(); 
          myservo1.write(60);
          myservo2.write(i);
          vTaskDelay(90 / portTICK_PERIOD_MS);
          distance = calculateDistance();
          Serial.print(i);
          Serial.print(",");
          Serial.print(distance);
          Serial.print(".");
      }              
    }

  }
 
}

//*******************************************************************
//******************************SUB-FUNCTIONS************************
//*******************************************************************
//Function for calculating the distance measured by the Ultrasonic Sensor
int calculateDistance()
{
  digitalWrite(trigPin, LOW);
  vTaskDelay(2 / portTICK_PERIOD_MS);
  // Sets the trigPin on HIGH state for 10 micro seconds
  digitalWrite(trigPin, HIGH);
  vTaskDelay(10 / portTICK_PERIOD_MS);
  digitalWrite(trigPin, LOW);
  
  // Reads the echoPin, returns the sound wave travel time in microseconds
  duration = pulseIn(echoPin, HIGH); 
  distance = duration * 0.034 / 2;
  return distance;
}

//Recieve Data Listener
void recievedDataListener()
{
  
         while (Serial.available() > 0)
        {
            int width = Serial.parseInt();
            int height = Serial.parseInt();
 
            if (Serial.read() == '\n') 
            {
                  int posH = map(width, 1, 640, 15, 165);
                  int posW = map(height, 1, 480, 15, 165);
                  myservo1.write(posH);
                  myservo2.write(posW);
                  w = width;
                  h = height;   
            }

        }
}












