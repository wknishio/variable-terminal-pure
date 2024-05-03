package org.vash.vate.socket;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public abstract class VTRemoteSocketFactory
{  
  public abstract Socket createSocket(String host, int port, VTProxy... proxies) throws IOException, UnknownHostException;
  public abstract Socket acceptSocket(String host, int port, int timeout) throws IOException, UnknownHostException;
}