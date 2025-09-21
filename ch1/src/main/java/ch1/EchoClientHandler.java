package ch1;

import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class EchoClientHandler extends ChannelInboundHandlerAdapter{
	
	// ChannelHandlerContext : Netty 파이프라인 내에서 핸들러 간의 이벤트 흐름과 상태 관리를 맡아, 
	// 네트워크 이벤트 처리에서 핵심적인 중간 매개체 역할
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		String sendMessage = "Hello, Netty!";
		
		// ByteBuf : 네트워크 I/O에서 데이터를 효율적으로 다루기 위한 바이트 컨테이너로, 
		// Java NIO의 ByteBuffer보다 더 유연하고 편리한 기능을 제공
		ByteBuf messageBuffer = Unpooled.buffer();
		messageBuffer.writeBytes(sendMessage.getBytes());
		
		StringBuilder builder = new StringBuilder();
		builder.append("전송할 문자열 [");
		builder.append(sendMessage);
		builder.append("]");
		
		System.out.println(builder.toString());
		
		ctx.writeAndFlush(messageBuffer);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		String readMessage = ((ByteBuf) msg).toString(Charset.defaultCharset());
		
		StringBuilder builder = new StringBuilder();
		builder.append("수신한 문자열 [");
		builder.append(readMessage);
		builder.append("]");
		
		System.out.println(builder.toString());
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		// ChannelHandlerContext의 close() 호출시 실행 순서를 간략히 요약하면 다음과 같습니다.
		// 1. 현재 핸들러 이후 파이프라인의 다른 핸들러들로 close 이벤트 전파 시작
		// 2. Channel 객체의 close() 메서드 호출해 실제 TCP 소켓 연결 닫기 시도 (비동기 처리)
		// 3. TCP 연결 닫히면 channelInactive, channelUnregistered 이벤트 발생
		// 4. 각 핸들러가 channelInactive 이벤트로 자원 해제 등 후처리 실행
		// 5. ChannelFuture 반환, 완료시점 리스너 실행 가능
		ctx.close();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

}
