package com.example.liberty.cluster;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/members")
public class CollectiveMembersResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCollectiveMembers() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            List<Map<String, Object>> members = new ArrayList<>();
            
            // The MBean for the collective controller itself
            ObjectName controllerMBeanName = new ObjectName("WebSphere:feature=collectiveController,type=CollectiveRepository,name=CollectiveRepository");

            if (!mbs.isRegistered(controllerMBeanName)) {
                System.err.println("CRITICAL: Collective Controller MBean is not registered. Ensure this app is on the controller server.");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "This endpoint must be deployed on a Liberty Collective Controller.");
                errorResponse.put("mbean_not_found", controllerMBeanName.toString());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
            }

            System.out.println("SUCCESS: Collective Controller MBean is registered. Querying for members...");

            // Query for all MBeans that represent collective members
            ObjectName memberQuery = new ObjectName("WebSphere:feature=collectiveController,type=CollectiveMember,*");
            Set<ObjectName> memberMBeans = mbs.queryNames(memberQuery, null);

            System.out.println("Found " + memberMBeans.size() + " collective member MBeans.");

            for (ObjectName mbeanName : memberMBeans) {
                Map<String, Object> memberDetails = new HashMap<>();
                try {
                    MBeanInfo info = mbs.getMBeanInfo(mbeanName);
                    AttributeList attributes = mbs.getAttributes(mbeanName, getAttributeNames(info));
                    
                    for (Attribute attr : attributes.asList()) {
                        memberDetails.put(attr.getName(), attr.getValue());
                    }
                    members.add(memberDetails);
                } catch (Exception e) {
                    System.err.println("Error processing member MBean " + mbeanName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("controllerStatus", "ACTIVE");
            response.put("memberCount", members.size());
            response.put("members", members);
            response.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to query collective members due to an unexpected exception.");
            error.put("message", e.getMessage());
            error.put("type", e.getClass().getName());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    private String[] getAttributeNames(MBeanInfo info) {
        MBeanAttributeInfo[] attributes = info.getAttributes();
        String[] names = new String[attributes.length];
        for (int i = 0; i < attributes.length; i++) {
            names[i] = attributes[i].getName();
        }
        return names;
    }
    
    @GET
    @Path("detailed")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDetailedMemberInfo() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            List<Map<String, Object>> allMembers = new ArrayList<>();
            
            // Query all collective-related MBeans
            String[] patterns = {
                "WebSphere:feature=collectiveController,type=Host,*",
                "WebSphere:feature=collectiveController,type=Server,*",
                "WebSphere:feature=collectiveController,type=Runtime,*",
                "WebSphere:feature=collectiveController,type=CollectiveRepository,*"
            };
            
            for (String pattern : patterns) {
                ObjectName objectPattern = new ObjectName(pattern);
                Set<ObjectName> mbeans = mbs.queryNames(objectPattern, null);
                
                for (ObjectName mbean : mbeans) {
                    Map<String, Object> memberDetails = new HashMap<>();
                    memberDetails.put("objectName", mbean.toString());
                    memberDetails.put("type", mbean.getKeyProperty("type"));
                    memberDetails.put("properties", mbean.getKeyPropertyList());
                    
                    // Get all available attributes
                    try {
                        MBeanInfo mbeanInfo = mbs.getMBeanInfo(mbean);
                        MBeanAttributeInfo[] attributes = mbeanInfo.getAttributes();
                        Map<String, Object> attributeValues = new HashMap<>();
                        
                        for (MBeanAttributeInfo attr : attributes) {
                            if (attr.isReadable()) {
                                try {
                                    Object value = mbs.getAttribute(mbean, attr.getName());
                                    attributeValues.put(attr.getName(), value != null ? value.toString() : "null");
                                } catch (Exception e) {
                                    attributeValues.put(attr.getName(), "Error: " + e.getMessage());
                                }
                            }
                        }
                        
                        memberDetails.put("attributes", attributeValues);
                    } catch (Exception e) {
                        memberDetails.put("attributeError", e.getMessage());
                    }
                    
                    allMembers.add(memberDetails);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalMembers", allMembers.size());
            response.put("members", allMembers);
            response.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to query detailed member information");
            error.put("message", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
    
    private Map<String, Object> extractMemberDetails(MBeanServer mbs, ObjectName mbean) {
        Map<String, Object> details = new HashMap<>();
        
        try {
            details.put("objectName", mbean.toString());
            details.put("mbeanType", mbean.getKeyProperty("type"));
            details.put("name", mbean.getKeyProperty("name"));
            
            // Try to get common attributes
            String[] commonAttributes = {
                "State", "Host", "HostName", "HttpPort", "HttpsPort",
                "RpcSslPort", "UserDir", "ServerName", "WlpInstallDir"
            };
            
            for (String attrName : commonAttributes) {
                try {
                    Object value = mbs.getAttribute(mbean, attrName);
                    if (value != null) {
                        details.put(attrName.toLowerCase(), value.toString());
                    }
                } catch (Exception e) {
                    // Attribute not available, skip
                }
            }
            
        } catch (Exception e) {
            details.put("error", e.getMessage());
        }
        
        return details;
    }
    
    private Map<String, Object> extractHostDetails(MBeanServer mbs, ObjectName mbean) {
        Map<String, Object> details = new HashMap<>();
        
        try {
            details.put("objectName", mbean.toString());
            details.put("mbeanType", "Host");
            
            String hostName = mbean.getKeyProperty("name");
            if (hostName != null) {
                details.put("hostName", hostName);
            }
            
            // Get host-specific attributes
            try {
                Object servers = mbs.getAttribute(mbean, "Servers");
                if (servers != null) {
                    details.put("servers", servers.toString());
                }
            } catch (Exception e) {
                // Attribute not available
            }
            
            try {
                Object runtimes = mbs.getAttribute(mbean, "Runtimes");
                if (runtimes != null) {
                    details.put("runtimes", runtimes.toString());
                }
            } catch (Exception e) {
                // Attribute not available
            }
            
        } catch (Exception e) {
            details.put("error", e.getMessage());
        }
        
        return details;
    }
    
    @GET
    @Path("/repo-operations")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRepositoryOperations() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName repoMBean = new ObjectName("WebSphere:feature=collectiveController,type=CollectiveRepository,name=CollectiveRepository");
            
            Map<String, Object> response = new HashMap<>();
            
            if (mbs.isRegistered(repoMBean)) {
                MBeanInfo info = mbs.getMBeanInfo(repoMBean);
                List<Map<String, Object>> operations = new ArrayList<>();
                
                for (MBeanAttributeInfo attr : info.getAttributes()) {
                    Map<String, Object> attrInfo = new HashMap<>();
                    attrInfo.put("name", attr.getName());
                    attrInfo.put("type", attr.getType());
                    attrInfo.put("description", attr.getDescription());
                    attrInfo.put("readable", attr.isReadable());
                    operations.add(attrInfo);
                }
                
                response.put("attributes", operations);
                
                List<Map<String, Object>> ops = new ArrayList<>();
                for (MBeanOperationInfo op : info.getOperations()) {
                    Map<String, Object> opInfo = new HashMap<>();
                    opInfo.put("name", op.getName());
                    opInfo.put("description", op.getDescription());
                    opInfo.put("returnType", op.getReturnType());
                    
                    List<Map<String, String>> params = new ArrayList<>();
                    for (MBeanParameterInfo param : op.getSignature()) {
                        Map<String, String> paramInfo = new HashMap<>();
                        paramInfo.put("name", param.getName());
                        paramInfo.put("type", param.getType());
                        params.add(paramInfo);
                    }
                    opInfo.put("parameters", params);
                    ops.add(opInfo);
                }
                
                response.put("operations", ops);
            } else {
                response.put("error", "CollectiveRepository MBean not registered");
            }
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to inspect CollectiveRepository MBean");
            error.put("message", e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
    
    @GET
    @Path("/mbeans")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMBeans() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            // Get all MBeans
            Set<ObjectName> allMBeans = mbs.queryNames(null, null);
            
            List<Map<String, Object>> mbeanList = new ArrayList<>();
            for (ObjectName mbean : allMBeans) {
                String mbeanName = mbean.toString();
                // Filter for WebSphere and collective-related MBeans
                if (mbeanName.startsWith("WebSphere:") || 
                    mbeanName.contains("collective") || 
                    mbeanName.contains("Collective")) {
                    
                    Map<String, Object> mbeanInfo = new HashMap<>();
                    mbeanInfo.put("objectName", mbeanName);
                    mbeanInfo.put("domain", mbean.getDomain());
                    mbeanInfo.put("properties", mbean.getKeyPropertyList());
                    mbeanList.add(mbeanInfo);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalMBeans", allMBeans.size());
            response.put("websphereAndCollectiveMBeans", mbeanList.size());
            response.put("mbeans", mbeanList);
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to query MBeans");
            error.put("message", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
    
    /**
     * List all available clusters in the collective
     */
    @GET
    @Path("/clusters")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listClusters() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            // Verify controller
            if (!isCollectiveController(mbs)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "This endpoint must be deployed on a Liberty Collective Controller");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
            }
            
            // Use ClusterManager MBean to get cluster names
            ObjectName clusterMgrMBean = new ObjectName("WebSphere:feature=collectiveController,type=ClusterManager,name=ClusterManager");
            Collection<String> clusterNames = (Collection<String>) invokeOperation(mbs, clusterMgrMBean, "listClusterNames", null, null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("clusterCount", clusterNames != null ? clusterNames.size() : 0);
            response.put("clusters", clusterNames != null ? new ArrayList<>(clusterNames) : new ArrayList<>());
            response.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to list clusters");
            error.put("message", e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
    
    /**
     * Get cluster members by cluster name with host IP/name, HTTPS port, and application MBean states
     */
    @GET
    @Path("/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClusterMembers(@QueryParam("clusterName") String clusterName) {
        try {
            // Validate input
            if (clusterName == null || clusterName.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing required parameter: clusterName");
                errorResponse.put("usage", "GET /members/cluster?clusterName=<cluster-name>");
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }
            
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            
            // Verify this is running on a collective controller
            if (!isCollectiveController(mbs)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "This endpoint must be deployed on a Liberty Collective Controller");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
            }
            
            System.out.println("Querying cluster members for cluster: " + clusterName);
            
            // Use ClusterManager MBean to get cluster members
            ObjectName clusterMgrMBean = new ObjectName("WebSphere:feature=collectiveController,type=ClusterManager,name=ClusterManager");
            Collection<String> memberNames = (Collection<String>) invokeOperation(mbs, clusterMgrMBean, "listMembers",
                new Object[] {clusterName}, new String[] {String.class.getName()});
            
            if (memberNames == null || memberNames.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("clusterName", clusterName);
                response.put("memberCount", 0);
                response.put("members", new ArrayList<>());
                response.put("message", "No members found for cluster: " + clusterName);
                response.put("timestamp", System.currentTimeMillis());
                return Response.status(Response.Status.NOT_FOUND).entity(response).build();
            }
            
            System.out.println("Found " + memberNames.size() + " members in cluster: " + clusterName);
            
            List<Map<String, Object>> clusterMembers = new ArrayList<>();
            
            // Get detailed information for each member
            for (String memberName : memberNames) {
                try {
                    // Query for the specific server MBean
                    ObjectName serverQuery = new ObjectName("WebSphere:feature=collectiveController,type=Server,name=" + memberName);
                    
                    if (mbs.isRegistered(serverQuery)) {
                        Map<String, Object> memberInfo = new HashMap<>();
                        memberInfo.put("serverName", memberName);
                        memberInfo.put("clusterName", clusterName);
                        
                        // Get host information
                        try {
                            Object hostAttr = mbs.getAttribute(serverQuery, "Host");
                            if (hostAttr != null) {
                                memberInfo.put("host", hostAttr.toString());
                            }
                        } catch (Exception e) {
                            System.err.println("Could not get Host attribute for " + memberName);
                        }
                        
                        // Get hostname
                        try {
                            Object hostNameAttr = mbs.getAttribute(serverQuery, "HostName");
                            if (hostNameAttr != null) {
                                memberInfo.put("hostName", hostNameAttr.toString());
                            }
                        } catch (Exception e) {
                            System.err.println("Could not get HostName attribute for " + memberName);
                        }
                        
                        // Get HTTPS port
                        try {
                            Object httpsPortAttr = mbs.getAttribute(serverQuery, "HttpsPort");
                            if (httpsPortAttr != null) {
                                memberInfo.put("httpsPort", httpsPortAttr.toString());
                            }
                        } catch (Exception e) {
                            System.err.println("Could not get HttpsPort attribute for " + memberName);
                        }
                        
                        // Get HTTP port
                        try {
                            Object httpPortAttr = mbs.getAttribute(serverQuery, "HttpPort");
                            if (httpPortAttr != null) {
                                memberInfo.put("httpPort", httpPortAttr.toString());
                            }
                        } catch (Exception e) {
                            // HTTP port might not be configured
                        }
                        
                        // Get server state
                        try {
                            Object stateAttr = mbs.getAttribute(serverQuery, "State");
                            if (stateAttr != null) {
                                memberInfo.put("state", stateAttr.toString());
                            }
                        } catch (Exception e) {
                            System.err.println("Could not get State attribute for " + memberName);
                        }
                        
                        // Get user directory
                        try {
                            Object userDirAttr = mbs.getAttribute(serverQuery, "UserDir");
                            if (userDirAttr != null) {
                                memberInfo.put("userDir", userDirAttr.toString());
                            }
                        } catch (Exception e) {
                            // Optional attribute
                        }
                        
                        // Get WLP install directory
                        try {
                            Object wlpInstallDirAttr = mbs.getAttribute(serverQuery, "WlpInstallDir");
                            if (wlpInstallDirAttr != null) {
                                memberInfo.put("wlpInstallDir", wlpInstallDirAttr.toString());
                            }
                        } catch (Exception e) {
                            // Optional attribute
                        }
                        
                        // Get application MBean states from this member server
                        String hostForQuery = memberInfo.get("hostName") != null ?
                            memberInfo.get("hostName").toString() :
                            (memberInfo.get("host") != null ? memberInfo.get("host").toString() : "localhost");
                        
                        List<Map<String, Object>> applications = getApplicationMBeansFromMember(mbs, memberName, hostForQuery);
                        if (!applications.isEmpty()) {
                            memberInfo.put("applications", applications);
                            memberInfo.put("applicationCount", applications.size());
                        } else {
                            memberInfo.put("applications", new ArrayList<>());
                            memberInfo.put("applicationCount", 0);
                        }
                        
                        clusterMembers.add(memberInfo);
                        System.out.println("Added cluster member: " + memberName + " with " + applications.size() + " applications");
                    } else {
                        System.err.println("Server MBean not found for member: " + memberName);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing member " + memberName + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("clusterName", clusterName);
            response.put("memberCount", clusterMembers.size());
            response.put("members", clusterMembers);
            response.put("timestamp", System.currentTimeMillis());
            
            if (clusterMembers.isEmpty()) {
                response.put("message", "No members found for cluster: " + clusterName);
                return Response.status(Response.Status.NOT_FOUND).entity(response).build();
            }
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to query cluster members");
            error.put("message", e.getMessage());
            error.put("type", e.getClass().getName());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
    
    /**
     * Helper method to query application MBean states from a member server through the controller.
     *
     * @param mbs MBeanServer instance
     * @param serverName Name of the member server
     * @param hostName Hostname of the member server
     * @return List of application MBean information
     */
    private List<Map<String, Object>> getApplicationMBeansFromMember(MBeanServer mbs, String serverName, String hostName) {
        List<Map<String, Object>> applications = new ArrayList<>();
        
        try {
            // Query for application MBeans on the specific member server
            // Pattern: WebSphere:feature=collectiveController,type=Runtime,name=<host>,host=<host>,server=<server>,*
            String mbeanPattern = String.format(
                "WebSphere:feature=collectiveController,type=Runtime,name=%s,host=%s,server=%s,*",
                hostName, hostName, serverName
            );
            
            ObjectName runtimeQuery = new ObjectName(mbeanPattern);
            Set<ObjectName> runtimeMBeans = mbs.queryNames(runtimeQuery, null);
            
            System.out.println("Found " + runtimeMBeans.size() + " runtime MBeans for server: " + serverName);
            
            // Also try alternative pattern for application MBeans
            String appPattern = String.format(
                "WebSphere:feature=collectiveController,type=Runtime,name=%s,*,j2eeType=Application,*",
                hostName
            );
            
            ObjectName appQuery = new ObjectName(appPattern);
            Set<ObjectName> appMBeans = mbs.queryNames(appQuery, null);
            
            System.out.println("Found " + appMBeans.size() + " application MBeans for server: " + serverName);
            
            // Process application MBeans
            for (ObjectName appMBean : appMBeans) {
                try {
                    Map<String, Object> appInfo = new HashMap<>();
                    
                    // Extract application name from MBean
                    String appName = appMBean.getKeyProperty("name");
                    if (appName != null) {
                        appInfo.put("applicationName", appName);
                    }
                    
                    // Get j2eeType
                    String j2eeType = appMBean.getKeyProperty("j2eeType");
                    if (j2eeType != null) {
                        appInfo.put("j2eeType", j2eeType);
                    }
                    
                    // Try to get application state
                    try {
                        Object state = mbs.getAttribute(appMBean, "state");
                        if (state != null) {
                            appInfo.put("state", state.toString());
                        }
                    } catch (Exception e) {
                        // State attribute might not be available
                    }
                    
                    // Try to get deployment descriptor
                    try {
                        Object deploymentDescriptor = mbs.getAttribute(appMBean, "deploymentDescriptor");
                        if (deploymentDescriptor != null) {
                            appInfo.put("deploymentDescriptor", deploymentDescriptor.toString());
                        }
                    } catch (Exception e) {
                        // Attribute might not be available
                    }
                    
                    // Get all available attributes
                    try {
                        MBeanInfo mbeanInfo = mbs.getMBeanInfo(appMBean);
                        MBeanAttributeInfo[] attributes = mbeanInfo.getAttributes();
                        Map<String, Object> allAttributes = new HashMap<>();
                        
                        for (MBeanAttributeInfo attr : attributes) {
                            if (attr.isReadable()) {
                                try {
                                    Object value = mbs.getAttribute(appMBean, attr.getName());
                                    if (value != null) {
                                        allAttributes.put(attr.getName(), value.toString());
                                    }
                                } catch (Exception e) {
                                    // Skip attributes that can't be read
                                }
                            }
                        }
                        
                        if (!allAttributes.isEmpty()) {
                            appInfo.put("attributes", allAttributes);
                        }
                    } catch (Exception e) {
                        System.err.println("Error reading MBean attributes: " + e.getMessage());
                    }
                    
                    appInfo.put("mbeanObjectName", appMBean.toString());
                    applications.add(appInfo);
                    
                } catch (Exception e) {
                    System.err.println("Error processing application MBean " + appMBean + ": " + e.getMessage());
                }
            }
            
            // If no application MBeans found, try to get general runtime information
            if (applications.isEmpty() && !runtimeMBeans.isEmpty()) {
                for (ObjectName runtimeMBean : runtimeMBeans) {
                    try {
                        Map<String, Object> runtimeInfo = new HashMap<>();
                        runtimeInfo.put("mbeanObjectName", runtimeMBean.toString());
                        runtimeInfo.put("type", runtimeMBean.getKeyProperty("type"));
                        
                        // Get available attributes
                        MBeanInfo mbeanInfo = mbs.getMBeanInfo(runtimeMBean);
                        MBeanAttributeInfo[] attributes = mbeanInfo.getAttributes();
                        Map<String, Object> attrValues = new HashMap<>();
                        
                        for (MBeanAttributeInfo attr : attributes) {
                            if (attr.isReadable()) {
                                try {
                                    Object value = mbs.getAttribute(runtimeMBean, attr.getName());
                                    if (value != null) {
                                        attrValues.put(attr.getName(), value.toString());
                                    }
                                } catch (Exception e) {
                                    // Skip
                                }
                            }
                        }
                        
                        if (!attrValues.isEmpty()) {
                            runtimeInfo.put("attributes", attrValues);
                            applications.add(runtimeInfo);
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing runtime MBean: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error querying application MBeans for server " + serverName + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return applications;
    }
    
    /**
     * Helper method to check if running on collective controller
     */
    private boolean isCollectiveController(MBeanServer mbs) {
        try {
            ObjectName controllerMBean = new ObjectName("WebSphere:feature=collectiveController,type=CollectiveRepository,name=CollectiveRepository");
            return mbs.isRegistered(controllerMBean);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Helper method to invoke MBean operations
     */
    private Object invokeOperation(MBeanServer mbs, ObjectName objName, String operationName, Object[] params, String[] signature) throws Exception {
        return mbs.invoke(objName, operationName, params, signature);
    }
}

// Made with Bob
