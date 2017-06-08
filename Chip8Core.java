import java.io.BufferedInputStream;
import java.io.FileInputStream;

/**
 * This class contains the virtual CPU of the Chip-8 environment
 * @author N.C. Cruz (2016)
 * SPECIAL THANKS TO LAURENCE MULLER FOR HIS BLOG (http://www.multigesture.net/articles/how-to-write-an-emulator-chip-8-interpreter/) [MUL]
 * SPECIAL THANKS TO JEFFREY BIAN (http://mochi8.weebly.com/) [JEF]
 * SPECIAL THANKS TO http://devernay.free.fr/hacks/chip8/C8TECH10.HTM [COW]
 * SPECIAL THANKS TO http://omokute.blogspot.com.es/2012/06/emulation-basics-write-your-own-chip-8.html [OMO]
 * SPECIAL THANKS TO https://es.wikipedia.org/wiki/CHIP-8 [WIK]
 * SPECIAL THANKS TO https://github.com/michaelarnauts/chip8-java/blob/master/Source/src/be/khleuven/arnautsmichael/chip8/Cpu.java GREAT JOB!
 * Thank you very much for sharing your tutorials and information! I had ever wondered how emulators work!
 */

public class Chip8Core {
	
	/*Attributes*///Some data-types have been selected to avoid extensive casting... 
	private int[] V;// Chip-8 has 16 8-bit registers: V0, V1, V2... VF (int data-type has been selected -> 32 bits)
	
	private int I;// Direction register (16 bits). Chip-8 has a 16-bit direction register (int data-type has been selected -> 32 bits)
	
	private int pc;// Program counter (16 bits). Hip-8 has a program counter which can have a value from 0x000 to 0xFFF (4095) (int data-type has been selected -> 32 bits)
	
	private int[] stack;// Where to save the current location before jumping (up to 16 levels)
	private int stackPointer;// Which level of the stack is currently used...
	
	private Boolean[] screen;// The Chip-8 system works in black and white with a screen of 2048 pixels (64 x 32)
	
	//Chip-8 has two special purpose 8-bit registers, for the delay and sound timers. When they are non-zero, they are automatically decremented at a rate of 60Hz. [COW]
	private int DT;// Delay timer
	private int ST;// Sound timer (it buzzes when ST==0)
	
	private int[] memory;//Main memory: The Chip 8 has 4KB of RAM memory in total [MUL]
	//Memory map [COW]:
	//0x000 (0) Start of Chip-8 RAM (0x000 to 0x1FF Reserved for the interpreter)
	//0x200 (512_(10)) Start of most Chip-8 programs
	//0xFFF (4095) End of Chip-8 RAM
	
	private Boolean[] keys; // Chip 8 has a HEX based keypad (0x0-0xF), where to save their status-> press and release
	
	private Boolean shouldDraw; // The screen will be updated only when needed, the system does not draw every cycle [MUL, OMO]
	
	private Boolean soundAllowed; // Do you want to allow sound?
	
	//Fontset: sprites are used for graphics. Sprites are a set of bits that indicate a corresponding set of pixel's on/off state [OMO].
	private static final int[] fontSet = {//[COW, MUL]:
		0xF0, 0x90, 0x90, 0x90, 0xF0, //0
	    0x20, 0x60, 0x20, 0x20, 0x70, //1
	    0xF0, 0x10, 0xF0, 0x80, 0xF0, //2
	    0xF0, 0x10, 0xF0, 0x10, 0xF0, //3
	    0x90, 0x90, 0xF0, 0x10, 0x10, //4
	    0xF0, 0x80, 0xF0, 0x10, 0xF0, //5
	    0xF0, 0x80, 0xF0, 0x90, 0xF0, //6
	    0xF0, 0x10, 0x20, 0x40, 0x40, //7
	    0xF0, 0x90, 0xF0, 0x90, 0xF0, //8
	    0xF0, 0x90, 0xF0, 0x10, 0xF0, //9
	    0xF0, 0x90, 0xF0, 0x90, 0x90, //A
	    0xE0, 0x90, 0xE0, 0x90, 0xE0, //B
	    0xF0, 0x80, 0x80, 0x80, 0xF0, //C
	    0xE0, 0x90, 0x90, 0x90, 0xE0, //D
	    0xF0, 0x80, 0xF0, 0x80, 0xF0, //E
	    0xF0, 0x80, 0xF0, 0x80, 0x80  //F
	};
	
