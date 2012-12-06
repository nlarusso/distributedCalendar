//the client keeps several items serialized
//conf.dcal - holds the client id and process id to IP mapping
//log.dcal - the partial log
//cal.dcal - the calendar serialized

//TODO Check conflict resolution
//TODO Bandwidth testing
package cal;

import java.io.*;
import java.text.*;
import java.util.*;

public class Client
{

	// ========================================================================

	// number of clients
	private int N;
	// client ID (zero based)
	private int ID;
	private HashMap<Integer, Address> id2ip;
	
	// The in-memory representation of the log
	// log-->creator ID--> {creator TS, Log entry}
	private ArrayList<TreeMap<Integer, LogEntry>> log;
	// The in-memory representation of the calender
	// cal-->{Start2String,Cal entry array}
	private TreeMap<String, ArrayList<CalEntry>> cal;
	
	private Comm comcontroller;
	private TimeTable timeTable;

	// ========================================================================
	
	public enum OP
	{
		CREATE, DELETE
	}
	
	// ========================================================================
	
	// INITIALIZATION
	public Client() throws IOException
	{
		readConfigFile(Utils.CONFIG_FILE);
		
		initLog();
		initCal();
		
		// Start the communication layer
		comcontroller = new Comm(ID, id2ip);
	}
	
	// ========================================================================

