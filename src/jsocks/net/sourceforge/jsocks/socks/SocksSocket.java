/*******************************************************************************
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package net.sourceforge.jsocks.socks;

import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;

/**
 * SocksSocket tryies to look very similar to normal Socket,
 * while allowing connections through the SOCKS4 or 5 proxy.
 * To use this class you will have to identify proxy you need
 * to use, Proxy class allows you to set default proxy, which
 * will be used by all Socks aware sockets. You can also create
 * either Socks4Proxy or Socks5Proxy, and use them by passing to the 
 * appropriate constructors.
 * <P>
 * Using Socks package can be as easy as that:
 *
 * <pre><tt>
 *
 *     import Socks.*;
 *     ....
 *
 *     try{
 *        //Specify SOCKS5 proxy
 *        Proxy.setDefaultProxy("socks-proxy",1080);
 *
 *        //OR you still use SOCKS4
 *        //Code below uses SOCKS4 proxy
 *        //Proxy.setDefaultProxy("socks-proxy",1080,userName);
 *
 *        Socket s = SocksSocket("some.host.of.mine",13);
 *        readTimeFromSock(s);
 *     }catch(SocksException sock_ex){
 *        //Usually it will turn in more or less meaningfull message
 *        System.err.println("SocksException:"+sock_ex);
 *     }
 *
 * </tt></pre>
 *<P>
 * However if the need exist for more control, like resolving addresses
 * remotely, or using some non-trivial authentication schemes, it can be done.
 */

public class SocksSocket extends Socket{
   //Data members
   protected Proxy proxy;
   protected String localHost, remoteHost;
   protected InetAddress localIP, remoteIP;
   protected int localPort,remotePort;
   protected int connectTimeout;

   private Socket directSock = null;

   /**
    * Tryies to connect to given host and port
    * using default proxy. If no default proxy speciefied
    * it throws SocksException with error code SOCKS_NO_PROXY.
      @param host Machine to connect to.
      @param port Port to which to connect.
    * @see SocksSocket#SocksSocket(Proxy,String,int)
    * @see Socks5Proxy#resolveAddrLocally
    */
   public SocksSocket(String host,int port)
	  throws SocksException,UnknownHostException{
      this(Proxy.defaultProxy,host,port, 0);
   }
   /**
    * Connects to host port using given proxy server.
      @param p Proxy to use.
      @param host Machine to connect to.
      @param port Port to which to connect.
      @throws UnknownHostException 
      If one of the following happens:
      <ol>

      <li> Proxy settings say that address should be resolved locally, but
           this fails.
      <li> Proxy settings say that the host should be contacted directly but
           host name can't be resolved. 
      </ol>
      @throws SocksException
      If one of the following happens:
      <ul>
       <li> Proxy is is null.
       <li> Proxy settings say that the host should be contacted directly but
            this fails.
       <li> Socks Server can't be contacted.
       <li> Authentication fails.
       <li> Connection is not allowed by the SOCKS proxy.
       <li> SOCKS proxy can't establish the connection.
       <li> Any IO error occured.
       <li> Any protocol error occured.
      </ul>
      @throws IOexception if anything is wrong with I/O.
      @see Socks5Proxy#resolveAddrLocally
    */
   public SocksSocket(Proxy p,String host,int port, int timeout)
	  throws SocksException,UnknownHostException{
      if(p == null) throw new SocksException(Proxy.SOCKS_NO_PROXY);
      //proxy=p;
      proxy = p.copy();
      remoteHost = host;
      remotePort = port;
      connectTimeout = timeout;
      if(proxy.isDirect(host)){
         remoteIP = InetAddress.getByName(host);
         doDirect();
      }
      else
         processReply(proxy.connect(host,port,timeout));
   }

   public SocksSocket(String host,int port, int timeout,Proxy p)
       throws SocksException,UnknownHostException{
         if(p == null) throw new SocksException(Proxy.SOCKS_NO_PROXY);
         //proxy=p;
         proxy = p;
         remoteHost = host;
         remotePort = port;
         connectTimeout = timeout;
         if(proxy.isDirect(host)){
            remoteIP = InetAddress.getByName(host);
            doDirect();
         }
         else
            processReply(proxy.connect(host,port,timeout));
      }
   /**
    * Tryies to connect to given ip and port
    * using default proxy. If no default proxy speciefied
    * it throws SocksException with error code SOCKS_NO_PROXY.
      @param ip Machine to connect to.
      @param port Port to which to connect.
    * @see SocksSocket#SocksSocket(Proxy,String,int)
    */
   public SocksSocket(InetAddress ip, int port) throws SocksException{
      this(Proxy.defaultProxy,ip,port,0);
   }

