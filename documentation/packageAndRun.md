# Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

# Packaging and running the application

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
If you want to build and skip tests an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar -Dmaven.test.skip=true
```

The application is now runnable using `java -jar target/sergent-1.1.0-SNAPSHOT-runner.jar`.

In short, you can build (skip test) and run the application with this single command :
```sh
./mvnw package -Dquarkus.package.type=uber-jar -Dmaven.test.skip=true && java -jar target/sergent-1.1.0-SNAPSHOT-runner.jar
```

# Packaging native container in a linux+docker env
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
docker build -f src/main/docker/Dockerfile.native -t manaty/sergent .
```
