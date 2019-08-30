package priv.Client.handler2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.collections4.CollectionUtils;
import priv.common.message.frame.bind.BindV1Message;
import priv.common.message.frame.bind.BindV2Message;
import priv.common.message.frame.close.CloseMessage;
import priv.common.message.frame.connect.ConnectMessage;
import priv.common.message.frame.establish.ConnectionEstablishFailedMessage;
import priv.common.message.frame.establish.ConnectionEstablishMessage;

import java.util.LinkedList;
import java.util.Queue;

/**
 *  * @author  pyuan
 *  * @date    2019/8/30 0030
 *  * @Description
 *  *
 *  
 */
public class HttpProxyMessageHandler extends ChannelDuplexHandler {
	private int status = ST_NOT_BIND;
	private static final int ST_NOT_BIND = 0;
	private static final int ST_BINDING = 1;
	private static final int ST_BIND_FINISH = 2;
	private static final int ST_BIND_FAILED = 3;
	private Queue<PendingWriteItem> pendingData;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof ConnectionEstablishMessage){
			status = ST_BIND_FINISH;
			writePendingData(ctx);
		}else if(msg instanceof ConnectionEstablishFailedMessage){
			status = ST_BIND_FAILED;
			clearPendingData(new RuntimeException("bind Failed"));
		}else if (msg instanceof ConnectMessage) {
			ctx.fireChannelRead(((ConnectMessage) msg).getContent());
		}else if (msg instanceof CloseMessage) {
			ctx.channel().close();
		}else {
			ctx.fireChannelRead(msg);
		}
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof BindV2Message || msg instanceof BindV1Message){
			status = ST_BINDING;
			ctx.write(msg);
		}else{
			switch (status){
				case ST_NOT_BIND:
					ReferenceCountUtil.release(msg);
					throw new RuntimeException("channel not bind yet");
				case ST_BINDING:
					if (pendingData == null){
						pendingData = new LinkedList<>();
					}
					pendingData.add(new PendingWriteItem(packageWriteItem(msg),promise));
					break;
				case ST_BIND_FINISH:
					writePendingData(ctx);
					ctx.write(packageWriteItem(msg),promise);
					break;
				case ST_BIND_FAILED:
					Throwable e = new RuntimeException("bind Failed");
					clearPendingData(e);
					ReferenceCountUtil.release(msg);
					promise.tryFailure(e);
					break;
			}
		}
	}
	private void writePendingData(ChannelHandlerContext ctx){
		Queue<PendingWriteItem> pendingData = this.pendingData;
		if (CollectionUtils.isNotEmpty(pendingData)) {
			while (!pendingData.isEmpty()){
				PendingWriteItem item = pendingData.poll();
				if (item != null) {
					ctx.write(item.data, item.promise);
				}
			}
			ctx.flush();
		}
	}

	private void clearPendingData(Throwable e){
		while (CollectionUtils.isNotEmpty(this.pendingData)){
			PendingWriteItem item = pendingData.poll();
			if (item != null) {
				ReferenceCountUtil.release(item.data);
				item.promise.tryFailure(e);
			}
		}
	}
	private Object packageWriteItem(Object obj){
		if (obj instanceof ByteBuf){
			return new ConnectMessage((ByteBuf) obj);
		}else{
			return obj;
		}
	}

	private static class PendingWriteItem {
		private Object data;
		private ChannelPromise promise;

		private PendingWriteItem(Object data, ChannelPromise promise) {
			this.data = data;
			this.promise = promise;
		}
	}
}
