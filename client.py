#!/usr/bin/env python3

import webbrowser
import argparse
import http.client
import json
import subprocess

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
            "title" : "test",
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
    redirect = response.read().decode()
    webbrowser.open(redirect, 1)

if __name__ == "__main__":
    main()