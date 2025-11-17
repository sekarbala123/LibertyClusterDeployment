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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
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
}

// Made with Bob
