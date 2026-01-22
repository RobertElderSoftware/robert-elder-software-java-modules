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

public class InputFormTextArea extends InputFormElement {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private List<List<String>> text = new ArrayList<List<String>>();
	protected ScreenLayer displayLayer = new ScreenLayer();
	//  For if the text area is bigger than what's displayed on screen:
	private int displayedColumnOffsetX = 0;
	private int displayedColumnOffsetY = 0;
	//  Offset relative to the entire (possibly partially hidden) text area:
	//  These offsets are relative to character # (not column):
	private int cursorCharacterX = 0;
	private int cursorLineY = 0;
	private Long maxLines = null;

	public InputFormTextArea(String name, InputElementContainer container) throws Exception{
		super(name, container);
		this.text.add(new ArrayList<String>()); //  Initialize with one empty line
	}

	public InputFormTextArea(String name, InputElementContainer container, Long maxLines) throws Exception{
		super(name, container);
		this.text.add(new ArrayList<String>()); //  Initialize with one empty line
		this.maxLines = maxLines;
	}

	public void setText(InputElementContainer container, String defaultText) throws Exception{
		this.text.clear();
		this.text.add(new ArrayList<String>());
		//  Input the default text just as if it was regularly typed text:
		for(String character : UserInterfaceFrameThreadState.splitStringIntoCharactersUnicodeAware(defaultText)){
			this.onKeyboardCharacter(container, character);
		}
	}

	public String getName(){
		return this.name;
	}

	public void onKeyboardCharacter(InputElementContainer container, String character) throws Exception {
		if(character.equals(CharacterConstants.BACKSPACE_CHARACTER)){
			if(this.text.size() > 0 && this.text.get(cursorLineY).size() > 0 && this.cursorCharacterX > 0){
				//  Delete character just before cursor:
				this.text.get(cursorLineY).remove(cursorCharacterX - 1);
				this.cursorCharacterX--;
			}else{
				//  Delete previous new line if it exists and join the lines:
				if(this.text.size() > 1 && this.cursorLineY > 0){
					List<String> newLine = new ArrayList<String>();
					int newXOffset = this.text.get(cursorLineY - 1).size();
					newLine.addAll(this.text.get(cursorLineY - 1));
					newLine.addAll(this.text.get(cursorLineY));
					this.text.remove(cursorLineY -1); //  Remove previous line
					this.text.remove(cursorLineY -1); //  Remove 'current' line.
					this.text.add(cursorLineY - 1, newLine);
					cursorLineY--;
					this.cursorCharacterX = newXOffset;
				}
			}
		}else if(character.equals(CharacterConstants.CARRIAGE_RETURN_CHARACTER)){
			if(this.maxLines == null || this.text.size() < this.maxLines){
				List<String> beforeCursor = new ArrayList<String>(this.text.get(cursorLineY).subList(0, cursorCharacterX));
				List<String> afterCursor = new ArrayList<String>(this.text.get(cursorLineY).subList(cursorCharacterX, this.text.get(cursorLineY).size()));
				this.text.remove(cursorLineY);
				this.text.add(cursorLineY, beforeCursor);
				this.text.add(cursorLineY + 1, afterCursor);
				cursorLineY++;
				this.cursorCharacterX = 0;
			}else{
				//  Do nothing, ignore new line.
			}
		}else{
			this.text.get(cursorLineY).add(cursorCharacterX, character);
			this.cursorCharacterX += 1;
		}
	}

	public int getColumnOffsetForCharacterAtIndex(InputElementContainer container, int characterXIndex) throws Exception{
		int totalColumnOffset = 0;
		for(int i = 0; i < characterXIndex && i < this.text.get(cursorLineY).size(); i++){
			String c = this.text.get(cursorLineY).get(i);

			TextWidthMeasurementWorkItemResult m = container.getConsoleWriterThreadState().measureTextLengthOnTerminal(c);
			totalColumnOffset += m.getDeltaX().intValue();
		}
		return totalColumnOffset;
	}

	public void sendCursorUpdate() throws Exception{
		this.container.onCursorPositionChange(
			new Coordinate(Arrays.asList(
				placementOffset.getX() - this.displayedColumnOffsetX + this.getColumnOffsetForCharacterAtIndex(container, this.cursorCharacterX) + 1L,
				placementOffset.getY() - this.displayedColumnOffsetY + this.cursorLineY + 1L
			))
		);
	}

	public void setCursorPosition(InputElementContainer container) throws Exception{
		//  Keep cursor inside text area:
		int cursorOffsetXInTextArea = getColumnOffsetForCharacterAtIndex(container, this.cursorCharacterX);

		if(
			//  Cursor beyond left edge of input area:
			(cursorOffsetXInTextArea - this.displayedColumnOffsetX) < 0 ||
			//  Cursor beyond right edge of input area:
			(cursorOffsetXInTextArea > (this.displayLayer.getWidth() -1))
		){
			this.displayedColumnOffsetX = Math.max(0, cursorOffsetXInTextArea - (this.displayLayer.getWidth() -1));
		}

		if(
			//  Cursor beyond top edge of input area:
			(this.cursorLineY - this.displayedColumnOffsetY) < 0 ||
			//  Cursor beyond bottom edge of input area:
			(this.cursorLineY > (this.displayLayer.getHeight() -1))
		){
			this.displayedColumnOffsetY = Math.max(0, this.cursorLineY - (this.displayLayer.getHeight() -1));
		}


		this.sendCursorUpdate();
	}

