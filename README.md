# Demo Integration Server

This project is a vertx 3 web server that mimics a vendor's server.  It is set up to be flexible to
demonstrate the various scenarios that a vendor might use to integrate with the Lyric APIs.

## Development

1) Clone the repository locally

        git clone https://github.com/LyricFinancial/demo-integration-server.git

2) Open project in your favorite IDE (tested in IntelliJ)

3) Make appropriate coding changes

4) From command line at the root of the project, build the project

        docker-compose build

5) Once the project is built, start up the server

        fig up dev