	/*Methods*/	
	//Default constructor
	public Chip8Core(Boolean soundAllowed){
		V = new int[16];//Initialize the bank of registers
		stack = new int[16];//Initialize the stack -> 16 levels
		memory = new int[4096];//Main memory: 4096 bytes
		screen = new Boolean[2048];//64x32 black/white pixels -> 2048.		
		keys = new Boolean[16];// 16 keys whose status (press and release) must be saved
		this.soundAllowed = soundAllowed;
	}
	
	//Cleans and reset the Chip-8 system
	public void initialize(){
		pc = 512; // Program counter starts at 0x200 -> The first 512 bytes are reserved for the interpreter
		stackPointer = 0;
		I = 0;// Direction register reset
		DT = 0;// Delay timer reset
		ST = 0;// Sound timer reset
		shouldDraw = false;//There is no need of drawing yet
		//Register cleaning:
		for(int i=0; i<16; i++){
			V[i] = 0;
		}
		//Stack cleaning:
		for(int i=0; i<16; i++){
			stack[i] = 0;
		}
		//Key status cleaning:
		for(int i=0; i<16; i++){
			keys[i] = false;
		}
		//Memory cleaning:
		for(int i=0; i<4096; i++){
			memory[i] = 0;
		}
		//Fontset loading in memory:
		for(int i=0; i<80; i++){
			memory[i] = fontSet[i];
		}
		//Screen cleaning:
		for(int i=0; i<2048; i++){
			screen[i] = false;
		}
	}
	
	public void emulateCycle() throws Exception{
		//Fetch the opcode
		int opcode = memory[pc] << 8 | memory[pc+1];//Opcodes have 2 bytes (INT DATA-TYPE HAS BEEN SELECTED: 32 BITS) -> Sift and OR to form 2 bytes for the full opcode (Alt: //int opcode = ((memory[pc]<<8) & 0x0000FF00) | memory[pc+1];) ALT:memory[pc] << 8 | (0x00FF & memory[pc+1]);
		try{
			switch (opcode & 0x0000F000){//Getting the root of the opcode: XXXXXXXXXXXXXXXX & 1111000000000000 (extracting...)
				case 0x0000: op0_(opcode);// Clean the screen OR Returns from a subrutine
					break;
				case 0x1000: op1NNN(opcode);// Jumps to address NNN
					break;
				case 0x2000: op2NNN(opcode);// Calls a subroutine at NNN
					break;
				case 0x3000: op3XKK(opcode);// Skips the next instruction if VX == KK
					break;
				case 0x4000: op4XKK(opcode);// Skips the next instruction if VX != KK
					break;
				case 0x5000: op5XY0(opcode);// Skips the next instruction if VX == VY
					break;
				case 0x6000: op6XKK(opcode);// Sets the register VX = KK
					break;
				case 0x7000: op7XKK(opcode);// Sets the register VX = VX + KK
					break;
				case 0x8000: op8XY_(opcode);// This is a large subgroup of possible operations over the registers VX and VY
					break;
				case 0x9000: op9XY0(opcode);// Skips the next instruction if VX != VY
					break;
				case 0xA000: opANNN(opcode);// Sets I to NNN
					break;
				case 0xB000: opBNNN(opcode);// 0xBNNN Jumps to V[0] + NNN
					break;
				case 0xC000: opCXKK(opcode);// 0xCXKK Sets VX to a random byte AND KK
					break;
				case 0xD000: opDXYN(opcode);// Draws a sprite on the screen starting at coordinate (VX, VY)
					break;
				case 0xE000: opE000(opcode);// Jump to the next instruction if VX is equal/different to the pressed key
					break;
				case 0xF000: opF000(opcode);//Several options
					break;
				default: throw new Exception("Fatal error: Unknown opcode");
			}
		}catch(Exception e){//A problem occurred while decoding
			throw e;
		}
	}
	
