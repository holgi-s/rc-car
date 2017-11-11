// Copyright © 2016-2017 Shawn Baker using the MIT License.
package ca.frozen.rpicameraviewer.classes;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public class Utils
{

	//******************************************************************************
	// getLocalIpAddress
	//******************************************************************************
	public static String getLocalIpAddress()
	{
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();)
			{
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
				{
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address)
					{
						return inetAddress.getHostAddress();
					}
				}
			}
		} catch (SocketException ex)
		{
			ex.printStackTrace();
		}
		return null;
	}

	//******************************************************************************
	// getBaseIpAddress
	//******************************************************************************
	public static String getBaseIpAddress()
	{
		String ipAddress = getLocalIpAddress();
		int i = ipAddress.lastIndexOf('.');
		return ipAddress.substring(0, i + 1);
	}

	//******************************************************************************
	// getFullAddress
	//******************************************************************************
	public static String getFullAddress(String baseAddress, int port)
	{
		String address = baseAddress;
		int i = address.indexOf("://");
		i = address.indexOf("/", (i != -1) ? (i + 3) : 0);
		if (i != -1)
		{
			address = address.substring(0, i) + ":" + port + address.substring(i);
		}
		else
		{
			address += ":" + port;
		}
		return address;
	}

	//******************************************************************************
	// getHttpAddress
	//******************************************************************************
	public static String getHttpAddress(String baseAddress)
	{
		String address = baseAddress;
		if (!address.startsWith("http://"))
		{
			address = "http://" + address;
		}
		return address;
	}


}