   /**
      Connects to given ip and port using given Proxy server.
      @param p Proxy to use.
      @param ip Machine to connect to.
      @param port Port to which to connect.

    */
   public SocksSocket(Proxy p,InetAddress ip, int port, int connectTimeout) throws SocksException{
      if(p == null) throw new SocksException(Proxy.SOCKS_NO_PROXY);
      this.proxy = p.copy();
      this.remoteIP = ip;
      this.remotePort = port;
      this.remoteHost = ip.getHostAddress();
      if(proxy.isDirect(remoteIP))
        doDirect();
      else
        processReply(proxy.connect(ip,port,0));
   }
   
   public SocksSocket(InetAddress ip, int port, int connectTimeout,Proxy p) throws SocksException{
     if(p == null) throw new SocksException(Proxy.SOCKS_NO_PROXY);
     this.proxy = p;
     this.remoteIP = ip;
     this.remotePort = port;
     this.remoteHost = ip.getHostAddress();
     if(proxy.isDirect(remoteIP))
       doDirect();
     else
       processReply(proxy.connect(ip,port,0));
  }

   /**
    * These 2 constructors are used by the SocksServerSocket.
    * This socket simply overrides remoteHost, remotePort
    */
   protected SocksSocket(String  host,int port,Proxy proxy){
      this.remotePort = port;
      this.proxy = proxy;
      this.localIP = proxy.proxySocket.getLocalAddress();
      this.localPort = proxy.proxySocket.getLocalPort();
      this.remoteHost = host;
   }
   protected SocksSocket(InetAddress ip,int port,Proxy proxy){
      remoteIP = ip;
      remotePort = port;
      this.proxy = proxy;
      this.localIP = proxy.proxySocket.getLocalAddress();
      this.localPort = proxy.proxySocket.getLocalPort();
      remoteHost = remoteIP.getHostAddress();
   }

   /**
    * when https needs socket, *somewhere* in its flow, it calls this method
    * to make sure it has a connected socket.
    */
   public boolean isConnected() {
     return proxy.proxySocket.isConnected();
   }
   
   /**
    * Same as Socket
    */
   public void close() throws IOException{
      if(proxy!= null)proxy.endSession();
      proxy = null;
   }
   /**
    * Same as Socket
    */
   public InputStream getInputStream(){
      return proxy.in;
   }
   /**
    * Same as Socket
    */
   public OutputStream getOutputStream(){
      return proxy.out;
   }
   
   public SocketChannel getChannel()
   {
     if (proxy.proxySocket != null)
     {
       return proxy.proxySocket.getChannel();
     }
     return null;
   }
   /**
    * Same as Socket
    */
   public int getPort(){
      return remotePort;
   }
   /**
    * Returns remote host name, it is usefull in cases when addresses
    * are resolved by proxy, and we can't create InetAddress object.
      @return The name of the host this socket is connected to.
    */
   public String getHost(){
      return remoteHost;
   }
   /**
    * Get remote host as InetAddress object, might return null if 
    * addresses are resolved by proxy, and it is not possible to resolve
    * it locally
      @return Ip address of the host this socket is connected to, or null
      if address was returned by the proxy as DOMAINNAME and can't be
      resolved locally.
    */
   public InetAddress getInetAddress(){
      if(remoteIP == null){
	 try{
	   remoteIP = InetAddress.getByName(remoteHost);
	 }catch(UnknownHostException e){
	   return null;
	 }
      }
      return remoteIP;
   }

   /**
    * Get the port assigned by the proxy for the socket, not
    * the port on locall machine as in Socket. 
      @return Port of the socket used on the proxy server.
    */
   public int getLocalPort(){
      return localPort;
   }
   
   /** the following returns what the Socket returns for getLocalPort()
    * 
    * @return int returns what the Socket returns for getLocalPort()
    */
   public int getLocalSocketPort() {
     return proxy.getLocalSocketPort();
   }
   
