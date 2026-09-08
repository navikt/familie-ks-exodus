FROM amazoncorretto:26.0.2-alpine3.24

RUN mkdir /opt/app
EXPOSE 8080

COPY target/*.jar /opt/app/app.jar
COPY init-scripts/init.sh /opt/app/init.sh

WORKDIR /opt/app

CMD ["sh", "init.sh"]
