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
	private final List<MapAreaInterfaceThreadState> mapAreaInterfaceThreadStates = new ArrayList<MapAreaInterfaceThreadState>();
	private final List<InventoryInterfaceThreadState> inventoryInterfaceThreadStates = new ArrayList<InventoryInterfaceThreadState>();

	private BlockingQueue<TextWidthMeasurementWorkItem> pendingTextWidthRequests = new LinkedBlockingDeque<TextWidthMeasurementWorkItem>();
	private BlockingQueue<ConsoleWriteWorkItem> pendingConsoleWrites = new LinkedBlockingDeque<ConsoleWriteWorkItem>();

	private Long terminalWidth = null;
	private Long terminalHeight = null;
	private FrameDimensions currentTerminalFrameDimensions = null;
	private boolean menuActive = false;
	private int numScreenOutputBuffers = 2;
	public static final int BUFFER_INDEX_DEFAULT = 0;
	public static final int BUFFER_INDEX_MENU = 1;
	private ScreenOutputBuffer [] screenOutputBuffer = new ScreenOutputBuffer [this.numScreenOutputBuffers];


	private WorkItemProcessorTask<UIWorkItem> helpMenuThread = null;
	private HelpMenuFrameThreadState helpMenuThreadState = null;

	private EmptyFrameThreadState emptyFrameThreadState1 = null;
	private EmptyFrameThreadState emptyFrameThreadState2 = null;
	private EmptyFrameThreadState emptyFrameThreadState3 = null;
	private EmptyFrameThreadState emptyFrameThreadState4 = null;
	private EmptyFrameThreadState emptyFrameThreadState5 = null;
	private EmptyFrameThreadState emptyFrameThreadState6 = null;
	private EmptyFrameThreadState emptyFrameThreadState7 = null;

	public UserInterfaceFrameThreadState focusedFrame = null;

	private UserInterfaceSplit root;

	private int [] lastUsedColourCodes = new int [] {UserInterfaceFrameThreadState.RESET_BG_COLOR};

	public ConsoleWriterThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception{
		for(int i = 0; i < this.numScreenOutputBuffers; i++){
			this.screenOutputBuffer[i]  = new ScreenOutputBuffer();
		}
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
		this.initializeConsole(80L, 24L); // Early setup is necessary so we can do text width calculations before drawing frame

		int numMapAreas = 1;
		int numInventoryAreas = 1;
		for(int i = 0; i < numMapAreas; i++){
			this.mapAreaInterfaceThreadStates.add(new MapAreaInterfaceThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext));
		}
		for(int i = 0; i < numInventoryAreas; i++){
			this.inventoryInterfaceThreadStates.add(new InventoryInterfaceThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext));
		}

		this.emptyFrameThreadState1 = new EmptyFrameThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext);
		this.emptyFrameThreadState2 = new EmptyFrameThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext);
		this.emptyFrameThreadState3 = new EmptyFrameThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext);
		this.emptyFrameThreadState4 = new EmptyFrameThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext);
		this.emptyFrameThreadState5 = new EmptyFrameThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext);
		this.emptyFrameThreadState6 = new EmptyFrameThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext);
		this.emptyFrameThreadState7 = new EmptyFrameThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext);

		boolean useMultiSplitDemo = false;
		if(useMultiSplitDemo){
			List<UserInterfaceSplit> splits1 = new ArrayList<UserInterfaceSplit>();
			for(MapAreaInterfaceThreadState mapAreaInterfaceThreadState : this.mapAreaInterfaceThreadStates){
				splits1.add(new UserInterfaceSplitLeafNode(mapAreaInterfaceThreadState));
			}
			for(InventoryInterfaceThreadState inventoryInterfaceThreadState : this.inventoryInterfaceThreadStates){
				splits1.add(new UserInterfaceSplitLeafNode(inventoryInterfaceThreadState));
			}
			splits1.add(new UserInterfaceSplitLeafNode(this.emptyFrameThreadState1));

			List<UserInterfaceSplit> splits2 = new ArrayList<UserInterfaceSplit>();
			splits2.add(new UserInterfaceSplitLeafNode(this.emptyFrameThreadState2));
			splits2.add(new UserInterfaceSplitLeafNode(this.emptyFrameThreadState3));
			splits2.add(new UserInterfaceSplitLeafNode(this.emptyFrameThreadState4));

			List<UserInterfaceSplit> splits3 = new ArrayList<UserInterfaceSplit>();
			splits3.add(new UserInterfaceSplitLeafNode(this.emptyFrameThreadState5));
			splits3.add(new UserInterfaceSplitLeafNode(this.emptyFrameThreadState6));
			splits3.add(new UserInterfaceSplitLeafNode(this.emptyFrameThreadState7));

			List<UserInterfaceSplit> topSplits = new ArrayList<UserInterfaceSplit>();
			topSplits.add(new UserInterfaceSplitHorizontal(splits1));
			topSplits.add(new UserInterfaceSplitHorizontal(splits2));
			topSplits.add(new UserInterfaceSplitHorizontal(splits3));

			this.setRootSplit(new UserInterfaceSplitVertical(topSplits));

			this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<UIWorkItem>(this.emptyFrameThreadState1, UIWorkItem.class));
			this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<UIWorkItem>(this.emptyFrameThreadState2, UIWorkItem.class));
			this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<UIWorkItem>(this.emptyFrameThreadState3, UIWorkItem.class));
			this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<UIWorkItem>(this.emptyFrameThreadState4, UIWorkItem.class));
			this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<UIWorkItem>(this.emptyFrameThreadState5, UIWorkItem.class));
			this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<UIWorkItem>(this.emptyFrameThreadState6, UIWorkItem.class));
			this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<UIWorkItem>(this.emptyFrameThreadState7, UIWorkItem.class));
		}else{
			List<Double> framePercents = new ArrayList<Double>();
			List<UserInterfaceSplit> splits = new ArrayList<UserInterfaceSplit>();
			for(MapAreaInterfaceThreadState mapAreaInterfaceThreadState : this.mapAreaInterfaceThreadStates){
				splits.add(new UserInterfaceSplitLeafNode(mapAreaInterfaceThreadState));
				framePercents.add(0.75 / this.mapAreaInterfaceThreadStates.size());
			}
			for(InventoryInterfaceThreadState inventoryInterfaceThreadState : this.inventoryInterfaceThreadStates){
				splits.add(new UserInterfaceSplitLeafNode(inventoryInterfaceThreadState));
				framePercents.add(0.25 / this.inventoryInterfaceThreadStates.size());
			}

			this.setRootSplit(new UserInterfaceSplitHorizontal(splits, framePercents));
		}

		this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<BlockModelContextWorkItem>(this.clientBlockModelContext, BlockModelContextWorkItem.class));
		for(MapAreaInterfaceThreadState mapAreaInterfaceThreadState : this.mapAreaInterfaceThreadStates){
			this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<UIWorkItem>(mapAreaInterfaceThreadState, UIWorkItem.class));
		}
		for(InventoryInterfaceThreadState inventoryInterfaceThreadState : this.inventoryInterfaceThreadStates){
			this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<UIWorkItem>(inventoryInterfaceThreadState, UIWorkItem.class));
		}
	}

	public void onPlayerInventoryChange(PlayerInventory playerInventory) throws Exception{
		for(InventoryInterfaceThreadState inventoryInterfaceThreadState : this.inventoryInterfaceThreadStates){
			inventoryInterfaceThreadState.putWorkItem(new PlayerInventoryChangeWorkItem(inventoryInterfaceThreadState, new PlayerInventory(playerInventory.getBlockData())), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void updateMapAreaFlags(CuboidAddress areaToUpdate) throws Exception {
		for(MapAreaInterfaceThreadState mapAreaInterfaceThreadState : this.mapAreaInterfaceThreadStates){
			mapAreaInterfaceThreadState.putWorkItem(new UpdateMapAreaFlagsWorkItem(mapAreaInterfaceThreadState, areaToUpdate.copy()), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void onPlayerPositionChange(Coordinate previousPosition, Coordinate newPosition) throws Exception{
		for(MapAreaInterfaceThreadState mapAreaInterfaceThreadState : this.mapAreaInterfaceThreadStates){
			mapAreaInterfaceThreadState.putWorkItem(new MapAreaNotifyPlayerPositionChangeWorkItem(mapAreaInterfaceThreadState, previousPosition, newPosition), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void notifyAllFramesOfFocusChange() throws Exception{
		this.renderHelpMenu();

		List<UserInterfaceFrameThreadState> allFrames = root.collectUserInterfaceFrames();
		//  Notify all frames of the update so they can redraw border:
		for(UserInterfaceFrameThreadState frame : allFrames){
			frame.putWorkItem(new FrameFocusChangeWorkItem(frame, this.focusedFrame.getFrameDimensions()), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void renderHelpMenu() throws Exception{
		int terminalWidth = this.currentTerminalFrameDimensions.getTerminalWidth().intValue();
		int terminalHeight = this.currentTerminalFrameDimensions.getTerminalHeight().intValue();
		int menuWidth = 20;
		int menuHeight = 10;
		int [][] characterWidths = new int[menuWidth][menuHeight];
		int [][][] colourCodes = new int[menuWidth][menuHeight][2];
		String [][] characters = new String[menuWidth][menuHeight];
		boolean [][] hasChange = new boolean [menuWidth][menuHeight];

		for(int i = 0; i < menuWidth; i++){
			for(int j = 0; j < menuHeight; j++){
				String character = this.menuActive ? " " : null;
				int [] colours = this.menuActive ? new int [] {UserInterfaceFrameThreadState.GREEN_BG_COLOR, UserInterfaceFrameThreadState.BLACK_FG_COLOR} : new int [] {};
				int characterWidth = this.menuActive ? 1 : 0;
				characterWidths[i][j] = characterWidth;
				colourCodes[i][j] = colours;
				characters[i][j] = character;
				hasChange[i][j] = true;
			}
		}

		int xOffset = (terminalWidth / 2) - (menuWidth / 2);
		int yOffset = (terminalHeight / 2) - (menuHeight / 2);
		int xSize = menuWidth;
		int ySize = menuHeight;

		this.prepareTerminalTextChange(characterWidths, colourCodes, characters, hasChange, xOffset, yOffset, xSize, ySize, this.currentTerminalFrameDimensions, ConsoleWriterThreadState.BUFFER_INDEX_MENU);

		if(!this.menuActive){
			//  De-activate everything in menu layer:
			this.setScreenAreaChangeStates(0, 0, terminalWidth, terminalHeight, ConsoleWriterThreadState.BUFFER_INDEX_MENU, false);
			//  Allow refresh of everything that was below the menu:
			this.setScreenAreaChangeStates(xOffset, yOffset, xOffset + menuWidth, yOffset + menuHeight, ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT, true);
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

	public void sendConsolePrintMessage(int [][] characterWidths, int [][][] colourCodes, String [][] characters, boolean [][] hasChange, int xOffset, int yOffset, int xSize, int ySize, FrameDimensions fd, int bufferIndex) throws Exception{

		this.putWorkItem(new ConsoleWriteWorkItem(this.clientBlockModelContext.getConsoleWriterThreadState(), characterWidths, colourCodes, characters, hasChange, xOffset, yOffset, xSize, ySize, fd, bufferIndex), WorkItemPriority.PRIORITY_LOW);
	}

	protected void printTextAtScreenXY(ColouredTextFragment colouredTextFragment, Long drawOffsetX, Long drawOffsetY, boolean xDirection) throws Exception{
		this.printTextAtScreenXY(new ColouredTextFragmentList(Arrays.asList(colouredTextFragment)), drawOffsetX, drawOffsetY, this.currentTerminalFrameDimensions, xDirection);
	}

	protected void printTextAtScreenXY(ColouredTextFragmentList colouredTextFragmentList, Long drawOffsetX, Long drawOffsetY, boolean xDirection) throws Exception{
		printTextAtScreenXY(colouredTextFragmentList, drawOffsetX, drawOffsetY, this.currentTerminalFrameDimensions, xDirection);
	}

	protected void printTextAtScreenXY(ColouredTextFragmentList colouredTextFragmentList, Long drawOffsetX, Long drawOffsetY, FrameDimensions fd, boolean xDirection) throws Exception{
		List<ColouredCharacter> colouredCharacters = colouredTextFragmentList.getColouredCharacters();
		List<String> charactersToPrint = new ArrayList<String>();
		int [][] newColourCodes = new int [colouredCharacters.size()][];
		for(int i = 0; i < colouredCharacters.size(); i++){
			ColouredCharacter c = colouredCharacters.get(i);
			newColourCodes[i] = c.getAnsiColourCodes();
			charactersToPrint.add(c.getCharacter());
		}
		//  Print a string in either the X or Y Direction.
		logger.info("charactersToPrint=" + charactersToPrint);
		if(charactersToPrint.size() != newColourCodes.length){
			throw new Exception("Size missmatch in colour code array: " + charactersToPrint.size() + " verus " + newColourCodes.length);
		}

		int totalWidth = 0;
		for(String s : charactersToPrint){
			int chrWidth = this.clientBlockModelContext.measureTextLengthOnTerminal(s).getDeltaX().intValue();
			logger.info("chrWidth=" + chrWidth + " for '" + s + "' (" + BlockModelContext.convertToHex(s.getBytes("UTF-8")) + " in hex).");
			totalWidth += (chrWidth < 1 ? 1 : chrWidth);
		}

		int xDimSize = xDirection ? totalWidth : 1;
		int yDimSize = xDirection ? 1 : totalWidth;
		int [][] characterWidths = new int[xDimSize][yDimSize];
		int [][][] colourCodes = new int[xDimSize][yDimSize][1];
		String [][] characters = new String[xDimSize][yDimSize];
		boolean [][] hasChange = new boolean[xDimSize][yDimSize];

		int currentOffset = 0;
		for(int i = 0; i < charactersToPrint.size(); i++){
			String s = charactersToPrint.get(i);
			int chrWidth = this.clientBlockModelContext.measureTextLengthOnTerminal(s).getDeltaX().intValue();
			int xIndex = xDirection ? currentOffset : 0;
			int yIndex = xDirection ? 0 : currentOffset;
			colourCodes[xIndex][yIndex] = newColourCodes[i];
			characters[xIndex][yIndex] = s;
			characterWidths[xIndex][yIndex] = chrWidth;
			hasChange[xIndex][yIndex] = true;
			if(xDirection){
				currentOffset++;
				//  For multi-column characters in 'x' direction, reset any of the 'covered'
				//  columns take up by the multi-column character:
				for(int k = 1; k < chrWidth; k++){
					colourCodes[currentOffset][yIndex] = new int [] {};
					characters[currentOffset][yIndex] = null;
					characterWidths[currentOffset][yIndex] = 0;
					hasChange[currentOffset][yIndex] = true;
					currentOffset++;
				}
			}else{
				//  Always advance by 1 if printing in Y direction.
				currentOffset += 1;
			}
		}

		int xOffset = drawOffsetX.intValue() + fd.getFrameOffsetX().intValue();
		int yOffset = drawOffsetY.intValue() + fd.getFrameOffsetY().intValue();
		int xSize = xDimSize;
		int ySize = yDimSize;

		sendConsolePrintMessage(characterWidths, colourCodes, characters, hasChange, xOffset, yOffset, xSize, ySize, fd, ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT);
	}

	public void prepareTerminalTextChange(int [][] newCharacterWidths, int [][][] newColourCodes, String [][] newCharacters, boolean [][] hasChange, int xOffset, int yOffset, int xChangeSize, int yChangeSize, FrameDimensions frameDimensions, int bufferIndex) throws Exception{
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
							this.screenOutputBuffer[bufferIndex].characterWidths[xOffset + i][yOffset + j] == newCharacterWidths[i][j] &&
							Arrays.equals(this.screenOutputBuffer[bufferIndex].colourCodes[xOffset + i][yOffset + j], newColourCodes[i][j]) &&
							this.screenOutputBuffer[bufferIndex].characters[xOffset + i][yOffset + j] == newCharacters[i][j]
						) || this.screenOutputBuffer[bufferIndex].changedFlags[xOffset + i][yOffset + j];
						this.screenOutputBuffer[bufferIndex].characterWidths[xOffset + i][yOffset + j] = newCharacterWidths[i][j];
						this.screenOutputBuffer[bufferIndex].colourCodes[xOffset + i][yOffset + j] = newColourCodes[i][j];
						this.screenOutputBuffer[bufferIndex].characters[xOffset + i][yOffset + j] = newCharacters[i][j];
						this.screenOutputBuffer[bufferIndex].changedFlags[xOffset + i][yOffset + j] = hasChanged;
						//  If we're printing on a screen buffer that's in front, we will evetually need to update whatever was behind it:
						if(hasChanged){
							for(int k = bufferIndex; k >= 0 ; k--){
								this.screenOutputBuffer[bufferIndex].changedFlags[xOffset + i][yOffset + j] = hasChanged;
							}
						}
					}else{
						//throw new Exception("Discarding character '" + newCharacters[i][j] + "' because if was out of bounds at x=" + (xOffset + i) + ", y=" + (yOffset + j));
					}
				}
			}
		}
	}

	public void printTerminalTextChanges(boolean resetCursorPosition) throws Exception{
		boolean useRightToLeftPrint = this.blockManagerThreadCollection.getRightToLeftPrint();
		int startColumn = useRightToLeftPrint ? this.terminalWidth.intValue() -1 : 0;
		int endColumn = useRightToLeftPrint ? -1 : this.terminalWidth.intValue();
		int loopUpdate = useRightToLeftPrint ? -1 : 1;
		boolean resetState = useRightToLeftPrint ? true : false;
		for(int j = 0; j < this.terminalHeight.intValue(); j++){
			boolean mustSetCursorPosition = true;
			boolean mustSetColourCodes = true;
			for(int i = startColumn; i != endColumn; i += loopUpdate){
				int bufferIndex = numScreenOutputBuffers -1;
				while(!this.screenOutputBuffer[bufferIndex].changedFlags[i][j] && bufferIndex > 0){
					bufferIndex--;
				}
				//  Try to intelligently issue as few ANSI escape sequences as possible:
				if(!Arrays.equals(this.screenOutputBuffer[bufferIndex].colourCodes[i][j], lastUsedColourCodes)){
					mustSetColourCodes = true;
				}
				if(this.screenOutputBuffer[bufferIndex].changedFlags[i][j]){
					if(mustSetCursorPosition){
						String currentPositionSequence = "\033[" + (j+1) + ";" + (i+1) + "H";
						System.out.print(currentPositionSequence);
						mustSetCursorPosition = resetState;
					}
					if(mustSetColourCodes){
						List<String> codes = new ArrayList<String>();
						for(int c : this.screenOutputBuffer[bufferIndex].colourCodes[i][j]){
							codes.add(String.valueOf(c));
						}
						String currentColorSequence = "\033[0m\033[" + String.join(";", codes) + "m";
						System.out.print(currentColorSequence);
						mustSetColourCodes = resetState;
						lastUsedColourCodes = this.screenOutputBuffer[bufferIndex].colourCodes[i][j];
					}
					if(this.screenOutputBuffer[bufferIndex].characters[i][j] != null){
						System.out.print(this.screenOutputBuffer[bufferIndex].characters[i][j]);
					}
					if(bufferIndex == 0){ //  Always keep overlay layers printed on top
						this.screenOutputBuffer[bufferIndex].changedFlags[i][j] = false;
					}
				}else{
					mustSetCursorPosition = true;
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
		for(int i = 0; i < this.numScreenOutputBuffers; i++){
			this.screenOutputBuffer[i].initialize(this.terminalWidth, this.terminalHeight);
		}
	}

	public void onTerminalDimensionsChanged(Long terminalWidth, Long terminalHeight, Long frameCharacterWidth) throws Exception{
		this.initializeConsole(terminalWidth, terminalHeight);

		//  When the terminal size changes, send a notify to all of the user interface frames to let them know about it
		this.currentTerminalFrameDimensions = new FrameDimensions(frameCharacterWidth, terminalWidth, terminalHeight, 0L, 0L, terminalWidth, terminalHeight);
		FrameBordersDescription frameBordersDescription = this.root.collectAllConnectionPoints(this.currentTerminalFrameDimensions);
		if(this.focusedFrame == null){
			this.focusOnNextFrame();
		}
		this.root.setEquidistantFrameDimensions(this.currentTerminalFrameDimensions, frameBordersDescription);
		this.notifyAllFramesOfFocusChange();
	}

	public void sendKeyboardInputToFocusedFrame(byte b) throws Exception {
		byte [] bytesToSend = new byte [] {b};
		if(this.focusedFrame != null){
			this.focusedFrame.putWorkItem(new ProcessFrameInputBytesWorkItem(this.focusedFrame, bytesToSend), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void onHelpMenuOpen() throws Exception{
		if(this.helpMenuThreadState == null){
			this.helpMenuThreadState = new HelpMenuFrameThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext);
			this.helpMenuThread = new WorkItemProcessorTask<UIWorkItem>(this.helpMenuThreadState, UIWorkItem.class);
			this.blockManagerThreadCollection.addThread(this.helpMenuThread);

			List<UserInterfaceSplit> newTopSplit = new ArrayList<UserInterfaceSplit>();
			newTopSplit.add(this.getRootSplit());
			newTopSplit.add(new UserInterfaceSplitLeafNode(this.helpMenuThreadState));

			this.setRootSplit(new UserInterfaceSplitVertical(newTopSplit));
			this.clientBlockModelContext.putWorkItem(new TellClientTerminalChangedWorkItem(this.clientBlockModelContext), WorkItemPriority.PRIORITY_LOW);
		}else{
			UserInterfaceSplitVertical top = (UserInterfaceSplitVertical)this.getRootSplit();

			this.setRootSplit(top.getSplitParts().get(0));

			this.helpMenuThreadState = null;
			this.helpMenuThread.setIsThreadFinished(true);
			this.helpMenuThread.interrupt();
			this.blockManagerThreadCollection.removeThread(this.helpMenuThread);
			this.helpMenuThread = null;
			this.clientBlockModelContext.putWorkItem(new TellClientTerminalChangedWorkItem(this.clientBlockModelContext), WorkItemPriority.PRIORITY_LOW);
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
						this.onHelpMenuOpen();
						//this.menuActive = !this.menuActive;
						//this.notifyAllFramesOfFocusChange();
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

	private void setScreenAreaChangeStates(int startX, int startY, int endX, int endY, int bufferIndex, boolean state){
		//  Invalidate a sub-area of screen so that the characters are that location will 
		//  be printed on the next print attempt.
		int width = endX - startX;
		int height = endY - startY;
		
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.screenOutputBuffer[bufferIndex].changedFlags[i + startX][j + startY] = state;
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
				this.setScreenAreaChangeStates(0, 0, 16, 4, ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT, true);
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
