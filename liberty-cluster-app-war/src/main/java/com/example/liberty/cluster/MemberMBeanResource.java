package com.example.liberty.cluster;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API resource for fetching counter MBean values from cluster members.
 * This resource dynamically discovers cluster members via the collective controller
 * and queries their counter MBeans.
 */
@Path("/mbeans")
public class MemberMBeanResource {
    
    private static final Logger LOGGER = Logger.getLogger(MemberMBeanResource.class.getName());
    private static final String COUNTER_MBEAN_NAME = "com.example.liberty.member:type=Counter";
    
    /**
     * Get counter MBean values from all members in a cluster
     * @param clusterName The name of the cluster (optional, if not provided, queries all members)
     */
    @GET
    @Path("/counters")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMemberCounters(@QueryParam("clusterName") String clusterName) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            // Verify this is running on a collective controller
            ObjectName controllerMBeanName = new ObjectName(
                "WebSphere:feature=collectiveController,type=CollectiveRepository,name=CollectiveRepository");
            
            if (!mbs.isRegistered(controllerMBeanName)) {
                JsonObject error = Json.createObjectBuilder()
                    .add("error", "This endpoint must be deployed on a Liberty Collective Controller")
                    .add("mbean_not_found", controllerMBeanName.toString())
                    .build();
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
            }
            
            LOGGER.info("Querying counter MBeans from cluster members" + 
                       (clusterName != null ? " in cluster: " + clusterName : ""));
            
            // Get all cluster members
            List<Map<String, Object>> members = getClusterMembers(mbs, clusterName);
            
            JsonArrayBuilder membersArray = Json.createArrayBuilder();
            int successCount = 0;
            int errorCount = 0;
            
