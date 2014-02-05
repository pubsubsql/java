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

public interface Client {
	boolean Connect(String address);
	void Disconnect();
	boolean Connected();
	boolean Ok();
	boolean Failed();
	String Error();
	boolean Execute(String command);
	String JSON();
	String Action();
	String PubSubId();
	int RowCount();
	boolean NextRow();
	String Value(String column);
	String ValueByOrdinal(int ordinal);
	boolean HasColumn(String column);
	int ColumnCount();
	Iterable<String> Columns();
	boolean WaitForPubSub(int timeout);	
}

