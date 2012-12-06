package cal;
import java.util.ArrayList;

/**
 * @author nlarusso
 *
 */
public class TimeTable
{
	private int myIndex;
	private int nodeCount;
	private ArrayList<ArrayList<Integer>> clock;
	
	// ========================================================================
	
	public TimeTable(int cnt, int idx)
	{
		nodeCount = cnt;
		myIndex = idx;
		clock = new ArrayList<ArrayList<Integer>>(nodeCount);

		// initialize clock
		for (int i = 0; i < nodeCount; ++i)
		{
			clock.add(new ArrayList<Integer>(nodeCount));
			for (int j = 0; j < nodeCount; ++j)
			{
				clock.get(i).add(0);
			}
		}
	}
	
	// ========================================================================
	
	public TimeTable(int [][] timetable, int idx)
	{
		int i,j;
		
		myIndex = idx;
		nodeCount = timetable.length;
		clock = new ArrayList<ArrayList<Integer>>(nodeCount);
		
		for (i = 0; i < nodeCount; ++i)
		{
			clock.add(new ArrayList<Integer>(nodeCount));
			for (j = 0; j < nodeCount; ++j)
			{
				clock.get(i).add(timetable[i][j]);
			}
		}
	}
	
	// ========================================================================
	
	public void merge(TimeTable t)
	{
		int i, j, max;
		ArrayList<Integer> c = t.getVClock(t.getIndex());
		ArrayList<Integer> me;
		
		/*
		 * Take the maximum of my vector clock and 
		 * the timetable t's vector clock
		 */
		for (i = 0; i < nodeCount; ++i)
		{
			max = java.lang.Math.max(clock.get(myIndex).get(i), c.get(i));
			clock.get(myIndex).set(i, max);
		}
		
		/*
		 * Take max from both timetables 
		 */
		for (i = 0; i < nodeCount; ++i)
		{
			c = t.getVClock(i);
			me = clock.get(i);
			
			for (j = 0; j < nodeCount; ++j)
			{
				max = java.lang.Math.max(me.get(j), c.get(j));
				clock.get(i).set(j, max);
			}
		}
	}
	
	// ========================================================================
	
	public void print()
	{
		System.out.println("\nMyIndex: " + myIndex);
		System.out.println("Nodes  : " + nodeCount);
		
		for (int i = 0; i < nodeCount; ++i)
		{
			System.out.print("\n" + i + ":\t");
			for (int j = 0; j < nodeCount; ++j)
			{
				System.out.print(clock.get(i).get(j) + "\t");
			}
		}
		System.out.println();
	}
	
	// ========================================================================
	
	/**
	 * Compute the timestamp which is least up to date 
	 */
	public int getAllMin(int colIdx)
	{
		int i, minVal = Integer.MAX_VALUE;
		
		for (i = 0; i < nodeCount; ++i)
		{
			if (clock.get(i).get(colIdx) < minVal)
				minVal = clock.get(i).get(colIdx);
		}
		return minVal;
	}
	
	// ========================================================================
	
	/**
	 * Update the vector clock after the occurance of a local event
	 */
	public int localEvent()
	{
		int localTime = clock.get(myIndex).get(myIndex) + 1;
		clock.get(myIndex).set(myIndex, localTime);
		
		return localTime;
	}
	
	// ========================================================================
	
	public ArrayList<Integer> getVClock(int idx)
	{
		return clock.get(idx);
	}
	
	// ========================================================================
	
	public int getIndex()
	{
		return myIndex;
	}

	// ========================================================================

	public int getMyTimeStamp()
	{
		return clock.get(myIndex).get(myIndex);
	}
	
	// ========================================================================
	
	public int[][] toArray()
	{
		int[][] tt = new int[nodeCount][nodeCount];
	    int i, j;

	    for (i = 0; i < nodeCount; ++i)
	    {
	    	for (j = 0; j < nodeCount; ++j)
	    		tt[i][j] = clock.get(i).get(j);
	    }
	    return tt;
	}

	// ========================================================================
}