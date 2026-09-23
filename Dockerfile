FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-27-dev

COPY --chown=nonroot:nonroot target/*.jar /app/app.jar
COPY --chown=nonroot:nonroot init-scripts/init.sh /app/init.sh

WORKDIR /app

ENV TZ="Europe/Oslo"

EXPOSE 8080

ENTRYPOINT ["sh", "init.sh"]
