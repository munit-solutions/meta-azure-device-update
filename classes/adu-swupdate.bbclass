inherit swupdate

python do_adu_swuimage() {
    import shutil
    import datetime
    import subprocess
    import base64

    deploydir = d.getVar('DEPLOY_DIR_IMAGE', True)

    workdir = d.getVar('WORKDIR', True)
    shutil.copyfile(os.path.join(workdir, "manifest.json.template"), os.path.join(workdir, "manifest.json"))

    image_name = d.getVar('IMAGE_LINK_NAME', True) + '.swu'
    image_path = os.path.join(deploydir, image_name)
    image_sha265 = base64.b64encode(subprocess.check_output('openssl dgst -sha256 -binary "%s"' % (image_path), shell=True)).decode('ascii')
    image_size = subprocess.check_output('stat -Lc %%s %s' % (image_path), shell=True).decode('ascii').rstrip()

    replaceset = [
        ['FILENAME',              image_name],
        ['SIZE',                  image_size],
        ['SHA265',                image_sha265],
        ['MANUFACTURER',          d.getVar('MANUFACTURER', True)],
        ['MODEL',                 d.getVar('MODEL', True)],
        ['ADU_SOFTWARE_PROVIDER', d.getVar('ADU_SOFTWARE_PROVIDER', True)],
        ['ADU_SOFTWARE_NAME',     d.getVar('ADU_SOFTWARE_NAME', True)],
        ['ADU_SOFTWARE_VERSION',  d.getVar('ADU_SOFTWARE_VERSION', True)],
        ['ADU_INSTALLED_CRITERIA', d.getVar('ADU_INSTALLED_CRITERIA', True)],
        ['CREATED_DATE_TIME',     datetime.datetime.utcnow().replace(tzinfo=datetime.timezone.utc).isoformat()],
    ]
    for replace in replaceset:
        os.system('sed -i "s|<%s>|%s|g" %s' % (replace[0], replace[1], os.path.join(workdir, "manifest.json")))

    os.system("cd " + workdir + "; " + "")
    shutil.copyfile(os.path.join(workdir, "manifest.json"), os.path.join(deploydir, d.getVar('IMAGE_NAME', True) + "-manifest.json"))

    line = 'ln -sf ' + d.getVar('IMAGE_NAME', True) + "-manifest.json " + d.getVar('IMAGE_LINK_NAME', True) + "-manifest.json"
    os.system("cd " + deploydir + "; " + line)
}

deltask do_swuimage
addtask do_swuimage after do_image_complete before do_build
addtask do_adu_swuimage after do_swuimage before do_build
