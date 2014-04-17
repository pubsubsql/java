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

import pubsubsql.Client;

/* 
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
MAKE SURE TO RUN PUBSUBSQL SERVER!
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
*/

public class ClientTest {
	private int failCount = 0;
	private String currentFunction = "";
	private static final String ADDRESS = "localhost:7777";
	private String TABLE = "T" + System.currentTimeMillis();
	private int ROWS = 3;
	private int COLUMNS = 4; // including id

	public static void main(String[] args) {
		ClientTest test = new ClientTest();
		test.testNetHeader();		
		test.testClient();
		if (test.failCount > 0) {
			System.out.println("Failed " + test.failCount + " tests.");
		} else {
			System.out.println("Passed all tests.");
		}
	}	

	// NetHeader
	private void testNetHeader() {
		testWriteRead();	
		testGetBytes();
	}	

	private void testWriteRead() {
		register("testWriteRead");
		pubsubsql.NetHeader header1 = new pubsubsql.NetHeader(32567, 9875235);
		pubsubsql.NetHeader header2 = new pubsubsql.NetHeader(0, 0);
		byte[] bytes = new byte[100];
		header1.writeTo(bytes);
		header2.readFrom(bytes);
		ASSERT_TRUE(header1.MessageSize == header2.MessageSize, "MessageSize do not match");
		ASSERT_TRUE(header1.RequestId == header2.RequestId, "RequestId do not match");
	}

	private void testGetBytes() {
		register("testGetBytes");
		pubsubsql.NetHeader header1 = new pubsubsql.NetHeader(32567, 9875235);
		pubsubsql.NetHeader header2 = new pubsubsql.NetHeader(0, 0);
		byte[] bytes = header1.getBytes();
		header2.readFrom(bytes);
		ASSERT_TRUE(header1.MessageSize == header2.MessageSize, "MessageSize do not match");
		ASSERT_TRUE(header1.RequestId == header2.RequestId, "RequestId do not match");
	}

	// Client
	private void testClient() {
		System.out.println("testing client...");
		testConnectDisconnect();						
		testExecuteStatus();
		testExecuteInvalidCommand();
		testInsertOneRow();
		testInsertManyRows();
		testSelectOneRow();
		testSelectManyRows();
		testUpdateOneRow();
		testUpdateManyRows();
		testDeleteOneRow();
		testDeleteManyRows();
		testKey();
		testTag();
		testSubscribeUnsubscribe();
		testSubscribeUnsubscribeByPubSubId();
		testPubSubTimeout();
		testSubscribeSkip();
		testPubSubAddOnSubscribe();
		testPubSubInsert();
		testPubSubUpdate();
		testPubSubDelete();
		testPubSubRemove();
	}

	private void testConnectDisconnect() {
		register("testConnectDisconnect");
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		ASSERT_CONNECTED(client, true);
		ASSERT_DISCONNECT(client);	
		ASSERT_CONNECT(client, "addresswithnoport", false);
		ASSERT_CONNECTED(client, false);
		ASSERT_DISCONNECT(client);	
		ASSERT_CONNECT(client, "addresswithnoport:", false);
		ASSERT_CONNECTED(client, false);
		ASSERT_DISCONNECT(client);	
		ASSERT_CONNECT(client, "localhost:7778", false);
		ASSERT_CONNECTED(client, false);
		ASSERT_DISCONNECT(client);	
	}

	private void testExecuteStatus() {
		register("testExecute");
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		ASSERT_EXECUTE(client, "status", true);
		ASSERT_ACTION(client, "status");
		ASSERT_DISCONNECT(client);	
	}

	private void testExecuteInvalidCommand() {
		register("testExecuteInvalidCommand");
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		ASSERT_EXECUTE(client, "blablabla", false);
	}

