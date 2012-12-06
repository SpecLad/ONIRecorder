This is a demo app that grabs and displays (and optionally records) images
from the Kinect.

To build, you will need special versions of libusb and OpenNI. To build
them, go to /3rdparty/android and run the provided scripts:

./libusb-fetch.sh
./libusb-build.sh
./openni-fetch.sh
./openni-build.sh

You will also need to download the lastest version of Apache Commons
Compress <http://commons.apache.org/compress/> and put the jar
(commons-compress-x.y.z.jar) into the libs directory.

After that, run the following in this directory (<pcl> means the full path to the PCL sources):

export NDK_MODULE_PATH=<pcl>/3rdparty/android/ndk-modules
ndk-build
android update project -p . -n ONIRecorder
ant debug
