package org.vash.vate.tunnel.connection;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.vash.vate.socket.proxy.VTProxy;
import org.vash.vate.socket.proxy.VTProxy.VTProxyType;
import org.vash.vate.stream.multiplex.VTLinkableDynamicMultiplexingInputStream.VTLinkableDynamicMultiplexedInputStream;
import org.vash.vate.stream.multiplex.VTLinkableDynamicMultiplexingOutputStream.VTLinkableDynamicMultiplexedOutputStream;
import org.vash.vate.tunnel.channel.VTTunnelChannel;
import org.vash.vate.tunnel.session.VTTunnelCloseableServerSocket;
import org.vash.vate.tunnel.session.VTTunnelCloseableSocket;
import org.vash.vate.tunnel.session.VTTunnelDatagramSocket;
import org.vash.vate.tunnel.session.VTTunnelPipedSocket;
import org.vash.vate.tunnel.session.VTTunnelRunnableSessionHandler;
import org.vash.vate.tunnel.session.VTTunnelSession;
import org.vash.vate.tunnel.session.VTTunnelSessionHandler;
import org.vash.vate.tunnel.session.VTTunnelSocksSessionHandler;

public class VTTunnelConnectionControlThread implements Runnable
{
  private final VTTunnelConnection connection;
  private volatile boolean closed = false;
  private static final String SESSION_SEPARATOR = "\f";
  private static final char SESSION_MARK = '\b';
  private final byte[] packet = new byte[8192];
  
  public VTTunnelConnectionControlThread(VTTunnelConnection connection)
  {
    this.connection = connection;
  }
  
