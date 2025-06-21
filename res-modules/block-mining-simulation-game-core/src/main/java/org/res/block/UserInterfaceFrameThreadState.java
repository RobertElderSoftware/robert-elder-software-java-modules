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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.ArrayList;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicLong;

public abstract class UserInterfaceFrameThreadState extends WorkItemQueueOwner<UIWorkItem>{

	private static final AtomicLong seq = new AtomicLong(0);
	public final long frameId;
	protected FrameChangeWorkItemParams currentFrameChangeWorkItemParams;
	protected FrameChangeWorkItemParams previousSuccessfullyPrintedFrameChangeWorkItemParams;
	protected int [] usedScreenLayers;
	protected boolean completedInitialOpen = false; //  Used to speed up feedback on initial open.

	private final AtomicLong frameDimensionsChangeSeq = new AtomicLong(0);

	public abstract void putWorkItem(UIWorkItem workItem, WorkItemPriority priority) throws Exception;
	public abstract void render() throws Exception;
	public abstract void onRenderFrame(boolean requiresRefresh) throws Exception;
	public abstract void onKeyboardInput(byte [] characters) throws Exception;
	public abstract void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception;

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;
	private boolean [] pendingScreenLayerActiveStates = new boolean [ConsoleWriterThreadState.numScreenLayers];

	private Long mapAreaCellWidth = null;
	private Long frameCharacterWidth = null;
	public static int RESET_BG_COLOR = 0;
	public static int BOLD_COLOR = 1;
	public static int UNDERLINE_COLOR = 4;

	public static int BLACK_FG_COLOR = 30;
	public static int RED_FG_COLOR = 31;
	public static int GREEN_FG_COLOR = 32;
	public static int YELLOW_FG_COLOR = 33;
	public static int BLUE_FG_COLOR = 34;
	public static int MAGENTA_FG_COLOR = 35;
	public static int CYAN_FG_COLOR = 36;
	public static int WHITE_FG_COLOR = 37;

	public static int BLACK_BG_COLOR = 40;
	public static int RED_BG_COLOR = 41;
	public static int GREEN_BG_COLOR = 42;
	public static int YELLOW_BG_COLOR = 43;
	public static int BLUE_BG_COLOR = 44;
	public static int MAGENTA_BG_COLOR = 45;
	public static int CYAN_BG_COLOR = 46;
	public static int WHITE_BG_COLOR = 47;

	public static int GRAY_BG_COLOR = 100;
	public static int PLAYER_BG_COLOR = GREEN_BG_COLOR;

	public static int DEFAULT_BG_COLOR = BLACK_BG_COLOR;
	public static int FRAME_BG_COLOR = DEFAULT_BG_COLOR;
	public static int MAP_CELL_BG_COLOR1 = DEFAULT_BG_COLOR;
	public static int MAP_CELL_BG_COLOR2 = DEFAULT_BG_COLOR;
	public static int FRAME_CLEAR_BG_COLOR = DEFAULT_BG_COLOR;
	public static int DEFAULT_TEXT_BG_COLOR = DEFAULT_BG_COLOR;

	public static int DEFAULT_TEXT_FG_COLOR = WHITE_FG_COLOR;

	public ScreenLayer[] previousBufferedScreenLayers = new ScreenLayer [ConsoleWriterThreadState.numScreenLayers];
	public ScreenLayer[] bufferedScreenLayers = new ScreenLayer [ConsoleWriterThreadState.numScreenLayers];
	public ScreenMask[] bufferedScreenMasks = new ScreenMask [ConsoleWriterThreadState.numScreenLayers];
	public List<Set<ScreenRegion>> changedScreenRegions = new ArrayList<Set<ScreenRegion>>();

	protected static List<String[]> splitStringByDelimiterPairs(String str, String delimiterRegex){
		List<String[]> rtn = new ArrayList<String[]>();

		Pattern stringPattern = Pattern.compile(delimiterRegex);
		Matcher m = stringPattern.matcher(str);

		int offset = 0;
		while(m.find(offset)){
			String token = str.substring(offset, m.start());
			String delimiter = str.substring(m.start(), m.end());
			offset = m.end();
			rtn.add(new String [] {token, delimiter});
		}
		String lastToken = str.substring(offset, str.length());
		if(!lastToken.equals("")){
			rtn.add(new String [] {lastToken, null});
		}
		return rtn;
	}

	protected static List<String> splitStringIntoCharactersUnicodeAware(String str){
		List<String> utf16CodePoints = str.codePoints().mapToObj(Character::toString).collect(Collectors.toList());
		List<String> rtn = new ArrayList<String>();
		for(String s : utf16CodePoints){
			//  If any of the characters are 'variation selectors', don't split
			//  them up and keep them associated with the previous character:
			if(s.codePointAt(0) >= 0xFE00 && s.codePointAt(0) <= 0xFE0F && rtn.size() > 0){
				int previousCharIndex = rtn.size() -1;
				String previousChar = rtn.get(previousCharIndex);
				rtn.set(previousCharIndex, previousChar + s);
			}else{
				rtn.add(s);
			}
		}
		return rtn;
	}

