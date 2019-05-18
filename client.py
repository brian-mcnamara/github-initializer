#!/usr/bin/env python3

import webbrowser
import argparse
import http.client
import json
import subprocess
import socket
import time

parser = argparse.ArgumentParser(description='Upload users SSH and GPG keys to GitHub')
parser.add_argument('--host', dest="host", default="localhost:8080",
                    help='The GitHub-initializer to connect to')

parser.add_argument('--sshFile', dest="ssh", help='The SSH public key')
parser.add_argument('--gpgId', dest="gpg", help='The GPG long keyid')

def main():
    args = parser.parse_args()
    connection = http.client.HTTPConnection(args.host)
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

    headers = {'Content-type': 'application/json'}
    connection.request('POST', '/upload', json.dumps(body), headers)
    response = connection.getresponse()
    if response.status is not 200:
        print("Failed to upload to server")
        exit(1)
    jsonResponse = json.loads(response.read().decode())
    redirect = jsonResponse["redirect"]
    webbrowser.open(redirect, 1)
    print("Check your webbrowser to finish uploading.")

    while True:
        time.sleep(2)
        connection.request('GET', "/status?uuid=" + jsonResponse["uuid"])
        response = connection.getresponse()
        if response.status is not 200:
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