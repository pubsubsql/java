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

public class AboutForm extends JDialog {
	private static final long serialVersionUID = 1L;

	public AboutForm(JFrame owner) {
		super(owner, "About PubSubSQL Interactive Query", true);		
		AboutPanel panel = new AboutPanel();
		add(panel, BorderLayout.CENTER);
		pack();
		setResizable(false);
		//		
		panel.okButton.addActionListener( new ActionListener() {	
			public void actionPerformed(ActionEvent event) {
				setVisible(false);
			}
		});
	}
}

