package ch1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class DiscardServer {

	public static void main(String[] args) throws Exception{
		// EventLoopGroup은 여러 개의 EventLoop를 포함하는 그룹으로, 
		// 각 EventLoop는 단일 스레드에서 비동기 네트워크 이벤트(입출력, 연결 등)를 순차적으로 처리합니다.
		// 일반적으로 서버 애플리케이션에서는 두 개의 EventLoopGroup을 만드는데, 
		// 하나는 클라이언트 연결을 수락하는 BossGroup, 다른 하나는 데이터 처리용 WorkerGroup 역할을 합니다.
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) {
					ChannelPipeline p = ch.pipeline();
					p.addLast(new DiscardServerHandler());
				}
			});
			
			ChannelFuture f = b.bind(8888).sync();
			
			f.channel().closeFuture().sync();
		}
		finally {
			// 현재 처리 중인 작업이 완료될 때까지 기다리며, 새로운 작업 수락을 중단
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
		
	}

}
