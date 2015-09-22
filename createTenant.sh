#!/bin/bash
# simplified version of SystemTestCurl.sh that doesn't cleanup after itself to allow configuration of agent against Gaia

CLIENT_NAME="gaia_agent"
CLIENT_SECRET="secret"
TENANT_ADMIN_NAME="gaia_agent@hp.com"

# Format: validate $? <good_message> <bad_message>
function validate {
   if [ $1 -eq 0 ]; then
      echo '>>>' $2
   else
      echo '>>>' $3
      exit 1
   fi
}

#### DB cleanup to enable re-run
curl -i -X DELETE http://localhost:9001/sts/oauth/client/$CLIENT_NAME | grep '204 No Content'
validate $? 'SUCCESS: OAUTH_CLIENT_DETAILS table is clean' 'ERROR: Failed to clean OAUTH_CLIENT_DETAILS table'
TENANT_ID=$(curl -H "Accept: application/json" http://localhost:9001/sts/tenant?user=$TENANT_ADMIN_NAME | grep 'tenantId' | sed s/,/\\n/g | grep 'tenantId' | sed -s 's/"tenantId":\(.*\)/\1/'  | sed -r 's/\{//g')
if [ ! -z "$TENANT_ID" ]; then
   echo Deleting tenant $TENANT_ID
   curl -i -X DELETE http://localhost:9001/sts/tenant/$TENANT_ID  | grep '204 No Content'
   validate $? 'SUCCESS: TENANT table is clean' 'ERROR: Failed to clean TENANT table'
fi

##### create tenant
curl -i -H "Content-Type: application/json" -d '{"adminUserName": "'$TENANT_ADMIN_NAME'"}' http://localhost:9001/sts/tenant | grep '201 Created'
validate $? 'SUCCESS: Tenant created successfully by admin '$TENANT_ADMIN_NAME 'ERROR: Failed to create tenant by admin '$TENANT_ADMIN_NAME

##### get tenant id for further usage
TENANT_ID=$(curl -H "Accept: application/json" http://localhost:9001/sts/tenant?user=$TENANT_ADMIN_NAME | grep 'tenantId' | sed s/,/\\n/g | grep 'tenantId' | sed -s 's/"tenantId":\(.*\)/\1/'  | sed -r 's/\{//g')
echo Using tenant $TENANT_ID
validate $? 'SUCCESS: Successfully got tenant data with admin user '$TENANT_ADMIN_NAME', its id is '$TENANT_ID 'ERROR: Cannot get tenant id for tenant with admin user '$TENANT_ADMIN_NAME

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
