package org.vash.vate.server.console.remote.standard.command;

import org.vash.vate.help.VTHelpManager;
import org.vash.vate.server.console.remote.standard.VTServerStandardRemoteConsoleCommandProcessor;

public class VTHOST extends VTServerStandardRemoteConsoleCommandProcessor
{
  public VTHOST()
  {
    this.setFullName("*VTHOST");
    this.setAbbreviatedName("*VTHT");
    this.setFullSyntax("*VTHOST <HOST>");
    this.setAbbreviatedSyntax("*VTHT <HT>");
  }
  
  public void execute(String command, String[] parsed) throws Exception
  {
    synchronized (session.getHostResolver())
    {
      // connection.getResultWriter().write(command);
      // connection.getResultWriter().flush();
      if (parsed.length >= 2)
      {
        if (session.getHostResolver().isFinished())
        {
          session.getHostResolver().joinThread();
        }
        if (!session.getHostResolver().aliveThread())
        {
          session.getHostResolver().setFinished(false);
          session.getHostResolver().setHost(parsed[1]);
          session.getHostResolver().startThread();
        }
        else
        {
          connection.getResultWriter().write("\nVT>Another network host resolution is still running!\nVT>");
          connection.getResultWriter().flush();
        }
      }
      else
      {
        connection.getResultWriter().write("\nVT>Invalid command syntax!" + VTHelpManager.getHelpForClientCommand(parsed[0]));
        connection.getResultWriter().flush();
      }
    }
  }
  
  public void close()
  {
    
  }
  
  public boolean remote()
  {
    return false;
  }
}