	/// Opcodes' implementation:

	private void op0_(int opcode) throws Exception{//0x0NNN will be ignored as done in modern interpreters -> Calls RCA 1802 program at address NNN. Not necessary for most ROMs. [WIK]
		switch(opcode & 0x000000FF){
			//The opcode is 0x00E0 -> Clean the screen
			case 0x000000E0:for(int i = 0; i<2048; i++){//64x32=2048
								screen[i] = false;
							}
							shouldDraw = true;
							pc = pc + 2;//Moving the program counter (by 2...)
							break;
			//The opcode is 0x00EE -> Returns from subroutine ()
			case 0x000000EE:stackPointer = stackPointer - 1;
							pc = stack[stackPointer];
							pc = pc + 2;//Moving the program counter to the next (as the current value should be the original jump)
							break;
			default: throw new Exception("Unknown opcode of form 0...(Expected: 00E0 OR 00EE)");
		}
	}
	
	private void op1NNN(int opcode){//0x1NNN -> Jumps to address NNN
		pc = opcode & 0x00000FFF;//Getting the 12 bits direction (FFF = 4095, the maximum direction value)
	}
	
	private void op2NNN(int opcode){//0x2NNN -> Calls a subroutine at NNN.
		stack[stackPointer] = pc;//Saving the calling direction
		stackPointer = stackPointer + 1;
		pc = opcode & 0x00000FFF;//Getting the 12 bits direction (FFF = 4095, the maximum direction value)		
	}
	
	private void op3XKK(int opcode){//0x3XKK Skips the next instruction if VX == KK
		if (V[(opcode & 0x00000F00) >> 8]==(opcode & 0x000000FF)){//Skips the next instruction
			pc += 4;//Skipping
		}else{
			pc += 2;//Not skipping... this is the common pc's increment
		}
	}
	
	private void op4XKK(int opcode){//0x4XKK Skips the next instruction if VX != KK
		if (V[(opcode & 0x00000F00) >> 8]!=(opcode & 0x000000FF)){//Skips the next instruction
			pc += 4;//Skipping
		}else{
			pc += 2;//Not skipping... this is the common pc's increment
		}
	}
	
	private void op5XY0(int opcode){//0x5XY0 Skips the next instruction if VX == VY
		if(V[(opcode & 0x00000F00) >> 8] == V[(opcode & 0x000000F0) >> 4]){
			pc += 4;//Skipping
		}else{
			pc += 2;//Not skipping... this is the common pc's increment
		}
	}
	
	private void op6XKK(int opcode){//0x6XKK Sets the register VX = KK
		V[(opcode & 0x00000F00) >> 8] = (opcode & 0x000000FF);
		pc += 2;//Common pc's increment
	}
	
	private void op7XKK(int opcode){//0x7XKK Sets the register VX = VX + KK
		int x = (opcode & 0x00000F00) >> 8;
		int result = (V[x] + (opcode & 0x000000FF));
		//[JEF] seems to have a bug at this point because of saving if there is a carry or not in V[0xF] -> E.g.: Tetris won't work properly
		V[x] = result & 0x000000FF;//Forcing 8 bits
		pc += 2;//Common pc's increment
	}
	
