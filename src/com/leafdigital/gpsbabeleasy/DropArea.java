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

import static com.leafdigital.gpsbabeleasy.DropArea.State.*;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.swing.*;

/**
 * Area of window that you can drop files onto.
 */
public class DropArea extends JComponent
{
	private GpsBabelEasy easy;

	private final static int CORNER_RADIUS = 8, BORDER_THICKNESS = 2;

	private final static Color
		COLOR_OUTLINE = new Color(0xc0, 0xc0, 0xc0),
		COLOR_INNER = new Color(0xd0, 0xd0, 0xd0),
		COLOR_EXPLANATION = new Color(0x70, 0x70, 0x70),
		COLOR_HOVERING_TEXT = Color.BLACK;

	private final static String DROP_HINT = "Drag and drop files here to convert them.";

	/**
	 * State for area.
	 */
	public enum State
	{
		/**
		 * Waiting for the user to drop files.
		 */
		AWAITING_FILES(DROP_HINT),
		/**
		 * User is hovering over the area with files.
		 */
		HOVERING(DROP_HINT);

		private String message;

		State(String message)
		{
			this.message = message;
		}

		private boolean isMessage()
		{
			return message != null;
		}

		private String getMessage()
		{
			return message;
		}
	}

	private BufferedImage bg;
	private Font labelFont;

	private State state;

	private DropTarget target;

	/**
	 * @param easy Owner
	 */
	public DropArea(GpsBabelEasy easy)
	{
		this.easy = easy;
		setPreferredSize(new Dimension(800, 80));
		labelFont = new JLabel().getFont();
		state = AWAITING_FILES;
		target = new DropTarget(this, new DropTargetAdapter()
		{
			@Override
			public void dragEnter(DropTargetDragEvent dtde)
			{
				if(state != AWAITING_FILES)
				{
					return;
				}
				try
				{
					DataFlavor expected = new DataFlavor("text/uri-list;class=java.io.Reader");
					for(DataFlavor found : dtde.getCurrentDataFlavors())
					{
						if(found.equals(expected))
						{
							// It is not possible to get the actual file list here :( That sucks!
							dtde.acceptDrag(DnDConstants.ACTION_COPY);
							setState(HOVERING);
							return;
						}
					}
				}
				catch(Exception e)
				{
					// wtf
					e.printStackTrace();
				}
				dtde.rejectDrag();
			}

		  @Override
		  public void dragOver(DropTargetDragEvent dtde)
		  {
		  	if(state == HOVERING)
		  	{
		  		dtde.acceptDrag(DnDConstants.ACTION_COPY);
		  	}
		  }

			@Override
			public void dragExit(DropTargetEvent dte)
			{
				if(state != HOVERING)
				{
					return;
				}
				setState(AWAITING_FILES);
			}

			@Override
			public void drop(DropTargetDropEvent dtde)
			{
				if(state != HOVERING)
				{
					return;
				}

				dtde.acceptDrop(DnDConstants.ACTION_COPY);
				try
				{
					DataFlavor expected2 = new DataFlavor("application/x-java-file-list;class=java.util.List");
					for(DataFlavor found : dtde.getCurrentDataFlavors())
					{
						if(found.equals(expected2))
						{
							@SuppressWarnings("unchecked")
							List<File> list = (List<File>)dtde.getTransferable().getTransferData(found);
							File[] files = list.toArray(new File[list.size()]);
							DropArea.this.easy.convert(files);
						}
					}
				}
				catch(Exception e)
				{
					// wtf
					e.printStackTrace();
				}

				dtde.dropComplete(true);
				setState(AWAITING_FILES);
			}
		});
		target.setActive(true);
	}

	private void setState(State state)
	{
		this.state = state;
		repaint();
	}

	private BufferedImage getBg()
	{
		int width = getWidth(), height = getHeight();
		if(bg == null || bg.getWidth() != width || bg.getHeight() != height)
		{
			BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = mask.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(new Color(255, 0, 0, 255));
			g2.fillRoundRect(0, 0, width - 1, height - 1,
				CORNER_RADIUS, CORNER_RADIUS);

			bg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			g2 = bg.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

			g2.setComposite(AlphaComposite.SrcOver);
			g2.setColor(COLOR_OUTLINE);
			g2.fillRoundRect(0, 0, width + CORNER_RADIUS, height + CORNER_RADIUS,
				CORNER_RADIUS, CORNER_RADIUS);
			g2.setColor(COLOR_INNER);
			g2.fillRoundRect(BORDER_THICKNESS, BORDER_THICKNESS,
				width + CORNER_RADIUS, height + CORNER_RADIUS,
				CORNER_RADIUS, CORNER_RADIUS);

			g2.setComposite(AlphaComposite.DstIn);
			g2.drawImage(mask, 0, 0, null);
		}
		return bg;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		int width = getWidth(), height = getHeight();

		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		BufferedImage bg = getBg();
		g2.drawImage(bg, 0, 0, null);

		String message = null;

		if(state.isMessage())
		{
			g2.setColor(COLOR_EXPLANATION);
			g2.setFont(labelFont);
			message = state.getMessage();
		}

		if(state == HOVERING)
		{
			g2.setColor(COLOR_HOVERING_TEXT);
		}

		if(message != null)
		{
			Rectangle2D rect = g2.getFontMetrics().getStringBounds(message, g2);
			g2.drawString(message, (width - (int)rect.getWidth()) / 2, height / 2);
		}
	}
}
