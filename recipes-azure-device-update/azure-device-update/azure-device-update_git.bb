# Build and install our ADU sample code.

# Environment variables that can be used to configure the behavior of this recipe.
# ADUC_GIT_BRANCH       Changes the branch that ADU code is pulled from.
# ADUC_SRC_URI          Changes the URI where the ADU code is pulled from.
#                       This URI follows the Yocto Fetchers syntax.
#                       See https://www.yoctoproject.org/docs/latest/ref-manual/ref-manual.html#var-SRC_URI
# BUILD_TYPE            Changes the type of build produced by this recipe.
#                       Valid values are Debug, Release, RelWithDebInfo, and MinRelSize.
#                       These values are the same as the CMAKE_BUILD_TYPE variable.

LICENSE = "CLOSED"

ADUC_GIT_BRANCH ?= "main"
ADUC_SRC_URI ?= "git://github.com/Azure/iot-hub-device-update;branch=${ADUC_GIT_BRANCH}"
SRC_URI = "${ADUC_SRC_URI}"
SRCREV = "2cd165ba05cf7a6eff34cbf6c26e4ae30cc9d5d9"

# This code handles setting variables for either git or for a local file.
# This is only while we are using private repos, once our repos are public,
# we will just use git.
python () {
    src_uri = d.getVar('ADUC_SRC_URI')
    if src_uri.startswith('git'):
        d.setVar('PV', '1.0+git' + d.getVar('SRCPV'))
        d.setVar('S', d.getVar('WORKDIR') + "/git")
    elif src_uri.startswith('file'):
        d.setVar('S',  d.getVar('WORKDIR') + "/adu-linux-client")
}

# ADUC depends on azure-iot-sdk-c, azure-blob-storage-file-upload-utility, DO Agent SDK, and curl
DEPENDS = "azure-iot-sdk-c azure-blob-storage-file-upload-utility deliveryoptimization-agent deliveryoptimization-sdk curl"

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
# curl - for running the diagnostics component
RDEPENDS_${PN} += "bash swupdate adu-pub-key adu-device-info-files adu-hw-compat adu-log-dir deliveryoptimization-agent-service curl"

INSANE_SKIP_${PN} += "installed-vs-shipped"

ADUC_DATA_DIR = "/var/lib/adu"
ADUC_EXTENSIONS_DIR = "${ADUC_DATA_DIR}/extensions"
ADUC_EXTENSIONS_INSTALL_DIR = "${ADUC_EXTENSIONS_DIR}/sources"
ADUC_COMPONENT_ENUMERATOR_EXTENSION_DIR = "${ADUC_EXTENSIONS_DIR}/component_enumerator"
ADUC_CONTENT_DOWNLOADER_EXTENSION_DIR = "${ADUC_EXTENSIONS_DIR}/content_downloader"
ADUC_UPDATE_CONTENT_HANDLER_EXTENSION_DIR = "${ADUC_EXTENSIONS_DIR}/update_content_handlers"
ADUC_DOWNLOADS_DIR = "${ADUC_DATA_DIR}/downloads"

ADUC_LOG_DIR = "/adu/logs"
ADUC_CONF_DIR = "/adu"

ADUUSER = "adu"
ADUGROUP = "adu"
DOUSER = "do"
DOGROUP = "do"

USERADD_PACKAGES = "${PN}"

GROUPADD_PARAM_${PN} = "\
    --gid 800 --system adu ; \
    --gid 801 --system do ; \
    "

# USERADD_PARAM specifies command line options to pass to the
# useradd command. Multiple users can be created by separating
# the commands with a semicolon. 
# Here we'll create 'adu' user, and 'do' user.
# To download the update payload file, 'adu' user must be a member of 'do' group.
# To save downloaded file into 'adu' downloads directory, 'do' user must be a member of 'adu' group.
USERADD_PARAM_${PN} = "\
    --uid 800 --system -g ${ADUGROUP} -G ${DOGROUP} --home-dir /home/${ADUUSER} --no-create-home --shell /bin/false ${ADUUSER} ; \
    --uid 801 --system -g ${DOGROUP} -G ${ADUGROUP} --home-dir /home/${DOUSER} --no-create-home --shell /bin/false ${DOUSER} ; \
    "
    
