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

import com.google.gson.Gson;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

public class Client {
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	private int CONNECTION_TIMEOUT = 500;
	private int requestId = 1;
	private String host;
	private int port;
	private NetHelper rw = new NetHelper();
	private ResponseData response = new ResponseData();
	private String rawjson = null;
	private int record = -1;
	private Hashtable<String, Integer> columns = new Hashtable<String, Integer>();
	private LinkedList<byte[]> backlog = new LinkedList<byte[]>();

	/**
	* connect connects the Client to the pubsubsql server.
	* address string has the form host:port.
	*/
	public void connect(String address) throws IOException, IllegalArgumentException  {
		disconnect();
		// validate address
		int sep = address.indexOf(':');	
		if (sep < 0) {
			throw new IllegalArgumentException("Invalid network address");
		}
		// set host and port
		host = address.substring(0, sep);	
		int portIndex = sep + 1;
		if (portIndex >= address.length()) {
			throw new IllegalArgumentException("Port is not provided");
		}	
		int port = toPort(address.substring(portIndex));
		if (port == 0) {
			throw new IllegalArgumentException("Invalid port");
		}
		java.net.Socket socket = new java.net.Socket();
		socket.connect(new java.net.InetSocketAddress(host, port), CONNECTION_TIMEOUT);
		rw.set(socket);
	}

	/**
	* disconnect disconnects the Client from the pubsubsql server.
	*/
	public void disconnect() {
		backlog.clear();	
		try {
			if (isConnected()) {
				write("close");
			}
		} catch (Exception e) {

		}
		reset();
		rw.close();
	}
	
	/**
	* isConnected returns true if the Client is currently connected to the pubsubsql server.
	*/
	public boolean isConnected() {
		return rw.isValid();
	}
	
	/**
	* execute executes a command against the pubsubsql server.
	* The pubsubsql server returns to the Client a response in JSON format.
	*/
	public void execute(String command) throws java.io.IOException, IllegalArgumentException, Exception {
		reset();
		write(command);
		NetHeader header = new NetHeader();
		for (;;) {
			reset();
			byte[] bytes = readTimeout(0, header);
			if (bytes == null) {
				throw new TimeoutException("Read timed out");
			}
			if (header.RequestId == requestId) {
				// response we are waiting for
				unmarshallJSON(bytes);
				return;
			} else if (header.RequestId == 0) {
				//backlog
				backlog.add(bytes);
			} else if (header.RequestId < requestId) {
				// we did not read full result set from previous command irnore it
				reset();
			} else {
				invalidRequestIdError();
			}
		}
	}

	/**
	* stream sends a command to the pubsubsql server.
	* The pubsubsql server does not return a response to the Client.
	*/
	public void stream(String command) throws java.io.IOException, IllegalArgumentException, Exception {
		reset();
		//TODO optimize
		write("stream " + command);
	}

	/** 
	* getJSON returns a response string in JSON format from the 
	* last command executed against the pubsubsql server.
	*/
	public String getJSON() {
		return NotNull(rawjson);
	}

		
	/** 
	* getAction returns an action string from the response 
	* returned by the last command executed against the pubsubsql server.
	*
	*/
	public String getAction() {
		return NotNull(response.action);
	}

	/** 
	* getPubSubId returns a unique identifier generated by the pubsubsql server when 
	* a Client subscribes to a table. If the client has subscribed to more than  one table, 
	* getPubSubId should be used by the Client to uniquely identify messages 
	* published by the pubsubsql server.
	*/
	public String getPubSubId() {
		return NotNull(response.pubsubid);
	}

	/**
	* getRowCount returns the number of rows in the result set returned by the pubsubsql server.
	*/
	public int getRowCount() {
		return response.rows;
	}

	
	/**
	* nextRow is used to move to the next row in the result set returned by the pubsubsql server.    
	* When called for the first time, NextRow moves to the first row in the result set.
	* Returns false when all rows are read.
	*/
	public boolean nextRow() throws java.io.IOException, IllegalArgumentException, Exception {
		for (;;) {
			// no result set
			if (response.rows == 0) return false;
			if (response.fromrow == 0 || response.torow == 0) return false;
			// the current record is valid
			record++;
			if (record <= (response.torow - response.fromrow)) return true;
			// we reached the end of the result set?
			if (response.rows == response.torow) {
				record--;
				return false;
			}
			// there is another batch of data
			reset();
			NetHeader header = new NetHeader();
			byte[] bytes = readTimeout(0, header);
			if (bytes == null) {
				throw new TimeoutException("Read timed out");
			}
			if (header.RequestId != requestId) {
				invalidRequestIdError();
			}
			unmarshallJSON(bytes);
		}
	}

