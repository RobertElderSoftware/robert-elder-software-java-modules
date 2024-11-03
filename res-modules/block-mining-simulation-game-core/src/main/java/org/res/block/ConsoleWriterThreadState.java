//  Copyright (c) 2024 Robert Elder Software Inc.
//   
//  Robert Elder Software Proprietary License
//  
//  In the context of this license, a 'Patron' means any individual who has made a 
//  membership pledge, a purchase of merchandise, a donation, or any other 
//  completed and committed financial contribution to Robert Elder Software Inc. 
//  for an amount of money greater than $1.  For a list of ways to contribute 
//  financially, visit https://blog.robertelder.org/patron
//  
//  Permission is hereby granted, to any 'Patron' the right to use this software 
//  and associated documentation under the following conditions:
//  
//  1) The 'Patron' must be a natural person and NOT a commercial entity.
//  2) The 'Patron' may use or modify the software for personal use only.
//  3) The 'Patron' is NOT permitted to re-distribute this software in any way, 
//  either unmodified, modified, or incorporated into another software product.
//  
//  An individual natural person may use this software for a temporary one-time 
//  trial period of up to 30 calendar days without becoming a 'Patron'.  After 
//  these 30 days have elapsed, the individual must either become a 'Patron' or 
//  stop using the software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
//  SOFTWARE.
package org.res.block;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;
import java.io.ByteArrayOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Collections;
import java.util.Comparator;

