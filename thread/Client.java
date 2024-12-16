import java.io.BufferedReader;  // char
import java.io.InputStreamReader;  // char
import java.io.PrintWriter; // char
import java.io.InputStream; // byte
import java.io.RandomAccessFile;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
    static String serverIP = "172.27.106.174";
    static final int port = 12345;
    static final String clientPath = "C:\\Users\\Mangpor\\Documents\\assignments\\try os\\";
    static final int threadN = 10;

    public Client() {
        try {
            System.out.println("This is Client");
            Socket clientSocket = new Socket(serverIP, port);
            System.out.println("Connected to server -> " + clientSocket); //เชื่อมต่อ client ด้วย Socket

            //getInputStream = เป็นเมธอดใน Java ซึ่งถูกใช้ในการอ่านข้อมูล
            //InputStreamReader แปลง byte เป็น char
            //BufferedReader เพิ่มประสิทธิภาพการอ่านข้อมูล

            BufferedReader inputFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter outputToServer = new PrintWriter(clientSocket.getOutputStream(), true);

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
                    outputToServer.println(clientCommand);//ส่ง clientCommand ไปยัง server
                    String allFiles;
                    while ((allFiles = inputFromServer.readLine()) != null) { //วน loop โดยเช็คว่า allFiles ว่าง = null หรือเปล่า
                        if ("EOF".equals(allFiles)) {
                            break;//ถ้า files สิ้นสุดแล้วให้ break (EOF = end of files)
                        }
                        System.out.println(allFiles);
                    }
                } else if (clientCommand.equals("/request")) {
                    outputToServer.println(clientCommand);
                    System.out.print("Enter file name to download: ");
                    String fileName = sc.nextLine();
                    outputToServer.println(fileName);//ส่ง fileName ที่ client ต้องการ ไปยัง server


                    /* String messageFromServer = inTextFromServer.readLine();
                    long fileSize = 0;
                    String errorFromServer = "";
                    if(messageFromServer.equals("Long")){
                        fileSize = Long.parseLong(inTextFromServer.readLine());
                    }
                    else if(messageFromServer.equals("String")){
                        errorFromServer = inTextFromServer.readLine();
                    } */

                    try {
                        /*try เป็นส่วนหนึ่งของกลไกการจัดการข้อผิดพลาด

                         readLine() อ่านข้อมูลทีละบรรทัดจากแหล่งข้อมูลที่เชื่อมต่ออยู่และคืนค่าเป็นสตริง (String)
                         parseLong() ใช้เพื่อแปลงสตริงที่เป็นตัวเลขให้เป็นค่าประเภท long*/
                        long fileSize = Long.parseLong(inputFromServer.readLine());
                        long partSize = fileSize / threadN; //100/3 partSize = 33 ทั้งหมด 3 partSize
                        long remainingSize = fileSize % threadN; //เศษเหลือ 1 เอาไปไว้ที่  partSize ตัวสุดท้าย

                        if (fileSize > 0) {
                            System.out.println("FileSize: " + fileSize);
                            
                            ArrayList<Thread> threads = new ArrayList<>(); // สร้าง ArrayList เพื่อนมาเก็บ thread ทั้ง 10 ตัว เพื่อมาเช็คว่า thread Download ครบทั้ง 10 ตัวและเช็ค completed
                            for (int i = 0; i < threadN; i++) {
                                long startByte = i * partSize;
                                long endByte;
                                if (i == threadN - 1) {// thread สุดท้าย
                                    endByte = startByte + partSize + remainingSize;
                                } else {// thread เริ่ม - ก่อนสุดท้าย
                                    endByte = startByte + partSize; 
                                }
                                System.out.println("Thread " + (i + 1) + " Start bytes: " + startByte + " End bytes: " + endByte);
                                Socket clientSocket2 = new Socket(serverIP, port);
                                Downloader t = new Downloader(startByte, endByte, fileName, clientSocket2); //แตก theread
                                threads.add(t);// เพิ่ม thread ใฟม่ที่แตกออกมาใน ArrayList
                                t.start();//เริ่มต้นการทำงานของเธรด (thread) ที่ถูกสร้างขึ้นมา
                            }
                            for (Thread thread : threads) {//วน loop arraylist
                                try {
                                    thread.join();
                                    /*เมื่อเรียก thread.join():
                                    เธรดหลัก (ซึ่งก็คือเธรดที่รันเมธอด main() หรือกระบวนการหลักของโปรแกรม) จะหยุดทำงานชั่วคราวและรอจนกว่าเธรดที่กำลังรออยู่ (ในกรณีนี้คือ thread) จะทำงานเสร็จสมบูรณ์
                                    เมื่อเธรดที่เรียก join() จบการทำงานแล้ว เธรดหลักจะดำเนินการต่อไปยังรอบถัดไปของลูป
                                    นี่เป็นการประกันว่าเธรดหลักจะรอจนกว่าเธรดทั้งหมดจะเสร็จสิ้นการทำงานก่อนที่จะทำงานอื่นต่อไปในโปรแกรม*/
                                } catch (InterruptedException e) {
                                    e.printStackTrace(); // พิมพ์รายละเอียดของข้อยกเว้น ex.ชื่อของข้อยกเว้น,ข้อความแสดงข้อผิดพลาด,สแต็กเทรซ
                                }
                            }
                            System.out.println("Download completed");
                        }
                    } catch (Exception e) {
                        System.err.println("File not found");
                    }
                } else if (clientCommand.equals("/exit")) {
                    outputToServer.println(clientCommand);
                    System.out.println("Disconneting...");
                    clientSocket.close();//ปิดการเชือมต่อ Socket
                } else {
                    System.out.println("Wrong command, Please try again");
                }
            }
        } catch (Exception e) {
            System.out.println("Connection error");
        }
    }   

    private static class Downloader extends Thread {
        private long startByte, endByte;
        private String fileName;
        private Socket clientSocket;

        public Downloader(long startByte, long endByte, String fileName, Socket clientSocket) {
            this.startByte = startByte;
            this.endByte = endByte;
            this.fileName = fileName;
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                // getOutputStream() ส่งข้อมูลเป็นไบต์ ไปให้ PrintWriter ที่ห่อหุ้ม OutputStream
                // เพื่อให้สามารถส่งข้อมูลในรูปแบบข้อความ (text)
                // true เปืดใช้งาน auto-flush" ทำให้ข้อมูลถูกส่งออก (flush)
                // ทันทีโดยไม่ต้องรอให้บัฟเฟอร์เต็ม
               
               
                out.println("download"); //ส่งคำว่า download
                out.println(fileName); // ส่งพารามิเตอร์ fileName, startByte, endByte ให้ฝั่ง server
                out.println(startByte);
                out.println(endByte);

                byte[] buffer = new byte[1024];// สร้าง + กำหนดขนาด buffer ใช้ 1024 เพราะมีขนาดไม่เล็กหรือใหญ่เกินไป Buffer ขนาดใหญ่จะใช้หน่วยความจำมากขึ้น
                int bytesRead;
                InputStream inByteFromServer = clientSocket.getInputStream(); //    สร้างการรับข้อมูลที่ส่งจากเซิร์ฟเวอร์ไปยังไคลเอนต์ผ่านการเชื่อมต่อแบบ Socket

                try (RandomAccessFile threadFile = new RandomAccessFile(clientPath + "copy-" + fileName, "rw")) {
                    // RandomAccessFile เหมาะสำหรับงานที่ต้องการเข้าถึงตำแหน่งข้อมูลในไฟล์โดยไม่ต้องอ่านข้อมูลทั้งหมดตั้งแต่ต้นจนจบ rw = โหมด read write
                    while (startByte < endByte && (bytesRead = inByteFromServer.read(buffer)) != -1) { //อ่านข้อมูลจาก InputStream และตรวจสอบว่ามีข้อมูลที่สามารถอ่านได้หรือไม่
                        //inByteFromServer.read(buffer) อ่่านข้อมูลจาก server ลงใน buffer
                        threadFile.seek(startByte); //กำหนดตำแหน่งปัจจุบันของไฟล์ที่ต้องการอ่านหรือเขียนข้อมูล
                        threadFile.write(buffer, 0, bytesRead);//เขียนข้อมูลจาก buffer ไปยังไฟล์ที่ตำแหน่งที่กำหนด (0 คือพารามิเตอร์ off ที่ระบุจุดเริ่มต้นภายใน buffer)(bytesRead = จำนวนไบต์ที่ต้องการเขียนจาก buffer)
                        startByte += bytesRead; //กำหนดจุดเริ่มต้นใหม่
                    }
                    /*
                    if(startByte == endByte){
                    System.out.println(Thread.currentThread().getName()+1 + " Final Downloaded part from: " + startByte + " to " + endByte + " [real position("+ (startByte+1) +")]");   
                    }
                    else{
                        System.err.println(Thread.currentThread().getName()+1 + " Final Downloaded part from: " + startByte + " to " + endByte +"  [real position("+ (startByte+1) +")]");   
                    }
                    */
                }
            } catch (IOException e) {
                e.printStackTrace();
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
        new Client(); //เรียก object class client
    }
}
