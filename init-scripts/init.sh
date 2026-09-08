export APP_DATASOURCE_URL=$(cat /secrets/oracle/config/jdbc_url)
export APP_DATASOURCE_USERNAME=$(cat /secrets/oracle/credentials/username)
export APP_DATASOURCE_PASSWORD=$(cat /secrets/oracle/credentials/password)

exec java -jar app.jar