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

public class InputFormLabel extends InputFormElement {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private String text;
	protected ScreenLayer displayLayer = new ScreenLayer();

	public InputFormLabel(String name, InputFormContainer container, String text) throws Exception{
		super(name, container);
		this.text = text;
	}

	public String getName(){
		return this.name;
	}

	public void onKeyboardCharacter(InputFormContainer container, String character) throws Exception {
	}

	public void sendCursorUpdate() throws Exception{
	}

	public void onAnsiEscapeSequence(InputFormContainer container, AnsiEscapeSequence ansiEscapeSequence) throws Exception{
	}

	public void updateRenderableArea(Coordinate placementOffset, CuboidAddress visibleArea, boolean hasFocus) throws Exception{
		this.placementOffset = placementOffset;
		int width = (int)visibleArea.getWidth();
		int height = (int)visibleArea.getHeight();

		this.displayLayer = new ScreenLayer(this.displayLayer.getPlacementOffset(), ScreenLayer.makeDimensionsCA(0, 0, width, height));
		//  Initialize to an obvious pattern.  Should be overwritten by child class:
		this.displayLayer.initializeInRegion(1, "x", new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR, UserInterfaceFrameThreadState.BLUE_BG_COLOR}, null, new ScreenRegion(ScreenRegion.makeScreenRegionCA(0, 0, width, height)), true, true);
	}

	public void render(InputFormContainer container, ScreenLayer bottomLayer) throws Exception{
		//  Initialize to clear any backspaced characters:
		this.displayLayer.initializeInRegion(1, " ", UserInterfaceFrameThreadState.getDefaultTextColors(), null, new ScreenRegion(ScreenRegion.makeScreenRegionCA(0, 0, this.displayLayer.getWidth(), this.displayLayer.getHeight())), true, true);

		ColouredTextFragment tf = new ColouredTextFragment(this.text, UserInterfaceFrameThreadState.getDefaultTextColors());

		container.printTextAtScreenXY(tf, 0L, 0L, PrintDirection.LEFT_TO_RIGHT, this.displayLayer);

		this.displayLayer.setPlacementOffset(this.placementOffset);
		bottomLayer.mergeDown(this.displayLayer, true, ScreenLayerMergeType.PREFER_BOTTOM_LAYER);
	}
}
