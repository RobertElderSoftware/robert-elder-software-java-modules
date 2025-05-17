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

import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import java.util.concurrent.atomic.AtomicLong;

public abstract class UserInterfaceFrameThreadState extends WorkItemQueueOwner<UIWorkItem>{

	private static final AtomicLong seq = new AtomicLong(0);
	public final long frameId;

	public abstract void putWorkItem(UIWorkItem workItem, WorkItemPriority priority) throws Exception;
	public abstract void render() throws Exception;
	public abstract void onFrameDimensionsChanged() throws Exception;
	public abstract void onFrameFocusChanged() throws Exception;
	public abstract void onKeyboardInput(byte [] characters) throws Exception;
	public abstract void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception;

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;
	protected FrameBordersDescription frameBordersDescription; // The set of screen coordinates that describe neighbouring frame edges (for drawing frames properly).

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

	protected FrameDimensions focusedFrameDimensions;  // The dimensions of whatever frame currently has focus.
	protected FrameDimensions frameDimensions;

	public FrameDimensions getFrameDimensions(){
		return frameDimensions;
	}

	private List<MeasuredTextFragment> getMeasuredTextFragments(ColouredTextFragmentList fragmentList, Long maxLineWidth) throws Exception{
		List<MeasuredTextFragment> textFragments = new ArrayList<MeasuredTextFragment>();
		for(ColouredTextFragment cf : fragmentList.getColouredTextFragments()){
			String newlineCharacter = "\n";
			String spaceCharacter = " ";
			String[] lines = cf.getText().split(newlineCharacter);
			for(int i = 0; i < lines.length; i++){
				String[] words = lines[i].split(spaceCharacter);
				for(int j = 0; j < words.length; j++){
					List<String> characters = ColouredTextFragment.splitStringIntoCharactersUnicodeAware(words[j]);
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
					textFragments.add(new MeasuredTextFragment(new ColouredTextFragment(String.join("", charactersSoFar), cf.getAnsiColourCodes()), new TextWidthMeasurementWorkItemResult(wordLength, wordHeight)));
					if(j != words.length -1){
						//  Add back space characters between words
						//  as long as they're not at the end of lines.
						textFragments.add(new MeasuredTextFragment(new ColouredTextFragment(spaceCharacter, cf.getAnsiColourCodes()), this.clientBlockModelContext.measureTextLengthOnTerminal(spaceCharacter)));
					}
				}
				if(i != lines.length -1){
					textFragments.add(new MeasuredTextFragment(new ColouredTextFragment(newlineCharacter, cf.getAnsiColourCodes()), this.clientBlockModelContext.measureTextLengthOnTerminal(newlineCharacter)));
				}
			}
		}

		return textFragments;
	}

