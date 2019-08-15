#!/usr/bin/env python3

import argparse
import socket
import time
import requests
import subprocess
import os

parser = argparse.ArgumentParser(description='Upload users SSH and GPG keys to GitHub')
parser.add_argument('--host', dest="host", default="https://gh-initializer.herokuapp.com",
                    help='The GitHub-initializer to connect to')

parser.add_argument('--sshFile', dest="sshFile", help='The SSH public key')
parser.add_argument('--gpgFile', dest="gpgFile", help='The gpg armored public key file')
parser.add_argument('--gpgId', dest="gpg", help='The GPG long keyid')

def main():
    args = parser.parse_args()
    if args.gpgFile is not None and args.gpg is not None:
        print("Can not have both gpgId and gpgFile")
        exit(1)

    body = {}
    if args.sshFile is not None:
        f=open(args.sshFile, "r")
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
    response = requests.post(args.host + "/upload", json=body, headers=headers)
    if response.status_code is not 200:
        print("Failed to upload to server")
        exit(1)

    jsonResponse = response.json()
    redirect = jsonResponse["redirect"]
    #webbrowser has some system erros I cant figure out how to suppress, this is the only way I found...
    FNULL = open(os.devnull, 'w')
    subprocess.run(['python3', '-m', 'webbrowser', '-t', redirect], stdout=FNULL, stderr=subprocess.STDOUT)
    print("Check your web browser to finish uploading.")

    while True:
        time.sleep(2)
        def checkStatus():
            response = requests.get(args.host + "/status?id=" + jsonResponse["id"])
            if response.status_code is 404:
                print("Failed to get status")
                exit(1)
            elif response.status_code is 200:
                failed = False
                status = response.json()

                for entry in status:
                    progress = status[entry]['progress']
                    if progress == 'IN_PROGRESS':
                        return
                    elif progress == "COMPLETE":
                        if 'error' in status[entry] and status[entry]['error'] is not None:
                            failed = True
                            print("Failed to upload " + status[entry]['type'] + " key: " + status[entry]['error'])
                        else:
                            print("Successfully uploaded " + status[entry]['type'] + " key")
                    else:
                        print("Unknown status " + progress)
                if failed:
                    exit(1)
                else:
                    exit(0)
        checkStatus()


if __name__ == "__main__":
        main()