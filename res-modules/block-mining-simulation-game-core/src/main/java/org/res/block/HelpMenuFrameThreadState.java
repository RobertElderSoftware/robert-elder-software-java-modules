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

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.stream.Collectors;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.ByteArrayOutputStream;

import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;



public class HelpMenuFrameThreadState extends UserInterfaceFrameThreadState {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;
	private boolean menuActive = false;
	private int selectedMenuIndex = 0;
	private List<String> menuOptions = new ArrayList<String>();
	private int currentMenuIndex = 0;

	private ClientBlockModelContext clientBlockModelContext;

	public HelpMenuFrameThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception {
		super(blockManagerThreadCollection, clientBlockModelContext);
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
		menuOptions.add("Close Current Frame");
		menuOptions.add("Open Help Menu");
		menuOptions.add("Quit Game");
	}

	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		if(ansiEscapeSequence instanceof AnsiEscapeSequenceUpArrowKey){
			this.currentMenuIndex = (this.currentMenuIndex <= 0) ? 0 : this.currentMenuIndex - 1;
			this.render();
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceDownArrowKey){
			int maxIndex = menuOptions.size() -1;
			this.currentMenuIndex = (this.currentMenuIndex >= maxIndex) ? maxIndex : this.currentMenuIndex + 1;
			this.render();
		}else{
			logger.info("HelpMenuFrameThreadState, discarding unknown ansi escape sequence of type: " + ansiEscapeSequence.getClass().getName());
		}
	}

	public void onKeyboardInput(byte [] characters) throws Exception {
		UserInteractionConfig ki = this.blockManagerThreadCollection.getUserInteractionConfig();
		for(byte b : characters){
			String actionString = new String(new byte [] {b}, "UTF-8");
			UserInterfaceActionType action = ki.getKeyboardActionFromString(actionString);

			if(action == null){
				logger.info("Ignoring " + b);
			}else{
				switch(action){
					case ACTION_HELP_MENU_TOGGLE:{
						this.menuActive = false;
						this.render();
						break;
					}default:{
						logger.info("Ignoring " + b);
					}
				}
			}
		}
	}

	public boolean getIsMenuActive(){
		return this.menuActive;
	}

	public void setIsMenuActive(boolean b){
		this.menuActive = b;
	}

	public void render() throws Exception{
		ConsoleWriterThreadState cwts = this.clientBlockModelContext.getConsoleWriterThreadState();

		int terminalWidth = this.frameDimensions.getTerminalWidth().intValue();
		int terminalHeight = this.frameDimensions.getTerminalHeight().intValue();
		int menuWidth = 30;
		int menuHeight = 15;
		int [][] characterWidths = new int[menuWidth][menuHeight];
		int [][][] colourCodes = new int[menuWidth][menuHeight][2];
		String [][] characters = new String[menuWidth][menuHeight];
		boolean [][] hasChange = new boolean [menuWidth][menuHeight];

		for(int i = 0; i < menuWidth; i++){
			for(int j = 0; j < menuHeight; j++){
				String character = menuActive ? " " : null;
				int [] colours = menuActive ? new int [] {UserInterfaceFrameThreadState.GREEN_BG_COLOR, UserInterfaceFrameThreadState.BLACK_FG_COLOR} : new int [] {};
				int characterWidth = menuActive ? 1 : 0;
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

		sendConsolePrintMessage(characterWidths, colourCodes, characters, hasChange, xOffset, yOffset, xSize, ySize, this.frameDimensions, ConsoleWriterThreadState.BUFFER_INDEX_MENU);


		List<LinePrintingInstructionAtOffset> instructions = new ArrayList<LinePrintingInstructionAtOffset>();

		Long leftPadding = Long.valueOf(xOffset) + 1L;
		Long rightPadding = this.getInnerFrameWidth() - (xOffset + menuWidth) + 1L;
		Long currentLine = 1L;

		for(int i = 0; i < menuOptions.size(); i++){
			String menuOption = menuOptions.get(i);
			ColouredTextFragmentList menuItemTextFragmentList = new ColouredTextFragmentList();
			int [] ansiColourCodes = (i == currentMenuIndex) ? new int[] {YELLOW_BG_COLOR, BLACK_FG_COLOR} : new int[] {RESET_BG_COLOR, GREEN_FG_COLOR};
			menuItemTextFragmentList.add(new ColouredTextFragment(menuOption, ansiColourCodes));

			List<LinePrintingInstruction> menuItemLineInstructions = this.getLinePrintingInstructions(menuItemTextFragmentList, leftPadding, rightPadding, false, false, Long.valueOf(menuWidth));
			instructions.addAll(this.wrapLinePrintingInstructionsAtOffset(menuItemLineInstructions, currentLine, 1L));
			currentLine += menuItemLineInstructions.size() + 1L;
		}

		for(LinePrintingInstructionAtOffset instruction : instructions){
			Long lineYOffset = instruction.getOffsetY() + yOffset;
			this.executeLinePrintingInstructionsAtYOffset(Arrays.asList(instruction.getLinePrintingInstruction()), lineYOffset, ConsoleWriterThreadState.BUFFER_INDEX_MENU);
		}

		if(!menuActive){
			//  De-activate everything in menu layer:
			cwts.putWorkItem(new ConsoleScreenAreaChangeStatesWorkItem(cwts, 0, 0, terminalWidth, terminalHeight, ConsoleWriterThreadState.BUFFER_INDEX_MENU, false), WorkItemPriority.PRIORITY_LOW);
			//  Allow refresh of everything that was below the menu:
			//cwts.putWorkItem(new ConsoleScreenAreaChangeStatesWorkItem(cwts, xOffset, yOffset, xOffset + menuWidth, yOffset + menuHeight, ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT, true), WorkItemPriority.PRIORITY_LOW);
			cwts.putWorkItem(new ConsoleScreenAreaChangeStatesWorkItem(cwts, 0, 0, terminalWidth, terminalHeight, ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT, true), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void onFrameDimensionsChanged() throws Exception{
		this.render();
	}

	public void onFrameFocusChanged() throws Exception{
		this.render();
	}

	public void reprintFrame() throws Exception {
		//this.drawBorders(true);
	}

	public UIWorkItem takeWorkItem() throws Exception {
		UIWorkItem workItem = this.workItemQueue.takeWorkItem();
		return workItem;
	}

	public void putWorkItem(UIWorkItem workItem, WorkItemPriority priority) throws Exception{
		this.workItemQueue.putWorkItem(workItem, priority);
	}

	public boolean doBackgroundProcessing() throws Exception{
		return false;
	}
}
