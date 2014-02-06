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

public class ConnectForm extends JDialog {
	public ConnectForm(JFrame owner) {
		super(owner, "Connect", true);		
		ConnectPanel panel = new ConnectPanel();
		add(panel, BorderLayout.CENTER);
		pack();
		setResizable(false);
		//	on ok
		panel.okButton.addActionListener( new ActionListener() {	
			public void actionPerformed(ActionEvent event) {
				ok = true;
				setVisible(false);
			}
		});
		// on cancel
		panel.cancelButton.addActionListener( new ActionListener() {	
			public void actionPerformed(ActionEvent event) {
				ok = false;
				setVisible(false);
			}
		});
	}

	public void setHost(String host) {
		panel.hostText.setText(host);			
	}

	public void setPort(int port) {
		panel.portSpinner.setValue(port);		
	}

	public String getAddress() {
		return String.format("%s:%s", panel.hostText.getText(), panel.portSpinner.getValue()); 
	}	

	public boolean Ok() {
		return ok;
	}

	private boolean ok = false;
	private ConnectPanel panel;
}

