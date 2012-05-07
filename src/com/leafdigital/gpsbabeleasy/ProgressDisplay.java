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

import javax.swing.*;
import javax.swing.table.*;

/** */
public class ProgressDisplay extends JPanel
{
	private final static String INPUT_TEXT = "Input file";
	private final static String RESULT_TEXT = "Result";
	private final static String PROCESSING_TEXT = "Processing...";
	private final static String[] COLUMNS =
	{
		"\u00a0",
		INPUT_TEXT,
		RESULT_TEXT
	};
	private final static int[] COLUMN_WIDTHS =
	{
		20,
		0,
		0
	};
	private final static int COLUMN_DEFAULT_MIN_WIDTH = 100;

	private final static String TICK = "\u2714";
	private final static String CROSS = "\u2718";

	private final static Color TICK_RGB = new Color(20, 128, 20);
	private final static Color CROSS_RGB = new Color(128, 20, 20);

	private ProgressTableModel model;

	/**
	 * @param easy Owner
	 */
	public ProgressDisplay(GpsBabelEasy easy)
	{
		super(new BorderLayout(GpsBabelEasy.UI_SPACING, GpsBabelEasy.UI_SPACING));

		DefaultTableColumnModel columnModel = new DefaultTableColumnModel();
		for(int i=0; i<COLUMNS.length; i++)
		{
			TableColumn column = new TableColumn(i);
			column.setHeaderValue(COLUMNS[i]);
			if(COLUMN_WIDTHS[i] != 0)
			{
				column.setMaxWidth(COLUMN_WIDTHS[i]);
				column.setMinWidth(COLUMN_WIDTHS[i]);
				column.setResizable(false);
			}
			else
			{
				column.setMinWidth(COLUMN_DEFAULT_MIN_WIDTH);
				column.setResizable(true);
			}

			if(i == 0)
			{
				column.setCellRenderer(new TickRenderer());
			}
			columnModel.addColumn(column);
		}

		model = new ProgressTableModel();
		JTable table = new JTable(model, columnModel);
		JScrollPane scrollPane = new JScrollPane(table,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane, BorderLayout.CENTER);
	}

	private static class TickRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column)
		{
			JLabel result = (JLabel)super.getTableCellRendererComponent(
				table, value, isSelected, hasFocus, row, column);
			result.setHorizontalAlignment(JLabel.HORIZONTAL);
			if(TICK.equals(value))
			{
				result.setForeground(TICK_RGB);
			}
			else if(CROSS.equals(value))
			{
				result.setForeground(CROSS_RGB);
			}
			return result;
		}
	}

	/**
	 * @return Table model
	 */
	public ProgressTableModel getModel()
	{
		return model;
	}

	/**
	 * Data model used for progress table.
	 */
	public static class ProgressTableModel extends AbstractTableModel
	{
		/**
		 * A row within the table.
		 */
		public class Row
		{
			private int index;
			private String[] cols = new String[COLUMNS.length];

			private Row(String input, int index)
			{
				for(int i=0; i<cols.length; i++)
				{
					switch(i)
					{
					case 1 :
						cols[1] = input;
						break;
					default:
						cols[i] = "";
					}
				}
				this.index = index;
			}

			/**
			 * Sets the 'Processing...' text.
			 */
			public void setProcessing()
			{
				synchronized(ProgressTableModel.this)
				{
					if(cols[2].length() != 0)
					{
						throw new IllegalStateException("Cannot set more than once");
					}
					cols[2] = PROCESSING_TEXT;
					update();
				}
			}

			/**
			 * Successful result.
			 * @param result Result text
			 * @throws IllegalStateException If result was already set
			 */
			public void setSuccess(String result) throws IllegalStateException
			{
				synchronized(ProgressTableModel.this)
				{
					if(cols[0].length() != 0)
					{
						throw new IllegalStateException("Cannot set more than once");
					}
					cols[0] = TICK;
					cols[2] = result;
					update();
				}
			}

			/**
			 * Failure result.
			 * @param result Result text
			 * @throws IllegalStateException If result was already set
			 */
			public void setFailure(String result) throws IllegalStateException
			{
				synchronized(ProgressTableModel.this)
				{
					if(cols[0].length() != 0)
					{
						throw new IllegalStateException("Cannot set more than once");
					}
					cols[0] = CROSS;
					cols[2] = result;
					update();
				}
			}

			private void update()
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						fireTableRowsUpdated(index, index);
					}
				});
			}
		}

		private LinkedList<Row> rows = new LinkedList<Row>();
		private HashMap<Object, Row> index = new HashMap<Object, Row>();

		@Override
		public int getColumnCount()
		{
			return COLUMNS.length;
		}

		@Override
		public synchronized int getRowCount()
		{
			return rows.size();
		}

		@Override
		public synchronized Object getValueAt(int rowIndex, int columnIndex)
		{
			return rows.get(rowIndex).cols[columnIndex];
		}

		/**
		 * Adds a row.
		 * @param input Input file for row
		 * @param key Key for row so it can be retrieved later
		 */
		synchronized void addRow(String input, Object key)
		{
			final Row row = new Row(input, rows.size());
			rows.addLast(row);
			index.put(key, row);

			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					fireTableRowsInserted(row.index, row.index);
				}
			});
		}

		/**
		 * Obtains row for a given key.
		 * @param key Row key
		 * @return Row
		 * @throws IllegalArgumentException If doesn't exist
		 */
		public synchronized Row getRow(Object key) throws IllegalArgumentException
		{
			Row result = index.get(key);
			if(result == null)
			{
				throw new IllegalArgumentException("Row not found");
			}
			return result;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return false;
		}
	}

}
