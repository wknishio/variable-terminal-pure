package org.vash.vate.server.session;

import java.util.Collection;

import org.vash.vate.console.VTConsole;
import org.vash.vate.server.VTServer;
import org.vash.vate.server.authentication.VTServerAuthenticator;
import org.vash.vate.server.connection.VTServerConnection;

public class VTServerSessionHandler implements Runnable
{
  private boolean authenticated;
  // private VTServer server;
  private VTServerConnection connection;
  private VTServerSession session;
  private VTServerAuthenticator authenticator;
  private Collection<VTServerSessionListener> listeners;
  
  public VTServerSessionHandler(VTServer server, VTServerConnection connection)
  {
    this.authenticated = false;
    // this.server = server;
    this.connection = connection;
    this.authenticator = new VTServerAuthenticator(server, connection);
    this.session = new VTServerSession(server, connection);
    // this.session.initialize();
  }
  
  public VTServerAuthenticator getAuthenticator()
  {
    return authenticator;
  }
  
  public boolean isAuthenticated()
  {
    return authenticated;
  }
  
  public String getUser()
  {
    return session.getUser();
  }
  
  public VTServerSession getSession()
  {
    return session;
  }
  
  public void run()
  {
    authenticated = false;
    //VTConsole.print("\rVT>Authenticating session with client...\nVT>");
    try
    {
      connection.setAuthenticationStreams();
      if (authenticator.tryAuthentication() && connection.setConnectionStreams(authenticator.getDigestedCredential()))
      {
        VTConsole.print("\rVT>Session with client accepted!\nVT>");
        processSession();
      }
      else
      {
        VTConsole.print("\rVT>Session with client rejected!\nVT>");
        connection.closeConnection();
      }
    }
    catch (Throwable e)
    {
      // e.printStackTrace();
      VTConsole.print("\rVT>Session with client failed!\nVT>");
      connection.closeConnection();
    }
    System.runFinalization();
    System.gc();
  }
  
  private void processSession()
  {
    boolean started = false;
    try
    {
      connection.startConnection();
      session.initialize();
      session.setUser(authenticator.getUser());
      session.negotiateShell();
      session.startShell();
      session.startSession();
      session.startSessionThreads();
      authenticated = true;
      started = true;
      try
      {
        for (VTServerSessionListener listener : listeners)
        {
          try
          {
            listener.sessionStarted(session);
          }
          catch (Throwable t)
          {
            
          }
        }
      }
      catch (Throwable t)
      {
        
      }
      session.waitSession();
      session.stopShell();
      session.tryStopSessionThreads();
      connection.closeConnection();
      session.waitThreads();
    }
    catch (Throwable e)
    {
      //e.printStackTrace();
    }
    if (started)
    {
      try
      {
        for (VTServerSessionListener listener : listeners)
        {
          try
          {
            listener.sessionFinished(session);
          }
          catch (Throwable t)
          {
            
          }
        }
      }
      catch (Throwable t)
      {
        
      }
    }
    session.clearSessionCloseables();
  }
  
  public void setSessionListeners(Collection<VTServerSessionListener> listeners)
  {
    this.listeners = listeners;
  }
}