            for (Map<String, Object> member : members) {
                String serverName = (String) member.get("serverName");
                String hostName = (String) member.get("hostName");
                String httpsPort = (String) member.get("httpsPort");
                
                try {
                    JsonObject counterData = queryMemberCounterMBean(
                        mbs, serverName, hostName, httpsPort);
                    membersArray.add(counterData);
                    
                    if (counterData.containsKey("counter")) {
                        successCount++;
                    } else {
                        errorCount++;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to query counter from member: " + serverName, e);
                    JsonObject errorData = Json.createObjectBuilder()
                        .add("serverName", serverName)
                        .add("hostName", hostName != null ? hostName : "unknown")
                        .add("status", "error")
                        .add("message", e.getMessage())
                        .build();
                    membersArray.add(errorData);
                    errorCount++;
                }
            }
            
            JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                .add("clusterName", clusterName != null ? clusterName : "all")
                .add("totalMembers", members.size())
                .add("successCount", successCount)
                .add("errorCount", errorCount)
                .add("members", membersArray)
                .add("timestamp", System.currentTimeMillis());
            
            return Response.ok(responseBuilder.build()).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to query member counters", e);
            JsonObject error = Json.createObjectBuilder()
                .add("error", "Failed to query member counters")
                .add("message", e.getMessage())
                .add("type", e.getClass().getName())
                .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
    
    /**
     * Get counter MBean value from a specific member
     */
    @GET
    @Path("/counters/{serverName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMemberCounter(@PathParam("serverName") String serverName) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            // Verify controller
            ObjectName controllerMBeanName = new ObjectName(
                "WebSphere:feature=collectiveController,type=CollectiveRepository,name=CollectiveRepository");
            
            if (!mbs.isRegistered(controllerMBeanName)) {
                JsonObject error = Json.createObjectBuilder()
                    .add("error", "This endpoint must be deployed on a Liberty Collective Controller")
                    .build();
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
            }
            
            // Find the specific member
            Map<String, Object> member = findMemberByName(mbs, serverName);
            
            if (member == null) {
                JsonObject error = Json.createObjectBuilder()
                    .add("error", "Member not found: " + serverName)
                    .build();
                return Response.status(Response.Status.NOT_FOUND).entity(error).build();
            }
            
            String hostName = (String) member.get("hostName");
            String httpsPort = (String) member.get("httpsPort");
            
            JsonObject counterData = queryMemberCounterMBean(mbs, serverName, hostName, httpsPort);
            return Response.ok(counterData).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to query counter from member: " + serverName, e);
            JsonObject error = Json.createObjectBuilder()
                .add("error", "Failed to retrieve data from member: " + serverName)
                .add("message", e.getMessage())
                .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
    
    /**
     * Get list of available cluster members
     */
    @GET
    @Path("/members")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvailableMembers(@QueryParam("clusterName") String clusterName) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            List<Map<String, Object>> members = getClusterMembers(mbs, clusterName);
            
            JsonArrayBuilder membersArray = Json.createArrayBuilder();
            for (Map<String, Object> member : members) {
                JsonObjectBuilder memberBuilder = Json.createObjectBuilder();
                memberBuilder.add("serverName", (String) member.get("serverName"));
                if (member.get("hostName") != null) {
                    memberBuilder.add("hostName", (String) member.get("hostName"));
                }
                if (member.get("httpsPort") != null) {
                    memberBuilder.add("httpsPort", (String) member.get("httpsPort"));
                }
                if (member.get("state") != null) {
                    memberBuilder.add("state", (String) member.get("state"));
                }
                if (member.get("clusterName") != null) {
                    memberBuilder.add("clusterName", (String) member.get("clusterName"));
                }
                membersArray.add(memberBuilder.build());
            }
            
            JsonObject response = Json.createObjectBuilder()
                .add("clusterName", clusterName != null ? clusterName : "all")
                .add("memberCount", members.size())
                .add("members", membersArray)
                .add("timestamp", System.currentTimeMillis())
                .build();
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get available members", e);
            JsonObject error = Json.createObjectBuilder()
                .add("error", "Failed to get available members")
                .add("message", e.getMessage())
                .build();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
    
    /**
     * Helper method to get cluster members from the collective controller
     */
    private List<Map<String, Object>> getClusterMembers(MBeanServer mbs, String clusterName) throws Exception {
        List<Map<String, Object>> members = new ArrayList<>();
        
        // Query for all server MBeans in the collective
        ObjectName serverQuery = new ObjectName("WebSphere:feature=collectiveController,type=Server,*");
        Set<ObjectName> serverMBeans = mbs.queryNames(serverQuery, null);
        
        LOGGER.info("Found " + serverMBeans.size() + " server MBeans in the collective");
        
        for (ObjectName serverMBean : serverMBeans) {
            try {
                // If clusterName is specified, filter by cluster
                if (clusterName != null && !clusterName.trim().isEmpty()) {
                    Object clusterAttr = null;
                    try {
                        clusterAttr = mbs.getAttribute(serverMBean, "Cluster");
                    } catch (Exception e) {
                        // Server might not have cluster attribute
                        continue;
                    }
                    
                    if (clusterAttr == null || !clusterName.equals(clusterAttr.toString())) {
                        continue;
                    }
                }
                
                Map<String, Object> memberInfo = new HashMap<>();
                
                // Extract server details
                String serverName = serverMBean.getKeyProperty("name");
                memberInfo.put("serverName", serverName);
                
                // Get cluster name if available
                try {
                    Object clusterAttr = mbs.getAttribute(serverMBean, "Cluster");
                    if (clusterAttr != null) {
                        memberInfo.put("clusterName", clusterAttr.toString());
                    }
                } catch (Exception e) {
                    // No cluster attribute
                }
                
                // Get host information
                try {
                    Object hostNameAttr = mbs.getAttribute(serverMBean, "HostName");
                    if (hostNameAttr != null) {
                        memberInfo.put("hostName", hostNameAttr.toString());
                    }
                } catch (Exception e) {
                    LOGGER.fine("Could not get HostName for " + serverName);
                }
                
                // Get HTTPS port
                try {
                    Object httpsPortAttr = mbs.getAttribute(serverMBean, "HttpsPort");
                    if (httpsPortAttr != null) {
                        memberInfo.put("httpsPort", httpsPortAttr.toString());
                    }
                } catch (Exception e) {
                    LOGGER.fine("Could not get HttpsPort for " + serverName);
                }
                
                // Get server state
                try {
                    Object stateAttr = mbs.getAttribute(serverMBean, "State");
                    if (stateAttr != null) {
                        memberInfo.put("state", stateAttr.toString());
                    }
                } catch (Exception e) {
                    LOGGER.fine("Could not get State for " + serverName);
                }
                
                members.add(memberInfo);
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error processing server MBean " + serverMBean, e);
            }
        }
        
        return members;
    }
    
    /**
     * Helper method to find a specific member by server name
     */
    private Map<String, Object> findMemberByName(MBeanServer mbs, String serverName) throws Exception {
        List<Map<String, Object>> members = getClusterMembers(mbs, null);
        
        for (Map<String, Object> member : members) {
            if (serverName.equals(member.get("serverName"))) {
                return member;
            }
        }
        
        return null;
    }
    
    /**
     * Helper method to query counter MBean from a member via JMX through the collective controller
     */
    private JsonObject queryMemberCounterMBean(MBeanServer mbs, String serverName, 
                                                String hostName, String httpsPort) throws Exception {
        
        JsonObjectBuilder resultBuilder = Json.createObjectBuilder();
        resultBuilder.add("serverName", serverName);
        resultBuilder.add("hostName", hostName != null ? hostName : "unknown");
        
        try {
            // Try to query the counter MBean through the collective controller's routing
            // The MBean name pattern for accessing member MBeans through the controller
            String mbeanPattern = String.format(
                "WebSphere:feature=collectiveController,type=Runtime,name=%s,host=%s,server=%s,*",
                hostName, hostName, serverName
            );
            
            ObjectName runtimeQuery = new ObjectName(mbeanPattern);
            Set<ObjectName> runtimeMBeans = mbs.queryNames(runtimeQuery, null);
            
            LOGGER.info("Found " + runtimeMBeans.size() + " runtime MBeans for server: " + serverName);
            
            // Try to find our counter MBean
            // The counter MBean should be accessible through the collective controller
            // Pattern: WebSphere:feature=collectiveController,type=Runtime,name=<host>,host=<host>,server=<server>,<original-mbean-properties>
            String counterMBeanPattern = String.format(
                "WebSphere:feature=collectiveController,type=Runtime,name=%s,host=%s,server=%s,type=Counter,*",
                hostName, hostName, serverName
            );
            
            ObjectName counterQuery = new ObjectName(counterMBeanPattern);
            Set<ObjectName> counterMBeans = mbs.queryNames(counterQuery, null);
            
            if (counterMBeans.isEmpty()) {
                // Try alternative pattern - direct member MBean access
                // This requires the member to have restConnector enabled
                resultBuilder.add("status", "mbean_not_found");
                resultBuilder.add("message", "Counter MBean not found on member. Ensure member-app is deployed and running.");
                resultBuilder.add("searchPattern", counterMBeanPattern);
                return resultBuilder.build();
            }
            
            // Get the first counter MBean found
            ObjectName counterMBean = counterMBeans.iterator().next();
            
            // Get MBean attributes
            Long counter = (Long) mbs.getAttribute(counterMBean, "Counter");
            Long totalRequests = (Long) mbs.getAttribute(counterMBean, "TotalRequests");
            String mbeanMemberName = (String) mbs.getAttribute(counterMBean, "MemberName");
            
            resultBuilder.add("mbeanMemberName", mbeanMemberName);
            resultBuilder.add("counter", counter);
            resultBuilder.add("totalRequests", totalRequests);
            resultBuilder.add("status", "success");
            resultBuilder.add("mbeanObjectName", counterMBean.toString());
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error querying counter MBean for " + serverName, e);
            resultBuilder.add("status", "error");
            resultBuilder.add("message", e.getMessage());
            resultBuilder.add("errorType", e.getClass().getSimpleName());
        }
        
        resultBuilder.add("timestamp", System.currentTimeMillis());
        return resultBuilder.build();
    }
}

// Made with Bob
