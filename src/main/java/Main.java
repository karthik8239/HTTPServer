import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
      String[] str = HttpRequest[1].split("/");
      
      //System.out.println(HttpRequest[1]);
      if(HttpRequest[1].equals("/")){
        System.out.println("version");
        String response = "HTTP/1.1 200 OK\r\n" +
        "Content-Type: application/octet-stream\r\n" +
        "Content-Length: 0\r\n\r\n";
        output.write(response.getBytes());
      }
      else if(str.length > 2 ){
        String fileName = str[2];
        Path path = Paths.get(finalDirectory,fileName);
        if(Files.exists(path)){
          byte[] finalBytes = Files.readAllBytes(path);
          String response = "HTTP/1.1 200 OK\r\n" +
          "Content-Type: application/octet-stream\r\n" +
          "Content-Length: " + finalBytes.length + "\r\n\r\n";
          output.write(response.getBytes());//Header
          output.write(finalBytes);//response body
        }
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
