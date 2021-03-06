package common;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

public class Message implements ReferenceCounted {
    private short operationCode;
    private byte[] destination;
    private int length;
    private ByteBuf content;

    public Message setOperationCode(short operationCode) {
        this.operationCode = operationCode;
        return this;
    }

    public Message setDestination(byte[] destination) {
        this.destination = destination;
        return this;
    }

    public Message setContent(ByteBuf content) {
        this.content = content;
        return this;
    }

    private Message() {

    }

    public static Message resloveRequest(ByteBuf msg){
        Message m=new Message();
        m.operationCode=msg.readShort();
        m.length=msg.readInt();
        m.destination=new byte[6];
        msg.readBytes(m.destination);
        m.content=msg;
        return m;
    }

    public int getOperationCode() {
        return operationCode;
    }

    public byte[] getDestination() {
        return destination;
    }

    public int getLength() {
        return length;
    }

    public ByteBuf getContent() {
        return content;
    }

    @Override
    public int refCnt() {
        return content.refCnt();
    }

    @Override
    public ReferenceCounted retain() {
        return content.retain();
    }

    @Override
    public ReferenceCounted retain(int increment) {
        return content.retain(increment);
    }

    @Override
    public ReferenceCounted touch() {
        return content.touch();
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return content.touch(hint);
    }

    @Override
    public boolean release() {
        return content.release();
    }

    @Override
    public boolean release(int decrement) {
        return content.release(decrement);
    }

}
