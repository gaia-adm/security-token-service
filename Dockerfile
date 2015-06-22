FROM gaiaadm/basejava:0.1.0

ENV GAIA_HOME=/gaia foo=boo
RUN mkdir -p  $GAIA_HOME

ADD . $GAIA_HOME
WORKDIR $GAIA_HOME

RUN mvn clean install
RUN cp $GAIA_HOME/target/*.war $JETTY_HOME/webapps

WORKDIR $JETTY_HOME
EXPOSE 8080

# CMD ["/bin/bash"]
CMD java -jar start.jar
