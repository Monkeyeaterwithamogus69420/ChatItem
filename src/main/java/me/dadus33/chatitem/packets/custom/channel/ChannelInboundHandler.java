package me.dadus33.chatitem.packets.custom.channel;

import java.util.HashMap;
import java.util.logging.Level;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import me.dadus33.chatitem.packets.ChatItemPacket;
import me.dadus33.chatitem.packets.PacketContent;
import me.dadus33.chatitem.packets.PacketType;
import me.dadus33.chatitem.packets.custom.CustomPacketManager;

public class ChannelInboundHandler extends ChannelInboundHandlerAdapter {

	public static final HashMap<Channel, ChatItemPacket> TMP = new HashMap<>();
	private CustomPacketManager packetManager;
	
	public ChannelInboundHandler(CustomPacketManager packetManager) {
		this.packetManager = packetManager;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ctx.fireChannelRead(msg);
		((Channel) msg).pipeline().addFirst(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel channel) {
				try {
					channel.eventLoop().submit(() -> {
						try {
							ChannelHandler interceptor = channel.pipeline().get(ChannelAbstract.KEY_HANDSHAKE);
							// Inject our packet interceptor
							if (interceptor == null) {
								interceptor = new ChannelHandlerHandshakeReceive(channel);
								channel.pipeline().addBefore("packet_handler", ChannelAbstract.KEY_HANDSHAKE, interceptor);
							}
							return interceptor;
						} catch (IllegalArgumentException e) {
							// Try again
							return channel.pipeline().get(ChannelAbstract.KEY_HANDSHAKE);
						}
					});
				} catch (Exception e) {
					packetManager.getPlugin().getLogger().log(Level.SEVERE, "Cannot inject incoming channel " + channel, e);
				}
			}
		});
	}
	
	public class ChannelHandlerHandshakeReceive extends ChannelInboundHandlerAdapter {
		
		private Channel channel;
		
		public ChannelHandlerHandshakeReceive(Channel channel) {
			this.channel = channel;
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object packet) {
			try {
				PacketType packetType = PacketType.getType(packet.getClass().getSimpleName());
				if(packetType != null && packetType == PacketType.Handshake.IS_SET_PROTOCOL) {
					packetManager.protocolVersionPerChannel.put(channel, new PacketContent(packet).getIntegers().readSafely(0, 0));
				}
				super.channelRead(ctx, packet);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}
