## Extra work

 - Add unit tests to prove that operations can be simultaneous.

 - Add integration tests that test the microservice under heavy workload.

 - Add environment-dependent configurations in a tool such as Spring Cloud Config to facilitate continuous integration later.

 - Use a real database, as we want robustness it would be SQL and we would implement the new database functionality in each sprint using Liquibase.

 - Add auditing, operations should be logged.

 - Add authorisation and securitisation of calls, as this is very sensitive data.

 - Add trace tracking and microservice monitoring. For example Graylog and Grafana.

 - Ensure that the microservice is scalable.

 - From the devOps point of view, implement continuous integration so that the microservice can be deployed in the desired environment and that it is automatic for the development and test environments (that builds and deployments are activated when merge in a branch). Also add code quality review (Sonar).

 - Add an alert system to report problems in production environments.