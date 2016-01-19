
To build the client, the server should be running on the
same host so that the WADL can be compiled.

To build and run the client: 

   $ mvn clean assembly:assembly

Set the service to applicable host(s) (defaults to localhost) and the 
user and password to default Hiinoono Admin User:

   $ export HIINOONO_SERVICE=http://host1.domain/api,http://host2.domain/api
   $ export HIINOONO_USER=/hiinoono/admin
   $ export HIINOONO_PASSWORD=Welcome1

Run the client and get the usage message:

   $ java -jar target/Hc.jar 

