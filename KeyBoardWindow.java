import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

//N.C. Cruz (2016)
@SuppressWarnings("serial")
public class KeyBoardWindow extends JDialog{
	
	private static final String KEYS = "123C456D789EA0BF";//Button generation order
	private static final int[] MAPPING = {1, 2, 3, 12, 4, 5, 6, 13, 7, 8, 9, 14, 10, 0, 11, 15};
	private static final int BUT_SIZE = 60;
	private Chip8Core kernel;//It is expected to be a reference to the kernel
	private JButton[] buttons;
	private JCheckBox fModeCheck;//To check if the fast mode is enabled
	
	private class MyMouseListener extends MouseAdapter{
		int focus;
		
		public MyMouseListener(int focus){
			this.focus = focus;
		}
		@Override
	    public void mousePressed(MouseEvent e) {
			kernel.setKey(focus, true);
	    }
		@Override
		public void mouseReleased(MouseEvent e){
			kernel.setKey(focus, false);
		}
	}
	
	private class MyKeyListener implements KeyListener{
		private int getFocus(KeyEvent e){//Code to get the key value!
			String text = KeyEvent.getKeyText(e.getKeyCode()).toUpperCase();//Working with capital letters!
			return KEYS.indexOf(text);
		}		
		@Override
		public void keyPressed(KeyEvent arg0) {
			int index = getFocus(arg0);
			if(index>=0){//FOUND!
				kernel.setKey(MAPPING[index], true);
			}
		}
		@Override
		public void keyReleased(KeyEvent arg0) {
			int index = getFocus(arg0);
			if(index>=0){//FOUND!
				kernel.setKey(MAPPING[index], false);//Translating from the index in "KEYS" to the real index as key in "MAPPING"
			}
		}
		@Override
		public void keyTyped(KeyEvent arg0){}//NOT NEEDED
	}
	
	public KeyBoardWindow(Chip8Core kernel){
		this.kernel = kernel;//To update the keys over the kernel
		setBounds(500,570,4*BUT_SIZE, 6*BUT_SIZE);
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);//Only for JDialog
		setLayout(null);
		setResizable(false);
		setTitle("Chip8's Keyboard");//Only for JDialog
		
		buttons = new JButton[16];//Hexa-decimal keyboard
		for(int i=0; i<16; i++){
			buttons[i] = new JButton(KEYS.charAt(i) + "");
			buttons[i].setBounds((i%4)*BUT_SIZE,(i/4)*BUT_SIZE, BUT_SIZE, BUT_SIZE);
			buttons[i].addMouseListener(new MyMouseListener(MAPPING[i]));//Linking each button with its real value (which is different from the order)
			buttons[i].setFocusable(false);//TO KEEP THE KEY LISTENER LISTENING TO THE KEYBOARD (A better option is to use KeyBindings...)
			add(buttons[i]);
		}		
		fModeCheck = new JCheckBox("Fast mode");
		fModeCheck.setBounds(72, 240, 3*BUT_SIZE,BUT_SIZE);
		fModeCheck.setFocusable(false);
		add(fModeCheck);
		
		JLabel label = new JLabel ("by N.C. Cruz (2016)");
		label.setBounds(55,280,3*BUT_SIZE,BUT_SIZE);
	    add(label);	    
		addKeyListener(new MyKeyListener());
		setAlwaysOnTop(true);//setUndecorated(true);
		addComponentListener(new ComponentAdapter() {
			  @Override
			  public void componentHidden(ComponentEvent e) {
			    System.exit(0);
			  }});
		setVisible(true);
	}
	
	public Boolean isFastMode(){
		return fModeCheck.isSelected();
	}
}
