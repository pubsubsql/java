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

public class MainForm extends JFrame implements ActionListener {
	private final String DEFAULT_ADDRESS = "localhost:7777";
	private JMenuItem connectLocalMenu;
	private JButton connectLocalButton;
	private JMenuItem connectMenu;
	private JButton connectButton;
	private JMenuItem disconnectMenu;
	private JButton disconnectButton;
	private JMenuItem executeMenu;
	private JButton executeButton;
	private JMenuItem cancelMenu;
	private JButton cancelButton;
	private JMenuItem simulateMenu;
	private JTextArea queryText;
	private JTabbedPane resultsTabContainer;
	private JTextArea statusText;
	private JTextArea jsonText;
	private pubsubsql.Client client = new pubsubsql.Client();
	private String connectedAddress = "";
	private boolean cancelExecuteFlag = false;
	private TableDataset dataset = new TableDataset();
	private int FLASH_TIMER_INTERVAL = 150;	
	private int PUBSUB_TIMEOUT = 5;
	private TableView tableView = new TableView(FLASH_TIMER_INTERVAL * 2000000, dataset); 
	private Timer timer;	
	private Simulator simulator = new Simulator();
	private AboutForm aboutForm;

	public MainForm() {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screen = toolkit.getScreenSize();
		setupMenuAndToolBar();		
		// query text
		queryText = new JTextArea();
		queryText.setPreferredSize(new Dimension(screen.width / 2, 100));
		// tabs
		resultsTabContainer = new JTabbedPane();
		resultsTabContainer.addTab("Results", tableView);
		statusText = new JTextArea();		
		resultsTabContainer.addTab("Status", statusText);
		jsonText = new JTextArea();
		resultsTabContainer.addTab("JSON Response", jsonText);
		// splitter
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, queryText, resultsTabContainer); 
		this.add(splitPane, BorderLayout.CENTER);	
		splitPane.setResizeWeight(0.5);	
		// position
        setSize(screen.width / 2, screen.height / 2);
        setLocation(screen.width / 4, screen.height / 4);
		//
        updateConnectedAddress("");
		enableDisableControls();
		//
		timer = new Timer(FLASH_TIMER_INTERVAL, this); 
		timer.start();
	}

	void setupMenuAndToolBar() {
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);	
		JToolBar toolBar = new JToolBar();
		add(toolBar, BorderLayout.NORTH);
		// File
		JMenu fileMenu = new JMenu("File");
			// New
			JMenuItem newMenu = new JMenuItem(new_);	
			new_.putValue(Action.SHORT_DESCRIPTION, "New PubSubSQL Interactive Query");
			fileMenu.add(newMenu);
			fileMenu.addSeparator();
			toolBar.add(new_);
			toolBar.addSeparator();
			// Exit	
			JMenuItem exitMenu = new JMenuItem(exit);
			defaultTooltips(exit);
			fileMenu.add(exitMenu);
		menuBar.add(fileMenu);
		// Connection
		JMenu connectionMenu = new JMenu("Connection");
			// Connect local
			connectLocalMenu = new JMenuItem(connectLocal);
			defaultTooltips(connectLocal);
			connectionMenu.add(connectLocalMenu);
			connectLocalButton = toolBar.add(connectLocal);
			// Connect
			connectMenu = new JMenuItem(connect);
			connect.putValue(Action.SHORT_DESCRIPTION, "Connect to remote server");
			connectionMenu.add(connectMenu);
			connectButton = toolBar.add(connect);
			// Disconnect
			disconnectMenu = new JMenuItem(disconnect);
			defaultTooltips(disconnect);
			connectionMenu.add(disconnectMenu);
			disconnectButton = toolBar.add(disconnect);
			toolBar.addSeparator();
		menuBar.add(connectionMenu);	
		// Query
		JMenu queryMenu = new JMenu("Query");
			// Execute 
			executeMenu = new JMenuItem(execute);
			defaultTooltips(execute);
			queryMenu.add(executeMenu);
			executeButton = toolBar.add(execute);
			// Cancel Executing Query 
			cancelMenu = new JMenuItem(cancelExecute);
			defaultTooltips(cancelExecute);
			queryMenu.add(cancelMenu);
			cancelButton = toolBar.add(cancelExecute);
			// Simulate 
			simulateMenu = new JMenuItem(simulate);
			defaultTooltips(simulate);
			queryMenu.add(simulateMenu);
		menuBar.add(queryMenu);	
		// Help
		JMenu helpMenu = new JMenu("Help");
			// About 
			JMenuItem aboutMenu = new JMenuItem(about);
			defaultTooltips(about);
			helpMenu.add(aboutMenu);
		menuBar.add(helpMenu);	
	}

	// timer event
	long flashTicks = System.nanoTime();
	public void actionPerformed(ActionEvent e) {
		if (dataset.resetDirtyData()) {
			setStatusOk();
			setJSON();
			tableView.update();
			flashTicks = System.nanoTime();
		} else if (System.nanoTime() - flashTicks < tableView.FLASH_TIMEOUT * 2) {
			tableView.update();
		}
	}

	// events
	Action new_ = new AbstractAction("New", createImageIcon("images/New.png")) {
		public void actionPerformed(ActionEvent event) {
			
		}
	};
	
	Action exit = new AbstractAction("Exit") {
		public void actionPerformed(ActionEvent event) {
			System.exit(0);
		}
	};

	Action connectLocal = new AbstractAction("Connect to " + DEFAULT_ADDRESS, createImageIcon("images/ConnectLocal.png")) {
		public void actionPerformed(ActionEvent event) {
			connect(DEFAULT_ADDRESS);
		}
	};

	Action connect = new AbstractAction("Connect...", createImageIcon("images/Connect.png")) {
		public void actionPerformed(ActionEvent event) {
			ConnectForm connectForm = new ConnectForm(MainForm.this);	
			connectForm.setLocationRelativeTo(MainForm.this);
			connectForm.setHost("localhost");	
			connectForm.setPort(7777);
			connectForm.setVisible(true);
			if (connectForm.Ok()) {
				connect(connectForm.getAddress());
			}
		}
	};

	private void connect(String address) {
		clearResults();
		try {
			client.connect(address);
			updateConnectedAddress(address);	
			setStatusOk();
		} catch (Exception e) {
			setStatusError(e.getMessage());
		}
		enableDisableControls();
	}

	Action disconnect = new AbstractAction("Disconnect", createImageIcon("images/Disconnect.png")) {
		public void actionPerformed(ActionEvent event) {
			simulator.stop();
			cancelExecuteFlag = true;
			updateConnectedAddress("");
			client.disconnect();
			enableDisableControls();
			clearResults();
		}
	};

	private void executeCommand() {
		executing();
		String command = queryText.getText().trim();
		if (command.length() == 0) return;
		try {
			client.execute(command);

		} catch (Exception e) {
			setStatusError(e.getMessage());
		}
		// determine if we just subscribed  
		if (client.getPubSubId().length() > 0 && client.getAction().equals("subscribe")) {
			setStatusOk();
			setJSON();
			// enter event loop
			waitForPubSubEvent();
			return;
		}
		processResponse();
		doneExecuting();
	}

	Action execute = new AbstractAction("Execute", createImageIcon("images/Execute2.png")) {
		public void actionPerformed(ActionEvent event) {
			executeCommand();
		}
	};

	Action cancelExecute = new AbstractAction("Cancel Executing Query", createImageIcon("images/Stop.png")) {
		public void actionPerformed(ActionEvent event) {
			simulator.stop();
			cancelExecuteFlag = true;
		}
	};

	Action simulate = new AbstractAction("Simulate") {
		public void actionPerformed(ActionEvent event) {
			simulator.stop();		
			simulator.Address = connectedAddress;
			SimulatorForm simulatorForm = new SimulatorForm(MainForm.this);	
			simulatorForm.setLocationRelativeTo(MainForm.this);
			simulatorForm.setVisible(true);
			if (!simulatorForm.Ok()) {
				return;
			}
			//
			simulator.Columns = simulatorForm.getColumns();
			simulator.Rows = simulatorForm.getRows();	
			simulator.TableName = "T" + System.currentTimeMillis();	
			simulator.start();
			queryText.setText("subscribe * from " + simulator.TableName);
			executeCommand();
			//
		}
	};

	Action about = new AbstractAction("About") {
		public void actionPerformed(ActionEvent event) {
			if (aboutForm == null) aboutForm = new AboutForm(MainForm.this);	
			aboutForm.setLocationRelativeTo(MainForm.this);
			aboutForm.setVisible(true);
		}
	};
	
	private void defaultTooltips(Action action) {
		action.putValue(Action.SHORT_DESCRIPTION, action.getValue(Action.NAME)); 
	}

	private ImageIcon createImageIcon(String path) {
		java.net.URL url = getClass().getResource(path);
		if (url == null) return null;
		return new ImageIcon(url);
	}

	private void clearResults() {
		dataset.clear();
		statusText.setText("");	
		jsonText.setText("");	
	}

	private void updateConnectedAddress(String address) {
        setTitle("PubSubSQL Interactive Query " + address);
		connectedAddress = address;
	}

	private void setStatusOk() {
		statusText.setForeground(Color.black);
		statusText.setText("ok");
	}

	private void setStatusError(String error) {
		statusText.setForeground(Color.red);
		statusText.setText("error\n" + error);
		enableDisableControls();
	}

	private void setJSON() {
		jsonText.setText(client.getJSON());						
	}

	private void enableDisableControls() {
		boolean connected = client.isConnected();
		connectLocalButton.setEnabled(!connected);
		connectLocalMenu.setEnabled(!connected);
		connectButton.setEnabled(!connected);
		connectMenu.setEnabled(!connected);
		disconnectButton.setEnabled(connected);
		disconnectMenu.setEnabled(connected);
		executeButton.setEnabled(connected);
		executeMenu.setEnabled(connected);
		cancelButton.setEnabled(false); cancelMenu.setEnabled(false);
		simulateMenu.setEnabled(executeMenu.isEnabled());
	}

	private void executing() {
		clearResults();
		cancelExecuteFlag = false;
		queryText.setEnabled(false);
		executeButton.setEnabled(false);
		executeMenu.setEnabled(false);
		cancelButton.setEnabled(true);
		cancelMenu.setEnabled(true);
	}

	private void doneExecuting() {
		queryText.setEnabled(true);
		enableDisableControls();	
	}

	private void processResponse() {
		// check if it is result set
		if (client.getRowCount() > 0 && client.getColumnCount() > 0) {
			updateDataset(); 
		}
		setStatusOk();
		setJSON();			
	}

	private void waitForPubSubEvent() {
		if (cancelExecuteFlag) { 
			doneExecuting();		
			// just reconnect to avoid unsubscribing
			if (connectedAddress.length() > 0) {
				connect(connectedAddress);
			}
			clearResults();
			return;
		}
		try {
			client.waitForPubSub(PUBSUB_TIMEOUT);
			updateDataset();
		} catch (Exception e) {
			doneExecuting();
			setStatusError(e.getMessage());
			return;	
		}
		// release control to gui thread and post back to continue polling for pubsub events
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				waitForPubSubEvent();
			}
		});	
	}

	private void updateDataset() {
		try {
			if (!(client.getRowCount() > 0 && client.getColumnCount() > 0)) return;
			dataset.syncColumns(client);
			while (client.nextRow() && !cancelExecuteFlag) {
				dataset.processRow(client);
			}	
		} catch (Exception e) {
			setStatusError(e.getMessage());
		}
	}
}

