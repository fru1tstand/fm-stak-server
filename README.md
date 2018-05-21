# Stak Server
[![codecov](https://codecov.io/gh/fru1tstand/fm-stak-server/branch/master/graph/badge.svg)](https://codecov.io/gh/fru1tstand/fm-stak-server)  

Stak is a task organizer split into two components: client and server. While the end result is the
same; for scalability, we want to allow the option of self-hosting for multi-user systems, and
portable mode for individual offline use. The server side is a simple REST api that handles all the
data. The client side deals purely with graphical rendering and controls for Stak. It will forward
all data requests to the server.

###### Dependencies
+ **Dagger** dependency injection
+ **ktor** kotlin web server

## Development
Please see the [dev/README.md](https://github.com/fru1tstand/fm-stak/blob/master/dev/README.md) in
the stak desktop client repo.
