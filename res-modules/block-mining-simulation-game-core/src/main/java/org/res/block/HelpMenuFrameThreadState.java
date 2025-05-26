//  Copyright (c) 2025 Robert Elder Software Inc.
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

	private ClientBlockModelContext clientBlockModelContext;
	private Long previousRootSplitId = null;
	private HelpMenu helpMenu = null;

	public HelpMenuFrameThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception {
		super(blockManagerThreadCollection, clientBlockModelContext);
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;

		this.helpMenu = new HelpMenu(
			new HelpMenuLevel(
				Arrays.asList(
					new SubMenuHelpMenuOption(
						"Open Custom Frame",
						HelpMenuOptionType.DO_SUBMENU_OPEN_CUSTOM_FRAME, 
						new HelpMenuLevel(
							Arrays.asList(
								new OpenFrameClassHelpMenuOption("Open Help Menu", HelpMenuOptionType.OPEN_NEW_FRAME, HelpDetailsFrameThreadState.class),
								new OpenFrameClassHelpMenuOption("Open Map Area", HelpMenuOptionType.OPEN_NEW_FRAME, MapAreaInterfaceThreadState.class),
								new OpenFrameClassHelpMenuOption("Open Inventory", HelpMenuOptionType.OPEN_NEW_FRAME, InventoryInterfaceThreadState.class),
								new OpenFrameClassHelpMenuOption("Empty Frame", HelpMenuOptionType.OPEN_NEW_FRAME, EmptyFrameThreadState.class),

								new SimpleHelpMenuOption("Back", HelpMenuOptionType.BACK_UP_LEVEL)
							)
						)
					),
					new SimpleHelpMenuOption("Close Current Frame", HelpMenuOptionType.CLOSE_CURRENT_FRAME),
					new SimpleHelpMenuOption("Quit Game", HelpMenuOptionType.QUIT_GAME)
				)
			)
		);
	}

	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		if(ansiEscapeSequence instanceof AnsiEscapeSequenceUpArrowKey){
			this.helpMenu.moveSelectionUp();
			this.render();
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceDownArrowKey){
			this.helpMenu.moveSelectionDown();
			this.render();
		}else{
			logger.info("HelpMenuFrameThreadState, discarding unknown ansi escape sequence of type: " + ansiEscapeSequence.getClass().getName());
		}
	}


	public void removeSplitWithFrameId(Long splitId, Long frameId) throws Exception {
		ConsoleWriterThreadState cwts = this.clientBlockModelContext.getConsoleWriterThreadState();

		GetSplitInfoWorkItem getSplitInfoWorkItem = new GetSplitInfoWorkItem(cwts, splitId,false);
		WorkItemResult getSplitInfoWorkItemResult = cwts.putBlockingWorkItem(getSplitInfoWorkItem, WorkItemPriority.PRIORITY_LOW);
		SplitInfoWorkItemResult parentInfo = ((SplitInfoWorkItemResult)getSplitInfoWorkItemResult);
		if(frameId.equals(parentInfo.getFrameId())){
			RemoveChildSplitWorkItem r = new RemoveChildSplitWorkItem(cwts, splitId);
			cwts.putBlockingWorkItem(r, WorkItemPriority.PRIORITY_LOW);
		}

		// Continue searching through children:
		GetSplitChildrenInfoWorkItem getSplitChildrenInfoWorkItem = new GetSplitChildrenInfoWorkItem(cwts, splitId);
		WorkItemResult getSplitChildrenInfoWorkItemResult = cwts.putBlockingWorkItem(getSplitChildrenInfoWorkItem, WorkItemPriority.PRIORITY_LOW);
		GetSplitChildrenInfoWorkItemResult childInfos = ((GetSplitChildrenInfoWorkItemResult)getSplitChildrenInfoWorkItemResult);
		for(SplitInfoWorkItemResult info : childInfos.getSplitInfos()){
			removeSplitWithFrameId(info.getSplitId(), frameId);
		}
	}

	public void onCloseCurrentFrame() throws Exception {
		ConsoleWriterThreadState cwts = this.clientBlockModelContext.getConsoleWriterThreadState();

		//  Get id of currently focused frame
		GetFocusedFrameWorkItem getFocusedFrameWorkItem = new GetFocusedFrameWorkItem(cwts);
		WorkItemResult getFocusedFrameWorkItemResult = cwts.putBlockingWorkItem(getFocusedFrameWorkItem, WorkItemPriority.PRIORITY_LOW);
		Long focusedFrameId = ((GetFocusedFrameWorkItemResult)getFocusedFrameWorkItemResult).getFocusedFrameId();

		if(focusedFrameId == null){
			logger.info("Not removing frame, focusedFrameId was null");
		}else{
			//  Figure out the id of the root split
			GetSplitInfoWorkItem getRootSplitInfoWorkItem = new GetSplitInfoWorkItem(cwts, null, true);
			WorkItemResult getRootSplitInfoWorkItemResult = cwts.putBlockingWorkItem(getRootSplitInfoWorkItem, WorkItemPriority.PRIORITY_LOW);
			Long rootSplitId  = ((SplitInfoWorkItemResult)getRootSplitInfoWorkItemResult).getSplitId();
			//  Remove this split
			this.removeSplitWithFrameId(rootSplitId, focusedFrameId);

			//  Close the focused frame
			ConsoleWriterWorkItem w = new CloseFrameWorkItem(cwts, focusedFrameId);
			cwts.putBlockingWorkItem(w, WorkItemPriority.PRIORITY_LOW);
		}

		//  Update screen
		this.clientBlockModelContext.putWorkItem(new TellClientTerminalChangedWorkItem(this.clientBlockModelContext), WorkItemPriority.PRIORITY_LOW);
	}

	public void onOpenHelpDetails(Class<?> frameStateClass) throws Exception {
		ConsoleWriterThreadState cwts = this.clientBlockModelContext.getConsoleWriterThreadState();
		//  Open the required frame:
		OpenFrameWorkItem openFrameWorkItem = new OpenFrameWorkItem(cwts, frameStateClass);
		WorkItemResult openFrameWorkItemResult = cwts.putBlockingWorkItem(openFrameWorkItem, WorkItemPriority.PRIORITY_LOW);
		Long newFrameId = ((OpenFrameWorkItemResult)openFrameWorkItemResult).getFrameId();

		//  Add it to a leaf node
		CreateLeafNodeSplitWorkItem createLeafNodeSplitWorkItem = new CreateLeafNodeSplitWorkItem(cwts, newFrameId);
		WorkItemResult createLeafNodeSplitWorkItemResult = cwts.putBlockingWorkItem(createLeafNodeSplitWorkItem, WorkItemPriority.PRIORITY_LOW);
		Long newlyCreatedLeafNodeSplitId = ((CreateLeafNodeSplitWorkItemResult)createLeafNodeSplitWorkItemResult).getSplitId();

		//  Figure out the current root split id
		GetSplitInfoWorkItem getRootSplitInfoWorkItem = new GetSplitInfoWorkItem(cwts, null, true);
		WorkItemResult getRootSplitInfoWorkItemResult = cwts.putBlockingWorkItem(getRootSplitInfoWorkItem, WorkItemPriority.PRIORITY_LOW);
		this.previousRootSplitId = ((SplitInfoWorkItemResult)getRootSplitInfoWorkItemResult).getSplitId();

		if(this.previousRootSplitId == null){
			//  Root split was null, make new root the leaf node with newly opened frame:
			SetRootSplitIdWorkItem setRootSplitIdWorkItem = new SetRootSplitIdWorkItem(cwts, newlyCreatedLeafNodeSplitId);
			WorkItemResult setRootSplitIdWorkItemResult = cwts.putBlockingWorkItem(setRootSplitIdWorkItem, WorkItemPriority.PRIORITY_LOW);
			Long unused2 = ((SetRootSplitIdWorkItemResult)setRootSplitIdWorkItemResult).getSplitId();
		}else{
			//  Add the new leaf node with the new frame and old root to a list
			List<Long> newTopSplitPartIds = new ArrayList<Long>();
			newTopSplitPartIds.add(this.previousRootSplitId);
			newTopSplitPartIds.add(newlyCreatedLeafNodeSplitId);

			//  Create a new multi split that will become the new root
			CreateMultiSplitWorkItem createMultiSplitWorkItem = new CreateMultiSplitWorkItem(cwts, UserInterfaceSplitVertical.class);
			WorkItemResult createMultiSplitWorkItemResult = cwts.putBlockingWorkItem(createMultiSplitWorkItem, WorkItemPriority.PRIORITY_LOW);
			Long newRootSplitId = ((CreateMultiSplitWorkItemResult)createMultiSplitWorkItemResult).getSplitId();

			//  Add the children to the new root
			AddSplitPartsByIdsWorkItem addSplitPartsByIdsWorkItem = new AddSplitPartsByIdsWorkItem(cwts, newRootSplitId, newTopSplitPartIds);
			WorkItemResult addSplitPartsByIdsWorkItemResult = cwts.putBlockingWorkItem(addSplitPartsByIdsWorkItem, WorkItemPriority.PRIORITY_LOW);
			Long unused1 = ((AddSplitPartsByIdsWorkItemResult)addSplitPartsByIdsWorkItemResult).getSplitId();

			//  Set the new root split id
			SetRootSplitIdWorkItem setRootSplitIdWorkItem = new SetRootSplitIdWorkItem(cwts, newRootSplitId);
			WorkItemResult setRootSplitIdWorkItemResult = cwts.putBlockingWorkItem(setRootSplitIdWorkItem, WorkItemPriority.PRIORITY_LOW);
			Long unused2 = ((SetRootSplitIdWorkItemResult)setRootSplitIdWorkItemResult).getSplitId();
		}

		//  Focus on the newly opened frame:
		SetFocusedFrameWorkItem setFocusedFrameWorkItem = new SetFocusedFrameWorkItem(cwts, newFrameId);
		WorkItemResult setFocusedFrameWorkItemResult = cwts.putBlockingWorkItem(setFocusedFrameWorkItem, WorkItemPriority.PRIORITY_LOW);

		this.clientBlockModelContext.putWorkItem(new TellClientTerminalChangedWorkItem(this.clientBlockModelContext), WorkItemPriority.PRIORITY_LOW);
	}

	public void onEnterKeyPressed() throws Exception {
		HelpMenuOption option = this.helpMenu.getDisplayedHelpMenuOptions().get(this.helpMenu.getCurrentMenuYIndex());
		switch(option.getHelpMenuOptionType()){
			case HelpMenuOptionType.CLOSE_CURRENT_FRAME:{
				this.onCloseCurrentFrame();
				this.helpMenu.resetMenuState();
				this.menuActive = false;
				break;
			} case HelpMenuOptionType.OPEN_NEW_FRAME:{
				OpenFrameClassHelpMenuOption co = (OpenFrameClassHelpMenuOption)option;
				this.onOpenHelpDetails(co.getFrameStateClass());
				this.helpMenu.resetMenuState();
				this.menuActive = false;
				break;
			} case HelpMenuOptionType.DO_SUBMENU_OPEN_CUSTOM_FRAME:{
				this.helpMenu.descendIntoSubmenu();
				break;
			} case HelpMenuOptionType.BACK_UP_LEVEL:{
				this.helpMenu.ascendFromSubmenu();
				break;
			} case HelpMenuOptionType.QUIT_GAME:{
				logger.info("Menu option to quit was selected.  Exiting...");
				this.blockManagerThreadCollection.setIsProcessFinished(true, null); // Start shutting down the entire application.
				break;
			} default:{
				throw new Exception("Unknown help menu option: " + option.getHelpMenuOptionType().toString());
			} 
		}
		this.render();
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
					case ACTION_ENTER:{
						this.onEnterKeyPressed();
						break;
					}case ACTION_HELP_MENU_TOGGLE:{
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
				int [] colours = menuActive ? new int [] {UserInterfaceFrameThreadState.GREEN_BG_COLOR} : new int [] {};
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

		for(int i = 0; i < this.helpMenu.getDisplayedHelpMenuOptions().size(); i++){
			HelpMenuOption menuOption = this.helpMenu.getDisplayedHelpMenuOptions().get(i);
			ColouredTextFragmentList menuItemTextFragmentList = new ColouredTextFragmentList();
			int [] ansiColourCodes = (i == this.helpMenu.getCurrentMenuYIndex()) ? new int[] {YELLOW_BG_COLOR, BLACK_FG_COLOR} : new int[] {DEFAULT_TEXT_BG_COLOR, DEFAULT_TEXT_FG_COLOR};
			menuItemTextFragmentList.add(new ColouredTextFragment(menuOption.getTitle(), ansiColourCodes));

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
