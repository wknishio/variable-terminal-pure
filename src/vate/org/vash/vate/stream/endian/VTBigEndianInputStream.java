package org.vash.vate.stream.endian;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public final class VTBigEndianInputStream extends InputStream implements DataInput
{
  // private int readed;
  // private int total;
  // private int remaining;
  private byte[] shortBuffer;
  private byte[] ushortBuffer;
  private byte[] charBuffer;
  private byte[] subintBuffer;
  private byte[] intBuffer;
  private byte[] longBuffer;
  private InputStream in;
  
  public VTBigEndianInputStream(InputStream in)
  {
    this.in = in;
    this.shortBuffer = new byte[2];
    this.ushortBuffer = new byte[2];
    this.charBuffer = new byte[2];
    this.subintBuffer = new byte[3];
    this.intBuffer = new byte[4];
    this.longBuffer = new byte[8];
  }
  
  public final void setIntputStream(InputStream in)
  {
    this.in = in;
  }
  
  public final InputStream getInputStream()
  {
    return in;
  }
  
  public final int read() throws IOException
  {
    return in.read();
  }
  
  public final int read(byte[] b) throws IOException
  {
    return in.read(b);
  }
  
  public final int read(byte[] b, int off, int len) throws IOException
  {
    return in.read(b, off, len);
  }
  
  public final int available() throws IOException
  {
    return in.available();
  }
  
  public final long skip(long l) throws IOException
  {
    return in.skip(l);
  }
  
  public final void close() throws IOException
  {
    in.close();
  }
  
  public final boolean readBoolean() throws IOException
  {
    return in.read() != 0;
  }
  
  public final byte readByte() throws IOException
  {
    int b = in.read();
    if (b == -1)
    {
      throw new EOFException();
    }
    return (byte) b;
  }
  
  public final short readShort() throws IOException
  {
    readFully(shortBuffer);
    return (short) ((shortBuffer[0] & 0xFF) << 8 | (shortBuffer[1] & 0xFF));
  }
  
  public final int readUnsignedShort() throws IOException
  {
    readFully(ushortBuffer);
    return ((ushortBuffer[0] & 0xFF) << 8 | (ushortBuffer[1] & 0xFF));
  }
  
  public final char readChar() throws IOException
  {
    readFully(charBuffer);
    return (char) ((charBuffer[0] & 0xFF) << 8 | (charBuffer[1] & 0xFF));
  }
  
  public final int readSubInt() throws IOException
  {
    readFully(subintBuffer);
    return ((subintBuffer[0] & 0x80) << 24 | (subintBuffer[0] & 0xFF) << 16 | (subintBuffer[1] & 0xFF) << 8 | (subintBuffer[2] & 0xFF));
  }
  
  public final int readInt() throws IOException
  {
    readFully(intBuffer);
    // return 0 & 0xFFL;
    return ((intBuffer[0] & 0xFF) << 24 | (intBuffer[1] & 0xFF) << 16 | (intBuffer[2] & 0xFF) << 8 | (intBuffer[3] & 0xFF));
  }
  
  public final long readUnsignedInt() throws IOException
  {
    readFully(intBuffer);
    return ((intBuffer[0] & 0xFFL) << 24) | ((intBuffer[1] & 0xFFL) << 16) | ((intBuffer[2] & 0xFFL) << 8) | ((intBuffer[3] & 0xFFL));
  }
  
  public final long readLong() throws IOException
  {
    readFully(longBuffer);
    return ((longBuffer[0] & 0xFFL) << 56) | ((longBuffer[1] & 0xFFL) << 48) | ((longBuffer[2] & 0xFFL) << 40) | ((longBuffer[3] & 0xFFL) << 32) | ((longBuffer[4] & 0xFFL) << 24) | ((longBuffer[5] & 0xFFL) << 16) | ((longBuffer[6] & 0xFFL) << 8) | ((longBuffer[7] & 0xFFL));
  }
  
  public final float readFloat() throws IOException
  {
    return Float.intBitsToFloat(readInt());
  }
  
  public final double readDouble() throws IOException
  {
    return Double.longBitsToDouble(readLong());
  }
  
  public final void readFully(byte b[]) throws IOException
  {
    readFully(b, 0, b.length);
  }
  
  public final void readFully(byte[] buf, int offset, int len) throws IOException
  {
    if (len < 0)
    {
      throw new IndexOutOfBoundsException("Negative length: " + len);
    }
    while (len > 0)
    {
      int numread = in.read(buf, offset, len);
      if (numread < 0)
      {
        throw new EOFException();
      }
      len -= numread;
      offset += numread;
    }
  }
  
  public final int skipBytes(int n) throws IOException
  {
    return (int) in.skip(n);
  }
  
  public final int readUnsignedByte() throws IOException
  {
    int b = in.read();
    if (b == -1)
    {
      throw new EOFException();
    }
    return b;
  }
  
  public final byte[] readData() throws IOException
  {
    int size = this.readInt();
    byte[] data = new byte[size];
    readFully(data, 0, size);
    return data;
  }
  
  public final String readUTF() throws IOException
  {
    byte[] utf = readData();
    String data = new String(utf, "UTF-8");
    return data;
  }
  
  public final String readLine() throws IOException
  {
    return readUTF();
  }
}