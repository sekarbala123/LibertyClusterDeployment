package com.example.liberty.cluster;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

@Path("/cluster")
public class ClusterInfoResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClusterInfo() {
        try {
            // Return server information using standard JMX
            Map<String, Object> serverInfo = new HashMap<>();
            serverInfo.put("serverName", "Open Liberty Server");
            serverInfo.put("javaVersion", System.getProperty("java.version"));
            serverInfo.put("javaVendor", System.getProperty("java.vendor"));
            serverInfo.put("osName", System.getProperty("os.name"));
            serverInfo.put("osVersion", System.getProperty("os.version"));
            serverInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
            serverInfo.put("freeMemory", Runtime.getRuntime().freeMemory());
            serverInfo.put("totalMemory", Runtime.getRuntime().totalMemory());
            serverInfo.put("maxMemory", Runtime.getRuntime().maxMemory());
            
            // Add MBean server info
            serverInfo.put("mbeanCount", ManagementFactory.getPlatformMBeanServer().getMBeanCount());
            serverInfo.put("message", "Open Liberty server running successfully");
            
            return Response.ok(serverInfo).build();
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getName());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(error)
                           .build();
        }
    }
}
