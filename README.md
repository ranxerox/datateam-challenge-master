## Launching App

1) We need a Redis instance. Try running a Docker image:
		$ **docker run -p 0.0.0.0:32770:6379 -d redis**
Or course you may change the access port from 32770 to anyother, but it should be properly 
configured in file application.conf. This file should be in the same folder as the airporttest.jar

2) $ java -jar airporttest.jar
All necesaries dependencies are packed inside this jar, hence called fat-far.
Not any other jar or dependency (besides redis of course) is needed for running this app.


3) File structure: name files could be customized inside the application.conf file.
All the files are packed inside the resource folder (application.conf + .cvs's files)
But if these files are found in the same folder of the airporttest.jar they will be taken instead
(please refer to the self explanatory function getFileOrResource)

4) This is it. the UsersWithAirport.txt output file will be generated in the same folder as the .jar file

## Launching App With SBT

1) Run SBT on the folder where the built.sbt is locatd
java -Dsbt.log.noformat=true -Djline.terminal=jline.UnsupportedTerminal -Xmx512M -XX:MaxPermSize=256M -jar sbt-launch.jar
note: sbt-launch.jar shold be located in %HOMEPATH%\.IdeaIC2016.3\system\sbt\

2)
 for running test:
	>test
 
 for running app:
	>run

 for assembling the application(creating the stand alone airporttest.jar):
	>assembly

## The Project

Project was developed in IntelliJ+SBT+Scala.
You shold be able to import it.

I have commented in the code the highlights

#application.conf
The data.users_rows key in the config file is to limit the number of users to be loaded.
To load the whole file, just comment this entry (with #) of put the total number of users (1000000)

## Parallelisation
Since Redis intances are Single-Thread, doing parallelismo is not so simple as adding .par. into Scala's collections.
Parallelization can be achieved by creating multiple instances of redis (redis Cluster) and a more suitable Redis client like debasishg/scala-redis

## Notes about Testing
For testing the application it is necessary an running instance of Redis. 
I might not be suitable in the case of unit tests and this could be seen as an integration test but so far 
I didn't find a suitable way for mocking redis on a test env.




