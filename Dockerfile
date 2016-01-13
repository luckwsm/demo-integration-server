FROM java:8
MAINTAINER Chuck Swanberg <cswanberg@mad-swan.com>
EXPOSE 8080
ENV PORT 8080


ADD . /src
WORKDIR /src

RUN ./gradlew clean shadowJar
RUN mkdir /app
RUN cp /src/build/libs/params-api-0.0.1-SNAPSHOT-fat.jar /app
RUN rm -rf /src

WORKDIR /app
CMD ["/usr/bin/java", "-jar", "/app/params-api-0.0.1-SNAPSHOT-fat.jar"]