# TODO: Replace with corporate JRE image when available
FROM amazoncorretto:21-alpine3.18-jre

RUN apk update && apk add --no-cache curl
VOLUME /tmp
RUN mkdir /app
ADD application/build/distributions/application-0.0.1-SNAPSHOT.tar /app/

# TODO: Add Datadog configurations when provided by the company

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "/app/application-0.0.1-SNAPSHOT/bin/application"]