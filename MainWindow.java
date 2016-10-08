import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

//N.C. Cruz (2016)
@SuppressWarnings("serial")
public class MainWindow extends JFrame{
	private static final int WIDTH_SCREEN = 64;//The CHIP-8 Screen
	private static final int HEIGHT_SCREEN = 32;
	private String appName;
	private String appPath;
	private Screen scr;
	private Insets ins;
	
	private class Screen extends JPanel{
		private Boolean[] screenMap;
		private int xFactor, yFactor;
		
		public Screen(int xFactor, int yFactor){
			this.xFactor = xFactor; 
			this.yFactor = yFactor;
			screenMap = new Boolean[2048];
			for(int i = 0; i<2048; i++){
				screenMap[i] = false;
			}
		}
		
		public void setScreenMap(Boolean[] screenMap){
			this.screenMap = screenMap;
		}
		
		public void updateFactors(int xFactor, int yFactor){
			this.xFactor = xFactor; this.yFactor = yFactor;
		}
		
		@Override
		public void paint(Graphics g){
			for(int i = 0; i<2048; i++){
				g.setColor(screenMap[i]?Color.WHITE:Color.BLACK);
				g.fillRect((i%WIDTH_SCREEN)*xFactor, (i/WIDTH_SCREEN)*yFactor, xFactor, yFactor);
			}
		}		
	}
	
	public MainWindow(){
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);//setResizable(false);//Ask for a game:
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));//Alternative: "user.home"
		int result = fileChooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			appPath = selectedFile.getAbsolutePath();
			String parts[] = appPath.split(java.io.File.separator);
			appName = parts[parts.length-1];
			this.setTitle(appName);
			this.ins = getInsets();
			setBounds(500,250,640, 320);
			this.scr = new Screen(this.getWidth()/WIDTH_SCREEN, (this.getHeight()-ins.top-ins.bottom)/HEIGHT_SCREEN);
			this.add(scr);//linking
			setVisible(true);
		}else{
			System.exit(0);
		}
		addComponentListener(new ComponentAdapter(){//Scaling compatibility
			@Override
			public void componentResized(ComponentEvent e) {
				scr.updateFactors(getWidth()/WIDTH_SCREEN, (getHeight()-ins.top-ins.bottom)/HEIGHT_SCREEN);
				repaint();
			}}
		);
	}
	
	public String getAppPath(){
		return this.appPath;
	}
	
	public void Update(Boolean[] screen){
		scr.setScreenMap(screen);
		repaint();
	}	
}
