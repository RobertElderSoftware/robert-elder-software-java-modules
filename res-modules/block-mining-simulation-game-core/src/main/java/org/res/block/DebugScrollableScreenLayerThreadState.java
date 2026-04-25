//  Copyright (c) 2026 Robert Elder Software Inc.
//   
//  Robert Elder Software Proprietary License (Version 2026-04-09)
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
//  either unmodified, modified, or incorporated into another software product, 
//  except as described in the document "REDISTRIBUTION.md" (a file with SHA256 
//  hash value 'c39a6c8200a22caf30eac97095b78def80c9cab1b6f7ddd3fca7fdae71df43da').
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

public class DebugScrollableScreenLayerThreadState extends UserInterfaceFrameThreadState {

	public static String DISPLAY_TITLE = "Debug Scollable Screen Layer";
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;

	private DebugScrollableScreenLayer debugScrollableScreenLayer;
	private boolean isChangingLayer = true;   //  Are we moving the layer or scrolling the layer?

	public DebugScrollableScreenLayerThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ConsoleWriterThreadState consoleWriterThreadState) throws Exception {
		super(blockManagerThreadCollection, consoleWriterThreadState, new int [] {ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT}, new ScreenLayerMergeType [] {ScreenLayerMergeType.PREFER_BOTTOM_LAYER});
		this.blockManagerThreadCollection = blockManagerThreadCollection;
	}

	protected void init(Object o) throws Exception{
		this.debugScrollableScreenLayer = new DebugScrollableScreenLayer(blockManagerThreadCollection);
		this.debugScrollableScreenLayer.setVisibleWidth(12L);
		this.debugScrollableScreenLayer.setVisibleHeight(14L);

		this.debugScrollableScreenLayer.setContentColumnHeight(50L);
		this.debugScrollableScreenLayer.setContentColumnWidth(100L);
	}

	public void updateVisibleWidth(Long newWidth) throws Exception{
		this.debugScrollableScreenLayer.setVisibleWidth(newWidth);
		this.render();
		this.onFinalizeFrame();
	}

	public void updateVisibleHeight(Long newHeight) throws Exception{
		this.debugScrollableScreenLayer.setVisibleHeight(newHeight);
		this.render();
		this.onFinalizeFrame();
	}

	public void onKeyboardInput(String actionString) throws Exception {
		logger.info("DebugScrollableScreenLayerThreadState keyboard input: " + actionString);
		UserInteractionConfig ki = this.blockManagerThreadCollection.getUserInteractionConfig();
		UserInterfaceActionType action = ki.getKeyboardActionFromString(actionString);

		if(actionString.equals("W")){
			if(isChangingLayer){
				this.updateVisibleWidth(this.debugScrollableScreenLayer.getWidth() + 1L);
			}else{
				this.debugScrollableScreenLayer.setContentColumnWidth(this.debugScrollableScreenLayer.getContentColumnWidth() + 1);
				this.render();
				this.onFinalizeFrame();
			}
		}else if(actionString.equals("w")){
			if(isChangingLayer){
				this.updateVisibleWidth(this.debugScrollableScreenLayer.getWidth() -1L);
			}else{
				this.debugScrollableScreenLayer.setContentColumnWidth(this.debugScrollableScreenLayer.getContentColumnWidth() - 1);
				this.render();
				this.onFinalizeFrame();
			}
		}else if(actionString.equals("H")){
			if(isChangingLayer){
				this.updateVisibleHeight(this.debugScrollableScreenLayer.getHeight() + 1L);
			}else{
				this.debugScrollableScreenLayer.setContentColumnHeight(this.debugScrollableScreenLayer.getContentColumnHeight() + 1);
				this.render();
				this.onFinalizeFrame();
			}
		}else if(actionString.equals("h")){
			if(isChangingLayer){
				this.updateVisibleHeight(this.debugScrollableScreenLayer.getHeight() -1L);
			}else{
				this.debugScrollableScreenLayer.setContentColumnHeight(this.debugScrollableScreenLayer.getContentColumnHeight() - 1);
				this.render();
				this.onFinalizeFrame();
			}
		}else if(actionString.equals("m")){
			this.isChangingLayer = !this.isChangingLayer;
			this.render();
			this.onFinalizeFrame();
		}else{
			logger.info("Discarding " + actionString);
		}

		if(action == null){

		}else{
			switch(action){
				case ACTION_QUIT:{
					//  Open help menu
					getConsoleWriterThreadState().putBlockingWorkItem(new OpenHelpMenuWorkItem(getConsoleWriterThreadState()), WorkItemPriority.PRIORITY_LOW);
					break;
				}case ACTION_TAB_NEXT_FRAME:{
					getConsoleWriterThreadState().putBlockingWorkItem(new FocusOnNextFrameWorkItem(getConsoleWriterThreadState()), WorkItemPriority.PRIORITY_LOW);
					break;
				}default:{
					logger.info("Discarding Unexpected action=" + action.toString());
				}
			}
		}
	}

	public void moveLayer(Long deltaX, Long deltaY) throws Exception{
		Coordinate oldPlacement = this.debugScrollableScreenLayer.getPlacementOffset();
		this.debugScrollableScreenLayer.setPlacementOffset(oldPlacement.changeByDeltaXY(deltaX, deltaY));
		this.render();
		this.onFinalizeFrame();
	}

	public void onUp() throws Exception{
		if(isChangingLayer){
			this.moveLayer(0L, -1L);
		}else{
			this.debugScrollableScreenLayer.setScrollColumnOffsetY(this.debugScrollableScreenLayer.getScrollColumnOffsetY() -1L);
			this.render();
			this.onFinalizeFrame();
		}
	}

	public void onRight()throws Exception{
		if(isChangingLayer){
			this.moveLayer(1L, 0L);
		}else{
			this.debugScrollableScreenLayer.setScrollColumnOffsetX(this.debugScrollableScreenLayer.getScrollColumnOffsetX() +1L);
			this.render();
			this.onFinalizeFrame();
		}
	}

	public void onDown()throws Exception{
		if(isChangingLayer){
			this.moveLayer(0L, 1L);
		}else{
			this.debugScrollableScreenLayer.setScrollColumnOffsetY(this.debugScrollableScreenLayer.getScrollColumnOffsetY() +1L);
			this.render();
			this.onFinalizeFrame();
		}
	}

	public void onLeft()throws Exception{
		if(isChangingLayer){
			this.moveLayer(-1L, 0L);
		}else{
			this.debugScrollableScreenLayer.setScrollColumnOffsetX(this.debugScrollableScreenLayer.getScrollColumnOffsetX() -1L);
			this.render();
			this.onFinalizeFrame();
		}
	}

	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		if(ansiEscapeSequence instanceof AnsiEscapeSequenceUpArrowKey){
			this.onUp();
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceRightArrowKey){
			this.onRight();
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceDownArrowKey){
			this.onDown();
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceLeftArrowKey){
			this.onLeft();
		}else{
			logger.info("DebugScrollableScreenLayerThreadState, discarding unknown ansi escape sequence of type: " + ansiEscapeSequence.getClass().getName());
		}
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public void onRenderFrame(boolean hasThisFrameDimensionsChanged, boolean hasOtherFrameDimensionsChanged) throws Exception{
		this.render();
	}

	public void render() throws Exception{
		this.clearFrame();
		this.drawBorders();
		Coordinate placement = this.debugScrollableScreenLayer.getPlacementOffset();
		List<String> theText = new ArrayList<String>();
		theText.add("placement.getX()=" + placement.getX() + ",  this.placement.getY()=" + placement.getY());
		theText.add("");
		theText.add("visibleWidth=" + this.debugScrollableScreenLayer.getWidth() + ",  visibleHeight)=" + this.debugScrollableScreenLayer.getHeight());
		theText.add("");
		theText.add("contentWidth=" + this.debugScrollableScreenLayer.getContentColumnWidth() + ",  contentHeight)=" + this.debugScrollableScreenLayer.getContentColumnHeight());
		theText.add("");
		theText.add("this.isChangingLayer=" + this.isChangingLayer + ",  (Press 'm' to toggle");
		theText.add("between moving the layer and scrolling within it.)");
		theText.add("");
		theText.add("This is the debug frame for scrollable screen layers.");
		theText.add("");
		theText.add("Use arrows keys or wasd to move layer around.");
		theText.add("");
		theText.add("Use 'W' and 'w' to increase/decrease visible width.");
		theText.add("");
		theText.add("Use 'H' and 'h' to increase/decrease visible height.");

		for(int i = 0; i < theText.size(); i++){
			boolean xOverflow = this.getFrameWidth() > theText.get(i).length();
			Long xOffset = xOverflow ? ((this.getFrameWidth() - theText.get(i).length()) / 2L) : 0L;
			Long yOffset = (this.getFrameHeight() / 2L) - (theText.size() / 2) + i;
			this.printTextAtScreenXY(new ColouredTextFragment(theText.get(i), UserInterfaceFrameThreadState.getDefaultTextColors()), xOffset, yOffset, PrintDirection.LEFT_TO_RIGHT);
		}

		this.debugScrollableScreenLayer.render(this.bufferedScreenLayers[ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT]);
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

	public void onUIEventNotification(Object o, UINotificationType notificationType)throws Exception{
		switch(notificationType){
			default:{
				throw new Exception("Unknown event notification type: " + notificationType);
			}
		}
	}

	public void destroy(Object o) throws Exception{
	}
}
