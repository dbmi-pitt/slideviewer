import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


//import ViewerMapCreator.OverlayMap;

/**
 * Convenient class for converting selected .txt tutorViewerSessionPlayer files
 * into jpeg without opening each case.
 * 
 * @author medvop
 * 
 */
public class MapToJpegConverter extends JPanel implements ActionListener {

	static private final String newline = "\n";
	static private final String divider = "**************************";

	private JTextArea log;
	private JFileChooser chooser;
	private JButton convertButton;
	private File[] files;
	
	private ViewerMapCreator vmc;

	public MapToJpegConverter() {
		super(new BorderLayout());

		// Create the log first, because the action listeners
		// need to refer to it.
		log = new JTextArea(5, 20);
		log.setMargin(new Insets(5, 5, 5, 5));
		log.setEditable(false);
		JScrollPane logScrollPane = new JScrollPane(log);

		chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

		convertButton = new JButton("Convert File(s)...");
		convertButton.addActionListener(this);
		
		JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(convertButton);

        //Add the buttons and the log to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);


        try {
			vmc = new ViewerMapCreator("http://slidetutor.upmc.edu/viewer/", false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == convertButton) {
			resetChooser(true);
			int returnVal = chooser.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				files = chooser.getSelectedFiles();

				resetChooser(false);

				int rV = chooser.showSaveDialog(this);
				if (rV == JFileChooser.APPROVE_OPTION) {
					File dir = chooser.getSelectedFile();
					convertToJpeg(files, dir);
				}
			}
		}
	}

	public void convertToJpeg(File[] files, File dir) {
		log.append(divider+newline);
		for (int i = 0; i < files.length; i++) {
			vmc.doCreateMap(files[i].getAbsolutePath(), false);
			
			vmc.saveOverlayByName(files[i].getName(), dir.getAbsolutePath());
			
			log.append("Converting: " + files[i].getName() + "." + newline);
			log.setCaretPosition(log.getDocument().getLength());
		}
		log.append(divider+newline);
	}
	


	private void resetChooser(boolean readFromMode) {
		if (readFromMode) {
			chooser.setMultiSelectionEnabled(true);
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		} else {
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
	}


    private static void createAndShowGUI() {

        JFrame frame = new JFrame("Slide Search Map to Jpeg Converter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(new MapToJpegConverter());

        frame.pack();
        frame.setSize(500, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        /*SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE); 
                createAndShowGUI();
            }
        });*/
    	
    	JFrame frame = new JFrame("Slide Search Map to Jpeg Converter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(new MapToJpegConverter());

        frame.pack();
        frame.setSize(500, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }


}
