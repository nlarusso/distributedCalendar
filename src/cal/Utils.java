package cal;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Random;

public class Utils
{
	// ========================================================================
	
	public static final DateFormat DF = new SimpleDateFormat("MM-dd-yyyy HH:mm");
	public final static String SEP = "\t";
	public final static String LOG_FILE = "log.dcal";
	public final static String CAL_FILE = "cal.dcal";
	public final static String CONFIG_FILE = "conf.dcal";
	
	// ========================================================================

	public static long MEAN_WAIT_TIME = 2000;
	public static long HOLD_ON_TIME = 3000;
	
	// ========================================================================
	
	public static int max(int a, int b)
	{
		return (a > b) ? a : b;
	}
	
	// ========================================================================
	
	public static String arr2str(int[] arr)
	{
		StringBuilder res = new StringBuilder();
		
		for (int i : arr)
			res.append(i + " ");
		return res.toString();
	}
	
	// ========================================================================
	
	public static int[] str2arr(String s, String sep)
	{
		String[] ss = s.trim().split(sep);
		int[] res = new int[ss.length];

		try
		{
			for (int i = 0; i < ss.length; i++)
			{
				res[i] = Integer.parseInt(ss[i].trim());
			}
		}
		catch (NumberFormatException e)
		{
			System.out.println("Error reading input: " + e.getMessage());
			res = null;
		}
		return res;
	}
	
	// ========================================================================
	
	// returns true if all elements in b are contained in a
	public static boolean contains(int[] a, int[] b)
	{
		boolean found;
		int i, j;
	
		for (i = 0; i < b.length; i++)
		{
			found = false;
			for (j = 0; j < a.length; j++)
			{
				if (b[i] == a[j])
					found = true;
			}
			if (! found)
				return false;
		}
		return true;
	}
	
	// ========================================================================
	
	public static boolean equals(int[] a, int[] b)
	{
		return (contains(a, b) && contains(b, a));
	}
	
	// ========================================================================
	
	// returns true if b is contained in a
	public static boolean contains(int[] a, int b)
	{
		for (int j = 0; j < a.length; j++)
		{
			if (b == a[j])
				return true;
		}
		return false;
	}
	
	// ========================================================================
	
	public static int[] add(int[] a, int b)
	{
		if (contains(a, b))
			return a;

		int ret[] = new int[a.length + 1];
		System.arraycopy(a, 0, ret, 0, a.length);
		ret[a.length] = b;
		
		return ret;
	}

	// ========================================================================
	
	public static long getRandomWaitMS()
	{
		Random r = new Random();
		return MEAN_WAIT_TIME * (-1) * (long) Math.log(r.nextDouble());
	}
	
	// =============================================================================

}
