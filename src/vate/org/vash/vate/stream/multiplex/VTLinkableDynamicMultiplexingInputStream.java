package org.vash.vate.stream.multiplex;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.vash.vate.VT;
import org.vash.vate.security.VTSplitMix64Random;
import org.vash.vate.stream.array.VTByteArrayInputStream;
import org.vash.vate.stream.array.VTByteArrayOutputStream;
import org.vash.vate.stream.compress.VTCompressorSelector;
import org.vash.vate.stream.compress.VTPacketDecompressor;
import org.vash.vate.stream.endian.VTLittleEndianInputStream;
import org.vash.vate.stream.pipe.VTPipedInputStream;
import org.vash.vate.stream.pipe.VTPipedOutputStream;

public final class VTLinkableDynamicMultiplexingInputStream
{
//  private int type;
//  private int channel;
//  private int length;
//  private long sequence;
  private volatile boolean closed = false;
  private final int bufferSize;
  //private final byte[] packetHeader;
  private final byte[] packetDataBuffer;
  //private OutputStream out;
  private final Thread packetReaderThread;
  // private byte[] compressedBuffer = new byte[VT.VT_IO_BUFFFER_SIZE];
  private final VTLittleEndianInputStream lin;
  //private final VTLittleEndianInputStream hin;
  //private final VTByteArrayInputStream packetHeaderBuffer;
  private final VTLinkableDynamicMultiplexingInputStreamPacketReader packetReader;
  private final Map<Integer, VTLinkableDynamicMultiplexedInputStream> bufferedChannels;
  private final Map<Integer, VTLinkableDynamicMultiplexedInputStream> directChannels;
  private final SecureRandom packetSeed;
  //private final Random packetSequencer;
//  private final VTPipedOutputStream pout;
//  private final VTPipedInputStream pin;
//  private final VTLittleEndianInputStream lpin;
//  private final VTStreamRedirector dataReader;
//  private final Thread dataReaderThread;

  //private final SecureRandom packetSequencer;
  
  public VTLinkableDynamicMultiplexingInputStream(final InputStream in, final int packetSize, final int bufferSize, final boolean startPacketReader, final SecureRandom packetSeed)
  {
    this.packetSeed = packetSeed;
    //this.packetSequencer = new VTSplitMix64Random(packetSequence.nextLong());
    //this.packetSequencer = new VTMiddleSquareWeylSequenceDigestRandom(packetSeed);
    this.bufferSize = bufferSize;
    this.packetDataBuffer = new byte[packetSize * 2];
    //this.packetHeaderBuffer = new VTByteArrayInputStream(new byte[VT.VT_PACKET_HEADER_SIZE_BYTES]);
    this.lin = new VTLittleEndianInputStream(new BufferedInputStream(in, VT.VT_CONNECTION_PACKET_BUFFER_SIZE_BYTES));
    //this.hin = new VTLittleEndianInputStream(packetHeaderBuffer);
//    this.bufferedChannels = Collections.synchronizedMap(new LinkedHashMap<Integer, VTLinkableDynamicMultiplexedInputStream>());
//    this.directChannels = Collections.synchronizedMap(new LinkedHashMap<Integer, VTLinkableDynamicMultiplexedInputStream>());
    this.bufferedChannels = new LinkedHashMap<Integer, VTLinkableDynamicMultiplexedInputStream>();
    this.directChannels = new LinkedHashMap<Integer, VTLinkableDynamicMultiplexedInputStream>();
    this.packetReader = new VTLinkableDynamicMultiplexingInputStreamPacketReader(this);
    this.packetReaderThread = new Thread(null, packetReader, packetReader.getClass().getSimpleName());
    this.packetReaderThread.setDaemon(true);
    //this.packetReaderThread.setPriority((Thread.MAX_PRIORITY));
//    this.pin = new VTPipedInputStream(VT.VT_CONNECTION_PACKET_BUFFER_SIZE_BYTES);
//    this.pout = new VTPipedOutputStream();
//    this.lpin = new VTLittleEndianInputStream(pin);
//    this.dataReader = new VTStreamRedirector(in, pout, pout, VT.VT_CONNECTION_PACKET_BUFFER_SIZE_BYTES);
//    this.dataReaderThread = new Thread(null, dataReader, dataReader.getClass().getSimpleName());
//    this.dataReaderThread.setDaemon(true);
    //this.dataReaderThread.setPriority((Thread.MAX_PRIORITY));
    if (startPacketReader)
    {
//      try
//      {
//        pout.connect(pin);
//      }
//      catch (Throwable e)
//      {
//        
//      }
//      this.dataReaderThread.start();
      this.packetReaderThread.start();
    }
  }
  
//  public synchronized final VTLinkableDynamicMultiplexedInputStream linkInputStream(int type, int number)
//  {
//    VTLinkableDynamicMultiplexedInputStream stream = null;
//    stream = getInputStream(type, number);
//    return stream;
//  }
  
