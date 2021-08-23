#
# SPDX-License-Identifier: MIT
#

# The adufs wic plugin is used to create a partitions for wic images
# containing the file "adu-conf.txt" used for Azure Device Update.
# To use it you must pass "adufs" as argument for the "--source" parameter in
# the wks file. For example:
# part foo --source adufs --ondisk sda --size="1024" --align 1024

import logging

from wic.pluginbase import SourcePlugin
from wic.misc import (exec_cmd, exec_native_cmd,
                      get_bitbake_var)

logger = logging.getLogger('wic')

class AduPartitionPlugin(SourcePlugin):
    """
    Populates a partition usable for Azure Device Update
    """

    name = 'adufs'

    @classmethod
    def do_configure_partition(cls, part, source_params, creator, cr_workdir,
                               oe_builddir, audimg_dir, kernel_dir,
                               native_sysroot):
        """
        Called before do_prepare_partition(), creates adu config
        """
        hdddir = "%s/hdd/adu" % cr_workdir

        install_cmd = "install -d %s" % hdddir
        exec_cmd(install_cmd)

        bootloader = creator.ks.bootloader

        adu_conf = ""
        adu_conf += "connection_string=<ADD DEVICE CONNECTION STRING HERE>\n"
        adu_conf += "aduc_manufacturer=%s\n" % (get_bitbake_var("MANUFACTURER"))
        adu_conf += "aduc_model=%s\n" % (get_bitbake_var("MODEL"))

        logger.debug("Writing adu config %s/hdd/adu/adu-conf.txt",
                     cr_workdir)
        cfg = open("%s/hdd/adu/adu-conf.txt" % cr_workdir, "w")
        cfg.write(adu_conf)
        cfg.close()

    @classmethod
    def do_prepare_partition(cls, part, source_params, creator, cr_workdir,
                             oe_builddir, audimg_dir, kernel_dir,
                             rootfs_dir, native_sysroot):
        """
        Called to do the actual content population for a partition i.e. it
        'prepares' the partition to be incorporated into the image.
        In this case, prepare content for legacy bios adu partition.
        """
        hdddir = "%s/hdd/adu" % cr_workdir

        du_cmd = "du -bks %s" % hdddir
        out = exec_cmd(du_cmd)
        blocks = int(out.split()[0])

        extra_blocks = part.get_extra_block_count(blocks)
        blocks += extra_blocks

        logger.debug("Added %d extra blocks to %s to get to %d total blocks",
                     extra_blocks, part.mountpoint, blocks)

        # dosfs image, created by mkdosfs
        aduimg = "%s/adu%s.img" % (cr_workdir, part.lineno)

        dosfs_cmd = "mkdosfs -n adu -i %s -S 512 -C %s %d" % \
                    (part.fsuuid, aduimg, blocks)
        exec_native_cmd(dosfs_cmd, native_sysroot)

        mcopy_cmd = "mcopy -i %s -s %s/* ::/" % (aduimg, hdddir)
        exec_native_cmd(mcopy_cmd, native_sysroot)

        chmod_cmd = "chmod 644 %s" % aduimg
        exec_cmd(chmod_cmd)

        du_cmd = "du -Lbks %s" % aduimg
        out = exec_cmd(du_cmd)
        aduimg_size = out.split()[0]

        part.size = int(aduimg_size)
        part.source_file = aduimg
