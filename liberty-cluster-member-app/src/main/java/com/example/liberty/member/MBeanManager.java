package com.example.liberty.member;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton EJB that manages MBean registration and lifecycle
 */
@Singleton
@Startup
public class MBeanManager {
    
    private static final Logger LOGGER = Logger.getLogger(MBeanManager.class.getName());
    private static final String MBEAN_NAME = "com.example.liberty.member:type=Counter";
    
    private Counter counter;
    private ObjectName objectName;
    private MBeanServer mBeanServer;
    
    @PostConstruct
    public void init() {
        try {
            // Get member name from system property or environment variable
            String memberName = System.getProperty("member.name");
            if (memberName == null) {
                memberName = System.getenv("MEMBER_NAME");
            }
            if (memberName == null) {
                memberName = "unknown-member";
            }
            
            // Create counter instance
            counter = new Counter(memberName);
            
            // Get platform MBean server
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
            
            // Create ObjectName
            objectName = new ObjectName(MBEAN_NAME);
            
            // Register MBean
            if (mBeanServer.isRegistered(objectName)) {
                mBeanServer.unregisterMBean(objectName);
                LOGGER.info("Unregistered existing MBean: " + MBEAN_NAME);
            }
            
            mBeanServer.registerMBean(counter, objectName);
            LOGGER.info("Successfully registered MBean: " + MBEAN_NAME + " for member: " + memberName);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to register MBean", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        try {
            if (mBeanServer != null && objectName != null && mBeanServer.isRegistered(objectName)) {
                mBeanServer.unregisterMBean(objectName);
                LOGGER.info("Successfully unregistered MBean: " + MBEAN_NAME);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to unregister MBean", e);
        }
    }
    
    /**
     * Get the counter instance
     * @return Counter instance
     */
    public Counter getCounter() {
        return counter;
    }
}

// Made with Bob
