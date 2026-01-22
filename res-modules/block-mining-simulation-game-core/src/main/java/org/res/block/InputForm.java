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
import java.util.HashMap;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

import org.res.block.WorkItem;
import org.res.block.BlockSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class InputForm implements InputElementContainer {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	//  For iterating through tab presses:
	private List<InputFormElement> focusableFormElementsList = new ArrayList<InputFormElement>();
	//  Ordered list of items:
	private List<InputFormElement> formElementsList = new ArrayList<InputFormElement>();
	//  Map for O(1) access method:
	private Map<String, InputFormElement> formElementsMap = new HashMap<String, InputFormElement>();
	private String focusedItemName = null; //  The name of whichever input area has focus.  null = no focus.
	private InputFormContainer container;

	public InputForm(InputFormContainer container) throws Exception{
		this.container = container;
	}

	public ConsoleWriterThreadState getConsoleWriterThreadState(){
		return this.container.getConsoleWriterThreadState();
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.container.getBlockManagerThreadCollection();
	}

	public void onCursorPositionChange(Coordinate c) throws Exception{
		this.container.onCursorPositionChange(c);
	}

	public void printTextAtScreenXY(ColouredTextFragment ctf, Long x, Long y, PrintDirection direction, ScreenLayer screenLayer) throws Exception{
		this.container.printTextAtScreenXY(ctf, x, y, direction, screenLayer);
	}

	public void addFormElement(String name, InputFormElement element) throws Exception{
		formElementsList.add(element);
		formElementsMap.put(name, element);
		if(element.canCaptureFocus()){
			focusableFormElementsList.add(element);
		}
	}

	public void addInputFormTextArea(String name, Long maxLines) throws Exception{
		this.addFormElement(name, new InputFormTextArea(name, this, maxLines));
	}

	public void addInputFormLabel(String name, String text) throws Exception{
		this.addFormElement(name, new InputFormLabel(name, this, text));
	}

	public void addInputFormButton(String name, String text) throws Exception{
		this.addFormElement(name, new InputFormButton(name, this, text));
	}

	public void setFocusedItem(String itemName) throws Exception{
		for(InputFormElement input : formElementsList){
			if(input.getName().equals(itemName)){
				this.focusedItemName = itemName;
			}
		}
	}

	public Integer getCurrentFocusIndex(){
		for(int i = 0; i < focusableFormElementsList.size(); i++){
			if(this.focusedItemName == null || this.focusedItemName.equals(focusableFormElementsList.get(i).getName())){
				return i;
			}
		}
		return null;
	}

	public String getFocusedItemName() throws Exception{
		return this.focusedItemName;
	}

	public String tryChangeElementFocus(int indexAdjustment) throws Exception{
		Integer focusedIndex = this.getCurrentFocusIndex();

		if(focusedIndex == null){
			//  No input areas at all:
			return null;
		}else{
			Integer newFocusIndex = focusedIndex + indexAdjustment;
			//  Past the end of the list:
			if(newFocusIndex < 0){
				//  Do nothing:  Proposed focus goes beyond start of form.
			}else if((focusableFormElementsList.size() -1) < newFocusIndex){
				//  Do nothing:  Proposed focus goes beyond end of form.
			}else{
				// Advance focus to next input area:
				this.focusedItemName = focusableFormElementsList.get(newFocusIndex).getName();
			}
			this.refreshCursor();
			return this.focusedItemName;
		}
	}

	public void onKeyboardCharacter(String character) throws Exception {
		if(this.focusedItemName != null){
			this.formElementsMap.get(this.focusedItemName).onKeyboardCharacter(this, character);
		}
		this.refreshCursor();
	}

	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		if(this.focusedItemName != null){
			this.formElementsMap.get(this.focusedItemName).onAnsiEscapeSequence(this, ansiEscapeSequence);
		}
		this.refreshCursor();
	}

	public void updateRenderableArea(String name, Coordinate placementOffset, CuboidAddress visibleArea) throws Exception{
		boolean hasFocus = this.focusedItemName != null && this.focusedItemName.equals(name);
		this.formElementsMap.get(name).updateRenderableArea(placementOffset, visibleArea, hasFocus);
		this.refreshCursor();
	}

	public void render(ScreenLayer bottomLayer) throws Exception{
		for(InputFormElement area : this.formElementsList){
			area.render(this, bottomLayer);
		}
	}

	public void updateFocusedElement(int adjustment) throws Exception{
		this.tryChangeElementFocus(adjustment);
	}

	public void refreshCursor() throws Exception{
		if(this.focusedItemName != null){
			this.formElementsMap.get(this.focusedItemName).sendCursorUpdate();
		}
	}

	public void onButtonPress(String buttonName) throws Exception{
		this.container.onButtonPress(buttonName);
	}

	public void setInputFormTextAreaText(String name, String text) throws Exception{
		((InputFormTextArea)this.formElementsMap.get(name)).setText(this, text);
	}
}