import java.lang.Thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class ConsoleWriterThreadState extends WorkItemQueueOwner<ConsoleWriterWorkItem> {
	protected Object lock = new Object();
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;
	private ClientBlockModelContext clientBlockModelContext;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private TextWidthMeasurementWorkItem currentTextWidthMeasurement = null;

	private BlockingQueue<TextWidthMeasurementWorkItem> pendingTextWidthRequests = new LinkedBlockingDeque<TextWidthMeasurementWorkItem>();
	private BlockingQueue<ConsoleWriteWorkItem> pendingConsoleWrites = new LinkedBlockingDeque<ConsoleWriteWorkItem>();

	private Long terminalWidth = null;
	private Long terminalHeight = null;
	private int [][] characterWidths = null;
	private int [][][] colourCodes = null;
	private String [][] characters = null;
	private boolean [][] changedFlags = null;

	public UserInterfaceFrameThreadState focusedFrame = null;

	private UserInterfaceSplit root;

	private int [] lastUsedColourCodes = new int [] {UserInterfaceFrameThreadState.RESET_BG_COLOR};

	public ConsoleWriterThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception{
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
		this.initializeConsole(80L, 24L); // Early setup is necessary so we can do text width calculations before drawing frame
	}

	public void notifyAllFramesOfFocusChange() throws Exception{
		List<UserInterfaceFrameThreadState> allFrames = root.collectUserInterfaceFrames();
		//  Notify all frames of the update so they can redraw border:
		for(UserInterfaceFrameThreadState frame : allFrames){
			frame.putWorkItem(new FrameFocusChangeWorkItem(frame, this.focusedFrame.getFrameDimensions()), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void focusOnNextFrame() throws Exception{
		UserInterfaceFrameThreadState previousFrame = this.focusedFrame;
		String oldFrameInfo = this.focusedFrame == null ? "null" : "frameId=" + this.focusedFrame.getFrameId();
		List<UserInterfaceFrameThreadState> allFrames = root.collectUserInterfaceFrames();
		Collections.sort(allFrames, new Comparator<UserInterfaceFrameThreadState>() {
			@Override
			public int compare(UserInterfaceFrameThreadState a, UserInterfaceFrameThreadState b) {
				return (int)(a.getFrameId() - b.getFrameId());
			}
		});

		UserInterfaceFrameThreadState newFocusedFrame;
		if(this.focusedFrame == null){
			if(allFrames.size() > 0){
				newFocusedFrame = allFrames.get(0);
			}else{
				throw new Exception("No frames to focus on!");
			}
		}else{
			int currentFrameIndex = -1;
			for(int i = 0; i < allFrames.size(); i++){
				if(allFrames.get(i).getFrameId() == this.focusedFrame.getFrameId()){
					if(currentFrameIndex != -1){
						throw new Exception("Two frames have same id?");
					}else{
						currentFrameIndex = i;
					}
				}
			}
			if(currentFrameIndex == -1){
				throw new Exception("Could not find frame index of currently focused frame?");
			}else{
				int newFrameIndex = (currentFrameIndex + 1) % allFrames.size();
				newFocusedFrame = allFrames.get(newFrameIndex);
				logger.info("Focused on frame at index " + newFrameIndex + " with frameId=" + newFocusedFrame.getFrameId());
			}
		}

		String newFrameInfo = newFocusedFrame == null ? "null" : "frameId=" + newFocusedFrame.getFrameId();
		logger.info("Switched from " + oldFrameInfo + " to " + newFrameInfo);
		this.focusedFrame = newFocusedFrame;

	}

	public UserInterfaceSplit getRootSplit(){
		return this.root;
	}

	public void setRootSplit(UserInterfaceSplit split) throws Exception{
		this.root = split;
		if(this.focusedFrame instanceof HelpMenuFrameThreadState){ // TODO: Remove this.  Required to prevent crashes when closing help menu.
			List<UserInterfaceFrameThreadState> allFrames = root.collectUserInterfaceFrames();
			if(allFrames.size() > 0){
				this.focusedFrame = allFrames.get(0);
			}else{
				throw new Exception("Not expected.");
			}
		}else{ //  A hack to select help menu after opening it:
			//  Select the first frame that is a help menu:
			List<UserInterfaceFrameThreadState> allFrames = root.collectUserInterfaceFrames();
			for(UserInterfaceFrameThreadState frame : allFrames){
				if(frame instanceof HelpMenuFrameThreadState){
					this.focusedFrame = frame;
				}
			}
		}
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public void addPendingTextWidthRequest(TextWidthMeasurementWorkItem w){
		synchronized(lock){
			this.pendingTextWidthRequests.add(w);
		}
	}

	public void addPendingConsoleWrite(ConsoleWriteWorkItem w){
		synchronized(lock){
			this.pendingConsoleWrites.add(w);
		}
	}

	public void onCursorPositionReport(CursorPositionReport cpr) throws Exception{
		synchronized(lock){
			if(this.currentTextWidthMeasurement == null){
				throw new Exception("ERROR:  Discarding cursor position report (" + cpr.getX() + "," + cpr.getY() + ") since there appears to be no active text width request?");
			}else{
				this.currentTextWidthMeasurement.notifyOfCurrentCursorPosition(cpr.getX(), cpr.getY());
			}
		}
	}

	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		if(ansiEscapeSequence instanceof CursorPositionReport){
			CursorPositionReport cpr = (CursorPositionReport)ansiEscapeSequence;
			logger.info("Got Character Position Report: x=" + cpr.getX() + ", y=" + cpr.getY() + "");
			this.onCursorPositionReport(cpr);
		}else{
			logger.info("Got other type of ansi escape sequence, pass to the focused frame: " + ansiEscapeSequence.getClass().getName());
			if(this.focusedFrame != null){
				this.focusedFrame.putWorkItem(new ProcessFrameAnsiEscapeSequenceWorkItem(this.focusedFrame, ansiEscapeSequence), WorkItemPriority.PRIORITY_LOW);
			}
		}
	}

	public void prepareTerminalTextChange(int [][] newCharacterWidths, int [][][] newColourCodes, String [][] newCharacters, boolean [][] hasChange, int xOffset, int yOffset, int xChangeSize, int yChangeSize, FrameDimensions frameDimensions) throws Exception{
		for(int j = 0; j < yChangeSize; j++){
			for(int i = 0; i < xChangeSize; i++){
				if(hasChange[i][j]){
					int extraSpaceForLastCharacter = newCharacterWidths[i][j] < 1 ? 0 : (newCharacterWidths[i][j] -1);
					if(
						//  Don't write beyond current terminal dimenions
						//  Individual frames should be inside terminal area
						//  but terminal resize events means there could
						//  be a temporary inconsistency that makes this check
						//  necessary:
						(xOffset + i) < terminalWidth &&
						(yOffset + j) < terminalHeight &&
						(xOffset + i) >= 0 &&
						(yOffset + j) >= 0 &&
						//  Only allow a frame to write inside it's own borders:
						(xOffset + i) < (frameDimensions.getFrameOffsetX() + frameDimensions.getFrameWidth() - extraSpaceForLastCharacter) &&
						(yOffset + j) < (frameDimensions.getFrameOffsetY() + frameDimensions.getFrameHeight()) &&
						(xOffset + i) >= frameDimensions.getFrameOffsetX() &&
						(yOffset + j) >= frameDimensions.getFrameOffsetY()
					){
						//  If it's changing in this update, or there is a change that hasn't been printed yet.
						boolean hasChanged = !(
							this.characterWidths[xOffset + i][yOffset + j] == newCharacterWidths[i][j] &&
							Arrays.equals(this.colourCodes[xOffset + i][yOffset + j], newColourCodes[i][j]) &&
							this.characters[xOffset + i][yOffset + j] == newCharacters[i][j]
						) || this.changedFlags[xOffset + i][yOffset + j];
						this.characterWidths[xOffset + i][yOffset + j] = newCharacterWidths[i][j];
						this.colourCodes[xOffset + i][yOffset + j] = newColourCodes[i][j];
						this.characters[xOffset + i][yOffset + j] = newCharacters[i][j];
						this.changedFlags[xOffset + i][yOffset + j] = hasChanged;
						if(newCharacterWidths[i][j] > 1){ //  If the character takes up more than one column, skip to the appropriate column:
							i += newCharacterWidths[i][j] - 1;
						}
					}else{
						//throw new Exception("Discarding character '" + newCharacters[i][j] + "' because if was out of bounds at x=" + (xOffset + i) + ", y=" + (yOffset + j));
					}
				}
			}
		}
	}

	public void printTerminalTextChanges(boolean resetCursorPosition) throws Exception{
		for(int j = 0; j < this.terminalHeight.intValue(); j++){
			boolean mustSetCursorPosition = true;
			boolean mustSetColourCodes = true;
			for(int i = 0; i < this.terminalWidth.intValue(); i++){
				//  Try to intelligently issue as few ANSI escape sequences as possible:
				if(!Arrays.equals(this.colourCodes[i][j], lastUsedColourCodes)){
					mustSetColourCodes = true;
				}
				if(this.changedFlags[i][j]){
					if(mustSetCursorPosition){
						String currentPositionSequence = "\033[" + (j+1) + ";" + (i+1) + "H";
						System.out.print(currentPositionSequence);
						mustSetCursorPosition = false;
					}
					if(mustSetColourCodes){
						List<String> codes = new ArrayList<String>();
						for(int c : this.colourCodes[i][j]){
							codes.add(String.valueOf(c));
						}
						String currentColorSequence = "\033[" + String.join(";", codes) + "m";
						System.out.print(currentColorSequence);
						mustSetColourCodes = false;
						lastUsedColourCodes = this.colourCodes[i][j];
					}
					System.out.print(this.characters[i][j]);
					this.changedFlags[i][j] = false;
				}else{
					mustSetCursorPosition = true;
				}
				if(this.characterWidths[i][j] > 1){ //  If the character takes up more than one column, skip to the appropriate column:
					i += this.characterWidths[i][j] - 1;
				}
			}
		}
		if(resetCursorPosition){
			System.out.print("\033[0;0H"); //  Move cursor to 0,0 after every print.
		}
	}

	public void initializeConsole(Long terminalWidth, Long terminalHeight) throws Exception{
		this.terminalWidth = terminalWidth;
		this.terminalHeight = terminalHeight;
		//  Initialize screen to be all spaces:
		this.characterWidths = new int [terminalWidth.intValue()][terminalHeight.intValue()];
		for(int [] a : this.characterWidths){
			Arrays.fill(a, 1);
		}
		this.colourCodes = new int [terminalWidth.intValue()][terminalHeight.intValue()][1];
		for(int [][] a : this.colourCodes){
			for(int [] b : a){
				Arrays.fill(b, UserInterfaceFrameThreadState.RESET_BG_COLOR);
			}
		}
		this.characters = new String [terminalWidth.intValue()][terminalHeight.intValue()];
		for(String [] a : this.characters){
			Arrays.fill(a, " ");
		}
		this.changedFlags = new boolean [terminalWidth.intValue()][terminalHeight.intValue()];
		for(boolean [] a : this.changedFlags){
			Arrays.fill(a, true);
		}
		this.printTerminalTextChanges(true);
	}

	public void onTerminalDimensionsChanged(Long terminalWidth, Long terminalHeight, Long frameCharacterWidth) throws Exception{
		this.initializeConsole(terminalWidth, terminalHeight);

		//  When the terminal size changes, send a notify to all of the user interface frames to let them know about it
		FrameDimensions fd = new FrameDimensions(frameCharacterWidth, terminalWidth, terminalHeight, 0L, 0L, terminalWidth, terminalHeight);
		FrameBordersDescription frameBordersDescription = this.root.collectAllConnectionPoints(fd);
		if(this.focusedFrame == null){
			this.focusOnNextFrame();
		}
		this.root.setEquidistantFrameDimensions(fd, frameBordersDescription);
		this.notifyAllFramesOfFocusChange();
	}

	public void sendKeyboardInputToFocusedFrame(byte b) throws Exception {
		byte [] bytesToSend = new byte [] {b};
		if(this.focusedFrame != null){
			this.focusedFrame.putWorkItem(new ProcessFrameInputBytesWorkItem(this.focusedFrame, bytesToSend), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void onKeyboardInput(byte [] characters) throws Exception {
		logger.info("Saw keyboard input: " + new String(characters, "UTF-8"));
		UserInteractionConfig ki = this.blockManagerThreadCollection.getUserInteractionConfig();
		for(byte b : characters){
			String actionString = new String(new byte [] {b}, "UTF-8");
			UserInterfaceActionType action = ki.getKeyboardActionFromString(actionString);

			if(action == null){
				this.sendKeyboardInputToFocusedFrame(b);
			}else{
				switch(action){
					case ACTION_QUIT:{
						logger.info("The 'q' key was pressed.  Exiting...");
						this.blockManagerThreadCollection.setIsProcessFinished(true, null); // Start shutting down the entire application.
						break;
					}case ACTION_HELP_MENU_TOGGLE:{
						this.clientBlockModelContext.putWorkItem(new TellClientOpenHelpMenuWorkItem(this.clientBlockModelContext), WorkItemPriority.PRIORITY_LOW);
						break;
					}case ACTION_TAB_NEXT_FRAME:{
						this.focusOnNextFrame();
						this.notifyAllFramesOfFocusChange();
						break;
					}default:{
						this.sendKeyboardInputToFocusedFrame(b);
					}
				}
			}
		}
	}

	public ConsoleWriterWorkItem takeWorkItem() throws Exception {
		ConsoleWriterWorkItem w = this.workItemQueue.takeWorkItem();
		return w;
	}

	public void putWorkItem(ConsoleWriterWorkItem workItem, WorkItemPriority priority) throws Exception{
		this.workItemQueue.putWorkItem(workItem, priority);
	}

	private void invalidateScreenArea(int startX, int startY, int endX, int endY){
		//  Invalidate a sub-area of screen so that the characters are that location will 
		//  be printed on the next print attempt.
		int width = endX - startX;
		int height = endY - startY;
		
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.changedFlags[i + startX][j + startY] = true;
			}
		}
	}

	public boolean doBackgroundProcessing() throws Exception{
		synchronized(lock){
			//  If there is a character width measurement request available, start working on it:
			if(currentTextWidthMeasurement == null && this.pendingTextWidthRequests.size() > 0){
				this.currentTextWidthMeasurement = this.pendingTextWidthRequests.take();
				Long x1 = currentTextWidthMeasurement.getX1();
				Long y1 = currentTextWidthMeasurement.getY1();
				String text = currentTextWidthMeasurement.getText();

				logger.info("About to print test text '" + text + "' so width can be measured.");
				System.out.print("\033[" + y1 + ";" + x1 + "H"); //  Move cursor known reference point to calculate offset of text.
				System.out.flush();
				System.out.print(text); //  Print the text for which we want to measure width.
				System.out.flush();
				System.out.println("\033[6n");  //  Request cursor position measurement.
				System.out.flush();
				//  We just by-passed the screen printing buffer, so invalidate the area of the
				//  screen where the test character was and re-print whatever was there:
				this.invalidateScreenArea(0, 0, 16, 4);
				this.printTerminalTextChanges(false);
				logger.info("Finished printing test text '" + text + "' and issued cursor re-positioning request to calculate width. Waiting for result on stdin...");
			}
			if(this.currentTextWidthMeasurement == null){
				if(this.terminalWidth != null && this.terminalHeight != null){
					while(this.pendingConsoleWrites.size() > 0){
						ConsoleWriteWorkItem w = this.pendingConsoleWrites.take();
						w.prepareTerminalTextChange();
					}
					this.printTerminalTextChanges(true);
				}
			}else{
				TextWidthMeasurementWorkItemResult r = this.currentTextWidthMeasurement.getResult();
				if(r != null){ //  Did we actually get a result yet?
					this.addResultForThreadId(r, this.currentTextWidthMeasurement.getThreadId()); //  Unblock whatever thread was waiting.
					logger.info("In CharacterWidthMeasurementThreadState, Added a text width result for '" + this.currentTextWidthMeasurement.getText() + "' with a value of deltaX=" + r.getDeltaX() + ", deltaY=" + r.getDeltaY() + ",  thread_id=" + this.currentTextWidthMeasurement.getThreadId());
					this.currentTextWidthMeasurement = null; //  Allow for processing of next character width request.
				}
			}
		}
		return pendingConsoleWrites.size() > 0; // There is no additional work we can do until we get another work item.
	}

	public WorkItemResult putBlockingWorkItem(ConsoleWriterWorkItem workItem, WorkItemPriority priority) throws Exception {
		return this.workItemQueue.putBlockingWorkItem(workItem, priority);
	}

	public void addResultForThreadId(WorkItemResult workItemResult, Long threadId) throws Exception {
		this.workItemQueue.addResultForThreadId(workItemResult, threadId);
	}
}
