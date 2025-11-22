package com.example.controller;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
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
     * Query the Counter from a specific member using REST API
     * Gets endpoint information from collective controller MBeans and makes HTTP call to member
     */
    private JsonObject queryMemberCounter(MBeanServer mbs, String serverName, String hostName) throws Exception {
        JsonObjectBuilder resultBuilder = Json.createObjectBuilder();
        resultBuilder.add("serverName", serverName);
        resultBuilder.add("hostName", hostName != null ? hostName : "unknown");
        
        try {
            LOGGER.info("Getting endpoint information for server: " + serverName);
            
            // Get member endpoint information from ApplicationRoutingInfoMBean
            String memberUrl = getMemberRestEndpoint(mbs, serverName, hostName);
            
            if (memberUrl == null) {
                resultBuilder.add("status", "endpoint_not_found");
                resultBuilder.add("message", "Could not determine REST endpoint for member: " + serverName);
                return resultBuilder.build();
            }
            
            LOGGER.info("Member REST endpoint: " + memberUrl);
            
            // Make HTTP call to member's Counter REST endpoint
            String counterEndpoint = memberUrl + "/liberty-cluster-member-app/api/counter";
            LOGGER.info("Calling Counter endpoint: " + counterEndpoint);
            
            String jsonResponse = makeHttpGetRequest(counterEndpoint);
            
            if (jsonResponse == null) {
                resultBuilder.add("status", "connection_failed");
                resultBuilder.add("message", "Failed to connect to member REST endpoint: " + counterEndpoint);
                return resultBuilder.build();
            }
            
            // Parse the JSON response from member
            try (JsonReader reader = Json.createReader(new StringReader(jsonResponse))) {
                JsonObject counterData = reader.readObject();
                
                // Extract counter information
                resultBuilder.add("memberName", counterData.getString("memberName", serverName));
                resultBuilder.add("counter", counterData.getJsonNumber("counter").longValue());
                resultBuilder.add("totalRequests", counterData.getJsonNumber("totalRequests").longValue());
                resultBuilder.add("status", "success");
                resultBuilder.add("endpoint", counterEndpoint);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error querying counter from member: " + serverName, e);
            resultBuilder.add("status", "error");
            resultBuilder.add("message", e.getMessage());
            resultBuilder.add("errorType", e.getClass().getSimpleName());
        }
        
        resultBuilder.add("timestamp", System.currentTimeMillis());
        return resultBuilder.build();
    }
    
    /**
     * Get the REST endpoint URL for a member server by querying collective controller MBeans
     */
    private String getMemberRestEndpoint(MBeanServer mbs, String serverName, String hostName) {
        try {
            // Query for ApplicationRoutingInfoMBean for the member
            String pattern = String.format("WebSphere:feature=collectiveMember,type=ApplicationRoutingInfoMBean,name=*");
            ObjectName query = new ObjectName(pattern);
            Set<ObjectName> routingMBeans = mbs.queryNames(query, null);
            
            LOGGER.info("Found " + routingMBeans.size() + " ApplicationRoutingInfoMBeans");
            
            // Try to get endpoint from EndpointRoutingInfo MBean
            ObjectName endpointMBean = new ObjectName("WebSphere:feature=collectiveMember,type=EndpointRoutingInfo,name=EndpointRoutingInfo");
            
            if (mbs.isRegistered(endpointMBean)) {
                try {
                    // Get HTTPS port - try different attribute names
                    Object httpsPort = null;
                    try {
                        httpsPort = mbs.getAttribute(endpointMBean, "HttpsPort");
                    } catch (Exception e) {
                        LOGGER.fine("HttpsPort attribute not found, trying alternatives");
                    }
                    
                    if (httpsPort == null) {
                        try {
                            httpsPort = mbs.getAttribute(endpointMBean, "DefaultHttpsPort");
                        } catch (Exception e) {
                            LOGGER.fine("DefaultHttpsPort attribute not found");
                        }
                    }
                    
                    if (httpsPort != null) {
                        String url = "https://" + hostName + ":" + httpsPort;
                        LOGGER.info("Constructed member URL from EndpointRoutingInfo: " + url);
                        return url;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Error getting endpoint from EndpointRoutingInfo", e);
                }
            }
            
            // Fallback: use default HTTPS port
            String defaultUrl = "https://" + hostName + ":9443";
            LOGGER.info("Using default HTTPS port, constructed URL: " + defaultUrl);
            return defaultUrl;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting member REST endpoint", e);
            return null;
        }
    }
    
    /**
     * Make HTTP GET request to the specified URL
     * Handles both HTTP and HTTPS with SSL trust
     */
    private String makeHttpGetRequest(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            
            // Setup SSL trust for HTTPS
            if (urlString.startsWith("https://")) {
                setupSSLTrust();
            }
            
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json");
            
            int responseCode = conn.getResponseCode();
            LOGGER.info("HTTP Response Code: " + responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                return response.toString();
            } else {
                LOGGER.warning("HTTP request failed with response code: " + responseCode);
                return null;
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error making HTTP request to: " + urlString, e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    /**
     * Setup SSL trust to accept all certificates (for development/testing)
     * In production, use proper certificate validation
     */
    private void setupSSLTrust() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error setting up SSL trust", e);
        }
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
     * Diagnostic endpoint to list all MBeans available on the controller
     * This helps debug MBean discovery issues
     */
    @GET
    @Path("/debug/mbeans")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllMBeans(@QueryParam("filter") String filter) {
        LOGGER.info("Listing all MBeans" + (filter != null ? " with filter: " + filter : ""));
        
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            String pattern = filter != null && !filter.trim().isEmpty() ? filter : "*:*";
            ObjectName query = new ObjectName(pattern);
            Set<ObjectName> allMBeans = mbs.queryNames(query, null);
            
            LOGGER.info("Found " + allMBeans.size() + " MBeans with pattern: " + pattern);
            
            JsonArrayBuilder mbeansArray = Json.createArrayBuilder();
            for (ObjectName mbean : allMBeans) {
                mbeansArray.add(mbean.toString());
            }
            
            JsonObject response = Json.createObjectBuilder()
                .add("pattern", pattern)
                .add("count", allMBeans.size())
                .add("mbeans", mbeansArray)
                .add("timestamp", System.currentTimeMillis())
                .build();
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to list MBeans", e);
            return createErrorResponse(
                "Failed to list MBeans: " + e.getMessage(),
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
