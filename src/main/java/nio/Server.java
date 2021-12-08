package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

public class Server {

    public static void main(String[] args) throws IOException {
        LinkedList<SocketChannel> clients = new LinkedList<>();
        ServerSocketChannel ss = ServerSocketChannel.open();
        ss.bind(new InetSocketAddress(8081));
        ss.configureBlocking(false);
        while(true) {
            // 不会阻塞了，NIO模式下，如果没有客户端连进来，直接返回null，BIO模式下会一直阻塞
            SocketChannel client = ss.accept();
            if (client != null) {
                // 为客户端也设置非阻塞
                client.configureBlocking(false);
                System.out.println(String.format("client connect success, port is %s", client.socket().getPort()));
                clients.add(client);
            }

            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
            for (SocketChannel socketChannel : clients) {
                int num = socketChannel.read(buffer);
                if (num > 0) {
                    buffer.flip();
                    byte[] bytes = new byte[buffer.limit()];
                    buffer.get(bytes);
                    System.out.println(String.format("Port: %s, message: %s", socketChannel.socket().getPort(), new String(bytes)));
                }
            }
        }
    }

}
