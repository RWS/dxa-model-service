// todo DXA
# Getting started with the Content Delivery Service Container

Please find below brief instructions on how to setup and run the Content Delivery Service Container, as well as how to add new extensions and configurations.

## Start/stop the Content Delivery Service Container from the command line in Windows

To start the Content Delivery in Windows, please run the Power Shell script `.\bin\start.ps1`.
Closing the Power Shell script window used to start the Content Delivery Service Container would kill the process therefore stopping the container.

## Start/stop the Content Delivery Service Container from the command line in Linux/Unix

To start the Content Delivery Service Container in Linux/Unix, please run the Shell script `./bin/start.sh`.
To stop the Content Delivery Service Container in Linux/Unix, please run the Shell script `./bin/stop.sh`.

Before you will start your work with shell scripts, make sure you make the files executable.
Use 'chmod +x start.sh' or 'chmod +x stop.sh' for the respective scripts.

## How to add extensions and configurations to the Content Delivery Service Container

- Extensions can be added to the Content Delivery Service Container under the folder `./services`.
- Configurations can be added to the Content Delivery Service Container under the folder `./config`.

**NOTE:** After adding extensions and/or configurations to the Content Delivery Service Container, this needs to be restarted for these to take effect.

## HTTPS mode in the Content Delivery Service Container

The application is configured by default not to use https mode.
Also can be overriden by command line arguments:

- --https.enabled=true - to enable https mode (default: false)
- --https.keystore-path=keystore_location - to provide location of keystore file (default: config/keystore)
- --https.port=9999 - to override port for https (default: 8443)
- --https.key-alias=tomcat - to provide key alias (default: tomcat)
- --https.keystore-passwd=new_passwd - to provide keystore password (default: changeit)
- --https.truststore-passwd=some_passwd - to provide truststore password (default: changeit)

