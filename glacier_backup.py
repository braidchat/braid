#!/usr/bin/env python

from boto.glacier.layer1 import Layer1
from boto.glacier.concurrent import ConcurrentUploader
import sys
import os.path

# XXX: replace these with your credentials
ACCESS_KEY = "AWS_ACCESS_KEY"
SECRET_KEY = "AWS_SECRET_KEY"
VAULT_NAME = "VAULT_NAME"
REGION_NAME = 'us-west-2'

try:
    backup_file = sys.argv[1]
except IndexError:
    sys.stderr.write("Usage: {} <file to backup>\n".format(sys.argv[0]))
    sys.exit(1)
if not os.path.isfile(backup_file):
    sys.stderr.write("Bad upload file {}\n".format(backup_file))
    sys.exit(2)

glacier_layer1 = Layer1(aws_access_key_id=ACCESS_KEY,
                        aws_secret_access_key=SECRET_KEY,
                        region_name=REGION_NAME)

uploader = ConcurrentUploader(glacier_layer1, VAULT_NAME, 32*1024*1024)

sys.stdout.write("Uploading {} as {}...".format(
    backup_file, os.path.basename(backup_file)))
archive_id = uploader.upload(backup_file, os.path.basename(backup_file))
sys.stdout.write("done\n")
