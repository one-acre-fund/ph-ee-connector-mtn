
FROM eclipse-temurin:17 AS build

WORKDIR /ph-ee-connector-mtn

COPY  . .


RUN ./gradlew bootJar

FROM eclipse-temurin:17

EXPOSE 5000
EXPOSE 8080

RUN mkdir /app

COPY --from=build /ph-ee-connector-mtn/build/libs/*.jar /app/

ENTRYPOINT ["java", "-jar" ,"/app/ph-ee-connector-mtn.jar"]