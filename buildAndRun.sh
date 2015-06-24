#/bin/bash

BUILD_IMAGE=sts-build
BUILD_CONT=build-cont

docker build -t $BUILD_IMAGE -f Dockerfile.build .

if [[ $(docker ps -a | grep $BUILD_CONT | wc -l) > 0 ]] ;
then docker rm $BUILD_CONT
fi
docker create --name $BUILD_CONT $BUILD_IMAGE
docker cp $BUILD_CONT:/usr/local/gaia/target/sts.war ./target/

docker build -t gaiaadm/sts .

docker run -d -u jetty -p 9001:8080 gaiaadm/sts
#curl -v http://localhost:9001/sts/oauth/check_token?token=62ad16cf-ab6c-42fa-af3d-359ecf98cdec
