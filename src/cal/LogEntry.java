package cal;

import java.io.Serializable;
import java.util.Date;

public class LogEntry implements Serializable
{
	public final int creator; 	// id of the creator
	public final int ts; 		// time stamp of the creator
	public final int op; 		// operation new=0 conf=1, delete=2
	public final int[] participants; // the list of participants ids
	public final Date start;
	public final Date end;
	
	// ========================================================================
	
	public LogEntry(int c, int t, int o, int[] p, Date s, Date e)
	{
		creator = c;
		ts = t;
		op = o;
		start = s;
		end = e;
		participants = p;
	}
	
	// ========================================================================
	
	public String toString()
	{
		String dates = Utils.DF.format(start) + ";" + Utils.DF.format(end);
		String res = creator + "\t" + ts + "\t" + op + "\t" + dates + "\t";
		
		for (int i = 0; i < participants.length - 1; i++)
		{
			res += participants[i] + ",";
		}
		res += participants[participants.length - 1];
		return res;
	}
	
	// ========================================================================
	
	public static LogEntry String2LogEntry(String line)
	{
		// Dates contain spaces! ONLY TAB FOR
		// DELIMITER is the easiest solution
		String[] sLine;
		String[] parts;
		int[] participants = null;
		int creator, ts, op, i;
		Date start, end;
		
		// parse string
		sLine = line.split("\t");
		
		creator = Integer.parseInt(sLine[0]);
		ts = Integer.parseInt(sLine[1]);
		op = Integer.parseInt(sLine[2]);
				
		try
		{
			start = Utils.DF.parse(sLine[3].split(";")[0]);
			end = Utils.DF.parse(sLine[3].split(";")[1]);
		}
		catch (java.text.ParseException pe)
		{
			//TODO throw an exception here, or try to recover?
			return null;
		}

		// parse the list of participants
		parts = sLine[4].split(",");
		participants = new int[parts.length];
		for (i = 0; i < parts.length; i++)
		{
			participants[i] = Integer.parseInt(parts[i]);
		}
		return new LogEntry(creator, ts, op, participants, start, end);
	}

	// ========================================================================
	
}
