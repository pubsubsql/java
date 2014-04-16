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

public class SimulatorForm extends JDialog {
	private static final long serialVersionUID = 1L;
	private SimulatorPanel panel;
	private boolean ok = false;

	public SimulatorForm(JFrame owner) {
		super(owner, "Simulator", true);		
		panel = new SimulatorPanel();
		add(panel, BorderLayout.CENTER);
		panel.columnsSpinner.setValue(5);	
		panel.rowsSpinner.setValue(50);	
		pack();
		setResizable(false);
		// on ok	
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

	public int getColumns() {
		return (Integer)panel.columnsSpinner.getValue();
	}

	public int getRows() {
		return (Integer)panel.rowsSpinner.getValue();
	}

	public boolean Ok() {
		return ok;
	}
}

