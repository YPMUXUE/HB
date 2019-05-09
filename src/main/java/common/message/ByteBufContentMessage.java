package common.message;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;


public interface ByteBufContentMessage extends ContentMessage<ByteBuf> , ReferenceCounted {

}
