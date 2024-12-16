import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.nio.channels.*;

public class Client {
    static String serverIP = "127.0.0.1";
    static final int port = Server.port;
    static final int portChannel = Server.portChannel;
    static final String clientPath = "./ClientPath/";

    private Socket clientSocket;
    private SocketChannel clientChannel;
    private BufferedReader inputFromServer;
    private PrintWriter outputToServer;
    private BufferedInputStream inByteFromServer;

    public Client() {
        try {
            System.out.println("This is Client");
            clientSocket = new Socket(serverIP, port);
            clientChannel = SocketChannel.open(new InetSocketAddress(serverIP, portChannel));
            System.out.println("Connected to server -> " + clientSocket);

            this.inputFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.outputToServer = new PrintWriter(clientSocket.getOutputStream(), true);
            this.inByteFromServer = new BufferedInputStream(clientSocket.getInputStream());

            Scanner sc = new Scanner(System.in);
            String clientCommand;
            while (!clientSocket.isClosed()) {
                System.out.println("----------------------------------------");
                System.out.println("-- All Command --");
                System.out.println("Type /filelist to see all files in server");
                System.out.println("Type /request to download file from server");
                System.out.println("Type /exit to disconnect to server");
                System.out.print("Enter your command: ");
                clientCommand = sc.nextLine();

                if (clientCommand.equals("/filelist")) {
                    outputToServer.println(clientCommand);
                    String allFiles;
                    while ((allFiles = inputFromServer.readLine()) != null) {
                        if ("EOF".equals(allFiles)) {
                            break;
                        }
                        System.out.println(allFiles);
                    }
                } else if (clientCommand.equals("/request")) {
                    outputToServer.println(clientCommand);
                    System.out.print("Enter file name to download: ");
                    String fileName = sc.nextLine();
                    outputToServer.println(fileName);

                    System.out.println("Please select download type");
                    System.out.println("[1] By Copy\n[2] By Zerocopy\n[3] Both type");
                    System.out.print("Enter type number for download: ");
                    String type = sc.nextLine();
                    outputToServer.println(type);

                    String messageFromServer = inputFromServer.readLine(); // receive Long/String from server
                    long fileSize = 0;
                    String errorFromServer = "";
                    if (messageFromServer.equals("Long")) {
                        fileSize = Long.parseLong(inputFromServer.readLine());
                    } else if (messageFromServer.equals("String")) {
                        errorFromServer = inputFromServer.readLine();
                    }

                    if (fileSize > 0) {
                        if (type.equals("1")) {
                            System.out.println("Downloading file by Copy -> " + clientSocket);
                            long start = System.currentTimeMillis();
                            copy(fileName, fileSize);
                            long end = System.currentTimeMillis();
                            long time = end - start;
                            System.out.println("> Time used " + time + " ms.");
                        } else if (type.equals("2")) {
                            System.out.println("Downloading file by ZeroCopy -> " + clientSocket);
                            long start = System.currentTimeMillis();
                            zeroCopy(fileName, fileSize);
                            long end = System.currentTimeMillis();
                            long time = end - start;
                            System.out.println("> Time used " + time + " ms.");
                        } else if (type.equals("3")) {
                            System.out.println("Downloading file by both type -> " + clientSocket);
                            long start1 = System.currentTimeMillis();
                            copy(fileName, fileSize);
                            long end1 = System.currentTimeMillis();
                            long time1 = end1 - start1;
                            System.out.println("> [Copy] Time used " + time1 + " ms.");

                            long start2 = System.currentTimeMillis();
                            zeroCopy(fileName, fileSize);
                            long end2 = System.currentTimeMillis();
                            long time2 = end2 - start2;
                            System.out.println("> [Zerocopy] Time used " + time2 + " ms.");
                        } else {
                            System.out.println("Wrong type, Please try again");
                            continue;
                        }
                    } else {
                        System.err.println("Server said: " + errorFromServer);
                    }

                } else if (clientCommand.equals("/exit")) {
                    outputToServer.println(clientCommand);
                    close();
                } else {
                    System.out.println("Wrong command, Please try again");
                }
            }
        } catch (Exception e) {
            System.out.println("Connection error");
        }
    }

    public void copy(String fileName, long fileSize) {
        FileOutputStream fileToDisk = null;
        try {
            fileToDisk = new FileOutputStream(clientPath + "Copy-" + fileName);
            byte[] buffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;

            // System.out.println("File size : " + fileSize);
            System.out.println("Start downloading file...");
            while (totalBytesRead < fileSize && (bytesRead = inByteFromServer.read(buffer)) != -1) {
                fileToDisk.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                // System.out.println("Bytes read: " + bytesRead + ", Total bytes read: " +
                // totalBytesRead);
            }
            // System.out.println("Expected file size: " + fileSize +"Total bytes read: " +
            // totalBytesRead);
            if (totalBytesRead != fileSize) {
                System.err
                        .println("Warning: File size mismatch. Expected: " + fileSize + ", but got: " + totalBytesRead);
            } else {
                System.out.println("File download completely!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // try {
            // fileToDisk.flush();
            // } catch (IOException e) {
            // e.printStackTrace();
            // }
        }
    }

    public void zeroCopy(String fileName, long fileSize) {
        try (FileChannel destinationFile = new FileOutputStream(clientPath + "Zerocopy-" + fileName).getChannel()) {
            long position = 0;
            long count;

            System.out.println("Start downloading file...");
            while (position < fileSize
                    && (count = destinationFile.transferFrom(clientChannel, position, fileSize - position)) > 0) {
                position += count;
                // System.out.println("Transferred: " + count + " bytes, Total transferred: " +
                // (position + count) + " bytes");
            }

            if (position == fileSize) {
                System.out.println("File download completely!");
            } else {
                System.err.println("Warning: Expected file size was " + fileSize + " bytes, but only " + position
                        + " bytes were transferred.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                System.out.println("Disconneting...");
            }
            clientChannel.close();
            inByteFromServer.close();
            outputToServer.close();
            inByteFromServer.close();
        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Client();
    }
}