	private void op8XY_(int opcode) throws Exception{//Subgroup of possible operations over the registers VX and VY
		int x, y, result;//for internal operations
		switch (opcode & 0x0000000F){
			case 0x00000000:// 0x8XY0 Sets VX to VY
				V[(opcode & 0x00000F00)>>8] = V[(opcode & 0x000000F0)>>4];
				break;
			case 0x00000001: //0x8XY1 Sets VX to VX OR VY
				V[(opcode & 0x00000F00)>>8] |= V[(opcode & 0x000000F0)>>4];
				break;
			case 0x00000002: //0x8XY2 Sets VX to VX AND VY
				V[(opcode & 0x00000F00)>>8] &= V[(opcode & 0x000000F0)>>4];
				break;
			case 0x00000003: //0x8XY3 Sets VX to "VX XOR VY"
				V[(opcode & 0x00000F00)>>8] ^= V[(opcode & 0x000000F0)>>4];
				break;
			case 0x00000004: //0x8XY4 Adds VY to VX. VF is set to 1 when there's a carry (>8 bits required) (and to 0 otherwise)
				x = (opcode & 0x00000F00) >> 8;
				y = (opcode & 0x000000F0) >> 4;
				result = V[y] + V[x];//VY + VX
				if(result>0xFF){//255 = 0xFF (8 bits). The V* registers are of 8 bits
					V[0xF] = 1; //Carry
				}else{
					V[0xF] = 0;//No carry
				}
				V[x] = result & 0x000000FF;//Forcing 8 bits
				break;
			case 0x00000005: // 0x8XY5 VY is subtracted from VX. VF is set to 0 when there's a borrow (and to 1 otherwise)
				x = (opcode & 0x00000F00) >> 8;
				y = (opcode & 0x000000F0) >> 4;
				if(V[x] >= V[y]){//If VX > VY -> VF is set to 1 [COW] ([JEF] does >= !! Why? <I WILL DO >= too>)
					V[x] = (V[x] - V[y]) & 0x000000FF;//Forcing 8 bits
					V[0xF] = 1;//NOT borrow
				}else{
					V[x] = (/*0x100+*/V[x] - V[y]) & 0x000000FF;//Forcing 8 bits ([JEF] takes 0x100 as borrow)
					V[0xF] = 0;//Borrow
				}
				break;
			case 0x00000006: // 0x8XY6 Shifts VX right by one. VF is set to the value of the least significant bit of VX before the shift
				x = (opcode & 0x00000F00) >> 8;
				V[0xF] = V[x] & 0x00000001;//Getting the least significant value of VX
				result = V[x] >> 1;//Shifting by one (equivalent to dividing by two)
				V[x] = result & 0x000000FF;//Forcing 8 bits
				break;
			case 0x00000007: // 0x8XY7 Sets VX to VY minus VX. VF is set to 0 when there's a borrow (and to 1 otherwise)
				x = (opcode & 0x0F00) >> 8;
				y = (opcode & 0x00F0) >> 4;
				if(V[y] >= V[x]){//If VY > VX there is no a borrow [COW] ([JEF] does >= !! Why? <I WILL DO >= too>)
					V[0xF] = 1;//NOT borrow
					V[x] = (V[y] - V[x]) & 0x000000FF;//Forcing 8 bits
				}else{
					V[0xF] = 0;//borrow
					V[x] = (/*0x100+*/V[y] - V[x]) & 0x000000FF;//Forcing 8 bits ([JEF] takes 0x100 as borrow)
				}
				break;
			case 0x0000000E: // 0x8XYE Shifts VX left by one. VF is set to the value of the most significant bit of VX before the shift
				x = (opcode & 0x00000F00) >> 8;
				V[0xF] = (V[x] >> 7) & 0x1;//(V[x] & 0x80);//0x80=200_10=10000000_2////Getting the value of the most significant bit before shifting
				V[x] = (V[x] << 1) & 0x000000FF;//Left Shifting & Forcing 8 bits				
				break;
			default: throw new Exception("Unknown variation of form 8XY_(Expected: 0, 1, 2, 3, 4, 5, 6, 7, E)");
		}
		pc += 2;//Common pc's increment for all the existing options
	}
	
