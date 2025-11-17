package com.example.liberty.cluster;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Servlet to test HTTP session replication across Liberty cluster members.
 * 
 * This servlet demonstrates:
 * - Session creation and persistence
 * - Session attribute storage and retrieval
 * - Session sharing across multiple servers
 * - Session failover capabilities
 * 
 * Access this servlet on different ports (9080, 9081, 9082) with the same
 * session cookie to verify session replication is working.
 */
@WebServlet("/session-test")
public class SessionTestServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Get or create session
        HttpSession session = request.getSession(true);
        
        // Get or initialize counter
        Integer counter = (Integer) session.getAttribute("counter");
        if (counter == null) {
            counter = 0;
        }
        counter++;
        session.setAttribute("counter", counter);
        
        // Get or initialize creation time
        Date creationTime = (Date) session.getAttribute("creationTime");
        if (creationTime == null) {
            creationTime = new Date();
            session.setAttribute("creationTime", creationTime);
        }
        
        // Get or initialize server history
        String serverHistory = (String) session.getAttribute("serverHistory");
        String currentServer = ":" + request.getLocalPort();
        if (serverHistory == null) {
            serverHistory = currentServer;
        } else if (!serverHistory.endsWith(currentServer)) {
            serverHistory += " → " + currentServer;
        }
        session.setAttribute("serverHistory", serverHistory);
        
        // Prepare response
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        // Generate HTML response
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("    <title>Liberty Cluster - Session Replication Test</title>");
        out.println("    <style>");
        out.println("        body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }");
        out.println("        .container { background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); max-width: 800px; margin: 0 auto; }");
        out.println("        h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }");
        out.println("        .info-box { background: #ecf0f1; padding: 15px; margin: 15px 0; border-radius: 5px; border-left: 4px solid #3498db; }");
        out.println("        .info-label { font-weight: bold; color: #2c3e50; display: inline-block; width: 150px; }");
        out.println("        .info-value { color: #27ae60; font-family: monospace; }");
        out.println("        .server-port { font-size: 24px; font-weight: bold; color: #e74c3c; }");
        out.println("        .button { display: inline-block; padding: 10px 20px; background: #3498db; color: white; text-decoration: none; border-radius: 5px; margin: 10px 5px; }");
        out.println("        .button:hover { background: #2980b9; }");
        out.println("        .instructions { background: #fff3cd; padding: 15px; margin: 20px 0; border-radius: 5px; border-left: 4px solid #ffc107; }");
        out.println("        .success { color: #27ae60; font-weight: bold; }");
        out.println("        .history { background: #e8f5e9; padding: 10px; border-radius: 5px; margin: 10px 0; }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("    <div class='container'>");
        out.println("        <h1>🔄 Liberty Cluster - Session Replication Test</h1>");
        out.println("        ");
        out.println("        <div class='info-box'>");
        out.println("            <p><span class='info-label'>Session ID:</span> <span class='info-value'>" + session.getId() + "</span></p>");
        out.println("            <p><span class='info-label'>Request Counter:</span> <span class='info-value'>" + counter + "</span></p>");
        out.println("            <p><span class='info-label'>Session Created:</span> <span class='info-value'>" + creationTime + "</span></p>");
        out.println("            <p><span class='info-label'>Current Server:</span> <span class='server-port'>Port " + request.getLocalPort() + "</span></p>");
        out.println("            <p><span class='info-label'>Max Inactive:</span> <span class='info-value'>" + session.getMaxInactiveInterval() + " seconds</span></p>");
        out.println("        </div>");
        out.println("        ");
        out.println("        <div class='history'>");
        out.println("            <p><span class='info-label'>Server History:</span> <span class='info-value'>" + serverHistory + "</span></p>");
        out.println("        </div>");
        out.println("        ");
        out.println("        <div style='text-align: center; margin: 20px 0;'>");
        out.println("            <a href='session-test' class='button'>🔄 Refresh (Same Server)</a>");
        out.println("            <a href='http://localhost:9080/liberty-cluster-app/session-test' class='button'>Controller (9080)</a>");
        out.println("            <a href='http://localhost:9081/liberty-cluster-app/session-test' class='button'>Member 1 (9081)</a>");
        out.println("            <a href='http://localhost:9082/liberty-cluster-app/session-test' class='button'>Member 2 (9082)</a>");
        out.println("        </div>");
        out.println("        ");
        out.println("        <div class='instructions'>");
        out.println("            <h3>📋 How to Test Session Replication:</h3>");
        out.println("            <ol>");
        out.println("                <li>Click <strong>Refresh</strong> to increment the counter on the current server</li>");
        out.println("                <li>Click on a <strong>different server button</strong> (9080, 9081, or 9082)</li>");
        out.println("                <li>Verify that:");
        out.println("                    <ul>");
        out.println("                        <li>✅ Session ID remains the same</li>");
        out.println("                        <li>✅ Counter continues incrementing</li>");
        out.println("                        <li>✅ Creation time is preserved</li>");
        out.println("                        <li>✅ Server port changes</li>");
        out.println("                        <li>✅ Server history shows the path</li>");
        out.println("                    </ul>");
        out.println("                </li>");
        out.println("            </ol>");
        out.println("            <p class='success'>✨ If all values persist across servers, session replication is working!</p>");
        out.println("        </div>");
        out.println("        ");
        out.println("        <div class='instructions'>");
        out.println("            <h3>🧪 Advanced Testing:</h3>");
        out.println("            <p><strong>Test Failover:</strong></p>");
        out.println("            <ol>");
        out.println("                <li>Create a session on one server</li>");
        out.println("                <li>Stop that server (Ctrl+C in its terminal)</li>");
        out.println("                <li>Access a different server with the same session</li>");
        out.println("                <li>Session data should still be available!</li>");
        out.println("            </ol>");
        out.println("        </div>");
        out.println("        ");
        out.println("        <div style='text-align: center; margin-top: 30px; color: #7f8c8d; font-size: 12px;'>");
        out.println("            <p>Liberty Cluster Deployment | Session Replication Demo</p>");
        out.println("        </div>");
        out.println("    </div>");
        out.println("</body>");
        out.println("</html>");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}

// Made with Bob
