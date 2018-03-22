### Building single executable
Execute command to build single executable jar 'bankier.jar' 

`sbt assembly`

Execute command to run built jar

`java -jar target/scala-2.12/bankier.jar`

For building docker (keep in mind docker daemon must be running) execute

`sbt docker`

Then you can start docker container 

`docker run -p 8080:8080 com.kpbochenek/banking-service`

Then you can interact with application using swagger:

`http://localhost:8080/api`

### Testing
Execute command to run full suite of tests

`sbt test`
