Authorization server based on spring security.
- Supports Client Credentials flow of Oauth2 (https://tools.ietf.org/html/rfc6749#section-4.4)
- Runs on any servlet container or with Jetty embedded (mvn jetty:run on port 9001)
- Running with H2 DB embedded for persisting client configuration and access tokens.
  Schema name is auth_db under your user home folder. The schema is created upon creation of the 1st client
- H2 DB accessible via the web console: http://localhost:9093/ (select H2 embedded, URL: jdbc:h2:~/sts_db)

Notes:
- Token never expires (can be revoked)

Current limitations/not implemented yet:
- No deployment via fleetctl
- Distinguish between tenants/schemas based on token (token enrichment?)
- Client authentication when requesting token
- Authentication when registering client
- Logging

Manually run with docker:
- Build the image (it includes building the project also): docker build -t gaia/sts:0.1.0 .
- Run the container: docker run -d -p 9001:8080 gaia/sts:0.1.0 java -jar start.jar
- Quick check URL from outside of the container: curl -v http://localhost:9001/sts/oauth/check_token?token=62ad16cf-ab6c-42fa-af3d-359ecf98cdec


Flow:
- Create Client
    @POST to http://localhost:9001/sts/oauth/client
    Body example:
    {
        "client_id": "restapp",
        "client_secret": "secret",
        "scope": "trust",
        "authorized_grant_types": "client_credentials",
        "authorities": "ROLE_APP",
        "additional_information": "more data"
    }
-  Obtain token
    @POST to http://localhost:9001/sts/oauth/token?grant_type=client_credentials&client_id=restapp&client_secret=secret


- Use token in your client - on example of Java webapp. NOT FINAL VERSION - works but should be cleaned up.
    web.xml:
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

    spring-security.xml (${authServer} is in default.properties, can be reset via -D parameter):

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


Other API's exposed:
- Check token: @GET to http://localhost:9001/sts/oauth/check_token?token=<token>. This API is actually used by client
- Revoke token: @DELETE to http://localhost:9001/sts/oauth/token/revoke?token=<token>
- Get client details by id: @GET to http://localhost:9001/sts/oauth/client/<client_id>
- Get all registered clients: @GET to http://localhost:9001/sts/oauth/client
- Delete client: @DELETE to http://localhost:9001/sts/oauth/client/<client_id>

All APIs use application/json as Content-Type and Accept header values

See more details about Oauth2 in Oauth2-authorization.docx

