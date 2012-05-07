/*
This file is part of leafdigital GPSBabel easy converter.

GPSBabel easy converter is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

GPSBabel easy converter is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GPSBabel easy converter. If not, see <http://www.gnu.org/licenses/>.

Copyright 2012 Samuel Marshall.
*/
package com.leafdigital.gpsbabeleasy;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import com.apple.eawt.*;
import com.apple.eawt.AppEvent.AboutEvent;

import com.leafdigital.gpsbabeleasy.AfterOptions.InFileAction;
import com.leafdigital.gpsbabeleasy.FormatChooser.Format;
import com.leafdigital.gpsbabeleasy.ProgressDisplay.ProgressTableModel;

/** 
 * Main application window.
 */
public class GpsBabelEasy extends JFrame
{
	/**
	 * Application title.
	 */
	public final static String TITLE_TEXT = "GPSBabel easy converter";
	
	/**
	 * Space normally used between UI components.
	 */
	public final static int UI_SPACING = 10;
	private final static int UI_SPACING_MINI_LABEL_ANTIPAD = 2;
	private final static int UI_SPACING_COMBO_ANTIPAD = 4;
	
	private int lockCount;
	private Object closeSynch = new Object();
	
	private String gpsBabelPath;

	private ProgressDisplay progress;
	private FormatChooser chooser;
	private AfterOptions options;
	
	private String version, gpsBabelVersion;
	
	/** 
	 * @return Version
	 */
	public String getVersion()
	{
		return version;
	}
	
	/** 
	 * @return GPSBabel version
	 */
	public String getGpsBabelNameAndVersion()
	{
		return gpsBabelVersion;
	}

