import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    static final int port = 8888;
    static final String serverPath = "C:\\Users\\Mangpor\\Pictures\\art genshin\\";
    private static int clientLeft;

    public Server() {
        int clientN = 0;
        try (ServerSocket serverSocket = new ServerSocket(port);) {//สร้างเซิร์ฟเวอร์ซ็อกเก็ต (server socket) ใน Java ซึ่งใช้ในการรับการเชื่อมต่อจากคลไคลเอนต์ผ่านเครือข่าย
            System.out.println("This is Server");
            System.out.println("Server started on port " + port);

            while (!serverSocket.isClosed()) { //เช็คว่า serverSocket ยังไม่ปิด
                Socket clientSocket = serverSocket.accept();
                clientN++; //นับ จำนวน client ทั้งหมดที่เข้ามาเชื่อมต่อ
                new ClientHandler(clientSocket, clientN).start(); //เริ่มต้นการทำงานของเธรด (thread) ที่ถูกสร้างขึ้นมา
                System.out.println("Client " +clientN+ " connected -> " +clientSocket);
                clientLeft++; //นับ จำนวน client ที่ใช้งานอยู่ในปัจจุบัน
                System.out.println("Amount of client connected left: " +clientLeft);
            }
        } catch (Exception e) {
            System.out.println("Server failed");
        }
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private BufferedReader inputFromClient;
        private PrintWriter outputToClient;
        private int clientN;

        public ClientHandler(Socket clientSocket, int clientN) throws IOException { // clientSocket ส่งมาจากบรรทัดที่ 25
            this.clientSocket = clientSocket;
            this.clientN = clientN;
            this.inputFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); //สร้างBufferedReader ที่เชื่อมต่อกับ InputStream ของ Socket ซึ่งใช้ในการรับข้อมูลจากคลไคลเอนต์ผ่านเครือข่าย
            this.outputToClient = new PrintWriter(clientSocket.getOutputStream(), true);//สร้าง PrintWriter ที่เชื่อมต่อกับ OutputStream ของ Socket ซึ่งใช้ในการส่งข้อมูลจากเซิร์ฟเวอร์ไปยังคลไคลเอนต์ผ่านเครือข่าย
        }

        public void fileList() throws IOException { //แสดงรายการ
            File[] files = new File(serverPath).listFiles(); //ดึงรายการไฟล์ทั้งหมดจากโฟลเดอร์ที่ระบุใน serverPath และเก็บไว้ในอาร์เรย์ของ File ออบเจ็กต์
            if (files.length == 0) { //กรณีไม่มีไฟล์
                outputToClient.println("No file in server");
                System.out.println("File list does not exit");
            } else {
                String allFiles = "";
                for (int i = 0; i < files.length; i++) {
                    allFiles += String.format("[%d] - %s\n", i + 1, files[i].getName()); //แสดงรายการ file ทั้งหมด
                }
                outputToClient.println(allFiles);
                outputToClient.println("EOF");
                System.out.println("File list sent successfully");
            }
        }

        public void sendFile(String fileName, long startByte, long endByte) { //ส่งไฟล์
            try (RandomAccessFile raf = new RandomAccessFile(new File(serverPath+fileName), "r")) {//เข้าถึงตำแหน่งข้อมูลในไฟล์ในโหมด read
                byte[] buffer = new byte[1024]; // สร้าง + กำหนดขนาด buffer
                int bytesRead;
                raf.seek(startByte); //กำหนดตำแหน่งปัจจุบันของไฟล์ที่ต้องการอ่านหรือเขียนข้อมูล
                OutputStream outByteToClient = clientSocket.getOutputStream(); //สร้างการส่งข้อมูลจากเซิร์ฟเวอร์ไปยังคลไคลเอนต์ผ่านเครือข่าย

                while (startByte < endByte && (bytesRead = raf.read(buffer)) != -1) { //อ่านข้อมูลจาก RandomAccessFile และเขียนข้อมูลลงในไฟล์ 
                    // raf.read(buffer) = อ่านเข้า buffer
                    outByteToClient.write(buffer, 0, bytesRead); //เขียนข้อมูลจากบัฟเฟอร์ไปยัง OutputStream ที่เชื่อมต่อกับคลไคลเอนต์
                    startByte += bytesRead;
                }
                System.out.println("Client " + clientN + " downloaded file: " + fileName);
                outByteToClient.flush();//ส่งข้อมูลที่ค้างอยู่ในบัฟเฟอร์ของ OutputStream ไปยังปลายทาง
                System.out.println("File sent successfully");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            String clientCommand;
            try {
                while ((clientCommand = inputFromClient.readLine()) != null) { //เงื่อนไขคือต้องมีข้อความจาก client เข้า
                    if (!clientCommand.equals("download")) {
                        System.out.println("Client " +clientN+ " selected command: " +clientCommand);
                    }
                    if (clientCommand.equals("/filelist")) {//กรณี fileList
                        fileList(); //แสดง fileList
                    } else if (clientCommand.equals("/request")) {//กรณี request
                        String fileName = inputFromClient.readLine(); //รับชื่อไฟล์จาก Client
                        File file = new File(serverPath + fileName); //สร้างออบเจ็กต์ File ที่ชี้ไปยังไฟล์ในระบบไฟล์ของเซิร์ฟเวอร์ โดยรวม serverPath และ fileName เพื่อกำหนดพาธที่แน่นอนของไฟล์นั้น
                        if (file.exists()) { //ถ้าหา file เจอ
                            outputToClient.println(file.length());
                        } else { //ถ้าหา file ไม่เจอ ให้ client ส่งชื่อ file มาใหม่
                            outputToClient.println("File does not exist");
                            System.out.println("Client " + clientN + " attempted to download a file that does not exist: " + fileName);
                        }
                    } else if (clientCommand.equals("download")) { 
                        String fileName = inputFromClient.readLine(); //รับชื่อ file ที่ต้องการโหลดมาจาก client
                        long startByte = Long.parseLong(inputFromClient.readLine()); //การอ่านข้อมูลจาก BufferedReader ซึ่งเป็นการเชื่อมต่อจาก client และแปลงข้อมูลที่อ่านได้จากสตริงเป็นค่าประเภท long
                        long endByte = Long.parseLong(inputFromClient.readLine());
                        sendFile(fileName, startByte, endByte);
                        clientLeft--; //ลบ thread ฝั่ง client ที่เอามาช่วย download 
                        System.err.println("Amount of client connected left: " +clientLeft);
                    } else if (clientCommand.equals("/exit")) {//กรณีexit
                        System.out.println("Client " +clientN+ " disconneted -> " + clientSocket);
                        clientLeft--; // ลบ client จริงๆ
                        System.err.println("Amount of client connected left: " +clientLeft);
                        clientSocket.close(); //ปิดการเชื่อมต่อ
                    } else {
                        System.out.println("Wrong command");
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
        new Server(); //เรียก object class server เพื่อเริ่มการทำงาน
    }
}
