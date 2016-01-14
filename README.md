# Demo Integration Server

This project is a vertx 3 web server that mimics a vendor's server.  It takes a clientId
and uses that to create a unique client to pass to the Lyric Registration API.  In a real life scenario,
the clientId would be used to look up the client data and use that to pass to the api.

    The Demo Integration Server is deployed at https://lyric-demo-server.herokuapp.com and the url to
    request an advance is https://lyric-demo-server.herokuapp.com/clients/:clientId/advance.

This project can be forked and used to make modifications.  You can then deploy it to your own heroku
instance.

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy)