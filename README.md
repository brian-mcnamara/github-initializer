# GitHub Initializer

Simple broker that enables clients to securely upload a users SSH and/or GPG keys.
Clients upload their keys to `/upload` with a payload of
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
which will return a JSON payload containing the id and a redirect URL.

The Redirect will perform the GitHub oauth dance eventually redirecting the user back to this service
where a confirmation will ask the user to agree to upload on their behalf. Upon confirmation, the service
grabs an access token and proceeds to upload the keys.

## Endpoints

The following are the endpoints used by this service:

* `POST /upload`:  
    As mentioned above, this endpoint is uploads the users public keys to begin the service flow. Returns the id and redirect url.
* `GET /initialize`:  
    This endpoint is the `Authorization callback URL` used by the GitHub Oauth App. Accepts
    `code` and `state` params passed back from GH. Returns [verification.html](src/main/resources/templates/verification.html)
* `POST /perform`:  
    Performs the authentication and upload of the keys to GitHub. Requires: `code` the GH access code, 
    `id`: the transaction id, `csrf` a CSRF token from the `/initialize` endpoint. These parameters are 
    autofilled in the [verification.html](src/main/resources/templates/verification.html). Returns
    [result.html](src/main/resources/templates/result.html)
* `GET /status`:  
    Returns the status of the transaction

## Using client

You can use the client attached by invoking:
` ./client.py --sshFile /path/to/id_rsa.pub --gpgId [GpGKeyId]`

## Development

Add a `application-dev.properties` file under src/main/resources that contains
`client.id` and `client.secret` from the GitHub oauth app.