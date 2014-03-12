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

import pubsubsql.Client;
import java.util.*;

/* MAKE SURE TO RUN PUBSUBSQL SERVER WHEN RUNNING THE EXAMPLE */

public class QuickStart {

	public static void main(String[] args) {
		try {
			quickStart();
		} catch (Exception e) {
			System.out.println(e.getMessage());	
		}
	}

	private static void quickStart() throws Exception {
		pubsubsql.Client client = new pubsubsql.Client();
		pubsubsql.Client subscriber = new pubsubsql.Client();

		//----------------------------------------------------------------------------------------------------
		// CONNECT
		//----------------------------------------------------------------------------------------------------

		String address = "localhost:7777";
		client.connect(address);
		subscriber.connect(address);

		//----------------------------------------------------------------------------------------------------
		// SQL MUST-KNOW RULES
		//
		// All commands must be in lower case.
		//
		// Identifiers can only begin with alphabetic characters and may contain any alphanumeric characters.
		//
		// The only available (but optional) data definition commands are
		//    key (unique index)      - key table_name column_name
		//    tag (non-unique index)  - tag table_name column_name
		//
		// Tables and columns are auto-created when accessed.
		//
		// The underlying data type for all columns is String.
		// Strings do not have to be enclosed in single quotes as long as they have no special characters.
		// The special characters are
		//    , - comma
		//      - white space characters (space, tab, new line)
		//    ) - right parenthesis
		//    ' - single quote
		//----------------------------------------------------------------------------------------------------

		//----------------------------------------------------------------------------------------------------
		// INDEX
		//----------------------------------------------------------------------------------------------------

		try {
			client.execute("key Stocks Ticker");
			client.execute("tag Stocks MarketCap");
		} catch (IllegalArgumentException e) {
			// key or tag may have already been defined, so its ok
		}

		//----------------------------------------------------------------------------------------------------
		// SUBSCRIBE
		//----------------------------------------------------------------------------------------------------

		subscriber.execute("subscribe * from Stocks where MarketCap = 'MEGA CAP'");
		String pubsubid = subscriber.getPubSubId();
		System.out.println("subscribed to Stocks pubsubid: " + pubsubid);

		//----------------------------------------------------------------------------------------------------
		// PUBLISH INSERT
		//----------------------------------------------------------------------------------------------------

		client.execute("insert into Stocks (Ticker, Price, MarketCap) values (GOOG, '1,200.22', 'MEGA CAP')");
		client.execute("insert into Stocks (Ticker, Price, MarketCap) values (MSFT, 38,'MEGA CAP')");

		//----------------------------------------------------------------------------------------------------
		// SELECT
		//----------------------------------------------------------------------------------------------------

		client.execute("select id, Ticker from Stocks");
		while (client.nextRow()) {
			System.out.println("*********************************");
			System.out.println(String.format("id:%s Ticker:%s \n", client.getValue("id"), client.getValue("Ticker")));
		}

		//----------------------------------------------------------------------------------------------------
		// PROCESS PUBLISHED INSERT
		//----------------------------------------------------------------------------------------------------

		int timeout = 100;
		while (subscriber.waitForPubSub(timeout)) {
			System.out.println("*********************************");
			System.out.println("Action:" + subscriber.getAction());
			while (subscriber.nextRow()) {
				System.out.println("New MEGA CAP stock:" + subscriber.getValue("Ticker"));
				System.out.println("Price:" + subscriber.getValue("Price"));
			}
		}

		//----------------------------------------------------------------------------------------------------
		// PUBLISH UPDATE
		//----------------------------------------------------------------------------------------------------

		client.execute("update Stocks set Price = '1,500.00' where Ticker = GOOG");

		//----------------------------------------------------------------------------------------------------
		// SERVER WILL NOT PUBLISH INSERT BECAUSE WE ONLY SUBSCRIBED TO 'MEGA CAP'
		//----------------------------------------------------------------------------------------------------

		client.execute("insert into Stocks (Ticker, Price, MarketCap) values (IBM, 168, 'LARGE CAP')");

		//----------------------------------------------------------------------------------------------------
		// PUBLISH ADD
		//----------------------------------------------------------------------------------------------------

		client.execute("update Stocks set Price = 230.45, MarketCap = 'MEGA CAP' where Ticker = IBM");

		//----------------------------------------------------------------------------------------------------
		// PUBLISH REMOVE
		//----------------------------------------------------------------------------------------------------

		client.execute("update Stocks set Price = 170, MarketCap = 'LARGE CAP' where Ticker = IBM");

		//----------------------------------------------------------------------------------------------------
		// PUBLISH DELETE
		//----------------------------------------------------------------------------------------------------

		client.execute("delete from Stocks");

		//----------------------------------------------------------------------------------------------------
		// PROCESS ALL PUBLISHED
		//----------------------------------------------------------------------------------------------------

		while (subscriber.waitForPubSub(timeout)) {
			System.out.println("*********************************");
			System.out.println("Action:" + subscriber.getAction());
			while (subscriber.nextRow()) {
				int ordinal = 0;
				for (String column : subscriber.getColumns()) {
					System.out.print(String.format("%s:%s ", column, subscriber.getValue(ordinal)));
					ordinal++;
				}
				System.out.println(); 
			}
		}

		//----------------------------------------------------------------------------------------------------
		// UNSUBSCRIBE
		//----------------------------------------------------------------------------------------------------

		subscriber.execute("unsubscribe from Stocks");

		//----------------------------------------------------------------------------------------------------
		// DISCONNECT
		//----------------------------------------------------------------------------------------------------

		client.disconnect();
	}
}

