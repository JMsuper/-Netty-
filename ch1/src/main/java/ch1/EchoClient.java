package ch1;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class EchoClient {

	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		
		EventLoopGroup group = new NioEventLoopGroup();
		
		try {
			Bootstrap b = new Bootstrap();
			b.group(group)
			// 서버와 달리 굳이 Client라고 지정하지 않는건가?
			.channel(NioSocketChannel.class)
			.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception{
					ChannelPipeline p = ch.pipeline();
					p.addLast(new EchoClientHandler());
				}
			});
			
			// connect 메서드는 ChannelFuter 객체를 반환
			// sync() : ChannleFuture 객체의 요청이 완료될 때까지 대기. 단, 요청 실패시 예외던짐
			ChannelFuture f = b.connect("localhost", 8888).sync();
			
			f.channel().closeFuture().sync();
		}
		finally{
			group.shutdownGracefully();
		}
	}

}
