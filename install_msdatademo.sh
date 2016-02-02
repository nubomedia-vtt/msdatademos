#!/bin/bash
set -e -x
echo Installing armarkerdetector filter development environment
echo Version 2015-03-03
echo @author Markus Ylikerala, VTT, http://www.vtt.fi/

#######################################
# Note that version numbers etc may differ
# in the future eg the current 0.0.6-SNAPSHOT
#######################################

#######################################
# Tweak the TARGET variable if needed
# default is ~/nubomedia
#######################################
USER_HOME=$(eval echo ~${SUDO_USER})
TARGET=${USER_HOME}/nubomedia
mkdir -p $TARGET


#######################################
# The rest is going automagically
#######################################
cd $TARGET
AR3D=$TARGET/msdatademos

wget -nd http://ssi.vtt.fi/ar-markerdetector-binaries/msdata_0.0.1~rc1_java/datachannelexample-6.2.1-SNAPSHOT.jar
wget -nd http://ssi.vtt.fi/ar-markerdetector-binaries/msdata_0.0.1~rc1_java/pom.xml

mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=datachannelexample-6.2.1-SNAPSHOT.jar -DpomFile=pom.xml

if [ ! -d $AR3D ]; then
  git clone https://github.com/nubomedia-vtt/msdatademos.git
fi

cd $TARGET/msdatademos/Models
sudo cp heartrate1.bmp /opt/
sudo cp temperature0.bmp /opt/

cd $AR3D

curl -sL https://deb.nodesource.com/setup | sudo bash -
sudo apt-get install -y nodejs
sudo npm install -g bower
bower --config.analytics=false install --allow-root
mv bower_components src/main/resources/static/
