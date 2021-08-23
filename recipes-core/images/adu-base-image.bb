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
    adu-agent-service \
    "

export IMAGE_BASENAME = "adu-base-image"

# This must be at the bottom of this file.
inherit core-image
