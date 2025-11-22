package com.example.controller;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API Resource for querying Counter MBean from cluster members
 * This resource runs on the Liberty Collective Controller and queries
 * the Counter MBean deployed in liberty-cluster-member-app on member servers
 */
@Path("/counters")
public class MemberCounterResource {
    
    private static final Logger LOGGER = Logger.getLogger(MemberCounterResource.class.getName());
    private static final String COUNTER_MBEAN_TYPE = "com.example.liberty.member:type=Counter";
    
    /**
     * Get counter values from all cluster members
     * 
     * @return JSON response with counter data from all members
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMemberCounters() {
        LOGGER.info("Querying counter MBeans from all cluster members");
        
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            // Verify this is running on a collective controller
            if (!isCollectiveController(mbs)) {
                return createErrorResponse(
                    "This application must be deployed on a Liberty Collective Controller",
                    Response.Status.INTERNAL_SERVER_ERROR
                );
            }
            
            // Get all cluster members
            List<Map<String, Object>> members = getClusterMembers(mbs);
            
            if (members.isEmpty()) {
                return createErrorResponse(
                    "No cluster members found. Ensure members are joined to the collective.",
                    Response.Status.NOT_FOUND
                );
            }
            
            JsonArrayBuilder membersArray = Json.createArrayBuilder();
            int successCount = 0;
            int errorCount = 0;
            
            // Query counter MBean from each member
            for (Map<String, Object> member : members) {
                String serverName = (String) member.get("serverName");
                String hostName = (String) member.get("hostName");
                
                try {
                    JsonObject counterData = queryMemberCounter(mbs, serverName, hostName);
                    membersArray.add(counterData);
                    
                    if ("success".equals(counterData.getString("status", ""))) {
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
            
            JsonObject response = Json.createObjectBuilder()
                .add("totalMembers", members.size())
                .add("successCount", successCount)
                .add("errorCount", errorCount)
                .add("members", membersArray)
                .add("timestamp", System.currentTimeMillis())
                .build();
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to query member counters", e);
            return createErrorResponse(
                "Failed to query member counters: " + e.getMessage(),
                Response.Status.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Get counter value from a specific cluster member
     * 
     * @param serverName The name of the cluster member server
     * @return JSON response with counter data from the specified member
     */
    @GET
    @Path("/{serverName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMemberCounter(@PathParam("serverName") String serverName) {
        LOGGER.info("Querying counter MBean from member: " + serverName);
        
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            // Verify controller
            if (!isCollectiveController(mbs)) {
                return createErrorResponse(
                    "This application must be deployed on a Liberty Collective Controller",
                    Response.Status.INTERNAL_SERVER_ERROR
                );
            }
            
            // Find the specific member
            Map<String, Object> member = findMemberByName(mbs, serverName);
            
            if (member == null) {
                return createErrorResponse(
                    "Member not found: " + serverName,
                    Response.Status.NOT_FOUND
                );
            }
            
            String hostName = (String) member.get("hostName");
            JsonObject counterData = queryMemberCounter(mbs, serverName, hostName);
            
            return Response.ok(counterData).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to query counter from member: " + serverName, e);
            return createErrorResponse(
                "Failed to retrieve data from member: " + serverName + " - " + e.getMessage(),
                Response.Status.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * List all available cluster members
     * 
     * @return JSON response with list of cluster members
     */
    @GET
    @Path("/members")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listMembers() {
        LOGGER.info("Listing all cluster members");
        
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            List<Map<String, Object>> members = getClusterMembers(mbs);
            
            JsonArrayBuilder membersArray = Json.createArrayBuilder();
            for (Map<String, Object> member : members) {
                JsonObjectBuilder memberBuilder = Json.createObjectBuilder();
                memberBuilder.add("serverName", (String) member.get("serverName"));
                
                if (member.get("hostName") != null) {
                    memberBuilder.add("hostName", (String) member.get("hostName"));
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
                .add("memberCount", members.size())
                .add("members", membersArray)
                .add("timestamp", System.currentTimeMillis())
                .build();
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to list members", e);
            return createErrorResponse(
                "Failed to list members: " + e.getMessage(),
                Response.Status.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Check if this server is a collective controller
     */
    private boolean isCollectiveController(MBeanServer mbs) {
        try {
            ObjectName controllerMBean = new ObjectName(
                "WebSphere:feature=collectiveController,type=CollectiveRepository,name=CollectiveRepository"
            );
            return mbs.isRegistered(controllerMBean);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking for collective controller", e);
            return false;
        }
    }
    
    /**
     * Get all cluster members from the collective using ClusterManager MBean
     */
    private List<Map<String, Object>> getClusterMembers(MBeanServer mbs) throws Exception {
        List<Map<String, Object>> allMembers = new ArrayList<>();
        
        try {
            // Get all clusters first
            ObjectName clusterMgrMBean = new ObjectName("WebSphere:feature=collectiveController,type=ClusterManager,name=ClusterManager");
            Collection<String> clusterNames = (Collection<String>) invokeOperation(mbs, clusterMgrMBean, "listClusterNames", null, null);
            
            if (clusterNames == null || clusterNames.isEmpty()) {
                LOGGER.info("No clusters found in the collective");
                return allMembers;
            }
            
            LOGGER.info("Found " + clusterNames.size() + " clusters");
            
            // Get members from each cluster
            for (String clusterName : clusterNames) {
                try {
                    Collection<String> clusterMemberTuples = (Collection<String>) invokeOperation(mbs, clusterMgrMBean, "listMembers",
                        new Object[] {clusterName}, new String[] {String.class.getName()});
                    
                    if (clusterMemberTuples != null) {
                        for (Iterator<String> iter = clusterMemberTuples.iterator(); iter.hasNext();) {
                            String clusterMemberTuple = iter.next();
                            LibertyClusterMember cm = LibertyClusterMember.parseClusterMemberTuple(clusterMemberTuple);
                            
                            if (cm != null) {
                                Map<String, Object> memberInfo = new HashMap<>();
                                memberInfo.put("serverName", cm.getServerName());
                                memberInfo.put("clusterName", clusterName);
                                memberInfo.put("hostName", cm.getHostName());
                                memberInfo.put("userDir", cm.getUserDir());
                                
                                allMembers.add(memberInfo);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error getting members for cluster: " + clusterName, e);
                }
            }
            
            LOGGER.info("Found total " + allMembers.size() + " members across all clusters");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting cluster members", e);
            throw e;
        }
        
        return allMembers;
    }
    
    /**
     * Find a specific member by server name
     */
    private Map<String, Object> findMemberByName(MBeanServer mbs, String serverName) throws Exception {
        List<Map<String, Object>> members = getClusterMembers(mbs);
        
        for (Map<String, Object> member : members) {
            if (serverName.equals(member.get("serverName"))) {
                return member;
            }
        }
        
        return null;
    }
    
    /**
     * Query the Counter MBean from a specific member through the collective controller
     */
    private JsonObject queryMemberCounter(MBeanServer mbs, String serverName, String hostName) throws Exception {
        JsonObjectBuilder resultBuilder = Json.createObjectBuilder();
        resultBuilder.add("serverName", serverName);
        resultBuilder.add("hostName", hostName != null ? hostName : "unknown");
        
        try {
            // The Counter MBean is accessed through the collective controller's routing
            // Pattern: WebSphere:feature=collectiveController,type=Runtime,name=<host>,host=<host>,server=<server>,<original-mbean-properties>
            
            // Try to find the Counter MBean for this member
            String mbeanPattern = String.format(
                "WebSphere:feature=collectiveController,type=Runtime,name=%s,host=%s,server=%s,type=Counter,*",
                hostName, hostName, serverName
            );
            
            ObjectName counterQuery = new ObjectName(mbeanPattern);
            Set<ObjectName> counterMBeans = mbs.queryNames(counterQuery, null);
            
            LOGGER.info("Searching for Counter MBean with pattern: " + mbeanPattern);
            LOGGER.info("Found " + counterMBeans.size() + " Counter MBeans for server: " + serverName);
            
            if (counterMBeans.isEmpty()) {
                resultBuilder.add("status", "mbean_not_found");
                resultBuilder.add("message", "Counter MBean not found on member. Ensure liberty-cluster-member-app is deployed and running.");
                resultBuilder.add("searchPattern", mbeanPattern);
                return resultBuilder.build();
            }
            
            // Get the first counter MBean found
            ObjectName counterMBean = counterMBeans.iterator().next();
            LOGGER.info("Found Counter MBean: " + counterMBean.toString());
            
            // Get MBean attributes
            Long counter = (Long) mbs.getAttribute(counterMBean, "Counter");
            Long totalRequests = (Long) mbs.getAttribute(counterMBean, "TotalRequests");
            String memberName = (String) mbs.getAttribute(counterMBean, "MemberName");
            
            resultBuilder.add("memberName", memberName);
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
    
    /**
     * List all clusters using ClusterManager MBean
     */
    @GET
    @Path("/clusters")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listClusters() {
        LOGGER.info("Listing all clusters");
        
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            if (!isCollectiveController(mbs)) {
                return createErrorResponse(
                    "This application must be deployed on a Liberty Collective Controller",
                    Response.Status.INTERNAL_SERVER_ERROR
                );
            }
            
            ObjectName clusterMgrMBean = new ObjectName("WebSphere:feature=collectiveController,type=ClusterManager,name=ClusterManager");
            Collection<String> clusterNames = (Collection<String>) invokeOperation(mbs, clusterMgrMBean, "listClusterNames", null, null);
            
            JsonArrayBuilder clustersArray = Json.createArrayBuilder();
            if (clusterNames != null) {
                for (String clusterName : clusterNames) {
                    clustersArray.add(clusterName);
                }
            }
            
            JsonObject response = Json.createObjectBuilder()
                .add("clusterCount", clusterNames != null ? clusterNames.size() : 0)
                .add("clusters", clustersArray)
                .add("timestamp", System.currentTimeMillis())
                .build();
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to list clusters", e);
            return createErrorResponse(
                "Failed to list clusters: " + e.getMessage(),
                Response.Status.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Get cluster members by cluster name using ClusterManager MBean
     */
    @GET
    @Path("/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClusterMembers(@QueryParam("clusterName") String clusterName) {
        LOGGER.info("Getting members for cluster: " + clusterName);
        
        try {
            if (clusterName == null || clusterName.trim().isEmpty()) {
                return createErrorResponse(
                    "Missing required parameter: clusterName",
                    Response.Status.BAD_REQUEST
                );
            }
            
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            if (!isCollectiveController(mbs)) {
                return createErrorResponse(
                    "This application must be deployed on a Liberty Collective Controller",
                    Response.Status.INTERNAL_SERVER_ERROR
                );
            }
            
            // Use ClusterManager MBean to get cluster members
            ObjectName clusterMgrMBean = new ObjectName("WebSphere:feature=collectiveController,type=ClusterManager,name=ClusterManager");
            Collection<String> clusterMemberTuples = (Collection<String>) invokeOperation(mbs, clusterMgrMBean, "listMembers",
                new Object[] {clusterName}, new String[] {String.class.getName()});
            
            if (clusterMemberTuples == null || clusterMemberTuples.isEmpty()) {
                JsonObject response = Json.createObjectBuilder()
                    .add("clusterName", clusterName)
                    .add("memberCount", 0)
                    .add("members", Json.createArrayBuilder())
                    .add("message", "No members found for cluster: " + clusterName)
                    .add("timestamp", System.currentTimeMillis())
                    .build();
                return Response.status(Response.Status.NOT_FOUND).entity(response).build();
            }
            
            LOGGER.info("Found " + clusterMemberTuples.size() + " members in cluster: " + clusterName);
            
            JsonArrayBuilder membersArray = Json.createArrayBuilder();
            
            // Parse each member tuple
            for (Iterator<String> iter = clusterMemberTuples.iterator(); iter.hasNext();) {
                String clusterMemberTuple = iter.next();
                LibertyClusterMember cm = LibertyClusterMember.parseClusterMemberTuple(clusterMemberTuple);
                
                if (cm != null) {
                    JsonObjectBuilder memberBuilder = Json.createObjectBuilder();
                    
                    // Add non-null values only
                    if (cm.getServerName() != null) {
                        memberBuilder.add("serverName", cm.getServerName());
                    }
                    memberBuilder.add("clusterName", clusterName);
                    
                    if (cm.getHostName() != null) {
                        memberBuilder.add("hostName", cm.getHostName());
                    }
                    if (cm.getUserDir() != null) {
                        memberBuilder.add("userDir", cm.getUserDir());
                    }
                    
                    membersArray.add(memberBuilder.build());
                }
            }
            
            JsonObject response = Json.createObjectBuilder()
                .add("clusterName", clusterName)
                .add("memberCount", clusterMemberTuples.size())
                .add("members", membersArray)
                .add("timestamp", System.currentTimeMillis())
                .build();
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get cluster members", e);
            return createErrorResponse(
                "Failed to get cluster members: " + e.getMessage(),
                Response.Status.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Helper method to invoke MBean operations
     */
    private Object invokeOperation(MBeanServer mbs, ObjectName objName, String operationName, Object[] params, String[] signature) throws Exception {
        return mbs.invoke(objName, operationName, params, signature);
    }
    
    /**
     * Create an error response
     */
    private Response createErrorResponse(String message, Response.Status status) {
        JsonObject error = Json.createObjectBuilder()
            .add("error", message)
            .add("timestamp", System.currentTimeMillis())
            .build();
        return Response.status(status).entity(error).build();
    }
}

// Made with Bob
