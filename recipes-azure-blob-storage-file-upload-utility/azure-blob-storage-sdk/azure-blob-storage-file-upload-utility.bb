# Build and install the azure-blob-storage-file-upload-utility

DESCRIPTION = "Microsoft Azure Blob Storage File Upload Utility"
AUTHOR = "Microsoft Corporation"
HOMEPAGE = "https://github.com/Azure/azure-blob-storage-file-upload-utility"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=d4a904ca135bb7bc912156fee12726f0"

FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}:"

SRC_URI = "gitsm://github.com/Azure/azure-blob-storage-file-upload-utility.git;branch=main"

SRCREV = "${AUTOREV}"
PV = "1.0+git${SRCPV}"

S = "${WORKDIR}/git"

# util-linux for uuid-dev
# libxml2 for libxml2-dev
DEPENDS = "util-linux azure-iot-sdk-c azure-sdk-for-cpp curl openssl libxml2"

inherit cmake

sysroot_stage_all_append () {
    sysroot_stage_dir ${D}${exec_prefix}/cmake ${SYSROOT_DESTDIR}${exec_prefix}/cmake
}

FILES_${PN}-dev += "${exec_prefix}/cmake"

BBCLASSEXTEND = "native nativesdk"
