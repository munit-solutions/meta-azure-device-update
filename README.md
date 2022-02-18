# Introduction 

This project meta-azure-device-update serves as a base-layer of embedding device update agent in a custom Linux-based system. You will most likely create your own layer on top of this to overwrite the files provided in `recipes-extended/images`, if not more. You're welcome to write up a few lines as tutorial or best practice. The best will be included in the wiki of this repo.

This repository is maintained as a private initiative by me and you are welcome (but not obliged) to provide pull requests and help others as they open issues. Azure only supports `warrior` and leaves the rest up to the developer. This repository is thought to be a place for sharing best practices and to provide a basis for supporting other versions of yocto.

The repository git://github.com/RPi-Distro/firmware-nonfree removed the revision `f0ad1a42b051aa9da1d9e1dc606dd68ec2f163a5` from upstream, which means, that we cannot test this package for `warrior` or `zeus` anymore without applying changes to the `meta-raspberry` layer. We haven't checked what other changes are needed and are for now only planning to support `dunfell` and upwards. Feel free to open a pull request if you have a patch for those (or any other) versions.

This branch (including this readme file) will be updated as Azure releases new downloadable resources of the yocto-layer at [azure/iot-hub-device-update](https://github.com/Azure/iot-hub-device-update).

The following lines are part of the latest readme provided by azure.

---

# Introduction 

This project meta-azure-device-update serves as an example of embedding device update agent in a custom Linux-based system.

**Disclaimer:**
There is no guarantee or support for this project. Please modify and customize the project to fit your need, and reference this example as needed.

# Prerequisite

The following instructions are based on Yocto Project, please find the basics of Yocto Project in https://www.yoctoproject.org/.
The following instruction will guide you to create a Raspberry Pi Poky Yocto image with device update agent embedded.

# Yocto Layers, Recipes, and Configuration

There are three main areas that will be covered:

* Yocto build configurations
* Meta-azure-device-update recipe
* Image install with A/B partitions

## Yocto build configurations

This section describes the most important files of the Yocto build, including
'bblayers.conf.sample' and 'local.conf.sample' as well as the Yocto layers that need to be used.

| Layer Name      | Description |
| ------------- | ---------- |
| meta-azure-device-update | Provides the configuration and contains the recipes for installing both the ADU Agent and its dependencies as well as integrating them into the Yocto build layers.|
| meta-openembedded   | Brings in the openembedded layer for strengthening the raspberrypi BSP. Implements some of the Core functionality. |
| meta-raspberrypi   | Implements the BSP layer for the RaspberryPi. Without this layer Yocto cannot be built to work on the raspberry pi. |
| meta-swupdate   |  Adds support for a deployment mechanisms of Yocto's images based on swupdate project. |

There are three publicly available meta layer: meta-openembedded(git://git.openembedded.org/meta-openembedded), meta-raspberrypi (git://github.com/agherzan/meta-raspberrypi.git), meta-swupdate (git@github.com:sbabic/meta-swupdate.git).

#### bblayers.conf.sample

Usually this file would be located under `yocto/config-templates/raspberrypi3`.  
The 'bblayers.conf.sample' shows the complete list of Yocto layers included in
the build.

```shell
POKY_BBLAYERS_CONF_VERSION = "3"

BBPATH = "${TOPDIR}"
BBFILES ?= ""

BBLAYERS ?= " \
  ##OEROOT##/meta \
  ##OEROOT##/meta-poky \
  ##OEROOT##/meta-yocto-bsp \
  ##OEROOT##/../meta-openembedded/meta-oe \
  ##OEROOT##/../meta-openembedded/meta-multimedia \
  ##OEROOT##/../meta-openembedded/meta-networking \
  ##OEROOT##/../meta-openembedded/meta-python \
  ##OEROOT##/../meta-raspberrypi \
  ##OEROOT##/../meta-swupdate \
  ##OEROOT##/../meta-azure-device-update \
  "
```

#### local.conf.sample

Usually this file would be located under `yocto/config-templates/raspberrypi3`.  
Similarly, the 'local.conf.sample' handles the configuration for machine and
package information, architecture, image types, and more.

```shell
CONF_VERSION = "1"

MACHINE ?= "raspberrypi3"
DISTRO ?= "poky"
PACKAGE_CLASSES ?= "package_ipk"
SDKMACHINE = "x86_64"
USER_CLASSES ?= "buildstats image-mklibs image-prelink"
PATCHRESOLVE = "noop"
SSTATE_DIR ?= "build/sstate-cache"

RPI_USE_U_BOOT = "1"
ENABLE_UART = "1"

IMAGE_FSTYPES += "wic wic.bmap"

# Set PREFERRED_PROVIDER_u-boot-fw-utils to prevent warning about
# having two providers of u-boot-fw-utils
PREFERRED_PROVIDER_u-boot-fw-utils = "libubootenv"

DISTRO_FEATURES_append = " systemd"
DISTRO_FEATURES_BACKFILL_CONSIDERED += "sysvinit"
VIRTUAL-RUNTIME_init_manager = "systemd"
VIRTUAL-RUNTIME_initscripts = "systemd-compat-units"
```

## Meta-azure-device-update layer

This meta-azure-device-update layer describes the Yocto recipes in the layer
that builds and installs the ADU Agent code.

#### layer.conf

This file locates at `meta-azure-device-update/conf`.  
It is in the layer.conf file where configuration variables are set for ADU
software version,  SWUpdate, manufacturer, and model.

**NOTE:** For public preview, manufacturer and model defined in layer.conf will
define the PnP manufacturer and model properties.  Manufacturer and model must
match the 'compatibility' in the import manifest of the [ADU Publishing
flow](https://docs.microsoft.com/en-us/azure/iot-hub-device-update/import-update), with the specific format of
"manufacturer.model" (case-sensitive).

```shell
BBPATH .= ":${LAYERDIR}"

# We have a recipes directory containing .bb and .bbappend files, add to BBFILES
BBFILES += "${LAYERDIR}/recipes*/*/*.bb \
            ${LAYERDIR}/recipes*/*/*.bbappend"

BBFILE_COLLECTIONS += "azure-device-update"
BBFILE_PATTERN_azure-device-update := "^${LAYERDIR}/"

# Pri 16 ensures that our recipes are applied over other layers.
# This is applicable where we are using appends files to adjust other recipes.
BBFILE_PRIORITY_azure-device-update = "16"
LAYERDEPENDS_azure-device-update = "swupdate"
LAYERSERIES_COMPAT_azure-device-update  = "warrior"


# Layer-specific configuration variables.
# These values can/will be overriden by environment variables
# if those variables are added to the BB_ENV_EXTRAWHITE environment variable.

# ADU_SOFTWARE_VERSION will be written to a file that is read by the ADU Client.
# This value will be reported through the Device Information PnP interface by the ADU Client
# and is the version of the installed content/image/firmware.
# For the ADU reference image this is set to a new value every build.
ADU_SOFTWARE_VERSION ?= "0.0.0.1"

# HW_REV will be written to a file that is used by swupdate
# to determine hardware compatibility.
HW_REV ?= "1.0"
# MANUFACTURER will be written to file that is read by the ADU Client.
# This value will be reported through the Device Information PnP interface by the ADU Client.
# This value is used as the namespace of the content and for compatibiltiy checks.
MANUFACTURER ?= "Contoso"
# MODEL will also be written to file that is read by the ADU Client.
# This value will be reported through the Device Information PnP interface by the ADU Client.
# This value is used in the name of content and for compatibiltiy checks.
MODEL ?= "ADU Raspberry Pi Example"

# ADUC_PRIVATE_KEY is the build host path to the .pem private key file to use to sign the image.
# ADUC_PRIVATE_KEY_PASSWORD is the build host path to the .pass password file for the private key.

BBFILES += "${@' '.join('${LAYERDIR}/%s/recipes*/*/*.%s' % (layer, ext) \
               for layer in '${BBFILE_COLLECTIONS}'.split() for ext in ['bb', 'bbappend'])}"

# Image level user/group configuration.
# Inherit extrausers to make the setting of EXTRA_USERS_PARAMS effective.
INHERIT += "extrausers"
```

**NOTE:** We need to add `adu` and `do` user, and add `do` to `adu` group to grant it permission.
```shell
# User / group settings
# The settings are separated by the ; character.
# Each setting is actually a command. The supported commands are useradd,
# groupadd, userdel, groupdel, usermod and groupmod.
EXTRA_USERS_PARAMS = "groupadd --gid 800 adu ; \
 groupadd -r --gid 801 do ; \
 useradd --uid 800 -p '' -r -g adu --no-create-home --shell /bin/false adu ; \
 useradd --uid 801 -p '' -r -g do -G adu --no-create-home --shell /bin/false do ; \
 "
```

### Build ADU Agent into image

The ADU reference agent uses the
[CMake](../../cmake) build
system to build the source code binaries.  Build options are listed within the
azure-device-update_git.bb recipe and 'ADUC_SRC_URI' points to the ADU
reference agent to pull it into the image.

#### azure-device-update_git.bb

This file locates at `meta-azure-device-update/recipes-azure-device-update/azure-device-update/azure-device-update_git.bb`.  
This file builds and installs our ADU sample code.

```shell
# Environment variables that can be used to configure the behavior of this recipe.
# ADUC_GIT_BRANCH       Changes the branch that ADU code is pulled from.
# ADUC_SRC_URI          Changes the URI where the ADU code is pulled from.
#                       This URI follows the Yocto Fetchers syntax.
#                       See https://www.yoctoproject.org/docs/latest/ref-manual/ref-manual.html#var-SRC_URI
# BUILD_TYPE            Changes the type of build produced by this recipe.
#                       Valid values are Debug, Release, RelWithDebInfo, and MinRelSize.
#                       These values are the same as the CMAKE_BUILD_TYPE variable.

LICENSE = "CLOSED"

ADUC_GIT_BRANCH ?= "master"
ADUC_SRC_URI ?= "git://github.com/Azure/adu-private-preview;branch=${ADUC_GIT_BRANCH}"
SRC_URI = "${ADUC_SRC_URI}"

# This code handles setting variables for either git or for a local file.
# This is only while we are using private repos, once our repos are public,
# we will just use git.
python () {
    src_uri = d.getVar('ADUC_SRC_URI')
    if src_uri.startswith('git'):
        d.setVar('SRCREV', d.getVar('AUTOREV'))
        d.setVar('PV', '1.0+git' + d.getVar('SRCPV'))
        d.setVar('S', d.getVar('WORKDIR') + "/git")
    elif src_uri.startswith('file'):
        d.setVar('S',  d.getVar('WORKDIR') + "/adu-linux-client")
}

# ADUC depends on azure-iot-sdk-c and DO Agent SDK
DEPENDS = "azure-iot-sdk-c deliveryoptimization-agent curl deliveryoptimization-sdk"

inherit cmake useradd

BUILD_TYPE ?= "Debug"
EXTRA_OECMAKE += "-DCMAKE_BUILD_TYPE=${BUILD_TYPE}"
# Don't treat warnings as errors.
EXTRA_OECMAKE += "-DADUC_WARNINGS_AS_ERRORS=OFF"
# Build the non-simulator (real) version of the client.
EXTRA_OECMAKE += "-DADUC_PLATFORM_LAYER=linux"
# Integrate with SWUpdate as the installer
EXTRA_OECMAKE += "-DADUC_CONTENT_HANDLERS=microsoft/swupdate"
# Set the path to the manufacturer file
EXTRA_OECMAKE += "-DADUC_MANUFACTURER_FILE=${sysconfdir}/adu-manufacturer"
# Set the path to the model file
EXTRA_OECMAKE += "-DADUC_MODEL_FILE=${sysconfdir}/adu-model"
# Set the path to the version file
EXTRA_OECMAKE += "-DADUC_VERSION_FILE=${sysconfdir}/adu-version"
# Use zlog as the logging library.
EXTRA_OECMAKE += "-DADUC_LOGGING_LIBRARY=zlog"
# Change the log directory.
EXTRA_OECMAKE += "-DADUC_LOG_FOLDER=/adu/logs"
# Use /adu directory for configuration.
# The /adu directory is on a seperate partition and is not updated during an OTA update.
EXTRA_OECMAKE += "-DADUC_CONF_FOLDER=/adu"
# Don't install/configure the daemon, another bitbake recipe will do that.
EXTRA_OECMAKE += "-DADUC_INSTALL_DAEMON=OFF"
# cpprest installs its config.cmake file in a non-standard location.
# Tell cmake where to find it.
EXTRA_OECMAKE += "-Dcpprestsdk_DIR=${WORKDIR}/recipe-sysroot/usr/lib/cmake"
# Using the installed DO SDK include files.
EXTRA_OECMAKE += "-DDOSDK_INCLUDE_DIR=${WORKDIR}/recipe-sysroot/usr/include"

# bash - for running shell scripts for install.
# swupdate - to install update package.
# adu-pub-key - to install public key for update package verification.
# adu-device-info-files - to install the device info related files onto the image.
# adu-hw-compat - to install the hardware compatibility file used by swupdate.
# adu-log-dir - to create the temporary log directory in the image.
# deliveryoptimization-agent-service - to install the delivery optimization agent for downloads.
RDEPENDS_${PN} += "bash swupdate adu-pub-key adu-device-info-files adu-hw-compat adu-log-dir deliveryoptimization-agent-service"

INSANE_SKIP_${PN} += "installed-vs-shipped"

ADUC_DATA_DIR = "/var/lib/adu"
ADUC_LOG_DIR = "/adu/logs"
ADUC_CONF_DIR = "/adu"

ADUUSER = "adu"
ADUGROUP = "adu"
DOUSER = "do"
DOGROUP = "do"

PACKAGES =+ "${PN}-adu"

USERADD_PACKAGES = "${PN}-adu"

GROUPADD_PARAM_${PN}-adu = "\
    --gid 800 --system adu ; \
    --gid 801 --system do ; \
    "

# USERADD_PARAM specifies command line options to pass to the
# useradd command. Multiple users can be created by separating
# the commands with a semicolon. Here we'll create adu user:
USERADD_PARAM_${PN}-adu = "\
    --uid 800 --system -g ${ADUGROUP} --home-dir /home/${ADUUSER} --no-create-home --shell /bin/false ${ADUUSER} ; \
    --uid 801 --system -g ${DOGROUP} -G ${ADUGROUP} --home-dir /home/${DOUSER} --no-create-home --shell /bin/false ${DOUSER} ; \
    "

do_install_append() {
    #create ADUC_DATA_DIR
    install -d ${D}${ADUC_DATA_DIR}
    chgrp ${ADUGROUP} ${D}${ADUC_DATA_DIR}
    chmod 0770 ${D}${ADUC_DATA_DIR}

    #create ADUC_CONF_DIR
    install -d ${D}${ADUC_CONF_DIR}
    chgrp ${ADUGROUP} ${D}${ADUC_CONF_DIR}
    chmod 0774 ${D}${ADUC_CONF_DIR}

    #create ADUC_LOG_DIR
    install -d ${D}${ADUC_LOG_DIR}
    chgrp ${ADUGROUP} ${D}${ADUC_LOG_DIR}
    chmod 0774 ${D}${ADUC_LOG_DIR}

    #install adu-shell to /usr/lib/adu directory.
    install -d ${D}${libdir}/adu

    install -m 0550 ${S}/src/adu-shell/scripts/adu-swupdate.sh ${D}${libdir}/adu

    #set owner for adu-shell
    chown root:${ADUGROUP} ${D}${libdir}/adu/adu-shell

    #set S UID for adu-shell
    chmod u+s ${D}${libdir}/adu/adu-shell
}

FILES_${PN} += "${bindir}/AducIotAgent"
FILES_${PN} += "${libdir}/adu/* ${ADUC_DATA_DIR}/* ${ADUC_LOG_DIR}/* ${ADUC_CONF_DIR}/*"
FILES_${PN}-adu += "/home/${ADUUSER}/* /home/$(DOUSER)/*"
```

#### adu-agent-service.bb

This file locates at `meta-azure-device-update/recipes-azure-device-update/adu-agent-service/adu-agent-service.bb`.  
The adu-agent-service.bb recipe is used to install the adu-agent.service that
will auto start the ADU agent service on boot for the Raspberry Pi image,
passing in the IoT Hub connection string located at /adu/adu-conf.txt
There are also similar recipes for the 'deliveryoptimization-agent' found in the
azure-device-update_git.bb bundle.

```shell
# Installs the ADU Agent Service that will auto-start the ADU Agent
# and pass in the IoT Hub connection string located at /boot/iot-con-string.txt.

LICENSE="CLOSED"

SRC_URI = "\
    file://adu-agent.service \
"

SYSTEMD_SERVICE_${PN} = "adu-agent.service"

do_install_append() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/adu-agent.service ${D}${systemd_system_unitdir}
}

FILES_${PN} += "${systemd_system_unitdir}/adu-agent.service"

REQUIRED_DISTRO_FEATURES = "systemd"

DEPENDS_${PN} += "azure-device-update deliveryoptimization-agent-service"

RDEPENDS_${PN} += "azure-device-update deliveryoptimization-agent-service"

inherit allarch systemd
```

#### adu-agent.service

This file locates at `meta-azure-device-update/recipes-azure-device-update/adu-agent-service/files/adu-agent.service`.  
The adu-agent.service is a systemd unit file that defines the ADU Agent service (adu-agent.service).
This file will be installed at /lib/systemd/system directory.

```shell
[Unit]
Description=ADU Client service.
After=network-online.target
Wants=deliveryoptimization-agent.service

[Service]
Type=simple
Restart=on-failure
RestartSec=1
User=adu
# If /adu/adu-conf.txt does not exist, systemd will try to start the ADU executable
# 5 times and then give up.
# We can check logs with journalctl -f -u adu-agent.service
ExecStart=/usr/bin/AducIotAgent -l 0 -e

[Install]
WantedBy=multi-user.target
```

#### adu-device-info-files.bb

This file locates at `meta-azure-device-update/recipes-azure-device-update/adu-device-info-files/adu-device-info-files.bb`.  
The adu-device-info-files.bb specifies files the ADU reference agent uses to
implement the Device Information PnP interface.  This generates files with ADU
applicability info for manufacturer, model and version, which can be read by the
ADU reference agent.

```shell
# Generates a text file with the ADU applicability info
# for manufacturer and model and copies/installs that file into the image.

# Environment variables that can be used to configure the behaviour of this recipe.
# MANUFACTURER          The manufacturer string that will be written to the manufacturer
#                       file and reported through the Device Information PnP Interface.
# MODEL                 The model string that wil be written to the model file and
#                       reported through the Device Information PnP Interface.
# ADU_SOFTWARE_VERSION  The software version for the image/firmware. Will be written to
#                       the version file that is read by ADU Client.

LICENSE="CLOSED"

# Generate the manufacturer, model, and version files
do_compile() {
    echo "${MANUFACTURER}" > adu-manufacturer
    echo "${MODEL}" > adu-model
    echo "${ADU_SOFTWARE_VERSION}" > adu-version
}

# Install the files on the image in /etc
do_install() {
    install -d ${D}${sysconfdir}
    install -m ugo=r adu-manufacturer ${D}${sysconfdir}/adu-manufacturer
    install -m ugo=r adu-model ${D}${sysconfdir}/adu-model
    install -m ugo=r adu-version ${D}${sysconfdir}/adu-version
}

FILES_${PN} += "${sysconfdir}/adu-manufacturer"
FILES_${PN} += "${sysconfdir}/adu-model"
FILES_${PN} += "${sysconfdir}/adu-version"

inherit allarch
```

#### deliveryoptimization-agent_git.bb

This file locates at `meta-azure-device-update/recipes-azure-device-update/deliveryoptimization-agent/deliveryoptimization-agent_git.bb`.  
The deliveryoptimization-agent_git.bb is a recipe for building one of ADU Agent dependencies,
Delivery Optimization Client service.

```shell
# Build and install Delivery Optimization Simple Client.

# Environment variables that can be used to configure the behaviour of this recipe.
# DO_SRC_URI            Changes the URI where the DO code is pulled from.
#                       This URI follows the Yocto Fetchers syntax.
#                       See https://www.yoctoproject.org/docs/latest/ref-manual/ref-manual.html#var-SRC_URI
# BUILD_TYPE            Changes the type of build produced by this recipe.
#                       Valid values are Debug, Release, RelWithDebInfo, and MinRelSize.
#                       These values are the same as the CMAKE_BUILD_TYPE variable.

LICENSE = "CLOSED"

DO_SRC_URI ?= "gitsm://github.com/microsoft/do-client;branch=main"
SRC_URI = "${DO_SRC_URI}"

# This code handles setting variables for either git or for a local file.
# This is only while we are using private repos, once our repos are public,
# we will just use git.
python () {
    src_uri = d.getVar('DO_SRC_URI')
    if src_uri.startswith('git'):
        d.setVar('SRCREV', d.getVar('AUTOREV'))
        d.setVar('PV', '1.0+git' + d.getVar('SRCPV'))
        d.setVar('S', d.getVar('WORKDIR') + "/git")
    elif src_uri.startswith('file'):
        d.setVar('S',  d.getVar('WORKDIR') + "/do-client")
}

DEPENDS = "boost cpprest libproxy msft-gsl"

inherit cmake

BUILD_TYPE ?= "Debug"
EXTRA_OECMAKE += "-DCMAKE_BUILD_TYPE=${BUILD_TYPE}"
# Don't build DO tests.
EXTRA_OECMAKE += "-DDO_BUILD_TESTS=OFF"
# Specify build is for deliveryoptimization-agent
EXTRA_OECMAKE += "-DDO_INCLUDE_AGENT=ON"

# cpprest installs its config.cmake file in a non-standard location.
# Tell cmake where to find it.
EXTRA_OECMAKE += "-Dcpprestsdk_DIR=${WORKDIR}/recipe-sysroot/usr/lib/cmake"
BBCLASSEXTEND = "native nativesdk"
```

#### deliveryoptimization-agent-service.bb

This file locates at `meta-azure-device-update/recipes-azure-device-update/deliveryoptimization-agent-service/deliveryoptimization-agent-service.bb`.  
The deliveryoptimization-agent-service.bb file installs and configures the Delivery Optimization Client
service on the device.

```shell
# Installs and configures the DeliveryOptimization Agent Service

LICENSE="CLOSED"

SRC_URI = "\
    file://deliveryoptimization-agent.service \
"

SYSTEMD_SERVICE_${PN} = "deliveryoptimization-agent.service"

do_install_append() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/deliveryoptimization-agent.service ${D}${systemd_system_unitdir}
}

FILES_${PN} += "${systemd_system_unitdir}/deliveryoptimization-agent.service"
REQUIRED_DISTRO_FEATURES = "systemd"
RDEPENDS_${PN} += "deliveryoptimization-agent"

inherit allarch systemd
```

#### deliveryoptimization-agent.service

This file locates at `meta-azure-device-update/recipes-azure-device-update/deliveryoptimization-agent-service/files/deliveryoptimization-agent.service`.  
The deliveryoptimization-agent-list.service is a systemd unit file that defines the Delivery Optimization Client service.

```shell
[Unit]
Description=deliveryoptimization-agent: Performs content delivery optimization tasks
After=network-online.target

[Service]
Type=simple
Restart=on-failure
User=root
ExecStart=/usr/bin/deliveryoptimization-agent

[Install]
WantedBy=multi-user.target
```

#### azure-iot-sdk-c_git.bb

This file locates at `meta-azure-device-update/recipes-azure-iot/azure-iot-sdk-c/azure-iot-sdk-c_git.bb`.  
The ADU Agent communicates with ADU services using a PnP supports provied by
an Azure IoT SDK for C. azure-iot-sdk-c_git.bb is the recipe for building the SDK used by ADU Agent.

```shell
# Build and install the azure-iot-sdk-c with PnP support.

DESCRIPTION = "Microsoft Azure IoT SDKs and libraries for C"
AUTHOR = "Microsoft Corporation"
HOMEPAGE = "https://github.com/Azure/azure-iot-sdk-c"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=4283671594edec4c13aeb073c219237a"

# We pull from master branch in order to get PnP APIs
SRC_URI = "gitsm://github.com/Azure/azure-iot-sdk-c.git;branch=master"

SRCREV = "${AUTOREV}"
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
```

#### adu-base-image.bb

This file locates at `meta-azure-device-update/recipes-core/images/adu-base-image.bb`.  
The adu-base-image.bb file creates an image that can be flashed on an SD card
with the ADU Agent, DO Agent, and other useful features pre-installed.

```shell
# Creates the base image for ADU that can we used to flash an SD card.
# This image is also used to populate the ADU update image.

DESCRIPTION = "ADU base image"
SECTION = ""
LICENSE="CLOSED"

# .wks file is used to create partitions in image.
WKS_FILE_raspberrypi3 = "adu-raspberrypi.wks"
# wic* images our used to flash SD cards
# ext4.gz image is used to construct swupdate image.
IMAGE_FSTYPES += "wic wic.gz ext4.gz"

IMAGE_FEATURES += "splash debug-tweaks ssh-server-openssh tools-debug tools-profile"

# connman - provides network connectivity.
# parted - provides disk partitioning utility.
# fw-env-conf - installs fw_env.config file for fw utils. (fw_*)
IMAGE_INSTALL_append = " \
    packagegroup-core-boot \
    packagegroup-core-full-cmdline \
    openssh connman connman-client \
    parted fw-env-conf \
    binutils \
    adu-agent-service \
    register-adu-extensions \
    "

export IMAGE_BASENAME = "adu-base-image"

# This must be at the bottom of this file.
inherit core-image
```

#### adu-raspberrypi.wks

This file locates at `meta-azure-device-update/wic/adu-raspberrypi.wks`.  
The adu-raspberrypi.wks file creates the partition layout and populates files in
the final ADU base image that can be flashed onto an SD card.  
**Note:** This file would normally update the fstab file for the ADU base image,
but it would not update the fstab file for the ADU update image. To ensure the 
fstab files in both the base and update images are correct, we specify our own 
fstab file.

```shell
# Wic Kickstart file that defines which partitions are present in wic images.
# See https://www.yoctoproject.org/docs/current/mega-manual/mega-manual.html#ref-kickstart

# NOTE: Wic will update the /etc/fstab file in the .wic* images,
# but this file needs to also be in sync with the base-files fstab file
# that gets provisioned in the rootfs. Otherwise, updates will install
# an incompatible fstab file.

# Boot partition containing bootloader. This must be the first entry.
part /boot --source bootimg-partition --ondisk mmcblk0 --fstype=vfat --label boot --active --align 4096 --size 20 --fsoptions "defaults,sync"

# Primary rootfs partition. This must be the second entry.
part / --source rootfs --ondisk mmcblk0 --fstype=ext4 --label rootA --align 4096 --extra-space 512

# Secondary rootfs partition used for A/B updating. Starts as a copy of the primary rootfs partition.
# This must be the third entry.
part --source rootfs --ondisk mmcblk0 --fstype=ext4 --label rootB --align 4096 --extra-space 512

# ADU parition for ADU specfic configuration and logs.
# This partition allows configuration and logs to persist across updates (similar to a user data partition).
# The vfat file type allows this partition to be viewed and written to from Linux or Windows.
part /adu --ondisk mmcblk0 --fstype=vfat --label adu --align 4096 --size 512 --fsoptions "defaults,gid=800,uid=800"
```

#### fstab

This file locates at `meta-azure-device-update/recipes-core/base-files/base-files/raspberrypi3/fstab`.  
```shell
# The fstab (/etc/fstab) (or file systems table) file is a system configuration
# file on Debian systems. The fstab file typically lists all available disks and
# disk partitions, and indicates how they are to be initialized or otherwise
# integrated into the overall system's file system.
# See https://wiki.debian.org/fstab

# Default fstab entries
/dev/root            /                    auto       defaults              1  1
proc                 /proc                proc       defaults              0  0
devpts               /dev/pts             devpts     mode=0620,gid=5       0  0
tmpfs                /run                 tmpfs      mode=0755,nodev,nosuid,strictatime 0  0
tmpfs                /var/volatile        tmpfs      defaults              0  0

# Custom fstab entries for ADU raspberrypi3.
# NOTE: these entries must be kept in sync with the corresponding .wks file.

# Mount the boot partition that contains the bootloader.
/dev/mmcblk0p1  /boot   vfat    defaults,sync   0   0

# Mount the ADU specific partition for reading configuration and writing logs.
# This partition is only accessible by adu user/group.
/dev/mmcblk0p4  /adu    vfat    defaults,gid=800,uid=800 0   0
```

### Build image for A/B partitions

Alongside the ADU reference agent, two types of reference images, specifically
for the Raspberry Pi 3 B+ device, are built.  One is used as a base image
('rpi-u-boot-scr.bbappend', included in the raspberrypi bsp recipes) for the
initial flash of the device, installed on the "active partition" and an update
image, to be delivered by ADU, installed  on the "inactive partition".  After a
reboot the partitions will swap.

#### rpi-u-boot-scr.bbappend

This file locates at `meta-azure-device-update/raspberrypi/recipes-bsp/rpi-u-boot-scr/rpi-u-boot-scr.bbappend`.  
The bootloader script needs to override the raspberrypi bsp layer, to handle A/B
updating.  The rpi-u-boot-scr.bbappend file tells the recipe file to look in a
different directory for the bootloader.

```shell
# Overrides the default boot loader script that is defined in meta-raspberrypi
# Key line in the boot loader script is:
# if env exists rpipart;then setenv bootargs ${bootargs} root=/dev/mmcblk0p${rpipart}; fi
# This adds the ability to change which partition is used for boot into OS
# and can be changed with the rpipart firmware environment variable.
FILESEXTRAPATHS_prepend := "${THISDIR}/files:"
```

#### boot.cmd.in

This file locates at `meta-azure-device-update/raspberrypi/recipes-bsp/rpi-u-boot-scr/files/boot.cmd.in`.  
The 'rpi-u-boot-scr.bbappend' file installs a custom 'boot.cmd.in' instead of
the default one.  This script allows the ADU Agent to change the root partition
on reboot.

```shell
saveenv
fdt addr ${fdt_addr} && fdt get value bootargs /chosen bootargs
fatload mmc 0:1 ${kernel_addr_r} @@KERNEL_IMAGETYPE@@
if env exists rpipart;then setenv bootargs ${bootargs} root=/dev/mmcblk0p${rpipart}; fi
@@KERNEL_BOOTCMD@@ ${kernel_addr_r} - ${fdt_addr}
```

#### u-boot_%.bbappend and u-boot-fw-utils_%.bbappend


The files locate at `meta-azure-device-update/recipes-bsp/u-boot`.  
```shell
FILESEXTRAPATHS_prepend := "${THISDIR}/files:"
```

#### fw-env-conf.bb

This file locates at `meta-azure-device-update/recipes-bsp/u-boot/fw-env-conf.bb`.  
Similarly, the u-boot recipe file needs to be overwritten to facilitate A/B
updates.

```shell
# Copy/install fw_env.config which is necessary
# for fw utils (fw_*) like fw_printenv and fw_setenv

LICENSE = "CLOSED"

SRC_URI = "file://fw_env.config"

do_install() {
    install -d ${D}${sysconfdir}
    install -m 644 ${WORKDIR}/fw_env.config ${D}${sysconfdir}/fw_env.config
}

FILES_${PN} += "${sysconfdir}/fw_env.config"
```

### SWUpdate support

The SWUpdate framework is used to build the base and update ADU Agent images.
It is also used to install the image on the Raspberry Pi device.

#### swupdate_%.bbappend

This file locates at `meta-azure-device-update/recipes-support/swupdate/swupdate_%.bbappend`.  
Point to the 'defconfig' file instead using 'swupdate_%.bbappend'.

```shell
# Configure swupdate client/agent for our purposes.
# In general we only want the minimum functionality
# required to verify and install an image file.
FILESEXTRAPATHS_append := "${THISDIR}/${PN}:"

PACKAGECONFIG_CONFARGS = ""
```

#### defconfig

This file locates at `meta-azure-device-update/recipes-support/swupdate/swupdate/defconfig`.  
The 'defconfig' file is how ADU configures SWUpdate to build and install into
the image, which are applied at build time.  The result is a custom
implementation of SWUpdate to suit the needs of the ADU reference agent.

```shell
CONFIG_HAVE_DOT_CONFIG=y

CONFIG_LUA=y
CONFIG_LUAPKG="lua"

CONFIG_HW_COMPATIBILITY=y
CONFIG_HW_COMPATIBILITY_FILE="/etc/adu-hw-compat"

CONFIG_CROSS_COMPILE=""
CONFIG_SYSROOT=""
CONFIG_EXTRA_CFLAGS=""
CONFIG_EXTRA_LDFLAGS=""
CONFIG_EXTRA_LDLIBS=""

CONFIG_UBOOT=y
CONFIG_UBOOT_NEWAPI=y
CONFIG_UBOOT_FWENV="/etc/fw_env.config"
CONFIG_HASH_VERIFY=y
CONFIG_SIGNED_IMAGES=y
CONFIG_SIGALG_RAWRSA=y

CONFIG_GUNZIP=y

CONFIG_LIBCONFIG=y
CONFIG_PARSERROOT=""

CONFIG_RAW=y
CONFIG_ARCHIVE=y
CONFIG_BOOTLOADERHANDLER=y
```

#### adu-update-image.bb

This file locates at `meta-azure-device-update/recipes-extended/images/adu-update-image.bb`.  
The adu-update-image.bb file builds the SWUpdate image.

```shell
# This recipe and related files were taken from
# https://github.com/sbabic/meta-swupdate-boards
# and modifed for our purposes.

DESCRIPTION = "ADU swupdate image"
SECTION = ""
LICENSE="CLOSED"

DEPENDS += "adu-base-image swupdate"

SRC_URI = " \
    file://sw-description \
"

# images to build before building adu update image
IMAGE_DEPENDS = "adu-base-image"

# images and files that will be included in the .swu image
SWUPDATE_IMAGES = " \
        adu-base-image \
        "

SWUPDATE_IMAGES_FSTYPES[adu-base-image] = ".ext4.gz"

# Configure signing of the image with private key and password files.
# ADUC_PRIVATE_KEY - private key (.pem) file.
# ADUC_PRIVATE_KEY_PASSWORD - private key password (.pass) file.
# Generated RSA key with password using command:
# openssl genrsa -aes256 -passout file:priv.pass -out priv.pem
SWUPDATE_SIGNING = "RSA"
SWUPDATE_PRIVATE_KEY = "${ADUC_PRIVATE_KEY}"
SWUPDATE_PASSWORD_FILE = "${ADUC_PRIVATE_KEY_PASSWORD}"

inherit swupdate
```

#### sw-description file

This file locates at `meta-azure-device-update/recipes-extended/images/adu-update-image/raspberrypi3/sw-description`.  
This configuration file defines meta data about the update and the primary bootloader location.
Two files are referenced in the file and which partition each goes to.

```shell
copy1 on the device = location for partition A
copy2 on the device = location for partition B
```

The script looks at what partition being used.  If that partition is in use, it
will switch to using the other one.  A reboot is required to switch partitions.

```shell
software =
{
    version = "@@ADU_SOFTWARE_VERSION@@";
    raspberrypi3 = {
        hardware-compatibility: ["1.0"];
        stable = {
            copy1 : {
                images: (
                    {
                        filename = "adu-base-image-raspberrypi3.ext4.gz";
                        sha256 = "@adu-base-image-raspberrypi3.ext4.gz";
                        type = "raw";
                        compressed = true;
                        device = "/dev/mmcblk0p2";
                    }
                );
            };
            copy2 : {
                images: (
                    {
                        filename = "adu-base-image-raspberrypi3.ext4.gz";
                        sha256 = "@adu-base-image-raspberrypi3.ext4.gz";
                        type = "raw";
                        compressed = true;
                        device = "/dev/mmcblk0p3";
                    }
                );
            };
        }
    }
}
```

## Install and apply image

#### src/adu-shell/scripts/adu-swupdate.sh (https://github.com/Azure/iot-hub-device-update/blob/main/src/adu-shell/scripts/adu-swupdate.sh)

This is a shell script that provides install options.

```shell
# Call swupdate with the image file and the public key for signature validation
swupdate -v -i "${image_file}" -k /adukey/public.pem -e ${selection} &>> $LOG_DIR/swupdate.log
```

#### boot.cmd.in

This file locates at `meta-azure-device-update/raspberrypi/recipes-bsp/rpi-u-boot-scr/files/boot.cmd.in`.  
The boot.cmd.in looks for the 'selection' variable to know which partition to boot.

```shell
saveenv
fdt addr ${fdt_addr} && fdt get value bootargs /chosen bootargs
fatload mmc 0:1 ${kernel_addr_r} @@KERNEL_IMAGETYPE@@
if env exists rpipart;then setenv bootargs ${bootargs} root=/dev/mmcblk0p${rpipart}; fi
@@KERNEL_BOOTCMD@@ ${kernel_addr_r} - ${fdt_addr}
```