FROM openjdk:8-jre-alpine

ENV APPLICATION_USER fiber
RUN adduser -D -g '' $APPLICATION_USER

RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

USER $APPLICATION_USER

COPY ./build/export /app/lib

COPY ./build/libs/app.fiber-gateway.jar /app/app.jar
WORKDIR /app

CMD ["java", "-cp",  "lib/*:app.jar", "app.fiber.GatewayKt"]