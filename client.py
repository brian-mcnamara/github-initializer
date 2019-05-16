#!/usr/bin/env python3

import webbrowser
import argparse
import http.client
import json

parser = argparse.ArgumentParser(description='Upload users SSH and GPG keys to GitHub')
parser.add_argument('--host', dest="host", default="localhost:8080",
                    help='The GitHub-initializer to connect to')

parser.add_argument('--sshFile', dest="ssh", help='The SSH public key')
parser.add_argument('--gpg', dest="gpg", help='The GPG public key')

def main():
    args = parser.parse_args()
    print(args.host)
    connection = http.client.HTTPConnection(args.host)
    f=open(args.ssh, "r")
    body = {"sshKey" : {
        "title" : "test",
        "key": f.read()
    }}
    headers = {'Content-type': 'application/json'}
    connection.request('POST', '/upload', json.dumps(body), headers)
    response = connection.getresponse()
    redirect = response.read().decode()
    webbrowser.open(redirect, 1)

if __name__ == "__main__":
    main()