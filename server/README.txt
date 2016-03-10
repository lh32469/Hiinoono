
To build and start the server:

$ mvn clean assembly:assembly -Djavax.xml.accessExternalSchema=all
$ java -jar target/Hs.jar

The server needs to be run as root and requires a running ZooKeeper cluster to connect to as defined in the config file as well as a Volume Group named 'hiinoono' on each server.

Here is a sample config file:

~$ cat /etc/hiinoono/config.props 

# This cluster node Id
NodeId=d78dc452-9021-4b68-aaae-33d5c7af7e31

# Key for encrypting data stored in ZooKeeper
AES_KEY=5A6BE0127FE74038919E0DA921D8EC78

# One, or more, CSV ZK hosts and ports.
zooKeepers=localhost:2181

# For log aggregation for LogViewer app.
redis.host=localhost
redis.port=6379
#redis.pass


