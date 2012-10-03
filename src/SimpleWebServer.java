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
public class SimpleWebServer extends Thread {

    static final String HTML_START =
            "<html>" +
                    "<title>HTTP Server in java</title>" +
                    "<body>";

    static final String HTML_END =
            "</body>" +
                    "</html>";

    private Socket connectedClient = null;
    private DataOutputStream outToClient = null;
    private static JCheckBox emulateSlowConnection = new JCheckBox("Emulate slow connection",false);


    public SimpleWebServer(Socket client) {
        connectedClient = client;
    }

    public void run() {

        try {

            System.out.println("The Client " +
                    connectedClient.getInetAddress() + ":" + connectedClient.getPort() + " is connected");

            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectedClient.getInputStream()));
            outToClient = new DataOutputStream(connectedClient.getOutputStream());

            String requestString = inFromClient.readLine();

            StringTokenizer tokenizer = new StringTokenizer(requestString);
            String httpMethod = tokenizer.nextToken();
            String httpQueryString = tokenizer.nextToken();

            while (inFromClient.ready()) {
                // Read the HTTP complete HTTP Query (not currently used)
                inFromClient.readLine();
            }

            if (httpMethod.equals("GET")) {
                String fileName = httpQueryString.replaceFirst("/", "");
                fileName = URLDecoder.decode(fileName, "UTF-8");
                int indexOfQ = fileName.indexOf('?');
                if (indexOfQ != -1){
                    fileName = fileName.substring(0,indexOfQ);
                }
                File file = new File(fileName.length()==0?".":fileName);
                if (file.isDirectory()){
                    String[] defaultFileNames = {"index.html", "index.htm"};
                    for (String defaultFilename : defaultFileNames){
                        File f = new File(file, defaultFilename );
                        if (f.exists() && f.isFile()){
                            file = f;
                            break;
                        }
                    }
                }
                System.out.println("File is "+file);
                if (file.isDirectory()){
                    String directory = buildDirectoryHtml(file, fileName);
                    sendResponse(200, directory, false);
                } else if (file.isFile()) {
                    sendResponse(200, fileName, true);
                } else {
                    sendResponse(404, "<b>The Requested resource not found ...." +
                            "Usage: http://127.0.0.1:5000 or http://127.0.0.1:5000/</b>", false);
                }
            } else sendResponse(404, "<b>The Requested resource not found ...." +
                    "Usage: http://127.0.0.1:5000 or http://127.0.0.1:5000/</b>", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildDirectoryHtml(File file, String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>");
        if (fileName.length()==0){
            sb.append("Root");
        } else {
            sb.append(fileName);
        }
        sb.append("</h1>");
        if (fileName.length()>0){
            fileName = "/"+fileName;

            String parentFileName = fileName.substring(0,fileName.lastIndexOf('/'));
            if (parentFileName.length()==0){
                parentFileName = "/";
            }
            sb.append("<a href=\"").append(parentFileName ).append("\">");
            sb.append("..");
            sb.append("</a><br>");
        }
        for (String f : file.list()){
            sb.append("<a href=\"").append(fileName).append("/").append(f).append("\">");
            sb.append(f);
            sb.append("</a><br>");
        }
        return sb.toString();
    }

    public void sendResponse(int statusCode, String responseString, boolean isFile) throws Exception {

        String statusLine;
        String serverDetails = "Server: Java HTTPServer" + "\r\n";
        String contentLengthLine;
        String fileName;
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
            responseString = SimpleWebServer.HTML_START + responseString + SimpleWebServer.HTML_END;
            contentLengthLine = "Content-Length: " + responseString.length() + "\r\n";
        }

        outToClient.writeBytes(statusLine);
        outToClient.writeBytes(serverDetails);
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

        final JFrame frame = new JFrame("SimpleWebServer 1.0");
        Container panel = frame.getContentPane();
        panel.setLayout(new GridLayout(0,1));
        panel.add(new JLabel("TCPServer Waiting for client on port 5000"));
        panel.add(emulateSlowConnection);

        JButton browser = new JButton("Open http://localhost:5000/");
        browser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                try {
                    URI uri = new URI( "http://localhost:5000/" );
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
                        Desktop.getDesktop().browse(new URI("https://github.com/mortennobel/SimpleWebServer"));
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
            (new SimpleWebServer(connected)).start();
        }
    }
}