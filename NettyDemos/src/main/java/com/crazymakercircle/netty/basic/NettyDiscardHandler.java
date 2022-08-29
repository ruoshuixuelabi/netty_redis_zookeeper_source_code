package com.crazymakercircle.netty.basic;

import com.crazymakercircle.util.Logger;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * create by 尼恩 @ 疯狂创客圈
 * 在Reactor模式中，所有的业务处理都在Handler中完成，业务处理一般需要自己编写，这里编写一个新类：NettyDiscardHandler
 **/
public class NettyDiscardHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        try {
            Logger.info("收到消息,丢弃如下:");
            while (in.isReadable()) {
                System.out.print((char) in.readByte());
            }
            System.out.println();
        } finally {
            ReferenceCountUtil.release(msg);
        }
//        ctx.fireChannelRead(msg);
    }
}