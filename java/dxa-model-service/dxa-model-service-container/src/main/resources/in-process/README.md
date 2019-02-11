# Getting started with the Model Service Container
# Getting started with the Model Service Container

Please find below brief instructions on how to setup and run the Model Service Container.

## Building the Model Service and configuring it by using the Model Service installer 

Pre-requisites:
- Maven 3.2+. Only if you want `installer` to update your Maven repository. You need it only if you want to contribute to Model Service development.

Model Service is delivered in a pre-built package that is not ready to be used though. Since the service is open-source, we cannot deliver some sensitive dependencies with it.
You are required to run the installer to configure the Model Service before the first use. You will be asked about the SDL Web layout which is essential to finish building the working instance of the Model Service.

The following artifacts will be installed to this Model Service Container and to your Maven local repository (if Maven is installed on this machine):

- `com.sdl.web:discovery-registration-api:jar`
- `com.sdl.web:web-oauth:jar`
- `com.sdl.web:web-ambient-engine:jar`

The following artifacts will be installed to this Model Service Container but not to your maven Repository:

- `com.sdl.delivery:service-container-assembly:zip`

You can run it simply by executing `prepare.bat` or `prepare.sh` sctipr for Windows or *nix/MacOS environments respectively. Scripts are located in the `installer` folder. 
Refer to [documentation](https://docs.sdl.com/LiveContent/content/en-US/SDL%20DXA-v10/GUID-6DAAFE4F-05D0-4E51-88B0-87A611D5CBB7) for details on how to use installer tool.

## Re-configuring the working Model Service

Installer tool can re-configure the existing Model Service. Simply run installer as for the first time; installer will not ask about SDL Web layout again, and neither you need it for the repeating call.

## Using ActiveMQ

If you use ActiveMQ for cache invalidation in the in-process Model Service and if you use the Active MQ client with version 
**5.13.0** or higher, then add the following command line parameter, environment variable or JVM option to either *installService.ps1*,
*start.ps1* or *start.sh*:

    -Dorg.apache.activemq.SERIALIZABLE_PACKAGES=com.tridion.cache,org.apache.activemq,com.thoughtworks.xstream,java.lang


## Start/stop the Model Service Container from the command line in Windows

To start the Model in Windows, please run the Power Shell script `.\bin\start.ps1`.
Closing the Power Shell script window used to start the Model Service Container would kill the process therefore stopping the container.

## Start/stop the Model Service Container from the command line in Linux/Unix

To start the Model Service Container in Linux/Unix, please run the Shell script `./bin/start.sh`.
To stop the Model Service Container in Linux/Unix, please run the Shell script `./bin/stop.sh`.

Before you will start your work with shell scripts, make sure you make the files executable.
Use 'chmod +x start.sh' or 'chmod +x stop.sh' for the respective scripts.

## How to add extensions and configurations to the Model Service Container

- Extensions can be added to the Model Service Container under the folder `./services`.
- Configurations can be added to the Model Service Container under the folder `./config`.

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

