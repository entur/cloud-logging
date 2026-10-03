package no.entur.logging.cloud.protobuf.json.codedoutputstream;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MessageLite;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Mirrors the abstract methods of {@linkplain CodedOutputStream} (and its superclass): a generated subclass of
 * CodedOutputStream forwards all calls to a sink.
 * <br><br>
 * Internal; must be public as the generated class is in the protobuf package.
 */

public interface CodedOutputStreamSink {
    void writeTag(int fieldNumber, int wireType) throws IOException;
    void writeInt32(int fieldNumber, int value) throws IOException;
    void writeUInt32(int fieldNumber, int value) throws IOException;
    void writeFixed32(int fieldNumber, int value) throws IOException;
    void writeUInt64(int fieldNumber, long value) throws IOException;
    void writeFixed64(int fieldNumber, long value) throws IOException;
    void writeBool(int fieldNumber, boolean value) throws IOException;
    void writeString(int fieldNumber, String value) throws IOException;
    void writeBytes(int fieldNumber, ByteString value) throws IOException;
    void writeByteArray(int fieldNumber, byte[] value) throws IOException;
    void writeByteArray(int fieldNumber, byte[] value, int offset, int length) throws IOException;
    void writeByteBuffer(int fieldNumber, ByteBuffer value) throws IOException;
    void writeRawBytes(ByteBuffer value) throws IOException;
    void writeMessage(int fieldNumber, MessageLite value) throws IOException;
    void writeMessageSetExtension(int fieldNumber, MessageLite value) throws IOException;
    void writeRawMessageSetExtension(int fieldNumber, ByteString value) throws IOException;
    void writeInt32NoTag(int value) throws IOException;
    void writeUInt32NoTag(int value) throws IOException;
    void writeFixed32NoTag(int value) throws IOException;
    void writeUInt64NoTag(long value) throws IOException;
    void writeFixed64NoTag(long value) throws IOException;
    void writeStringNoTag(String value) throws IOException;
    void writeBytesNoTag(ByteString value) throws IOException;
    void writeMessageNoTag(MessageLite value) throws IOException;
    void writeByteArrayNoTag(byte[] value, int offset, int length) throws IOException; // package-private in CodedOutputStream
    void write(byte value) throws IOException;
    void write(byte[] value, int offset, int length) throws IOException;
    void writeLazy(byte[] value, int offset, int length) throws IOException;
    void write(ByteBuffer value) throws IOException;
    void writeLazy(ByteBuffer value) throws IOException;
    void flush() throws IOException;
    int spaceLeft();
    int getTotalBytesWritten();
}
