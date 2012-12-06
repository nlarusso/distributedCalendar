package cal;

import java.util.*;

public class CalEntry
{
	/*
	 * Assumes that the slots of a calendar are fixed and enumerated
	 * integers in the range 0..S
	 */
	public static int S = Integer.MAX_VALUE;
	
	public final boolean confirmed;
	public final int[] participants;

	// an event can be deleted only by the creator or by a message from the creator
	public final Date start, end;
	public final int creator;
	
	// ========================================================================
	
	public CalEntry(boolean conf, int[] partic, int creat, Date s, Date e)
	{
		confirmed = conf;
		participants = partic;
		creator = creat;
		start = s;
		end = e;
	}
	
	// ========================================================================
	
	public String toPrettyString()
	{
		String res = Utils.DF.format(start).substring(11) + ";" +
					 Utils.DF.format(end).substring(11) + Utils.SEP +
					 (confirmed ? "Yes" : "No") + Utils.SEP + creator + Utils.SEP;
		
		for (int i = 0; i < participants.length - 1; i++)
		{
			res += participants[i] + ",";
		}
		res += participants[participants.length - 1];
		return res;
	}
	
	// ========================================================================
	
	public String toString()
	{
		String res = Utils.DF.format(start) + ";" + Utils.DF.format(end) + Utils.SEP
						+ (confirmed ? 1 : 0) + Utils.SEP + creator + Utils.SEP;
		for (int i = 0; i < participants.length - 1; i++)
		{
			res += participants[i] + ",";
		}
		res += participants[participants.length - 1];
		return res;
	}
	
	// ========================================================================
	
	public boolean equals(CalEntry e)
	{
		if (null == e)
			return false;
		
		if ((e.creator != creator) || (!e.start.equals(start)) || (!e.end.equals(end)))
			return false;
		
		return true;
	}
	
	// ========================================================================
	
	public boolean equals(int eCreator, Date eStart, Date eEnd)
	{
		if ((eCreator != creator) || (!eStart.equals(start)) || (!eEnd.equals(end)))
			return false;
		
		return true;
	}
	
	// ========================================================================
	
	public boolean overlaps(CalEntry e)
	{
		return overlaps(e.start, e.end);
	}
	
	// ========================================================================
	
	public boolean overlaps(Date eStart, Date eEnd)
	{
		if ((eEnd.compareTo(start) <= 0) || (end.compareTo(eStart) <= 0))
			return false;
		else
			return true;
	}

	// ========================================================================
	
	public static CalEntry String2CalEntry(String line)
	{
		String[] sLine = line.split(Utils.SEP);
		Date start, end;
		
		try
		{
			start = Utils.DF.parse(sLine[0].split(";")[0]);
			end = Utils.DF.parse(sLine[0].split(";")[1]);
		}
		catch (java.text.ParseException pe)
		{
			// TODO throw exception or try to recover?
			return null;
		}
		
		try
		{
			int i, status, creator;
			boolean confirmed;
			
			status = Integer.parseInt(sLine[1]);
			confirmed = (1 == status);
			creator = Integer.parseInt(sLine[2]);
			
			String[] parts = sLine[3].split(",");
			int[] participants = new int[parts.length];
			for (i = 0; i < parts.length; i++)
			{
				participants[i] = Integer.parseInt(parts[i]);
			}
			return (new CalEntry(confirmed, participants, creator, start, end));
		}
		catch (NumberFormatException e)
		{
			System.out.println("Error parsing Calendar: " + e.getMessage());
			return null;
		}
	}

	// ========================================================================

}