do_install_append() {
    #create ADUC_DATA_DIR
    install -d ${D}${ADUC_DATA_DIR}
    chgrp ${ADUGROUP} ${D}${ADUC_DATA_DIR}
    chown ${ADUUSER}:${ADUGROUP} ${D}${ADUC_DATA_DIR}
    chmod 0770 ${D}${ADUC_DATA_DIR}

    #create ADUC_EXTENSIONS_DIR
    install -d ${D}${ADUC_EXTENSIONS_DIR}
    chgrp ${ADUGROUP} ${D}${ADUC_EXTENSIONS_DIR}
    chmod 0770 ${D}${ADUC_EXTENSIONS_DIR}

    #create ADUC_EXTENSIONS_INSTALL_DIR
    install -d ${D}${ADUC_EXTENSIONS_INSTALL_DIR}
    chgrp ${ADUGROUP} ${D}${ADUC_EXTENSIONS_INSTALL_DIR}
    chmod 0770 ${D}${ADUC_EXTENSIONS_INSTALL_DIR}

    #create ADUC_COMPONENT_ENUMERATOR_EXTENSION_DIR
    install -d ${D}${ADUC_COMPONENT_ENUMERATOR_EXTENSION_DIR}
    chgrp ${ADUGROUP} ${D}${ADUC_COMPONENT_ENUMERATOR_EXTENSION_DIR}
    chmod 0770 ${D}${ADUC_COMPONENT_ENUMERATOR_EXTENSION_DIR}

    #create ADUC_CONTENT_DOWNLOADER_EXTENSION_DIR
    install -d ${D}${ADUC_CONTENT_DOWNLOADER_EXTENSION_DIR}
    chgrp ${ADUGROUP} ${D}${ADUC_CONTENT_DOWNLOADER_EXTENSION_DIR}
    chmod 0770 ${D}${ADUC_CONTENT_DOWNLOADER_EXTENSION_DIR}

    #create ADUC_UPDATE_CONTENT_HANDLER_EXTENSION_DIR
    install -d ${D}${ADUC_UPDATE_CONTENT_HANDLER_EXTENSION_DIR}
    chgrp ${ADUGROUP} ${D}${ADUC_UPDATE_CONTENT_HANDLER_EXTENSION_DIR}
    chmod 0770 ${D}${ADUC_UPDATE_CONTENT_HANDLER_EXTENSION_DIR}

    #create ADUC_DOWNLOADS_DIR
    install -d ${D}${ADUC_DOWNLOADS_DIR}
    chown ${ADUUSER}:${ADUGROUP} ${D}${ADUC_DOWNLOADS_DIR}
    chmod 0770 ${D}${ADUC_DOWNLOADS_DIR}

    #create ADUC_CONF_DIR
    install -d ${D}${ADUC_CONF_DIR}
    chgrp root ${D}${ADUC_CONF_DIR}
    chown root:${ADUGROUP} ${D}${ADUC_CONF_DIR}
    chmod 0774 ${D}${ADUC_CONF_DIR}

    #create ADUC_LOG_DIR
    install -d ${D}${ADUC_LOG_DIR}
    chown ${ADUUSER}:${ADUGROUP} ${D}${ADUC_LOG_DIR}
    chmod 0774 ${D}${ADUC_LOG_DIR}

    #install adu-shell to /usr/lib/adu directory.
    install -d ${D}${libdir}/adu

    install -m 0550 ${S}/src/adu-shell/scripts/adu-swupdate.sh ${D}${libdir}/adu
    chown ${ADUUSER}:${ADUGROUP} ${D}${libdir}/adu

    #set owner for adu-shell
    chmod 0550 ${D}${libdir}/adu/adu-shell
    chown root:${ADUGROUP} ${D}${libdir}/adu/adu-shell

    #set S UID for adu-shell
    chmod u+s ${D}${libdir}/adu/adu-shell
}

FILES_${PN} += "${bindir}/AducIotAgent"
FILES_${PN} += "${libdir}/adu/* ${ADUC_DATA_DIR}/* ${ADUC_LOG_DIR}/* ${ADUC_CONF_DIR}/*"
FILES_${PN} += "${ADUC_EXTENSIONS_DIR}/* ${ADUC_EXTENSIONS_INSTALL_DIR}/* ${ADUC_DOWNLOADS_DIR}/*"
FILES_${PN} += "${ADUC_COMPONENT_ENUMERATOR_EXTENSION_DIR}/* ${ADUC_CONTENT_DOWNLOADER_EXTENSION_DIR}/* ${ADUC_UPDATE_CONTENT_HANDLER_EXTENSION_DIR}/*"
FILES_${PN} += "/home/${ADUUSER}/* /home/$(DOUSER)/*"