	/**
	* getValue returns the value within the current row for the given column name.
	* If the column name does not exist, Value returns an empty string.	
	*/
	public String getValue(String column) {
		if (response.data == null) return "";
		if (response.data.size() <= record) return "";
		int ordinal = getColumn(column);
		if (ordinal == -1) return "";
		//
		return response.data.get(record).get(ordinal);
	}
	
	/**
	* getValue returns the value within the current row for the given column ordinal.
	* The column ordinal represents the zero based position of the column in the Columns collection of the result set.
	* If the column ordinal is out of range, getValue returns an empty string.	
	*/
	public String getValue(int ordinal) {
		if (ordinal == -1) return "";
		if (response.data == null) return "";
		if (response.data.size() <= record) return "";
		if (ordinal >= response.columns.size()) return "";
		return response.data.get(record).get(ordinal);
	}

	/**	
	* hasColumn determines if the column name exists in the columns collection of the result set.
	*/
	public boolean hasColumn(String column) {
        return getColumn(column) != -1;
	}

	/**	
	* columnCount returns the number of columns in the columns collection of the result set. 
	*/
	public int getColumnCount() {
		if (response.columns == null) return 0;
		return response.columns.size();
	}

	/**
	* getColumns returns the column names in the columns collection of the result set. 
	*/
	public Iterable<String> getColumns() {
		return response.columns;
	}

	/**
	* waitForPubSub waits until the pubsubsql server publishes a message for
	* the subscribed Client or until the timeout interval elapses.
	* Returns false when timeout interval elapses.
	*/
	public boolean waitForPubSub(int timeout) throws java.io.IOException, IllegalArgumentException, Exception {
		if (timeout <= 0) return false;		
		reset();
		// process backlog first
		if (backlog.size() > 0) {
			byte[] bytes = backlog.remove();
			unmarshallJSON(bytes);
			return true;
		}
		for (;;) {
			NetHeader header = new NetHeader();
			byte[] bytes = readTimeout(timeout, header);
			if (bytes == null) return false; 
			if (header.RequestId == 0) { 
				unmarshallJSON(bytes);			
				return true;
			}
			// this is not pubsub message; are we reading abandoned result set?
			// ignore and continue
		}
	}

	// helper functions
	private boolean IsNullOrEmpty(String str) {
		return (str == null || str.length() == 0);
	}

	private String NotNull(String str) {
		if (str == null) return "";
		return str;
	}

	private int toPort(String port) {
		try {
			return Integer.parseInt(port); 	
		} 
		catch (Exception e) {
				
		}
		return 0;
	}

	private void reset() {
		response = new ResponseData();
		rawjson = null;
		record = -1;
	}

	private void hardDisconnect() {
		backlog.clear();
		rw.close();
		reset();
	}

	private void write(String message) throws IOException, Exception {
		try {
			if (!rw.isValid()) throw new IOException("Not connected");
			requestId++;
			rw.writeWithHeader(requestId, message.getBytes(UTF8_CHARSET));
		} 
		catch (Exception e) {
			hardDisconnect();
			throw e;
		}
	}

	private byte[] readTimeout(int timeout, NetHeader header) throws IOException, Exception {
		try {
			if (!rw.isValid()) throw new IOException("Not connected");
			return rw.readTimeout(timeout, header);
		}		
		catch (Exception e) {
			hardDisconnect();
			throw e;
		}
	}

	private void invalidRequestIdError() throws Exception, IllegalArgumentException {
		throw new Exception("Protocol error invalid request id");	
	}

	private void unmarshallJSON(byte[] bytes) {
		Gson gson = new Gson();
		rawjson = new String(bytes, UTF8_CHARSET);
		response = gson.fromJson(rawjson, ResponseData.class);
		if (!response.status.equals("ok")) throw new IllegalArgumentException(response.msg); 
		setColumns();
	}

	private void setColumns() {
		if (response.columns != null) {
			int index = 0;
			for(String column : response.columns) {
				columns.put(column, index);
				index++;
			}
		}	
	}

	private int getColumn(String column) {
		Integer ordinal = columns.get(column);
		if (ordinal == null) return -1;
		return (int)ordinal;
	}

}

