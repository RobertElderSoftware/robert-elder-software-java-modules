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

public class HelpDetailsFrameThreadState extends UserInterfaceFrameThreadState {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;

	private ClientBlockModelContext clientBlockModelContext;
	private List<LinePrintingInstructionAtOffset> linePrintingInstructionsAtOffset = null;
	private Long scrollOffset = 0L;

	public HelpDetailsFrameThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception {
		super(blockManagerThreadCollection, clientBlockModelContext);
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
	}

	public String getKeyPresentation(String s){
		if(s.equals("\u001b")){
			return "ESC";
		}else if(s.equals("	")){
			return "TAB";
		}else if(s.equals("\r")){
			return "ENTER";
		}else{
			return "'" + s + "'";
		}
	}

	public List<LinePrintingInstructionAtOffset> getAllHelpMenuLines() throws Exception {
		int [] titleAnsiCodes = new int[] {UserInterfaceFrameThreadState.DEFAULT_TEXT_BG_COLOR, UserInterfaceFrameThreadState.BOLD_COLOR, UserInterfaceFrameThreadState.UNDERLINE_COLOR, UserInterfaceFrameThreadState.RED_FG_COLOR};
		int [] normalAnsiCodes = new int[] {UserInterfaceFrameThreadState.DEFAULT_TEXT_BG_COLOR, DEFAULT_TEXT_FG_COLOR};
		List<LinePrintingInstructionAtOffset> rtn = new ArrayList<LinePrintingInstructionAtOffset>();
		Long blockOffsetLeftPadding = 3L;
		Long descriptionOffsetLeftPadding = 9L;
		Long rightPadding = 3L;
		Long currentLine = 3L;

		ColouredTextFragmentList topTitlePart = new ColouredTextFragmentList();
		topTitlePart.add(new ColouredTextFragment("Block Mining Simulation Game Help Menu\n\n\nInput Keys", titleAnsiCodes));

		List<LinePrintingInstruction> introInstructions = this.getLinePrintingInstructions(topTitlePart, 1L, 1L, false, false, this.getInnerFrameWidth());
		rtn.addAll(this.wrapLinePrintingInstructionsAtOffset(introInstructions, currentLine, 1L));
		currentLine += introInstructions.size() + 1;

		Map<String, String> keyDescriptions = new HashMap<String, String>();
		keyDescriptions.put("'←'", "Navigate left (decrease x coordinate by 1) when map area is selected.");
		keyDescriptions.put("'↑'", "Scroll up in this help menu.  Also, used to navigate up (increase z coordinate by 1) when map area is selected.");
		keyDescriptions.put("'→'", "Navigate right (increase x coordinate by 1) when map area is selected.");
		keyDescriptions.put("'↓'", "Scroll down in this help menu.  Also, used to navigate down (decrease z coordinate by 1) when map area is selected.");
		keyDescriptions.put("TAB", "Switch focus to next frame.  Keyboard input is only sent to the currently active frame.  Therefore, you must use the tab key to make sure the correct frame is currently active.");
		keyDescriptions.put("ENTER", "Enter key to select an option in menu.");
		keyDescriptions.put("ESC", "Open or closes this help menu.");
		keyDescriptions.put("'w'", "Used to navigate up (increase z coordinate by 1) when map area is selected.");
		keyDescriptions.put("'a'", "Used to navigate left (decrease x coordinate by 1) when map area is selected.");
		keyDescriptions.put("'s'", "Used to navigate down (decrease z coordinate by 1) when map area is selected.");
		keyDescriptions.put("'d'", "Used to navigate right (increase x coordinate by 1) when map area is selected.");
		keyDescriptions.put("' '", "Used 'go up a level' (increase y coordinate by 1) when map area is selected.");
		keyDescriptions.put("'x'", "Used 'go down level' (decrease y coordinate by 1) when map area is selected.");

		keyDescriptions.put("'p'", "Used to place a block at current coordinate when map area is selected.");
		keyDescriptions.put("'m'", "Used to mine blocks near the current player position when map area is selected.");
		keyDescriptions.put("'c'", "Used to 'craft' new items.  Map area must be selected.");
		keyDescriptions.put("'q'", "Quit the game.");

		UserInteractionConfig ki = this.blockManagerThreadCollection.getUserInteractionConfig();
		Set<String> inputKeys = new LinkedHashSet<String>();
		inputKeys.add("←");
		inputKeys.add("↑");
		inputKeys.add("→");
		inputKeys.add("↓");
		inputKeys.addAll(ki.getAllKeyboardActions().keySet());
		for(String inputKey : inputKeys){
			String keyPresentation = this.getKeyPresentation(inputKey);
			List<LinePrintingInstruction> keyIns = this.getLinePrintingInstructions(keyPresentation, blockOffsetLeftPadding, rightPadding, true, false, this.getInnerFrameWidth());
			rtn.addAll(this.wrapLinePrintingInstructionsAtOffset(keyIns, currentLine, 1L));


			String keyDescription = keyDescriptions.containsKey(keyPresentation) ? keyDescriptions.get(keyPresentation) : "Key description not found.";
			ColouredTextFragmentList keyDescriptionPart = new ColouredTextFragmentList();
			keyDescriptionPart.add(new ColouredTextFragment("KEY DESCRIPTION:", titleAnsiCodes));
			keyDescriptionPart.add(new ColouredTextFragment(" " + keyDescription, normalAnsiCodes));
			List<LinePrintingInstruction> keyDescriptionInstructions = this.getLinePrintingInstructions(keyDescriptionPart, descriptionOffsetLeftPadding, rightPadding, true, false, this.getInnerFrameWidth());
			rtn.addAll(this.wrapLinePrintingInstructionsAtOffset(keyDescriptionInstructions, currentLine, 1L));
			currentLine += keyDescriptionInstructions.size() + 1;
		}

		ColouredTextFragmentList blockTypesTitlePart = new ColouredTextFragmentList();
		blockTypesTitlePart.add(new ColouredTextFragment("Block Types", titleAnsiCodes));

		List<LinePrintingInstruction> blockTypesTitleInstructions = this.getLinePrintingInstructions(blockTypesTitlePart, 1L, 1L, false, false, this.getInnerFrameWidth());
		rtn.addAll(this.wrapLinePrintingInstructionsAtOffset(blockTypesTitleInstructions, currentLine, 1L));
		currentLine += blockTypesTitleInstructions.size() + 1;

		List<LinePrintingInstruction> qB = this.getLinePrintingInstructions("'?'", blockOffsetLeftPadding, rightPadding, true, false, this.getInnerFrameWidth());
		rtn.addAll(this.wrapLinePrintingInstructionsAtOffset(qB, currentLine, 1L));

		ColouredTextFragmentList qBPart = new ColouredTextFragmentList();
		qBPart.add(new ColouredTextFragment("BLOCK DESCRIPTION:", titleAnsiCodes));
		qBPart.add(new ColouredTextFragment(" Question marks represent areas on the map that are not currently loaded into the game client's memory.", normalAnsiCodes));
		List<LinePrintingInstruction> qBInstructions = this.getLinePrintingInstructions(qBPart, descriptionOffsetLeftPadding, rightPadding, true, false, this.getInnerFrameWidth());
		rtn.addAll(this.wrapLinePrintingInstructionsAtOffset(qBInstructions, currentLine, 1L));
		currentLine += qBInstructions.size() + 1;

		List<LinePrintingInstruction> uB = this.getLinePrintingInstructions("'U'", blockOffsetLeftPadding, rightPadding, true, false, this.getInnerFrameWidth());
		rtn.addAll(this.wrapLinePrintingInstructionsAtOffset(uB, currentLine, 1L));

		ColouredTextFragmentList uBPart = new ColouredTextFragmentList();
		uBPart.add(new ColouredTextFragment("BLOCK DESCRIPTION:", titleAnsiCodes));
		uBPart.add(new ColouredTextFragment(" The 'U' character represents a block that has been loaded into the game's memory, but has not yet been initialized.  Uninitialized terrain occurs in areas of the map that have never been explored before.  Uninitialized blocks will automatically be replaced with generated terrain the first time it's loaded.", normalAnsiCodes));
		List<LinePrintingInstruction> uBInstructions = this.getLinePrintingInstructions(uBPart, descriptionOffsetLeftPadding, rightPadding, true, false, this.getInnerFrameWidth());
		rtn.addAll(this.wrapLinePrintingInstructionsAtOffset(uBInstructions, currentLine, 1L));
		currentLine += uBInstructions.size() + 1;

		for(BlockMatchDescription blockMatchDescription : blockManagerThreadCollection.getBlockSchema().getBlockMatchDescriptions()){
			String className = blockMatchDescription.getBlockInstanceClassName();
			Class<?> blockClass = Class.forName(className);
			if(blockMatchDescription instanceof ByteComparisonBlockMatchDescription){

				GraphicsMode mode = blockManagerThreadCollection.getGraphicsMode();
				String blockPresentation = BlockSkins.getPresentation(blockClass, mode.equals(GraphicsMode.ASCII));

				List<LinePrintingInstruction> blockPrint = this.getLinePrintingInstructions("'" + blockPresentation + "'", blockOffsetLeftPadding, rightPadding, true, false, this.getInnerFrameWidth());
				rtn.addAll(this.wrapLinePrintingInstructionsAtOffset(blockPrint, currentLine, 1L));

				ColouredTextFragmentList descriptionPart = new ColouredTextFragmentList();
				descriptionPart.add(new ColouredTextFragment("BLOCK DESCRIPTION:", titleAnsiCodes));
				descriptionPart.add(new ColouredTextFragment(" " + BlockSkins.getBlockDescription(blockClass), normalAnsiCodes));

				List<LinePrintingInstruction> instructions = this.getLinePrintingInstructions(descriptionPart, descriptionOffsetLeftPadding, rightPadding, true, false, this.getInnerFrameWidth());
				rtn.addAll(this.wrapLinePrintingInstructionsAtOffset(instructions, currentLine, 1L));
				currentLine += instructions.size();

				ColouredTextFragmentList classPart = new ColouredTextFragmentList();
				classPart.add(new ColouredTextFragment("CLASS NAME:", titleAnsiCodes));
				classPart.add(new ColouredTextFragment(" " + className, normalAnsiCodes));
				List<LinePrintingInstruction> classIns = this.getLinePrintingInstructions(classPart, descriptionOffsetLeftPadding, rightPadding, true, false, this.getInnerFrameWidth());
				rtn.addAll(this.wrapLinePrintingInstructionsAtOffset(classIns, currentLine, 1L));
				currentLine += classIns.size();

				byte [] bytePattern = blockManagerThreadCollection.getBlockSchema().getBinaryDataForByteComparisonBlockForClass(blockClass);
				ColouredTextFragmentList bytePatternPart = new ColouredTextFragmentList();
				bytePatternPart.add(new ColouredTextFragment("BYTE PATTERN:", titleAnsiCodes));
				bytePatternPart.add(new ColouredTextFragment(" '" + new String(bytePattern, "UTF-8") + "' (0x" + BlockModelContext.convertToHex(bytePattern) + ")", normalAnsiCodes));
				List<LinePrintingInstruction> bytePatternIns = this.getLinePrintingInstructions(bytePatternPart, descriptionOffsetLeftPadding, rightPadding, true, false, this.getInnerFrameWidth());
				rtn.addAll(this.wrapLinePrintingInstructionsAtOffset(bytePatternIns, currentLine, 1L));
				currentLine += bytePatternIns.size();

				String dedication = blockMatchDescription.getDedication();
				if(dedication != null){
					ColouredTextFragmentList dedicationPart = new ColouredTextFragmentList();
					dedicationPart.add(new ColouredTextFragment("DEDICATION:", titleAnsiCodes));
					dedicationPart.add(new ColouredTextFragment(" '" + dedication + "'", normalAnsiCodes));
					List<LinePrintingInstruction> dedicationIns = this.getLinePrintingInstructions(dedicationPart, descriptionOffsetLeftPadding, rightPadding, true, false, this.getInnerFrameWidth());
					rtn.addAll(this.wrapLinePrintingInstructionsAtOffset(dedicationIns, currentLine, 1L));
					currentLine += dedicationIns.size();
				}
				currentLine += 1;  //  Space between blocks.
			}
		}
		return rtn;
	}

