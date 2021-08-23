# Overrides the default boot loader script that is defined in meta-raspberrypi
# Key line in the boot loader script is:
# if env exists rpipart;then setenv bootargs ${bootargs} root=/dev/mmcblk0p${rpipart}; fi
# This adds the ability to change which partition is used for boot into OS
# and can be changed with the rpipart firmware environment variable.
FILESEXTRAPATHS_prepend := "${THISDIR}/files:"
