package cal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Comm
{
	// =============================================================================

	private int myId;
	private int msgSendCnt;

	private BlockingQueue<Message> queue;
	private HashMap<Integer, Address> nodeTable;
	private ObjectOutputStream objOut;
	private HashMap<Integer, ArrayList<Message>> msgSentList;
	private ArrayList<Message> orderedSend;

	private ListenServer listener;
	private Socket sock;

	// =============================================================================
	
	public Comm(int myid, HashMap<Integer, Address> id2ip)
	{
		queue = new LinkedBlockingQueue<Message>();
		listener = new ListenServer(queue);
		nodeTable = id2ip;
		myId = myid;
		objOut = null; 
		sock = null;
		msgSendCnt = 0;
		msgSentList = new HashMap<Integer, ArrayList<Message>>();
		orderedSend = new ArrayList<Message>();
		
		listener.start();
	}
	
	// =============================================================================
	
	private long getMsgSize(Message msg)
	{
		long size = -1;
		try
		{
			FileOutputStream fos = new FileOutputStream("t.tmp");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			
			oos.writeObject(msg);
			oos.close();
			fos.close();
			
			File t = new File("t.tmp");
			// get file size -> size of serialized object in bytes
			size = t.length();
			t.delete();
		}
		catch(IOException e)
		{
			System.out.println("Error computing message size: " + e.getMessage());
		}
		return size;
	}
	
	// =============================================================================
	
	public void clearSendList()
	{
		msgSentList = new HashMap<Integer, ArrayList<Message>>();
		orderedSend = new ArrayList<Message>();
		msgSendCnt = 0;
	}
	
	// =============================================================================
	
	public boolean sendMessage(int dest_id, int[][] timeTable, ArrayList<LogEntry> partialList)
	{
		Message msg = new Message(myId, dest_id, timeTable, partialList);
		boolean success = false;
		Address address = nodeTable.get(dest_id);
		
		if (address == null)
		{
			System.err.println("Comm.sendMessage() Error: Invalid destination ID.");
			return success;
		}
		
		try
		{
			sock = new Socket(address.getIP(), address.getPort());
			objOut = new ObjectOutputStream(sock.getOutputStream());
			System.out.println("Sending Message...");
			
			// send message!
			objOut.writeObject(msg);
			success = true;

			/*
			 * Collect stats on the messages we've sent 
			 */
			if (msgSentList.get(dest_id) == null)
				msgSentList.put(dest_id, new ArrayList<Message>());
			
			msgSentList.get(dest_id).add(msg);
			orderedSend.add(msg);
			++msgSendCnt;
			
			objOut.close();
			sock.close();
		}
		catch (ConnectException e)
		{
			System.out.println("IP: " + address.toString() + "\tNode(" + dest_id + ") is down!");
		}
		catch (IOException e)
		{
			System.err.println("Socket Error: " + e.getMessage());
			e.printStackTrace();
		}
		return success;
	}
	
	// =============================================================================
	
	public void printStats(OutputStream out)
	{
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(out));
		Set<Integer> keys = msgSentList.keySet();
		Iterator<Integer> iter = keys.iterator();
		ArrayList<Message> msgList;
		Integer i, key;

		try
		{
			br.write("\n-------------------------------------------------------");
			br.newLine();
			br.write("Total Number of messages sent from   " + myId + ": " + msgSendCnt);
			br.newLine();
			br.write("Total Number of messages received at " + myId + ": " + listener.getMsgRecvCount());
			br.newLine();
			
			while (iter.hasNext())
			{
				key = iter.next();
				msgList = msgSentList.get(key);

				br.write("\n\tMessages sent from " + myId + " to " + key);
				br.newLine();
				br.write("Msg#\tMsg Size(B)\tPLog Size");
				br.newLine();
				for (i = 0; i < msgList.size(); ++i)
				{
					br.write(i + ")\t" + getMsgSize(msgList.get(i)) + "\t\t" + msgList.get(i).partialLog.size());
					br.newLine();
				}
			}
			br.write("-------------------------------------------------------\n");
			br.newLine();

			br.write("Ordered Message List:");
			br.newLine();
			br.write("Cnt\tSender\tRecvr\tSize(B)\tPL size");
			br.newLine();

			Message tmp;
			for (i = 0; i < orderedSend.size(); ++i)
			{
				tmp = orderedSend.get(i);
				br.write(i + ")\t" + tmp.sendId + "\t" + tmp.recvId + "\t" + getMsgSize(tmp) + "\t" + tmp.partialLog.size());   
				br.newLine();
			}
			
			br.write("-------------------------------------------------------\n");
			br.newLine();
			br.flush();
			//TODO fix this 
			/*
			 * Do not close the bufferedWriter because that closes the underlying stream!
			 * Since we could be using standard out, this can cause some trouble
			 * There should be a better way to do this, but I haven't found it :(
			 */
		}
		catch (IOException e)
		{
			System.out.println("Error writing stats: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	// =============================================================================
	
	public void close()
	{
		System.out.println("Closing Comm. Please allow up to " + (ListenServer.INTERVAL / 1000) + " seconds for the server to quit.");
		listener.stopListening();

		try
		{
			printStats(new FileOutputStream("stats_" + myId + ".log"));
			if (objOut != null)
				objOut.close();
			if (sock != null)
				sock.close();
		}
		catch (Exception e)
		{
			System.out.println("Error in closing Comm: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	// =============================================================================
	
	public int queueSize()
	{
		return queue.size();
	}
	
	// =============================================================================
	
	public Message popQ()
	{
		return queue.poll();
	}
	
	// =============================================================================
	
	public void enqueue(Message msg)
	{
		queue.add(msg);
	}
	
	// =============================================================================
}