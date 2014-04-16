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

import java.util.*;

public class TableDataset {
	private ArrayList<String> columns = new ArrayList<String>();
	private Hashtable<String, Integer> columnOrdinals = new Hashtable<String, Integer>();
	private ArrayList<ArrayList<Cell>> rows = new ArrayList<ArrayList<Cell>>();
	private Hashtable<String, ArrayList<Cell>> idsToRows = new Hashtable<String, ArrayList<Cell>>();
	private volatile boolean dirtyData = false;
	private volatile boolean dirtySchema = false;

	public static class Cell {
		public String Value;
		public long LastUpdated;

		public Cell(String value) {
			Value = value;
			LastUpdated = System.nanoTime();
		}	
	}

	public boolean resetDirtyData() {
		boolean ret = dirtyData;
		dirtyData = false;
		return ret;
	}

	public boolean resetDirtySchema() {
		boolean ret = dirtySchema;
		dirtySchema = false;
		return ret;
	}	

	public void clear() {
		columns.clear();
		columnOrdinals.clear();
		rows.clear();
		idsToRows.clear();
		dirtyData = true;
		dirtySchema = true;
	}

	public void syncColumns(pubsubsql.Client client) {
		for(String col : client.getColumns()) {
			if (!columnOrdinals.containsKey(col)) {
				dirtySchema = true;
				dirtyData = true;
				int ordinal = columns.size();
				columnOrdinals.put(col, ordinal);
				columns.add(col);
			}
		}
	}

	public void processRow(pubsubsql.Client client) {
		dirtyData = true;
		String id = client.getValue("id");
		ArrayList<Cell> row = null;
		switch (client.getAction()) {
			case "select":
			case "add":
			case "insert":
				// add row
				row = new ArrayList<Cell>(columns.size());
				// for each selct operations columns are always in the same order
				for (String col : columns) {
					row.add(new Cell(client.getValue(col)));	
				}
				rows.add(row);
				if (id.length() > 0) {
					idsToRows.put(id, row);
				}
				break;
			case "update":
				row = idsToRows.get(id);
				if (row != null) {
					for (String col : client.getColumns()) {
						Integer ordinal = columnOrdinals.get(col);
						// auto expand row
						for (int i = row.size(); i <= ordinal; i++) {
							row.add(new Cell(""));
						}
						Cell cell = row.get(ordinal);
						cell.Value = client.getValue(col);
						cell.LastUpdated = System.nanoTime();
					}
				}
				break;

			case "delete":
			case "remove":
				row = idsToRows.get(id);
				if (row != null) {
					idsToRows.remove(id);
					rows.remove(row);
				}
				break;
		}
	}
	
	public ArrayList<Cell> getRow(int rowIndex) {
		if (rowIndex < rows.size()) return rows.get(rowIndex);
		return new ArrayList<Cell>();
	}

	public int getRowCount() {
		return rows.size();
	}

	public int getColumnCount() {
		return columns.size();
	}
	
	public String getColumn(int colIndex) {
		if (colIndex < columns.size()) {
			return columns.get(colIndex);
		}
		return "";
	}
} 