   /**
    * Get address assigned by proxy to make a remote connection,
    * it might be different from the host specified for the proxy.
    * Can return null if socks server returned this address as hostname
    * and it can't be resolved locally, use getLocalHost() then.
      @return Address proxy is using to make a connection.
    */
   public InetAddress getLocalAddress(){
      if(localIP == null){
	 try{
	    localIP = InetAddress.getByName(localHost);
	 }catch(UnknownHostException e){
	   return null;
	 }
      }
      return localIP;
   }
   /**
      Get name of the host, proxy has assigned to make a remote connection
      for this socket. This method is usefull when proxy have returned
      address as hostname, and we can't resolve it on this machine.
      @return The name of the host proxy is using to make a connection.
   */
   public String getLocalHost(){
      return localHost;
   }

   /**
     Same as socket.
   */
   public void setSoLinger(boolean on,int val) throws SocketException{
      proxy.proxySocket.setSoLinger(on,val);
   }
   /**
     Same as socket.
   */
   public int getSoLinger(int timeout) throws SocketException{
      return proxy.proxySocket.getSoLinger();
   }
   /**
     Same as socket.
   */
   public void setSoTimeout(int timeout) throws SocketException{
      proxy.proxySocket.setSoTimeout(timeout);
   }
   /**
     Same as socket.
   */
   public int getSoTimeout(int timeout) throws SocketException{
      return proxy.proxySocket.getSoTimeout();
   }
   /**
     Same as socket.
   */
   public void setTcpNoDelay(boolean on) throws SocketException{
     proxy.proxySocket.setTcpNoDelay(on);
   }
   /**
     Same as socket.
   */
   public boolean getTcpNoDelay() throws SocketException{
     return proxy.proxySocket.getTcpNoDelay();
   }

   /**
     Get string representation of the socket.
   */
   public String toString(){
      if(directSock!=null) return "Direct connection:"+directSock;
      return ("Proxy:"+proxy+";"+"addr:"+remoteHost+",port:"+remotePort
                                +",localport:"+localPort);

   }

//Private Methods
//////////////////

   private void processReply(ProxyMessage reply)throws SocksException{
      localPort = reply.port;
      /*
       * If the server have assigned same host as it was contacted on
       * it might return an address of all zeros
       */
      if(reply.host.equals("0.0.0.0")
      || reply.host.equals("::")
      || reply.host.equals("::0")
      || reply.host.equals("0:0:0:0:0:0:0:0")
      || reply.host.equals("00:00:00:00:00:00:00:00")
      || reply.host.equals("0000:0000:0000:0000:0000:0000:0000:0000")){
         //localIP = proxy.proxyIP;
       try
       {
        localIP = InetAddress.getByName(proxy.proxyHost);
        localHost = localIP.getHostAddress();
       }
       catch (UnknownHostException e)
       {
          
       }
      }else{
         localHost = reply.host;
         localIP = reply.ip;
      }
   }
   private void doDirect()throws SocksException{
      try{
         //System.out.println("IP:"+remoteIP+":"+remotePort);
         directSock = new Socket();
         //directSock.setReuseAddress(true);
         //directSock.setReceiveBufferSize(VT.VT_NETWORK_PACKET_BUFFER_SIZE - 1);
         //directSock.setSendBufferSize(VT.VT_NETWORK_PACKET_BUFFER_SIZE - 1);
         if (connectTimeout > 0)
         {
           directSock.connect(new InetSocketAddress(remoteIP, remotePort), connectTimeout);
         }
         else
         {
           directSock.connect(new InetSocketAddress(remoteIP, remotePort));
         }
         //directSock = new Socket(remoteIP, remotePort);
         proxy.out = directSock.getOutputStream();
         proxy.in  = directSock.getInputStream();
         directSock.setTcpNoDelay(true);
         //directSock.setSoLinger(true, 5);
         //directSock.setReuseAddress(true);
         directSock.setKeepAlive(true);
         //directSock.setSoTimeout(90000);
         //directSock.setSoLinger(true, 0);
         proxy.proxySocket = directSock;
         localIP = directSock.getLocalAddress();
         localPort = directSock.getLocalPort();
      }catch(IOException io_ex){
         throw new SocksException(Proxy.SOCKS_DIRECT_FAILED,
                                  "Direct connect failed:"+io_ex);
      }
   }

}