  public void run()
  {
    try
    {
      while (!closed)
      {
        final int packetLength = connection.getControlInputStream().readData(packet);
        if (packet[0] == 'U')
        {
          if (packet[1] == SESSION_MARK)
          {
            //final char tunnelChar = (char) packet[2];
            final char tunnelType = (char) packet[2];
            
            String text = new String(packet, 3, packetLength - 3, "UTF-8");
            String[] parts = text.split(SESSION_SEPARATOR);
            if (parts.length >= 8)
            {
              // request message received
              final int channelType = Integer.parseInt(parts[0]);
              final int outputNumber = Integer.parseInt(parts[1]);
              final int inputNumber = Integer.parseInt(parts[2]);
              final int connectTimeout = Integer.parseInt(parts[3]);
              final int dataTimeout = Integer.parseInt(parts[4]);
              
              if (tunnelType == VTTunnelChannel.TUNNEL_TYPE_TCP)
              {
                final String bind = parts[5];
                final String host = parts[6];
                final int port = Integer.parseInt(parts[7]);
                String proxyTypeLetter = parts[8];
                String proxyHost = parts[9];
                int proxyPort = Integer.parseInt(parts[10]);
                String proxyUser = parts[11];
                String proxyPassword = parts[12];
                
                if (parts.length > 13 && proxyUser.equals("*") && proxyPassword.equals("*") && parts[13].equals("*"))
                {
                  proxyUser = null;
                  proxyPassword = null;
                }
                
                VTProxyType proxyType = VTProxyType.GLOBAL;
                if (proxyTypeLetter.toUpperCase().startsWith("G"))
                {
                  proxyType = VTProxyType.GLOBAL;
                }
                else if (proxyTypeLetter.toUpperCase().startsWith("D"))
                {
                  proxyType = VTProxyType.DIRECT;
                }
                else if (proxyTypeLetter.toUpperCase().startsWith("H"))
                {
                  proxyType = VTProxyType.HTTP;
                }
                else if (proxyTypeLetter.toUpperCase().startsWith("S"))
                {
                  proxyType = VTProxyType.SOCKS;
                }
                else if (proxyTypeLetter.toUpperCase().startsWith("P"))
                {
                  proxyType = VTProxyType.PLUS;
                }
                final VTProxy proxy = new VTProxy(proxyType, proxyHost, proxyPort, proxyUser, proxyPassword);
                
                final boolean connect = proxyTypeLetter.toUpperCase().startsWith("A") ? false : true;
                
                final VTTunnelSession session = new VTTunnelSession(connection, false);
                final VTTunnelSessionHandler handler = new VTTunnelSessionHandler(session, connection.getResponseChannel());
                
                VTLinkableDynamicMultiplexedInputStream input = connection.getInputStream(channelType, inputNumber, handler);
                VTLinkableDynamicMultiplexedOutputStream output = connection.getOutputStream(channelType, outputNumber, handler);
                
                if (output != null && input != null)
                {
                  session.setTunnelInputStream(input);
                  session.setTunnelOutputStream(output);
                  
                  Runnable tcpTunnelThread = new Runnable()
                  {
                    public void run()
                    {
                      Socket remoteSocket = null;
                      InputStream socketInputStream = null;
                      OutputStream socketOutputStream = null;
                      
                      try
                      {
                        if (connect)
                        {
                          remoteSocket = connect(bind, host, port, connectTimeout, dataTimeout, proxy);
                        }
                        else
                        {
                          remoteSocket = accept(host, port, connectTimeout, dataTimeout);
                        }
                        socketInputStream = remoteSocket.getInputStream();
                        socketOutputStream = remoteSocket.getOutputStream();
                      }
                      catch (Throwable t)
                      {
                        
                      }
                      
                      try
                      {
                        if (socketInputStream != null && socketOutputStream != null)
                        {
                          session.setSocket(remoteSocket);
                          session.setSocketInputStream(socketInputStream);
                          session.setSocketOutputStream(socketOutputStream);
                          
                          session.getTunnelOutputStream().open();
                          //session.getTunnelInputStream().open();
                          session.getTunnelInputStream().setOutputStream(session.getSocketOutputStream(), new VTTunnelCloseableSocket(session.getSocket()));
                          // response message sent with ok
                          connection.getControlOutputStream().writeData(("U" + SESSION_MARK + tunnelType + channelType + SESSION_SEPARATOR + inputNumber + SESSION_SEPARATOR + outputNumber).getBytes("UTF-8"));
                          connection.getControlOutputStream().flush();
                          connection.getExecutorService().execute(handler);
                          session.setResult(true);
                        }
                        else
                        {
                          if (session != null)
                          {
                            session.close();
                          }
                          // response message sent with error
                          connection.getControlOutputStream().writeData(("U" + SESSION_MARK + tunnelType + channelType + SESSION_SEPARATOR + inputNumber + SESSION_SEPARATOR + "-1").getBytes("UTF-8"));
                          connection.getControlOutputStream().flush();
                        }
                      }
                      catch (Throwable t)
                      {
                        //t.printStackTrace();
                      }
                    }
                  };
                  connection.getExecutorService().execute(tcpTunnelThread);
                }
                else
                {
                  if (session != null)
                  {
                    session.close();
                  }
                  // response message sent with error
                  connection.getControlOutputStream().writeData(("U" + SESSION_MARK + tunnelType + channelType + SESSION_SEPARATOR + inputNumber + SESSION_SEPARATOR + "-1").getBytes("UTF-8"));
                  connection.getControlOutputStream().flush();
                }
              }
              else if (tunnelType == VTTunnelChannel.TUNNEL_TYPE_SOCKS)
              {
                String bind = parts[5];
                String socksUsername = parts[6];
                String socksPassword = parts[7];
                String proxyTypeLetter = parts[8];
                String proxyHost = parts[9];
                int proxyPort = Integer.parseInt(parts[10]);
                String proxyUser = parts[11];
                String proxyPassword = parts[12];
                
                if (parts.length > 13 && proxyUser.equals("*") && proxyPassword.equals("*") && parts[13].equals("*"))
                {
                  proxyUser = null;
                  proxyPassword = null;
                }
                
                VTProxyType proxyType = VTProxyType.GLOBAL;
                if (proxyTypeLetter.toUpperCase().startsWith("G"))
                {
                  proxyType = VTProxyType.GLOBAL;
                }
                else if (proxyTypeLetter.toUpperCase().startsWith("D"))
                {
                  proxyType = VTProxyType.DIRECT;
                }
                else if (proxyTypeLetter.toUpperCase().startsWith("H"))
                {
                  proxyType = VTProxyType.HTTP;
                }
                else if (proxyTypeLetter.toUpperCase().startsWith("S"))
                {
                  proxyType = VTProxyType.SOCKS;
                }
                else if (proxyTypeLetter.toUpperCase().startsWith("P"))
                {
                  proxyType = VTProxyType.PLUS;
                }
                VTProxy proxy = new VTProxy(proxyType, proxyHost, proxyPort, proxyUser, proxyPassword);
                
                VTTunnelSession session = new VTTunnelSession(connection, false);
                VTTunnelPipedSocket pipedSocket = new VTTunnelPipedSocket(null);
                session.setSocket(pipedSocket);
                VTTunnelSocksSessionHandler handler = new VTTunnelSocksSessionHandler(session, connection.getResponseChannel(), socksUsername, socksPassword, proxy, null, connectTimeout, bind);
                
                VTLinkableDynamicMultiplexedInputStream input = connection.getInputStream(channelType, inputNumber, handler);
                VTLinkableDynamicMultiplexedOutputStream output = connection.getOutputStream(channelType, outputNumber, handler);
                
                if (output != null && input != null)
                {
                  pipedSocket.setOutputStream(output);
                  session.setSocketInputStream(pipedSocket.getInputStream());
                  session.setSocketOutputStream(pipedSocket.getOutputStream());
                  
                  input.setOutputStream(pipedSocket.getInputStreamSource(), pipedSocket);
                  output.open();
                  
                  session.setTunnelInputStream(input);
                  session.setTunnelOutputStream(output);
                  // response message sent with ok
                  connection.getControlOutputStream().writeData(("U" + SESSION_MARK + tunnelType + channelType + SESSION_SEPARATOR + inputNumber + SESSION_SEPARATOR + outputNumber).getBytes("UTF-8"));
                  connection.getControlOutputStream().flush();
                  connection.getExecutorService().execute(handler);
                  session.setResult(true);
                }
                else
                {
                  if (session != null)
                  {
                    session.close();
                  }
                  // response message sent with error
                  connection.getControlOutputStream().writeData(("U" + SESSION_MARK + tunnelType + channelType + SESSION_SEPARATOR + inputNumber + SESSION_SEPARATOR + "-1").getBytes("UTF-8"));
                  connection.getControlOutputStream().flush();
                }
              }
              else if (tunnelType == VTTunnelChannel.TUNNEL_TYPE_UDP)
              {
                //final String bind = parts[5];
                final String host = parts[6];
                final int port = Integer.parseInt(parts[7]);
                
                VTTunnelSession session = new VTTunnelSession(connection, false);
                VTTunnelPipedSocket pipedSocket = new VTTunnelPipedSocket(null);
                session.setSocket(pipedSocket);
                VTTunnelDatagramSocket datagramSocket = null;
                
                try
                {
                  if (host != null && host.length() > 0)
                  {
                    datagramSocket = new VTTunnelDatagramSocket(pipedSocket, connection.getExecutorService(), host, port);
                  }
                  else
                  {
                    datagramSocket = new VTTunnelDatagramSocket(pipedSocket, connection.getExecutorService());
                  }
                }
                catch (Throwable t)
                {
                  
                }
                
                if (datagramSocket != null)
                {
                  datagramSocket.setSoTimeout(dataTimeout);
                  VTTunnelRunnableSessionHandler handler = new VTTunnelRunnableSessionHandler(session, connection.getResponseChannel(), datagramSocket);
                  
                  VTLinkableDynamicMultiplexedInputStream input = connection.getInputStream(channelType, inputNumber, handler);
                  VTLinkableDynamicMultiplexedOutputStream output = connection.getOutputStream(channelType, outputNumber, handler);
                  
                  if (output != null && input != null)
                  {
                    pipedSocket.setOutputStream(output);
                    session.setSocketInputStream(pipedSocket.getInputStream());
                    session.setSocketOutputStream(pipedSocket.getOutputStream());
                    
                    input.setOutputStream(pipedSocket.getInputStreamSource(), pipedSocket);
                    output.open();
                    
                    datagramSocket.setTunnelInputStream(pipedSocket.getInputStream());
                    datagramSocket.setTunnelOutputStream(pipedSocket.getOutputStream());
                    
                    session.setTunnelInputStream(input);
                    session.setTunnelOutputStream(output);
                    
                    int localPort = datagramSocket.getLocalPort();
                    InetAddress localAddress = datagramSocket.getLocalAddress();
                    
                    if (localAddress.getHostAddress().equals("0.0.0.0") || localAddress.getHostAddress().equals("::")
                    || localAddress.getHostAddress().equals("::0") || localAddress.getHostAddress().equals("0:0:0:0:0:0:0:0")
                    || localAddress.getHostAddress().equals("00:00:00:00:00:00:00:00")
                    || localAddress.getHostAddress().equals("0000:0000:0000:0000:0000:0000:0000:0000"))
                    {
                      localAddress = InetAddress.getLocalHost();
                    }
                    // response message sent with ok
                    connection.getControlOutputStream().writeData(("U" + SESSION_MARK + tunnelType + channelType + SESSION_SEPARATOR + inputNumber + SESSION_SEPARATOR + outputNumber + SESSION_SEPARATOR + localAddress.getHostAddress() + SESSION_SEPARATOR + localPort).getBytes("UTF-8"));
                    connection.getControlOutputStream().flush();
                    connection.getExecutorService().execute(handler);
                    session.setResult(true);
                  }
                  else
                  {
                    if (session != null)
                    {
                      session.close();
                    }
                    // response message sent with error
                    connection.getControlOutputStream().writeData(("U" + SESSION_MARK + tunnelType + channelType + SESSION_SEPARATOR + inputNumber + SESSION_SEPARATOR + "-1").getBytes("UTF-8"));
                    connection.getControlOutputStream().flush();
                  }
                }
                else
                {
                  if (session != null)
                  {
                    session.close();
                  }
                  // response message sent with error
                  connection.getControlOutputStream().writeData(("U" + SESSION_MARK + tunnelType + channelType + SESSION_SEPARATOR + inputNumber + SESSION_SEPARATOR + "-1").getBytes("UTF-8"));
                  connection.getControlOutputStream().flush();
                }
              }
              else
              {
                //closed = true;
              }
            }
            else if (parts.length >= 3)
            {
              // response message received
              final int channelType = Integer.parseInt(parts[0]);
              final int outputNumber = Integer.parseInt(parts[1]);
              final int inputNumber = Integer.parseInt(parts[2]);
              
              if (inputNumber > -1)
              {
                VTTunnelSessionHandler handler = null;
                Object link = connection.getOutputStream(channelType, outputNumber).getLink();
                if (link instanceof VTTunnelSessionHandler)
                {
                  handler = (VTTunnelSessionHandler) link;
                }
                if (handler != null)
                {
                  VTTunnelSession session = handler.getSession();
                  if (parts.length >= 5)
                  {
                    session.setRemoteHost(parts[3]);
                    session.setRemotePort(Integer.parseInt(parts[4]));
                  }
                  if (session.isOriginator())
                  {
                    // response message received ok
                    Socket sessionSocket = session.getSocket();
                    if (!(sessionSocket instanceof VTTunnelPipedSocket))
                    {
                      connection.getExecutorService().execute(handler);
                    }
                    session.setResult(true);
                  }
                  else
                  {
                    // response message received ok
                  }
                }
                else
                {
                  // handler not found
                }
              }
              else
              {
                // response message received has error
                VTTunnelSessionHandler handler = null;
                Object link = connection.getOutputStream(channelType, outputNumber).getLink();
                if (link instanceof VTTunnelSessionHandler)
                {
                  handler = (VTTunnelSessionHandler) link;
                }
                if (handler != null)
                {
                  VTTunnelSession session = handler.getSession();
                  if (session != null)
                  {
                    session.close();
                  }
                }
              }
            }
            else
            {
              // message with unsupported parts
            }
          }
          else
          {
            // session mark not found
          }
        }
        else
        {
          // unable to handle
        }
      }
    }
    catch (Throwable e)
    {
      //e.printStackTrace();
      //return;
    }
    closed = true;
  }
  
