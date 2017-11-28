#!/bin/bash
#
# Copyright 2017 Dell Inc. or its subsidiaries. All Rights Reserved.
# Dell EMC Confidential/Proprietary Information.
#

set -o errexit

# Script creates rpm for docker containers

set -x

if [[ -z "$DOCKER_IMAGE_NAME" ]]
then
    echo "Please make sure pom has set DOCKER_IMAGE_NAME variable"
    exit 1
fi

PKG_NAME=dell-cpsd-dne-$DOCKER_IMAGE_NAME

[[ -z "$BUILD_NUMBER" ]] && export BUILD_NUMBER=0
[[ -z "$RPM_NAME" ]] && export RPM_NAME=$PKG_NAME
[[ -z "$BRANCH_NAME" ]] && export BRANCH_NAME=local


SPEC_PATH=$(dirname $(realpath $0))
LOCAL_WORKSPACE=$(realpath "$SPEC_PATH/../target")
SPEC_FILE=$(echo $SPEC_PATH/*.spec | head -n1)
VERSION_PATH=$SPEC_PATH
while [[ ! -e "$VERSION_PATH/.version" && "$VERSION_PATH" != "/" ]]
do
    VERSION_PATH=$(realpath $VERSION_PATH/..)
done


if [[ ! -e "$VERSION_PATH/.version" ]]
then
    echo "Cannot file .version file in : $SPEC_PATH or in its parents"
    exit 1
fi

BUILD_ROOT=$LOCAL_WORKSPACE/rpmbuild

VERSION=$(cat $VERSION_PATH/.version)
TAR_NAME="$RPM_NAME-$VERSION"
TAR_PATH=$LOCAL_WORKSPACE/$TAR_NAME
IMAGE_TAG=$BRANCH_NAME-$VERSION-$BUILD_NUMBER
CONT_NAME=$PKG_NAME:$IMAGE_TAG

set +x
mkdir -vp $TAR_PATH
cp -r -v $SPEC_PATH/* $TAR_PATH
cp -r -v $LOCAL_WORKSPACE/dependency $TAR_PATH
sed -i 's|\${IMAGE_TAG}|'$IMAGE_TAG'|' $TAR_PATH/install/docker-compose.yml

(
  cd $TAR_PATH
  docker build -t $CONT_NAME .
)
[[ -e $SPEC_PATH/customize_container ]] && source $SPEC_PATH/customize_container
docker save $CONT_NAME > $TAR_PATH/$PKG_NAME.tar
docker rmi $CONT_NAME

tar -cvzf $TAR_PATH.tgz -C $LOCAL_WORKSPACE $TAR_NAME

rm -rfv $TAR_PATH

if [ -z "${GPG_PASSPHRASE}" ]
then
    rpmbuild -tb $TAR_PATH.tgz \
        --clean \
        --define "_topdir $BUILD_ROOT" \
        --define "_name $RPM_NAME" \
        --define "_revision $BUILD_NUMBER" \
        --define "_version $VERSION"

else
    echo ${GPG_PASSPHRASE} | rpmbuild -tb $TAR_PATH.tgz \
        --sign \
        --clean \
        --define "_topdir $BUILD_ROOT" \
        --define "_name $RPM_NAME" \
        --define "_revision $BUILD_NUMBER" \
        --define "_version $VERSION"
fi

rm -fv $TAR_PATH.tgz