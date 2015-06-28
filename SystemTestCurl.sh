#/bin/bash

TENANT_NAME="tenant1"
TENANT_DB="db1"

##### create tenant
curl -i -H "Content-Type: application/json" -d '{"tenantName": "'$TENANT_NAME'","tenantDbName": "'$TENANT_DB'"}' http://localhost:9001/sts/tenant | grep '201 Created'

if [ $? -eq 0 ]; then
   echo Tenant created successfully
else
   echo Failed to create tenant
   exit 1
fi

##### check that tenant created
curl -i -H "Accept: application/json" http://localhost:9001/sts/tenant/$TENANT_NAME  | grep '200 OK'
if [ $? -eq 0 ]; then
   echo Tenant $TENANT_NAME is available
else
   echo Failed to get tenant $TENANT_NAME
   exit 1
fi

##### get tenant id for further usage
TENANT_ID=$(curl -H "Accept: application/json" http://localhost:9001/sts/tenant/$TENANT_NAME | grep 'tenantId' | sed s/,/\\n/g | grep 'tenantId' | sed -s 's/"tenantId":\(.*\)/\1/'  | sed -r 's/\{//g')
if [ $? -eq 0 ]; then
   echo Successfully got tenant data for $TENANT_NAME, its id is $TENANT_ID
else
   echo Cannot get tenant id for tenant $TENANT_NAME
   exit 1
fi

##### create client
curl -i -H "Content-Type: application/json" -d '{"client_id": "restapp","client_secret": "secret","scope": "read,write,trust","authorized_grant_types": "client_credentials","authorities": "ROLE_APP","tenantId": "'$TENANT_ID'"}' http://localhost:9001/sts/oauth/client | grep '201 Created'
if [ $? -eq 0 ]; then
   echo Client restapp created successfully
else
   echo Failed to create client restapp
   exit 1
fi
