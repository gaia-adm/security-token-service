FROM jetty:9.3.0-jre8

# DB is created under home directory of user jetty that must be created
RUN mkdir -p /home/jetty && chown jetty:jetty /home/jetty

COPY ./target/*.war $JETTY_BASE/webapps/