	private List<MeasuredTextFragment> getMeasuredTextFragments(ColouredTextFragmentList fragmentList, Long maxLineWidth) throws Exception{
		List<MeasuredTextFragment> textFragments = new ArrayList<MeasuredTextFragment>();
		for(ColouredTextFragment cf : fragmentList.getColouredTextFragments()){
			String newlineCharacter = "\n";
			String spaceCharacter = " ";
			String[] lines = cf.getText().split(newlineCharacter);
			for(int i = 0; i < lines.length; i++){
				List<String []> delimitedWords = UserInterfaceFrameThreadState.splitStringByDelimiterPairs(lines[i], "\\s+");
				for(int j = 0; j < delimitedWords.size(); j++){
					String word = delimitedWords.get(j)[0];
					String spaceDelimiter = delimitedWords.get(j)[1];
					List<String> characters = UserInterfaceFrameThreadState.splitStringIntoCharactersUnicodeAware(word);
					List<String> charactersSoFar = new ArrayList<String>();
					Long wordLength = 0L;
					Long wordHeight = 0L;
					List<String> wordParts = new ArrayList<String>();
					for(String c : characters){
						TextWidthMeasurementWorkItemResult m = this.clientBlockModelContext.measureTextLengthOnTerminal(c);
						wordLength += m.getDeltaX();
						wordHeight += m.getDeltaY();
						charactersSoFar.add(c);

						if(wordLength >= maxLineWidth){
							textFragments.add(new MeasuredTextFragment(new ColouredTextFragment(String.join("", charactersSoFar), cf.getAnsiColourCodes()), new TextWidthMeasurementWorkItemResult(wordLength, wordHeight)));
							charactersSoFar.clear();
							wordLength = 0L;
							wordHeight = 0L;
						}
					}
					if(charactersSoFar.size() > 0){
						textFragments.add(new MeasuredTextFragment(new ColouredTextFragment(String.join("", charactersSoFar), cf.getAnsiColourCodes()), new TextWidthMeasurementWorkItemResult(wordLength, wordHeight)));
					}
					if(j != delimitedWords.size() -1 || fragmentList.getPreserveWhitespace()){
						//  Add back any space characters between words
						//  as long as they're not at the end of lines.
						if(spaceDelimiter != null){
							textFragments.add(new MeasuredTextFragment(new ColouredTextFragment(spaceDelimiter, cf.getAnsiColourCodes()), this.clientBlockModelContext.measureTextLengthOnTerminal(spaceCharacter)));
						}
					}
				}
				if(i != lines.length -1){
					textFragments.add(new MeasuredTextFragment(new ColouredTextFragment(newlineCharacter, cf.getAnsiColourCodes()), this.clientBlockModelContext.measureTextLengthOnTerminal(newlineCharacter)));
				}
			}
		}

		return textFragments;
	}

	public Long getFrameDimensionsChangeId(){
		return this.frameDimensionsChangeSeq.get();
	}

	public Long getAndIncrementFrameDimensionsChangeId(){
		return this.frameDimensionsChangeSeq.getAndIncrement();
	}

	protected List<LinePrintingInstruction> getLinePrintingInstructions(String text, Long paddingLeft, Long paddingRight, boolean leftAlign, boolean rightAlign, Long maxLineLength) throws Exception{
		ColouredTextFragmentList tfl = new ColouredTextFragmentList(new ColouredTextFragment(text, new int[] {DEFAULT_TEXT_BG_COLOR, DEFAULT_TEXT_FG_COLOR}));
		return getLinePrintingInstructions(tfl, paddingLeft, paddingRight, leftAlign, rightAlign, maxLineLength);
	}

	protected List<LinePrintingInstruction> getLinePrintingInstructions(ColouredTextFragmentList tfl, Long paddingLeft, Long paddingRight, boolean leftAlign, boolean rightAlign, Long maxLineLength) throws Exception{
		List<LinePrintingInstruction> instructions = new ArrayList<LinePrintingInstruction>();
		List<MeasuredTextFragment> textFragments = this.getMeasuredTextFragments(tfl, maxLineLength - paddingLeft - paddingRight);
		List<ColouredTextFragment> currentLineFragments = new ArrayList<ColouredTextFragment>();
		Long lineLengthSoFar = 0L;
		boolean flushBuffer = false;
		int i = 0;
		while(currentLineFragments.size() > 0 || i < textFragments.size()){
			MeasuredTextFragment textFragment = i < textFragments.size() ? textFragments.get(i) : null;

			if(flushBuffer){
				Long centeredInExtraSpaceOffset = (this.getFrameWidth() / 2L) - (lineLengthSoFar / 2L);
				Long textOffset = leftAlign ? paddingLeft : centeredInExtraSpaceOffset;
				instructions.add(new LinePrintingInstruction(textOffset, new ColouredTextFragmentList(new ArrayList<ColouredTextFragment>(currentLineFragments))));
				currentLineFragments = new ArrayList<ColouredTextFragment>();
				lineLengthSoFar = 0L;
				flushBuffer = false;
			}else if(textFragment.getTextDisplacement().getDeltaY() > 0L){ // A newline
				flushBuffer = true;
				i++;
			}else if((textFragment.getTextDisplacement().getDeltaX() + lineLengthSoFar + paddingLeft + paddingRight) >= this.getInnerFrameWidth()){ // Line too long
				if(currentLineFragments.size() == 0){ // Current line is empty, but we're already overflowing with one word?  Just add the word and let it overflow:
					lineLengthSoFar += textFragment.getTextDisplacement().getDeltaX();
					currentLineFragments.add(textFragment.getColouredTextFragment());
					i++;
				}
				flushBuffer = true;
			}else if(i == textFragments.size() -1){ //  End of text
				lineLengthSoFar += textFragment.getTextDisplacement().getDeltaX();
				currentLineFragments.add(textFragment.getColouredTextFragment());
				flushBuffer = true;
				i++;
			}else{
				boolean isSpace = textFragment.getColouredTextFragment().getText().matches("\\s+");
				//  Do not add leading spaces to the start of a line:
				if(!isSpace || lineLengthSoFar > 0L || tfl.getPreserveWhitespace()){
					lineLengthSoFar += textFragment.getTextDisplacement().getDeltaX();
					currentLineFragments.add(textFragment.getColouredTextFragment());
				}
				i++;
			}
		}
		return instructions;
	}


	protected void printTextAtScreenXY(ColouredTextFragment colouredTextFragment, Long drawOffsetX, Long drawOffsetY, boolean xDirection, int bufferIndex) throws Exception{
		this.printTextAtScreenXY(new ColouredTextFragmentList(Arrays.asList(colouredTextFragment)), drawOffsetX, drawOffsetY, this.getFrameDimensions(), xDirection, bufferIndex);
	}

	protected void printTextAtScreenXY(ColouredTextFragment colouredTextFragment, Long drawOffsetX, Long drawOffsetY, boolean xDirection) throws Exception{
		this.printTextAtScreenXY(new ColouredTextFragmentList(Arrays.asList(colouredTextFragment)), drawOffsetX, drawOffsetY, this.getFrameDimensions(), xDirection, ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT);
	}

