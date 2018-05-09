#Stak Server
Stak is a task organizer split into two components: client and server. While the end result is the
same; for scalability, we want to allow the option of self-hosting for multi-user systems, and
portable mode for individual offline use. The server side is a simple REST api that handles all the
data. The client side deals purely with graphical rendering and controls for Stak. It will forward
all data requests to the server.

## Development
Please add the following to your .git/hooks/pre-push file which will check tests before pushing.
```bash
#!/bin/bash
CMD="./gradlew clean test"

# Check if we actually have commits to push
commits=`git log @{u}..`
if [ -z "$commits" ]; then
 exit 0
fi

$CMD
RESULT=$?
if [ $RESULT -ne 0 ]; then
 echo "failed $CMD"
 exit 1
fi

exit 0
```
