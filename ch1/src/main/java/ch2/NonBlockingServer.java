package ch2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NonBlockingServer {

    // 클라이언트 소켓 채널별로 데이터를 저장할 리스트를 관리하는 맵
    private Map<SocketChannel, List<byte[]>> keepDataTrack = new HashMap<>();
    // 읽기/쓰기 버퍼 크기 2KB로 할당
    private ByteBuffer buffer = ByteBuffer.allocate(2 * 1024);

    private void startEchoServer() {
        try(
            // 멀티플렉서 역할을 하는 Selector 생성
            Selector selector = Selector.open();
            // 서버 소켓 채널 생성 (서버 소켓 스트림 기능 사용할 채널)
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        ){
            // 서버 소켓 채널과 Selector가 정상 생성됐는지 확인
            if(serverSocketChannel.isOpen() && selector.isOpen()) {
                // 논블로킹 모드 설정 (동기 블로킹 하지 않음)
                serverSocketChannel.configureBlocking(false);
                // 8888 포트로 바인딩 (서버가 해당 포트로 클라이언트 연결 기다림)
                serverSocketChannel.bind(new InetSocketAddress(8888));

                // 서버 소켓 채널을 Selector에 등록, OP_ACCEPT 이벤트 수신 대기
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                System.out.println("접속 대기중");

                while(true) {
                    // 이벤트가 발생할 때까지 블로킹하며 기다림
                    selector.select();

                    // Selector의 이벤트 키들(이벤트 발생한 채널)을 반복 처리
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    while(keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove(); // 처리 중인 키는 삭제

                        if(!key.isValid()) { // 유효하지 않으면 다음으로
                            continue;
                        }

                        // 연결 요청 이벤트가 발생하면 연결 처리 메소드 호출
                        if(key.isAcceptable()) {
                            this.acceptOP(key, selector);
                        }
                        // 읽기 가능 이벤트가 발생하면 데이터 읽기 메소드 호출
                        else if(key.isReadable()) {
                            this.readOP(key);
                        }
                        // 쓰기 가능 이벤트가 발생하면 데이터 쓰기 메소드 호출
                        else if(key.isWritable()) {
                            this.writeOP(key);
                        }
                    }
                }
            }
            else {
                System.out.println("서버 소켓을 생성하지 못했습니다.");
            }
        }
        catch(IOException e) {
            System.err.println(e);
        }
    }

    private void acceptOP(SelectionKey key, Selector selector) throws IOException {
        // 연결 요청받은 서버 소켓 채널 가져오기
        ServerSocketChannel serverChannel = (ServerSocketChannel)key.channel();
        // 실제 클라이언트와 통신할 소켓 채널 생성 (클라이언트 연결 수락)
        SocketChannel socketChannel = serverChannel.accept();
        // 논블로킹 모드 설정
        socketChannel.configureBlocking(false);

        System.out.println("클라이언트 연결됨 : " + socketChannel.getRemoteAddress());

        // 해당 클라이언트 소켓에 대한 데이터를 담을 리스트 생성 및 맵에 저장
        keepDataTrack.put(socketChannel, new ArrayList<byte[]>());
        // 클라이언트 소켓을 Selector에 등록, 읽기 이벤트 등록
        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    private void readOP(SelectionKey key) throws IOException {
        try {
            SocketChannel socketChannel = (SocketChannel)key.channel();
            buffer.clear(); // 버퍼 초기화 (position = 0)

            int numRead = -1;
            try {
                // 클라이언트로부터 데이터를 읽음, 읽은 바이트 수 반환
                numRead = socketChannel.read(buffer);
            }
            catch(IOException e) {
                System.out.println("데이터 읽기 에러!");
            }

            // -1이면 클라이언트가 연결 종료한 상태 (EOF)
            if(numRead == -1) {
                this.keepDataTrack.remove(socketChannel);
                System.out.println("클라이언트 연결 종료 : " + socketChannel.getRemoteAddress());
                socketChannel.close();
                key.cancel(); // Selector에서 키 제거 요청
                return;
            }

            // 버퍼에 담긴 읽은 데이터만큼 바이트 배열에 복사
            byte[] data = new byte[numRead];
            System.arraycopy(buffer.array(), 0, data, 0, numRead);

            // 클라이언트로부터 받은 데이터 출력(UTF-8 문자열)
            System.out.println(new String(data, "UTF-8") + " from " + socketChannel.getRemoteAddress());

            // 받은 데이터를 에코 작업(쓰기로 대기) 수행
            doEchoJob(key, data);
        }
        catch(IOException ex) {
            System.err.println(ex);
        }
    }

    private void writeOP(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // 해당 소켓채널에 쓰기 대기 중인 데이터 리스트 가져오기
        List<byte[]> channelData = keepDataTrack.get(socketChannel);
        Iterator<byte[]> its = channelData.iterator();

        // 대기중인 데이터를 모두 전송
        while(its.hasNext()) {
            byte[] it = its.next();
            its.remove();
            socketChannel.write(ByteBuffer.wrap(it));
        }

        // 다시 읽기 모드로 관심 이벤트 변경
        key.interestOps(SelectionKey.OP_READ);
    }

    private void doEchoJob(SelectionKey key, byte[] data) {
        SocketChannel socketChannel = (SocketChannel)key.channel();
        // 쓰기 대기 데이터 리스트 가져오기
        List<byte[]> channelData = keepDataTrack.get(socketChannel);
        // 받은 데이터 추가(에코할 데이터)
        channelData.add(data);

        // 쓰기 이벤트 관심 등록 (writeOP 호출될 수 있게)
        // "이 채널이 네트워크로 데이터를 쓸 수 있을 때 알려달라"는 의도로 Selector에 쓰기 이벤트 감시를 등록하는 것
        key.interestOps(SelectionKey.OP_WRITE);
    }

    public static void main(String[] args) {
        NonBlockingServer main = new NonBlockingServer();
        main.startEchoServer();
    }
}
