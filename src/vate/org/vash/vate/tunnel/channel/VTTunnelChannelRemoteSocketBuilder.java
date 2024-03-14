package org.vash.vate.tunnel.channel;

import java.io.IOException;
import java.net.Socket;

import org.vash.vate.socket.VTProxy;
import org.vash.vate.socket.VTProxy.VTProxyType;
import org.vash.vate.stream.multiplex.VTLinkableDynamicMultiplexingInputStream.VTLinkableDynamicMultiplexedInputStream;
import org.vash.vate.stream.multiplex.VTLinkableDynamicMultiplexingOutputStream.VTLinkableDynamicMultiplexedOutputStream;
import org.vash.vate.tunnel.session.VTTunnelPipedSocket;
import org.vash.vate.tunnel.session.VTTunnelSession;
import org.vash.vate.tunnel.session.VTTunnelSessionHandler;

public class VTTunnelChannelRemoteSocketBuilder
{
  private final VTTunnelChannel channel;
  private static final String SESSION_SEPARATOR = "\f";
  private static final char SESSION_MARK = '\b';
  private static final VTProxy PROXY_NONE = new VTProxy(VTProxy.VTProxyType.GLOBAL, "", 0, "", "");
  
  public VTTunnelChannelRemoteSocketBuilder(VTTunnelChannel channel)
  {
    this.channel = channel;
  }
  
  public String toString()
  {
    return channel.toString();
  }
  
  public boolean equals(Object other)
  {
    return this.toString().equals(other.toString());
  }
  
  public VTTunnelChannel getChannel()
  {
    return channel;
  }
  
//  public Socket connect(int channelType, String host, int port, VTProxy proxy) throws IOException
//  {
//    return connect(channelType, host, port, proxy.getProxyType(), proxy.getProxyHost(), proxy.getProxyPort(), proxy.getProxyUser(), proxy.getProxyPassword(), null);
//  }
  
  public Socket connect(String host, int port, VTProxy proxy) throws IOException
  {
    if (proxy == null)
    {
      return connect(host, port, PROXY_NONE.getProxyType(), PROXY_NONE.getProxyHost(), PROXY_NONE.getProxyPort(), PROXY_NONE.getProxyUser(), PROXY_NONE.getProxyPassword());
    }
    return connect(host, port, proxy.getProxyType(), proxy.getProxyHost(), proxy.getProxyPort(), proxy.getProxyUser(), proxy.getProxyPassword());
  }
  
  public Socket connect(String host, int port, VTProxyType proxyType, String proxyHost, int proxyPort, String proxyUser, String proxyPassword) throws IOException
  {
    VTTunnelSession session = null;
    VTTunnelSessionHandler handler = null;
    int channelType = channel.getChannelType();
    
    String proxyTypeLetter = "G";
    if (proxyType == VTProxyType.GLOBAL)
    {
      proxyTypeLetter = "G";
    }
    else if (proxyType == VTProxyType.DIRECT)
    {
      proxyTypeLetter = "D";
    }
    else if (proxyType == VTProxyType.HTTP)
    {
      proxyTypeLetter = "H";
    }
    else if (proxyType == VTProxyType.SOCKS)
    {
      proxyTypeLetter = "S";
    }
    else if (proxyType == VTProxyType.ANY)
    {
      proxyTypeLetter = "A";
    }
    
    session = new VTTunnelSession(channel.getConnection(), true);
    VTTunnelPipedSocket pipedSocket = new VTTunnelPipedSocket(session);
    session.setSocket(pipedSocket);
    handler = new VTTunnelSessionHandler(session, channel);
    
    VTLinkableDynamicMultiplexedOutputStream output = channel.getConnection().getOutputStream(channelType, handler);
    VTLinkableDynamicMultiplexedInputStream input = channel.getConnection().getInputStream(channelType, handler);
    
    if (output != null && input != null)
    {
      final int outputNumber = output.number();
      final int inputNumber = input.number();
      
      pipedSocket.setOutputStream(output);
      session.setSocketInputStream(pipedSocket.getInputStream());
      session.setSocketOutputStream(pipedSocket.getOutputStream());
      
      session.setTunnelOutputStream(output);
      session.setTunnelInputStream(input);
      session.getTunnelOutputStream().open();
      session.getTunnelInputStream().setOutputStream(pipedSocket.getInputStreamSource(), pipedSocket);
      
      if (proxyUser == null || proxyPassword == null || proxyUser.length() == 0 || proxyPassword.length() == 0)
      {
        proxyUser = "*";
        proxyPassword = "*" + SESSION_SEPARATOR + "*";
      }
      // request message sent
      channel.getConnection().getControlOutputStream().writeData(("U" + SESSION_MARK + "T" + channelType + SESSION_SEPARATOR + inputNumber + SESSION_SEPARATOR + outputNumber + SESSION_SEPARATOR + host + SESSION_SEPARATOR + port + SESSION_SEPARATOR + proxyTypeLetter + SESSION_SEPARATOR + proxyHost + SESSION_SEPARATOR + proxyPort + SESSION_SEPARATOR + proxyUser + SESSION_SEPARATOR + proxyPassword).getBytes("UTF-8"));
      channel.getConnection().getControlOutputStream().flush();
      //System.out.println("sent.request:output=" + outputNumber);
      boolean result = false;
      try
      {
        result = session.waitResult();
      }
      catch (Throwable t)
      {
        //t.printStackTrace();
      }
      if (result)
      {
        return pipedSocket;
      }
    }
    else
    {
      // cannot handle more sessions
    }
    if (session != null)
    {
      pipedSocket.close();
    }
    throw new IOException("Failed to connect remotely to: host " + host + " port " + port + "");
  }
}