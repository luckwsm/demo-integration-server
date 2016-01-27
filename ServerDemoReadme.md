This api mimics taking the id from the url and using that to look the user up in the system.  This
demo creates a new unique user to pass to the API.  To trigger this flow, **an options json object must
be sent as the body of the request**.  The options tell the server how the data should be sent to the
Lyric registration API and how the Royalty Earnings should be retrieved and sent.  These options are
strictly for demo and testing purposes so that all of the different scenarios can be explored.  The
options json should look like:

    "options": {
        "contentType":"multipart/form-data"
        "royaltyEarningsContentType": "text/csv",
        "filename": "sample.csv"
    }

The endpoint is **/clients/:clientId/advance_server**.  There needs to be a file on the server with the
specified filename.  Right now the only file is sample.csv.
