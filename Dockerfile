FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -B

COPY src ./src

RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["sh","-c","java -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true -jar target/*.jar"]