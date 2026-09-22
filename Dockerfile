FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25-dev

RUN mkdir /opt/app
EXPOSE 8080

COPY target/*.jar /opt/app/app.jar
COPY init-scripts/init.sh /opt/app/init.sh

WORKDIR /opt/app

CMD ["sh", "init.sh"]
