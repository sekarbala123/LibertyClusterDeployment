package com.example.controller;

/**
 * Represents a Liberty Cluster Member parsed from ClusterManager MBean tuple
 * The actual tuple format from listMembers is: "hostname,userDir,serverName"
 * Example: "cads-v9-rss-ft1.fyre.ibm.com,C:/wlp-m1/usr,m1"
 */
public class LibertyClusterMember {
    private String hostName;
    private String userDir;
    private String serverName;
    
    public LibertyClusterMember() {
    }
    
    public LibertyClusterMember(String hostName, String userDir, String serverName) {
        this.hostName = hostName;
        this.userDir = userDir;
        this.serverName = serverName;
    }
    
    /**
     * Parse a cluster member tuple string returned by ClusterManager.listMembers()
     * Actual Format: "hostname,userDir,serverName"
     * Example: "cads-v9-rss-ft1.fyre.ibm.com,C:/wlp-m1/usr,m1"
     *
     * @param tuple The tuple string to parse
     * @return LibertyClusterMember object
     */
    public static LibertyClusterMember parseClusterMemberTuple(String tuple) {
        if (tuple == null || tuple.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = tuple.split(",");
        if (parts.length < 3) {
            // Handle incomplete tuples - set what we have
            LibertyClusterMember member = new LibertyClusterMember();
            if (parts.length > 0) member.setHostName(parts[0].trim());
            if (parts.length > 1) member.setUserDir(parts[1].trim());
            if (parts.length > 2) member.setServerName(parts[2].trim());
            return member;
        }
        
        return new LibertyClusterMember(
            parts[0].trim(),  // hostName
            parts[1].trim(),  // userDir
            parts[2].trim()   // serverName
        );
    }
    
    // Getters and Setters
    public String getHostName() {
        return hostName;
    }
    
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
    
    public String getUserDir() {
        return userDir;
    }
    
    public void setUserDir(String userDir) {
        this.userDir = userDir;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
    
    @Override
    public String toString() {
        return "LibertyClusterMember{" +
                "hostName='" + hostName + '\'' +
                ", userDir='" + userDir + '\'' +
                ", serverName='" + serverName + '\'' +
                '}';
    }
}

// Made with Bob
