FROM gaia/basejava:0.1.0

ADD . $GAIA_HOME
WORKDIR $GAIA_HOME

RUN mvn clean install
RUN cp $GAIA_HOME/target/*.war $JETTY_HOME/webapps

WORKDIR $JETTY_HOME
EXPOSE 8080
# RUN java -jar start.jar
CMD ["/bin/bash"]