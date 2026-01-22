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

	private Long previousRootSplitId = null;
	private HelpMenu helpMenu = null;

	public HelpMenuFrameThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ConsoleWriterThreadState consoleWriterThreadState) throws Exception {
		super(blockManagerThreadCollection, consoleWriterThreadState, new int [] {ConsoleWriterThreadState.BUFFER_INDEX_MENU}, new ScreenLayerMergeType [] {ScreenLayerMergeType.PREFER_INPUT_TRANSPARENCY});
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.helpMenu = new HelpMenu(false, new HelpMenuLevel(Arrays.asList()));
	}

	protected void init(Object o){
	}

	public SubMenuHelpMenuOption makeMoveToSubmenu(String menuTitle, SplitInfoWorkItemResult parentSplitInfo, SplitInfoWorkItemResult info, int numSiblings) throws Exception{
		List<HelpMenuOption> options = new ArrayList<HelpMenuOption>();
		if(numSiblings > 0L){
			String backDirection = parentSplitInfo.getSplitClassType() == UserInterfaceSplitVertical.class ? "Left" : "Up";
			String forwardDirection = parentSplitInfo.getSplitClassType() == UserInterfaceSplitVertical.class ? "Right" : "Down";
			options.add(new RotateSplitHelpMenuOption("Rotate Split " + backDirection, HelpMenuOptionType.ROTATE_SPLIT, parentSplitInfo.getSplitId(), info.getSplitId(), false));
			options.add(new RotateSplitHelpMenuOption("Rotate Split " + forwardDirection, HelpMenuOptionType.ROTATE_SPLIT, parentSplitInfo.getSplitId(), info.getSplitId(), true));
		}
		options.add(new SimpleHelpMenuOption("Back", HelpMenuOptionType.BACK_UP_LEVEL));

		SubMenuHelpMenuOption rtnOption = new SubMenuHelpMenuOption(
			menuTitle,
			HelpMenuOptionType.DO_SUBMENU, 
			new HelpMenuLevel(options)
		);
		return rtnOption;
	}

	public List<HelpMenuOption> enumerateMoveFromOptions(String prefix, SplitInfoWorkItemResult currentSplitInfo, int numSiblings) throws Exception{
		ConsoleWriterThreadState cwts = getConsoleWriterThreadState();
		List<HelpMenuOption> rtn = new ArrayList<HelpMenuOption>();
		GetSplitChildrenInfoWorkItem getSplitChildrenInfoWorkItem = new GetSplitChildrenInfoWorkItem(cwts, currentSplitInfo.getSplitId());
		WorkItemResult getSplitChildrenInfoWorkItemResult = cwts.putBlockingWorkItem(getSplitChildrenInfoWorkItem, WorkItemPriority.PRIORITY_LOW);
		GetSplitChildrenInfoWorkItemResult childInfos = ((GetSplitChildrenInfoWorkItemResult)getSplitChildrenInfoWorkItemResult);
		for(SplitInfoWorkItemResult info : childInfos.getSplitInfos()){
			String menuTitle = prefix + info.getSplitClassType().getSimpleName() + "(" + info.getSplitId() + ")-> ";
			SubMenuHelpMenuOption option = null;
			if(info.getFrameId() == null){
				option = this.makeMoveToSubmenu(menuTitle, currentSplitInfo, info, numSiblings);
			}else{
				GetFrameInfoWorkItem getFrameInfoWorkItem = new GetFrameInfoWorkItem(cwts, info.getFrameId());
				FrameInfoWorkItemResult frameInfo = (FrameInfoWorkItemResult)cwts.putBlockingWorkItem(getFrameInfoWorkItem, WorkItemPriority.PRIORITY_LOW);
				String frameDesc = frameInfo.getFrameStateClassType().getSimpleName() + "(" + frameInfo.getFrameId() + ")";
				option = this.makeMoveToSubmenu(menuTitle + frameDesc, currentSplitInfo, info, numSiblings);
			}
			rtn.add(option);
			rtn.addAll(this.enumerateMoveFromOptions(menuTitle, info, childInfos.getSplitInfos().size()));
		}
		return rtn;
	}

	public List<HelpMenuOption> enumerateChangeSplitTypeOptions(String prefix, SplitInfoWorkItemResult parentSplitInfo, SplitInfoWorkItemResult childSplitInfo, int numSiblings) throws Exception{
		ConsoleWriterThreadState cwts = getConsoleWriterThreadState();
		List<HelpMenuOption> rtn = new ArrayList<HelpMenuOption>();
		GetSplitChildrenInfoWorkItem getSplitChildrenInfoWorkItem = new GetSplitChildrenInfoWorkItem(cwts, childSplitInfo.getSplitId());
		WorkItemResult getSplitChildrenInfoWorkItemResult = cwts.putBlockingWorkItem(getSplitChildrenInfoWorkItem, WorkItemPriority.PRIORITY_LOW);
		GetSplitChildrenInfoWorkItemResult childInfos = ((GetSplitChildrenInfoWorkItemResult)getSplitChildrenInfoWorkItemResult);
		List<String> childFrameNames = new ArrayList<String>();
		for(SplitInfoWorkItemResult info : childInfos.getSplitInfos()){
			String splitDescription = info.getSplitClassType().getSimpleName() + "(" + info.getSplitId() + ")";
			String menuTitle = prefix + splitDescription + "-> ";

			rtn.addAll(this.enumerateChangeSplitTypeOptions(menuTitle, childSplitInfo, info, childInfos.getSplitInfos().size()));

			if(info.getFrameId() == null){
				childFrameNames.add(splitDescription);
			}else{
				GetFrameInfoWorkItem getFrameInfoWorkItem = new GetFrameInfoWorkItem(cwts, info.getFrameId());
				FrameInfoWorkItemResult frameInfo = (FrameInfoWorkItemResult)cwts.putBlockingWorkItem(getFrameInfoWorkItem, WorkItemPriority.PRIORITY_LOW);
				String frameDesc = frameInfo.getFrameStateClassType().getSimpleName() + "(" + frameInfo.getFrameId() + ")";
				childFrameNames.add(frameDesc);
			}
		}

		if(
			childSplitInfo.getSplitClassType() == UserInterfaceSplitVertical.class ||
			childSplitInfo.getSplitClassType() == UserInterfaceSplitHorizontal.class
		){
			String newTypeName = childSplitInfo.getSplitClassType() == UserInterfaceSplitVertical.class ? "Horizontal" : "Vertical";
			Long parentSplitId = parentSplitInfo == null ? null : parentSplitInfo.getSplitId();
			rtn.add(new ChangeSplitTypeHelpMenuOption(prefix + "[" + String.join(", ", childFrameNames) + "]" + " Change Split To " + newTypeName, HelpMenuOptionType.CHANGE_SPLIT_TYPE, parentSplitId, childSplitInfo.getSplitId()));
		}

		return rtn;
	}

	public List<HelpMenuOption> getSplitMoveToOptionsList() throws Exception{
		//  Get root split:
		ConsoleWriterThreadState cwts = getConsoleWriterThreadState();
		GetSplitInfoWorkItem getRootSplitInfoWorkItem = new GetSplitInfoWorkItem(cwts, null, true);
		SplitInfoWorkItemResult getRootSplitInfoWorkItemResult = (SplitInfoWorkItemResult)cwts.putBlockingWorkItem(getRootSplitInfoWorkItem, WorkItemPriority.PRIORITY_LOW);
		Long rootSplitId = getRootSplitInfoWorkItemResult.getSplitId();

		List<HelpMenuOption> rtn = new ArrayList<HelpMenuOption>();
		if(rootSplitId != null){
			//  Find all child splits:
			String rootSplitTitle = getRootSplitInfoWorkItemResult.getSplitClassType().getSimpleName() + "(" + rootSplitId + ", root)-> ";
			rtn.addAll(this.enumerateMoveFromOptions(rootSplitTitle, getRootSplitInfoWorkItemResult, 1));
		}
		rtn.add(new SimpleHelpMenuOption("Back", HelpMenuOptionType.BACK_UP_LEVEL));
		return rtn;
	}

	public List<HelpMenuOption> getChangeSplitTypeOptionsList() throws Exception{
		//  Get root split:
		ConsoleWriterThreadState cwts = getConsoleWriterThreadState();
		GetSplitInfoWorkItem getRootSplitInfoWorkItem = new GetSplitInfoWorkItem(cwts, null, true);
		SplitInfoWorkItemResult getRootSplitInfoWorkItemResult = (SplitInfoWorkItemResult)cwts.putBlockingWorkItem(getRootSplitInfoWorkItem, WorkItemPriority.PRIORITY_LOW);
		Long rootSplitId = getRootSplitInfoWorkItemResult.getSplitId();

		List<HelpMenuOption> rtn = new ArrayList<HelpMenuOption>();
		if(rootSplitId != null){
			//  Find all child splits:
			String rootSplitTitle = getRootSplitInfoWorkItemResult.getSplitClassType().getSimpleName() + "(" + rootSplitId + ", root)-> ";
			rtn.addAll(this.enumerateChangeSplitTypeOptions(rootSplitTitle, null, getRootSplitInfoWorkItemResult, 1));
		}
		rtn.add(new SimpleHelpMenuOption("Back", HelpMenuOptionType.BACK_UP_LEVEL));
		return rtn;
	}

	public void rebuildHelpMenu(HelpMenu previousHelpMenu) throws Exception{
		List<HelpMenuOption> moveToOptionsList = this.getSplitMoveToOptionsList();
		List<HelpMenuOption> changeSplitTypeOptionsList = this.getChangeSplitTypeOptionsList();

		this.helpMenu = new HelpMenu(
			previousHelpMenu,
			new HelpMenuLevel(
				Arrays.asList(
					new SubMenuHelpMenuOption(
						"Open Custom Frame",
						HelpMenuOptionType.DO_SUBMENU, 
						new HelpMenuLevel(
							Arrays.asList(
								new OpenFrameClassHelpMenuOption("Open Help Menu", HelpMenuOptionType.OPEN_NEW_FRAME, HelpDetailsFrameThreadState.class),
								new OpenFrameClassHelpMenuOption("Open Open World Connection Menu", HelpMenuOptionType.OPEN_NEW_FRAME, OpenWorldConnectionInterfaceThreadState.class),
								new OpenFrameClassHelpMenuOption("Open Map Area", HelpMenuOptionType.OPEN_NEW_FRAME, MapAreaInterfaceThreadState.class),
								new OpenFrameClassHelpMenuOption("Open Crafting Menu", HelpMenuOptionType.OPEN_NEW_FRAME, CraftingInterfaceThreadState.class),
								new OpenFrameClassHelpMenuOption("Open Debug List", HelpMenuOptionType.OPEN_NEW_FRAME, DebugListInterfaceThreadState.class),
								new OpenFrameClassHelpMenuOption("Open Debug Input", HelpMenuOptionType.OPEN_NEW_FRAME, DebugInputInterfaceThreadState.class),

								new OpenFrameClassHelpMenuOption("Open Inventory", HelpMenuOptionType.OPEN_NEW_FRAME, InventoryInterfaceThreadState.class),
								new OpenFrameClassHelpMenuOption("Empty Frame", HelpMenuOptionType.OPEN_NEW_FRAME, EmptyFrameThreadState.class),

								new SimpleHelpMenuOption("Back", HelpMenuOptionType.BACK_UP_LEVEL)
							)
						)
					),
					new SubMenuHelpMenuOption(
						"Move Split",
						HelpMenuOptionType.DO_SUBMENU, 
						new HelpMenuLevel(moveToOptionsList)
					),
					new SubMenuHelpMenuOption(
						"Change Split Type",
						HelpMenuOptionType.DO_SUBMENU, 
						new HelpMenuLevel(changeSplitTypeOptionsList)
					),
					new SubMenuHelpMenuOption(
						"Resize Current Frame",
						HelpMenuOptionType.DO_SUBMENU, 
						new HelpMenuLevel(Arrays.asList(
							new SimpleHelpMenuOption("+++Increase Width+++", HelpMenuOptionType.RESIZE_FRAME_X_PLUS),
							new SimpleHelpMenuOption("---Decrease Width---", HelpMenuOptionType.RESIZE_FRAME_X_MINUS),
							new SimpleHelpMenuOption("+++Increase Height+++", HelpMenuOptionType.RESIZE_FRAME_Y_PLUS),
							new SimpleHelpMenuOption("---Decrease Height---", HelpMenuOptionType.RESIZE_FRAME_Y_MINUS),
							new SimpleHelpMenuOption("Back", HelpMenuOptionType.BACK_UP_LEVEL)
						))
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
		this.onFinalizeFrame();
	}

	public void onResizeFrame(Long deltaXColumns, Long deltaYColumns) throws Exception {
		ConsoleWriterThreadState cwts = getConsoleWriterThreadState();

		//  Get id of currently focused frame
		GetFocusedFrameWorkItem getFocusedFrameWorkItem = new GetFocusedFrameWorkItem(cwts);
		WorkItemResult getFocusedFrameWorkItemResult = cwts.putBlockingWorkItem(getFocusedFrameWorkItem, WorkItemPriority.PRIORITY_LOW);
		Long focusedFrameId = ((GetFocusedFrameWorkItemResult)getFocusedFrameWorkItemResult).getFocusedFrameId();
		if(focusedFrameId == null){
			logger.info("Cannot resize when no focused frame.");
		}else{
			ResizeFrameWorkItem resizeFrameWorkItem = new ResizeFrameWorkItem(cwts, focusedFrameId, deltaXColumns, deltaYColumns);
			cwts.putBlockingWorkItem(resizeFrameWorkItem, WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void onChangeSplitType(ChangeSplitTypeHelpMenuOption o) throws Exception {
		ConsoleWriterThreadState cwts = getConsoleWriterThreadState();
		ChangeSplitTypeWorkItem changeSplitTypeWorkItem = new ChangeSplitTypeWorkItem(cwts, o.getParentSplitId(), o.getChildSplitIdToChange());
		
		cwts.putBlockingWorkItem(changeSplitTypeWorkItem, WorkItemPriority.PRIORITY_LOW);

		//  Update screen
		cwts.putWorkItem(new TellClientTerminalChangedWorkItem(cwts), WorkItemPriority.PRIORITY_LOW);
	}

	public void onRotateSplit(RotateSplitHelpMenuOption o) throws Exception {
		ConsoleWriterThreadState cwts = getConsoleWriterThreadState();
		logger.info("onRotateSplit Rotated o.getIsForward()=" + o.getIsForward());
		RotateSplitWorkItem rotateSplitWorkItem = new RotateSplitWorkItem(cwts, o.getParentSplitId(), o.getChildSplitIdToRotate(), o.getIsForward());
		
		cwts.putBlockingWorkItem(rotateSplitWorkItem, WorkItemPriority.PRIORITY_LOW);

		//  Update screen
		cwts.putWorkItem(new TellClientTerminalChangedWorkItem(cwts), WorkItemPriority.PRIORITY_LOW);
	}




	public void onOpenFrame(Class<?> frameStateClass) throws Exception {

		//  TODO:  This needs to be generalized once there are multiple clients:
		ClientBlockModelContext client = this.blockManagerThreadCollection.getClientBlockModelContexts().get(0);

		ConsoleWriterThreadState cwts = getConsoleWriterThreadState();
		//  Open the required frame:
		OpenFrameWorkItem openFrameWorkItem = new OpenFrameWorkItem(cwts, frameStateClass, client);
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

		cwts.putWorkItem(new TellClientTerminalChangedWorkItem(cwts), WorkItemPriority.PRIORITY_LOW);
	}

	public void onEnterKeyPressed() throws Exception {
		//  Only explicitly call render from some menu options.  Others will trigger a frame change anyway.
		//  Currently, there are bugs in handling missed redraw events when frame sizes are changing.
		HelpMenuOption option = this.helpMenu.getDisplayedHelpMenuOptions().get(this.helpMenu.getCurrentMenuYIndex());
		switch(option.getHelpMenuOptionType()){
			case HelpMenuOptionType.CLOSE_CURRENT_FRAME:{
				this.onCloseCurrentFrame();
				this.helpMenu.resetMenuState();
				this.helpMenu.setActiveState(false);
				break;
			} case HelpMenuOptionType.OPEN_NEW_FRAME:{
				OpenFrameClassHelpMenuOption co = (OpenFrameClassHelpMenuOption)option;
				this.onOpenFrame(co.getFrameStateClass());
				this.helpMenu.resetMenuState();
				this.helpMenu.setActiveState(false);
				break;
			} case HelpMenuOptionType.DO_SUBMENU:{
				this.helpMenu.descendIntoSubmenu();
				this.render();
				this.onFinalizeFrame();
				break;
			} case HelpMenuOptionType.BACK_UP_LEVEL:{
				this.helpMenu.ascendFromSubmenu();
				this.render();
				this.onFinalizeFrame();
				break;
			} case HelpMenuOptionType.ROTATE_SPLIT:{
				this.onRotateSplit((RotateSplitHelpMenuOption)option);
				this.helpMenu.setActiveState(false);
				break;
			} case HelpMenuOptionType.RESIZE_FRAME_Y_PLUS:{
				this.onResizeFrame(0L, 1L);
				break;
			} case HelpMenuOptionType.RESIZE_FRAME_Y_MINUS:{
				this.onResizeFrame(0L, -1L);
				break;
			} case HelpMenuOptionType.RESIZE_FRAME_X_PLUS:{
				this.onResizeFrame(1L, 0L);
				break;
			} case HelpMenuOptionType.RESIZE_FRAME_X_MINUS:{
				this.onResizeFrame(-1L, 0L);
				break;
			} case HelpMenuOptionType.CHANGE_SPLIT_TYPE:{
				this.onChangeSplitType((ChangeSplitTypeHelpMenuOption)option);
				this.helpMenu.setActiveState(false);
				break;
			} case HelpMenuOptionType.QUIT_GAME:{
				logger.info("Menu option to quit was selected.  Exiting...");
				this.blockManagerThreadCollection.setIsProcessFinished(true, null); // Start shutting down the entire application.
				return;
			} default:{
				throw new Exception("Unknown help menu option: " + option.getHelpMenuOptionType().toString());
			} 
		}
	}

	public void onKeyboardInput(String actionString) throws Exception {
		UserInteractionConfig ki = this.blockManagerThreadCollection.getUserInteractionConfig();
		UserInterfaceActionType action = ki.getKeyboardActionFromString(actionString);

		if(action == null){
			logger.info("Ignoring " + actionString);
		}else{
			switch(action){
				case ACTION_ENTER:{
					this.onEnterKeyPressed();
					break;
				}case ACTION_HELP_MENU_TOGGLE:{
					this.helpMenu.setActiveState(false);
					this.render();
					this.onFinalizeFrame();
					break;
				}default:{
					logger.info("Ignoring " + actionString);
				}
			}
		}
	}

	public boolean getIsMenuActive(){
		return this.helpMenu.getActiveState();
	}

	public void setIsMenuActive(boolean b){
		this.helpMenu.setActiveState(b);
	}

	public void render() throws Exception{
		ConsoleWriterThreadState cwts = getConsoleWriterThreadState();

		int terminalWidth = this.getFrameDimensions().getTerminalWidth().intValue();
		int terminalHeight = this.getFrameDimensions().getTerminalHeight().intValue();

		List<LinePrintingInstructionAtOffset> instructions = new ArrayList<LinePrintingInstructionAtOffset>();

		int menuWidth = 45;
		int xOffset = (terminalWidth / 2) - (menuWidth / 2);
		Long leftPadding = Long.valueOf(xOffset) + 1L;
		Long rightPadding = Long.valueOf(terminalWidth) - (xOffset + menuWidth) + 1L;
		Long currentLine = 1L;

		for(int i = 0; i < this.helpMenu.getDisplayedHelpMenuOptions().size(); i++){
			HelpMenuOption menuOption = this.helpMenu.getDisplayedHelpMenuOptions().get(i);
			ColouredTextFragmentList menuItemTextFragmentList = new ColouredTextFragmentList();
			int [] ansiColourCodes = (i == this.helpMenu.getCurrentMenuYIndex()) ? UserInterfaceFrameThreadState.getActiveHelpMenuItemColors() : UserInterfaceFrameThreadState.getDefaultTextColors();
			menuItemTextFragmentList.add(new ColouredTextFragment(menuOption.getTitle(), ansiColourCodes));

			List<LinePrintingInstruction> menuItemLineInstructions = this.getLinePrintingInstructions(menuItemTextFragmentList, leftPadding, rightPadding, false, false, Long.valueOf(terminalWidth));
			instructions.addAll(this.wrapLinePrintingInstructionsAtOffset(menuItemLineInstructions, currentLine, 1L));
			currentLine += menuItemLineInstructions.size() + 1L;
		}

		int menuHeight = instructions.size() + this.helpMenu.getDisplayedHelpMenuOptions().size() + 1;

		ScreenLayer changes = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, terminalWidth, terminalHeight));
		changes.setAllChangedFlagStates(false);

		int yOffset = (terminalHeight / 2) - (menuHeight / 2);
		for(int i = 0; i < terminalWidth; i++){
			for(int j = 0; j < terminalHeight; j++){
				boolean isInMenuBox = (i >= xOffset && i < (xOffset + menuWidth)) && (j >= yOffset && j < (yOffset + menuHeight));
				if(this.helpMenu.getActiveState() && isInMenuBox){
					int [] colours = UserInterfaceFrameThreadState.getHelpMenuBackgroundColours();
					String character = " ";
					int characterWidth = getConsoleWriterThreadState().measureTextLengthOnTerminal(character).getDeltaX().intValue();;
					changes.setMultiColumnCharacter(i, j, character, characterWidth, colours, true, true);
				}else{
					changes.setToEmpty(i, j, true, true);
				}
			}
		}

		ScreenRegion region = new ScreenRegion(ScreenRegion.makeScreenRegionCA(0, 0, terminalWidth, terminalHeight));
		changes.addChangedRegion(region);
		this.bufferedScreenLayers[ConsoleWriterThreadState.BUFFER_INDEX_MENU].mergeDown(changes, false, ScreenLayerMergeType.PREFER_INPUT_TRANSPARENCY);

		for(LinePrintingInstructionAtOffset instruction : instructions){
			Long lineYOffset = instruction.getOffsetY() + yOffset;
			this.executeLinePrintingInstructionsAtYOffset(Arrays.asList(instruction.getLinePrintingInstruction()), lineYOffset, this.bufferedScreenLayers[ConsoleWriterThreadState.BUFFER_INDEX_MENU]);
		}

		this.setScreenLayerState(ConsoleWriterThreadState.BUFFER_INDEX_MENU, this.helpMenu.getActiveState());
	}

	public void onRenderFrame(boolean hasThisFrameDimensionsChanged, boolean hasOtherFrameDimensionsChanged) throws Exception{
		this.rebuildHelpMenu(this.helpMenu);
		this.render();
	}

	public void reprintFrame() throws Exception {
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

	public void onUIEventNotification(Object o, UINotificationType notificationType) throws Exception{
		switch(notificationType){
			default:{
				throw new Exception("Unknown event notification type: " + notificationType);
			}
		}
	}
}
