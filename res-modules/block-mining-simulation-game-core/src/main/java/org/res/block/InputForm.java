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

public class InputForm implements InputFormContainer {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	//  Ordered list to cycle though with tab press:
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

	public void onCursorPositionChange(Coordinate c) throws Exception{
		this.container.onCursorPositionChange(c);
	}

	public void printTextAtScreenXY(ColouredTextFragment ctf, Long x, Long y, PrintDirection direction, ScreenLayer screenLayer) throws Exception{
		this.container.printTextAtScreenXY(ctf, x, y, direction, screenLayer);
	}

	public void addInputFormTextArea(String name, Long maxLines) throws Exception{
		InputFormTextArea tia = new InputFormTextArea(name, this.container, maxLines);
		formElementsList.add(tia);
		formElementsMap.put(tia.getName(), tia);
	}

	public void addInputFormLabel(String name, String text) throws Exception{
		InputFormLabel label = new InputFormLabel(name, this.container, text);
		formElementsList.add(label);
		formElementsMap.put(label.getName(), label);
	}

	public void addInputFormButton(String name, String text) throws Exception{
		InputFormButton button = new InputFormButton(name, this.container, text);
		formElementsList.add(button);
		formElementsMap.put(button.getName(), button);
	}

	public void setFocusedItem(String itemName) throws Exception{
		for(InputFormElement input : formElementsList){
			if(input.getName().equals(itemName)){
				this.focusedItemName = itemName;
			}
		}
	}

	public String setNextInputFocus() throws Exception{
		String newFocusItem = null;
		Integer focusedIndex = null;
		for(int i = 0; i < formElementsList.size(); i++){
			if(this.focusedItemName == null || this.focusedItemName.equals(formElementsList.get(i).getName())){
				focusedIndex = i;
				break;
			}
		}
		if(focusedIndex != null){
			//  Focus was on last input area:
			if((formElementsList.size() -1) == focusedIndex){ 
				this.focusedItemName = formElementsList.get(0).getName();
				newFocusItem = null;
			}else{
				// Advance focus to next input area:
				this.focusedItemName = formElementsList.get(focusedIndex + 1).getName();
				newFocusItem = this.formElementsMap.get(this.focusedItemName).getName();
			}
		}else{
			//  No input areas at all:
			newFocusItem = null;
		}

		this.setFocusedItem(this.focusedItemName);
		return newFocusItem;
	}

	public void onKeyboardCharacter(String character) throws Exception {
		if(this.focusedItemName != null){
			this.formElementsMap.get(this.focusedItemName).onKeyboardCharacter(this.container, character);
		}
	}

	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		if(this.focusedItemName != null){
			this.formElementsMap.get(this.focusedItemName).onAnsiEscapeSequence(this.container, ansiEscapeSequence);
		}
	}

	public void updateRenderableArea(String name, Coordinate placementOffset, CuboidAddress visibleArea) throws Exception{
		boolean hasFocus = this.focusedItemName != null && this.focusedItemName.equals(name);
		this.formElementsMap.get(name).updateRenderableArea(placementOffset, visibleArea, hasFocus);
		if(this.focusedItemName != null){
			this.formElementsMap.get(this.focusedItemName).sendCursorUpdate();
		}
	}

	public void render(ScreenLayer bottomLayer) throws Exception{
		for(InputFormElement area : this.formElementsList){
			area.render(this.container, bottomLayer);
		}
	}
}