	protected void printTextAtScreenXY(ColouredTextFragmentList colouredTextFragmentList, Long drawOffsetX, Long drawOffsetY, boolean xDirection, int bufferIndex) throws Exception{
		this.printTextAtScreenXY(colouredTextFragmentList, drawOffsetX, drawOffsetY, this.getFrameDimensions(), xDirection, bufferIndex);
	}

	protected void printTextAtScreenXY(ColouredTextFragmentList colouredTextFragmentList, Long drawOffsetX, Long drawOffsetY, boolean xDirection) throws Exception{
		this.printTextAtScreenXY(colouredTextFragmentList, drawOffsetX, drawOffsetY, this.getFrameDimensions(), xDirection, ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT);
	}

	protected void printTextAtScreenXY(ColouredTextFragmentList colouredTextFragmentList, Long drawOffsetX, Long drawOffsetY, FrameDimensions fd, boolean xDirection, int bufferIndex) throws Exception{
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

		ScreenLayer changes = new ScreenLayer(xDimSize, yDimSize);

		ScreenMask mask = new ScreenMask();
		mask.initialize(xDimSize, yDimSize, false);

		int currentOffset = 0;
		for(int i = 0; i < charactersToPrint.size(); i++){
			String s = charactersToPrint.get(i);
			int chrWidth = this.clientBlockModelContext.measureTextLengthOnTerminal(s).getDeltaX().intValue();
			int xIndex = xDirection ? currentOffset : 0;
			int yIndex = xDirection ? 0 : currentOffset;
			changes.colourCodes[xIndex][yIndex] = newColourCodes[i];
			changes.characters[xIndex][yIndex] = s;
			changes.characterWidths[xIndex][yIndex] = chrWidth;
			mask.flags[xIndex][yIndex] = true;
			if(xDirection){
				currentOffset++;
				//  For multi-column characters in 'x' direction, reset any of the 'covered'
				//  columns take up by the multi-column character:
				for(int k = 1; k < chrWidth; k++){
					changes.colourCodes[currentOffset][yIndex] = new int [] {};
					changes.characters[currentOffset][yIndex] = null;
					changes.characterWidths[currentOffset][yIndex] = 0;
					mask.flags[currentOffset][yIndex] = true;
					currentOffset++;
				}
			}else{
				//  Always advance by 1 if printing in Y direction.
				currentOffset += 1;
			}
		}

		int xOffset = drawOffsetX.intValue();
		int yOffset = drawOffsetY.intValue();
		int xSize = xDimSize;
		int ySize = yDimSize;

		ScreenRegion region = new ScreenRegion(xOffset, yOffset, xOffset + xDimSize, yOffset + yDimSize);
		this.writeToLocalFrameBuffer(changes, mask, region, fd, bufferIndex);
	}

	public boolean sendConsolePrintMessage(List<ScreenLayerPrintParameters> params, FrameDimensions fd) throws Exception{
		WorkItemResult workItemResult = this.clientBlockModelContext.getConsoleWriterThreadState().putBlockingWorkItem(new ConsoleWriteWorkItem(this.clientBlockModelContext.getConsoleWriterThreadState(), params, fd, this.currentFrameChangeWorkItemParams), WorkItemPriority.PRIORITY_LOW);

		if(workItemResult instanceof FrameChangeWorkItemParams){
			//  This scenario happens when the write was discarded and 
			//  the returned new params should catch us up with the newest frame/terminal dimensions
			this.onNewFrameChangeWorkItemParams((FrameChangeWorkItemParams)workItemResult);
			this.initializeFrames();
			return false;
		}else{ //  Successfully printed
			for(int i = 0; i < this.usedScreenLayers.length; i++){
				int l = usedScreenLayers[i];
				this.previousBufferedScreenLayers[l] = new ScreenLayer(this.bufferedScreenLayers[l]);
				this.bufferedScreenMasks[l].initialize(this.getFrameDimensions().getFrameWidth().intValue(), this.getFrameDimensions().getFrameHeight().intValue(), false);
				//  All these screen regions were successfully printed and are no longer needed:
				this.changedScreenRegions.get(l).clear();
			}
			return true;
		}
	}

	protected void executeLinePrintingInstructionsAtYOffset(List<LinePrintingInstruction> instructions, Long yOffset) throws Exception{
		this.executeLinePrintingInstructionsAtYOffset(instructions, yOffset, ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT);
	}

	protected void executeLinePrintingInstructionsAtYOffset(List<LinePrintingInstruction> instructions, Long yOffset, int bufferIndex) throws Exception{
		for(int i = 0; i < instructions.size(); i++){
			LinePrintingInstruction instruction = instructions.get(i);
			this.printTextAtScreenXY(instruction.getColouredTextFragmentList(), instruction.getXOffsetInFrame(), yOffset + i, true, bufferIndex);
		}
	}

	protected List<LinePrintingInstructionAtOffset> wrapLinePrintingInstructionsAtOffset(List<LinePrintingInstruction> instructions, Long yOffset, Long step) throws Exception{
		List<LinePrintingInstructionAtOffset> wrappedInstructions = new ArrayList<LinePrintingInstructionAtOffset>();
		for(long i = 0; i < instructions.size(); i++){
			LinePrintingInstruction instruction = instructions.get((int)i);
			wrappedInstructions.add(new LinePrintingInstructionAtOffset(instruction, yOffset + (i*step)));
		}
		return wrappedInstructions;
	}

	protected FrameDimensions getFrameDimensions(){
		if(this.currentFrameChangeWorkItemParams == null){
			return null;
		}else{
			return this.currentFrameChangeWorkItemParams.getCurrentFrameDimensions();
		}
	}

	protected Long getFrameWidth(){
		return this.getFrameDimensions() == null ? null : this.getFrameDimensions().getFrameWidth();
	}

