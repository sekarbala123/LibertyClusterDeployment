package com.example.liberty.cluster;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.Set;

@Path("/cluster")
public class ClusterInfoResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClusterInfo() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName clusterManagerObjectName = new ObjectName("WebSphere:feature=collectiveController,type=ClusterManager,name=ClusterManager");

            if (mbs.isRegistered(clusterManagerObjectName)) {
                // This code will execute if the application is running on the collective controller
                Set<String> clusters = (Set<String>) mbs.invoke(clusterManagerObjectName, "getClusters", null, null);
                return Response.ok(clusters).build();
            } else {
                // If not on the controller, we need to connect to the controller's MBean server.
                // This is a more complex scenario that requires JMX connection details.
                // For now, we'll return a message indicating that this is not the controller.
                return Response.ok("{\"message\": \"This server is not a collective controller.\"}").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("{\"error\": \"" + e.getMessage() + "\"}")
                           .build();
        }
    }
}
