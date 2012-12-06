package cal;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.BlockingQueue;


public class ListenServer extends Thread
{
	/*
	 * Determines the interval at which the server will check to see if
	 * it should close the listen socket
	 */
	public static final int INTERVAL = 8000;
	
	// =============================================================================
	
	private ServerSocket servSocket;
	private int listenPort;
	private int messagesRecvd;
	private boolean isListening;
	private BlockingQueue<Message> queue;
	
	// =============================================================================
	
	public ListenServer(int port, BlockingQueue<Message> q_in)
	{
		listenPort = port;
		queue = q_in;
		messagesRecvd = 0;
		
		try
		{
			servSocket = new ServerSocket(listenPort);
			servSocket.setReuseAddress(true);
		}
		catch(IOException e)
		{
			System.err.println("Could not listen on port: " + listenPort + "\nERROR: " + e.getMessage());
			System.exit(-1);
		}
	}
	
	// =============================================================================
	
	public ListenServer(BlockingQueue<Message> q_in)
	{
		this(Address.DEFAULT_PORT, q_in);
	}
	
	// =============================================================================
	
	@Override
	public void run()
	{
		isListening = true;
		
		while (isListening)
		{
			try
			{
				servSocket.setSoTimeout(INTERVAL);
				
				while (isListening)
				{
					new ServerThread(servSocket.accept(), queue).start();
					++messagesRecvd;
				}
				servSocket.close();
			}
			catch(IOException e)
			{
				if (! e.getMessage().equalsIgnoreCase("Accept timed out"))
					System.err.println("Socket IO Error!\nERROR: " + e.getMessage());
			}
			catch(Exception e)
			{
				System.err.println("Socket Error!\nERROR: " + e.getMessage());
			}
		}
		isListening = false;
		System.out.println("Server Quiting");
	}
	
	// =============================================================================
	
	public void stopListening()
	{
		isListening = false;
	}
	
	// =============================================================================
	
	public int getMsgRecvCount()
	{
		return messagesRecvd;
	}
	
	// =============================================================================
}
