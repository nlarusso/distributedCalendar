package cal;
import java.io.Serializable;
import java.util.ArrayList;


public class Message implements Serializable
{
	private static final long serialVersionUID = 1L;
	public int timeTable[][];
	public ArrayList<LogEntry> partialLog;
	public int  sendId;
	public int recvId;

	// ========================================================================
	
	public Message(int my_id, int dest_id, int t1[][], ArrayList<LogEntry> log)
	{
		timeTable  = t1;
		partialLog = log;
		sendId = my_id;
		recvId = dest_id;
	}
	
	// ========================================================================
}
