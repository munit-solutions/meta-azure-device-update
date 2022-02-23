#!/bin/sh

AGENT="AducIotAgent"
STATFILE="/adu/default-extensions-registered"

logger "Start initializing Device Update extensions"

# Register a default update manifest handler.
/usr/bin/${AGENT} --update-type "microsoft/update-manifest" -C /var/lib/adu/extensions/sources/libmicrosoft_steps_1.so

# Register an update manifest v4 handler.
/usr/bin/${AGENT} --update-type "microsoft/update-manifest:4" -C /var/lib/adu/extensions/sources/libmicrosoft_steps_1.so

# Register swupdate content handler.
logger "Register an update content handler for 'microsoft/swupdate:1"

/usr/bin/${AGENT} --update-type "microsoft/script:1" -C /var/lib/adu/extensions/sources/libmicrosoft_script_1.so
/usr/bin/${AGENT} --update-type "microsoft/steps:1" -C /var/lib/adu/extensions/sources/libmicrosoft_steps_1.so
/usr/bin/${AGENT} --update-type "microsoft/swupdate:1" -C /var/lib/adu/extensions/sources/libmicrosoft_swupdate_1.so
/usr/bin/${AGENT} --update-type "microsoft/update-manifest" -C /var/lib/adu/extensions/sources/libmicrosoft_steps_1.so
/usr/bin/${AGENT} --update-type "microsoft/update-manifest:4" -C /var/lib/adu/extensions/sources/libmicrosoft_steps_1.so

# Register a script handler.
/usr/bin/${AGENT} --update-type "microsoft/script:1" -C /var/lib/adu/extensions/sources/libmicrosoft_script_1.so

# Register Delivery Optimization content downloader extension.
logger "Register a content downloader (Delivery Optimization agent)"
/usr/bin/${AGENT} -D /var/lib/adu/extensions/sources/libdeliveryoptimization-content-downloader.so

logger "Device Update extensions registration completed"

# Job done, remove it from systemd services
systemctl disable register-adu-extensions.service

