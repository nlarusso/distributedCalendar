package cal;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;


public class ServerThread extends Thread
{
	private Socket socket;
	private ObjectInputStream objIn;
	private BlockingQueue<Message> queue;
	
	// =============================================================================
	
	public ServerThread(Socket sock_in, BlockingQueue<Message> q_in)
	{
		super("ServerThread");
		objIn  = null;
		socket = sock_in;
		queue = q_in;
	}
	
	// =============================================================================
	
	@Override
	public void run()
	{
		Message msg = null;
		try
		{
			objIn = new ObjectInputStream(socket.getInputStream());
			msg = (Message) objIn.readObject();
			queue.add(msg);
			
			objIn.close();
			socket.close();
		}
		catch (EOFException eof)
	    {
			System.out.println("EOF Encountered\nError: " + eof.getMessage());
	    } 
		catch (OptionalDataException ode)
	    {
			System.out.println("Optional Data Exception\nError: " + ode.getMessage());
	    }
		catch (IOException ioe)
	    {                  
			System.out.println("IOException on read object\nError: " + ioe.getMessage());
	    }
		catch (Exception e)
	    {
			System.out.println("Error: " + e.getMessage());
			System.exit(-1);
	    }
	}
	
	// =============================================================================
}