	/**
	 * Read and parse configuration file
	 * @param file
	 * configuration file name
	 */
	private void readConfigFile(String file)
	{
		BufferedReader conf = null;
		Address address = null;
		String[] vals = null;
		String line;
		int id;
		
		/*
		 * Read the configuration in conf.dcal
		 */
		try
		{
			conf = new BufferedReader(new FileReader(file));
			line = conf.readLine();
			
			// ID of this machine
			ID = Integer.parseInt(line.split(Utils.SEP)[0]);
			if (line.split(Utils.SEP).length > 1)
			{
				// configuration params
				Utils.HOLD_ON_TIME = Long.parseLong(line.split(Utils.SEP)[1]);
				Utils.MEAN_WAIT_TIME = Long.parseLong(line.split(Utils.SEP)[2]);
			}
			
			// total number of machines in group
			N = 0;
			id2ip = new HashMap<Integer, Address>();
			while ((line = conf.readLine()) != null)
			{
				if ((line.isEmpty()) || (line.startsWith("#")))
					continue;
			
				vals = line.split(Utils.SEP);
				if (vals.length > 1)
				{
					id = Integer.parseInt(vals[0]);
					address = Address.parseAddressFromString(vals[1]);
					id2ip.put(id, address);
					N++;
				}
			}
			conf.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	// ========================================================================
	
	private void initCal()
	{
		cal = new TreeMap<String, ArrayList<CalEntry>>();
		readCal();
	}
	
	// ========================================================================
	
	private void initLog()
	{
		TreeMap<Integer, LogEntry> hm;
		int i;
		
		log = new ArrayList<TreeMap<Integer, LogEntry>>();
		for (i = 0; i < N; ++i)
		{
			hm = new TreeMap<Integer, LogEntry>();
			log.add(hm);
		}
		timeTable = new TimeTable(readLog(), ID);
	}
	
	// ========================================================================
	
	/**
	 *  reads the log upon initialization. Updates the clock and T
	 *  CREATOR TS OPERATION START;END PARTICIPANTS
	 *  0 0 0 11-11-2008 10:55;11-11-2008 11:55 0,1
	 */
	private int[][] readLog()
	{
		BufferedReader lr = null;
		String line = null;
		LogEntry le = null;
		int[][] T = new int[N][N];
		
		try
		{
			lr = new BufferedReader(new FileReader(Utils.LOG_FILE));
			while ((line = lr.readLine()) != null)
			{
				if (! line.trim().equals(""))
				{
					le = LogEntry.String2LogEntry(line);
					if (le != null)
					{
						log.get(le.creator).put(le.ts, le);
						T[ID][le.creator] = Utils.max(T[ID][le.creator], le.ts);
					}
				}
			}
			lr.close();
		}
		catch (FileNotFoundException fne)
		{
			// create if there is no log file
			writeFile("", Utils.LOG_FILE, true);
			timeTable = new TimeTable(N, ID);
		}
		catch (IOException e)
		{
			System.out.println("ERROR on reading file: " + e.getMessage());
		}
		
		return T;
	}
	
	// ========================================================================
	
	/*
	 * Initial loading of calendar from the disk
	 * 
	 * START;END CONFIRMED CREATOR PARTICIPANTS
	 * 11-11-2008 10:55;11-11-2008 11:55 1 1 0,1
	 */
	private void readCal()
	{
		BufferedReader cr = null;
		String line = null;
		CalEntry ce = null;
		
		try
		{
			cr = new BufferedReader(new FileReader(Utils.CAL_FILE));
			
			while ((line = cr.readLine()) != null)
			{
				if (line.length() > 1)
				{
					ce = CalEntry.String2CalEntry(line);
					if (ce != null)
					{
						if (cal.get(Utils.DF.format(ce.start)) == null)
						{
							cal.put(Utils.DF.format(ce.start), new ArrayList<CalEntry>());
						}
						cal.get(Utils.DF.format(ce.start)).add(ce);
					}
				}
			}
			cr.close();
		}
		catch (FileNotFoundException fne)
		{
			// create if there is no log file
			writeFile("", Utils.CAL_FILE, true);
		}
		catch (IOException e)
		{
			System.out.println("ERROR on reading file: " + e.getMessage());
		}
	}
	
	// ========================================================================
	
	/*
	 * FRONTEND MEETING MANAGERS
	 * Client menu: main cycle
	 */
	public void run() throws IOException, ParseException
	{
		BufferedReader brFile = null;
		BufferedReader brIn = new BufferedReader(new InputStreamReader(System.in));
		BufferedReader br = brIn;// new BufferedReader(new InputStreamReader(System.in));
		String input = null;
		boolean isDone = false;
		
		while (!isDone)
		{
			// Show main menu and wait for input
			if (br == brIn)
			{
				System.out.println("\n=====================================");
				System.out.println("Choose an action:");
				System.out.println("b \t Batch File");
				System.out.println("c \t Show calendar");
				System.out.println("l \t Show log");
				System.out.println("t \t Show timetable");
				System.out.println("n \t New event");
				System.out.println("r \t Random event");
				System.out.println("d \t Delete event");
				System.out.println("p \t Print node stats");
				System.out.println("s \t Sleep for a random period");
				System.out.println("h \t Hold on for " + Utils.HOLD_ON_TIME + "ms");
				System.out.println("f \t Print node stats to file");
				System.out.println("w \t Wipe Calendar, Log, and Disk");
				System.out.println("x \t Exit");
			}
			
			try
			{
				// wait until we have data to complete a readLine()
				do
				{
					if (comcontroller.queueSize() > 0)
					{
						processMesssages();
					}
					else
					{
						Thread.sleep(250);
					}
				}
				while ((!br.ready()) && (br == brIn));

				
				if ((input = br.readLine()) == null)
				{
					br = brIn;
					continue;
				}
				if (input.equals(""))
					continue;
				input = input.substring(0, 1);
				
				if (input.equalsIgnoreCase("c"))
					prettyPrintCal();
				else if (input.equalsIgnoreCase("l"))
					printLog();
				else if (input.equalsIgnoreCase("b"))
				{
					System.out.println("Enter filename: ");
					input = br.readLine();
					brFile = new BufferedReader(new FileReader(input));
					br = brFile;
				}
				else if (input.equalsIgnoreCase("n"))
					create(br);
				else if (input.equalsIgnoreCase("t"))
					timeTable.print();
				else if (input.equalsIgnoreCase("d"))
				{
					if (br.equals(brIn))
					{
						delete(br);
					}
					else
					{
						String d1 = br.readLine();
						String d2 = br.readLine();
						
						int[] participants = null;
						try
						{
							String[] parts = br.readLine().split(",");
							participants = new int[parts.length];
							for (int i = 0; i < parts.length; i++)
							{
								participants[i] = Integer.parseInt(parts[i]);
							}
						}
						catch (NumberFormatException e)
						{
							System.out.println("Error in parsing participant list (for delete): "
												+ e.getMessage());
						}
						delete(Utils.DF.parse(d1), Utils.DF.parse(d2), participants);
					}
				}
				else if (input.equalsIgnoreCase("s"))
				{
					long sleepms = Utils.getRandomWaitMS();
					System.out.println("Sleeping for" + sleepms / 1000f + "s");
					Thread.sleep(sleepms);
				}
				else if (input.equalsIgnoreCase("h"))
				{
					Thread.sleep(Utils.HOLD_ON_TIME);
				}
				else if (input.equalsIgnoreCase("p"))
					comcontroller.printStats(System.out);
				else if (input.equalsIgnoreCase("f"))
				{
					comcontroller.printStats(new FileOutputStream("stats_" + ID + "__"
							+ System.currentTimeMillis() + ".log"));
				}
				else if (input.equalsIgnoreCase("r"))
					rcreate();
				else if (input.equalsIgnoreCase("w"))
				{
					writeFile("", Utils.LOG_FILE, false);
					writeFile("", Utils.CAL_FILE, false);
					initLog();
					initCal();
					comcontroller.clearSendList();
				}
				else if (input.equalsIgnoreCase("x"))
				{
					comcontroller.close();
					isDone = true;
				}
				else
				{
					System.out.print("invalid operation");
				}
			}
			catch (InterruptedException e)
			{
				// timed out. check out for received messages
			}
			catch (IOException ioe)
			{
				System.out.print("invalid operation");
			}
		}
	}
	
	// ========================================================================
	
	/*
	 * ---------------- Writing data to disk ---------------- All writing of
	 * data to any files should take place in one of these two functions
	 */
	private boolean writeFile(String str, String fileName, boolean append)
	{
		try
		{
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, append));
			bw.write(str);
			bw.newLine();
			bw.close();
		}
		catch (Exception e)
		{
			System.out.println("Error printing to file: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	// ========================================================================
	// TODO can we reduce this code so that only one function actually writes to disk?
	private boolean writeFile(String fileName)
	{
		try
		{
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, false));
			
			if (fileName.equalsIgnoreCase(Utils.LOG_FILE))
			{
				for (int cID = 0; cID < log.size(); cID++)
				{
					for (int ts : log.get(cID).keySet())
					{
						bw.write(log.get(cID).get(ts).toString());
						bw.newLine();
					}
				}
			}
			else
			{
				for (String slotstart : cal.keySet())
				{
					for (CalEntry c : cal.get(slotstart))
					{
						bw.write(c.toString());
						bw.newLine();
					}
				}
			}
			bw.close();
		}
		catch (Exception e)
		{
			System.out.println("Error printing to file: " + e.getMessage());
			return false;
		}
		return true;
	}
	
	// ========================================================================
	
	/*
	 * Prompts the user for a data, parses and creates a new date object
	 */
	private Date getDate(String str, BufferedReader br)
	{
		String input = null;
		Date s = null;
		
		try
		{
			while (s == null)
			{
				if (str.equalsIgnoreCase("end"))
					System.out.print("Select an end date [mm-dd-yyyy HH:mm] (or 'x' to exit)\n");
				else
					System.out.print("Select a start date [mm-dd-yyyy HH:mm] (or 'x' to exit)\n");
				
				try
				{
					input = br.readLine();
					System.out.println("\nINPUT: " + input);
					
					if ((input == null) || (input.equalsIgnoreCase("x")))
						return null;
					s = Utils.DF.parse(input);
				}
				catch (ParseException pe)
				{
					System.out.print("Incorrect date format!\n");
					s = null;
				}
			}
		}
		catch (IOException e)
		{
			System.out.println("Error reading input: " + e.getMessage());
			s = null;
		}
		return s;
	}
	
	// ========================================================================
	
	// creates a new event
	public void create(BufferedReader br)
	{
		String input = null;
		Date s = getDate("start", br);
		Date e = getDate("end", br);
		
		if ((s == null) || (e == null))
		{
			System.out.println("Problems reading input!");
			return;
		}
		
		try
		{
			int[] WhoIsFree = getAvailable(s, e);
			int[] part;

			if (WhoIsFree == null)
			{
				System.out.print("You already have an appointment overlapping with " +
			                 	 Utils.DF.format(s) + " and " + Utils.DF.format(e) + "\n");
				return;
			}
			// ask for participants
			System.out.print("Select participants(separate by comma).\n" + "Free: " +
			                 Utils.arr2str(WhoIsFree) + "\n");
			input = br.readLine();
			
			part = Utils.str2arr(input, ",");
			if (part == null)
			{
				System.out.println("Problems reading input!");
				return;
			}
			
			if (! Utils.contains(WhoIsFree, part))
			{
				System.out.print("One of the participants you've chosen is busy during that time. Please redo the appointment!\n");
				return;
			}
			if (! Utils.contains(part, ID))
			{
				part = Utils.add(part, ID);
			}
			
			// add the appointment in the local log and calendar
			int curTS = timeTable.localEvent();
			LogEntry le = new LogEntry(ID, curTS, OP.CREATE.ordinal(), part, s, e);
			log.get(ID).put(le.ts, le);
			
			CalEntry ce = new CalEntry(false, part, ID, s, e);
			if (cal.get(Utils.DF.format(s)) == null)
			{
				cal.put(Utils.DF.format(s), new ArrayList<CalEntry>());
			}
			cal.get(Utils.DF.format(s)).add(ce);
			
			writeFile(le.toString(), Utils.LOG_FILE, true);
			writeFile(ce.toString(), Utils.CAL_FILE, true);
			
			sendMsgs(part);
		}
		catch (IOException ioe)
		{
			System.out.print("invalid operation");
		}
	}
	
	// ========================================================================
	
	private void sendMsgs(int[] participants)
	{
		for (int i = 0; i < participants.length; i++)
		{
			if (ID != participants[i])
			{
				ArrayList<LogEntry> pl = selectPartialLog(participants[i]);
				comcontroller.sendMessage(participants[i], timeTable.toArray(), pl);
			}
		}
	}
	
	// ========================================================================
	
	private Date getRandomDate() throws ParseException
	{
		Random r = new Random();
		Date d = Utils.DF.parse((r.nextInt(12) + 1) + "-" + (r.nextInt(28) + 1) + "-"
						+ (2000 + r.nextInt(8)) + " " + r.nextInt(11) + ":"
						+ r.nextInt(59));
		return d;
	}
	
	// ========================================================================
	
	public void rcreate() throws ParseException, IOException
	{
		Date s = null, e = null;
		int[] part = null;
		int curTS;
		
		while (part == null)
		{
			s = getRandomDate();
			e =	Utils.DF.parse((s.getMonth() + 1) + "-" + s.getDate() + "-"
							+ (1900 + s.getYear()) + " " + (s.getHours() + 1) + ":"
							+ s.getMinutes());
			part = getAvailable(s, e);
		}
		curTS = timeTable.localEvent();
		LogEntry le = new LogEntry(ID, curTS, OP.CREATE.ordinal(), part, s, e);
		log.get(ID).put(le.ts, le);
		CalEntry ce = new CalEntry(false, part, ID, s, e);
		
		if (cal.get(Utils.DF.format(s)) == null)
		{
			cal.put(Utils.DF.format(s), new ArrayList<CalEntry>());
		}
		cal.get(Utils.DF.format(s)).add(ce);
		
		writeFile(le.toString(), Utils.LOG_FILE, true);
		writeFile(le.toString(), Utils.CAL_FILE, true);
		
		sendMsgs(part);
	}
	
	// ========================================================================
	
	/*
	 * Prompts the user for which calendar entry she would like to delete, we
	 * index that entry and return it
	 */
	private CalEntry getDelSlot(BufferedReader br)
	{
		String input;
		int apptId = -1;
		boolean isDone = false;
		
		ArrayList<CalEntry> ceList = printCal(true);
		
		while (!isDone)
		{
			System.out.println("\nWhich appointment would you like to delete? ");
			try
			{
				input = br.readLine();
				apptId = Integer.parseInt(input);
				
				if ((apptId < 0) || (apptId >= cal.size()))
					System.out.println("Invalid appointment! Please choose a number between 0 and "
										+ (cal.size() - 1));
				else
					isDone = true;
			}
			catch (Exception e)
			{
				System.out.println("Error reading input: " + e.getMessage());
				System.out.println("Please try again. ");
			}
		}
		return ceList.get(apptId);
	}
	
	// ========================================================================
	
	// deletes an event
	public void delete(BufferedReader br)
	{
		final int IDX = 0;
		LogEntry le = null;
		CalEntry ce = getDelSlot(br);
		
		// if (! contains(ce.participants, ID))
		// {
		// System.out.print("You can delete only your events!");
		// return;
		// }
		
		cal.get(Utils.DF.format(ce.start)).remove(IDX);
		
		int curTS = timeTable.localEvent();
		le = new LogEntry(ID, curTS, OP.DELETE.ordinal(), ce.participants, ce.start,
								ce.end);
		// le = new LogEntry(ID, clock++, OP.DELETE.ordinal(), ce.participants,
		// ce.start, ce.end);
		log.get(ID).put(le.ts, le);
		
		writeFile(le.toString(), Utils.LOG_FILE, true);
		writeFile(Utils.CAL_FILE);
	}
	
	// ========================================================================
	
	// deletes an event by start,end date
	public void delete(Date s, Date e, int[] participants)
	{
		boolean success = false;
		if (cal.get(Utils.DF.format(s)) == null)
		{
			System.out.println("WARNING: Batch file attempt to delete inexisting event.");
			return;
		}
		
		for (int i = 0; i < cal.get(Utils.DF.format(s)).size(); i++)
		{
			CalEntry ce = cal.get(Utils.DF.format(s)).get(i);
			if ((ce.end.equals(e)) && 
				(Utils.equals(ce.participants, participants)) &&
				(Utils.contains(ce.participants, ID)))
			{
				cal.get(Utils.DF.format(s)).remove(i);
				int curTS = timeTable.localEvent();
				
				LogEntry le = new LogEntry(ID, curTS, OP.DELETE.ordinal(), ce.participants, ce.start, ce.end);
				
				log.get(ID).put(le.ts, le);
				
				writeFile(le.toString(), Utils.LOG_FILE, true);
				writeFile(Utils.CAL_FILE);
				
				success = true;
				break;
			}
		}
		if (!success)
			System.out.println("WARNING: Batch file attempt to delete inexisting event or one in which you are not a participant.");
	}
	
	// ========================================================================
	
	// prints the local log
	public void printLog()
	{
		int i, cnt = 0;
		System.out.println("\n");
		System.out.println("\tCreator\tTmsp\tOp\t\tStart;End\t\tParticipants");
		System.out.println("\t_________________________________________________");
		
		for (i = 0; i < log.size(); i++)
		{
			for (LogEntry le : log.get(i).values())
			{
				System.out.println(cnt++ + "\t" + le.toString());
			}
		}
		System.out.println("\n");
	}
	
	// ========================================================================
	
	public ArrayList<CalEntry> printCal()
	{
		return printCal(false);
	}
	
	// ========================================================================
	
	// prints the local calendar
	public ArrayList<CalEntry> printCal(boolean onlyMine)
	{
		int cnt = 0;
		ArrayList<CalEntry> ceList = new ArrayList<CalEntry>();
		
		System.out.println("\n");
		System.out.println("\t\t\tStart;End\t\tCnfrm?\tOwner\tParticipants");
		System.out.println("\t_______________________________________________");
		
		for (ArrayList<CalEntry> ces : cal.values())
		{
			for (CalEntry ce : ces)
			{
				if ((!onlyMine) || (Utils.contains(ce.participants, ID)))
				{
					System.out.println(cnt++ + "\t" + ce.toString());
					ceList.add(ce);
				}
			}
		}
		System.out.println("\n");
		return ceList;
	}
	
	// ========================================================================
	
	// prints the local calendar
	public void prettyPrintCal()
	{
		int cnt = 0;
		Set<String> keys = cal.keySet();
		Iterator<String> iter = keys.iterator();
		String key, curDate = "";
		
		System.out.println("=========== CALENDAR ===========");
		
		while (iter.hasNext())
		{
			key = iter.next();
			
			if (!key.substring(0, 10).equalsIgnoreCase(curDate))
			{
				curDate = key.substring(0, 10);
				System.out.println("\n\n\t~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
				System.out.println("\t\tDATE: " + curDate);
				System.out.println("\t~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
				System.out.println("  Cnt\tStart;End\tCnfrm?\tOwner\tParticipants");
				System.out.println("  ____________________________________________________");
			}
			
			for (CalEntry ce : cal.get(key))
			{
				System.out.print("  " + (cnt++) + "\t" + ce.toPrettyString());
				/*
				 * Print an asterisk to show the user that appointments are
				 * overlapping
				 */
				if (cal.get(key).size() > 1)
					System.out.print(" *");
				System.out.println();
			}
		}
		System.out.println("\n");
	}
	
	// ========================================================================
	
	// COMMUNICATION HANDLERS (BACKGROUND)
	// Handles receipts of messages from the receipt queue
	// COMMUNICATION HANDLERS (BACKGROUND)
	// Handles receipts of messages from the receipt queue
	private void processMesssages() throws IOException
	{
		int source;
		TimeTable Tf;
		ArrayList<LogEntry> nle = null;
		ArrayList<LogEntry> pl = null;
		
		while (comcontroller.queueSize() > 0)
		{
			Message m = comcontroller.popQ();
			source = m.sendId;
			Tf = new TimeTable(m.timeTable, source);
			pl = m.partialLog;
			
			timeTable.merge(Tf);
			
			nle = updateLog(pl);
			updateCal(nle);
		}
	}
	
	// ========================================================================
	
	// DATA STRUCTURES MANAGERS
	// merges the received Tf from process id sender and T
	
	// Returns the available parties for a start-end date
	private int[] getAvailable(Date s, Date e)
	{
		boolean[] busy = new boolean[N];
		Arrays.fill(busy, false);
		int i;
		
		for (String start : cal.keySet())
		{
			for (CalEntry ce : cal.get(start))
			{
				if (ce.overlaps(s, e))
				{
					for (i = 0; i < ce.participants.length; i++)
					{
						busy[ce.participants[i]] = true;
					}
				}
			}
			// a little dummy optimization
			// if(Utils.DF.parse(start).after(e)) break;
		}
		
		if (busy[ID])
		{
			return null;
		}
		else
		{
			int[] res = null;
			int c = 0, idx = 0;
			
			for (boolean b : busy)
			{
				if (!b)
				{
					c++;
				}
			}
			res = new int[c];

			for (i = 0; i < busy.length; i++)
			{
				if (!busy[i])
				{
					res[idx] = i;
					idx++;
				}
			}
			return res;
		}
	}
	
	// ========================================================================
	
	// Computes the partial log for id
	// The entries for each creator are in ascending (ts) order
	private ArrayList<LogEntry> selectPartialLog(int pid)
	{
		ArrayList<LogEntry> pl = new ArrayList<LogEntry>();
		ArrayList<Integer> partTT = timeTable.getVClock(pid);
		
		for (int cID = 0; cID < log.size(); cID++)
		{
			for (int ts : log.get(cID).keySet())
			{
				if (ts > partTT.get(cID))
					pl.add(log.get(cID).get(ts));
			}
		}
		return pl;
	}
	
	// ========================================================================
	
	// Merges a partial log with the local
	// returns the difference (i.e. the new operations to be executed)
	// Merges a partial log with the local
	// returns the difference (i.e. the new operations to be executed)
	private ArrayList<LogEntry> updateLog(ArrayList<LogEntry> pl) throws IOException
	{
		ArrayList<LogEntry> newEntries = new ArrayList<LogEntry>();
		
		for (LogEntry le : pl)
		{
			if (!log.get(le.creator).containsKey(le.ts))
			{
				if (!calContains(le) || le.op == OP.DELETE.ordinal())
				{
					newEntries.add(le);
					log.get(le.creator).put(le.ts, le);
				}
			}
		}
		// truncate based on the new T
		truncateLog();
		
		writeFile(Utils.LOG_FILE);
		return newEntries;
	}
	
	// ========================================================================
	
	/*
	 * Truncate the log (!!! Assumes that T has merged the Tf !!!)
	 */
	private void truncateLog()
	{
		// truncate the in-memory log
		
		for (int cID = 0; cID < log.size(); cID++)
		{
			if (log.get(cID) != null)
			{
				TreeMap<Integer, LogEntry> le = new TreeMap<Integer, LogEntry>();
				for (int ts : log.get(cID).keySet())
				{
					if (ts > timeTable.getAllMin(cID))
						le.put(ts, log.get(cID).get(ts));
				}
				log.set(cID, le);
			}
		}
	}

	// ========================================================================

	// Applies the new entries from the partial log to the calendar
	private void updateCal(ArrayList<LogEntry> pl) throws IOException
	{
		int i;
		
		// update the in-memory
		for (LogEntry le : pl)
		{
			if (OP.DELETE.ordinal() == le.op)
			{
				// a delete event
				for (i = 0; i < cal.get(Utils.DF.format(le.start)).size(); i++)
				{
					if (cal.get(Utils.DF.format(le.start)).get(i).equals(le.creator, le.start, le.end))
					{
						cal.get(Utils.DF.format(le.start)).remove(i);
						break;
					}
				}
			}
			else
			{
				CalEntry ce = new CalEntry(false, le.participants, le.creator, le.start, le.end);
				if (cal.get(Utils.DF.format(ce.start)) == null)
				{
					cal.put(Utils.DF.format(ce.start), new ArrayList<CalEntry>());
				}
				cal.get(Utils.DF.format(ce.start)).add(ce);
			}
		}
		writeFile(Utils.CAL_FILE);
	}
	
	// ========================================================================
	
	private boolean calContains(LogEntry le)
	{
		if (cal.get(Utils.DF.format(le.start)) == null)
		{
			return false;
		}
		
		for (CalEntry ce : cal.get(Utils.DF.format(le.start)))
		{
			if ((ce.start.equals(le.start)) && (ce.end.equals(le.end)) &&
				(Utils.equals(le.participants, ce.participants)))
			{
				return true;
			}
		}
		return false;
	}
	
	// ========================================================================
	
	public static void main(String[] Args) throws IOException, Exception
	{
		Client c = new Client();
		c.run();
	}
	
	// ========================================================================
}
