[![Build Status](https://travis-ci.org/brian-mcnamara/github-initializer.svg?branch=master)](https://travis-ci.org/brian-mcnamara/github-initializer)
[![Coverage Status](https://coveralls.io/repos/github/brian-mcnamara/github-initializer/badge.svg?branch=master)](https://coveralls.io/github/brian-mcnamara/github-initializer?branch=master)

# GitHub Initializer

Simple broker that provides API endpoints to securely upload a users SSH and/or GPG keys in order to
upload them on behalf of the user after an Oauth dance.

The client upload their keys to `/upload` with a payload of
```json
{
    "sshKey": {
        "title" : "title",
        "key": "ssh-rsa..."
    },
    "gpgKey": {
        "armored_public_key": "-----BEGIN PGP..."
    }
}
```
which will return a JSON payload containing the id and the redirect URL pointing to the GitHub aouth authentication endpoint.

The Redirect will perform a Oauth dance eventually redirecting the user back to this service
where a confirmation will require the user to agree to upload on their behalf. Upon confirmation, the service
grabs an access token and proceeds to upload the keys.

## Endpoints

The following are the endpoints used by this service:

* `POST /upload`:  
    As mentioned above, Clients upload their public keys to this endpoint, initializing the service flow; returns the id and redirect url.
* `GET /initiate`:  
    This endpoint is the `Authorization callback URL` used by the GitHub Oauth App. Accepts
    `code` and `state` params passed back from GH. Returns [verification.html](src/main/resources/templates/verification.html)
* `POST /perform`:  
    Performs the authentication and upload of the keys to GitHub. Requires: `code`: the GH access code, 
    `id`: the transaction id, `csrf`: a CSRF token from the `/initiate` endpoint. These parameters are 
    autofilled in the [verification.html](src/main/resources/templates/verification.html). Returns
    [result.html](src/main/resources/templates/result.html)
* `GET /status`:  
    Returns the status of the transaction

## Using client

You can use the client attached by invoking:
` ./client.py upload --sshFile /path/to/id_rsa.pub --gpgId [GpGKeyId]`

## Development

Add a `application-dev.properties` file under src/main/resources that contains
`client.id` and `client.secret` from the GitHub Oauth app.