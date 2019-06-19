package priv.common;

import priv.Client.bean.HostAndPort;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

import java.util.Objects;

public class Message implements ReferenceCounted {
    private short operationCode;
    private byte[] destination;
    //没啥用，解析包不用这个
    private int contentLength;
    private ByteBuf content;

    @Deprecated
    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public Message(short operationCode, HostAndPort hostAndPort, ByteBuf content) throws Exception{
        this.operationCode=operationCode;
        this.content= Objects.requireNonNull(content);
        if (hostAndPort==null){
            this.destination=null;
        }else {
            this.destination = getDestinationBytes(hostAndPort);
        }
    }

    public Message(short operationCode, byte[] destination, ByteBuf content){
        this.operationCode=operationCode;
        this.content=Objects.requireNonNull(content);
        this.destination=destination;
    }

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

    public Message() {

    }

    private byte[] getDestinationBytes(HostAndPort hostAndPort) throws Exception {
        byte[] host = hostAndPort.getHost().getAddress();
        int iport = hostAndPort.getPort();
        byte[] port = new byte[]{
//                (byte) ((iport >> 24) & 0xFF),
//                (byte) ((iport >> 16) & 0xFF),
                (byte) ((iport >> 8) & 0xFF),
                (byte) (iport & 0xFF)
        };
        return new byte[]{
                host[0],
                host[1],
                host[2],
                host[3],
                port[0],
                port[1]
        };
    }

    public int getOperationCode() {
        return operationCode;
    }

    public byte[] getDestination() {
        return destination;
    }

    public int getContentLength() {
        return contentLength;
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
        content.retain();
        return this;
    }

    @Override
    public ReferenceCounted retain(int increment) {
        content.retain(increment);
        return this;
    }

    @Override
    public ReferenceCounted touch() {
         content.touch();
         return this;
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        content.touch(hint);
        return this;
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
