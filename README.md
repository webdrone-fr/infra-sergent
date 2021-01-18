# sergent project

Sergent is a (Quarkus)[https://quarkus.io/] project that allow to remotely admnistrate our servers.

sergent should be deployed on any server we want to administrate.

The environment variable `SERGENT_COMMAND_PATH` must contain the working path for the commands that Sergent will execute.

## Command end point
The role of sergent is to affer an endpoint that allow to execute management commands on the server

Path : `sergent?command=<command-name>`

to list all available commands use `list` command name

```
https://wdintegration.site/sergent?command=list
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `sergent-1.0-runner.jar` file in the `/target` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/lib` directory.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application is now runnable using `java -jar target/sergent-1.0-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/sergent-1.0-runner`


## Packaging native container in a linux+docker env
You need at least 4Go free memory to perform the build


In Debian buster install java
```
sudo apt install openjdk-11-jdk
echo "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/" > /etc/profile.d/java.sh
source /etc/profile.d/java.sh
```

Install maven
```
sudo apt install maven
echo "export MAVEN_HOME=/usr/share/maven" > /etc/profile.d/maven.sh
source /etc/profile.d/maven.sh
```

Clone this repo and change permission on mvnw
```
cd home
git clone 
```

Package the app in a native container
```
cd infra-sergent
./mvnw package -Pnative -Dquarkus.native.container-build=true
docker build -f src/main/docker/Dockerfile.native -t webdrone/sergent .
```

## Building native docker image using Jenkins

Run [Jenkins job](https://jenkins.webdrone.fr/view/7%20-%20CrawlServices/job/dck_sergent/)

To pull the image up in the server, run these commands:
```
docker login nexus.webdrone.fr
docker pull nexus.webdrone.fr/sergent
```
