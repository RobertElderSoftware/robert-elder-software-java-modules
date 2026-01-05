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

public class DebugInputInterfaceThreadState extends UserInterfaceFrameThreadState implements TextInputContainer{

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;

	private ClientBlockModelContext clientBlockModelContext;
	private TextInputArea textInputArea;

	public DebugInputInterfaceThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext, ConsoleWriterThreadState consoleWriterThreadState) throws Exception {
		super(blockManagerThreadCollection, consoleWriterThreadState, new int [] {ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT}, new ScreenLayerMergeType [] {ScreenLayerMergeType.PREFER_BOTTOM_LAYER});
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
	}

	public void onCursorPositionChange(Coordinate c){
		this.currentCursorPosition = c;
	}

	protected void init(Object o) throws Exception{
		Long maxColumns = 200L;
		Long maxLines = 1L;
		this.textInputArea = new TextInputArea(this);
		this.textInputArea.updateRenderableArea(
			new Coordinate(Arrays.asList(5L, 5L)),
			new CuboidAddress(
				new Coordinate(Arrays.asList(0L, 0L)),
				new Coordinate(Arrays.asList(20L, 4L))
			),
			maxColumns,
			maxLines
		);
	}

	public void onDefaultKeyboardInput(String actionString) throws Exception {
		this.textInputArea.onKeyboardCharacter(this, actionString);
		this.onRenderFrame(false, false);
		this.onFinalizeFrame();
	}

	public void onKeyboardInput(String actionString) throws Exception {
		UserInteractionConfig ki = this.blockManagerThreadCollection.getUserInteractionConfig();
		UserInterfaceActionType action = ki.getKeyboardActionFromString(actionString);

		if(action == null){
			this.onDefaultKeyboardInput(actionString);
		}else{
			switch(action){
				case ACTION_TAB_NEXT_FRAME:{
					getConsoleWriterThreadState().putBlockingWorkItem(new FocusOnNextFrameWorkItem(getConsoleWriterThreadState()), WorkItemPriority.PRIORITY_LOW);
					break;
				}default:{
					this.onDefaultKeyboardInput(actionString);
				}
			}
		}
	}

	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		this.textInputArea.onAnsiEscapeSequence(this, ansiEscapeSequence);
		this.onRenderFrame(false, false);
		this.onFinalizeFrame();
	}

	public void onRenderFrame(boolean hasThisFrameDimensionsChanged, boolean hasOtherFrameDimensionsChanged) throws Exception{
		this.textInputArea.render(this, this.bufferedScreenLayers[ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT]);
		this.drawBorders();
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
