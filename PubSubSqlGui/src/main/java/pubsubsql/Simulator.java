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

package pubsubsql;

import java.util.*;
import javax.swing.*;
import java.awt.*;

public class Simulator implements Runnable {
	public int Columns = 0;
	public int Rows = 0;
	public String TableName = "";
	public String Address = "";
	private pubsubsql.Client client = new pubsubsql.Client();
	private volatile boolean stopFlag = false;
	private Thread thread;
	private ArrayList<String> ids = new ArrayList<String>();
	private Random rnd = new Random();

	public void run() {
		try {
			rnd.setSeed(System.nanoTime());
			ids.clear();	
			client.connect(Address);
			// first insert data
			for (int row = 1; row <= Rows && !stopFlag; row++) {
				String insert = generateInsert(row);
				client.execute(insert);
				client.nextRow();
				String id = client.getValue("id");
				if (id.length() == 0) throw new Exception("id is empty");
				ids.add(id);
			}
			// 
			while (!stopFlag) {
				for (int i = 0; i < 100 && !stopFlag; i++) {
					String update = generateUpdate();
					client.stream(update);
				}
				// gui thread can not process too many messages from the server
				// slow downs the updates
				Thread.sleep(100);
			}
			client.disconnect();
		}		
		catch (Exception e) {
			final String error = e.getMessage();
			EventQueue.invokeLater(new Runnable() {
			public void run() {
				JOptionPane.showMessageDialog(null, error);		
			}
			});	
		}
		finally {
			client.disconnect();
		}
	}

	public void reset() {
		Columns = 0;
		Rows = 0;
		TableName = "";
		Address = "";
		thread = null;
	}

	public void start() {
		stop();		
		stopFlag = false;
		thread = new Thread(this); 
		thread.start();
	}

	public void stop() {
		stopFlag = true;	
		if (thread != null) {
			try {
				thread.join();
			}
			catch (Exception e) {

			}
			thread = null;
		}
	}

	private String generateUpdate() {
		int idIndex = rnd.nextInt(ids.size());
		String id = ids.get(idIndex);
		int col = rnd.nextInt(Columns + 1);
		if (col == 0) col++;
		int value = rnd.nextInt(1000000);
		return String.format("update %s set col%s = %s where id = %s", TableName, col, value, id); 
	}
	
	private String generateInsert(int row) {
		StringBuilder builder = new StringBuilder();
		builder.append("insert into ");
		builder.append(TableName);
		// columns
		for (int i = 0; i < Columns; i++) {
			if (i == 0) builder.append(" ( ");
			else builder.append(" , ");
			builder.append(String.format("col%s", i + 1));
		}
		// values
		builder.append(") values ");
		for (int i = 0; i < Columns; i++) {
			if (i == 0) builder.append(" ( ");
			else builder.append(" , ");
			builder.append(row);
		}
		builder.append(") returning id");
		return builder.toString();
	}
}

