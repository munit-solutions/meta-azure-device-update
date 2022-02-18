SUMMARY = "Register Device Update extensions upon first boot"
DESCRIPTION = "Perform first boot initialization, started as a systemd service which removes itself once finished"
LICENSE = "CLOSED"

SRC_URI =  " \
    file://register-adu-extensions.sh \
    file://register-adu-extensions.service \
"

do_compile () {
}   

do_install () {
    install -d ${D}/${sbindir}
    install -m 0755 ${WORKDIR}/register-adu-extensions.sh ${D}/${sbindir}

    install -d ${D}${systemd_unitdir}/system/
    install -m 0644 ${WORKDIR}/register-adu-extensions.service ${D}${systemd_unitdir}/system
}

DEPENDS_${PN} += "azure-device-update"

RDEPENDS_${PN} += "azure-device-update"

NATIVE_SYSTEMD_SUPPORT = "1"
SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE_${PN} = "register-adu-extensions.service"

inherit allarch systemd