  public Socket connect(String bind, String host, int port, int connectTimeout, int dataTimeout, VTProxy proxy)
  {
    VTTunnelCloseableSocket clientSocket = null;
    Socket socket = null;
    try
    {
      if (host == null || host.length() == 0 || host.equals("*"))
      {
        host = "";
      }
      if (bind == null || bind.length() == 0 || bind.equals("*"))
      {
        bind = "";
      }
      socket = VTProxy.next(null, bind, connectTimeout, proxy);
      clientSocket = new VTTunnelCloseableSocket(socket);
      connection.getCloseables().add(clientSocket);
      
      socket = VTProxy.connect(bind, host, port, connectTimeout, socket);
      if (dataTimeout > 0)
      {
        socket.setSoTimeout(dataTimeout);
      }
    }
    catch (Throwable t)
    {
      //t.printStackTrace();
      if (socket != null)
      {
        try
        {
          socket.close();
        }
        catch (Throwable e)
        {
          
        }
      }
      socket = null;
    }
    finally
    {
      if (clientSocket != null)
      {
        connection.getCloseables().remove(clientSocket);
      }
    }
    return socket;
  }
  
  public Socket accept(String host, int port, int connectTimeout, int dataTimeout)
  {
    VTTunnelCloseableServerSocket serverSocket = null;
    Socket socket = null;
    try
    {
      if (host == null || host.length() == 0 || host.equals("*"))
      {
        host = "";
      }
      else
      {
        
      }
      serverSocket = new VTTunnelCloseableServerSocket(new ServerSocket());
      serverSocket.bind(new InetSocketAddress(host, port));
      if (connectTimeout > 0)
      {
        serverSocket.setSoTimeout(connectTimeout);
      }
      else
      {
        serverSocket.setSoTimeout(0);
      }
      connection.getCloseables().add(serverSocket);
      
      socket = serverSocket.accept();
      if (dataTimeout > 0)
      {
        socket.setSoTimeout(dataTimeout);
      }
    }
    catch (Throwable t)
    {
      //t.printStackTrace();
      if (socket != null)
      {
        try
        {
          socket.close();
        }
        catch (Throwable e)
        {
          
        }
      }
    }
    finally
    {
      if (serverSocket != null)
      {
        try
        {
          serverSocket.close();
        }
        catch (Throwable t)
        {
          //t.printStackTrace();
        }
        connection.getCloseables().remove(serverSocket);
      }
    }
    return socket;
  }
}