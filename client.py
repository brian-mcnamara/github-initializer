#!/usr/bin/env python3

import argparse
import socket
import time
import requests
import subprocess
import os
import sys

def verify(host, id):
    while True:
        time.sleep(2)
        def checkStatus():
            response = requests.get(host + "/status?id=" + id)
            if response.status_code == 404:
                print("Failed to get status")
                exit(1)
            elif response.status_code == 200:
                failed = False
                status = response.json()

                for entry in status:
                    progress = status[entry]['progress']
                    if progress == 'IN_PROGRESS':
                        return
                    elif progress == "COMPLETE":
                        if 'error' in status[entry] and status[entry]['error'] is not None:
                            if 'is already in use' not in status[entry]['error'] and 'already exists' not in status[entry]['error']:
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


def upload(host, sshFile, gpgFile, gpgId):
    if sshFile is None and gpgFile is None and gpgId is None:
        print("Must specify one key to upload")
        exit(1)
    if gpgFile is not None and gpgId is not None:
        print("Can not have both gpgId and gpgFile")
        exit(1)
    body = {}
    if sshFile is not None:
        f=open(sshFile, "r")
        body["sshKey"] = {
            "title" : socket.gethostname(),
            "key": f.read()
        }
    if gpgId is not None:
        key = subprocess.getoutput("gpg --armor --export " + gpgId)
        body["gpgKey"] = {
            "armored_public_key" : key
        }
    elif gpgFile is not None:
        f=open(gpgFile, "r")
        body["gpgKey"] = {
            "armored_public_key" : f.read()
        }
    headers = {'Content-type': 'application/json'}
    response = requests.post(host + "/upload", json=body, headers=headers)
    if response.status_code is not 200:
        print("Failed to upload to server")
        exit(1)

    return response.json()


def openBrowser(url):
    #webbrowser has some system erros I cant figure out how to suppress, this is the only way I found...
    FNULL = open(os.devnull, 'w')
    subprocess.run(['python3', '-m', 'webbrowser', '-t', url], stdout=FNULL, stderr=subprocess.STDOUT)


parser = argparse.ArgumentParser(description='Upload users SSH and GPG keys to GitHub')
parser.add_argument('--host', dest="host", default="https://gh-initializer.herokuapp.com",
                    help='The GitHub-initializer to connect to')

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: client.py (upload|verify) [options]")
        exit(1)
    command = sys.argv[1]
    if command == 'verify':
        parser.add_argument('id', help="The ID to verify")
        args = parser.parse_args(sys.argv[2:])
        verify(args.host, args.id)
    elif command == 'upload':
        parser.add_argument('--sshFile', dest="sshFile", help='The SSH public key')
        parser.add_argument('--gpgFile', dest="gpgFile", help='The gpg armored public key file')
        parser.add_argument('--gpgId', dest="gpg", help='The GPG long keyid')
        parser.add_argument('--no-verify', dest='noVerify', help='Disable verification, will print out redirect and uuid for use', action='store_true')
        parser.add_argument('--headless', dest='headless', help='Disable opening browser', action='store_true')
        args = parser.parse_args(sys.argv[2:])
        response = upload(args.host, args.sshFile, args.gpgFile, args.gpg)
        if not args.headless:
            openBrowser(response["redirect"])
            print("Check your web browser to finish uploading.")
        if args.headless or args.noVerify:
            print("redirect=" + response["redirect"])
            print("id=" + response["id"])
        if not args.noVerify:
            verify(args.host, response["id"])
    else:
        print("Must specify the command, either verify, or upload.")
        exit(1)