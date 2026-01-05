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

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

import org.res.block.WorkItem;
import org.res.block.BlockSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class TextInputArea {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private List<List<String>> text = new ArrayList<List<String>>();
	private TextInputContainer container;
	private Coordinate placementOffset = new Coordinate(Arrays.asList(0L, 0L));
	protected ScreenLayer displayLayer = new ScreenLayer();
	//  For if the text area is bigger than what's displayed on screen:
	private int displayedColumnOffsetX = 0;
	private int displayedColumnOffsetY = 0;
	//  Offset relative to the entire (possibly partially hidden) text area:
	private int cursorCharacterX = 0;
	private int cursorLineY = 0;

	public TextInputArea(TextInputContainer container) throws Exception{
		this.container = container;
		this.text.add(new ArrayList<String>()); //  Initialize with one empty line
	}

	public void onKeyboardCharacter(UserInterfaceFrameThreadState frame, String character) throws Exception {
		if(character.equals(CharacterConstants.BACKSPACE_CHARACTER)){
			if(this.text.size() > 0 && this.text.get(cursorLineY).size() > 0 && this.cursorCharacterX > 0){
				//  Delete character just before cursor:
				this.text.get(cursorLineY).remove(cursorCharacterX - 1);
				this.cursorCharacterX--;
			}else{
				//  Delete previous new line if it exists
			}
		}else if(character.equals(CharacterConstants.CARRIAGE_RETURN_CHARACTER)){
			List<String> beforeCursor = this.text.get(cursorLineY).subList(0, cursorCharacterX);
			List<String> afterCursor = this.text.get(cursorLineY).subList(cursorCharacterX, this.text.get(cursorLineY).size());
			this.text.remove(cursorLineY);
			this.text.add(cursorLineY, beforeCursor);
			this.text.add(cursorLineY + 1, afterCursor);
			cursorLineY++;
			this.cursorCharacterX = 0;
		}else{
			this.text.get(cursorLineY).add(cursorCharacterX, character);
			this.cursorCharacterX += 1;
		}

		this.setCursorPosition(frame);
	}

	public int getColumnOffsetForCharacterAtIndex(UserInterfaceFrameThreadState frame, int characterXIndex) throws Exception{
		int totalColumnOffset = 0;
		for(int i = 0; i < characterXIndex && i < this.text.get(cursorLineY).size(); i++){
			String c = this.text.get(cursorLineY).get(i);

			TextWidthMeasurementWorkItemResult m = frame.getConsoleWriterThreadState().measureTextLengthOnTerminal(c);
			totalColumnOffset += m.getDeltaX().intValue();
		}
		return totalColumnOffset;
	}

	public void setCursorPosition(UserInterfaceFrameThreadState frame) throws Exception{
		//  Keep cursor inside text area:
		int cursorOffsetXInTextArea = getColumnOffsetForCharacterAtIndex(frame, this.cursorCharacterX);

		if(
			//  Cursor beyond left edge of input area:
			(cursorOffsetXInTextArea - this.displayedColumnOffsetX) < 0 ||
			//  Cursor beyond rigth edge of input area:
			(cursorOffsetXInTextArea > (this.displayLayer.getWidth() -1))
		){
			this.displayedColumnOffsetX = Math.max(0, cursorOffsetXInTextArea - (this.displayLayer.getWidth() -1));
		}

		this.container.onCursorPositionChange(
			new Coordinate(Arrays.asList(
				placementOffset.getX() - this.displayedColumnOffsetX + this.getColumnOffsetForCharacterAtIndex(frame, this.cursorCharacterX) + 1L,
				placementOffset.getY() - this.displayedColumnOffsetY + this.cursorLineY + 1L
			))
		);
	}

	public void onAnsiEscapeSequence(UserInterfaceFrameThreadState frame, AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		if(ansiEscapeSequence instanceof AnsiEscapeSequenceUpArrowKey){
			if(this.cursorLineY > 0){
				this.cursorLineY--;
			}
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceRightArrowKey){
			if(this.cursorCharacterX < this.text.get(cursorLineY).size()){
				this.cursorCharacterX++;
			}
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceDownArrowKey){
			if(this.cursorLineY < (this.text.size() - 1)){
				this.cursorLineY++;
			}
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceLeftArrowKey){
			if(this.cursorCharacterX > 0){
				this.cursorCharacterX--;
			}
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceRemoveKey){
			if(this.text.size() > 0 && this.text.get(cursorLineY).size() > 0 && this.cursorCharacterX < this.text.get(cursorLineY).size()){
				//  Delete character just after cursor:
				this.text.get(cursorLineY).remove(cursorCharacterX);
			}else{
				//  Delete following new line if it exists
			}
			this.setCursorPosition(frame);
		}else{
			logger.info("Discarding unknown ansi escape sequence of type: " + ansiEscapeSequence.getClass().getName());
		}
		this.setCursorPosition(frame);
	}

	public void updateRenderableArea(Coordinate placementOffset, CuboidAddress visibleArea, Long maxColumnWidth, Long maxLines) throws Exception{
		this.placementOffset = placementOffset;
		int width = (int)visibleArea.getWidth();
		int height = (int)visibleArea.getHeight();

		this.displayLayer = new ScreenLayer(this.displayLayer.getPlacementOffset(), ScreenLayer.makeDimensionsCA(0, 0, width, height));
		//  Initialize to an obvious pattern.  Should be overwritten by child class:
		this.displayLayer.initializeInRegion(1, "x", new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR, UserInterfaceFrameThreadState.BLUE_BG_COLOR}, null, new ScreenRegion(ScreenRegion.makeScreenRegionCA(0, 0, width, height)), true, true);

	}

	public void render(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		//  Initialize to clear any backspaced characters:
		this.displayLayer.initializeInRegion(1, " ", UserInterfaceFrameThreadState.getTextInputAreaColors(), null, new ScreenRegion(ScreenRegion.makeScreenRegionCA(0, 0, this.displayLayer.getWidth(), this.displayLayer.getHeight())), true, true);

		for(int i = 0; i < this.text.size(); i++){
			ColouredTextFragment tf = new ColouredTextFragment(String.join("", this.text.get(i)), UserInterfaceFrameThreadState.getTextInputAreaColors());

			frame.printTextAtScreenXY(tf, (long)-this.displayedColumnOffsetX, (long)i, PrintDirection.LEFT_TO_RIGHT, this.displayLayer);
		}

		this.displayLayer.setPlacementOffset(this.placementOffset);
		bottomLayer.mergeDown(this.displayLayer, true, ScreenLayerMergeType.PREFER_BOTTOM_LAYER);
	}
}
