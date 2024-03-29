package org.vash.vate.socket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;

import org.vash.vate.nanohttpd.VTNanoHTTPDProxySession;

import net.sourceforge.jsocks.socks.server.ServerAuthenticator;
import net.sourceforge.jsocks.socks.server.ServerAuthenticatorNone;

public class VTSocksHttpProxyAuthenticatorNone extends ServerAuthenticatorNone
{
  private VTProxy connect_proxy;
  private VTAuthenticatedProxySocketFactory socket_factory;
  
  public VTSocksHttpProxyAuthenticatorNone(VTProxy proxy, VTAuthenticatedProxySocketFactory socket_factory)
  {
    this.connect_proxy = proxy;
    this.socket_factory = socket_factory;
  }
  
  public ServerAuthenticator startSession(Socket s) throws IOException
  {
    PushbackInputStream in = new PushbackInputStream(s.getInputStream());
    OutputStream out = s.getOutputStream();
    int version = in.read();
    //System.out.println("version=" + version);
    if (version == 5)
    {
      if (!selectSocks5Authentication(in, out, 0))
        return null;
    }
    else if (version == 4)
    {
      // Else it is the request message allready, version 4
      in.unread(version);
    }
    else
    {
      //System.out.println("version=" + version);
      if (version != -1)
      {
        in.unread(version);
        //fallback to use http proxy instead
        VTNanoHTTPDProxySession httpProxy = new VTNanoHTTPDProxySession(s, in, true, null, null, connect_proxy, socket_factory);
        try
        {
          httpProxy.run();
        }
        catch (Throwable t)
        {
          //t.printStackTrace();
        }
      }
      return null;
    }
    return new ServerAuthenticatorNone(in, out);
  }
}
