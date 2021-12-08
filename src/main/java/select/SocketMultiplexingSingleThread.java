package select;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SocketMultiplexingSingleThread {

    private ServerSocketChannel server = null;
    /**
     * 具体哪种多路复用器根据操作系统决定
     * linux：select、poll、epoll
     * unix: kqueue类似epoll
     * windows: select
     */
    private Selector selector = null;

    private static final Integer PORT = 8082;

    public void initServer() {
        try {
            // server -> fd3
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(PORT));

            // epoll_create -> fd7
            selector = Selector.open(); // select/poll、epoll都是用的同一套，但是优先选择epoll, 但是可以 -D修改

            /**
             * register
             * 如果： select/poll： jvm里开辟一个数组，fd3放进去
             * epoll: epoll_ctl(fd7, ADD, fd3, EPOLLIN
             */
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        initServer();
        System.out.println("sever start !!");
        try {
            while(true) {
                Set<SelectionKey> keys = selector.keys();
//                System.out.println(String.format("selectionKey size：%s", keys.size()));
                /**
                 * 调用多路复用器 select, poll or epoll(epoll_wait)
                 * 如果： select/poll： select(fd3) poll(fd3)
                 * epoll: epoll_wait
                 */
                while(selector.select(500) > 0) { // 询问内核有没有事件
                    // 有状态的fd集合
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> it = selectionKeys.iterator();
                    while(it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove(); // 不移除会重复处理
                        if (key.isAcceptable()) { // 连接事件
                            /**
                             * 接收新连接，返回的新连接fd需要
                             * select/poll: 因为内核没有空间，那么在jvm中保存和前面的fd3那个listen的一起
                             * epoll: 希望通过epoll_ctl把新的客户端fd注册到内核空间
                             */
                            acceptHandler(key);
                        } else if (key.isReadable()) { // 可读事件
                            readHandler(key);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept();
            client.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println(String.format("client connect success!! port: %s", client.socket().getPort()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readHandler(SelectionKey key) {
        try {
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
            int num = sc.read(buffer);
            if (num > 0) {
                buffer.flip();
                byte[] bytes = new byte[buffer.limit()];
                buffer.get(bytes);
                System.out.println(String.format("Port: %s, message: %s", sc.socket().getPort(), new String(bytes)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new SocketMultiplexingSingleThread().start();
    }
}
