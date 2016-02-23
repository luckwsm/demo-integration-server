# Demo Integration Server

This project is a vertx 3 web server that mimics a vendor's server.  It is set up to be flexible to
demonstrate the various scenarios that a vendor might use to integrate with the Lyric APIs. The primary integration
method is shown in the [Server Demo API](#server-demo-api). The Lyric API uses basic authentication and vendor API credentials
should never be exposed to client devices. To experiment with this demo now, you can access the [Server Demo](http://lyricfinancial.github.io/integration-guides/#/demo-server)
application. This application demonstrates the full primary integration method with Lyric's APIs. 
However, it is limited as the server uses randomized datasets.

We also created a [Client Demo API](#client-demo-api) to support a [demo app](http://lyricfinancial.github.io/integration-guides/#/demo)
that is less limited. This app is good for experimenting with the vATM "flow", but is not representative
of "real world" integrations.

Documentation for both of the demo applications can be found [here](https://github.com/LyricFinancial/integration-guides/tree/master/examples/client/angular/lyric-vendor-demo).

Use the [API Documentation](https://api.lyricfinancial.com/docs/vendor-api/) to see how to properly
use the Lyric registration API.


## Server Demo API
This API demonstrates how to use the Lyric API. The endpoint is **/clients/:clientId/advance_server**. 
It is invoked from the [Server Demo](http://lyricfinancial.github.io/integration-guides/#/demo-server) application
when pressing "Get Advance". The example code [here](https://github.com/LyricFinancial/demo-integration-server/blob/master/src/main/java/com/lyric/ServerDemoController.java)
shows how to POST registrations to the Lyric API. Currently earnings data can only be posted using multipart/form-data.
Eventually we will also have ways to embed this in a standard JSON call. However, multipart/form-data will be preferred
as it will allow for smaller payloads. You can toggle between JSON and Mutlipart Form when using the demo app. For further
documentation on this demo api, see [here](ServerDemoReadme.md). 

## Assignment API
This API demonstrates the how vendors will need to implement assignments.  Once an advance has been made in the Lyric
system, Lyric will make an API call to the vendor so that payments will be assigned to Lyric until the advance plus
processing fee has been paid back.  The body of the request will be signed with Lyrics private key (using
[JsonWebSignature](https://bitbucket.org/b_c/jose4j/wiki/JWS%20Examples)) and then encrypted (using
[JsonWebEncryption](https://bitbucket.org/b_c/jose4j/wiki/JWE%20Examples)) with the vendor's public key.
The vendor will be responsible to decrypting the body with their private key and then verifying the signature with Lyric's
public key.  The vendor will then be responsible to handling the assignment, then sending back a signed/encrypted response
to Lyric.  Lyric's receipt of this response will be logged and used as a proof that the vendor received and acknowledged
the assignment request.  More information can be found [here](https://lyric-demo-server.herokuapp.com/docs/assignments-api/).

## Token API
This API demonstrates how the vendor will need to generate a token that will authenticate with the Lyric system.  It can be
used when calling loadData on the LyricWidget to view the advanceStatus.  The token needs to be signed with the vendor's
private key and include the memberToken as the subject and the vendorId as a claim.  For demonstration purposes this API takes
the memberToken as a parameter, but it could also take the vendor's unique id for the user and use that to look up the memberToken.

## Try It

You are free to experiment with the Lyric Demo server, [deployed here](https://lyric-demo-server.herokuapp.com).
In fact, the Demo Apps are preconfigured to work with this server. They also allow you to change vendorID and API 
credentials under Advance Settings. However, if want to experiment with your own server, you can use this Heroku button
to deploy your own instance.

    The Demo Integration Server is deployed at https://lyric-demo-server.herokuapp.com and the url to
    request an advance is https://lyric-demo-server.herokuapp.com/clients/:clientId/advance_client and
    https://lyric-demo-server.herokuapp.com/clients/:clientId/advance_server.

This project can be forked and used to make modifications.  You can then deploy it to your own heroku
instance.  Make sure to set the vendorId, username and password environment variables in heroku once
it is deployed.

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy)

### Next Steps

[Configure a new Demo Integration Server](Welcome.md) to work with the demo apps.



Return to [Integration Guides](https://github.com/LyricFinancial/integration-guides#4-save-the-membertoken-that-gets-returned)