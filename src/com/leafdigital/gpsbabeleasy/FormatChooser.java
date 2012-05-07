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
import java.util.*;
import java.util.prefs.*;
import java.util.regex.*;

import javax.swing.*;

/**
 * Component that lets people select input and output formats.
 */
public class FormatChooser extends JPanel
{
	private final static String IN_TEXT = "Input format";
	private final static String OUT_TEXT = "Output format";

	/** Extra space required either side of combo boxes by theme */
	private final static int UI_COMBO_EDGE = 5;

	private final static int UI_SMALL_TEXT_ANTIPAD = 3;

	private GpsBabelEasy easy;
	private JComboBox inFormat, outFormat;

	private TreeSet<Format> formatSet;

	/**
	 * @param easy Owner
	 */
	public FormatChooser(GpsBabelEasy easy)
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

		inFormat = new JComboBox(inModel);
		outFormat = new JComboBox(outModel);
		inFormat.setMaximumRowCount(25);
		outFormat.setMaximumRowCount(25);
		ListCellRenderer renderer = new ComboBoxSeparatorRenderer(inFormat.getRenderer());
		inFormat.setRenderer(renderer);
		outFormat.setRenderer(renderer);
		left.add(inFormat,BorderLayout.SOUTH);
		right.add(outFormat,BorderLayout.SOUTH);

		// Set temporary data
		inModel.addElement("\u00a0");
		outModel.addElement("\u00a0");
		inFormat.setEnabled(false);
		outFormat.setEnabled(false);

		// Acquire actual data in separate thread
		(new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				initFormats();
			}
		})).start();
	}

	/**
	 * Represents a format for input/output.
	 */
	public static class Format implements Comparable<Format>
	{
		private String code, display;

		private Format(String code, String display)
		{
			this.code = code;
			this.display = display;
		}

		@Override
		public int compareTo(Format other)
		{
			return display.compareTo(other.display);
		}

		@Override
		public String toString()
		{
			return display;
		}

		/**
		 * @return Code of format
		 */
		public String getCode()
		{
			return code;
		}
	}

	private final static int MRU_LENGTH = 3;

	/**
	 * @return Input format
	 */
	public Format getInFormat()
	{
		return (Format)inFormat.getSelectedItem();
	}

	/**
	 * @return Output format
	 */
	public Format getOutFormat()
	{
		return (Format)outFormat.getSelectedItem();
	}

	/**
	 * Called when a conversion is actually done. Updates formats in preferences
	 */
	public void remember()
	{
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());

		JComboBox[] boxes = { inFormat, outFormat };
		for(JComboBox box : boxes)
		{
			// Work out new MRU list
			String[] after = new String[MRU_LENGTH];
			after[0] = ((Format)box.getSelectedItem()).getCode();
			int afterIndex = 1;
		  for(int i=0; i < MRU_LENGTH; i++)
		  {
		  	String recent = prefs.get(getPrefKey(box, i), null);

		  	// Stop as soon as there's a missing one
		  	if(recent == null)
		  	{
		  		break;
		  	}

		  	// Ignore if it's the same as the one we put in there
		  	if(after[0].equals(recent))
		  	{
		  		continue;
		  	}

		  	// OK, add it
		  	after[afterIndex++] = recent;

		  	// Stop if we're out of space
		  	if(afterIndex == MRU_LENGTH)
		  	{
		  		break;
		  	}
		  }

		  // Save new MRU list
		  for(int i=0; i < MRU_LENGTH; i++)
		  {
		  	String key = getPrefKey(box, i);
		  	if(after[i] == null)
		  	{
		  		prefs.remove(key);
		  	}
		  	else
		  	{
		  		prefs.put(key, after[i]);
		  	}
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

		updateCombos();
	}

	private void updateCombos()
	{
		// Get most recently used formats
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());

		// Use list to update combo boxes
		JComboBox[] boxes = { inFormat, outFormat };
		for(JComboBox box : boxes)
		{
			ComboBoxSeparatorModel model = (ComboBoxSeparatorModel)box.getModel();
			model.removeAllElements();

			// Add recently used formats
		  for(int i=0; i < MRU_LENGTH; i++)
		  {
				String recent = prefs.get(getPrefKey(box, i), i == 0 ? "gpx" : null);
		  	if(recent != null)
		  	{
		  		// Find specified format
		  		for(Format format : formatSet)
		  		{
		  			if(format.getCode().equals(recent))
		  			{
		  				model.addElement(format);
		  				break;
		  			}
		  		}
		  	}
		  }
			model.addSeparator();

			// Add all formats alphabetically
			for(Format format : formatSet)
			{
				model.addElement(format);
			}

			// Combo box is enabled now
			box.setEnabled(true);
		}
	}

	/**
	 * @param box Box
	 * @param i Index
	 * @return Prefs key string
	 */
	private String getPrefKey(JComboBox box, int i)
	{
		String key = "recentFormat." + (box == inFormat ? "in" : "out") + "." + i;
		return key;
	}

	/**
	 * Separate thread that gets the list of formats.
	 */
	private void initFormats()
	{
		try
		{
			// Run GPSBabel and parse result to get list of formats
			GpsBabelEasy.RunResult result = easy.runGpsBabel("-h");
			if(result.getResult() != 0)
			{
				throw new Exception(
					"Unexpected return value for gpsbabel -h: " + result.getResult());
			}
			String out = result.getStdout();
			int pos = 0;
			boolean inFileTypes = false;
			Pattern FORMAT_LINE = Pattern.compile("^(\\s+)([^\\s]+)\\s+(.*)$");
			formatSet = new TreeSet<Format>();
			while(pos < out.length())
			{
				// Get next line
				int lf = out.indexOf('\n', pos);
				if(lf == -1)
				{
					lf = out.length();
				}
				String line = out.substring(pos, lf);
				// Trim whitespace at end of line only (this will mean it'll work in
				// Windows if there's a CR)
				line = line.replaceFirst("\\s+$", "");
				pos = lf + 1;

				// Process line
				if(!inFileTypes)
				{
					// Look for file types header: any line containing words '-i' and '-o'
					// terminated by a colon
					if(line.matches(".*-i.*-o.*:$"))
					{
						inFileTypes = true;
					}
				}
				else
				{
					// Exit loop on blank line
					if(line.trim().equals(""))
					{
						break;
					}

					// Match line
					Matcher m = FORMAT_LINE.matcher(line);
					if(m.matches())
					{
						if (m.group(1).equals("\t"))
						{
							formatSet.add(new Format(m.group(2), m.group(3)));
						}
					}
				}
			}

			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					updateCombos();
				}
			});
		}
		catch(Throwable t)
		{
			easy.fatalError(t);
		}
	}

}
