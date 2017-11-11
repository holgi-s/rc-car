// Copyright © 2017 Shawn Baker using the MIT License.
package ca.frozen.library.classes;

import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public final class Log
{
	// instance variables
	private static String tag = null;
	private static LogLevel level = LogLevel.Debug;
	private static int maxFileSize = 1024 * 1024;
	private static File file1 = null;
	private static File file2 = null;
	private static File currFile = null;
	private static FileWriter fOut = null;
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	//******************************************************************************
	// init
	//******************************************************************************
	public synchronized static void init(Context context, String tag, String baseFileName)
	{
		// save the tag
		Log.tag = tag;

		if (fOut == null)
		{
			// get the path to the files directory
			File dir = new File(context.getFilesDir(), "logs");
			if (!dir.exists())
			{
				dir.mkdirs();
			}

			// get the two log files
			file1 = new File(dir, baseFileName + "1.log");
			long file1Time = file1.exists() ? file1.lastModified() : 0;
			file2 = new File(dir, baseFileName + "2.log");
			long file2Time = file2.exists() ? file2.lastModified() : 0;

			// open or create the current log file
			try
			{
				currFile = (file1Time >= file2Time) ? file1 : file2;
				if (!currFile.exists())
				{
					currFile.createNewFile();
				}
				fOut = new FileWriter(currFile, true);
			}
			catch (FileNotFoundException ex)
			{
				fOut = null;
			}
			catch (IOException ex)
			{
				fOut = null;
			}
		}
	}

	//******************************************************************************
	// init
	//******************************************************************************
	public static void init(Context context, String baseFileName)
	{
		init(context, null, baseFileName);
	}

	//******************************************************************************
	// clear
	//******************************************************************************
	public synchronized static void clear()
	{
		if (fOut != null)
		{
			try
			{
				fOut.close();
				if (file1.exists())
				{
					file1.delete();
				}
				if (file2.exists())
				{
					file2.delete();
				}
				currFile = file1;
				currFile.createNewFile();
				fOut = new FileWriter(currFile);
			}
			catch (IOException ex)
			{
			}
		}
	}

	//******************************************************************************
	// getLevel
	//******************************************************************************
	public static LogLevel getLevel()
	{
		return level;
	}

	//******************************************************************************
	// setLevel
	//******************************************************************************
	public static void setLevel(LogLevel level)
	{
		Log.level = level;
	}

	//******************************************************************************
	// getTag
	//******************************************************************************
	public static String getTag()
	{
		return tag;
	}

	//******************************************************************************
	// setTag
	//******************************************************************************
	public static void setTag(String tag)
	{
		Log.tag = tag;
	}

	//******************************************************************************
	// getMaxFileSize
	//******************************************************************************
	public static int getMaxFileSize()
	{
		return maxFileSize;
	}

	//******************************************************************************
	// setMaxFileSize
	//******************************************************************************
	public static void setMaxFileSize(int maxFileSize)
	{
		Log.maxFileSize = maxFileSize;
	}

	//******************************************************************************
	// getFile1
	//******************************************************************************
	public static File getFile1()
	{
		return file1;
	}

	//******************************************************************************
	// getFile2
	//******************************************************************************
	public static File getFile2()
	{
		return file2;
	}

	//******************************************************************************
	// write
	//******************************************************************************
	public synchronized static void write(LogLevel level, String message)
	{
		try
		{
			if (fOut != null && level.ordinal() <= Log.level.ordinal())
			{
				// switch files if the current one is full
				if (currFile.length() >= maxFileSize)
				{
					fOut.close();
					currFile = (currFile == file1) ? file2 : file1;
					if (currFile.exists())
					{
						currFile.delete();
					}
					currFile.createNewFile();
					fOut = new FileWriter(currFile);
				}

				// write the log message
				String msg = dateFormat.format(Calendar.getInstance().getTime()) + " - " + level.name() + " - ";
				if (tag != null)
				{
					msg += tag + " - ";
				}
				msg += message + "\n";
				fOut.write(msg);
				fOut.flush();
			}
		}
		catch (IOException ex)
		{
			android.util.Log.d("write", ex.toString());
		}
	}

	//******************************************************************************
	// debug
	//******************************************************************************
	public static void debug(String message)
	{
		write(LogLevel.Debug, message);
	}

	//******************************************************************************
	// info
	//******************************************************************************
	public static void info(String message)
	{
		write(LogLevel.Info, message);
	}

	//******************************************************************************
	// warning
	//******************************************************************************
	public static void warning(String message)
	{
		write(LogLevel.Warning, message);
	}

	//******************************************************************************
	// error
	//******************************************************************************
	public static void error(String message)
	{
		write(LogLevel.Error, message);
	}

	//******************************************************************************
	// fatal
	//******************************************************************************
	public static void fatal(String message)
	{
		write(LogLevel.Fatal, message);
	}

	//******************************************************************************
	// LogLevel
	//******************************************************************************
	public enum LogLevel
	{
		Fatal,
		Error,
		Warning,
		Info,
		Debug
	}
}
