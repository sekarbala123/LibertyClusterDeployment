package com.example.liberty.cluster;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
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

@Path("/member-details")
public class MemberDetailsResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMemberDetails() {
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
                    memberDetails.put("name", mbean.getKeyProperty("name"));
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
                                    if (value != null) {
                                        // Extract useful information
                                        String attrName = attr.getName();
                                        if (attrName.contains("Host") || attrName.contains("Port") || 
                                            attrName.contains("Ssl") || attrName.contains("Ip") ||
                                            attrName.contains("Address") || attrName.contains("Endpoint") ||
                                            attrName.contains("Server") || attrName.contains("Name")) {
                                            attributeValues.put(attrName, value.toString());
                                        }
                                    }
                                } catch (Exception e) {
                                    // Skip attributes that can't be read
                                }
                            }
                        }
                        
                        if (!attributeValues.isEmpty()) {
                            memberDetails.put("attributes", attributeValues);
                        }
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
            response.put("note", "Detailed member information from collective MBeans");
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to query detailed member information");
            error.put("message", e.getMessage());
            error.put("type", e.getClass().getName());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
}

// Made with Bob