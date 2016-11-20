#!/bin/bash

CLIENT_NAME="restapp"
CLIENT_SECRET="secret"

# Format: validate $? <good_message> <bad_message>
function validate {
   if [ $1 -eq 0 ]; then
      echo '>>>' $2
   else
      echo '>>>' $3
      exit 1
   fi
}

##### login to ACM mock as a superuser
GAIATOKEN=$(curl http://localhost:3100/acms/mock)

##### create tenant in ACM mock
TENANT=$(curl -X POST -H 'Content-Type: application/json' --cookie 'gaia.token='$GAIATOKEN -d '{"name": "TestAccount","description": "Using for STS testing"}' http://localhost:3100/acms/api/accounts)
echo $TENANT | grep id
validate $? 'SUCCESS: Tenant created successfully: '$TENANT 'ERROR: Failed to create tenant, error received: '$TENANT

##### get tenant id for further usage
TENANT_ID=$(echo $TENANT | jq '.id')
echo Using tenant $TENANT_ID
validate $? 'SUCCESS: Successfully got tenant id '$TENANT_ID 'ERROR: Cannot get tenant id for tenant '$TENANT

##### create client
curl -i -H "Content-Type: application/json" -d '{"client_id": "'$CLIENT_NAME'","client_secret": "'$CLIENT_SECRET'","scope": "read,write,trust","authorized_grant_types": "client_credentials","authorities": "ROLE_APP","tenantId": '$TENANT_ID'}' http://localhost:9001/sts/oauth/client | grep '201 Created'
validate $? 'SUCCESS: Client '$CLIENT_NAME' created successfully' 'ERROR: Failed to create client '$CLIENT_NAME

#### get client details by id
curl -i -H "Accept: application/json" http://localhost:9001/sts/oauth/client/$CLIENT_NAME | grep '"client_secret":"'$CLIENT_SECRET'"' | grep '"tenantId":'$TENANT_ID
validate $? 'SUCCESS: Client '$CLIENT_NAME' details can be fetched' 'ERROR: Failed to fetch client details for client '$CLIENT_NAME

#### create token
TOKEN=$(curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X POST "http://localhost:9001/sts/oauth/token?grant_type=client_credentials&client_id=$CLIENT_NAME&client_secret=$CLIENT_SECRET" | grep 'access_token' | sed s/,/\\n/g | grep 'access_token' | sed -s 's/"access_token":\(.*\)/\1/'  | sed -r 's/\{//g' | sed -r 's/\"//g')
validate $? 'SUCCESS: Successfully created access_token for client '$CLIENT_NAME', its value is '$TOKEN 'ERROR: Cannot create access_token for client '$CLIENT_NAME

#### validate token
curl -i -H "Accept: application/json" http://localhost:9001/sts/oauth/check_token?token=$TOKEN | grep '200 OK'
validate $? 'SUCCESS: Token '$TOKEN' is valid' 'ERROR: Token '$TOKEN' is invalid'

#### revoke token
curl -i -X DELETE http://localhost:9001/sts/oauth/token/revoke?token=$TOKEN | grep '200 OK'
validate $? 'SUCCESS: Token '$TOKEN' revoked successfully' 'ERROR: Failed to revoke token '$TOKEN

#### validate after revoking
curl -i -H "Accept: application/json" http://localhost:9001/sts/oauth/check_token?token=$TOKEN | grep '400 Bad Request'
validate $? 'SUCCESS: Token '$TOKEN' is invalid after revoking' 'ERROR: Token '$TOKEN' is still valid after revoking'

#### create token again to validate it is removed when client deleted
TOKEN=$(curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X POST "http://localhost:9001/sts/oauth/token?grant_type=client_credentials&client_id=$CLIENT_NAME&client_secret=$CLIENT_SECRET" | grep 'access_token' | sed s/,/\\n/g | grep 'access_token' | sed -s 's/"access_token":\(.*\)/\1/'  | sed -r 's/\{//g' | sed -r 's/\"//g')
validate $? 'SUCCESS: Successfully created access_token for client '$CLIENT_NAME', its value is '$TOKEN 'ERROR: Cannot create access_token for client '$CLIENT_NAME

#### DB cleanup to enable re-run
curl -i -X DELETE http://localhost:9001/sts/oauth/client/$CLIENT_NAME | grep '204 No Content'
validate $? 'SUCCESS: OAUTH_CLIENT_DETAILS table is clean' 'ERROR: Failed to clean OAUTH_CLIENT_DETAILS table'
curl -i -H "Accept: application/json" http://localhost:9001/sts/oauth/check_token?token=$TOKEN | grep '400 Bad Request'
validate $? 'SUCCESS: Token '$TOKEN' is deleted after client deletion' 'ERROR: Token '$TOKEN' is still existing after client deletion'
curl -X DELETE -H 'Content-Type: application/json' --cookie 'gaia.token='$GAIATOKEN http://localhost:3100/acms/api/accounts/$TENANT_ID | grep 'Account successfully deleted'
validate $? 'SUCCESS: TENANT table is clean' 'ERROR: Failed to clean TENANT table'