  public synchronized final VTLinkableDynamicMultiplexedInputStream linkInputStream(final int type, final Object link)
  {
    VTLinkableDynamicMultiplexedInputStream stream = null;
    if (link == null)
    {
      return stream;
    }
    if (link instanceof Integer)
    {
      stream = getInputStream(type, (Integer) link);
      if (stream != null && stream.getLink() == null)
      {
        stream.setLink(link);
      }
      return stream;
    }
    // search for a multiplexed outputstream that has no link
    stream = getInputStream(type);
    if (stream != null && stream.getLink() == null)
    {
      stream.setLink(link);
    }
    return stream;
  }
  
  public synchronized final VTLinkableDynamicMultiplexedInputStream linkInputStream(final int type, final int number, final Object link)
  {
    VTLinkableDynamicMultiplexedInputStream stream = null;
    if (link == null)
    {
      return stream;
    }
    stream = getInputStream(type, number);
    if (stream != null)
    {
      stream.setLink(link);
    }
    return stream;
  }
  
  public synchronized final void releaseInputStream(final VTLinkableDynamicMultiplexedInputStream stream)
  {
    if (stream != null)
    {
      stream.setLink(null);
//      if ((stream.type & VT.VT_MULTIPLEXED_CHANNEL_TYPE_PIPE_DIRECT) == VT.VT_MULTIPLEXED_CHANNEL_TYPE_PIPE_BUFFERED)
//      {
//        bufferedChannels.remove(stream.number());
//      }
//      else
//      {
//        directChannels.remove(stream.number());
//      }
    }
  }
  
  private synchronized final VTLinkableDynamicMultiplexedInputStream getInputStream(final int type, final int number)
  {
    VTLinkableDynamicMultiplexedInputStream stream = null;
    Map<Integer, VTLinkableDynamicMultiplexedInputStream> channelMap;
    if ((type & VT.VT_MULTIPLEXED_CHANNEL_TYPE_PIPE_DIRECT) == VT.VT_MULTIPLEXED_CHANNEL_TYPE_PIPE_BUFFERED)
    {
      channelMap = bufferedChannels;
    }
    else
    {
      channelMap = directChannels;
    }
    stream = channelMap.get(number);
    if (stream != null)
    {
      stream.type(type);
      return stream;
    }
    stream = new VTLinkableDynamicMultiplexedInputStream(type, number, bufferSize, packetSeed);
    channelMap.put(number, stream);
    return stream;
  }
  
  private synchronized final VTLinkableDynamicMultiplexedInputStream getInputStream(final int type)
  {
    VTLinkableDynamicMultiplexedInputStream stream = null;
    Map<Integer, VTLinkableDynamicMultiplexedInputStream> channelMap;
    if ((type & VT.VT_MULTIPLEXED_CHANNEL_TYPE_PIPE_DIRECT) == VT.VT_MULTIPLEXED_CHANNEL_TYPE_PIPE_BUFFERED)
    {
      channelMap = bufferedChannels;
    }
    else
    {
      channelMap = directChannels;
    }
    for (int number = 0; number < 16777216; number++)
    {
      stream = channelMap.get(number);
      if (stream != null && stream.getLink() == null)
      {
        stream.type(type);
        return stream;
      }
      else if (stream == null)
      {
        stream = new VTLinkableDynamicMultiplexedInputStream(type, number, bufferSize, packetSeed);
        channelMap.put(number, stream); 
        return stream;
      }
    }
    return stream;
  }
  
  public final void startPacketReader()
  {
    if (!packetReaderThread.isAlive())
    {
//      try
//      {
//        pout.connect(pin);
//      }
//      catch (Throwable e)
//      {
//        
//      }
//      dataReaderThread.start();
      packetReaderThread.start();
    }
  }
  
  public final boolean isPacketReaderStarted()
  {
    if (packetReaderThread != null)
    {
      return packetReaderThread.isAlive();
    }
    return false;
  }
  
  public final void stopPacketReader() throws IOException, InterruptedException
  {
    close();
    packetReaderThread.join();
  }
  
  private final void open(final int type, final int number) throws IOException
  {
    getInputStream(type, number).open();
  }
  
  public final void close(final int type, final int number) throws IOException
  {
    getInputStream(type, number).close();
  }
  
