# Build and install the azure-iot-sdk-c with PnP support.

DESCRIPTION = "Microsoft Azure IoT SDKs and libraries for C"
AUTHOR = "Microsoft Corporation"
HOMEPAGE = "https://github.com/Azure/azure-iot-sdk-c"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=4283671594edec4c13aeb073c219237a"

# We pull from master branch in order to get PnP APIs
SRC_URI = "gitsm://github.com/Azure/azure-iot-sdk-c.git;branch=main"

SRCREV = "6b6e2449672d229d4a0fc9503d8fd3b2b2dde5f1"
PV = "1.0+git${SRCPV}"

S = "${WORKDIR}/git"

# util-linux for uuid-dev
DEPENDS = "util-linux curl openssl boost cpprest libproxy msft-gsl"

inherit cmake

# Do not use amqp since it is deprecated.
# Do not build sample code to save build time.
EXTRA_OECMAKE += "-Duse_amqp:BOOL=OFF -Duse_http:BOOL=ON -Duse_mqtt:BOOL=ON -Dskip_samples:BOOL=ON -Dbuild_service_client:BOOL=OFF -Dbuild_provisioning_service_client:BOOL=OFF"

sysroot_stage_all_append () {
    sysroot_stage_dir ${D}${exec_prefix}/cmake ${SYSROOT_DESTDIR}${exec_prefix}/cmake
}


FILES_${PN}-dev += "${exec_prefix}/cmake"

BBCLASSEXTEND = "native nativesdk"
