/******************************************
Author: Patrick Vandergrift
Date:   Sept. 15, 2026
Description: Java Application meant to
    be GUI for database queries 
******************************************/
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

public class App extends JFrame
{
    private JTextArea queryArea, statusArea, db_URL, db_User, db_Pass;
	private final JPanel appContainer;
	private final CardLayout appCards;
	private JLabel statusLabel;
    private JTable resultTable;
    private JButton clearButton, executeButton, connectButton, saveButton;
	private static final Path file = Paths.get("credentials.txt");

	//**********************
	//Constructor
	//**********************
	public App()
	{
		setTitle("MySQL Query Executer");
		setSize(650, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		
		appCards = new CardLayout();			//Reason: Multiple Panels (Login, SQL)
		appContainer = new JPanel(appCards);	//Reason: Main Container for Panels
		appContainer.setBackground(Color.DARK_GRAY);
		
		appContainer.add(loginPanel(), "LOGIN");
		appContainer.add(sqlPanel(), "SQL");
		
		add(appContainer);
		
		credentialChecker();
	}
	
	//**********************
    //Method to check user credentials 
	//**********************
    void credentialChecker()
    {
		if(Files.exists(file))
		{
			try
			{
				// -- Logic: Get Credentials --
				Properties prop = new Properties();
				BufferedReader credentials = new BufferedReader(new FileReader("credentials.txt"));
				prop.load(credentials);
				
				db_URL.setText(prop.getProperty("url", ""));
				db_User.setText(prop.getProperty("user", ""));
				db_Pass.setText(prop.getProperty("password", ""));
				
				// -- Logic: Valid Credentials --
				if(connectDB())
					appCards.show(appContainer, "SQL");
				else
				{
					appCards.show(appContainer, "LOGIN");
					statusLabel.setText("Error: Credentials Denied, Sent to Login Page");
				}
			}
			catch(IOException e)
			{
				appCards.show(appContainer, "LOGIN");
				statusLabel.setText("Error: Invalid File Formatting, Sent to Login Page");
			}
		}
		else
		{
			appCards.show(appContainer, "LOGIN");
		}
    }

	//**********************
    //Method to create the login panel for user authentication
	//**********************
    private JPanel loginPanel()
	{
		JPanel wrapper = new JPanel(new GridBagLayout());
		wrapper.setBackground(Color.DARK_GRAY);

		JPanel login = new JPanel(null);
		login.setPreferredSize(new Dimension(360, 255));
		login.setBackground(Color.DARK_GRAY.darker());
		login.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
		
		// -- URL Components --
		JLabel label_URL = new JLabel("JDBC URL: ");
		label_URL.setBounds(20, 20, 90, 25);
		label_URL.setForeground(Color.WHITE);
		login.add(label_URL);
		
		db_URL = new JTextArea();
		db_URL.setBounds(110, 20, 200, 25);
		db_URL.setBackground(Color.DARK_GRAY);
		db_URL.setForeground(Color.WHITE);
		db_URL.setCaretColor(Color.WHITE);
		db_URL.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
		login.add(db_URL);
		
		// -- User name Components --
		JLabel label_User = new JLabel("User Name: ");
		label_User.setBounds(20, 60, 90, 25);
		label_User.setForeground(Color.WHITE);
		login.add(label_User);
		
		db_User = new JTextArea();
		db_User.setBounds(110, 60, 200, 25);
		db_User.setBackground(Color.DARK_GRAY);
		db_User.setForeground(Color.WHITE);
		db_User.setCaretColor(Color.WHITE);
		db_User.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
		login.add(db_User);
		
		// -- Password Components --
		JLabel label_Pass = new JLabel("Password: ");
		label_Pass.setBounds(20, 100, 90, 25);
		label_Pass.setForeground(Color.WHITE);
		login.add(label_Pass);
		
		db_Pass = new JTextArea();
		db_Pass.setBounds(110, 100, 200, 25);
		db_Pass.setBackground(Color.DARK_GRAY);
		db_Pass.setForeground(Color.WHITE);
		db_Pass.setCaretColor(Color.WHITE);
		db_Pass.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
		login.add(db_Pass);
		
		// -- Connection Button --
		connectButton = new JButton("Connect");
		connectButton.setBounds(110, 140, 100, 25);
		connectButton.setBackground(Color.GRAY);
		connectButton.setForeground(Color.WHITE);
		login.add(connectButton);
		
		// -- Save Credentials --
		saveButton = new JButton("Save Login-Info");
		saveButton.setBounds(220, 140, 130, 25);
		saveButton.setBackground(Color.GRAY);
		saveButton.setForeground(Color.WHITE);
		login.add(saveButton);
		
		// -- Status labels --
		statusLabel = new JLabel("");
		statusLabel.setBounds(20, 180, 290, 25);
		statusLabel.setForeground(Color.RED);
		login.add(statusLabel);
		
		// -- Action Listener: Save Button --
		saveButton.addActionListener(e -> saveCredentials());
		
		// -- Action Listener: Connect Button --
		connectButton.addActionListener(e ->{
			if(connectDB())
				appCards.show(appContainer, "SQL");
			else
				statusLabel.setText("Error: Connection Failed, Check Credentials");
		});
		
		wrapper.add(login);
		return wrapper;
	}
	
	//**********************
	//Method to Save Credentials
	//**********************
	void saveCredentials()
	{
		Properties prop = new Properties();
		prop.setProperty("url", db_URL.getText());
		prop.setProperty("user", db_User.getText());
		prop.setProperty("password", db_Pass.getText());
		
		if(connectDB())
		{
			try(BufferedWriter outFile = Files.newBufferedWriter(file))
			{
				prop.store(outFile, "Saved Login");
				statusLabel.setText("Saved Login");
				statusLabel.setForeground(Color.GREEN);
			}
			catch(IOException e)
			{
				//e.printStackTrace();	//Save Credentials DEBUGGING
				statusLabel.setText("Error: Could Not Save Login");
			}
		}
		else
			statusLabel.setText("Error: Save Failed, Check Credentials");
	}

	//**********************
	//Method to create a SQL panel for user input and results 
	//**********************
	private JPanel sqlPanel()
	{
		JPanel wrapper = new JPanel(new BorderLayout(10, 10));
		wrapper.setBackground(Color.DARK_GRAY);

		JPanel sql = new JPanel(new BorderLayout(10, 10));
		sql.setBackground(Color.DARK_GRAY);
		sql.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        // -- SQL Input Area --
        queryArea = new JTextArea(5, 50);
        queryArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
		queryArea.setBackground(Color.DARK_GRAY);
		queryArea.setForeground(Color.WHITE);
		queryArea.setCaretColor(Color.WHITE);
		TitledBorder sqlBorder = BorderFactory.createTitledBorder("Execute SQL");
		sqlBorder.setTitleColor(Color.WHITE);
        JScrollPane inputScrollPane = new JScrollPane(queryArea);
		inputScrollPane.setBackground(Color.DARK_GRAY);
        inputScrollPane.setBorder(sqlBorder);

        // -- Control Buttons --
        executeButton = new JButton("Execute SQL");
		executeButton.setBackground(Color.LIGHT_GRAY);
		executeButton.setForeground(Color.BLACK);
        clearButton = new JButton("Clear");
		clearButton.setBackground(Color.LIGHT_GRAY);
		clearButton.setForeground(Color.BLACK);
        
        JPanel buttonPanel = new JPanel();
		buttonPanel.setBackground(Color.DARK_GRAY);
        buttonPanel.add(executeButton);
        buttonPanel.add(clearButton);

        // -- SQL Input and Buttons Panel --
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
		topPanel.setBackground(Color.DARK_GRAY);
		topPanel.setForeground(Color.WHITE);
        topPanel.add(inputScrollPane, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
		
        sql.add(topPanel, BorderLayout.NORTH);
		
		
        // -- Query Result Area --
        resultTable = new JTable();
		resultTable.setBackground(Color.DARK_GRAY);
		resultTable.setForeground(Color.WHITE);
		TitledBorder queryBorder = BorderFactory.createTitledBorder("Query Results");
		queryBorder.setTitleColor(Color.WHITE);
        JScrollPane tableScrollPane = new JScrollPane(resultTable);
		tableScrollPane.setBackground(Color.DARK_GRAY);
        tableScrollPane.setBorder(queryBorder);

		// -- Status message Panel --
        statusArea = new JTextArea(4, 50);
        statusArea.setEditable(false);
		statusArea.setBackground(Color.DARK_GRAY);
        statusArea.setForeground(Color.RED);
		TitledBorder statusBorder = BorderFactory.createTitledBorder("Status / Error Messages");
		statusBorder.setTitleColor(Color.WHITE);
        JScrollPane statusScrollPane = new JScrollPane(statusArea);
		statusScrollPane.setBackground(Color.DARK_GRAY);
        statusScrollPane.setBorder(statusBorder);
		
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
		bottomPanel.setBackground(Color.DARK_GRAY);
		bottomPanel.setForeground(Color.WHITE);
        bottomPanel.add(tableScrollPane, BorderLayout.CENTER);
        bottomPanel.add(statusScrollPane, BorderLayout.SOUTH);
        sql.add(bottomPanel, BorderLayout.CENTER);
		
		// -- Action Listener: Run Query --
        executeButton.addActionListener(e -> runQuery());

		// -- Action Listener: Clear Table & Query --
        clearButton.addActionListener(e -> {
            queryArea.setText("");
            statusArea.setText("");
            resultTable.setModel(new DefaultTableModel());
        });
		
		wrapper.add(sql);
		return wrapper;
	}
	
	//**********************
    //Method to connect to Database 
	//**********************
    boolean connectDB(){
		String url = db_URL.getText().trim(), 
		user = db_User.getText().trim(), pass = db_Pass.getText().trim();
		try(Connection conn = DriverManager.getConnection(url, user, pass))
		{
			return true;
		}
		catch(SQLException e)
		{
			//e.printStackTrace();	//Connect to Database DEBUGGING
			return false;
		}	
	}

	//**********************
	//Method to handle SQL Queries
	//**********************
	private void runQuery()
	{
		String query = queryArea.getText().trim();

        // -- Logic: Block DROP operations --
        if (query.toUpperCase().contains("DROP ")) {
            statusArea.setForeground(Color.RED);
            statusArea.setText("Error: DROP operations Prohibited.");
            return;
        }

		// -- Logic: Query is blank --
        if (query.isEmpty()) {
            statusArea.setForeground(Color.RED);
            statusArea.setText("Please enter an SQL statement.");
            return;
        }
		
		String url = db_URL.getText().trim(), 
		user = db_User.getText().trim(), pass = db_Pass.getText().trim();

        // -- Logic: Database connection and execution --
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement()) {

            // execute() returns true if the first result is a ResultSet (SELECT)
            // returns false if it is an update count or there are no results (INSERT, UPDATE)
            boolean hasResultSet = stmt.execute(query);
			
			// -- Logic: Display results --
            if (hasResultSet) 
			{
                try (ResultSet rs = stmt.getResultSet()) 
				{
                    generateTable(rs);
                    statusArea.setForeground(Color.GREEN.brighter()); // Light green
                    statusArea.setText("Query executed successfully.");
                }
            } else // -- Logic: Display updates --
			{
                int rowsAffected = stmt.getUpdateCount();
                resultTable.setModel(new DefaultTableModel()); 	// Clear previous table data
                statusArea.setForeground(Color.GREEN.brighter()); // Light green
                statusArea.setText("Statement executed successfully. Rows affected: " + rowsAffected);
            }

        } 
		catch (SQLException ex) 
		{
            // Logic: -- Display SQL errors --
            resultTable.setModel(new DefaultTableModel()); // Clear table on error
            statusArea.setForeground(Color.RED);
            statusArea.setText("SQL Error [" + ex.getErrorCode() + "]: " + ex.getMessage());
        }
	}
	  
	
	//**********************
	//Method to generate a table from query results
	//**********************
	private void generateTable(ResultSet rs) throws SQLException
	{
		ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // -- Logic: Get Column Names --
        ArrayList<String> columnNames = new ArrayList<>();
        for (int column = 1; column <= columnCount; column++) {
            columnNames.add(metaData.getColumnName(column));
        }

		// -- Logic: Apply data to table --
        DefaultTableModel tableModel = new DefaultTableModel();
		tableModel.setColumnIdentifiers(columnNames.toArray());

        // -- Logic: Get Row Data -- 
        while (rs.next()) 
		{
            ArrayList<Object> row = new ArrayList<>();
            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) 
			{
                row.add(rs.getObject(columnIndex));
            }
			tableModel.addRow(row.toArray());
        }

        resultTable.setModel(tableModel);
	}
	
	//**********************
    //DRIVER
	//**********************
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }
}
