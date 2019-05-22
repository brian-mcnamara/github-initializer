# GitHub Initializer

Simple broker that enables clients to securely upload a users SSH and GPG keys.
Clients upload their keys to `/upload` with a payload of
```json
{
    "sshKey": {
        "title" : title,
        "key": "ssh-rsa..."
    },
    "gpgKey": {
        "armored_public_key": "-----BEGIN PGP..."
    }
}
```
which will return a JSON payload containing the UUID identifier and a redirect URL.

The Redirect will preform the GitHub oauth dance eventually redirecting the user back to this service
where a confirmation will ask the user to agree to upload on behalf of them. Upon confirmation, the service
grabs an access token and proceeds to upload the keys.


## Using client

You can use the client attached by invoking:
` ./client.py --sshFile /path/to/id_rsa.pub --gpg [GpGKeyId]`

## Development

Add a `application-dev.properties` file under src/main/resources that contains
`client.id` and `client.secret` from the GitHub oauth app.