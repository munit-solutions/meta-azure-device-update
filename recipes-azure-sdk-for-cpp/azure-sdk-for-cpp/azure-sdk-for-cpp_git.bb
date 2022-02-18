# Build and install the azure-blob-storage-file-upload-utility

DESCRIPTION = "Microsoft Azure SD for CPP"
AUTHOR = "Microsoft Corporation"
HOMEPAGE = "https://github.com/Azure/azure-sdk-for-cpp"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=e74f78882cab57fd1cc4c5482b9a214a"

FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}:"

SRC_URI = "gitsm://github.com/Azure/azure-sdk-for-cpp.git;branch=main"

SRCREV = "${AUTOREV}"
PV = "1.0+git${SRCPV}"

S = "${WORKDIR}/git"

# util-linux for uuid-dev
# libxml2 for libxml2-dev
DEPENDS = "util-linux curl openssl libxml2"

inherit cmake

sysroot_stage_all_append () {
    sysroot_stage_dir ${D}${exec_prefix}/cmake ${SYSROOT_DESTDIR}${exec_prefix}/cmake
}

FILES_${PN}-dev += "${exec_prefix}/cmake"

FILES_${PN} = "/usr/share/azure-storage-blobs-cpp \
                /usr/share/azure-storage-queues-cpp \
                /usr/share/azure-storage-common-cpp \
                /usr/share/azure-storage-files-shares-cpp \
                /usr/share/azure-security-keyvault-secrets-cpp \
                /usr/share/azure-security-keyvault-certificates-cpp \
                /usr/share/azure-security-keyvault-keys-cpp \
                /usr/share/azure-identity-cpp \
                /usr/share/azure-template-cpp \
                /usr/share/azure-core-cpp \
                /usr/share/azure-storage-files-datalake-cpp"

BBCLASSEXTEND = "native nativesdk"
