# Liberty Cluster Deployment

This project demonstrates how to create a clustered IBM WebSphere Liberty environment and deploy a simple JAX-RS application that provides information about the cluster.

## Project Structure

*   `liberty-cluster-app-parent`: The parent Maven project.
*   `liberty-cluster-app-war`: A WAR module containing a simple JAX-RS RESTful service.
*   `liberty-cluster-app-ear`: An EAR module that packages the WAR for deployment.
*   `liberty-cluster-app-ear/src/main/liberty/config`: Contains the Liberty server configuration (`server.xml`).

## Requirements

*   Java 17
*   Maven 3.6+

## How to Build

To build the project, run the following command from the root directory:

```bash
mvn clean install
```

This will produce the EAR file in the `liberty-cluster-app-ear/target` directory.

## How to Run

This project is configured to run a Liberty collective controller.

To start the server, run the following command from the `liberty-cluster-app-ear` directory:

```bash
mvn liberty:run-server
```

The server will be created in `liberty-cluster-app-ear/target/liberty/wlp/usr/servers/controller`.

Once the server is started, you can access the application at:

[http://localhost:9080/liberty-cluster-app/api/cluster](http://localhost:9080/liberty-cluster-app/api/cluster)

You can access the Liberty Admin Center at:

[https://localhost:9443/adminCenter/](https://localhost:9443/adminCenter/)

## Cluster Setup

This project sets up a single Liberty server as a collective controller. To create a full cluster, you would need to:

1.  Create another Liberty server configuration for a collective member.
2.  Join the member server to the collective.
3.  Start both the controller and member servers.

The REST service in this application is designed to provide information about the cluster. When deployed on the controller, it can access the `ClusterManager` MBean to get information about the clusters and their members.

## Things to Consider

*   **Security:** The JMX connection to the collective controller should be secured with a username and password. The `server.xml` and client code would need to be updated to handle this.
*   **Cluster Member Configuration:** A separate `server.xml` would be needed for cluster members. This configuration would be similar to the controller's, but would use the `collectiveMember` feature instead of `collectiveController`.
*   **Production Environments:** For production, you would typically have a dedicated collective controller (or a replica set of controllers) and multiple collective members running on different machines.
*   **Application Logic:** The sample application currently only returns a list of cluster names. It could be extended to return more detailed information about each member, such as its host, port, and status.