	private void op9XY0(int opcode){// 0x9XY0 Skips the next instruction if VX != VY
		if(V[(opcode & 0x00000F00) >> 8] != V[(opcode & 0x000000F0) >> 4]){
			pc += 4;//Skipping
		}else{
			pc += 2;//Not skipping... this is the common pc's increment
		}
	}
	
	private void opANNN(int opcode){// 0xANNN Sets I to NNN
		I = (opcode & 0x00000FFF);
		pc += 2;
	}
	
	private void opBNNN(int opcode){// 0xBNNN Jumps to V[0] + NNN
		pc = ((opcode & 0x00000FFF) + V[0]) & 0x00000FFF;//The program counter (PC) should be 16-bit [COW] -> (Limiting to 4095)
	}
	
	private void opCXKK(int opcode){// 0xCXKK Sets VX to a random byte AND KK
		V[(opcode & 0x00000F00) >> 8] = (int)(Math.random()*0xFF) & (opcode & 0x000000FF);//[0,1]*255 & KK
		pc += 2;
	}
	
	private void opDXYN(int opcode) {// Draws a sprite on the screen starting at coordinate (VX, VY)
		int x = V[(opcode & 0x00000F00)>>8];//Remember: integers are of 32 bits
		int y = V[(opcode & 0x000000F0)>>4];
		int n = (opcode & 0x0000000F);
		int pixels, fX, fY, buffer;		
		V[0xF] = 0;//VF is set to 1 if any screen pixels are flipped from set to unset 
		for(int i = 0; i<n; i++){//The sprite has a height of N
			pixels = memory[I+i];//Each row of 8 pixels is read as bit-coded starting from memory location I (which will remain unaltered)
			for(int j=0; j<8; j++){//Reading the row of 8 pixels
				if((pixels & (0x80>>j))!=0){//Accessing to the j^th bit: 0x80 = 10000000. We will shift according to j to progressively perform the AND where needed
					fX = (x+j) /*% 64*/;//Module forces turning around the screen, but some games are not compatible with this (e.g. Blitz) 
					fY = (y + i) /*% 32*/;
					if(fX>63 || fY>31) continue;
					buffer = fX + fY*64;
					if(screen[buffer] == true){
						V[0xF] = 1;//Collision 
						screen[buffer] = false;
					}else{
						screen[buffer] = true;
					}				
				}//We don't mind when it is 0 as 0_XOR_0 = 0 and 0_XOR_1 = 1 -> It does not change the screen
			}
		}
		shouldDraw = true;			
		pc += 2;
	}
	
	private void opE000(int opcode) throws Exception{// Jump to the next instruction if VX is equal/different to the pressed key
		switch(opcode & 0x000000FF){
			case 0x0000009E: // EX9E: Skips the next instruction if the key stored in VX is pressed
				if(keys[V[(opcode & 0x00000F00) >> 8]]){//==true
					pc += 4;
				}else{
					pc += 2;
				}
				break;
		
			case 0x000000A1: // EXA1: Skips the next instruction if the key stored in VX isn't pressed
				if(!keys[V[(opcode & 0x00000F00) >> 8]]){//==false
					pc += 4;
				}else{
					pc += 2;
				}
				break;
			default: throw new Exception("Unknown variation of form EX__(Expected: 9E or A1)");
		}
	}
	
