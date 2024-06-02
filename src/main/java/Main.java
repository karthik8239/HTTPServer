import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");
    String directory = null;
    //Uncomment this block to pass the first stage
    if(args.length > 1 && args[0].equalsIgnoreCase("--directory")){
      directory = args[1];
    }
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      //clientSocket = serverSocket.accept(); // Wait for connection from client.

      while(true) {
        Socket clientSocket = serverSocket.accept();
        final String finalDirectory = directory;
        var thread = new Thread(
          () -> handleHttpConnection(clientSocket,finalDirectory), "HTTP connection");
        thread.start();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

  private static void handleHttpConnection(Socket clientSocket,String finalDirectory) {
     try {
      InputStream input = clientSocket.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(input));
      String line = reader.readLine();
      String[] HttpRequest = line.split(" ");
      OutputStream output = clientSocket.getOutputStream();
      String path = HttpRequest[1];
      //System.out.println(HttpRequest[1]);
      String useragent = "";
      String line1 ;
      StringBuffer sb = new StringBuffer();
      //read headers
      String header = null;
       while((header = reader.readLine()) != null && !header.isEmpty()) { 
          useragent = header.split("\\s+")[1];
       }
       //read body
        while(reader.ready()){
          sb.append((char)reader.read());
        }
        String body = sb.toString();
        if(path.equals("/")){
          String response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: 0\r\n\r\n";
                output.write(response.getBytes());
        }
        else if(path.startsWith("/files/") && Objects.equals(HttpRequest[0], "POST")) {
          String fileName = path.substring(7);
          File file = new File(finalDirectory + "/" + fileName);
          if(file.createNewFile()){
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(body);
            fileWriter.close();
          }
          output.write("HTTP/1.1 201 Created\r\n\r\n".getBytes());
          output.close();
          return;
        }
         else if( path.startsWith("/files/") && Objects.equals(HttpRequest[0], "GET")){
          String fileName = path.substring(7);
          Path filePath = Paths.get(finalDirectory,fileName);
          if(Files.exists(filePath)){
            System.out.println("code reached here");
            byte[] finalBytes = Files.readAllBytes(filePath);
            String response = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "Content-Length: " + finalBytes.length + "\r\n\r\n";
            output.write(response.getBytes());//Header
            output.write(finalBytes);//response body
          }
          else {
            output.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
          }
        }
        else if(path.startsWith("/user-agent")){
          //reader.readLine();
          //reader.readLine();
          String response = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/plain\r\n" + "Content-Length: " + 
          useragent.length() + "\r\n\r\n" + useragent;
          output.write(response.getBytes());
        }
        else if (path.startsWith("/echo/")) {
          String randomString = path.substring(6);
          String response =
              "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
              randomString.length() + "\r\n\r\n" + randomString;
          output.write(response.getBytes());
        }
        else {
          output.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
        }
        output.flush();
        System.out.println("accepted new connection");
     }
       catch(IOException e){
       System.out.println("IOException: " + e.getMessage());
     }
  }
}
