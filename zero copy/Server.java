import java.io.*;
import java.net.*;
import java.nio.channels.*;

public class Server {
    static final int port = 12345;
    static final int portChannel = 12346;
    static final String serverPath = "./ServerPath/";

    public Server() {
        try (ServerSocket serverSocket = new ServerSocket(port);
             ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            serverChannel.socket().bind(new InetSocketAddress(portChannel));
            System.out.println("This is Server");
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                SocketChannel clientChannel = serverChannel.accept();
                if (clientSocket.isConnected()) {
                    System.out.println("Client connected -> " + clientSocket);
                    System.out.println("ClientSocketChannel connected -> " + clientChannel);
                    new ClientHandler(clientSocket,clientChannel).start();
                }
            }
        } catch (Exception e) {
            System.err.println("Server failed");
        }
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private SocketChannel clientChannel;
        private BufferedReader inputFromClient;
        private BufferedOutputStream outByteToClient;
        private PrintWriter outputToClient;

        public ClientHandler(Socket clientSocket, SocketChannel clientChannel) throws IOException {
            this.clientSocket = clientSocket;
            this.clientChannel = clientChannel;
            this.inputFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.outputToClient = new PrintWriter(clientSocket.getOutputStream(), true);
            this.outByteToClient = new BufferedOutputStream(clientSocket.getOutputStream());
        }

        public void fileList() throws IOException {
            File[] files = new File(serverPath).listFiles();
            if (files.length == 0) {
                outputToClient.println("No file in server");
                System.err.println("File list does not exit");
            } else {
                String allFiles = "";
                for (int i = 0; i < files.length; i++) {
                    double fileSize = files[i].length();
                    String prefix = "B";
                    if(fileSize > 1000000){
                        prefix = "MB";
                        fileSize = fileSize / (1024 * 1024);
                    }
                    else if(fileSize > 1000){
                        prefix = "KB";
                        fileSize = fileSize / 1024;
                    }
                    allFiles += String.format("%d -",i+1) +" "+ files[i].getName() + " [" + String.format("%.1f", fileSize) + " " + prefix + "]\n";
                }
                outputToClient.println(allFiles);
                outputToClient.println("EOF");
                System.out.println("File list sent successfully");
            }
        }

        public void zeroCopySend(File file){
            try(FileChannel sourceFile = new FileInputStream(file).getChannel()){
                long position = 0;
                long count;

                while (position < file.length() && (count = sourceFile.transferTo(position, sourceFile.size() - position, clientChannel)) > 0) {
                    // System.out.println("Position: "+position+", Count: "+count);
                    position += count;
                    // System.out.println("CountRead: " + count);
                    System.out.println("Transferred: " + count + " bytes, Total transferred: " + position + " bytes");
                }
                // long bytesTransferred = sourceFile.transferTo(0, sourceFile.size(), clientChannel);
                if (position == file.length()) {
                    System.out.println("File size: " + file.length() + " Total bytes read: " + position);
                    System.out.println("File sent successfully!");
                } else {
                    System.err.println("Warning: Not all bytes transferred. Expected: " + file.length() + ", Transferred: " + position);
                }
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }

        public void copySend (File file){
            System.out.println("File Copying..");
            BufferedInputStream inByteFromFile = null;
            BufferedOutputStream outByteToClient = null;
            try {
                inByteFromFile = new BufferedInputStream(new FileInputStream(file));
                outByteToClient = new BufferedOutputStream(clientSocket.getOutputStream());
                if(file.exists()){
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    long totalBytesRead = 0;
                    while( totalBytesRead < file.length() && ( bytesRead = inByteFromFile.read(buffer) ) != -1 ){
                        outByteToClient.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;                        
                        System.out.println("Bytes read: " + bytesRead + ", Total bytes read: " + totalBytesRead);
                    }
                    
                    outByteToClient.flush();
                    if (totalBytesRead != file.length()) {
                        System.err.println("Warning: File size mismatch. Expected: " + file.length() + ", but got: " + totalBytesRead);
                    } else {
                        System.out.println("File size: " + file.length() + " Total bytes read: " + totalBytesRead);
                        System.out.println("File sent successfully");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // try {
                //     inByteFromFile.close();
                //     outByteToClient.flush();
                // } catch (IOException e) {
                //     e.printStackTrace();
                // }
            }
        }

        @Override
        public void run() {
            String clientCommand;
            try {
                while ((clientCommand = inputFromClient.readLine()) != null) {
                    System.out.println("Client selected command: " +clientCommand);
                    if (clientCommand.equals("/filelist")) {
                        fileList();
                    } else if (clientCommand.equals("/request")) {
                        String fileName = inputFromClient.readLine();
                        String clientType = inputFromClient.readLine();
                        System.out.println("Client selected type: " +clientType);
                        File file = new File(serverPath + fileName);
                        if (file.exists()) {
                            outputToClient.println("Long");
                            outputToClient.println(file.length());
                             if (clientType.equals("1")) {
                                System.out.println("Copy sent " + fileName);
                                copySend(file);
                            } else if (clientType.equals("2")) {
                                System.out.println("Zerocopy sent " + fileName);
                                zeroCopySend(file);
                            } else if (clientType.equals("3")) {
                                System.out.println("Both sent " + fileName);
                                copySend(file);
                                zeroCopySend(file);
                            }
                        } else {
                            outputToClient.println("String");
                            outputToClient.println("File does not exist");
                            System.out.println("Client attempted to download a file that does not exist: " + fileName);
                        }
                    } else if (clientCommand.equals("/exit")) {
                        System.out.println("Client disconneted -> " + clientSocket);
                        clientSocket.close();
                    } else {
                        System.err.println("Wrong command");
                    }
                }
            } catch (Exception e) {

            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        new Server();
    }
}