	protected Long getInnerFrameWidth() throws Exception{
		Long fchrw = this.getFrameCharacterWidth();
		return getFrameWidth() - (hasLeftBorder() ? fchrw : 0L) - (hasRightBorder() ? fchrw: 0L);
	}

	protected Long getInnerFrameHeight() throws Exception{
		return getFrameHeight() - (hasTopBorder() ? 1L : 0L) - (hasBottomBorder() ? 1L : 0L);
	}

	protected Long getFrameHeight(){
		return this.getFrameDimensions() == null ? null : this.getFrameDimensions().getFrameHeight();
	}

	protected Long getFrameOffsetX(){
		return this.getFrameDimensions() == null ? null : this.getFrameDimensions().getFrameOffsetX();
	}

	protected Long getFrameOffsetY(){
		return this.getFrameDimensions() == null ? null : this.getFrameDimensions().getFrameOffsetY();
	}

	protected Long getTerminalWidth(){
		return this.getFrameDimensions() == null ? null : this.getFrameDimensions().getTerminalWidth();
	}

	protected Long getTerminalHeight(){
		return this.getFrameDimensions() == null ? null : this.getFrameDimensions().getTerminalHeight();
	}

	public Long getMapAreaCellWidth() throws Exception{
		if(this.mapAreaCellWidth == null){
			GraphicsMode mode = blockManagerThreadCollection.getGraphicsMode();
			this.mapAreaCellWidth = this.clientBlockModelContext.measureTextLengthOnTerminal(BlockSkins.getPresentation(IronOxide.class, mode.equals(GraphicsMode.ASCII))).getDeltaX();
		}
		return this.mapAreaCellWidth;
	}

	public Long getFrameCharacterWidth() throws Exception{
		if(this.frameCharacterWidth == null){
			GraphicsMode mode = blockManagerThreadCollection.getGraphicsMode();

			String exampleFrameCharacter = mode.equals(GraphicsMode.ASCII) ? CharacterConstants.PLUS_SIGN : CharacterConstants.BOX_DRAWINGS_DOUBLE_HORIZONTAL;
			this.frameCharacterWidth = this.clientBlockModelContext.measureTextLengthOnTerminal(exampleFrameCharacter).getDeltaX();
		}
		return this.frameCharacterWidth;
	}

	public Long getFrameCharacterHeight() throws Exception{
		return 1L;
	}


	protected boolean hasBottomBorder(){
		return this.getTerminalHeight().equals(this.getFrameOffsetY() + this.getFrameHeight());
	}

	protected boolean hasTopBorder(){
		return true;
	}

	protected boolean hasLeftBorder(){
		return true;
	}

	protected boolean hasRightBorder() throws Exception{
		//  This should work in both normal and wide character mode:
		return (this.getFrameOffsetX() + this.getFrameWidth() + this.getFrameCharacterWidth()) >= this.getTerminalWidth();
	}

	protected Long getFrameCharacterWidthLeft() throws Exception{
		return this.getFrameCharacterWidth();
	}

	protected Long getFrameCharacterWidthRight() throws Exception{
		return this.getFrameCharacterWidth();
	}

	protected Long getFrameLineWidthTop(){
		return 1L;
	}

	protected Long getFrameLineWidthBottom(){
		return 1L;
	}

	protected Long getTotalXBorderSize() throws Exception{
		return (this.hasLeftBorder() ? this.getFrameCharacterWidthLeft() : 0L) + (this.hasRightBorder() ? this.getFrameCharacterWidthRight() : 0L);
	}

	protected Long getTotalYBorderSize() throws Exception{
		return (this.hasTopBorder() ? this.getFrameLineWidthTop() : 0L) + (this.hasBottomBorder() ? this.getFrameLineWidthBottom() : 0L);
	}


	public FrameBordersDescription getFrameBordersDescription(){
		return this.currentFrameChangeWorkItemParams.getFrameBordersDescription();
	}

