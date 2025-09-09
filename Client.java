import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public final class Client {

    Socket socket;

    BufferedReader br;
    PrintWriter out;

    public Client()
    {
        try {
            System.out.println("Sending request to server");
            socket= new Socket("192.168.31.10",7777);
            System.out.println("Connection done");

            br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out= new PrintWriter(socket.getOutputStream());

            startReading();
            startWriting();



        } catch (IOException e) {
        }
    }

    public void startReading(){
        Runnable r1= ()->{

            System.out.println("Reader Started...");
            try{
            while (true) { 
               
                String msg= br.readLine();
                if(msg.equals("exit")){
                    System.out.println("Server terminated the chat");
                    socket.close();
                    break;
                }

                System.out.println("Server:" +msg);
            }               
            }catch(Exception e){
                e.printStackTrace();
            }

        };
        new Thread(r1).start();
     }

     public void startWriting(){
        Runnable r2= ()->{
            System.out.println("Writer started...");
            while (!socket.isClosed()) { 
                try {

                    BufferedReader br1= new BufferedReader(new InputStreamReader(System.in));
                    String content= br1.readLine();
                    out.println(content);
                    out.flush();
                    if(content.equals("exit")){
                        socket.close();
                        break;
                    }
                    
                } catch (IOException e) {
                }
            }
        };

        new Thread(r2).start();

     }
    public static void main(String[] args) {
        System.out.println("this is client...");
        new Client();
    }}
    