	public List<LinePrintingInstructionAtOffset> getAllLinePrintingInstructions() throws Exception {
		if(this.linePrintingInstructionsAtOffset == null){
			this.linePrintingInstructionsAtOffset = this.getAllHelpMenuLines();
		}
		return this.linePrintingInstructionsAtOffset;
	}

	public Long getMaxLineOffset() throws Exception {
		Long maxLineOffset = 0L;
		for(LinePrintingInstructionAtOffset line : getAllLinePrintingInstructions()){
			if(line.getOffsetY() > maxLineOffset){
				maxLineOffset = line.getOffsetY();
			}
		}
		return maxLineOffset;
	}

	public void onTryScrollChange(Long proposedChange) throws Exception{
		Long resultAfterChange = this.scrollOffset + proposedChange;
		if(resultAfterChange < 0L){
			resultAfterChange = 0L;
		}

		Long limit = this.getMaxLineOffset() - this.getFrameHeight() + 3L;
		if(resultAfterChange > limit){
			resultAfterChange = limit;
		}

		if(!this.scrollOffset.equals(resultAfterChange)){
			this.scrollOffset = resultAfterChange;
			this.render();
		}
	}

	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		if(ansiEscapeSequence instanceof AnsiEscapeSequenceUpArrowKey){
			this.onTryScrollChange(-1L);
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceDownArrowKey){
			this.onTryScrollChange(1L);
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequencePageUpKey){
			this.onTryScrollChange(-(this.getFrameHeight() -2L));
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequencePageDownKey){
			this.onTryScrollChange((this.getFrameHeight() -2L));
		}else{
			logger.info("HelpMenuFrameThreadState, discarding unknown ansi escape sequence of type: " + ansiEscapeSequence.getClass().getName());
		}
	}

	public void onKeyboardInput(byte [] characters) throws Exception {
		logger.info("Help Menu, discarding keyboard input: " + new String(characters, "UTF-8"));
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public void render() throws Exception{
		this.reprintFrame();
		List<LinePrintingInstructionAtOffset> instructions = this.getAllLinePrintingInstructions();
		for(LinePrintingInstructionAtOffset instruction : instructions){
			Long lineOffset = instruction.getOffsetY() - this.scrollOffset;
			if((lineOffset >= 1L) && (lineOffset <= this.getFrameHeight() -2L)){
				this.executeLinePrintingInstructionsAtYOffset(Arrays.asList(instruction.getLinePrintingInstruction()), lineOffset);
			}
		}
	}

	public void onFrameDimensionsChanged() throws Exception{
		this.linePrintingInstructionsAtOffset = null; //  If dimension of frame change, must re-compute all lines.
		this.render();
	}

	public void onFrameFocusChanged() throws Exception{
		this.render();
	}

	public void reprintFrame() throws Exception {
		this.drawBorders(true);
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
