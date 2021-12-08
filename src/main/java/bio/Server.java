package bio;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9080);
        while(true) {
            Socket client = serverSocket.accept(); // 阻塞
            new Thread(() -> {
                System.out.println(String.format("client connect success, port is %s", client.getPort()));
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String msg;
                    while ((msg = br.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).run();
        }
    }

}
