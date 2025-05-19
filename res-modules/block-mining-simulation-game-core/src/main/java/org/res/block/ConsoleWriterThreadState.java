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

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;
import java.io.ByteArrayOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Collections;
import java.util.Comparator;

import java.lang.Thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class ConsoleWriterThreadState extends WorkItemQueueOwner<ConsoleWriterWorkItem> {
	protected Object lock = new Object();
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;
	private ClientBlockModelContext clientBlockModelContext;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private TextWidthMeasurementWorkItem currentTextWidthMeasurement = null;
	private final List<Long> mapAreaInterfaceFrameIds = new ArrayList<Long>();
	private final List<Long> inventoryInterfaceFrameIds = new ArrayList<Long>();

	private BlockingQueue<TextWidthMeasurementWorkItem> pendingTextWidthRequests = new LinkedBlockingDeque<TextWidthMeasurementWorkItem>();
	private BlockingQueue<ConsoleQueueableWorkItem> pendingQueueableWorkItems = new LinkedBlockingDeque<ConsoleQueueableWorkItem>();
	private Map<Long, UserInterfaceFrameThreadState> activeFrameStates = new HashMap<Long, UserInterfaceFrameThreadState>();
	private Map<Long, WorkItemProcessorTask<UIWorkItem>> activeFrameThreads = new HashMap<Long, WorkItemProcessorTask<UIWorkItem>>();

	private Long terminalWidth = null;
	private Long terminalHeight = null;
	private FrameDimensions currentTerminalFrameDimensions = null;
	private HelpMenuFrameThreadState helpMenuFrameThreadState = null;
	private int numScreenOutputBuffers = 2;
	public static final int BUFFER_INDEX_DEFAULT = 0;
	public static final int BUFFER_INDEX_MENU = 1;
	private ScreenOutputBuffer [] screenOutputBuffer = new ScreenOutputBuffer [this.numScreenOutputBuffers];

	public Long focusedFrameId = null;

	private Map<Long, UserInterfaceSplit> userInterfaceSplits = new HashMap<Long, UserInterfaceSplit>();
	private Long rootSplitId;

	private int [] lastUsedColourCodes = new int [] {UserInterfaceFrameThreadState.RESET_BG_COLOR};

	public ConsoleWriterThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception{
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
	}

	public void init() throws Exception {
		this.helpMenuFrameThreadState = new HelpMenuFrameThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext);
		this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<UIWorkItem>(this.helpMenuFrameThreadState, UIWorkItem.class));

		for(int i = 0; i < this.numScreenOutputBuffers; i++){
			this.screenOutputBuffer[i]  = new ScreenOutputBuffer();
		}
		this.initializeConsole(80L, 24L); // Early setup is necessary so we can do text width calculations before drawing frame

		int numMapAreas = 1;
		int numInventoryAreas = 1;
		for(int i = 0; i < numMapAreas; i++){
			this.mapAreaInterfaceFrameIds.add(this.createFrameAndThread(MapAreaInterfaceThreadState.class));
		}
		for(int i = 0; i < numInventoryAreas; i++){
			this.inventoryInterfaceFrameIds.add(this.createFrameAndThread(InventoryInterfaceThreadState.class));
		}

		boolean useMultiSplitDemo = false;
		if(useMultiSplitDemo){
			List<Long> splits1 = new ArrayList<Long>();
			for(Long mapAreaInterfaceFrameId : this.mapAreaInterfaceFrameIds){
				splits1.add(makeLeafNodeSplit(mapAreaInterfaceFrameId));
			}
			for(Long inventoryInterfaceFrameId : this.inventoryInterfaceFrameIds){
				splits1.add(makeLeafNodeSplit(inventoryInterfaceFrameId));
			}
			splits1.add(makeLeafNodeSplit(createFrameAndThread(EmptyFrameThreadState.class)));

			List<Long> splits2 = new ArrayList<Long>();
			splits2.add(makeLeafNodeSplit(createFrameAndThread(EmptyFrameThreadState.class)));
			splits2.add(makeLeafNodeSplit(createFrameAndThread(EmptyFrameThreadState.class)));
			splits2.add(makeLeafNodeSplit(createFrameAndThread(EmptyFrameThreadState.class)));

			List<Long> splits3 = new ArrayList<Long>();
			splits3.add(makeLeafNodeSplit(createFrameAndThread(EmptyFrameThreadState.class)));
			splits3.add(makeLeafNodeSplit(createFrameAndThread(EmptyFrameThreadState.class)));
			splits3.add(makeLeafNodeSplit(createFrameAndThread(EmptyFrameThreadState.class)));

			List<Long> topSplits = new ArrayList<Long>();
			Long h1 = makeHorizontalSplit();
			this.addSplitPartsByIds(h1, splits1);
			topSplits.add(h1);

			Long h2 = makeHorizontalSplit();
			this.addSplitPartsByIds(h2, splits2);
			topSplits.add(h2);

			Long h3 = makeHorizontalSplit();
			this.addSplitPartsByIds(h3, splits3);
			topSplits.add(h3);

			Long top = makeVerticalSplit();
			this.addSplitPartsByIds(top, topSplits);
			this.setRootSplit(top);
		}else{
			List<Double> framePercents = new ArrayList<Double>();
			List<Long> splits = new ArrayList<Long>();
			for(Long mapAreaInterfaceFrameId : this.mapAreaInterfaceFrameIds){
				splits.add(makeLeafNodeSplit(mapAreaInterfaceFrameId));
				framePercents.add(0.75 / this.mapAreaInterfaceFrameIds.size());
			}
			for(Long inventoryInterfaceFrameId : this.inventoryInterfaceFrameIds){
				splits.add(makeLeafNodeSplit(inventoryInterfaceFrameId));
				framePercents.add(0.25 / this.inventoryInterfaceFrameIds.size());
			}
			Long r = makeHorizontalSplit();
			this.addSplitPartsByIds(r, splits);
			((UserInterfaceSplitHorizontal)this.getUserInterfaceSplitById(r)).setSplitPercentages(framePercents);
			this.setRootSplit(r);
		}
	}

	public void addSplitPartsByIds(Long parentSplitId, List<Long> childrenToAdd) throws Exception{
		UserInterfaceSplit split = getUserInterfaceSplitById(parentSplitId);
		if(split instanceof UserInterfaceSplitMulti){
			UserInterfaceSplitMulti multi = (UserInterfaceSplitMulti)split;
			for(Long id : childrenToAdd){
				multi.addPart(getUserInterfaceSplitById(id));
			}
		}else if(split instanceof UserInterfaceSplitLeafNode){
			throw new Exception("Cannot add user interface splits to a split leaf node.");
		}else{
			throw new Exception("Unknown split type.");
		}
	}

	public Long makeHorizontalSplit() throws Exception{
		UserInterfaceSplitHorizontal h = new UserInterfaceSplitHorizontal();
		if(userInterfaceSplits.containsKey(h.getSplitId())){
			throw new Exception("Error, duplicate split id for horizontal split node: " + h.getSplitId());
		}else{
			userInterfaceSplits.put(h.getSplitId(), h);
		}
		return h.getSplitId();
	}

	public Long makeVerticalSplit() throws Exception{
		UserInterfaceSplitVertical v = new UserInterfaceSplitVertical();
		if(userInterfaceSplits.containsKey(v.getSplitId())){
			throw new Exception("Error, duplicate split id for vertical split node: " + v.getSplitId());
		}else{
			userInterfaceSplits.put(v.getSplitId(), v);
		}
		return v.getSplitId();
	}

	public Long makeLeafNodeSplit(Long frameId) throws Exception{
		UserInterfaceSplitLeafNode leaf = new UserInterfaceSplitLeafNode(this.getFrameStateById(frameId));
		if(userInterfaceSplits.containsKey(leaf.getSplitId())){
			throw new Exception("Error, duplicate split id for leaf node frame id: " + frameId);
		}else{
			userInterfaceSplits.put(leaf.getSplitId(), leaf);
		}
		return leaf.getSplitId();
	}

	public UserInterfaceSplit getUserInterfaceSplitById(Long splitId) throws Exception{
		if(userInterfaceSplits.containsKey(splitId)){
			return userInterfaceSplits.get(splitId);
		}else{
			throw new Exception("Error, split id not found: " + splitId);
		}
	}

	public Long createFrameThread(Long frameId, Class<?> frameStateClass) throws Exception{
		if(
			frameStateClass == HelpDetailsFrameThreadState.class ||
			frameStateClass == EmptyFrameThreadState.class ||
			frameStateClass == MapAreaInterfaceThreadState.class ||
			frameStateClass == InventoryInterfaceThreadState.class
		){
			UserInterfaceFrameThreadState frame = this.getFrameStateById(frameId);

			WorkItemProcessorTask<UIWorkItem> thread = new WorkItemProcessorTask<UIWorkItem>(frame, UIWorkItem.class);
			this.blockManagerThreadCollection.addThread(thread);

			if(activeFrameThreads.containsKey(frameId)){
				throw new Exception("Error, duplicate frame id for thread: " + frameId);
			}else{
				activeFrameThreads.put(frameId, thread);
			}

			return frameId;
		}else{
			throw new Exception("Unknown frame state type " + frameStateClass.getName());
		}
	}

	public void destroyFrameThreadById(Long frameId) throws Exception{
		if(activeFrameThreads.containsKey(frameId)){
			this.getFrameThreadById(frameId).setIsThreadFinished(true);
			this.getFrameThreadById(frameId).interrupt();
			this.blockManagerThreadCollection.removeThread(this.getFrameThreadById(frameId));
			activeFrameThreads.remove(frameId);
		}else{
			throw new Exception("destroyFrameThreadById: Frame id for thread: " + frameId);
		}
	}

	public WorkItemProcessorTask<UIWorkItem> getFrameThreadById(Long frameId) throws Exception{
		if(activeFrameThreads.containsKey(frameId)){
			return activeFrameThreads.get(frameId);
		}else{
			throw new Exception("Unknown frame thread id: " + frameId);
		}
	}

	public Long addFrameState(UserInterfaceFrameThreadState state) throws Exception{
		Long frameId = state.getFrameId();
		if(activeFrameStates.containsKey(frameId)){
			throw new Exception("Error, duplicate frame id for state: " + frameId);
		}else{
			activeFrameStates.put(frameId, state);
		}
		return frameId;
	}

	public Long createFrameState(Class<?> frameStateClass) throws Exception{
		if(frameStateClass == HelpDetailsFrameThreadState.class){
			return addFrameState(
				new HelpDetailsFrameThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext)
			);
		}else if(frameStateClass == EmptyFrameThreadState.class){
			return addFrameState(
				new EmptyFrameThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext)
			);
		}else if(frameStateClass == MapAreaInterfaceThreadState.class){
			return addFrameState(
				new MapAreaInterfaceThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext)
			);
		}else if(frameStateClass == InventoryInterfaceThreadState.class){
			return addFrameState(
				new InventoryInterfaceThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext)
			);
		}else{
			throw new Exception("Unknown frame state type " + frameStateClass.getName());
		}
	}

	public void destroyFrameStateById(Long frameId) throws Exception{
		if(activeFrameStates.containsKey(frameId)){
			activeFrameStates.remove(frameId);
		}else{
			throw new Exception("destroyFrameStateById: Frame id for thread: " + frameId);
		}
	}

	public void destroyFrameStateAndThreadById(Long frameId) throws Exception{
		this.destroyFrameThreadById(frameId);
		this.destroyFrameStateById(frameId);
	}

	public UserInterfaceFrameThreadState getFrameStateById(Long frameId) throws Exception{
		if(activeFrameStates.containsKey(frameId)){
			return activeFrameStates.get(frameId);
		}else{
			throw new Exception("Unknown frame state id: " + frameId);
		}
	}

	public Long createFrameAndThread(Class<?> frameStateClass) throws Exception{
		Long frameId = createFrameState(frameStateClass);
		UserInterfaceFrameThreadState frame = this.getFrameStateById(frameId);
		return this.createFrameThread(frameId, frameStateClass);
	}

	public void onCloseFrame(Long frameId) throws Exception{
		this.destroyFrameStateAndThreadById(frameId);
		//  If we're closing this frame, focus on another valid frame:
		if(frameId.equals(this.focusedFrameId)){
			this.focusedFrameId = null;
			focusOnNextFrame();
		}
	}

	public Long onSetRootSplitId(Long newRootSplitId) throws Exception{
		return this.setRootSplit(newRootSplitId);
	}

	public Long onGetRootSplitId() throws Exception{
		return this.getRootSplit();
	}

	public Long onCreateLeafNodeSplit(Long frameId) throws Exception{
		return this.makeLeafNodeSplit(frameId);
	}

	public Long onAddSplitPartsByIds(Long parentSplitId, List<Long> childSplitIds) throws Exception{
		this.addSplitPartsByIds(parentSplitId, childSplitIds);
		return parentSplitId;
	}

	public Long onCreateMultiSplit(Class<?> splitType) throws Exception{
		if(splitType == UserInterfaceSplitVertical.class){
			return this.makeVerticalSplit();
		}else if(splitType == UserInterfaceSplitHorizontal.class){
			return this.makeHorizontalSplit();
		}else{
			throw new Exception("Unknown split type " + splitType.getName());
		}
	}

	public Long onOpenFrame(Class<?> frameStateClass) throws Exception{
		return createFrameAndThread(frameStateClass);
	}

	public Long onSetFocusedFrame(Long frameIdToFocusOn) throws Exception{
		Long previouslyFocusedFrameId = this.focusedFrameId;
		if(activeFrameStates.containsKey(frameIdToFocusOn)){
			this.focusedFrameId = frameIdToFocusOn;
		}else{
			throw new Exception("Trying to set focused frame to a frame that does not exist: " + frameIdToFocusOn);
		}
		return previouslyFocusedFrameId;
	}

	public void onPlayerInventoryChange(PlayerInventory playerInventory) throws Exception{
		for(Long inventoryInterfaceFrameId : this.inventoryInterfaceFrameIds){
			InventoryInterfaceThreadState inv = (InventoryInterfaceThreadState)this.getFrameStateById(inventoryInterfaceFrameId);
			inv.putWorkItem(new PlayerInventoryChangeWorkItem(inv, new PlayerInventory(playerInventory.getBlockData())), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void updateMapAreaFlags(CuboidAddress areaToUpdate) throws Exception {
		for(Long mapAreaInterfaceFrameId : this.mapAreaInterfaceFrameIds){
			MapAreaInterfaceThreadState m = (MapAreaInterfaceThreadState)this.getFrameStateById(mapAreaInterfaceFrameId);
			m.putWorkItem(new UpdateMapAreaFlagsWorkItem(m, areaToUpdate.copy()), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void onPlayerPositionChange(Coordinate previousPosition, Coordinate newPosition) throws Exception{
		for(Long mapAreaInterfaceFrameId : this.mapAreaInterfaceFrameIds){
			MapAreaInterfaceThreadState m = (MapAreaInterfaceThreadState)this.getFrameStateById(mapAreaInterfaceFrameId);
			m.putWorkItem(new MapAreaNotifyPlayerPositionChangeWorkItem(m, previousPosition, newPosition), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void notifyAllFramesOfFocusChange() throws Exception{

		List<UserInterfaceFrameThreadState> allFrames = this.getUserInterfaceSplitById(rootSplitId).collectUserInterfaceFrames();
		//  Notify all frames of the update so they can redraw border:
		UserInterfaceFrameThreadState focusedFrame = this.focusedFrameId == null ? null : this.getFrameStateById(this.focusedFrameId);
		for(UserInterfaceFrameThreadState frame : allFrames){
			frame.putWorkItem(new FrameFocusChangeWorkItem(frame, focusedFrame.getFrameDimensions()), WorkItemPriority.PRIORITY_LOW);
		}

		this.helpMenuFrameThreadState.putWorkItem(new FrameFocusChangeWorkItem(this.helpMenuFrameThreadState, focusedFrame.getFrameDimensions()), WorkItemPriority.PRIORITY_LOW);
	}

	public void focusOnNextFrame() throws Exception{
		Long previousFrameId = this.focusedFrameId;
		String oldFrameInfo = this.focusedFrameId == null ? "null" : "frameId=" + this.focusedFrameId;
		List<UserInterfaceFrameThreadState> allFrames = this.getUserInterfaceSplitById(rootSplitId).collectUserInterfaceFrames();
		Collections.sort(allFrames, new Comparator<UserInterfaceFrameThreadState>() {
			@Override
			public int compare(UserInterfaceFrameThreadState a, UserInterfaceFrameThreadState b) {
				return (int)(a.getFrameId() - b.getFrameId());
			}
		});

		Long newFocusedFrameId;
		if(this.focusedFrameId == null){
			if(allFrames.size() > 0){
				newFocusedFrameId = allFrames.get(0).getFrameId();
			}else{
				throw new Exception("No frames to focus on!");
			}
		}else{
			int currentFrameIndex = -1;
			for(int i = 0; i < allFrames.size(); i++){
				if(allFrames.get(i).getFrameId() == this.focusedFrameId){
					if(currentFrameIndex != -1){
						throw new Exception("Two frames have same id?");
					}else{
						currentFrameIndex = i;
					}
				}
			}
			if(currentFrameIndex == -1){
				throw new Exception("Could not find frame index of currently focused frame?");
			}else{
				int newFrameIndex = (currentFrameIndex + 1) % allFrames.size();
				newFocusedFrameId = allFrames.get(newFrameIndex).getFrameId();
				logger.info("Focused on frame at index " + newFrameIndex + " with frameId=" + newFocusedFrameId);
			}
		}

		String newFrameInfo = newFocusedFrameId == null ? "null" : "frameId=" + newFocusedFrameId;
		logger.info("Switched from " + oldFrameInfo + " to " + newFrameInfo);
		this.focusedFrameId = newFocusedFrameId;
	}

	public Long getRootSplit(){
		return this.rootSplitId;
	}

	public final Long setRootSplit(Long newRootId) throws Exception{
		this.rootSplitId = newRootId;
		return this.rootSplitId;
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public void addPendingTextWidthRequest(TextWidthMeasurementWorkItem w){
		synchronized(lock){
			this.pendingTextWidthRequests.add(w);
		}
	}

	public void addPendingQueueableWorkItem(ConsoleQueueableWorkItem w){
		synchronized(lock){
			this.pendingQueueableWorkItems.add(w);
		}
	}

	public void onCursorPositionReport(CursorPositionReport cpr) throws Exception{
		synchronized(lock){
			if(this.currentTextWidthMeasurement == null){
				throw new Exception("ERROR:  Discarding cursor position report (" + cpr.getX() + "," + cpr.getY() + ") since there appears to be no active text width request?");
			}else{
				this.currentTextWidthMeasurement.notifyOfCurrentCursorPosition(cpr.getX(), cpr.getY());
			}
		}
	}

	public void sendAnsiEscapeSequenceToFrame(AnsiEscapeSequence ansiEscapeSequence, UserInterfaceFrameThreadState destinationFrame) throws Exception {
		if(destinationFrame != null){
			destinationFrame.putWorkItem(new ProcessFrameAnsiEscapeSequenceWorkItem(destinationFrame, ansiEscapeSequence), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		if(ansiEscapeSequence instanceof CursorPositionReport){
			CursorPositionReport cpr = (CursorPositionReport)ansiEscapeSequence;
			logger.info("Got Character Position Report: x=" + cpr.getX() + ", y=" + cpr.getY() + "");
			this.onCursorPositionReport(cpr);
		}else{
			logger.info("Got other type of ansi escape sequence, pass to the focused frame: " + ansiEscapeSequence.getClass().getName());
			if(this.helpMenuFrameThreadState.getIsMenuActive()){
				this.sendAnsiEscapeSequenceToFrame(ansiEscapeSequence, this.helpMenuFrameThreadState);
			}else{
				this.sendAnsiEscapeSequenceToFrame(ansiEscapeSequence, this.getFrameStateById(this.focusedFrameId));
			}
		}
	}

	public void prepareTerminalTextChange(int [][] newCharacterWidths, int [][][] newColourCodes, String [][] newCharacters, boolean [][] hasChange, int xOffset, int yOffset, int xChangeSize, int yChangeSize, FrameDimensions frameDimensions, int bufferIndex) throws Exception{
		for(int j = 0; j < yChangeSize; j++){
			for(int i = 0; i < xChangeSize; i++){
				if(hasChange[i][j]){
					int extraSpaceForLastCharacter = newCharacterWidths[i][j] < 1 ? 0 : (newCharacterWidths[i][j] -1);
					if(
						//  Don't write beyond current terminal dimenions
						//  Individual frames should be inside terminal area
						//  but terminal resize events means there could
						//  be a temporary inconsistency that makes this check
						//  necessary:
						(xOffset + i) < terminalWidth &&
						(yOffset + j) < terminalHeight &&
						(xOffset + i) >= 0 &&
						(yOffset + j) >= 0 &&
						//  Only allow a frame to write inside it's own borders:
						(xOffset + i) < (frameDimensions.getFrameOffsetX() + frameDimensions.getFrameWidth() - extraSpaceForLastCharacter) &&
						(yOffset + j) < (frameDimensions.getFrameOffsetY() + frameDimensions.getFrameHeight()) &&
						(xOffset + i) >= frameDimensions.getFrameOffsetX() &&
						(yOffset + j) >= frameDimensions.getFrameOffsetY()
					){
						//  If it's changing in this update, or there is a change that hasn't been printed yet.
						boolean hasChanged = !(
							this.screenOutputBuffer[bufferIndex].characterWidths[xOffset + i][yOffset + j] == newCharacterWidths[i][j] &&
							Arrays.equals(this.screenOutputBuffer[bufferIndex].colourCodes[xOffset + i][yOffset + j], newColourCodes[i][j]) &&
							this.screenOutputBuffer[bufferIndex].characters[xOffset + i][yOffset + j] == newCharacters[i][j]
						) || this.screenOutputBuffer[bufferIndex].changedFlags[xOffset + i][yOffset + j];
						this.screenOutputBuffer[bufferIndex].characterWidths[xOffset + i][yOffset + j] = newCharacterWidths[i][j];
						this.screenOutputBuffer[bufferIndex].colourCodes[xOffset + i][yOffset + j] = newColourCodes[i][j];
						this.screenOutputBuffer[bufferIndex].characters[xOffset + i][yOffset + j] = newCharacters[i][j];
						this.screenOutputBuffer[bufferIndex].changedFlags[xOffset + i][yOffset + j] = hasChanged;
						//  If we're printing on a screen buffer that's in front, we will evetually need to update whatever was behind it:
						if(hasChanged){
							for(int k = bufferIndex; k >= 0 ; k--){
								this.screenOutputBuffer[bufferIndex].changedFlags[xOffset + i][yOffset + j] = hasChanged;
							}
						}
					}else{
						//throw new Exception("Discarding character '" + newCharacters[i][j] + "' because if was out of bounds at x=" + (xOffset + i) + ", y=" + (yOffset + j));
					}
				}
			}
		}
	}

	public void printTerminalTextChanges(boolean resetCursorPosition) throws Exception{
		boolean useRightToLeftPrint = this.blockManagerThreadCollection.getRightToLeftPrint();
		int startColumn = useRightToLeftPrint ? this.terminalWidth.intValue() -1 : 0;
		int endColumn = useRightToLeftPrint ? -1 : this.terminalWidth.intValue();
		int loopUpdate = useRightToLeftPrint ? -1 : 1;
		boolean resetState = useRightToLeftPrint ? true : false;
		for(int j = 0; j < this.terminalHeight.intValue(); j++){
			boolean mustSetCursorPosition = true;
			boolean mustSetColourCodes = true;
			for(int i = startColumn; i != endColumn; i += loopUpdate){
				int bufferIndex = numScreenOutputBuffers -1;
				while(!this.screenOutputBuffer[bufferIndex].changedFlags[i][j] && bufferIndex > 0){
					bufferIndex--;
				}
				//  Try to intelligently issue as few ANSI escape sequences as possible:
				if(!Arrays.equals(this.screenOutputBuffer[bufferIndex].colourCodes[i][j], lastUsedColourCodes)){
					mustSetColourCodes = true;
				}
				if(this.screenOutputBuffer[bufferIndex].changedFlags[i][j]){
					if(mustSetCursorPosition){
						String currentPositionSequence = "\033[" + (j+1) + ";" + (i+1) + "H";
						System.out.print(currentPositionSequence);
						mustSetCursorPosition = resetState;
					}
					if(mustSetColourCodes){
						List<String> codes = new ArrayList<String>();
						for(int c : this.screenOutputBuffer[bufferIndex].colourCodes[i][j]){
							codes.add(String.valueOf(c));
						}
						String currentColorSequence = "\033[0m\033[" + String.join(";", codes) + "m";
						System.out.print(currentColorSequence);
						mustSetColourCodes = resetState;
						lastUsedColourCodes = this.screenOutputBuffer[bufferIndex].colourCodes[i][j];
					}
					if(this.screenOutputBuffer[bufferIndex].characters[i][j] != null){
						System.out.print(this.screenOutputBuffer[bufferIndex].characters[i][j]);
					}
					if(bufferIndex == 0){ //  Always keep overlay layers printed on top
						this.screenOutputBuffer[bufferIndex].changedFlags[i][j] = false;
					}
				}else{
					mustSetCursorPosition = true;
				}
			}
		}
		if(resetCursorPosition){
			System.out.print("\033[0;0H"); //  Move cursor to 0,0 after every print.
		}
	}

	public final void initializeConsole(Long terminalWidth, Long terminalHeight) throws Exception{
		this.terminalWidth = terminalWidth;
		this.terminalHeight = terminalHeight;
		for(int i = 0; i < this.numScreenOutputBuffers; i++){
			this.screenOutputBuffer[i].initialize(this.terminalWidth, this.terminalHeight);
		}
	}

	public void onTerminalDimensionsChanged(Long terminalWidth, Long terminalHeight, Long frameCharacterWidth) throws Exception{
		this.initializeConsole(terminalWidth, terminalHeight);

		if(this.focusedFrameId == null){
			this.focusOnNextFrame();
		}
		//  When the terminal size changes, send a notify to all of the user interface frames to let them know about it
		this.currentTerminalFrameDimensions = new FrameDimensions(frameCharacterWidth, terminalWidth, terminalHeight, 0L, 0L, terminalWidth, terminalHeight);
		FrameBordersDescription frameBordersDescription = this.getUserInterfaceSplitById(this.rootSplitId).collectAllConnectionPoints(this.currentTerminalFrameDimensions);
		this.getUserInterfaceSplitById(this.rootSplitId).setEquidistantFrameDimensions(this.currentTerminalFrameDimensions, frameBordersDescription);

		this.helpMenuFrameThreadState.putWorkItem(new FrameDimensionsChangeWorkItem(this.helpMenuFrameThreadState, this.currentTerminalFrameDimensions, frameBordersDescription), WorkItemPriority.PRIORITY_LOW);

		this.notifyAllFramesOfFocusChange();
	}

	public void sendKeyboardInputToFrame(byte [] bytesToSend, UserInterfaceFrameThreadState destinationFrame) throws Exception {
		if(destinationFrame != null){
			destinationFrame.putWorkItem(new ProcessFrameInputBytesWorkItem(destinationFrame, bytesToSend), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void onKeyboardInput(byte [] characters) throws Exception {
		logger.info("Saw keyboard input: " + new String(characters, "UTF-8"));
		if(this.helpMenuFrameThreadState.getIsMenuActive()){
			this.sendKeyboardInputToFrame(characters, this.helpMenuFrameThreadState);
		}else{
			UserInteractionConfig ki = this.blockManagerThreadCollection.getUserInteractionConfig();
			for(byte b : characters){
				String actionString = new String(new byte [] {b}, "UTF-8");
				UserInterfaceActionType action = ki.getKeyboardActionFromString(actionString);

				if(action == null){
					this.sendKeyboardInputToFrame(new byte [] {b}, this.getFrameStateById(this.focusedFrameId));
				}else{
					switch(action){
						case ACTION_QUIT:{
							logger.info("The 'q' key was pressed.  Exiting...");
							this.blockManagerThreadCollection.setIsProcessFinished(true, null); // Start shutting down the entire application.
							break;
						}case ACTION_HELP_MENU_TOGGLE:{
							this.helpMenuFrameThreadState.setIsMenuActive(!this.helpMenuFrameThreadState.getIsMenuActive());
							this.notifyAllFramesOfFocusChange();
							break;
						}case ACTION_TAB_NEXT_FRAME:{
							this.focusOnNextFrame();
							this.notifyAllFramesOfFocusChange();
							break;
						}default:{
							this.sendKeyboardInputToFrame(new byte [] {b}, this.getFrameStateById(this.focusedFrameId));
						}
					}
				}
			}
		}
	}

	public ConsoleWriterWorkItem takeWorkItem() throws Exception {
		ConsoleWriterWorkItem w = this.workItemQueue.takeWorkItem();
		return w;
	}

	public void putWorkItem(ConsoleWriterWorkItem workItem, WorkItemPriority priority) throws Exception{
		this.workItemQueue.putWorkItem(workItem, priority);
	}

	public void setScreenAreaChangeStates(int startX, int startY, int endX, int endY, int bufferIndex, boolean state){
		//  Invalidate a sub-area of screen so that the characters are that location will 
		//  be printed on the next print attempt.
		int width = endX - startX;
		int height = endY - startY;
		
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.screenOutputBuffer[bufferIndex].changedFlags[i + startX][j + startY] = state;
			}
		}
	}

	public boolean doBackgroundProcessing() throws Exception{
		synchronized(lock){
			//  If there is a character width measurement request available, start working on it:
			if(currentTextWidthMeasurement == null && this.pendingTextWidthRequests.size() > 0){
				this.currentTextWidthMeasurement = this.pendingTextWidthRequests.take();
				Long x1 = currentTextWidthMeasurement.getX1();
				Long y1 = currentTextWidthMeasurement.getY1();
				String text = currentTextWidthMeasurement.getText();

				logger.info("About to print test text '" + text + "' so width can be measured.");
				System.out.print("\033[" + y1 + ";" + x1 + "H"); //  Move cursor known reference point to calculate offset of text.
				System.out.flush();
				System.out.print(text); //  Print the text for which we want to measure width.
				System.out.flush();
				System.out.println("\033[6n");  //  Request cursor position measurement.
				System.out.flush();
				//  We just by-passed the screen printing buffer, so invalidate the area of the
				//  screen where the test character was and re-print whatever was there:
				this.setScreenAreaChangeStates(0, 0, 16, 4, ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT, true);
				this.printTerminalTextChanges(false);
				logger.info("Finished printing test text '" + text + "' and issued cursor re-positioning request to calculate width. Waiting for result on stdin...");
			}
			if(this.currentTextWidthMeasurement == null){
				if(this.terminalWidth != null && this.terminalHeight != null){
					while(this.pendingQueueableWorkItems.size() > 0){
						ConsoleQueueableWorkItem w = this.pendingQueueableWorkItems.take();
						WorkItemResult result = w.executeQueuedWork();
						//  If the thread expects a response, unblock it:
						if(result != null){
							this.addResultForThreadId(result, w.getThreadId());
						}
					}
					this.printTerminalTextChanges(true);
				}
			}else{
				TextWidthMeasurementWorkItemResult r = this.currentTextWidthMeasurement.getResult();
				if(r != null){ //  Did we actually get a result yet?
					this.addResultForThreadId(r, this.currentTextWidthMeasurement.getThreadId()); //  Unblock whatever thread was waiting.
					logger.info("In CharacterWidthMeasurementThreadState, Added a text width result for '" + this.currentTextWidthMeasurement.getText() + "' with a value of deltaX=" + r.getDeltaX() + ", deltaY=" + r.getDeltaY() + ",  thread_id=" + this.currentTextWidthMeasurement.getThreadId());
					this.currentTextWidthMeasurement = null; //  Allow for processing of next character width request.
				}
			}
		}
		return pendingQueueableWorkItems.size() > 0; // There is no additional work we can do until we get another work item.
	}

	public WorkItemResult putBlockingWorkItem(ConsoleWriterWorkItem workItem, WorkItemPriority priority) throws Exception {
		BlockManagerThread t = this.blockManagerThreadCollection.getThreadById(Thread.currentThread().threadId());
		if(t instanceof WorkItemProcessorTask){
			Class<?> ct = ((WorkItemProcessorTask<?>)t).getEntityClass();
			if(ct == ConsoleWriterWorkItem.class && workItem.getIsBlocking()){
				throw new Exception("Current thread is instanceof WorkItemProcessorTask<ConsoleWriterWorkItem>.  Attempting to block here will cause a deadlock.");
			}else{
				return this.workItemQueue.putBlockingWorkItem(workItem, priority);
			}
		}else{
			return this.workItemQueue.putBlockingWorkItem(workItem, priority);
		}
	}

	public void addResultForThreadId(WorkItemResult workItemResult, Long threadId) throws Exception {
		this.workItemQueue.addResultForThreadId(workItemResult, threadId);
	}
}
