# Demo Integration Server

This project is a vertx 3 web server that mimics a vendor's server.  It is set up to be flexible to
demonstrate the various scenarios that a vendor might use for implementation.  The main 2 use cases
are highlighted in the [Client Demo](http://lyricfinancial.github.io/integration-guides/#/demo) and
[Server Demo](http://lyricfinancial.github.io/integration-guides/#/demo).  View the documentation for
the Demos [here](https://github.com/LyricFinancial/integration-guides/tree/master/examples/client/angular/lyric-vendor-demo).
Use the [API Documentation](https://api.lyricfinancial.com/docs/vendor-api/) to see how to properly
use the Lyric registration API.

## Client Demo
This scenario mimics receiving the user data from the client ui and passing it straight through
to the Lyric registration API.  All the data and headers are copied onto the new request and then
the response is just proxied back to the client.  The data can be sent as json or multipart form data.
The endpoint is **/clients/:clientId/advance_client**.

## Server Demo
This scenario mimics taking the id from the url and using that to look the user up in the system.  This
demo creates a new unique user to pass to the API.  To trigger this flow, an options json object must
be sent as the body of the request.  The options tell the server how the data should be sent to the
Lyric registration API and how the Royalty Earnings should be retrieved and sent.  These options are
strictly for demo and testing purposes so that all of the different scenarios can be explored.  The
options json would look like:

    "options": {
        "contentType":"application/json"
        "royaltyEarningsContentType": "text/csv",
        "filename": "sample.csv"
    }

The endpoint is **/clients/:clientId/advance_server**.

## Try It

    The Demo Integration Server is deployed at https://lyric-demo-server.herokuapp.com and the url to
    request an advance is https://lyric-demo-server.herokuapp.com/clients/:clientId/advance_client and
    https://lyric-demo-server.herokuapp.com/clients/:clientId/advance_server.

This project can be forked and used to make modifications.  You can then deploy it to your own heroku
instance.

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy)