package com.leafdigital.gpsbabeleasy;

import static com.leafdigital.gpsbabeleasy.GpsBabelEasy.UI_SPACING;

import java.awt.*;
import java.awt.event.*;
import java.net.URI;

import javax.swing.*;

/**
 * About dialog. Not particularly exciting. 
 */
public class AboutDialog extends JDialog
{
	private final static String TITLE_TEXT = "About GPSBabel easy converter";
	private final static String GIT_HUB_URL = "https://github.com/quen/gpsbabeleasy";
	private final static String GIT_HUB_TEXT = "github.com/quen/gpsbabeleasy";
	private final static String GPS_BABEL_URL = "http://www.gpsbabel.org/";
	private final static String GPS_BABEL_TEXT = "gpsbabel.org";
	
	private final static int MINI_SPACING = 4;
	
	private GpsBabelEasy easy;
	
	AboutDialog(GpsBabelEasy easy)
	{
		super(easy, TITLE_TEXT, true);
		this.easy = easy;
		
		JPanel main = new JPanel(new BorderLayout(UI_SPACING, UI_SPACING));
		main.setBorder(BorderFactory.createEmptyBorder(
			UI_SPACING, UI_SPACING, UI_SPACING, UI_SPACING));
		getContentPane().add(main);
		
		JLabel label = new JLabel(new ImageIcon(getClass().getResource("icon256.png")));
		main.add(label, BorderLayout.WEST);
		
		JPanel text = new JPanel(new BorderLayout(0, MINI_SPACING));
		main.add(text, BorderLayout.CENTER);
		text.add(newHeading(GpsBabelEasy.TITLE_TEXT + " " + easy.getVersion()), BorderLayout.NORTH);
		
		JPanel next = new JPanel(new BorderLayout(0, MINI_SPACING));
		text.add(next, BorderLayout.CENTER);
		text = next;
		text.add(newSmall("sam marshall / leafdigital"), BorderLayout.NORTH);

		next = new JPanel(new BorderLayout(0, 3 * UI_SPACING));
		text.add(next, BorderLayout.CENTER);
		text = next;
		text.add(newLink(GIT_HUB_URL, GIT_HUB_TEXT), BorderLayout.NORTH);
		
		next = new JPanel(new BorderLayout(0, MINI_SPACING));
		text.add(next, BorderLayout.CENTER);
		text = next;
		text.add(newHeading(easy.getGpsBabelNameAndVersion()), BorderLayout.NORTH);
		
		next = new JPanel(new BorderLayout(0, MINI_SPACING));
		text.add(next, BorderLayout.CENTER);
		text = next;
		text.add(newSmall("Robert Lipe, gpsbabel.org"), BorderLayout.NORTH);

		next = new JPanel(new BorderLayout(0, UI_SPACING));
		text.add(next, BorderLayout.CENTER);
		text = next;
		text.add(newLink(GPS_BABEL_URL, GPS_BABEL_TEXT), BorderLayout.NORTH);
		
		pack();
		setLocationRelativeTo(easy);
		setVisible(true);
	}
	
	private JLabel newHeading(String line)
	{
		JLabel heading = new JLabel(line);
		heading.setFont(heading.getFont().deriveFont(Font.BOLD));
		return heading;
	}
	
	private JLabel newSmall(String line)
	{
		JLabel label = new JLabel(line);
		label.putClientProperty("JComponent.sizeVariant", "small");
		return label;
	}	
	
	private JLabel newLink(final String url, String text)
	{
		JLabel link = new JLabel(
			"<html><u><font color='#000099'>" + text + "</font></u></html>");
		link.putClientProperty("JComponent.sizeVariant", "small");
		link.setFocusable(true);
		final Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				Desktop desktop = java.awt.Desktop.getDesktop();
        try
				{
					desktop.browse(new URI(url));
				}
				catch(Exception e)
				{
					dispose();
					easy.fatalError(e);
				}
			}
		};
		link.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				r.run();
			}
		});
		link.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyTyped(KeyEvent e)
			{
				if(e.getKeyChar() == ' ' || e.getKeyChar() == '\n')
				{
					r.run();
				}
			}
		});
		link.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		return link;
	}
}
