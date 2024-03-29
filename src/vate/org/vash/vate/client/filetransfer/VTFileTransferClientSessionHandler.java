package org.vash.vate.client.filetransfer;

public class VTFileTransferClientSessionHandler implements Runnable
{
  private VTFileTransferClient client;
  private VTFileTransferClientSession session;
  
  public VTFileTransferClientSessionHandler(VTFileTransferClient client, VTFileTransferClientSession session)
  {
    this.client = client;
    this.session = session;
  }
  
  public VTFileTransferClient getClient()
  {
    return client;
  }
  
  public VTFileTransferClientSession getSession()
  {
    return session;
  }
  
  public void run()
  {
    try
    {
      session.getClient().getConnection().resetFileTransferStreams();
      session.startSession();
      session.getTransaction().run();
      session.endSession();
    }
    catch (Throwable e)
    {
      
    }
  }
}