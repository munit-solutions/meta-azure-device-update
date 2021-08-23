# Generates and copies/installs the public key .pem file
# used to validate the signatures of images.
# Note: ADU reference images are signed with test keys.

LICENSE="CLOSED"

# Path in the image to place the generated public key file.
ADUC_KEY_DIR = "/adukey"

DEPENDS = "openssl-native"

# Generated RSA key with password using command:
# openssl genrsa -aes256 -passout file:priv.pass -out priv.pem

# These variables can be overriden via whitelisted environment variables:
# ADUC_PRIVATE_KEY is the build host path to the .pem private key file to use to sign the image.
# ADUC_PRIVATE_KEY_PASSWORD is the build host path to the .pass password file for the private key.

# Generate the public key file using openssl, private key, and password file.
do_compile() {
    openssl rsa -in ${ADUC_PRIVATE_KEY} -passin file:${ADUC_PRIVATE_KEY_PASSWORD} -out public.pem -outform PEM -pubout
}

# Install the public key file to ADUC_KEY_DIR
do_install() {
    install -d ${D}${ADUC_KEY_DIR}
    install -m 0444 public.pem ${D}${ADUC_KEY_DIR}/public.pem
}

FILES_${PN} += "${ADUC_KEY_DIR}/public.pem"

inherit allarch
