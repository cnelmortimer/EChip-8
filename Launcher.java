//N.C. Cruz (2016)
public class Launcher {
	public static void main(String[] args) {		
		//Console window:
		MainWindow window = new MainWindow();
		//---------------
		Chip8Core kernel = new Chip8Core(false);//No sound for now
		kernel.initialize();
		try {
			kernel.loadApplication(window.getAppPath());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		KeyBoardWindow keyBoard = new KeyBoardWindow(kernel);
		keyBoard.requestFocus();
		
		//Emulation loop:
		long startTimeTimers = System.currentTimeMillis();
		long startTimeCore, endTimeCore, diffTime, waitInterval;//For controlling the emulation speed per cycle
		while(true){
			if ((System.currentTimeMillis() - startTimeTimers) >= 16) { // 60 Hz = 1s/60 = 1000ms/60
				startTimeTimers = System.currentTimeMillis();//For the timers
                kernel.updateTimers();//Decreasing the timers
            }
			try {
				startTimeCore = System.currentTimeMillis();//For the kernel
				waitInterval = (keyBoard.isFastMode()?1:2);
				kernel.emulateCycle();
				//if(kernel.getShouldDraw()){
					window.Update(kernel.getScreen());//ALWAYS DISPLAYING
					//kernel.setShouldDraw(false);
				//}//More CPU usage but better displaying quality in games such as Blitz
				endTimeCore = System.currentTimeMillis();
				diffTime = endTimeCore - startTimeCore;
				if (diffTime < waitInterval) { // 540 Hz = 1s/540 = 1000ms/540 -> 1.85 ms (slow mode). Fast mode: 1
					Thread.sleep(waitInterval - diffTime);//Approximately 2 ms per cycle (slow mode). Fast mode: 1
	            }//Thread.sleep(100);//DEBUG
			} catch (Exception e){
				e.printStackTrace();
				System.exit(0);
			}
		}
	}
}