	private void testInsertOneRow() {
		register("testInsertOneRow");
		newtable();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("insert into %s (col1, col2, col3) values (1:col1, 1:col2, 1:col3) returning *", TABLE);
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "insert");
		ASSERT_ROW_COUNT(client, 1);
		ASSERT_NEXT_ROW(client, true);
		ASSERT_ID(client);
		ASSERT_VALUE(client, "col1", "1:col1", true);
		ASSERT_VALUE(client, "col2", "1:col2", true);
		ASSERT_VALUE(client, "col3", "1:col3", true);
		ASSERT_HAS_COLUMN(client, "col1", true);
		ASSERT_HAS_COLUMN(client, "col2", true);
		ASSERT_HAS_COLUMN(client, "col3", true);
		ASSERT_COLUMN_COUNT(client, 4); // including id
		ASSERT_NEXT_ROW(client, false);
		ASSERT_DISCONNECT(client);
	}

	private void testInsertManyRows() {
		register("testInsertManyRows");
		newtable();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("insert into %s (col1, col2, col3) values (1:col1, 1:col2, 1:col3) returning *", TABLE);
		for (int r = 0; r < ROWS; r++) {
			ASSERT_EXECUTE(client, command, true);
			ASSERT_ACTION(client, "insert");
			ASSERT_ROW_COUNT(client, 1);
			ASSERT_NEXT_ROW(client, true);
			//
			ASSERT_ID(client);
			ASSERT_VALUE(client, "col1", "1:col1", true);
			ASSERT_VALUE(client, "col2", "1:col2", true);
			ASSERT_VALUE(client, "col3", "1:col3", true);
			ASSERT_HAS_COLUMN(client, "col1", true);
			ASSERT_HAS_COLUMN(client, "col2", true);
			ASSERT_HAS_COLUMN(client, "col3", true);
			ASSERT_COLUMN_COUNT(client, 4); // including id
			//
			ASSERT_NEXT_ROW(client, false);
		}
		ASSERT_DISCONNECT(client);
	}
	
	private void testSelectOneRow() {
		register("testSelectOneRow");
		newtable();
		insertRow();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		// select one row
		String command = String.format("select * from %s", TABLE);
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "select");
		ASSERT_ROW_COUNT(client, 1);
		ASSERT_NEXT_ROW(client, true);
		//
		ASSERT_ID(client);
		ASSERT_VALUE(client, "col1", "1:col1", true);
		ASSERT_VALUE(client, "col2", "1:col2", true);
		ASSERT_VALUE(client, "col3", "1:col3", true);
		ASSERT_HAS_COLUMN(client, "col1", true);
		ASSERT_HAS_COLUMN(client, "col2", true);
		ASSERT_HAS_COLUMN(client, "col3", true);
		ASSERT_COLUMN_COUNT(client, 4); // including id
		//
		ASSERT_NEXT_ROW(client, false);
		ASSERT_DISCONNECT(client);
	}

	private void testSelectManyRows() {
		register("testSelectRow");
		newtable();
		insertRows();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("select * from %s", TABLE);
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "select");
		ASSERT_ROW_COUNT(client, ROWS);
		for (int row = 0; row < ROWS; row++) {
			ASSERT_NEXT_ROW(client, true);
			ASSERT_ID(client);
			ASSERT_VALUE(client, "col1", row + ":col1", true);
			ASSERT_VALUE(client, "col2", row + ":col2", true);
			ASSERT_VALUE(client, "col3", row + ":col3", true);
			ASSERT_HAS_COLUMN(client, "col1", true);
			ASSERT_HAS_COLUMN(client, "col2", true);
			ASSERT_HAS_COLUMN(client, "col3", true);
			ASSERT_COLUMN_COUNT(client, 4); // including id
		}
		//
		ASSERT_NEXT_ROW(client, false);
		ASSERT_DISCONNECT(client);
	}

	private void testUpdateOneRow() {
		register("testUpdateOneRow");
		newtable();
		insertRow();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("update %s set col1 = newvalue", TABLE);
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "update");
		ASSERT_ROW_COUNT(client, 1);
		ASSERT_DISCONNECT(client);
	}

	private void testUpdateManyRows() {
		register("testUpdateManyRow");
		newtable();
		insertRows();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("update %s set col1 = newvalue", TABLE);
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "update");
		ASSERT_ROW_COUNT(client, ROWS);
		ASSERT_DISCONNECT(client);
	}

	private void testDeleteOneRow() {
		register("testDeleteOneRow");
		newtable();
		insertRow();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("delete from %s", TABLE);
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "delete");
		ASSERT_ROW_COUNT(client, 1);
		ASSERT_DISCONNECT(client);
	}

	private void testDeleteManyRows() {
		register("testDeleteManyRow");
		newtable();
		insertRows();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("delete from %s ", TABLE);
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "delete");
		ASSERT_ROW_COUNT(client, ROWS);
		ASSERT_DISCONNECT(client);
	}

	private void testKey() {
		register("testKey");
		newtable();
		insertRows();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("key %s col1", TABLE);
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "key");
		ASSERT_DISCONNECT(client);
	}

	private void testTag() {
		register("testTag");
		newtable();
		insertRows();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("tag %s col1", TABLE);
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "tag");
		ASSERT_DISCONNECT(client);
	}

	private void testSubscribeUnsubscribe() {
		register("testSubscribeUnsubscribe");
		newtable();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("subscribe * from %s", TABLE);
		// subscribe
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "subscribe");
		ASSERT_PUBSUBID(client);
		// unsubscribe
		command = String.format("unsubscribe from %s", TABLE);		
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "unsubscribe");
		//
		ASSERT_DISCONNECT(client);
	}

	private void testSubscribeUnsubscribeByPubSubId() {
		register("testSubscribeUnsubscribeByPubSubId");
		newtable();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("subscribe * from %s", TABLE);
		// subscribe
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "subscribe");
		ASSERT_PUBSUBID(client);
		// unsubscribe
		command = String.format("unsubscribe from %s where pubsubid = %s", TABLE, client.getPubSubId());		
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "unsubscribe");
		//
		ASSERT_DISCONNECT(client);
	} 

	private void testPubSubTimeout() {
		register("testPubSubTimeout");
		newtable();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		ASSERT_WAIT_FOR_PUBSUB(client, 10, false);	
	}

	private void testSubscribeSkip() {
		register("testSubscribeSkip");
		newtable();
		insertRows();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("subscribe skip * from %s", TABLE);
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "subscribe");
		ASSERT_PUBSUBID(client);
		ASSERT_WAIT_FOR_PUBSUB(client, 10, false);	
		ASSERT_DISCONNECT(client);
	}


	private void testPubSubAddOnSubscribe() {
		register("testPubSubAddOnSubscribe");
		newtable();
		insertRows();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("subscribe * from %s", TABLE);
		// subscribe
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "subscribe");
		ASSERT_PUBSUBID(client);
		// pubsub add
		String pubsubid = client.getPubSubId();
		ASSERT_PUBSUB_RESULT_SET(client, pubsubid, "add", ROWS, COLUMNS);
		ASSERT_DISCONNECT(client);
	}

	private void testPubSubInsert() {
		register("testPubSubInsert");
		newtable();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("subscribe * from %s", TABLE);
		// subscribe
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "subscribe");
		ASSERT_PUBSUBID(client);
		// generate insert event
		insertRows();
		// pubsub insert
		ASSERT_PUBSUB_RESULT_SET(client, client.getPubSubId(), "insert", ROWS, COLUMNS);
		ASSERT_DISCONNECT(client);
	}

	private void testPubSubUpdate() {
		register("testPubSubUpdate");
		newtable();
		insertRows();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("subscribe skip * from %s", TABLE);
		// subscribe
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "subscribe");
		ASSERT_PUBSUBID(client);
		String pubsubid = client.getPubSubId();
		// generate update event
		command = String.format("update %s set col1 = newvalue", TABLE);	
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ROW_COUNT(client, ROWS);
		// expected id and updated column (col1)
		ASSERT_PUBSUB_RESULT_SET(client, pubsubid, "update", ROWS, 2);
		ASSERT_DISCONNECT(client);
	}

	private void testPubSubDelete() {
		register("testPubSubDelete");
		newtable();
		insertRows();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("subscribe skip * from %s", TABLE);
		// subscribe
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ACTION(client, "subscribe");
		ASSERT_PUBSUBID(client);
		String pubsubid = client.getPubSubId();
		// generate update event
		command = String.format("delete from %s", TABLE);	
		ASSERT_EXECUTE(client, command, true);
		ASSERT_ROW_COUNT(client, ROWS);
		// expected id and updated column (col1)
		ASSERT_PUBSUB_RESULT_SET(client, pubsubid, "delete", ROWS, COLUMNS);
		ASSERT_DISCONNECT(client);
	}

	private void testPubSubRemove() {
		register("testPubSubRemove");
		newtable();
		insertRows();
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		// key col1
		String command = String.format("key %s col1", TABLE);
		ASSERT_EXECUTE(client, command, true);
		command = String.format("subscribe skip * from %s where col1 = 1:col1", TABLE);
		ASSERT_EXECUTE(client, command, true);
		String pubsubid = client.getPubSubId();
		// generate remove
		command = String.format("update %s set col1 = newvalue where col1 = 1:col1", TABLE);	
		ASSERT_EXECUTE(client, command, true);
		ASSERT_PUBSUB_RESULT_SET(client, pubsubid, "remove", 1, COLUMNS);
		ASSERT_DISCONNECT(client);
	}

	// helper functions

	private String generateTableName() {
		return "T" + System.currentTimeMillis();
	}

	private void newtable() {
		TABLE = generateTableName();
	}

	private void fail(String message) {
		System.out.println(currentFunction + " " + message);
		failCount++;	
	}

	/*
	private void print(String message) {
		System.out.println(message);
	}
	*/

	private void register(String function) {
		currentFunction = function;
		System.out.println(function);
	}

	private void insertRow() {
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("insert into %s (col1, col2, col3) values (1:col1, 1:col2, 1:col3)", TABLE);
		ASSERT_EXECUTE(client, command, true);
		ASSERT_DISCONNECT(client);	
	}

	private void insertRows() {
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		for (int row = 0; row < ROWS; row++) {	
			String command = String.format("insert into %s (col1, col2, col3) values (%s:col1, %s:col2, %s:col3)", TABLE, row, row, row);
			ASSERT_EXECUTE(client, command, true);
		}
		ASSERT_DISCONNECT(client);
	}

	/*
	private void key(String column) {
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("key %s %s", TABLE, column);
		ASSERT_EXECUTE(client, command, true);
		ASSERT_DISCONNECT(client);
	}
	*/

	/*
	private void tag(String column) {
		Client client = new Client();
		ASSERT_CONNECT(client, ADDRESS, true);
		String command = String.format("tag %s %s", TABLE, column);
		ASSERT_EXECUTE(client, command, true);
		ASSERT_DISCONNECT(client);
	}
	*/
	
	public void ASSERT_TRUE(boolean val, String message) {
		if (!val) {
			fail("ASSERT_TRUE failed: " + message);
		}	
	}

	public void ASSERT_FALSE(boolean val, String message) {
		if (val) {
			fail("ASSERT_FALSE failed: " + message);
		}
	}

	public void ASSERT_CONNECT(Client client, String address, boolean expected) {
		boolean got = true;
		try {
			client.connect(address);
		} catch (Exception e) {
			got = false;
		}
		if (expected != got) {
			fail(String.format("ASSERT_CONNECT failed: expected %s got %s ", expected, got));
		}	
	}

	public void ASSERT_DISCONNECT(Client client) {
		client.disconnect();
	}

	public void ASSERT_CONNECTED(Client client, boolean expected) {
		boolean got = false;
		try {
			got = client.isConnected();
		} catch (Exception e) {
		}
		if (expected != got) {
			fail(String.format("ASSERT_CONNECTED failed: expected %s got %s", expected, got));
		}
	}

	public void ASSERT_EXECUTE(Client client, String command, boolean expected) {
		boolean got = true; 
		try {
			client.execute(command);
		} catch (Exception e) {
			got = false;
		}
		if (expected != got) {
			fail(String.format("ASSERT_EXECUTE failed: expected %s got %s", expected, got));	
		}
	}

	public void ASSERT_ACTION(Client client, String expected) {
		String got = client.getAction();
		if (!expected.equals(got)) {
			fail(String.format("ASSERT_ACTION failed: expected %s got %s", expected, got));
		}
	}

	public void ASSERT_ROW_COUNT(Client client, int expected) {
		int got = client.getRowCount();
		if (expected != got) {
			fail(String.format("ASSERT_ROW_COUNT failed: expected %s but got %s", expected, got));
		}
	}

	public void ASSERT_NEXT_ROW(Client client, boolean expected) {
		boolean got = false;
		try {
			got = client.nextRow();
		} catch (Exception e) {

		}
		if (expected != got) {
			fail(String.format("ASSERT_NEXT_ROW failed: expected %s but got %s", expected, got));
		}
	}

	public void ASSERT_ID(Client client) {
		String id = client.getValue("id");
		if (id.length() == 0) {
			fail("ASSERT_ID failed: expected non empty string");
		}
	}

	public void ASSERT_PUBSUBID(Client client) {
		String pubsubid = client.getPubSubId();	
		if (pubsubid.length() == 0) {
			fail("ASSERT_PUBSUBID failed: expected non empty string");
		}	
	}

	public void ASSERT_PUBSUBID_VALUE(Client client, String expected) {
		String got = client.getPubSubId(); 		
		if (!expected.equals(got)) {
			fail(String.format("ASSERT_PUBSUBID_VALUE failed: expected %s but got %s", expected, got));
		}
	}

	public void ASSERT_VALUE(Client client, String column, String value, boolean match) {
		String got = client.getValue(column);	
		if (match && !value.equals(got)) {
			fail(String.format("ASSERT_VALUE failed: expected %s but got %s", value, got));
		}
		else if (!match && value.equals(got)) {
			fail(String.format("ASSERT_VALUE failed: not expected %s", value));
		}
	}

	public void ASSERT_COLUMN_COUNT(Client client, int expected) {
		int got = client.getColumnCount();
		if (expected != got) {
			fail(String.format("ASSERT_COLUMN_COUNT failed: expected %s but got %s", expected, got));
		}
	}

	public void ASSERT_HAS_COLUMN(Client client, String column, boolean expected) {
		boolean got = client.hasColumn(column);
		if (expected != got) {
			fail(String.format("ASSERT_HAS_COLUMN failed: expected %s but got %s", expected, got));
		}
	}	

	public void ASSERT_WAIT_FOR_PUBSUB(Client client, int timeout, boolean expected) {
		boolean got = false;
		try {
			got = client.waitForPubSub(timeout);
		} catch (Exception e) {

		}
		if (expected != got) {
			fail(String.format("ASSERT_WAIT_FOR_PUBSUB failed: expected %s but got %s", expected, got));
		}	
	}

	public void ASSERT_NON_EMPTY_VALUE(Client client, int ordinal) {
		if (client.getValue(ordinal).length() == 0) {
			fail(String.format("ASSERT_NON_EMPTY_VALUE failed: expected non empty string for ordinal %s", ordinal));
		}
	}

	public void ASSERT_RESULT_SET(Client client, int rows, int columns) {
		ASSERT_ROW_COUNT(client, rows);
		for (int row = 0; row < rows; row++) {
			ASSERT_NEXT_ROW(client, true);
			ASSERT_COLUMN_COUNT(client, columns); 
			for (int col = 0; col < columns; col++) {
				ASSERT_NON_EMPTY_VALUE(client, col);
			} 
		}
		ASSERT_NEXT_ROW(client, false);
	} 

	public void ASSERT_PUBSUB_RESULT_SET(Client client, String pubsubid, String action, int rows, int columns) {
		try {
			int readRows = 0;		
			while (readRows < rows) {
				if (!client.waitForPubSub(100)) {
					fail(String.format("ASSERT_PUBSUB_RESULT_SET failed expected %s rows but got %s", rows, readRows));
					return;
				}
				ASSERT_PUBSUBID_VALUE(client, pubsubid);
				ASSERT_ACTION(client, action);
				while (client.nextRow()) {
					readRows++;
					ASSERT_COLUMN_COUNT(client, columns); 
					for (int col = 0; col < columns; col++) {
						ASSERT_NON_EMPTY_VALUE(client, col);
					} 
				}
			}
		} catch (Exception e) {
			fail(String.format(e.getMessage()));
		}
	}

}

