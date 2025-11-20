package com.example.liberty.member;

/**
 * MBean interface for exposing counter metrics
 */
public interface CounterMBean {
    
    /**
     * Get the current counter value
     * @return current counter value
     */
    long getCounter();
    
    /**
     * Get the total number of requests
     * @return total request count
     */
    long getTotalRequests();
    
    /**
     * Reset the counter to zero
     */
    void resetCounter();
    
    /**
     * Get the member name
     * @return member name
     */
    String getMemberName();
}

// Made with Bob
