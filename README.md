Not ready yet, will come soon





Get token request: POST http://localhost:9001/auth/oauth/token?grant_type=client_credentials&client_id=restapp&client_secret=secret
Get token response: {
                        "access_token": "0b5b5701-4301-4ef8-afd2-2579fd24f8e1",
                        "token_type": "bearer",
                        "scope": "read trust write"
                    }

Check token request: GET to http://localhost:9001/auth/oauth/check_token?token=24b70dd2-0815-4c79-8094-624150589d25
Check token response: {
                          "scope": [
                              "read",
                              "trust",
                              "write"
                          ],
                          "authorities": [
                              "ROLE_APP"
                          ],
                          "client_id": "restapp"
                      }


Spring Security links:
http://www.beingjavaguys.com/2014/10/spring-security-oauth2-integration.html

http://localhost:9000/mgs/oauth/token?grant_type=password&client_id=restapp&client_secret=restapp&username=user&password=password
http://localhost:9000/mgs/api/users/?access_token=75225ddc-788a-403e-98eb-e55d1b8a8aea
http://localhost:9000/mgs/oauth/token?grant_type=refresh_token&client_id=restapp&client_secret=restapp&refresh_token=a40e2794-475e-463c-9070-af0eb699e80e


ALSO can be useful: https://github.com/raonirenosto/silverauth



# metrics-gateway-service
Temporary metrics-gateway-service based on jax-rs and jersey with async I/O
This implementation is temporary, can be moved to another platform if we discover it is not robust and scalable enough.

TBD: docker file and service configuration file

How to run: build a war and put it on Jetty (tested with Jetty 9.2) or run mnn jetty:run

Functionality:
- Accept metrics publishing and store it into logs/metrics-storage.log. Only tenant id and metric name stored to the file.
- Set OAuth2 token for the provided tenant. Multiple tokens can be added.
- Get OAuth2 token(s) for the provided tenant.
- Monitor memory and number of threads consumed by this service

API:
- Publish metrics 
    - URL: /mgs/rest/v1/gateway/publish
    - Method: POST
    - Headers: Content-Type: application/json, Accept: application/json, Authorization: Bearer <oauth2 token>
    - Response code: 201
    - Body: 
    ``` json
    [{
      "metric":"metric-type(test,build,defect,scm)",
      "category":"automatic-test,commit,fork",
      "name":"test-name,job-name,defect-number,sha-of-commit",
      "source":"ci-server,qc-name/project,scm-repository",
      "timestamp":1432191000,
      "tags":["any.string.value.for.further.usage","any.string.value.for.further.usage"],
      "measurements":[{"name":"aut.build","value":968},{"name":"duration","value":350}],
      "events":[{"name":"status","value":"failed"},{"name":"runBy","value":"admin"}]
    }]
    ```
- Create oauth2 token for tenant
    - URL: /mgs/rest/auth/set/{tenantId}
    - Method: POST
    - Headers: Content-Type: application/json, Accept: application/json
    - Response code: 201
    - Parameters:
      - tenantId: String
      
  - Get oauth2 tokens for tenant
    - URL: /mgs/rest/auth/get/{tenantId}
    - Method: GET
    - Headers: Content-Type: application/json, Accept: application/json
    - Response code: 200
    - Parameters:
      - tenantId: String
      
  - Start/Stop monitoring memory and thread count every 1 second
    - URL: /mgs/rest/monitor/action/{action}
    - Method: GET
    - Headers: Content-Type: application/json, Accept: application/json
    - Response code: 200
    - Parameters:
      - action: String; on - start monitoring, off - stop monitoring
    
