#include <Wire.h>
#include <Adafruit_MotorShield.h>
#include "utility/Adafruit_PWMServoDriver.h"
#include <SoftwareSerial.h>

#define RXD_PIN 7
#define TXD_PIN 6
#define TURN_LEFT '4'
#define TURN_RIGHT '6'
#define MOVE_FORWARD '8'
#define MOVE_REVERSE '2'
#define STOP_MOTORS '5'

SoftwareSerial BlueTooth(RXD_PIN,TXD_PIN);
Adafruit_MotorShield AFMS = Adafruit_MotorShield(); 
Adafruit_DCMotor *rightMotor = AFMS.getMotor(1);
Adafruit_DCMotor *leftMotor = AFMS.getMotor(4);

int BluetoothData;
int leftSpeed = 150;
int rightSpeed = 150;

void setup()
{
	BlueTooth.begin(9600);
	AFMS.begin();

	leftMotor->setSpeed(150);
	rightMotor->setSpeed(150);

	BlueTooth.println("Setup Complete");
}

void loop()
{
	if (BlueTooth.isListening()) {
		while(!BlueTooth.available());
		switch (BlueTooth.read())
		{
		case TURN_LEFT:
			BlueTooth.println("TURN LEFT");
			leftMotor->run(FORWARD);
			rightMotor->run(FORWARD);
			break;
		case TURN_RIGHT:
			BlueTooth.println("TURN RIGHT");
			leftMotor->run(BACKWARD);
			rightMotor->run(BACKWARD);
			break;
		case MOVE_FORWARD:
			BlueTooth.println("FORWARD");
			leftMotor->run(FORWARD);
			rightMotor->run(BACKWARD);
			break;
		case MOVE_REVERSE:
			BlueTooth.println("REVERSE");
			leftMotor->run(BACKWARD);
			rightMotor->run(FORWARD);
			break;
		case STOP_MOTORS:
			BlueTooth.println("STOP");
			leftMotor->run(RELEASE);
			rightMotor->run(RELEASE);
			break;
		}
		delay(50);
	}
	else{
		BlueTooth.println("CONNECTION LOST");
		leftMotor->run(RELEASE);
		rightMotor->run(RELEASE);
	}
}