	public void onAnsiEscapeSequence(InputElementContainer container, AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		if(ansiEscapeSequence instanceof AnsiEscapeSequenceUpArrowKey){
			if(this.cursorLineY > 0){
				this.cursorLineY--;
				int currentCharacterIndex = this.cursorCharacterX;
				int maxCharacterIndexNextLine = this.text.get(this.cursorLineY).size();
				if(currentCharacterIndex > maxCharacterIndexNextLine){
					this.cursorCharacterX = maxCharacterIndexNextLine;
				}
			}else{
				//  Move focus to previous item:
				container.updateFocusedElement(-1);
			}
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceRightArrowKey){
			if(this.cursorCharacterX < this.text.get(cursorLineY).size()){
				this.cursorCharacterX++;
			}
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceDownArrowKey){
			if(this.cursorLineY < (this.text.size() - 1)){
				this.cursorLineY++;
				int currentCharacterIndex = this.cursorCharacterX;
				int maxCharacterIndexNextLine = this.text.get(this.cursorLineY).size();
				if(currentCharacterIndex > maxCharacterIndexNextLine){
					this.cursorCharacterX = maxCharacterIndexNextLine;
				}
			}else{
				//  Move focus to next item:
				container.updateFocusedElement(1);
			}
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceLeftArrowKey){
			if(this.cursorCharacterX > 0){
				this.cursorCharacterX--;
			}
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceRemoveKey){
			if(this.text.size() > 0){
				if(this.cursorCharacterX < this.text.get(cursorLineY).size()){
					//  Deleting character in the middle of a line somewhere:
					//  Just delete character at cursor:
					this.text.get(cursorLineY).remove(cursorCharacterX);
				}else{
					//  It not at end of very last line:
					if(cursorLineY < this.text.size() -1){
						//  Delete any newline that may exist after this line:
						List<String> newLine = new ArrayList<String>();
						newLine.addAll(this.text.get(cursorLineY));
						newLine.addAll(this.text.get(cursorLineY + 1));
						this.text.remove(cursorLineY); //  Remove current line
						this.text.remove(cursorLineY); //  Remove 'next' line.
						this.text.add(cursorLineY, newLine);
					}
				}
			}else{
				//  Do nothing
			}
		}else{
			logger.info("Discarding unknown ansi escape sequence of type: " + ansiEscapeSequence.getClass().getName());
		}
	}

	public void updateRenderableArea(Coordinate placementOffset, CuboidAddress visibleArea, boolean hasFocus) throws Exception{
		this.placementOffset = placementOffset;
		int width = (int)visibleArea.getWidth();
		int height = (int)visibleArea.getHeight();

		this.displayLayer = new ScreenLayer(this.displayLayer.getPlacementOffset(), ScreenLayer.makeDimensionsCA(0, 0, width, height));
		//  Initialize to an obvious pattern.  Should be overwritten by child class:
		this.displayLayer.initializeInRegion(1, "x", new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR, UserInterfaceFrameThreadState.BLUE_BG_COLOR}, null, new ScreenRegion(ScreenRegion.makeScreenRegionCA(0, 0, width, height)), true, true);
	}

	public void render(InputElementContainer container, ScreenLayer bottomLayer) throws Exception{
		//  Initialize to clear any backspaced characters:
		this.displayLayer.initializeInRegion(1, " ", UserInterfaceFrameThreadState.getTextInputAreaColors(), null, new ScreenRegion(ScreenRegion.makeScreenRegionCA(0, 0, this.displayLayer.getWidth(), this.displayLayer.getHeight())), true, true);

		for(int i = this.displayedColumnOffsetY; ((i - this.displayedColumnOffsetY) < this.displayLayer.getHeight()) && i < this.text.size(); i++){
			List<String> line = this.text.get(i);
			ColouredTextFragment tf = new ColouredTextFragment(String.join("", line), UserInterfaceFrameThreadState.getTextInputAreaColors());

			container.printTextAtScreenXY(tf, (long)-this.displayedColumnOffsetX, (long)(i - this.displayedColumnOffsetY), PrintDirection.LEFT_TO_RIGHT, this.displayLayer);
		}

		this.displayLayer.setPlacementOffset(this.placementOffset);
		bottomLayer.mergeDown(this.displayLayer, true, ScreenLayerMergeType.PREFER_BOTTOM_LAYER);
	}

	public boolean canCaptureFocus() throws Exception{
		return true;
	}
}
