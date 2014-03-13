/* Copyright (C) 2014 CompleteDB LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License Version 2.0 http://www.apache.org/licenses.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;

public class TableView extends JPanel {
	public int FLASH_TIMEOUT;
	private TableDataset dataset;	
	private JTable table;
	private TableModel model;
		
	public TableView(int flashTimeout, TableDataset dataset) {
		this.FLASH_TIMEOUT = flashTimeout;
		this.dataset = dataset;
		setLayout(new BorderLayout());
		model = this.new TableModel();
		table = new JTable(model);
		table.setDefaultRenderer(Object.class, this.new CellRenderer());
		add(new JScrollPane(table));
	}

	public void update() {
		model.Update();
	}

	// Model
	private class TableModel extends AbstractTableModel {
		public void Update() {
			boolean structureChanged = false;
			if (dataset.getRowCount() != rows) structureChanged = true;
			if (dataset.getColumnCount() != cols) structureChanged = true;
			if (dataset.resetDirtySchema()) structureChanged = true;
			rows = dataset.getRowCount();
			cols = dataset.getColumnCount();
			if (structureChanged) fireTableStructureChanged();
			else fireTableDataChanged();
		}

		// AbstractTableModel
		public int getRowCount() {
			return dataset.getRowCount();
		}

		public int getColumnCount() {
			return dataset.getColumnCount();
		}

		public Object getValueAt(int r, int c) {
			ArrayList<TableDataset.Cell> row = dataset.getRow(r);				
			if (row.size() <= c) return null;
			return row.get(c);
		}

		@Override
		public String getColumnName(int c) {
			return dataset.getColumn(c);		
		}

		private int rows = 0;
		private int cols = 0;
	}

	// Renderer	
	private class CellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
															boolean hasFocus, int r, int c) {
						
			Color backcolor = Color.white;
			if (value == null) {
				 setValue("");
				 setBackground(backcolor);
				 return this;
			}
			TableDataset.Cell cell = (TableDataset.Cell)value;
			if (System.nanoTime() - cell.LastUpdated < FLASH_TIMEOUT) backcolor = Color.pink;
			setBackground(backcolor);
			setValue(cell.Value);
			return this;
		}
	}
}