  public final void close() throws IOException
  {
    if (closed)
    {
      return;
    }
    closed = true;
    lin.close();
    //packetReader.setRunning(false);
    //synchronized (pipedChannels)
    //{
      for (VTLinkableDynamicMultiplexedInputStream stream : bufferedChannels.values().toArray(new VTLinkableDynamicMultiplexedInputStream []{ }))
      {
        try
        {
          stream.close();
        }
        catch (Throwable e)
        {
          // e.printStackTrace();
        }
      }
    //}
    //synchronized (directChannels)
    //{
      for (VTLinkableDynamicMultiplexedInputStream stream : directChannels.values().toArray(new VTLinkableDynamicMultiplexedInputStream []{ }))
      {
        try
        {
          stream.close();
        }
        catch (Throwable e)
        {
          // e.printStackTrace();
        }
      }
    //}
    bufferedChannels.clear();
    directChannels.clear();
  }
  
//  public final long nextPacketSequencerLong()
//  {
//    return packetSequencer.nextLong();
//  }
  
  // critical method, handle with care
  private final void readPackets() throws IOException
  {
    VTLinkableDynamicMultiplexedInputStream stream;
    //long next;
    long sequence;
    int type; 
    int channel;
    int length;
    
    while (!closed)
    {
      //next = nextPacketSequencerLong();
      sequence = lin.readLong();
      type = lin.readByte();
      channel = lin.readSubInt();
      length = lin.readInt();
      stream = getInputStream(type, channel);
      if (stream == null || stream.getPacketSequencer().nextLong() != sequence)
      {
        close();
        return;
      }
      if (length > 0)
      {
        OutputStream out = stream.getOutputStream();
        lin.readFully(packetDataBuffer, 0, length);
        try
        {
          out.write(packetDataBuffer, 0, length);
          out.flush();
        }
        catch (Throwable e)
        {
          //e.printStackTrace();
        }
      }
      else if (length == -2)
      {
        close(type, channel);
      }
      else if (length == -3)
      {
        open(type, channel);
      }
      else
      {
        close();
        return;
      }
    }
  }
  
  public final class VTLinkableDynamicMultiplexedInputStream extends InputStream
  {
    private volatile boolean closed;
    private volatile Object link = null;
    private final int number;
    private final long seed;
    private volatile int type;
    private final VTPipedInputStream bufferedInputStream;
    private final VTPipedOutputStream bufferedOutputStream;
    private InputStream input;
    private OutputStream directOutputStream;
    private Closeable directCloseable;
    private VTByteArrayOutputStream compressedPacketOutputPipe;
    private VTByteArrayInputStream compressedPacketInputPipe;
    private InputStream compressedInputStream;
    private final List<Closeable> propagated;
    private final Random packetSequencer;
    
    private VTLinkableDynamicMultiplexedInputStream(final int type, final int number, final int bufferSize, final SecureRandom packetSeed)
    {
      this.seed = packetSeed.nextLong();
      this.packetSequencer = new VTSplitMix64Random(seed);
      this.type = type;
      this.number = number;
      this.propagated = new ArrayList<Closeable>();
      if ((type & VT.VT_MULTIPLEXED_CHANNEL_TYPE_PIPE_DIRECT) == VT.VT_MULTIPLEXED_CHANNEL_TYPE_PIPE_BUFFERED)
      {
        this.bufferedInputStream = new VTPipedInputStream(bufferSize);
        this.bufferedOutputStream = new VTPipedOutputStream();
        try
        {
          this.bufferedInputStream.connect(this.bufferedOutputStream);
        }
        catch (IOException e)
        {
          
        }
        if ((type & VT.VT_MULTIPLEXED_CHANNEL_TYPE_COMPRESSION_ENABLED) == 0)
        {
          this.input = bufferedInputStream;
        }
        else
        {
          if ((type & VT.VT_MULTIPLEXED_CHANNEL_TYPE_COMPRESSION_MODE_HEAVY) != 0)
          {
            //this.compressedInputStream = VTCompressorSelector.createDirectZlibInputStream(bufferedInputStream);
            this.compressedInputStream = VTCompressorSelector.createDirectZstdInputStream(bufferedInputStream);
          }
          else
          {
            this.compressedInputStream = VTCompressorSelector.createDirectLz4InputStream(bufferedInputStream);
          }
          this.input = compressedInputStream;
        }
      }
      else
      {
        this.bufferedInputStream = null;
        this.bufferedOutputStream = null;
      }
    }
    
    public final int number()
    {
      return number;
    }
    
    public final int type()
    {
      return type;
    }
    
    public final void type(final int type)
    {
      this.type = type;
    }
    
    public final Object getLink()
    {
      return link;
    }
    
    public final void setLink(final Object link)
    {
      this.link = link;
    }
    
