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

import javax.swing.*;

/**
 * Renderer that puts separator over certain entries
 */
class ComboBoxSeparatorRenderer implements ListCellRenderer
{
	private ListCellRenderer originalRenderer, defaultRenderer;
	private JPanel separatorPanel = new JPanel(new BorderLayout());
	private JSeparator separator = new JSeparator();

	ComboBoxSeparatorRenderer(ListCellRenderer originalRenderer)
	{
		this.originalRenderer = originalRenderer;
		defaultRenderer = new DefaultListCellRenderer();
	}

	@Override
	public Component getListCellRendererComponent(
		JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
    if(index == -1)
    {
	    return originalRenderer.getListCellRendererComponent(
	    	list, value, index, isSelected, cellHasFocus);
    }
    JComponent normal = (JComponent)defaultRenderer.getListCellRendererComponent(
    	list, value, index, isSelected, cellHasFocus);
    normal.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

    // Default renderer uses wrong baackground colour for Mac
    if(isSelected)
    {
    	normal.setBackground(UIManager.getColor("List.selectionBackground"));
    }

    if(((ComboBoxSeparatorModel)list.getModel()).separatorAfter(index))
    {
      separatorPanel.removeAll();
      separatorPanel.add(normal, BorderLayout.CENTER);
      separatorPanel.add(separator, BorderLayout.SOUTH);
      return separatorPanel;
    }
    else
    {
    	return normal;
    }
	}
}