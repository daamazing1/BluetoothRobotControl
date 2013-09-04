Bluetooth Robot Controller
==========================

This is a simple Android application which uses Bluetooth to interact with an Arduino robot as a remote control.  

The arduino is using an HC-05 Bluetooth module configured as a slave. We are using serial communication to send commands
to Arduino which is running a scketch waiting for input via a serial connection provided by the HC-05 module.

For now there are only a couple of simple commands:
- Forward (8)
- Backwards (2)
- In-place turn right (4)
- In-place turn left (5)

Future enchancments planned are to add more commands to:
- Control turning speed
- Control forward and backward speeds
- Allow for gradual turning
