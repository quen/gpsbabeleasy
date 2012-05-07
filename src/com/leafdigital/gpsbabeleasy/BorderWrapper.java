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

import javax.swing.*;

/**
 * Convenient way to put borders on a component without affecting its insets
 * and stuff.
 */
public class BorderWrapper extends JPanel
{
	/**
	 * @param component Component to wrap
	 * @param top Top border
	 * @param left Left border
	 * @param bottom Bottom border
	 * @param right Right border
	 */
	public BorderWrapper(JComponent component, int top, int left, int bottom, int right)
	{
		super(new BorderLayout());
		add(component, BorderLayout.CENTER);
		setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
	}
}
