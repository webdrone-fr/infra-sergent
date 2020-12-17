if java -version | grep -q "java version" ; then
  echo "Java installed."
else 
  echo "Installing Java"
  sudo apt install  -y openjdk-11-jdk 
fi

if [ -z "$JAVA_HOME" ]
then
  echo "set \$JAVA_HOME"
  echo "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/" > /etc/profile.d/java.sh
  source /etc/profile.d/java.sh
fi

if mvn -version | grep -q "maven version" ; then
  echo Maven installed."
else
  echo "Installing Maven"
  sudo apt install  -y maven
fi

if [ -z "$MAVEN_HOME" ]
then
  echo "setting \$MAVEN_HOME"
  echo "export MAVEN_HOME=/usr/share/maven" > /etc/profile.d/maven.sh
  source /etc/profile.d/maven.sh
fi

./mvnw package -Pnative -Dquarkus.native.container-build=true


