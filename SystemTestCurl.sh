#!/bin/bash

TENANT_ID=9 #set in ACM server mock seed file

# Format: validate $? <good_message> <bad_message>
function validate {
   if [ $1 -eq 0 ]; then
      echo '>>>' $2
   else
      echo '>>>' $3
      exit 1
   fi
}


until $(curl --output /dev/null --silent --head --fail http://localhost:3000/acms/mock); do
    printf 'ACM server is not ready yet; trying again in 5 second ...'
    sleep 5
done

until $(curl --output /dev/null --silent --head --fail http://localhost:9001/sts); do
    printf 'STS server is not ready yet; trying again in 5 second ...'
    sleep 5
done


#================================ NOTE: ACM hostname or IP must be known when running STS container

##### obtain gaia.token for testing user
GAIATOKEN=$(curl http://localhost:3000/acms/mock)
echo GAIATOKEN=$GAIATOKEN

##### log available information for better debugging
echo ACCOUNTS AVAILABLE:
curl -H 'Content-Type: application/json' --cookie 'gaia.token='$GAIATOKEN http://localhost:3000/acms/api/accounts
echo CURRENT USER DETAILS:
curl -H 'Content-Type: application/json' --cookie 'gaia.token='$GAIATOKEN http://localhost:3000/acms/api/users/self

#### create token and oauth client
curl -H 'Content-Type: application/json' --cookie 'gaia.token='$GAIATOKEN http://localhost:9001/sts/facade/getmyapitoken?st=9
validate $? 'SUCCESS: Successfully created access_token' 'ERROR: Cannot create access_token'

sleep 1

#### get oauth client created automatically during the token creation
CLIENT_NAME=$(curl -H 'Content-Type: application/json' --cookie 'gaia.token='$GAIATOKEN http://localhost:9001/sts/oauth/client | grep 'client_id'  | sed s/,/\\n/g | grep 'client_id' | cut -d \" -f 4 )
validate $? 'SUCCESS: oauth client '$CLIENT_NAME' received successfully' 'ERROR: Failed to get oauth client '$CLIENT_NAME
echo CLIENT_NAME=$CLIENT_NAME

sleep 1

#### validate token
TOKEN=$(curl -H 'Content-Type: application/json' --cookie 'gaia.token='$GAIATOKEN http://localhost:9001/sts/facade/getmyapitoken?st=9 | grep 'access_token' | sed s/,/\\n/g | grep 'access_token' | sed -s 's/"access_token":\(.*\)/\1/'  | sed -r 's/\{//g' | sed -r 's/\"//g')
validate $? 'SUCCESS: Token '$TOKEN' is valid' 'ERROR: Token '$TOKEN' is invalid'
echo TOKEN=$TOKEN

sleep 1

#### revoke token
curl -i -X DELETE http://localhost:9001/sts/oauth/token/revoke?token=$TOKEN | grep '200 OK'
validate $? 'SUCCESS: Token '$TOKEN' revoked successfully' 'ERROR: Failed to revoke token '$TOKEN

sleep 1

#### validate after revoking
curl -i -H "Accept: application/json" http://localhost:9001/sts/oauth/check_token?token=$TOKEN | grep '400 Bad Request'
validate $? 'SUCCESS: Token '$TOKEN' is invalid after revoking' 'ERROR: Token '$TOKEN' is still valid after revoking'

sleep 1

#### create token again to validate it is removed when client deleted bit client is kept
TOKEN=$(curl -H 'Content-Type: application/json' --cookie 'gaia.token='$GAIATOKEN http://localhost:9001/sts/facade/getmyapitoken?st=9 | grep 'access_token' | sed s/,/\\n/g | grep 'access_token' | sed -s 's/"access_token":\(.*\)/\1/'  | sed -r 's/\{//g' | sed -r 's/\"//g')
validate $? 'SUCCESS: Successfully created access_token for client '$TOKEN', its value is '$TOKEN 'ERROR: Cannot create access_token for client '$CLIENT_NAME
echo NEW TOKEN=$TOKEN

sleep 1

#### cleanup to enable re-run
curl -i -X DELETE http://localhost:9001/sts/oauth/client/$CLIENT_NAME | grep '204 No Content'
validate $? 'SUCCESS: Oauth client is removed' 'ERROR: Failed to remove oauth client'
curl -i -H "Accept: application/json" http://localhost:9001/sts/oauth/check_token?token=$TOKEN | grep '400 Bad Request'
validate $? 'SUCCESS: Token '$TOKEN' is deleted after client deletion' 'ERROR: Token '$TOKEN' is still existing after client deletion'
