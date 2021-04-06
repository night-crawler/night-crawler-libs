[![Build Status](https://travis-ci.com/night-crawler/night-crawler-libs.svg?branch=main)](https://travis-ci.com/night-crawler/night-crawler-libs)
[![codecov](https://codecov.io/gh/night-crawler/night-crawler-libs/branch/main/graph/badge.svg?token=yEqbsMUuxM)](https://codecov.io/gh/night-crawler/night-crawler-libs)

# night-crawler-libs

### Publishing

#### Publishing to Maven Central

Publishing extension has been preconfigured for deployment to Maven Central repository via OSSRH. A jar file with
documentation (`javadoc.jar`) is created with Dokka. In order to sigh the publication, you have to provide one of the
following sets of environmental variables:

1)
    * SIGN_KEY_ID - The public key ID (The last 8 symbols of the keyId)
    * SIGN_KEY - The secret (private) key
    * SIGN_KEY_PASSPHRASE - The passphrase used to protect your private key

2)
    * SIGN_KEY - The secret (private) key
    * SIGN_KEY_PASSPHRASE - The passphrase used to protect your private key

For more information about signing the publication, please refer to
the [Signing Plugin readme](https://docs.gradle.org/current/userguide/signing_plugin.html).

OSSRH credentials also have to be provided via

* SONATYPE_USER
* SONATYPE_PASSWORD

environmental variables.

Please follow the [OSSRH guide](https://central.sonatype.org/pages/ossrh-guide.html) for the detailed steps on how to
get the credentials and claim the group name.

Finally, the publication can be started with `./gradlew publish` command

```shell
gpg --list-keys --keyid-format short
export SIGN_KEY_B64=$(gpg --armor --export-secret-keys B6083312 | base64)
export SIGN_KEY=$(gpg --armor --export-secret-keys B6083312)
export SIGN_KEY_ID=B6083312
export SIGN_KEY_PASSPHRASE=pass
export SONATYPE_USER=user
export SONATYPE_PASSWORD=password
```

```shell
./gradlew publish
```

```shell
./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
```

```kotlin
publishing {
    repositories {
        maven {
            name = "OSSRH"
            if (isRemoteRepositoryEnabled) {
                setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("SONATYPE_USER")
                    password = System.getenv("SONATYPE_PASSWORD")
                }
            } else {
                setUrl("$buildDir/repo")
            }
        }
    }
}
```
