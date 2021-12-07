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
SRC_URI = "${DO_SRC_URI} \
    file://0001-Add-boost-dependancies.patch \
    "
SRCREV = "cd2eaadc0755c93bb31234a19a4577aced9845e5"

# This code handles setting variables for either git or for a local file.
# This is only while we are using private repos, once our repos are public,
# we will just use git.
python () {
    src_uri = d.getVar('DO_SRC_URI')
    if src_uri.startswith('git'):
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