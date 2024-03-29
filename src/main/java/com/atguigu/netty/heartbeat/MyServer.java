package com.atguigu.netty.heartbeat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class MyServer {
    public static void main(String[] args) throws Exception{
        //创建两个线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(); //8个NioEventLoop
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup);
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.handler(new LoggingHandler(LogLevel.INFO));//向bossGroup添加一个日志处理器
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    //加入一个netty 提供的 IdleStateHandler 处理器
/*说明
1. IdleStateHandler 是netty提供的处理空闲状态的处理器
2. long readerIdleTime : 表示服务端多长时间没有读到客户端的数据, 就会发送一个心跳检测包检测客户端是否还是连接状态
3. long writerIdleTime : 表示服务端多长时间没有向客户端写数据, 就会发送一个心跳检测包检测客户端是否还是连接状态
4. long allIdleTime : 表示服务端多长时间没有与客户端有读写数据操作, 就会发送一个心跳检测包检测客户端是否还是连接状态
5. 文档说明 triggers an {@link IdleStateEvent} when a {@link Channel} has not performed(执行) read, write, or both operation for a while.
6. 当IdleStateEvent触发后,就会传递给管道的下一个handler去处理，通过调用(触发)下一个handler的userEventTriggered方法,在该方法中去处理IdleStateEvent(读空闲，写空闲，读写空闲)*/
                    pipeline.addLast(new IdleStateHandler(7000,7000,10, TimeUnit.SECONDS));
                    //加入一个对空闲检测进一步处理的handler(自定义)
                    pipeline.addLast(new MyServerHandler());
                }
            });

            //启动服务器
            ChannelFuture channelFuture = serverBootstrap.bind(7000).sync();
            channelFuture.channel().closeFuture().sync();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