	protected List<LinePrintingInstruction> getLinePrintingInstructions(String text, Long paddingLeft, Long paddingRight, boolean leftAlign, boolean rightAlign, Long maxLineLength) throws Exception{
		ColouredTextFragmentList tfl = new ColouredTextFragmentList(new ColouredTextFragment(text, new int[] {RESET_BG_COLOR}));
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
				Long extraSpace = this.getInnerFrameWidth() - lineLengthSoFar;
				Long textOffset = leftAlign ? paddingLeft : (extraSpace / 2L);
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
				boolean isSpace = textFragment.getColouredTextFragment().getText().equals(" ");
				//  Do not add leading spaces to the start of a line:
				if(!isSpace || lineLengthSoFar > 0L){
				lineLengthSoFar += textFragment.getTextDisplacement().getDeltaX();
					currentLineFragments.add(textFragment.getColouredTextFragment());
				}
				i++;
			}
		}
		return instructions;
	}


	protected void printTextAtScreenXY(ColouredTextFragment colouredTextFragment, Long drawOffsetX, Long drawOffsetY, boolean xDirection, int bufferIndex) throws Exception{
		this.printTextAtScreenXY(new ColouredTextFragmentList(Arrays.asList(colouredTextFragment)), drawOffsetX, drawOffsetY, this.frameDimensions, xDirection, bufferIndex);
	}

	protected void printTextAtScreenXY(ColouredTextFragment colouredTextFragment, Long drawOffsetX, Long drawOffsetY, boolean xDirection) throws Exception{
		this.printTextAtScreenXY(new ColouredTextFragmentList(Arrays.asList(colouredTextFragment)), drawOffsetX, drawOffsetY, this.frameDimensions, xDirection, ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT);
	}

	protected void printTextAtScreenXY(ColouredTextFragmentList colouredTextFragmentList, Long drawOffsetX, Long drawOffsetY, boolean xDirection, int bufferIndex) throws Exception{
		this.printTextAtScreenXY(colouredTextFragmentList, drawOffsetX, drawOffsetY, this.frameDimensions, xDirection, bufferIndex);
	}

	protected void printTextAtScreenXY(ColouredTextFragmentList colouredTextFragmentList, Long drawOffsetX, Long drawOffsetY, boolean xDirection) throws Exception{
		this.printTextAtScreenXY(colouredTextFragmentList, drawOffsetX, drawOffsetY, this.frameDimensions, xDirection, ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT);
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

		sendConsolePrintMessage(characterWidths, colourCodes, characters, hasChange, xOffset, yOffset, xSize, ySize, fd, bufferIndex);
	}

	public void sendConsolePrintMessage(int [][] characterWidths, int [][][] colourCodes, String [][] characters, boolean [][] hasChange, int xOffset, int yOffset, int xSize, int ySize, FrameDimensions fd, int bufferIndex) throws Exception{

		this.clientBlockModelContext.getConsoleWriterThreadState().putWorkItem(new ConsoleWriteWorkItem(this.clientBlockModelContext.getConsoleWriterThreadState(), characterWidths, colourCodes, characters, hasChange, xOffset, yOffset, xSize, ySize, fd, bufferIndex), WorkItemPriority.PRIORITY_LOW);
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

	protected Long getFrameWidth(){
		return this.frameDimensions == null ? null : this.frameDimensions.getFrameWidth();
	}

	protected Long getInnerFrameWidth() throws Exception{
		Long fchrw = this.getFrameCharacterWidth();
		return getFrameWidth() - (hasLeftBorder() ? fchrw : 0L) - (hasRightBorder() ? fchrw: 0L);
	}

	protected Long getFrameHeight(){
		return this.frameDimensions == null ? null : this.frameDimensions.getFrameHeight();
	}

	protected Long getFrameOffsetX(){
		return this.frameDimensions == null ? null : this.frameDimensions.getFrameOffsetX();
	}

	protected Long getFrameOffsetY(){
		return this.frameDimensions == null ? null : this.frameDimensions.getFrameOffsetY();
	}

	protected Long getTerminalWidth(){
		return this.frameDimensions == null ? null : this.frameDimensions.getTerminalWidth();
	}

	protected Long getTerminalHeight(){
		return this.frameDimensions == null ? null : this.frameDimensions.getTerminalHeight();
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


	public void clearFrame() throws Exception{
		for(long l = 0L; l < this.getFrameHeight(); l++){
			this.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(this.getFrameWidth().intValue()), new int[] {RESET_BG_COLOR}), 0L, l, true);
		}
	}

	public String getFrameConnectionCharacterForCoordinate(Coordinate center) throws Exception{
		Coordinate top = center.changeByDeltaXY(0L, -1L);  //  Cursor position above
		Coordinate right = center.changeByDeltaXY(this.getFrameCharacterWidth(), 0L); //  Cursor position on right
		Coordinate left = center.changeByDeltaXY(-this.getFrameCharacterWidth(), 0L); //  Cursor position on left
		Coordinate bottom = center.changeByDeltaXY(0L, 1L);//  Cursor position below
		boolean hasTopConnection = this.frameBordersDescription.getFramePoints().contains(top);
		boolean hasRightConnection = this.frameBordersDescription.getFramePoints().contains(right);
		boolean hasLeftConnection = this.frameBordersDescription.getFramePoints().contains(left);
		boolean hasBottomConnection = this.frameBordersDescription.getFramePoints().contains(bottom);
	
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
		//  TODO:  This is not thread safe.
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

	public void drawBorders(boolean alsoClearBackground) throws Exception{
		if(alsoClearBackground){
			this.clearFrame();
		}
		Long fchrw = this.getFrameCharacterWidth();
		boolean hasLeftBorder = this.hasLeftBorder();
		boolean hasTopBorder = this.hasTopBorder();
		boolean hasRightBorder = this.hasRightBorder();
		boolean hasBottomBorder = this.hasBottomBorder();
		boolean containsBottomLeftHandCorner = hasBottomBorder && hasLeftBorder;
		boolean containsBottomRightHandCorner = hasBottomBorder && hasRightBorder;

		FrameDimensions ffd = this.clientBlockModelContext.getConsoleWriterThreadState().getFrameStateById(this.clientBlockModelContext.getConsoleWriterThreadState().focusedFrameId).getFrameDimensions();

		if(hasTopBorder){
			ColouredTextFragmentList fragmentList = new ColouredTextFragmentList();
			long borderLength = this.getFrameWidth() / fchrw;
			for(long i = 0; i < borderLength; i++){
				Coordinate c = new Coordinate(Arrays.asList(this.getFrameOffsetX() + i * fchrw, this.getFrameOffsetY()));
				String borderCharacter = this.getFrameConnectionCharacterForCoordinate(c);
				if(this.isCoordinateRelatedToFocusedFrame(c, ffd)){
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {RESET_BG_COLOR, RED_FG_COLOR}));
				}else{
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {RESET_BG_COLOR}));
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
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {RESET_BG_COLOR, RED_FG_COLOR}));
				}else{
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {RESET_BG_COLOR}));
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
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {RESET_BG_COLOR, RED_FG_COLOR}));
				}else{
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {RESET_BG_COLOR}));
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
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {RESET_BG_COLOR, RED_FG_COLOR}));
				}else{
					fragmentList.add(new ColouredTextFragment(borderCharacter, new int [] {RESET_BG_COLOR}));
				}
			}
			this.printTextAtScreenXY(fragmentList, 0L, this.getFrameHeight() -1, true);
		}

		boolean highlightConnectionPoints = false;
		if(highlightConnectionPoints){ //  For debugging the code that draws the frame connections correctly.
			for(Coordinate c : this.frameBordersDescription.getFramePoints()){
				this.printTextAtScreenXY(new ColouredTextFragment("X", new int [] {RED_BG_COLOR}), c.getX() - this.getFrameOffsetX(), c.getY() - this.getFrameOffsetY(), false);
			}
		}
	}

	public void onFrameDimensionsChange(FrameDimensions frameDimensions, FrameBordersDescription frameBordersDescription) throws Exception{
		this.frameDimensions = frameDimensions;
		this.frameBordersDescription = frameBordersDescription;
		this.onFrameDimensionsChanged();
	}

	public void onFrameFocusChange(FrameDimensions focusedFrameDimensions) throws Exception{
		this.focusedFrameDimensions = focusedFrameDimensions;
		this.onFrameFocusChanged();
	}

	public void sendCellUpdatesInScreenArea(CuboidAddress areaToUpdate, String [][] updatedCellContents, int [][][] updatedColourCodes, Long drawOffsetX, Long drawOffsetY) throws Exception{
		//  Print a square of padded cells on the terminal.
		int areaCellWidth = (int)areaToUpdate.getWidthForIndex(0L);
		int areaCellHeight = (int)areaToUpdate.getWidthForIndex(2L);
		int totalWidth = (int)(this.getMapAreaCellWidth() * areaCellWidth);
		int totalHeight = areaCellHeight;

		int [][] characterWidths = new int[totalWidth][totalHeight];
		int [][][] colourCodes = new int[totalWidth][totalHeight][1];
		String [][] characters = new String[totalWidth][totalHeight];
		boolean [][] hasChange = new boolean [totalWidth][totalHeight];

		for(int j = 0; j < areaCellHeight; j++){
			int currentOffset = 0;
			for(int i = 0; i < areaCellWidth; i++){
				if(updatedCellContents[i][j] == null){
					int paddedWidthSoFar = 0;
					while(paddedWidthSoFar < this.getMapAreaCellWidth()){
						colourCodes[currentOffset][j] = new int [] {RESET_BG_COLOR};
						characters[currentOffset][j] = null;
						characterWidths[currentOffset][j] = 0;
						hasChange[currentOffset][j] = false;
						currentOffset += 1;
						paddedWidthSoFar += 1;
					}
				}else{
					// The character in the cell
					int chrWidth = this.clientBlockModelContext.measureTextLengthOnTerminal(updatedCellContents[i][j]).getDeltaX().intValue();
					colourCodes[currentOffset][j] = updatedColourCodes[i][j];
					characters[currentOffset][j] = updatedCellContents[i][j];
					characterWidths[currentOffset][j] = chrWidth;
					hasChange[currentOffset][j] = true;
					currentOffset += (chrWidth > 0) ? 1 : 0;

					//  For multi-column characters, reset any of the 'covered'
					//  columns take up by the multi-column character:
					for(int k = 1; k < chrWidth; k++){
						colourCodes[currentOffset][j] = new int [] {};
						characters[currentOffset][j] = null;
						characterWidths[currentOffset][j] = 0;
						hasChange[currentOffset][j] = true;
						currentOffset++;
					}

					// The padding after the character:
					int paddedWidthSoFar = chrWidth;
					while(paddedWidthSoFar < this.getMapAreaCellWidth()){
						String space = " ";
						int spaceWidth = this.clientBlockModelContext.measureTextLengthOnTerminal(space).getDeltaX().intValue();
						colourCodes[currentOffset][j] = updatedColourCodes[i][j];
						characters[currentOffset][j] = space;
						characterWidths[currentOffset][j] = spaceWidth;
						hasChange[currentOffset][j] = true;
						currentOffset += spaceWidth;
						paddedWidthSoFar += spaceWidth;
					}
				}
				if(!(currentOffset == ((i+1) * this.getMapAreaCellWidth()))){
					throw new Exception("not expected: currentOffset=" + currentOffset + " and ((i+1) * this.getMapAreaCellWidth())=" + ((i+1) * this.getMapAreaCellWidth()));
				}
			}
		}

		int xOffset = drawOffsetX.intValue() + this.frameDimensions.getFrameOffsetX().intValue();
		int yOffset = drawOffsetY.intValue() + this.frameDimensions.getFrameOffsetY().intValue();
		int xSize = totalWidth;
		int ySize = totalHeight;

		this.sendConsolePrintMessage(characterWidths, colourCodes, characters, hasChange, xOffset, yOffset, xSize, ySize, this.frameDimensions, ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT);
	}

	public String whitespacePadMapAreaCell(String presentedText) throws Exception{
		//  An empty cell with zero byte length will otherwise render to nothing causing the last cell to not get overprinted.
		//  Adding the extra space after the Rocks because the 'rock' emoji only takes up one space for the background colour, and BG colour won't update correctly otherwise.

		Long presentedTextWidth = this.clientBlockModelContext.measureTextLengthOnTerminal(presentedText).getDeltaX();
		Long paddedMapAreaCellWidth = this.getMapAreaCellWidth();
		if(presentedTextWidth > paddedMapAreaCellWidth){
			throw new Exception("Character has terminal width of " + presentedTextWidth + " which is wider than allowed paddedMapAreaCellWidth value of " + paddedMapAreaCellWidth);
		}

		while(presentedTextWidth < paddedMapAreaCellWidth){
			presentedText += " ";
			presentedTextWidth += 1;
		}

		return presentedText;
	}

	private ClientBlockModelContext clientBlockModelContext;

	public UserInterfaceFrameThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception {
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
		this.frameId = seq.getAndIncrement();
	}

	public long getFrameId(){
		return this.frameId;
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}
}
