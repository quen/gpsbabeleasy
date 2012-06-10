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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.prefs.*;
import java.util.regex.*;

import javax.script.*;
import javax.swing.*;

/**
 * Component that lets people select what happens after conversion completes
 * to the original and new file.
 */
public class AfterOptions extends JPanel
{
	private final static String IN_TEXT = "Input file move location";
	private final static String OUT_TEXT = "Output file location";

	private final static String IN_DO_NOTHING_TEXT = "Leave input file alone";
	private final static String IN_TRASH_TEXT = "Move input file to trash (via desktop)";
	private final static String SELECT_FOLDER_TEXT = "Move to folder...";

	private final static String OUT_SAME_FOLDER_TEXT = "Same folder as input file";

	/** Extra space required either side of combo boxes by theme */
	private final static int UI_COMBO_EDGE = 5;

	private final static int UI_SMALL_TEXT_ANTIPAD = 3;

	private GpsBabelEasy easy;
	private JComboBox inOption, outOption;

	private Object selectedSynch = new Object();
	private InFileAction inSelected;
	private OutOption outSelected;

	private boolean ignoreComboEvents = false;

	/**
	 * @param easy Owner
	 */
	public AfterOptions(GpsBabelEasy easy)
	{
		super(new GridLayout(1, 2, GpsBabelEasy.UI_SPACING - 2 * UI_COMBO_EDGE, 0));
		this.easy = easy;
		setBorder(BorderFactory.createEmptyBorder(
			GpsBabelEasy.UI_SPACING - UI_SMALL_TEXT_ANTIPAD,
			GpsBabelEasy.UI_SPACING - UI_COMBO_EDGE,
			0, GpsBabelEasy.UI_SPACING - UI_COMBO_EDGE));

		JPanel left = new JPanel(new BorderLayout(1, 1)),
			right = new JPanel(new BorderLayout(1, 1));
		add(left);
		add(right);

		JLabel inLabel = new JLabel(IN_TEXT), outLabel = new JLabel(OUT_TEXT);
		inLabel.putClientProperty("JComponent.sizeVariant", "small");
		outLabel.putClientProperty("JComponent.sizeVariant", "small");
		inLabel.setBorder(BorderFactory.createEmptyBorder(0, UI_COMBO_EDGE, 0, 0));
		outLabel.setBorder(BorderFactory.createEmptyBorder(0, UI_COMBO_EDGE, 0, 0));
		left.add(inLabel, BorderLayout.NORTH);
		right.add(outLabel, BorderLayout.NORTH);

		DefaultComboBoxModel inModel = new ComboBoxSeparatorModel(),
			outModel = new ComboBoxSeparatorModel();

		inOption = new JComboBox(inModel);
		outOption = new JComboBox(outModel);
		inOption.setMaximumRowCount(10);
		outOption.setMaximumRowCount(10);
		ListCellRenderer renderer = new ComboBoxSeparatorRenderer(inOption.getRenderer());
		inOption.setRenderer(renderer);
		outOption.setRenderer(renderer);
		left.add(inOption,BorderLayout.SOUTH);
		right.add(outOption,BorderLayout.SOUTH);

		// Listen for selections
		inOption.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				if(e.getStateChange() == ItemEvent.SELECTED)
				{
					if(!ignoreComboEvents)
					{
						inClicked(e.getItem());
					}
				}
			}
		});
		outOption.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				if(e.getStateChange() == ItemEvent.SELECTED)
				{
					if(!ignoreComboEvents)
					{
						outClicked(e.getItem());
					}
				}
			}
		});

		// Set up options
		updateCombos();
	}

	/**
	 * @return Selected action for input file handling.
	 */
	public InFileAction getInAction()
	{
		return inSelected;
	}

	/**
	 * @return Target location for output files or null to use same as input
	 */
	public File getOutFolder()
	{
		if(outSelected instanceof TargetTo)
		{
			return new File(((TargetTo)outSelected).folder);
		}
		else
		{
			return null;
		}
	}

	private final static int MRU_LENGTH = 3;

	private void inClicked(Object selected)
	{
		InOption option = (InOption)selected;
		if(option instanceof InSelectFolder)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					// Set to select directories
					System.setProperty("apple.awt.fileDialogForDirectories", "true");
					FileDialog fd = new FileDialog(easy);
					// Enter modal dialog
					fd.setVisible(true);
					// Modal dialog finished
					System.setProperty("apple.awt.fileDialogForDirectories", "false");
					if(fd.getDirectory() != null && fd.getFile() != null)
					{
						File selected = new File(fd.getDirectory(), fd.getFile());
						try
						{
							// Check path
							selected = selected.getCanonicalFile();

							// Add folder to MRU and select it
							addFolderToMru(inOption, selected);
							updateCombos();
							inOption.setSelectedIndex(2);
							return;
						}
						catch(IOException e)
						{
							JOptionPane.showMessageDialog(easy, e.getMessage(),
								"Error", JOptionPane.ERROR_MESSAGE);
						}
					}
					// Update selection
					updateCombos();
				}
			});
		}
		else
		{
			synchronized(selectedSynch)
			{
				inSelected = (InFileAction)option;
			}
		}
	}

	private void outClicked(Object selected)
	{
		OutOption option = (OutOption)selected;
		if(option instanceof OutSelectFolder)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					// Set to select directories
					System.setProperty("apple.awt.fileDialogForDirectories", "true");
					FileDialog fd = new FileDialog(easy);
					// Enter modal dialog
					fd.setVisible(true);
					// Modal dialog finished
					System.setProperty("apple.awt.fileDialogForDirectories", "false");
					if(fd.getDirectory() != null && fd.getFile() != null)
					{
						File selected = new File(fd.getDirectory(), fd.getFile());
						try
						{
							// Check path
							selected = selected.getCanonicalFile();

							// Add folder to MRU and select it
							addFolderToMru(outOption, selected);
							updateCombos();
							outOption.setSelectedIndex(1);
							return;
						}
						catch(IOException e)
						{
							JOptionPane.showMessageDialog(easy, e.getMessage(),
								"Error", JOptionPane.ERROR_MESSAGE);
						}
					}
					// Update selection
					updateCombos();
				}
			});
		}
		else
		{
			synchronized(selectedSynch)
			{
				outSelected = option;
			}
		}
	}

	private void addFolderToMru(JComboBox box, File folder)
	{
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());

		// Get current MRU and see if it contains this folder
		int found = MRU_LENGTH - 1; // If not, treat like it was last
		String[] current = new String[MRU_LENGTH];
	  for(int i=0; i < MRU_LENGTH; i++)
	  {
			current[i] = prefs.get(getPrefKey(box, i), null);
			if(folder.toString().equals(current[i]))
			{
				found = i;
	  	}
	  }

	  // Move down existing values
	  for(int i=found; i>0; i--)
	  {
	  	current[i] = current[i-1];
		}

	  // Put this value in 0
	  current[0] = folder.toString();

	  // Save list
	  for(int i = 0; i < MRU_LENGTH; i++)
	  {
	  	String key = getPrefKey(box, i);
	  	if(current[i] == null)
	  	{
	  		prefs.remove(key);
	  	}
	  	else
	  	{
	  		prefs.put(key, current[i]);
	  	}
	  }

	  // Save prefs
	  try
		{
			prefs.flush();
		}
		catch(BackingStoreException e)
		{
			easy.fatalError(e);
		}
	}

	/**
	 * Called when a conversion happens; remember the dropdown choices
	 */
	public void remember()
	{
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		synchronized(selectedSynch)
		{
			prefs.put("afterAction.in", inSelected.getType());
			if(outSelected instanceof TargetTo)
			{
				prefs.put("afterAction.out", "target");
			}
			else
			{
				prefs.remove("afterAction.out");
			}
		}
		try
		{
			prefs.flush();
		}
		catch(BackingStoreException e)
		{
			easy.fatalError(e);
		}
	}

	/**
	 * Base class for everything to add to the input combo.
	 */
	private static abstract class InOption
	{
		/**
		 * @return A type code used in preferences
		 */
		abstract String getType();
	}

	/**
	 * Action on the input files after successful conversion.
	 */
	abstract class InFileAction extends InOption
	{
		/**
		 * Called after conversion with a list of all the input files that were
		 * successfully converted.
		 * @param usedFile File that converted OK and should be acted on
		 */
		abstract void afterConversion(File usedFile);

		/**
		 * @param usedFile Original file
		 * @param folder Destination folder
		 * @return New file in destination folder (may have different name)
		 * @throws IOException If failure when renaming
		 */
		protected File renameCollision(File usedFile, File folder) throws IOException
		{
			// Check for file collision
			File inTarget = new File(folder, usedFile.getName());
			if(inTarget.exists())
			{
				String newName;
				for(int i=2;; i++)
				{
					newName = usedFile.getName();
					if(newName.contains("."))
					{
						newName = newName.replaceFirst("(\\.[^.]*)$", " (" + i + ")$1");
					}
					else
					{
						newName += " (" + i + ")";
					}
					inTarget = new File(folder, newName);
					if(!inTarget.exists())
					{
						break;
					}
				}
			}
			if(!usedFile.renameTo(inTarget))
			{
				// Rename can fail across filesystems, so try a copy
				try
				{
					renameWithCopy(usedFile, inTarget);
				}
				catch(IOException e)
				{
					IOException changedMessage = new IOException(
						"Failed to move file from " + usedFile + " to " + inTarget);
					changedMessage.initCause(e);
					throw changedMessage;
				}
			}
			return inTarget;
		}
	}

	/**
	 * Renames a file by copying it and deleting the original.
	 * @param source Source file
	 * @param target Target file
	 * @throws IOException Any error
	 */
	private static void renameWithCopy(File source, File target) throws IOException
	{
		boolean ok = false;
		try
		{
			// Copy file.
			FileInputStream input = new FileInputStream(source);
			FileOutputStream output = new FileOutputStream(target);
			byte[] buffer = new byte[65536];
			while(true)
			{
				int read = input.read(buffer);
				if(read == -1)
				{
					break;
				}
				output.write(buffer, 0, read);
			}
			input.close();
			output.close();

			// Update date.
			if(!target.setLastModified(source.lastModified()))
			{
				throw new IOException("Date set failed");
			}

			// Delete original file.
			if(!source.delete())
			{
				throw new IOException("Delete failed");
			}
			ok = true;
		}
		finally
		{
			// If anything failed, try to delete the target file so this operation is
			// atomic, as best we can manage.
			if(!ok)
			{
				target.delete();
			}
		}
	}

	/**
	 * Input action: leave files alone.
	 */
	private class DoNothing extends InFileAction
	{
		@Override
		public String toString()
		{
			return IN_DO_NOTHING_TEXT;
		}

		@Override
		void afterConversion(File usedFile)
		{
			// Do nothing!
		}

		@Override
		String getType()
		{
			return "nothing";
		}
	}

	/**
	 * Input action: move files to desktop then trash. (Move to desktop is to
	 * make sure we don't add files to trash on a removable device, which takes
	 * up limited space on that device and is hard to spot.)
	 */
	private class Trash extends InFileAction
	{
		@Override
		public String toString()
		{
			return IN_TRASH_TEXT;
		}

		@Override
		void afterConversion(File usedFile)
		{
			// Get desktop (note: pretty sure these names are language-independent,
			// i.e. even on a French or Chinese installation, /Desktop ought to work).
			File desktop = new File(System.getProperty("user.home") + "/Desktop");

			// Move file onto desktop
			try
			{
				usedFile = renameCollision(usedFile, desktop);
			}
			catch(IOException e)
			{
				easy.fatalError(e);
				return;
			}

			runApplescript("tell application \"Finder\"\n" +
				"delete POSIX file %1\n" +
				"end tell",
				usedFile.getAbsolutePath());
		}

		@Override
		String getType()
		{
			return "trash";
		}
	}

	/**
	 * Input action: move files to specified location.
	 */
	private class MoveTo extends InFileAction
	{
		private String folder;

		private MoveTo(String folder)
		{
			this.folder = folder;
		}

		@Override
		public String toString()
		{
			return folder;
		}

		@Override
		void afterConversion(File usedFile)
		{
			// Skip if it's already in that folder
			if(usedFile.getParentFile().equals(folder))
			{
				return;
			}

			// Handle collisions
			try
			{
				usedFile = renameCollision(usedFile, new File(folder));
			}
			catch(IOException e)
			{
				easy.fatalError(e);
				return;
			}

			// Move file
			runApplescript("tell application \"Finder\" to move POSIX file %1 to POSIX file %2",
				usedFile.getAbsolutePath(), new File(folder).getAbsolutePath());
		}

		@Override
		String getType()
		{
			return "move";
		}
	}

	/**
	 * Quotes and escapes a string for use in AppleScript
	 * @param input Input string
	 * @return String surrounded by double quotes and escaped
	 */
	private static String applescriptQuote(String input)
	{
		return "\"" + input.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	private final static Pattern REGEX_PERCENT = Pattern.compile("%([0-9]{1,2})");

	/**
	 * Runs some AppleScript.
	 * @param script Script to run
	 * @param params String parameters to be inserted in %1, %2, etc within script
	 */
	private void runApplescript(String script, String... params)
	{
		try
		{
			// Replace all the % placeholders in the script
			StringBuffer out = new StringBuffer();
			Matcher m = REGEX_PERCENT.matcher(script);
			while(m.find())
			{
				int num = Integer.parseInt(m.group(1));
				if(num < 1 || num > params.length)
				{
					throw new ScriptException("Undefined %" + num + " parameter in script");
				}
				String replacement = applescriptQuote(params[num-1]);
				m.appendReplacement(out, replacement);
			}
			m.appendTail(out);
			script = out.toString();

			// Run script
			ScriptEngineManager manager = new ScriptEngineManager();
			ScriptEngine engine = manager.getEngineByName("AppleScript");
			engine.eval(script);
		}
		catch(ScriptException e)
		{
			easy.fatalError(e);
		}
	}

	/**
	 * Input combo option; select a new folder.
	 */
	private static class InSelectFolder extends InOption
	{
		@Override
		public String toString()
		{
			return SELECT_FOLDER_TEXT;
		}

		@Override
		String getType()
		{
			return "selectfolder";
		}
	}

	/**
	 * Base class for everything in the output option.
	 */
	private static abstract class OutOption
	{
	}

	/**
	 * Output option: same folder as input.
	 */
	private static class SameFolder extends OutOption
	{
		@Override
		public String toString()
		{
			return OUT_SAME_FOLDER_TEXT;
		}
	}

	/**
	 * Output action: save files to specified location.
	 */
	private static class TargetTo extends OutOption
	{
		private String folder;

		private TargetTo(String folder)
		{
			this.folder = folder;
		}

		@Override
		public String toString()
		{
			return folder;
		}
	}

	/**
	 * Output combo option; select a new folder.
	 */
	private static class OutSelectFolder extends OutOption
	{
		@Override
		public String toString()
		{
			return SELECT_FOLDER_TEXT;
		}
	}

	private void updateCombos()
	{
		try
		{
			ignoreComboEvents = true;
			Preferences prefs = Preferences.userNodeForPackage(this.getClass());

			// Do input options first
			ComboBoxSeparatorModel inModel = (ComboBoxSeparatorModel)inOption.getModel();

			// Remove existing options
			inModel.removeAllElements();

			// Add default options
			inModel.addElement(new DoNothing());
			inModel.addElement(new Trash());

			// Add MRU locations
			boolean gotFolder = false;
		  for(int i=0; i < MRU_LENGTH; i++)
		  {
				String recent = prefs.get(getPrefKey(inOption, i), null);
		  	if(recent != null)
		  	{
		  		if(!gotFolder)
		  		{
		  			inModel.addSeparator();
		  			gotFolder = true;
		  		}
		  		inModel.addElement(new MoveTo(recent));
		  	}
		  }

			// Add 'new folder' option
			inModel.addSeparator();
			inModel.addElement(new InSelectFolder());

			if(inSelected != null)
			{
				// Preserve selection if specified...
				for(int i=0; i<inModel.getSize(); i++)
				{
					InOption possible = (InOption)inModel.getElementAt(i);
					if(possible.toString() == inSelected.toString())
					{
						inOption.setSelectedIndex(i);
						break;
					}
				}
			}
			else
			{
				// Selection was not specified, so get from options
				String type = prefs.get("afterAction.in", "nothing");
				if(type.equals("nothing"))
				{
					inOption.setSelectedIndex(0);
				}
				else if(type.equals("trash"))
				{
					inOption.setSelectedIndex(1);
				}
				else
				{
					// Must be top MRU folder
					inOption.setSelectedIndex(2);
				}
				synchronized(selectedSynch)
				{
					inSelected = (InFileAction)inOption.getSelectedItem();
				}
			}

			// Do output options now
			ComboBoxSeparatorModel outModel = (ComboBoxSeparatorModel)outOption.getModel();

			// Remove existing options
			outModel.removeAllElements();

			// Add default options
			outModel.addElement(new SameFolder());

			// Add MRU locations
			gotFolder = false;
		  for(int i=0; i < MRU_LENGTH; i++)
		  {
				String recent = prefs.get(getPrefKey(outOption, i), null);
		  	if(recent != null)
		  	{
		  		if(!gotFolder)
		  		{
		  			outModel.addSeparator();
		  			gotFolder = true;
		  		}
		  		outModel.addElement(new TargetTo(recent));
		  	}
		  }

			// Add 'new folder' option
		  outModel.addSeparator();
		  outModel.addElement(new OutSelectFolder());

			if(outSelected != null)
			{
				// Preserve selection if specified...
				for(int i=0; i<outModel.getSize(); i++)
				{
					OutOption possible = (OutOption)outModel.getElementAt(i);
					if(possible.toString() == outSelected.toString())
					{
						outOption.setSelectedIndex(i);
						break;
					}
				}
			}
			else
			{
				// Selection was not specified, so get from options
				String type = prefs.get("afterAction.out", null);
				if(type == null)
				{
					outOption.setSelectedIndex(0);
				}
				else
				{
					// Must be top MRU folder
					outOption.setSelectedIndex(2);
				}
				synchronized(selectedSynch)
				{
					outSelected = (OutOption)outOption.getSelectedItem();
				}
			}
		}
		finally
		{
			ignoreComboEvents = false;
		}
	}

	/**
	 * @param box Box
	 * @param i Index
	 * @return Prefs key string
	 */
	private String getPrefKey(JComboBox box, int i)
	{
		String key = "afterFolder." + (box == inOption ? "in" : "out") + "." + i;
		return key;
	}
}
