package com.example.liberty.member;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API resource for counter operations
 */
@Path("/counter")
public class CounterResource {
    
    @EJB
    private MBeanManager mBeanManager;
    
    /**
     * Increment counter and return current value
     * This endpoint is called on every request to increment the counter
     */
    @GET
    @Path("/increment")
    @Produces(MediaType.APPLICATION_JSON)
    public Response incrementCounter() {
        Counter counter = mBeanManager.getCounter();
        counter.increment();
        
        Map<String, Object> response = new HashMap<>();
        response.put("memberName", counter.getMemberName());
        response.put("counter", counter.getCounter());
        response.put("totalRequests", counter.getTotalRequests());
        response.put("message", "Counter incremented successfully");
        
        return Response.ok(response).build();
    }
    
    /**
     * Get current counter value without incrementing
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCounter() {
        Counter counter = mBeanManager.getCounter();
        
        Map<String, Object> response = new HashMap<>();
        response.put("memberName", counter.getMemberName());
        response.put("counter", counter.getCounter());
        response.put("totalRequests", counter.getTotalRequests());
        
        return Response.ok(response).build();
    }
    
    /**
     * Reset counter to zero
     */
    @POST
    @Path("/reset")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetCounter() {
        Counter counter = mBeanManager.getCounter();
        counter.resetCounter();
        
        Map<String, Object> response = new HashMap<>();
        response.put("memberName", counter.getMemberName());
        response.put("counter", counter.getCounter());
        response.put("message", "Counter reset successfully");
        
        return Response.ok(response).build();
    }
}

// Made with Bob
