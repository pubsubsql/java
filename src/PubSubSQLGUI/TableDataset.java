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
	public static class Cell {
		public String Value;
		public long LastUpdated;

		public Cell(String value) {
			Value = value;
			LastUpdated = System.nanoTime();
		}	
	}

	public boolean ResetDirtyData() {
		boolean ret = dirtyData;
		dirtyData = false;
		return ret;
	}

	public boolean ResetDirtySchema() {
		boolean ret = dirtySchema;
		dirtySchema = false;
		return ret;
	}	

	public void Clear() {
		columns.clear();
		columnOrdinals.clear();
		rows.clear();
		idsToRows.clear();
		dirtyData = true;
		dirtySchema = true;
	}

	public void SyncColumns(pubsubsql.Client client) {
		for(String col : client.Columns()) {
			if (!columnOrdinals.containsKey(col)) {
				dirtySchema = true;
				dirtyData = true;
				int ordinal = columns.size();
				columnOrdinals.put(col, ordinal);
				columns.add(col);
			}
		}
	}

	public void ProcessRow(pubsubsql.Client client) {
		dirtyData = true;
		String id = client.Value("id");
		ArrayList<Cell> row = null;
		switch (client.Action()) {
			case "select":
			case "add":
			case "insert":
				// add row
				row = new ArrayList<Cell>(columns.size());
				// for each selct operations columns are always in the same order
				for (String col : columns) {
					row.add(new Cell(client.Value(col)));	
				}
				rows.add(row);
				if (id.length() > 0) {
					idsToRows.put(id, row);
				}
				break;
			case "update":
				row = idsToRows.get(id);
				if (row != null) {
					for (String col : client.Columns()) {
						Integer ordinal = columnOrdinals.get(col);
						// auto expand row
						for (int i = row.size(); i <= ordinal; i++) {
							row.add(new Cell(""));
						}
						Cell cell = row.get(ordinal);
						cell.Value = client.Value(col);
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
	
	public ArrayList<Cell> Row(int rowIndex) {
		if (rowIndex < rows.size()) return rows.get(rowIndex);
		return new ArrayList<Cell>();
	}

	public int RowCount() {
		return rows.size();
	}

	public int ColumnCount() {
		return columns.size();
	}
	
	public String Column(int colIndex) {
		if (colIndex < columns.size()) {
			return columns.get(colIndex);
		}
		return "";
	}

	private ArrayList<String> columns = new ArrayList<String>();
	private Hashtable<String, Integer> columnOrdinals = new Hashtable<String, Integer>();
	private ArrayList<ArrayList<Cell>> rows = new ArrayList<ArrayList<Cell>>();
	private Hashtable<String, ArrayList<Cell>> idsToRows = new Hashtable<String, ArrayList<Cell>>();
	private volatile boolean dirtyData = false;
	private volatile boolean dirtySchema = false;
} 

