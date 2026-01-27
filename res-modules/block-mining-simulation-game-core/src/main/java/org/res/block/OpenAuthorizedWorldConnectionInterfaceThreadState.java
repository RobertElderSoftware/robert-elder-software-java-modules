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
import java.util.Objects;
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

public class OpenAuthorizedWorldConnectionInterfaceThreadState extends UserInterfaceFrameThreadState implements InputFormContainer{

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;

	private ClientBlockModelContext clientBlockModelContext;
	private InputForm textInputAreaCollection;
	private String initialFocus = "local_world_name";

	public OpenAuthorizedWorldConnectionInterfaceThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext, ConsoleWriterThreadState consoleWriterThreadState) throws Exception {
		super(blockManagerThreadCollection, consoleWriterThreadState, new int [] {ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT}, new ScreenLayerMergeType [] {ScreenLayerMergeType.PREFER_BOTTOM_LAYER});
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
	}

	public void onCursorPositionChange(Coordinate c){
		this.currentCursorPosition = c;
	}

	protected void init(Object o) throws Exception{
		this.textInputAreaCollection = new InputForm(this);
		this.textInputAreaCollection.addInputFormLabel("active_connection_index_label", "Enter Index of World:");
		this.textInputAreaCollection.addInputFormTextArea("active_connection_index", 1L);
		this.textInputAreaCollection.setInputFormTextAreaText("active_connection_index", "0");

		this.textInputAreaCollection.addInputFormLabel("authorized_connection_id_label", "Authorized Connection ID:");
		this.textInputAreaCollection.addInputFormTextArea("authorized_connection_id", 1L);
		this.textInputAreaCollection.setInputFormTextAreaText("authorized_connection_id", "1");

		this.textInputAreaCollection.addInputFormButton("submit_button", "Submit");

		this.textInputAreaCollection.setFocusedItem(initialFocus);
	}

	public void onDefaultKeyboardInput(String character) throws Exception {
		this.textInputAreaCollection.onKeyboardCharacter(character);
	}

	public void onKeyboardInput(String actionString) throws Exception {
		UserInteractionConfig ki = this.blockManagerThreadCollection.getUserInteractionConfig();
		UserInterfaceActionType action = ki.getKeyboardActionFromString(actionString);

		if(action == null){
			this.onDefaultKeyboardInput(actionString);
		}else{
			switch(action){
				case ACTION_TAB_NEXT_FRAME:{
					String focusBefore = this.textInputAreaCollection.getFocusedItemName();
					String focusAfter = this.textInputAreaCollection.tryChangeElementFocus(1);
					//  Form Focus didn't change, so give focus back to next frame.
					if(Objects.equals(focusBefore, focusAfter)){
						this.textInputAreaCollection.setFocusedItem(initialFocus);
						getConsoleWriterThreadState().putBlockingWorkItem(new FocusOnNextFrameWorkItem(getConsoleWriterThreadState()), WorkItemPriority.PRIORITY_LOW);
					}else{

					}
					break;
				}default:{
					this.onDefaultKeyboardInput(actionString);
				}
			}
		}
		this.onRenderFrame(false, false);
		this.onFinalizeFrame();
	}

	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		this.textInputAreaCollection.onAnsiEscapeSequence(ansiEscapeSequence);
		this.onRenderFrame(false, false);
		this.onFinalizeFrame();
	}

	public void onRenderFrame(boolean hasThisFrameDimensionsChanged, boolean hasOtherFrameDimensionsChanged) throws Exception{

		Long spaceWidth = getConsoleWriterThreadState().measureTextLengthOnTerminal(CharacterConstants.SPACE).getDeltaX();
		Long initialOffset = 2L;

		int [] titleAnsiCodes = UserInterfaceFrameThreadState.getHelpDetailsTitleColors();

		ColouredTextFragmentList activeWorldsTitlePart = new ColouredTextFragmentList();
		activeWorldsTitlePart.add(new ColouredTextFragment("Active World Connection:", titleAnsiCodes));

		List<LinePrintingInstruction> activeWorldsTitleInstructions = this.getLinePrintingInstructions(activeWorldsTitlePart, spaceWidth, spaceWidth, false, false, this.getInnerFrameWidth());

		//for(int i = 0; i < this.getBlockManagerThreadCollection().getWorldConnections().size(); i++){
			
		//}

		this.executeLinePrintingInstructionsAtYOffset(activeWorldsTitleInstructions, initialOffset);

		Long localTitleOffset = initialOffset + activeWorldsTitleInstructions.size();

		ColouredTextFragmentList localTitlePart = new ColouredTextFragmentList();
		localTitlePart.add(new ColouredTextFragment("Open Authorized World Connection:", titleAnsiCodes));


		List<LinePrintingInstruction> localTitleInstructions = this.getLinePrintingInstructions(localTitlePart, spaceWidth, spaceWidth, false, false, this.getInnerFrameWidth());

		this.executeLinePrintingInstructionsAtYOffset(localTitleInstructions, localTitleOffset);

		Long textAreaWidth = Math.max(0, this.getInnerFrameWidth() - 2L);
		Long activeConnectionIndexLabelOffset = localTitleOffset + localTitleInstructions.size() + 2L;
		Long activeConnectionIndexOffset = activeConnectionIndexLabelOffset + 1L;
		Long authorizedConnectionIdLabelOffset = activeConnectionIndexOffset + 2L;
		Long authorizedConnectionIdOffset = authorizedConnectionIdLabelOffset + 1L;
		Long submitButtonOffset = authorizedConnectionIdOffset + 2L;

		this.textInputAreaCollection.updateRenderableArea(
			"active_connection_index_label",
			new Coordinate(Arrays.asList(2L, activeConnectionIndexLabelOffset)),
			new CuboidAddress(
				new Coordinate(Arrays.asList(0L, 0L)),
				new Coordinate(Arrays.asList(textAreaWidth, 1L))
			)
		);

		this.textInputAreaCollection.updateRenderableArea(
			"active_connection_index",
			new Coordinate(Arrays.asList(2L, activeConnectionIndexOffset)),
			new CuboidAddress(
				new Coordinate(Arrays.asList(0L, 0L)),
				new Coordinate(Arrays.asList(textAreaWidth, 1L))
			)
		);

		this.textInputAreaCollection.updateRenderableArea(
			"authorized_connection_id_label",
			new Coordinate(Arrays.asList(2L, authorizedConnectionIdLabelOffset)),
			new CuboidAddress(
				new Coordinate(Arrays.asList(0L, 0L)),
				new Coordinate(Arrays.asList(textAreaWidth, 1L))
			)
		);

		this.textInputAreaCollection.updateRenderableArea(
			"authorized_connection_id",
			new Coordinate(Arrays.asList(2L, authorizedConnectionIdOffset)),
			new CuboidAddress(
				new Coordinate(Arrays.asList(0L, 0L)),
				new Coordinate(Arrays.asList(textAreaWidth, 1L))
			)
		);

		this.textInputAreaCollection.updateRenderableArea(
			"submit_button",
			new Coordinate(Arrays.asList(2L, submitButtonOffset)),
			new CuboidAddress(
				new Coordinate(Arrays.asList(0L, 0L)),
				new Coordinate(Arrays.asList(textAreaWidth, 3L))
			)
		);

		this.textInputAreaCollection.render(this.bufferedScreenLayers[ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT]);
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

	public void onButtonPress(String buttonName) throws Exception{
		if(buttonName.equals("close_frame_button")){
			this.onCloseCurrentFrame();
		}
	}
}
