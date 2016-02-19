mvn clean package
java	-Xmn16m \
	-Xms32m \
	-Xmx64m \
	-jar target/LogViewer*-war-exec.jar \
		-httpPort 7000