	public String getFrameConnectionCharacterForCoordinate(Coordinate center) throws Exception{
		Coordinate top = center.changeByDeltaXY(0L, -1L);  //  Cursor position above
		Coordinate right = center.changeByDeltaXY(this.getFrameCharacterWidth(), 0L); //  Cursor position on right
		Coordinate left = center.changeByDeltaXY(-this.getFrameCharacterWidth(), 0L); //  Cursor position on left
		Coordinate bottom = center.changeByDeltaXY(0L, 1L);//  Cursor position below
		boolean hasTopConnection = getFrameBordersDescription().getFramePoints().contains(top);
		boolean hasRightConnection = getFrameBordersDescription().getFramePoints().contains(right);
		boolean hasLeftConnection = getFrameBordersDescription().getFramePoints().contains(left);
		boolean hasBottomConnection = getFrameBordersDescription().getFramePoints().contains(bottom);
	
		GraphicsMode mode = blockManagerThreadCollection.getGraphicsMode();
		boolean rg = mode.equals(GraphicsMode.ASCII);

		if(!hasTopConnection && !hasRightConnection && !hasLeftConnection && !hasBottomConnection){      // 0000
			return rg ? CharacterConstants.PLUS_SIGN : CharacterConstants.BOX_DRAWINGS_LIGHT_VERTICAL_AND_HORIZONTAL; // ┼  This case should never happen.
		}else if(!hasTopConnection && !hasRightConnection && !hasLeftConnection && hasBottomConnection){ // 0001
			return rg ? CharacterConstants.VERTICAL_LINE : CharacterConstants.BOX_DRAWINGS_DOWN_DOUBLE_AND_HORIZONTAL_SINGLE; // ╥  This case should never happen.
		}else if(!hasTopConnection && !hasRightConnection && hasLeftConnection && !hasBottomConnection){ // 0010
			return rg ? CharacterConstants.EQUALS_SIGN : CharacterConstants.BOX_DRAWINGS_VERTICAL_SINGLE_AND_LEFT_DOUBLE; // ╡  This case should never happen.
		}else if(!hasTopConnection && !hasRightConnection && hasLeftConnection && hasBottomConnection){  // 0011
			return rg ? CharacterConstants.PLUS_SIGN : CharacterConstants.BOX_DRAWINGS_DOUBLE_DOWN_AND_LEFT; // ╗
		}else if(!hasTopConnection && hasRightConnection && !hasLeftConnection && !hasBottomConnection){ // 0100
			return rg ? CharacterConstants.EQUALS_SIGN : CharacterConstants.BOX_DRAWINGS_VERTICAL_SINGLE_AND_RIGHT_DOUBLE; // ╞  This case should never happen.
		}else if(!hasTopConnection && hasRightConnection && !hasLeftConnection && hasBottomConnection){  // 0101
			return rg ? CharacterConstants.PLUS_SIGN : CharacterConstants.BOX_DRAWINGS_DOUBLE_DOWN_AND_RIGHT; // ╔
		}else if(!hasTopConnection && hasRightConnection && hasLeftConnection && !hasBottomConnection){  // 0110
			return rg ? CharacterConstants.EQUALS_SIGN : CharacterConstants.BOX_DRAWINGS_DOUBLE_HORIZONTAL; // ═
		}else if(!hasTopConnection && hasRightConnection && hasLeftConnection && hasBottomConnection){   // 0111
			return rg ? CharacterConstants.PLUS_SIGN : CharacterConstants.BOX_DRAWINGS_DOUBLE_DOWN_AND_HORIZONTAL; // ╦
		}else if(hasTopConnection && !hasRightConnection && !hasLeftConnection && !hasBottomConnection){ // 1000
			return rg ? CharacterConstants.PLUS_SIGN : CharacterConstants.BOX_DRAWINGS_UP_DOUBLE_AND_HORIZONTAL_SINGLE; // ╨  This case should never happen.
		}else if(hasTopConnection && !hasRightConnection && !hasLeftConnection && hasBottomConnection){  // 1001
			return rg ? CharacterConstants.VERTICAL_LINE : CharacterConstants.BOX_DRAWINGS_DOUBLE_VERTICAL; // ║
		}else if(hasTopConnection && !hasRightConnection && hasLeftConnection && !hasBottomConnection){  // 1010
			return rg ? CharacterConstants.PLUS_SIGN : CharacterConstants.BOX_DRAWINGS_DOUBLE_UP_AND_LEFT; // ╝
		}else if(hasTopConnection && !hasRightConnection && hasLeftConnection && hasBottomConnection){   // 1011
			return rg ? CharacterConstants.PLUS_SIGN : CharacterConstants.BOX_DRAWINGS_DOUBLE_VERTICAL_AND_LEFT; // ╣
		}else if(hasTopConnection && hasRightConnection && !hasLeftConnection && !hasBottomConnection){  // 1100
			return rg ? CharacterConstants.PLUS_SIGN : CharacterConstants.BOX_DRAWINGS_DOUBLE_UP_AND_RIGHT; // ╚
		}else if(hasTopConnection && hasRightConnection && !hasLeftConnection && hasBottomConnection){   // 1101
			return rg ? CharacterConstants.PLUS_SIGN : CharacterConstants.BOX_DRAWINGS_DOUBLE_VERTICAL_AND_RIGHT; // ╠
		}else if(hasTopConnection && hasRightConnection && hasLeftConnection && !hasBottomConnection){   // 1110
			return rg ? CharacterConstants.PLUS_SIGN : CharacterConstants.BOX_DRAWINGS_DOUBLE_UP_AND_HORIZONTAL; // ╩
		}else if(hasTopConnection && hasRightConnection && hasLeftConnection && hasBottomConnection){    // 1111
			return rg ? CharacterConstants.PLUS_SIGN : CharacterConstants.BOX_DRAWINGS_DOUBLE_VERTICAL_AND_HORIZONTAL; // ╬
		}else{
			throw new Exception("Impossible.");
		}
	}

