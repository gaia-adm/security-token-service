CircleCI build status: [![Circle CI](https://circleci.com/gh/gaia-adm/security-token-service.svg?style=svg)](https://circleci.com/gh/gaia-adm/security-token-service)

## Authorization server based on spring security.
- Supports Client Credentials flow of Oauth2 (https://tools.ietf.org/html/rfc6749#section-4.4)
- Runs on any servlet container or with Jetty embedded (mvn jetty:run on port 9001)

## Persistence layer
- Supports Etcd and H2 embedded
- Selected using environment variable: SPRING_PROFILES_ACTIVE=default (optional, works with Etcd if nothing provided) or SPRING_PROFILES_ACTIVE=db (mandatory for working with H2)
- When H2 database selected, DB file named sts_db sits locally under user home folder. The schema is created upon creation of the 1st client
- H2 DB accessible via the web console: http://localhost:9093/ (select H2 embedded, URL: jdbc:h2:~/sts_db)
- etcdUrl environment variable used to set the Etcd location. Complete URL should be passed (http://11.22.33.44:1234); if not set, default used (http://127.0.0.1:4001)

## Notes:
- Token never expires (can be revoked)

## Current limitations/not implemented yet:
- No deployment via fleetctl
- Distinguish between tenants/schemas based on token (token enrichment?)
- Client authentication is missing when requesting token or registering client

## Manually run with docker 
- docker build -t sts_build -f Dockerfile.build .
- Optional: docker rm build_cont
- docker create --name build_cont sts_build
- docker cp build_cont:/usr/local/gaia/target/sts.war ./target/
- docker build -t gaiaadm/sts .
- docker run -d --name sts -u jetty -p 9001:8080 -p 9093:9093 gaiaadm/sts
- Optional: check that server started as needed from outside of docker - curl -v http://localhost:9001/sts/oauth/check_token?token=62ad16cf-ab6c-42fa-af3d-359ecf98cdec <br />
***Base images used during the process***:
- maven:3.3.3-jdk-8 - build the project (https://registry.hub.docker.com/u/library/maven/)
- jetty:9.3.0-jre8 - run the server (https://registry.hub.docker.com/u/library/jetty/) <br />
***The above process is automated with buildAndRun.sh script***

## Flow:
- Create tenant
- Create client
- Create token
- Use token ("Authorization: Bearer <token>" header) in other services
***Refer curl.txt and SystemTestCurl.sh files to see the usage examples of ReST APIs***

## Example of java-based client service configuration
- Use token in your client - on example of Java webapp. NOT FINAL VERSION - works but should be cleaned up.
    **web.xml**:
        <context-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>
                /WEB-INF/spring-security.xml
            </param-value>
        </context-param>

        <listener>
            <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
        </listener>

        <servlet>
            <servlet-name>spring</servlet-name>
            <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
            <load-on-startup>1</load-on-startup>
            <async-supported>true</async-supported>
        </servlet>
        <servlet-mapping>
            <servlet-name>spring</servlet-name>
            <!--<url-pattern>/</url-pattern>-->
            <url-pattern>/</url-pattern>
        </servlet-mapping>

        <filter>
            <filter-name>springSecurityFilterChain</filter-name>
            <filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
            <async-supported>true</async-supported>
        </filter>
        <filter-mapping>
            <filter-name>InputFilter</filter-name>
            <url-pattern>/rest/v1/gateway/publish</url-pattern>
        </filter-mapping>
        <filter-mapping>
            <filter-name>springSecurityFilterChain</filter-name>
            <url-pattern>/*</url-pattern>
        </filter-mapping>

    **spring-security.xml** (${authServer} is in default.properties, can be reset via -D parameter):
```
    <context:property-placeholder system-properties-mode="OVERRIDE" location="classpath*:default.properties"/>
    <bean class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
        <property name="systemPropertiesMode" value="2" />
    </bean>

    <sec:authentication-manager alias="authenticationManager"/>

    <http auto-config="true" use-expressions="false" create-session="stateless" xmlns="http://www.springframework.org/schema/security">
        <csrf disabled="true"/>
        <anonymous enabled="false"/>
        <intercept-url pattern="/rest/v1/gateway/**" access="ROLE_APP"/>
        <custom-filter ref="resourceServerFilter" before="PRE_AUTH_FILTER"/>
    </http>

    <bean id="remoteTokenServices" class="org.springframework.security.oauth2.provider.token.RemoteTokenServices">
        <property name="checkTokenEndpointUrl" value="http://${authServer}/sts/oauth/check_token"/>
        <property name="restTemplate" ref="restTemplate"/>
    </bean>

    <bean id="restTemplate" class="org.springframework.web.client.RestTemplate">
        <constructor-arg name="messageConverters">
            <list>
                <bean class="org.springframework.http.converter.json.MappingJackson2HttpMessageConverter"/>
                <bean class="org.springframework.http.converter.FormHttpMessageConverter"/>
            </list>
        </constructor-arg>
    </bean>

    <oauth:resource-server id="resourceServerFilter" resource-id="test" token-services-ref="remoteTokenServices"/>
```

## More details
**See more details about Oauth2 in Oauth2-authorization.docx**
