# Copy/install fw_env.config which is necessary
# for fw utils (fw_*) like fw_printenv and fw_setenv

LICENSE = "CLOSED"

SRC_URI = "file://fw_env.config"

do_install() {
    install -d ${D}${sysconfdir}
    install -m 644 ${WORKDIR}/fw_env.config ${D}${sysconfdir}/fw_env.config
}

FILES_${PN} += "${sysconfdir}/fw_env.config"
