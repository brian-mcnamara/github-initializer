# GitHub Initializer

Simple broker that enables clients to securely upload a users SSH and GPG keys.
Clients upload their keys to `/upload` with a payload of
```json
{
    "sshKey": {
        "title" : title,
        "key": "ssh-ras..."
    },
    "gpgKey": {
        "armored_public_key": "-----BEGIN PGP..."
    }
}
```

A example client can be found in this repo name client.py

## Development

Add a `application-dev.properties` file under src/main/resources that contains
`client.id` and `client.secret` from the GitHub oauth app.