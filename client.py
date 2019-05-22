#!/usr/bin/env python3

import webbrowser
import argparse
import http.client
import json
import subprocess
import socket
import time
import urllib.request
import urllib.parse

parser = argparse.ArgumentParser(description='Upload users SSH and GPG keys to GitHub')
parser.add_argument('--host', dest="host", default="https://gh-initializer.herokuapp.com",
                    help='The GitHub-initializer to connect to')

parser.add_argument('--sshFile', dest="ssh", help='The SSH public key')
parser.add_argument('--gpgFile', dest="gpgFile", help='The gpg armored public key file')
parser.add_argument('--gpgId', dest="gpg", help='The GPG long keyid')

def main():
    args = parser.parse_args()
    if args.gpgFile is not None and args.gpg is not None:
        print("Can not have both gpgId and gpgFile")
        exit(1)

    body = {}
    if args.ssh is not None:
        f=open(args.ssh, "r")
        body["sshKey"] = {
            "title" : socket.gethostname(),
            "key": f.read()
        }
    if args.gpg is not None:
        key = subprocess.getoutput("gpg --armor --export " + args.gpg)
        body["gpgKey"] = {
            "armored_public_key" : key
        }
    elif args.gpgFile is not None:
        f=open(args.gpgFile, "r")
        body["gpgKey"] = {
            "armored_public_key" : f.read()
        }
    headers = {'Content-type': 'application/json'}
    data = str.encode(json.dumps(body))
    request = urllib.request.Request(args.host + "/upload", data, headers)
    with urllib.request.urlopen(request) as response:
        if response.code is not 200:
            print("Failed to upload to server")
            exit(1)
        resBody = response.read().decode('utf-8')
        jsonResponse = json.loads(resBody)
        redirect = jsonResponse["redirect"]
        webbrowser.open(redirect, 1)
        print("Check your web browser to finish uploading.")

    while True:
        time.sleep(2)
        with urllib.request.urlopen(args.host + "/status?uuid=" + jsonResponse["uuid"]) as response:
            if response.code is not 200:
                print("Failed to get status")
                exit(1)
            status = response.read().decode()
            if status == "INITIATED" or status == "IN_PROGRESS":
                continue
            elif status == "FAILED":
                print("Failed to upload keys")
                exit(1)
            elif status == "COMPLETE":
                print("Successfully uploaded keys")
                exit(0)
            else:
                print("Unknown status " + status)
                exit(1)


if __name__ == "__main__":
    main()