	private void opF000(int opcode) throws Exception{
		int x;
		switch(opcode & 0x000000FF){
			case 0x00000007: // FX07: Sets VX to the value of the delay timer
				V[(opcode & 0x00000F00) >> 8] = (DT & 0x000000FF);
				pc += 2;
				break;
			case 0x0000000A: // FX0A: A key press is expected and finally stored in VX		
				Boolean keyPress = false;
				x = (opcode & 0x00000F00) >> 8;
				for(int i = 0; i < 16; i++){
					if(keys[i]){
						V[x] = i;
						keyPress = true; break;//Exit the loop (assuming just a required key)
					}
				}
				if(!keyPress){//No pressed key, skip this cycle and re-try in the next cycle (the program counter won't be moved)						
					return;
				}//Otherwise, we will move the program counter normally:
				pc += 2;
				break;
			case 0x00000015: // FX15: Sets the delay timer to VX
				DT = V[(opcode & 0x00000F00) >> 8];
				pc += 2;
				break;
			case 0x00000018: // FX18: Sets the sound timer to VX
				ST = V[(opcode & 0x00000F00) >> 8];
				pc += 2;
				break;
			case 0x0000001E: // FX1E: Adds VX to I
				int result = I + V[(opcode & 0x00000F00) >> 8];
				if(result > 0x0000FFF){// VF is set to 1 when range overflow (I+VX>0xFFF) and to 0 otherwise (See Note 3 in Wikipedia)
					V[0xF] = 1;
				}else{
					V[0xF] = 0;
				}
				I = result & 0x00000FFF;//Forcing 16 bits considering the register length (12 bits would force a valid direction though)
				pc += 2;
				break;
			case 0x00000029: // FX29: Sets I to the location of the sprite for the character in VX. Characters 0-F (in hexadecimal) are represented by a 4x5 font
				I = V[(opcode & 0x00000F00) >> 8] * 0x00000005;
				pc += 2;
				break;
			case 0x00000033: // FX33: Stores the Binary-coded decimal representation of VX at the addresses I, I plus 1, and I plus 2
				x = (opcode & 0x00000F00) >> 8;
				memory[I]     = (int) Math.floor(V[x]/100) % 10;
				memory[I + 1] = (int) Math.floor(V[x]/10) % 10;
				memory[I + 2] = (V[x] % 10);					
				pc += 2;
				break;
			case 0x00000055: // FX55: Stores V0 to VX in memory starting at address I					
				x = ((opcode & 0x00000F00) >> 8);
				for (int i = 0; i <=x ; i++){
					memory[I + i] = V[i];
				}// On the original interpreter, when the operation is done, I = I + X + 1. [WIK]
				//I = I + x + 1; //On current implementations, I is left unchanged. (No problems found when doing it though)
				pc += 2;
				break;
			case 0x00000065: // FX65: Fills V0 to VX with values from memory starting at address I
				x = ((opcode & 0x00000F00) >> 8);
				for (int i = 0; i <= x; ++i){
					V[i] = (memory[I + i] & 0x000000FF);//The V registers are of 8 bits			
				}// On the original interpreter, when the operation is done, I = I + X + 1: [WIK]
				//I = I + x + 1;//On current implementations, I is left unchanged.!!<WARNING. CHANGING THIS BREAKS BLINKY AND SYZYGY>
				pc += 2;
			break;
			default: throw new Exception("Unknown variation of form FX__");
		}
	}	
	///
	
	public void loadApplication(String appPath) throws Exception{
		try{
			FileInputStream fileInput = new FileInputStream(appPath);
			BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);
			int nextByte, memFocus = 512;//Initial offset in the memory
			while((nextByte = bufferedInput.read()) != -1){
				memory[memFocus] = nextByte;
				memFocus++;
			}		
			bufferedInput.close();
		}catch(Exception e){
			throw e;
		}		
	}
	
	public void updateTimers(){//Udating (decrementing) timers:
		if(DT>0){//Delay timer
			DT = DT - 1;
		}
		if(ST>0){//Sound timer
			ST = ST - 1;
			if(soundAllowed && ST>0){//When ST's value is greater than zero Chip-8 will sound [COW, WIK]
				//No sound FOR NOW
			}
		}
	}

	public Boolean getShouldDraw() {
		return shouldDraw;
	}

	public void setShouldDraw(boolean b) {
		this.shouldDraw = b;		
	}

	public Boolean[] getScreen(){
		return screen;
	}
	
	public void setKey(int focusKey, Boolean valKey){
		keys[focusKey] = valKey;
	}
}
