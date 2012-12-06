package cal;

public class Address
{
	// ========================================================================
	
	public static final int DEFAULT_PORT = 7007;
	
	// ========================================================================
	
	private String ipAddress;
	private int portNumber;
	
	// ========================================================================
	
	public Address(String ip, int port)
	{
		ipAddress = ip;
		portNumber = port;
	}
	
	// ========================================================================
	
	public Address(String ip)
	{
		this(ip, DEFAULT_PORT);
	}
	
	// ========================================================================
	
	public String getIP()
	{
		return ipAddress;
	}

	// ========================================================================
	
	public int getPort()
	{
		return portNumber;
	}

	// ========================================================================
	
	public String toString()
	{
		return String.format("%s:%d", ipAddress, portNumber);
	}
	
	// ========================================================================
	
	public static Address parseAddressFromString(String str)
	{
		int idx = str.indexOf(":");
		
		if (idx < 0)
		{
			return new Address(str);
		}
		else
		{
			int port = Integer.parseInt(str.substring(idx + 1));
			String ip = str.substring(0, idx);
			return new Address(ip, port);
		}
	}
	
	// ========================================================================
}