    public final boolean closed()
    {
      return closed;
      //return pipedInputStream == null || pipedInputStream.isClosed() || pipedInputStream.isEof();
    }
    
    private final OutputStream getOutputStream()
    {
      if (bufferedOutputStream != null)
      {
        return bufferedOutputStream;
      }
      return directOutputStream;
    }
    
    public final void setOutputStream(final OutputStream outputStream, final Closeable closeable)
    {
      this.directCloseable = closeable;
      if ((type & VT.VT_MULTIPLEXED_CHANNEL_TYPE_COMPRESSION_ENABLED) == 0)
      {
        this.directOutputStream = outputStream;
      }
      else
      {
        if (compressedPacketOutputPipe == null || compressedPacketInputPipe == null)
        {
          compressedPacketOutputPipe = new VTByteArrayOutputStream(packetDataBuffer.length);
          compressedPacketInputPipe = new VTByteArrayInputStream(compressedPacketOutputPipe.buf());
        }
        if ((type & VT.VT_MULTIPLEXED_CHANNEL_TYPE_COMPRESSION_MODE_HEAVY) != 0)
        {
          compressedInputStream = VTCompressorSelector.createDirectZstdInputStream(compressedPacketInputPipe);
        }
        else
        {
          compressedInputStream = VTCompressorSelector.createDirectLz4InputStream(compressedPacketInputPipe);
        }
        this.directOutputStream = new VTPacketDecompressor(outputStream, compressedInputStream, compressedPacketOutputPipe, compressedPacketInputPipe);
      }
    }
    
    public final void addPropagated(final Closeable propagated)
    {
      this.propagated.add(propagated);
    }
    
    public final void removePropagated(final Closeable propagated)
    {
      this.propagated.remove(propagated);
    }
    
    private final void open() throws IOException
    {
      closed = false;
      if (bufferedInputStream != null)
      {
        bufferedInputStream.open();
        if ((type & VT.VT_MULTIPLEXED_CHANNEL_TYPE_COMPRESSION_ENABLED) != 0)
        {
          if ((type & VT.VT_MULTIPLEXED_CHANNEL_TYPE_COMPRESSION_MODE_HEAVY) != 0)
          {
            compressedInputStream = VTCompressorSelector.createDirectZstdInputStream(bufferedInputStream);
          }
          else
          {
            compressedInputStream = VTCompressorSelector.createDirectLz4InputStream(bufferedInputStream);
          }
          input = compressedInputStream;
        }
      }
      else
      {
        //work already done by setDirectOutputStream
      }
    }
    
    public final void close() throws IOException
    {
      closed = true;
      compressedInputStream = null;
      compressedPacketInputPipe = null;
      compressedPacketOutputPipe = null;
      if (bufferedOutputStream != null)
      {
        bufferedOutputStream.close();
      }
      else
      {
        if (directCloseable != null)
        {
          directCloseable.close();
          directCloseable = null;
        }
        if (directOutputStream != null)
        {
          //directOutputStream.close();
          directOutputStream = null;
        }
      }
      if (propagated.size() > 0)
      {
        // propagated.close();
        for (Closeable closeable : propagated.toArray(new Closeable[]{ }))
        {
          try
          {
            closeable.close();
          }
          catch (Throwable t)
          {
            
          }
        }
      }
      //propagated.clear();
    }
    
    public final int available() throws IOException
    {
      return input.available();
    }
    
    public final int read() throws IOException
    {
      return input.read();
    }
    
    public final int read(final byte[] data) throws IOException
    {
      return input.read(data);
    }
    
    public final int read(final byte[] data, final int offset, final int length) throws IOException
    {
      return input.read(data, offset, length);
    }
    
    public final long skip(final long count) throws IOException
    {
      return input.skip(count);
    }
    
    private final Random getPacketSequencer()
    {
      return packetSequencer;
    }
  }
  
  private final class VTLinkableDynamicMultiplexingInputStreamPacketReader implements Runnable
  {
    private final VTLinkableDynamicMultiplexingInputStream multiplexingInputStream;
    
    private VTLinkableDynamicMultiplexingInputStreamPacketReader(VTLinkableDynamicMultiplexingInputStream multiplexingInputStream)
    {
      this.multiplexingInputStream = multiplexingInputStream;
      //this.running = true;
    }
    
    public final void run()
    {
      try
      {
        multiplexingInputStream.readPackets();
      }
      catch (Throwable e)
      {
        //e.printStackTrace();
        //running = false;
      }
      try
      {
        multiplexingInputStream.close();
      }
      catch (Throwable e1)
      {
        // e1.printStackTrace();
      }
    }
  }
}