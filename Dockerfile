FROM jetty:9.3.0-jre8

COPY ./target/*.war $JETTY_HOME/webapps/
