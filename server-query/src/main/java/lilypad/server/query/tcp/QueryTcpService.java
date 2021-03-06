package lilypad.server.query.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lilypad.server.query.tcp.net.QueryTcpHandler;
import lilypad.server.common.service.Service;

public class QueryTcpService extends Service<QueryTcpConfig> {
	
	private static final StringEncoder stringEncoder = new StringEncoder();
	private static final StringDecoder stringDecoder = new StringDecoder();
	private QueryTcpHandler handler;
	
	private NioEventLoopGroup parentEventGroup;
	private NioEventLoopGroup childEventGroup;
	private boolean running;
	
	public void enable(QueryTcpConfig config) throws Exception {
		this.handler = new QueryTcpHandler(config.querytcp_getPlayable());
		ServerBootstrap serverBootstrap = new ServerBootstrap().group(this.parentEventGroup = new NioEventLoopGroup(), this.childEventGroup = new NioEventLoopGroup())
				.channel(NioServerSocketChannel.class)
				.localAddress(config.querytcp_getBindAddress())
				.childHandler(new ChannelInitializer<SocketChannel>() {
					public void initChannel(SocketChannel channel) throws Exception {
						channel.config().setAllocator(PooledByteBufAllocator.DEFAULT);
						channel.pipeline().addLast(new ReadTimeoutHandler(10));
						channel.pipeline().addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
						channel.pipeline().addLast(stringEncoder);
						channel.pipeline().addLast(stringDecoder);
						channel.pipeline().addLast(handler);
					}
		});
		serverBootstrap.bind().sync();
		this.running = true;
	}

	public void disable() {
		try {
			if(this.parentEventGroup != null) {
				this.parentEventGroup.shutdownGracefully();
			}
			if(this.childEventGroup != null) {
				this.childEventGroup.shutdownGracefully();
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			this.handler = null;
			this.parentEventGroup = null;
			this.childEventGroup = null;
			this.running = false;
		}
	}

	public boolean isRunning() {
		return this.running;
	}

}
