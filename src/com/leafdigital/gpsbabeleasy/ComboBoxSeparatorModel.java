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

import java.util.HashSet;

import javax.swing.DefaultComboBoxModel;

class ComboBoxSeparatorModel extends DefaultComboBoxModel
{
	private HashSet<Integer> separators = new HashSet<Integer>();

	@Override
	public void removeAllElements()
	{
		super.removeAllElements();
		separators.clear();
	}

	@Override
	public void removeElement(Object anObject)
	{
		throw new UnsupportedOperationException(
			"Remove all elements at once instead");
	}

	@Override
	public void removeElementAt(int index)
	{
		throw new UnsupportedOperationException(
			"Remove all elements at once instead");
	}

	/**
	 * Adds separator after the last item.
	 * @throws IllegalStateException If you try to add at start of list
	 */
	void addSeparator() throws IllegalStateException
	{
		int size = getSize();
		if(size == 0)
		{
			throw new IllegalStateException("Can't add separator at start of list");
		}
		separators.add(size - 1);
	}

	/**
	 * @param index Index
	 * @return True if there is a separator after this index
	 */
	boolean separatorAfter(int index)
	{
		return separators.contains(index);
	}
}