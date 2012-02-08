import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * License: Creative Commons Attribution 3.0 Unported (http://creativecommons.org/licenses/by/3.0/)
 * 2011 Morten Nobel-Joergensen
 * https://github.com/mortennobel/SoftimageWebGLExport
 *
 * Based on http://www.prasannatech.net/2008/10/simple-http-server-java.html
 */
public class SimpleHttpServer extends Thread {

    static final String HTML_START =
            "<html>" +
                    "<title>HTTP Server in java</title>" +
                    "<body>";

    static final String HTML_END =
            "</body>" +
                    "</html>";

    private Socket connectedClient = null;
    private BufferedReader inFromClient = null;
    private DataOutputStream outToClient = null;
    private static JCheckBox emulateSlowConnection = new JCheckBox("Emulate slow connection",false);


    public SimpleHttpServer(Socket client) {
        connectedClient = client;
    }

    public void run() {

        try {

            System.out.println("The Client " +
                    connectedClient.getInetAddress() + ":" + connectedClient.getPort() + " is connected");

            inFromClient = new BufferedReader(new InputStreamReader(connectedClient.getInputStream()));
            outToClient = new DataOutputStream(connectedClient.getOutputStream());

            String requestString = inFromClient.readLine();
            String headerLine = requestString;

            StringTokenizer tokenizer = new StringTokenizer(headerLine);
            String httpMethod = tokenizer.nextToken();
            String httpQueryString = tokenizer.nextToken();

            StringBuffer responseBuffer = new StringBuffer();
            responseBuffer.append("<b> This is the HTTP Server Home Page.... </b><BR>");
            responseBuffer.append("The HTTP Client request is ....<BR>");

            System.out.println("The HTTP request string is ....");
            while (inFromClient.ready()) {
                // Read the HTTP complete HTTP Query
                responseBuffer.append(requestString + "<BR>");
                System.out.println(requestString);
                requestString = inFromClient.readLine();
            }

            if (httpMethod.equals("GET")) {
                if (httpQueryString.equals("/")) {
                    // The default home page
                    sendResponse(200, responseBuffer.toString(), false);
                } else {
                    String fileName = httpQueryString.replaceFirst("/", "");
                    fileName = URLDecoder.decode(fileName);
                    if (new File(fileName).isFile()) {
                        sendResponse(200, fileName, true);
                    } else {
                        sendResponse(404, "<b>The Requested resource not found ...." +
                                "Usage: http://127.0.0.1:5000 or http://127.0.0.1:5000/</b>", false);
                    }
                }
            } else sendResponse(404, "<b>The Requested resource not found ...." +
                    "Usage: http://127.0.0.1:5000 or http://127.0.0.1:5000/</b>", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendResponse(int statusCode, String responseString, boolean isFile) throws Exception {

        String statusLine = null;
        String serverdetails = "Server: Java HTTPServer" + "\r\n";
        String contentLengthLine = null;
        String fileName = null;
        String contentTypeLine = "Content-Type: text/html" + "\r\n";
        FileInputStream fin = null;

        if (statusCode == 200)
            statusLine = "HTTP/1.1 200 OK" + "\r\n";
        else
            statusLine = "HTTP/1.1 404 Not Found" + "\r\n";

        if (isFile) {
            fileName = responseString;
            fin = new FileInputStream(fileName);
            contentLengthLine = "Content-Length: " + Integer.toString(fin.available()) + "\r\n";

            if (fileName.endsWith(".htm") || fileName.endsWith(".html"))
                contentTypeLine = "Content-Type: text/html" + "\r\n";
            else if (fileName.endsWith(".js"))
                contentTypeLine = "Content-Type: application/x-javascript\r\n";
            else if (fileName.endsWith(".json"))
                contentTypeLine = "Content-Type: application/json\r\n";
            else if (fileName.endsWith(".jpg"))
                contentTypeLine = "Content-Type: image/jpeg\r\n";
            else if (fileName.endsWith(".png"))
                contentTypeLine = "Content-Type: image/png\r\n";
            else if (fileName.endsWith(".gif"))
                contentTypeLine = "Content-Type: image/gif\r\n";
            else if (fileName.endsWith(".frag") || fileName.endsWith(".vert"))
                contentTypeLine = "Content-Type: text/plain\r\n";
            else
                contentTypeLine = "Content-Type: \r\n";
        } else {
            responseString = SimpleHttpServer.HTML_START + responseString + SimpleHttpServer.HTML_END;
            contentLengthLine = "Content-Length: " + responseString.length() + "\r\n";
        }

        outToClient.writeBytes(statusLine);
        outToClient.writeBytes(serverdetails);
        outToClient.writeBytes(contentTypeLine);
        outToClient.writeBytes(contentLengthLine);
        outToClient.writeBytes("Connection: close\r\n");
        outToClient.writeBytes("\r\n");

        if (isFile) sendFile(fin, outToClient);
        else outToClient.writeBytes(responseString);

        outToClient.close();
    }

    public void sendFile(FileInputStream fin, DataOutputStream out) throws Exception {
        byte[] buffer = new byte[1024];
        int bytesRead;
        if (emulateSlowConnection.isSelected()){
            while ((bytesRead = fin.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                Thread.sleep(50);
            }
        } else {
            while ((bytesRead = fin.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        fin.close();
    }

    public static void main(String args[]) throws Exception {

        ServerSocket Server = new ServerSocket(5000, 10, InetAddress.getByName("127.0.0.1"));

        final JFrame frame = new JFrame("Simple web-server");
        Container panel = frame.getContentPane();
        panel.setLayout(new GridLayout(0,1));
        panel.add(new JLabel("TCPServer Waiting for client on port 5000"));
        panel.add(emulateSlowConnection);

        JButton browser = new JButton("Open http://localhost:5000/");
        browser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                try {
                    URI uri = new URI( "http://localhost:5000/index.html" );
                    desktop.browse( uri );
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            }
        });
        panel.add(browser);
        JButton about = new JButton("About");
        about.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(new URI("https://github.com/mortennobel/SimpleHttpServer"));
                    } catch (Exception ee) {
                        JOptionPane.showMessageDialog(frame,ee.toString());
                    }
                } else {
                    JOptionPane.showMessageDialog(frame,"");
                }
            }
        });
        panel.add(about);
        JButton exit = new JButton("Exit");
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        panel.add(exit);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.pack();
        frame.setVisible(true);
        while (true) {
            Socket connected = Server.accept();
            (new SimpleHttpServer(connected)).start();
        }
    }
}