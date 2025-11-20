package com.example.liberty.member;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of CounterMBean for tracking request counts
 */
public class Counter implements CounterMBean {
    
    private final AtomicLong counter = new AtomicLong(0);
    private final String memberName;
    
    public Counter(String memberName) {
        this.memberName = memberName;
    }
    
    /**
     * Increment the counter
     */
    public void increment() {
        counter.incrementAndGet();
    }
    
    @Override
    public long getCounter() {
        return counter.get();
    }
    
    @Override
    public long getTotalRequests() {
        return counter.get();
    }
    
    @Override
    public void resetCounter() {
        counter.set(0);
    }
    
    @Override
    public String getMemberName() {
        return memberName;
    }
}

// Made with Bob