	/**
	 * Main method just opens window.
	 * @param args Parameters are ignored
	 */
	public static void main(String[] args)
	{
		// Construct on Swing thread
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				}
				catch(Exception e)
				{
					throw new Error(e);
				}
				new GpsBabelEasy();
			}
		});
	}

	/**
	 * Contructor
	 */
	public GpsBabelEasy()
	{
		// Window basics
		super(TITLE_TEXT);

		// TODO Find this inside the app
		gpsBabelPath = "/Users/sam/Desktop/gpsbabel";
	
		setLayout(new BorderLayout());
		JPanel main = new JPanel(new BorderLayout(UI_SPACING, UI_SPACING - UI_SPACING_COMBO_ANTIPAD));
		getContentPane().add(main, BorderLayout.CENTER);

		// Format options
		chooser = new FormatChooser(this);
		main.add(chooser, BorderLayout.NORTH);
		
		// Drop area
		JPanel lower = new JPanel(new BorderLayout(UI_SPACING, 0));
		main.add(lower, BorderLayout.CENTER);
		DropArea drop = new DropArea(this);
		lower.add(new BorderWrapper(drop, 0, UI_SPACING, 0, UI_SPACING),
			BorderLayout.NORTH);

		// Get GPSBabel details
		gpsBabelVersion = "GPSBabel";
		String gpsBabelDate = "";
		try
		{
			RunResult result = runGpsBabel("-V");
			gpsBabelVersion = result.getStdout().trim().replace(" Version ", " ").trim();
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(new File(gpsBabelPath).lastModified());
			gpsBabelDate = c.get(Calendar.YEAR) + "";
		}
		catch(IOException e)
		{
			// Ignore error this time
		}

		JPanel lower2 = new JPanel(new BorderLayout(UI_SPACING, UI_SPACING - UI_SPACING_MINI_LABEL_ANTIPAD));
		lower.add(lower2, BorderLayout.CENTER);

		// Options about what happens after conversion
		options = new AfterOptions(this);
		lower2.add(options, BorderLayout.NORTH);
		
		// Results display table
		progress = new ProgressDisplay(this);
		lower2.add(new BorderWrapper(progress, 0, UI_SPACING, 0, UI_SPACING),
			BorderLayout.CENTER);

		// Copyright
		InputStream versionStream = getClass().getResourceAsStream("version.txt");
		version = "(dev)";
		if(versionStream != null)
		{
			try
			{
				version = new BufferedReader(new InputStreamReader(versionStream, "UTF-8")).readLine();
				versionStream.close();
			}
			catch(IOException e)
			{
				// Come on, this can't happen
				e.printStackTrace();
			}
		}
		final JLabel copyright = new JLabel(
			gpsBabelVersion + " \u00a9 " + gpsBabelDate + " Robert Lipe, gpsbabel.org. " +
			"Easy converter " + version + " \u00a9 2012 Samuel Marshall / leafdigital.");
		copyright.putClientProperty("JComponent.sizeVariant", "mini");
		lower2.add(new BorderWrapper(copyright, 0, UI_SPACING, UI_SPACING, UI_SPACING), BorderLayout.SOUTH);
		
		// Close handling
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				if(canClose())
				{
					System.exit(0);
				}
			}
		});
		
		// Set up Mac-specific stuff
		Application.getApplication().setAboutHandler(new AboutHandler()
		{
			@Override
			public void handleAbout(AboutEvent e)
			{
				new AboutDialog(GpsBabelEasy.this);
			}
		});
		Application.getApplication().setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS);

		// Finish off window
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	/**
	 * @return True if app can currently be closed
	 */
	private boolean canClose()
	{
		synchronized(closeSynch)
		{
			return lockCount == 0;
		}
	}
	
	/**
	 * Locks close, preventing the user from closing the app.
	 */
	public void lockClose()
	{
		synchronized(closeSynch)
		{
			if(lockCount == 0)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						getRootPane().putClientProperty("Window.documentModified", true);
					}
				});
			}
			lockCount++;			
		}		
	}

	/**
	 * Stops locking close.
	 * @throws IllegalStateException If close is not locked
	 */
	public void unlockClose() throws IllegalStateException
	{
		synchronized(closeSynch)
		{
			if(lockCount == 0)
			{
				throw new IllegalStateException("Already unlocked");
			}
			lockCount--;
			if(lockCount == 0)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						getRootPane().putClientProperty("Window.documentModified", false);
					}
				});
			}
		}		
	}
	
	/**
	 * Result of running the program.
	 */
	public static class RunResult
	{
		private int result;
		private String stdout, stderr;
		
		private RunResult(int result, String stdout, String stderr)
		{
			this.result = result;
			this.stdout = stdout;
			this.stderr = stderr;
		}
		
		/**
		 * @return Program result integer
		 */
		public int getResult()
		{
			return result;
		}
		
		/** 
		 * @return Standard output (treated as ISO 8859-1)
		 */
		public String getStdout()
		{
			return stdout;
		}
		
		/** 
		 * @return Standard err (treated as ISO 8859-1)
		 */
		public String getStderr()
		{
			return stderr;
		}
	}
	
	/**
	 * Runs the GPSBabel command-line tool and waits for it to complete.
	 * @param parameters Parameters
	 * @return Result of run
	 * @throws IOException If there's any problem
	 */
	public RunResult runGpsBabel(String... parameters) throws IOException
	{
		// Prepare full commandline
		String[] commandLine = new String[parameters.length + 1];
		commandLine[0] = gpsBabelPath;
		System.arraycopy(parameters, 0, commandLine, 1, parameters.length);
		Process process;
		try
		{
			process = Runtime.getRuntime().exec(commandLine, null);
		}
		catch(Exception e)
		{
			throw new IOException("Error running GPSBabel", e);
		}
		StreamEater stdout = new StreamEater(process.getInputStream());
		StreamEater stderr = new StreamEater(process.getErrorStream());
		int value;
		try
		{
			value = process.waitFor();
		}
		catch(InterruptedException e)
		{
			throw new IOException("Error waiting for GPSBabel result", e);
		}
		return new RunResult(value, stdout.getOut(), stderr.getOut());
	}
	
	/**
	 * Thread that eats the content of a stream from running a process.
	 */
	private static class StreamEater extends Thread
	{
		private static final int BUFFER_SIZE = 4096;
		private InputStream stream;
		
		private String out;
		private Throwable error;
		
		private StreamEater(InputStream stream)
		{
			this.stream = stream;
			start();
		}
		
		@Override
		public void run()
		{
			ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
			try
			{
				byte[] buffer = new byte[BUFFER_SIZE];
				while(true)
				{
					int read = stream.read(buffer);
					if(read == -1)
					{
						break;
					}
					outBytes.write(buffer, 0, read);
				}
			}
			catch(Throwable t)
			{
				setError(t);
			}
			finally
			{
				try
				{
					stream.close();
				}
				catch(Throwable t)
				{
					setError(t);
				}
				
				synchronized(this)
				{
					// Use ISO-8859-1 just so it can't error
					try
					{
						out = new String(outBytes.toByteArray(), "ISO-8859-1");
					}
					catch(Throwable t)
					{
						setError(t);
						out = "?";
					}
					notifyAll();
				}
			}
		}
		
		private void setError(Throwable t)
		{
			// Only take the first error
			if(error == null)
			{
				error = t;
			}
		}

		/**
		 * @return Output text as string
		 * @throws IOException If there was any error during the process
		 */
		private String getOut() throws IOException
		{
			synchronized(this)
			{
				while(out == null)
				{
					try
					{
						wait();
					}
					catch(InterruptedException e)
					{
						setError(e);
					}
				}
				if(error != null)
				{
					throw new IOException("Error reading process stream", error);
				}
				return out;
			}
		}		
	}
	
	/**
	 * Called if there is a fatal error in the system.
	 * @param t Error trace
	 */
	public void fatalError(final Throwable t)
	{
		// Make sure we're on the thread
		if(!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					fatalError(t);
				}
			});
			return;
		}
		
		JOptionPane.showMessageDialog(this,
			"An error occurred. The program will now exit. (Details in Console.)", 
			"GPSBabel easy convertor error", JOptionPane.ERROR_MESSAGE);
		System.err.println("GPSBabel easy converter fatal error:");
		t.printStackTrace();
		System.exit(0);
	}
	
	private LinkedList<Conversion> queue = new LinkedList<Conversion>();
	private boolean threadRunning;
	
	private static class Conversion
	{
		private FormatChooser.Format in, out;
		private InFileAction inAction;
		private File file, outFolder;
		private Conversion(Format in, Format out, File file, InFileAction inAction, File outFolder)
		{
			this.in = in;
			this.out = out;
			this.file = file;
			this.inAction = inAction;
			this.outFolder = outFolder;
		}
	}
	
	/**
	 * Called to initiate convert.
	 * @param files Files to convert
	 */
	public void convert(final File[] files)
	{
		// Add files to results list
		for(File file : files)
		{
			progress.getModel().addRow(file.getName(), file);
		}
		
		// Get settings
		final FormatChooser.Format inFormat = chooser.getInFormat(),
			outFormat = chooser.getOutFormat();
		chooser.remember();
		final AfterOptions.InFileAction inAction = options.getInAction();
		final File outFolder = options.getOutFolder();
		options.remember();
		
		// Add to queue
		boolean needsThread;
		synchronized(queue)
		{
			needsThread = !threadRunning;
			for(File file : files)
			{
				queue.add(new Conversion(inFormat, outFormat, file, inAction, outFolder));
			}
			if(needsThread)
			{
				lockClose();
				new ConversionThread();
			}
		}
	}
	
	private class ConversionThread extends Thread
	{
		private ConversionThread()
		{
			start();
			threadRunning = true;
		}
		
		@Override
		public void run()
		{
			while(true)
			{
				Conversion conversion;
				synchronized(queue)
				{
					if(queue.isEmpty())
					{
						threadRunning = false;
						unlockClose();
						return;
					}
					conversion = queue.removeFirst();
				}
				
				// Get result display row
				ProgressTableModel.Row row = progress.getModel().getRow(conversion.file);
				row.setProcessing();
				
				try
				{
					// Work out new name for the file
					File targetFolder = conversion.outFolder;
					if(targetFolder == null)
					{
						targetFolder = conversion.file.getParentFile();
					}
					File targetFile = new File(targetFolder,
						conversion.file.getName().replaceFirst("\\.[^.]+$", "") +
						"." + conversion.out.getCode());
					
					// Check they're not the same
					if(targetFile.getCanonicalFile().equals(conversion.file.getCanonicalFile()))
					{
						row.setFailure("Target file would have same name as source");
						continue;
					}
					
					// Check it doesn't exist already
					if(targetFile.exists())
					{
						row.setFailure("Target file already exists");
						continue;
					}
					
					// Check it doesn't exist already
					if(!targetFile.getParentFile().canWrite())
					{
						row.setFailure("Target file not writable");
						continue;
					}
					
					// Do convert
					RunResult result = runGpsBabel(
						"-r", "-t",
						"-i", conversion.in.getCode(), "-f", conversion.file.getAbsolutePath(),
						"-o", conversion.out.getCode(), "-F", targetFile.getAbsolutePath());
					
					System.out.println(result.getStdout());
					System.err.println(result.getStderr());
					// TODO Check output for failure!
					
					// After-conversion actions
					conversion.inAction.afterConversion(conversion.file);
					
					// OK, it succeeded
					row.setSuccess("\u2192 " + targetFile.getName());
				}
				catch(Throwable t)
				{
					row.setFailure("Error: " + t.getMessage());
					t.printStackTrace();
				}
			}
		}
	}
}