	public boolean isCoordinateRelatedToFocusedFrame(Coordinate c, FrameDimensions ffd) throws Exception{
		if(ffd != null){
			if(
				c.getX() >= ffd.getFrameOffsetX() &&
				c.getY() >= ffd.getFrameOffsetY() &&
				c.getX() <= (ffd.getFrameOffsetX() + ffd.getFrameWidth()) &&
				c.getY() <= (ffd.getFrameOffsetY() + ffd.getFrameHeight())
			){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}

	public void drawBorders() throws Exception{
		Long fchrw = this.getFrameCharacterWidth();
		boolean hasLeftBorder = this.hasLeftBorder();
		boolean hasTopBorder = this.hasTopBorder();
		boolean hasRightBorder = this.hasRightBorder();
		boolean hasBottomBorder = this.hasBottomBorder();
		boolean containsBottomLeftHandCorner = hasBottomBorder && hasLeftBorder;
		boolean containsBottomRightHandCorner = hasBottomBorder && hasRightBorder;

		FrameDimensions ffd = this.currentFrameChangeWorkItemParams.getFocusedFrameDimensions();

		if(hasTopBorder){
			ColouredTextFragmentList fragmentList = new ColouredTextFragmentList();
			long borderLength = this.getFrameWidth() / fchrw;
			for(long i = 0; i < borderLength; i++){
				Coordinate c = new Coordinate(Arrays.asList(this.getFrameOffsetX() + i * fchrw, this.getFrameOffsetY()));
				String borderCharacter = this.getFrameConnectionCharacterForCoordinate(c);
				if(this.isCoordinateRelatedToFocusedFrame(c, ffd)){
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {FRAME_BG_COLOR, RED_FG_COLOR}));
				}else{
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {FRAME_BG_COLOR, DEFAULT_TEXT_FG_COLOR}));
				}
			}
			this.printTextAtScreenXY(fragmentList, 0L, 0L, true);
		}
		if(hasLeftBorder){
			ColouredTextFragmentList fragmentList = new ColouredTextFragmentList();
			Long bottomBorderOmission = hasBottomBorder ? -1L : 0L;
			long borderStart = this.getFrameOffsetY() + 1L;
			long borderEnd = this.getFrameOffsetY() + this.getFrameHeight() + bottomBorderOmission;
			long borderLength = borderEnd - borderStart;
			for(long i = borderStart; i < borderEnd; i++){
				Coordinate c = new Coordinate(Arrays.asList(this.getFrameOffsetX(), i));
				String borderCharacter = this.getFrameConnectionCharacterForCoordinate(c);
				if(this.isCoordinateRelatedToFocusedFrame(c, ffd)){
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {FRAME_BG_COLOR, RED_FG_COLOR}));
				}else{
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {FRAME_BG_COLOR, DEFAULT_TEXT_FG_COLOR}));
				}
			}
			this.printTextAtScreenXY(fragmentList, 0L, 1L, false);
		}
		if(hasRightBorder){
			ColouredTextFragmentList fragmentList = new ColouredTextFragmentList();
			Long bottomBorderOmission = hasBottomBorder ? -1L : 0L;
			long borderStart = this.getFrameOffsetY() + 1L;
			long borderEnd = this.getFrameOffsetY() + this.getFrameHeight() + bottomBorderOmission;
			long borderLength = borderEnd - borderStart;
			for(long i = borderStart; i < borderEnd; i++){
				Coordinate c = new Coordinate(Arrays.asList(this.getFrameOffsetX() + this.getFrameWidth() -fchrw, i));
				String borderCharacter = this.getFrameConnectionCharacterForCoordinate(c);
				if(this.isCoordinateRelatedToFocusedFrame(c, ffd)){
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {FRAME_BG_COLOR, RED_FG_COLOR}));
				}else{
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {FRAME_BG_COLOR, DEFAULT_TEXT_FG_COLOR}));
				}
			}
			this.printTextAtScreenXY(fragmentList, this.getFrameWidth() -fchrw, 1L, false);
		}
		if(hasBottomBorder){
			ColouredTextFragmentList fragmentList = new ColouredTextFragmentList();
			long borderLength = this.getFrameWidth() / fchrw;
			for(long i = 0; i < borderLength; i++){
				Coordinate c = new Coordinate(Arrays.asList(this.getFrameOffsetX() + i * fchrw, this.getFrameOffsetY() + this.getFrameHeight() -1));
				String borderCharacter = this.getFrameConnectionCharacterForCoordinate(c);
				if(this.isCoordinateRelatedToFocusedFrame(c, ffd)){
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {FRAME_BG_COLOR, RED_FG_COLOR}));
				}else{
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {FRAME_BG_COLOR, DEFAULT_TEXT_FG_COLOR}));
				}
			}
			this.printTextAtScreenXY(fragmentList, 0L, this.getFrameHeight() -1, true);
		}

		boolean highlightConnectionPoints = false;
		if(highlightConnectionPoints){ //  For debugging the code that draws the frame connections correctly.
			for(Coordinate c : getFrameBordersDescription().getFramePoints()){
				this.printTextAtScreenXY(new ColouredTextFragment("X", new int [] {RED_BG_COLOR}), c.getX() - this.getFrameOffsetX(), c.getY() - this.getFrameOffsetY(), false);
			}
		}
	}

	public void onFrameChange(FrameChangeWorkItemParams params) throws Exception{
		if(
			//  If this change work item is out of date because of a previously rejected console write operation:
			this.currentFrameChangeWorkItemParams != null &&
			(
				params.getFrameDimensionsChangeId() < this.currentFrameChangeWorkItemParams.getFrameDimensionsChangeId() ||
				params.getTerminalDimensionsChangeId() < this.currentFrameChangeWorkItemParams.getTerminalDimensionsChangeId()
			)
		){
			logger.info("Discarding frame change message that's out of date: params.getFrameChangeDimensionsId()=" + params.getFrameDimensionsChangeId() + " this.currentFrameChangeWorkItemParams.getFrameDimensionsChangeId()=" + this.currentFrameChangeWorkItemParams.getFrameDimensionsChangeId() + " || , " + params.getTerminalDimensionsChangeId() + " this.currentFrameChangeWorkItemParams.getTerminalDimensionsChangeId()=" + this.currentFrameChangeWorkItemParams.getTerminalDimensionsChangeId());
		}else{
			logger.info("Not discarding onFrame change, pass to child frame:");
			this.onNewFrameChangeWorkItemParams(params);
			boolean hasFrameDimensionsChanged = this.hasFrameDimensionsChanged(params);
			if(hasFrameDimensionsChanged){
				this.initializeFrames();
			}
			this.onRenderFrame(hasFrameDimensionsChanged);
			boolean printedSuccessfully = this.onFinalizeFrame();
			if(printedSuccessfully){
				this.previousSuccessfullyPrintedFrameChangeWorkItemParams = this.currentFrameChangeWorkItemParams;
			}
		}
	}

	public void writeToLocalFrameBuffer(ScreenLayer changes, ScreenMask mask, ScreenRegion region, FrameDimensions frameDimensions, int bufferIndex) throws Exception{
		int frameWidth = frameDimensions.getFrameWidth().intValue();
		int frameHeight = frameDimensions.getFrameHeight().intValue();
		int startX = region.getStartX();
		int startY = region.getStartY();
		int endX = region.getEndX();
		int endY = region.getEndY();
		for(int x = startX; x < endX; x++){
			for(int y = startY; y < endY; y++){
				int i = x - startX;
				int j = y - startY;
				if(mask.flags[i][j]){
					if(
						//  This function allows frames to try and write off the edge of the screen,
						//  but safely clips the writes to only occur inside the frame buffer.
						//  Don't write beyond current terminal dimenions
						//  Individual frames should be inside terminal area
						//  but terminal resize events means there could
						//  be a temporary inconsistency that makes this check
						//  necessary:
						x < this.getFrameDimensions().getTerminalWidth() &&
						y < this.getFrameDimensions().getTerminalHeight() &&
						x >= 0 &&
						y >= 0 &&
						//  Only allow a frame to write inside it's own borders:
						x < frameWidth &&
						y < frameHeight
					){
						this.bufferedScreenLayers[bufferIndex].characterWidths[x][y] = changes.characterWidths[i][j];
						this.bufferedScreenLayers[bufferIndex].colourCodes[x][y] = changes.colourCodes[i][j];
						this.bufferedScreenLayers[bufferIndex].characters[x][y] = changes.characters[i][j];
					}else{
						//logger.info("Discarding character '" + changes.characters[i][j] + "' because if was out of bounds at x=" + x + ", y=" + y);
					}
				}
			}
		}

		//  Constrain update region into frame buffer:
		int startXConstrained = startX < 0 ? 0 : startX;
		int startYConstrained = startY < 0 ? 0 : startY;
		int endXConstrained = endX < 0 ? 0 : endX;
		int endYConstrained = endY < 0 ? 0 : endY;
		startXConstrained = startXConstrained > frameWidth ? frameWidth : startXConstrained;
		startYConstrained = startYConstrained > frameHeight ? frameHeight : startYConstrained;
		endXConstrained = endXConstrained > frameWidth ? frameWidth : endXConstrained;
		endYConstrained = endYConstrained > frameHeight ? frameHeight : endYConstrained;

		ScreenRegion constrainedRegion = new ScreenRegion(startXConstrained, startYConstrained, endXConstrained, endYConstrained);
		//ScreenRegion constrainedRegion = new ScreenRegion(0, 0, frameWidth, frameHeight);
		/*
		if(
			//  If it's not a zero-sized region:
			(constrainedRegion.getStartX() == constrainedRegion.getEndX()) ||
			(constrainedRegion.getStartY() == constrainedRegion.getEndY())
		){
		}*/
		this.changedScreenRegions.get(bufferIndex).add(constrainedRegion);
	}

	public boolean hasFrameDimensionsChanged(FrameChangeWorkItemParams params){
		FrameDimensions previous = this.previousSuccessfullyPrintedFrameChangeWorkItemParams == null ? null : this.previousSuccessfullyPrintedFrameChangeWorkItemParams.getCurrentFrameDimensions();
		FrameDimensions current = params == null ? null : params.getCurrentFrameDimensions();

		if(current == null){
			return previous != null;
		}else{
			return !current.equals(previous);
		}
	}

	public void onNewFrameChangeWorkItemParams(FrameChangeWorkItemParams params) throws Exception{
		this.currentFrameChangeWorkItemParams = params;
	}

	public void initializeFrames() throws Exception{
		// Refresh screen
		int width = this.getFrameDimensions().getFrameWidth().intValue();
		int height = this.getFrameDimensions().getFrameHeight().intValue();
		//  After every resize event, clear the frame buffer and start with a blank background:
		for(int i = 0; i < this.usedScreenLayers.length; i++){
			int l = usedScreenLayers[i];
			int chrWidth = 0;
			String s = null;
			int [] colours = new int[] {};

			this.bufferedScreenLayers[l] = new ScreenLayer(width, height);
			this.bufferedScreenLayers[l].initialize(chrWidth, s, colours);
			this.previousBufferedScreenLayers[l] = new ScreenLayer(width, height);
			this.previousBufferedScreenLayers[l].initialize(0, null, new int [] {});
			this.bufferedScreenMasks[l].initialize(width, height, true);
			this.changedScreenRegions.get(l).clear();
			this.changedScreenRegions.get(l).add(new ScreenRegion(0, 0, width, height));
		}
	}

	public void setScreenLayerState(int bufferIndex, boolean isActive){
		if(!(this.pendingScreenLayerActiveStates[bufferIndex] == isActive)){
			this.pendingScreenLayerActiveStates[bufferIndex] = isActive;
			//  Refresh for all layers when a layer changes active state
			int width = this.getFrameDimensions().getFrameWidth().intValue();
			int height = this.getFrameDimensions().getFrameHeight().intValue();
			for(int i = 0; i < this.usedScreenLayers.length; i++){
				int l = usedScreenLayers[i];
				this.changedScreenRegions.get(l).add(
					new ScreenRegion(
						0,
						0,
						width,
						height
					)
				);
				this.bufferedScreenMasks[l].initialize(width, height, true);
			}
		}
	}

	public List<ScreenLayerPrintParameters> makeScreenPrintParameters(ScreenLayer l, ScreenMask m, Set<ScreenRegion> regions, int bufferIndex, boolean isActive) throws Exception{
		List<ScreenLayerPrintParameters> rtn = new ArrayList<ScreenLayerPrintParameters>();
		rtn.add(new ScreenLayerPrintParameters(l, m, regions, bufferIndex, isActive));
		return rtn;
	}

	public void computeFrameDifferences(ScreenLayer previous, ScreenLayer current, ScreenMask mask, int bufferIndex){
		for(int i = 0; i < current.width; i++){
			for(int j = 0; j < current.height; j++){
				boolean hasChanged = !(
					(current.characterWidths[i][j] == previous.characterWidths[i][j]) &&
					Arrays.equals(current.colourCodes[i][j], previous.colourCodes[i][j]) &&
					Objects.equals(current.characters[i][j], previous.characters[i][j])
				) || mask.flags[i][j];
				mask.flags[i][j] = hasChanged;
			}
		}
	}

	public boolean onFinalizeFrame() throws Exception{
		//  Send message with current frame contents.
		if(this.getFrameDimensions() != null){
			List<ScreenLayerPrintParameters> params = new ArrayList<ScreenLayerPrintParameters>();
			for(int i = 0; i < this.usedScreenLayers.length; i++){
				int l = usedScreenLayers[i];
				int xOffset = 0;
				int yOffset = 0;

				int width = this.getFrameDimensions().getFrameWidth().intValue();
				int height = this.getFrameDimensions().getFrameHeight().intValue();

				this.computeFrameDifferences(this.previousBufferedScreenLayers[l], this.bufferedScreenLayers[l], this.bufferedScreenMasks[l], l);
				params.addAll(
					makeScreenPrintParameters(
						this.bufferedScreenLayers[l],
						this.bufferedScreenMasks[l],
						this.changedScreenRegions.get(l),
						l,
						this.pendingScreenLayerActiveStates[l]
					)
				);
			}
			return this.sendConsolePrintMessage(params, this.getFrameDimensions());
		}
		return false;
	}

	public void sendCellUpdatesInScreenArea(CuboidAddress areaToUpdate, String [][] updatedCellContents, int [][][] updatedColourCodes, Long drawOffsetX, Long drawOffsetY) throws Exception{
		//  Print a square of padded cells on the terminal.
		int areaCellWidth = (int)areaToUpdate.getWidthForIndex(0L);
		int areaCellHeight = (int)areaToUpdate.getWidthForIndex(2L);
		int totalWidth = (int)(this.getMapAreaCellWidth() * areaCellWidth);
		int totalHeight = areaCellHeight;

		ScreenLayer changes = new ScreenLayer(totalWidth, totalHeight);

		ScreenMask mask = new ScreenMask();
		mask.initialize(totalWidth, totalHeight, false);

		for(int j = 0; j < areaCellHeight; j++){
			int currentOffset = 0;
			for(int i = 0; i < areaCellWidth; i++){
				if(updatedCellContents[i][j] == null){
					int paddedWidthSoFar = 0;
					while(paddedWidthSoFar < this.getMapAreaCellWidth()){
						changes.colourCodes[currentOffset][j] = new int [] {MAP_CELL_BG_COLOR1};
						changes.characters[currentOffset][j] = null;
						changes.characterWidths[currentOffset][j] = 0;
						mask.flags[currentOffset][j] = false;
						currentOffset += 1;
						paddedWidthSoFar += 1;
					}
				}else{
					// The character in the cell
					int chrWidth = this.clientBlockModelContext.measureTextLengthOnTerminal(updatedCellContents[i][j]).getDeltaX().intValue();
					changes.colourCodes[currentOffset][j] = updatedColourCodes[i][j];
					changes.characters[currentOffset][j] = updatedCellContents[i][j];
					changes.characterWidths[currentOffset][j] = chrWidth;
					mask.flags[currentOffset][j] = true;
					currentOffset += (chrWidth > 0) ? 1 : 0;

					//  For multi-column characters, reset any of the 'covered'
					//  columns take up by the multi-column character:
					for(int k = 1; k < chrWidth; k++){
						changes.colourCodes[currentOffset][j] = new int [] {};
						changes.characters[currentOffset][j] = null;
						changes.characterWidths[currentOffset][j] = 0;
						mask.flags[currentOffset][j] = true;
						currentOffset++;
					}

					// The padding after the character:
					int paddedWidthSoFar = chrWidth;
					while(paddedWidthSoFar < this.getMapAreaCellWidth()){
						String space = " ";
						int spaceWidth = this.clientBlockModelContext.measureTextLengthOnTerminal(space).getDeltaX().intValue();
						changes.colourCodes[currentOffset][j] = updatedColourCodes[i][j];
						changes.characters[currentOffset][j] = space;
						changes.characterWidths[currentOffset][j] = spaceWidth;
						mask.flags[currentOffset][j] = true;
						currentOffset += spaceWidth;
						paddedWidthSoFar += spaceWidth;
					}
				}
				if(!(currentOffset == ((i+1) * this.getMapAreaCellWidth()))){
					throw new Exception("not expected: currentOffset=" + currentOffset + " and ((i+1) * this.getMapAreaCellWidth())=" + ((i+1) * this.getMapAreaCellWidth()));
				}
			}
		}

		int xOffset = drawOffsetX.intValue();
		int yOffset = drawOffsetY.intValue();
		int xSize = totalWidth;
		int ySize = totalHeight;

		ScreenRegion region = new ScreenRegion(xOffset, yOffset, xOffset + xSize, yOffset + ySize);
		this.writeToLocalFrameBuffer(changes, mask, region, this.getFrameDimensions(), ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT);
	}

	public String whitespacePad(String presentedText, Long paddedWidth) throws Exception{
		//  An empty cell with zero byte length will otherwise render to nothing causing the last cell to not get overprinted.
		//  Adding the extra space after the Rocks because the 'rock' emoji only takes up one space for the background colour, and BG colour won't update correctly otherwise.

		Long presentedTextWidth = this.clientBlockModelContext.measureTextLengthOnTerminal(presentedText).getDeltaX();
		if(presentedTextWidth > paddedWidth){
			throw new Exception("Text has terminal width of " + presentedTextWidth + " which is wider than allowed paddedWidth value of " + paddedWidth);
		}

		while(presentedTextWidth < paddedWidth){
			presentedText += " ";
			presentedTextWidth += 1;
		}

		return presentedText;
	}

	public void clearFrame() throws Exception{
		for(long l = 0L; l < this.getFrameHeight(); l++){
			int repeatNumber = this.getFrameWidth().intValue();
			if(repeatNumber < 0){
				throw new Exception("repeatNumber is negative: " + repeatNumber);
			}
			this.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(repeatNumber), new int[] {FRAME_CLEAR_BG_COLOR}), 0L, l, true);
		}
		if(!this.completedInitialOpen){
			this.onFinalizeFrame(); //  Initial clear frame to give fast feedback to user.
			this.completedInitialOpen = true;
		}
	}

	private ClientBlockModelContext clientBlockModelContext;

	public UserInterfaceFrameThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext, int [] usedScreenLayers) throws Exception {
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
		this.frameId = seq.getAndIncrement();
		this.usedScreenLayers = usedScreenLayers;

		for(int i = 0; i < usedScreenLayers.length; i++){
			this.bufferedScreenLayers[usedScreenLayers[i]] = new ScreenLayer(0,0);
			this.previousBufferedScreenLayers[usedScreenLayers[i]] = new ScreenLayer(0,0);
			this.bufferedScreenMasks[usedScreenLayers[i]] = new ScreenMask();
		}

		for(int i = 0; i < ConsoleWriterThreadState.numScreenLayers; i++){
			this.changedScreenRegions.add(new TreeSet<ScreenRegion>());
			this.pendingScreenLayerActiveStates[i] = true;
		}
	}

	public long getFrameId(){
		return this.frameId;
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}
}
