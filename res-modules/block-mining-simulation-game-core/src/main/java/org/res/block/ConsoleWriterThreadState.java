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
import java.util.concurrent.atomic.AtomicLong;

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

	private static final AtomicLong terminalDimensionsChangeSeq = new AtomicLong(0);

	private Coordinate previousPosition;     //  For openning new map areas
	private Coordinate newPosition;          //  For openning new map areas
	private PlayerInventory playerInventory; //  For openning new inventories
	private Long terminalWidth = null;
	private Long terminalHeight = null;
	private FrameDimensions currentTerminalFrameDimensions = null;
	private HelpMenuFrameThreadState helpMenuFrameThreadState = null;
	private int numScreenLayers = 2;
	public static final int BUFFER_INDEX_DEFAULT = 0;
	public static final int BUFFER_INDEX_MENU = 1;
	private ScreenLayer [] screenLayer = new ScreenLayer [this.numScreenLayers];
	private ScreenMask [] screenMask = new ScreenMask [this.numScreenLayers];

	public Long focusedFrameId = null;

	private Map<Long, FrameDimensions> currentFrameDimensionsCollection = new HashMap<Long, FrameDimensions>();
	private Map<Long, UserInterfaceSplit> userInterfaceSplits = new HashMap<Long, UserInterfaceSplit>();
	private Long rootSplitId;

	private int [] lastUsedColourCodes = new int [] {UserInterfaceFrameThreadState.RESET_BG_COLOR};

	public ConsoleWriterThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception{
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
	}

	public void init() throws Exception {
		Long helpMenuFrameId = this.createFrameAndThread(HelpMenuFrameThreadState.class);
		this.helpMenuFrameThreadState = (HelpMenuFrameThreadState)this.getFrameStateById(helpMenuFrameId);

		for(int i = 0; i < this.numScreenLayers; i++){
			this.screenLayer[i] = new ScreenLayer();
			this.screenMask[i] = new ScreenMask();
		}
		this.initializeConsole(80L, 24L); // Early setup is necessary so we can do text width calculations before drawing frame

		int numMapAreas = 1;
		int numInventoryAreas = 1;
		for(int i = 0; i < numMapAreas; i++){
			this.createFrameAndThread(MapAreaInterfaceThreadState.class);
		}
		for(int i = 0; i < numInventoryAreas; i++){
			this.createFrameAndThread(InventoryInterfaceThreadState.class);
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
			Long r = makeVerticalSplit();
			this.addSplitPartsByIds(r, splits);
			((UserInterfaceSplitMulti)this.getUserInterfaceSplitById(r)).setSplitPercentages(framePercents);
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

	public Map<Long, Long> getFrameIdToParentSplitIdMap() throws Exception{
		Map<Long, Long> m = new HashMap<Long, Long>();
		for(Map.Entry<Long, UserInterfaceSplit> e : userInterfaceSplits.entrySet()){
			UserInterfaceSplit u = e.getValue();
			if(u instanceof UserInterfaceSplitLeafNode){
				UserInterfaceSplitLeafNode l = (UserInterfaceSplitLeafNode)u;
				Long frameId = l.getUserInterfaceFrameThreadState().getFrameId();
				if(m.containsKey(frameId)){
					throw new Exception("Impossible");
				}else{
					m.put(frameId, l.getSplitId());
				}
			}
		}
		return m;
	}

	public Map<Long, Long> getChildToParentSplitIdMap() throws Exception{
		Map<Long, Long> m = new HashMap<Long, Long>();
		for(Map.Entry<Long, UserInterfaceSplit> e : userInterfaceSplits.entrySet()){
			UserInterfaceSplit u = e.getValue();
			if(u instanceof UserInterfaceSplitMulti){
				UserInterfaceSplitMulti l = (UserInterfaceSplitMulti)u;
				Long parentSplitId = l.getSplitId();
				for(UserInterfaceSplit child : l.getSplitParts()){
					Long childSplitId = child.getSplitId();
					if(m.containsKey(childSplitId)){
						throw new Exception("Impossible");
					}else{
						m.put(childSplitId, parentSplitId);
					}
				}
			}
		}
		return m;
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
			frameStateClass == HelpMenuFrameThreadState.class ||
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
		if(frameStateClass == HelpMenuFrameThreadState.class){
			return addFrameState(
				new HelpMenuFrameThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext)
			);
		}else if(frameStateClass == HelpDetailsFrameThreadState.class){
			return addFrameState(
				new HelpDetailsFrameThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext)
			);
		}else if(frameStateClass == EmptyFrameThreadState.class){
			return addFrameState(
				new EmptyFrameThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext)
			);
		}else if(frameStateClass == MapAreaInterfaceThreadState.class){
			Long mapId = addFrameState(
				new MapAreaInterfaceThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext)
			);
			this.mapAreaInterfaceFrameIds.add(mapId);
			if(this.newPosition != null){
				this.onPlayerPositionChange(this.previousPosition, this.newPosition);
			}
			return mapId;
		}else if(frameStateClass == InventoryInterfaceThreadState.class){
			Long inventoryId = addFrameState(
				new InventoryInterfaceThreadState(this.blockManagerThreadCollection, this.clientBlockModelContext)
			);
			if(this.playerInventory != null){
				this.onPlayerInventoryChange(this.playerInventory);
			}
			this.inventoryInterfaceFrameIds.add(inventoryId);
			return inventoryId;
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

	public Long onRotateSplit(Long parentSplitId, Long childSplitIdToRotate, boolean isForward) throws Exception{
		UserInterfaceSplitMulti m = (UserInterfaceSplitMulti)getUserInterfaceSplitById(parentSplitId);
		m.rotateChildWithId(childSplitIdToRotate, isForward);
		logger.info("onRotateSplit Rotated isForward=" + isForward);
		return 0L;
	}

	public EmptyWorkItemResult onResizeFrame(Long frameId, Long deltaXColumns, Long deltaYColumns) throws Exception{
		Long splitIdContainingFrame = this.getFrameIdToParentSplitIdMap().get(frameId);
		Map<Long, Long> childToParentSplitIdMap = this.getChildToParentSplitIdMap();
		Long closestHorizontalParentId = splitIdContainingFrame;
		Long closestHorizontalChildId = null;
		Long closestVerticalParentId = splitIdContainingFrame;
		Long closestVerticalChildId = null;
		UserInterfaceSplit closestHorizontalParent = null;
		UserInterfaceSplit closestVerticalParent = null;
		//  Look for closest parent horizontal split
		while(true){
			closestHorizontalParent = this.getUserInterfaceSplitById(closestHorizontalParentId);
			if(closestHorizontalParent instanceof UserInterfaceSplitHorizontal){
				break;
			}else if(childToParentSplitIdMap.containsKey(closestHorizontalParentId)){
				closestHorizontalChildId = closestHorizontalParentId;
				closestHorizontalParentId = childToParentSplitIdMap.get(closestHorizontalParentId);
			}else{
				closestHorizontalParent = null;
				break;
			}
		}
		//  Look for closest parent vertical split
		while(true){
			closestVerticalParent = this.getUserInterfaceSplitById(closestVerticalParentId);
			if(closestVerticalParent instanceof UserInterfaceSplitVertical){
				break;
			}else if(childToParentSplitIdMap.containsKey(closestVerticalParentId)){
				closestVerticalChildId = closestVerticalParentId;
				closestVerticalParentId = childToParentSplitIdMap.get(closestVerticalParentId);
			}else{
				closestVerticalParent = null;
				break;
			}
		}

		if(closestVerticalParent != null && (closestVerticalParent instanceof UserInterfaceSplitVertical)){
			((UserInterfaceSplitVertical)closestVerticalParent).resizeChildSplitWithId(closestVerticalChildId, deltaXColumns, this.terminalWidth);
		}
		if(closestHorizontalParent != null && (closestHorizontalParent instanceof UserInterfaceSplitHorizontal)){
			((UserInterfaceSplitHorizontal)closestHorizontalParent).resizeChildSplitWithId(closestHorizontalChildId, deltaYColumns, this.terminalHeight);
		}

		this.onFrameDimensionsChanged();
		return new EmptyWorkItemResult();
	}

	public Long onCloseFrame(Long frameId) throws Exception{
		this.destroyFrameStateAndThreadById(frameId);
		this.focusedFrameId = null;
		this.inventoryInterfaceFrameIds.remove(frameId);
		this.mapAreaInterfaceFrameIds.remove(frameId);
		return frameId;
	}

	public Long onSetRootSplitId(Long newRootSplitId) throws Exception{
		return this.setRootSplit(newRootSplitId);
	}

	public boolean onRemoveChildSplit_h(Long parentSplitId, Long childSplitId) throws Exception{
		UserInterfaceSplit u = getUserInterfaceSplitById(parentSplitId);
		List<Integer> splitsToRemove = new ArrayList<Integer>();
		boolean done = false;
		while(!done){
			for(int i = 0; i < u.getSplitParts().size(); i++){
				if(this.onRemoveChildSplit_h(u.getSplitParts().get(i).getSplitId(), childSplitId)){
					//  If removing the child split made the child split empty:
					u.removeSplitAtIndex(i);
					break;
				}
			}
			done = true;
		}

		for(int i = 0; i < u.getSplitParts().size(); i++){
			if(childSplitId.equals(u.getSplitParts().get(i).getSplitId())){
				u.removeSplitAtIndex(i);
				break;
			}
		}

		//  Is the current split an empty v or h split?
		if((!(u instanceof UserInterfaceSplitLeafNode)) && u.getSplitParts().size() == 0){
			return true;
		}else{
			return false;
		}
	}

	public Long onRemoveChildSplit(Long childSplitId) throws Exception{
		this.onRemoveChildSplit_h(this.rootSplitId, childSplitId);

		if(childSplitId.equals(this.rootSplitId)){
			this.rootSplitId = null;
		}

		if(this.rootSplitId != null){
			// When all children have been removed, remove the root split:
			UserInterfaceSplit u = getUserInterfaceSplitById(this.rootSplitId);
			if(u.getSplitParts().size() == 0){
				this.rootSplitId = null;
			}
		}
		return childSplitId;
	}

	public SplitInfoWorkItemResult makeOneSplitInfo(Long splitId) throws Exception{
		if(splitId == null){
			return new SplitInfoWorkItemResult(null, null, null);
		}else{
			UserInterfaceSplit split = getUserInterfaceSplitById(splitId);
			if(split instanceof UserInterfaceSplitLeafNode){
				Long frameId = ((UserInterfaceSplitLeafNode)split).getUserInterfaceFrameThreadState().getFrameId();
				return new SplitInfoWorkItemResult(splitId, split.getClass(), frameId);
			}else{
				return new SplitInfoWorkItemResult(splitId, split.getClass(), null);
			}
		}
	}

	public FrameInfoWorkItemResult makeOneFrameInfo(Long frameId) throws Exception{
		if(frameId == null){
			return new FrameInfoWorkItemResult(null, null);
		}else{
			UserInterfaceFrameThreadState state = getFrameStateById(frameId);
			return new FrameInfoWorkItemResult(frameId, state.getClass());
		}
	}

	public FrameInfoWorkItemResult onGetFrameInfo(Long frameId) throws Exception{
		return makeOneFrameInfo(frameId);
	}

	public SplitInfoWorkItemResult onGetSplitInfo(Long splitId, boolean returnRoot) throws Exception{
		if(returnRoot){
			return makeOneSplitInfo(this.getRootSplitId());
		}else{
			return makeOneSplitInfo(splitId);
		}
	}

	public GetSplitChildrenInfoWorkItemResult onGetSplitChildrenInfo(Long parentSplitId) throws Exception{
		List<SplitInfoWorkItemResult> rtn = new ArrayList<SplitInfoWorkItemResult>();
		UserInterfaceSplit u = getUserInterfaceSplitById(parentSplitId);
		for(UserInterfaceSplit part : u.getSplitParts()){
			rtn.add(makeOneSplitInfo(part.getSplitId()));
		}
		return new GetSplitChildrenInfoWorkItemResult(rtn);
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

	public Long onSetFocusedFrame() throws Exception{
		return this.focusedFrameId;
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
		this.playerInventory = playerInventory;
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
		this.previousPosition = previousPosition;
		this.newPosition = newPosition;
		for(Long mapAreaInterfaceFrameId : this.mapAreaInterfaceFrameIds){
			MapAreaInterfaceThreadState m = (MapAreaInterfaceThreadState)this.getFrameStateById(mapAreaInterfaceFrameId);
			m.putWorkItem(new MapAreaNotifyPlayerPositionChangeWorkItem(m, previousPosition, newPosition), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public List<UserInterfaceFrameThreadState> collectAllUserInterfaceFrames() throws Exception{
		if(this.rootSplitId == null){
		 	return new ArrayList<UserInterfaceFrameThreadState>();
		}else{
			return this.getUserInterfaceSplitById(rootSplitId).collectUserInterfaceFrames();
		}
	}

	public FrameChangeWorkItemParams makeFrameChangeWorkItemParams(UserInterfaceFrameThreadState frame) throws Exception{
		return new FrameChangeWorkItemParams(
			this.getFocusedFrameDimensions(),
			this.getFrameDimensionsForFrameId(frame.getFrameId()),
			this.getFrameBordersDescription(),
			terminalDimensionsChangeSeq.get(),
			frame.getFrameDimensionsChangeId(),
			frame.getFrameId()
		);
	}

	public void sendFrameChangeWorkItem(UserInterfaceFrameThreadState frame) throws Exception{
		frame.putWorkItem(
			new FrameChangeWorkItem(frame, makeFrameChangeWorkItemParams(frame)),
			WorkItemPriority.PRIORITY_LOW
		);
	}

	public void notifyAllFramesOfFocusChange() throws Exception{

		for(UserInterfaceFrameThreadState frame : this.collectAllUserInterfaceFrames()){
			this.sendFrameChangeWorkItem(frame);
		}

		this.sendFrameChangeWorkItem(this.helpMenuFrameThreadState);
	
		if(this.collectAllUserInterfaceFrames().size() == 0){
			//  If there are no active frames, there is no UI frame to clear what was there before:
			String msg = "All frames have been closed!  Press 'ESC' to open one.";
			this.screenLayer[0].initialize(this.terminalWidth.intValue(), this.terminalHeight.intValue(), 1, " ", msg);
			this.screenMask[0].initialize(this.terminalWidth.intValue(), this.terminalHeight.intValue(), true);
			this.printTerminalTextChanges(false);
		}
	}

	public void focusOnNextFrame() throws Exception{
		Long previousFrameId = this.focusedFrameId;
		String oldFrameInfo = this.focusedFrameId == null ? "null" : "frameId=" + this.focusedFrameId;
		List<UserInterfaceFrameThreadState> allFrames = this.collectAllUserInterfaceFrames();
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
				newFocusedFrameId = null;
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

	public Long getRootSplitId(){
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
				if(this.focusedFrameId == null){
					logger.info("Discarding because no focused frame: " + ansiEscapeSequence.getClass().getName());
				}else{
					this.sendAnsiEscapeSequenceToFrame(ansiEscapeSequence, this.getFrameStateById(this.focusedFrameId));
				}
			}
		}
	}

	public WorkItemResult prepareTerminalTextChange(int [][] newCharacterWidths, int [][][] newColourCodes, String [][] newCharacters, boolean [][] hasChange, int xOffset, int yOffset, int xChangeSize, int yChangeSize, FrameDimensions frameDimensions, int bufferIndex, FrameChangeWorkItemParams frameChangeParams) throws Exception{
		if(!activeFrameStates.containsKey(frameChangeParams.getFrameId())){
			logger.info("Discarding frame text change for frame id=: " + frameChangeParams.getFrameId() + " because frame id does not exist (it's probably closed).");
			return new EmptyWorkItemResult();
		}

		UserInterfaceFrameThreadState frame = this.getFrameStateById(frameChangeParams.getFrameId());
		if(frameChangeParams.getFrameDimensionsChangeId() < frame.getFrameDimensionsChangeId()){
			logger.info("Discarding outdated frame text change: frameChangeParams.getFrameDimensionsChangeId()=" + frameChangeParams.getFrameDimensionsChangeId() + ", frame.getFrameDimensionsChangeId()=" + frame.getFrameDimensionsChangeId());
			return makeFrameChangeWorkItemParams(frame);
		}
		if(frameChangeParams.getTerminalDimensionsChangeId() < ConsoleWriterThreadState.terminalDimensionsChangeSeq.get()){
			logger.info("Discarding outdated text change: frameChangeParams.getTerminalDimensionsChangeId()=" + frameChangeParams.getTerminalDimensionsChangeId() + ", ConsoleWriterThreadState.terminalDimensionsChangeSeq.get()=" + ConsoleWriterThreadState.terminalDimensionsChangeSeq.get());
			return makeFrameChangeWorkItemParams(frame);
		}
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
							this.screenLayer[bufferIndex].characterWidths[xOffset + i][yOffset + j] == newCharacterWidths[i][j] &&
							Arrays.equals(this.screenLayer[bufferIndex].colourCodes[xOffset + i][yOffset + j], newColourCodes[i][j]) &&
							this.screenLayer[bufferIndex].characters[xOffset + i][yOffset + j] == newCharacters[i][j]
						) || this.screenMask[bufferIndex].flags[xOffset + i][yOffset + j];
						this.screenLayer[bufferIndex].characterWidths[xOffset + i][yOffset + j] = newCharacterWidths[i][j];
						this.screenLayer[bufferIndex].colourCodes[xOffset + i][yOffset + j] = newColourCodes[i][j];
						this.screenLayer[bufferIndex].characters[xOffset + i][yOffset + j] = newCharacters[i][j];
						this.screenMask[bufferIndex].flags[xOffset + i][yOffset + j] = hasChanged;
						//  If we're printing on a screen buffer that's in front, we will eventually need to update whatever was behind it:
						if(hasChanged){
							for(int k = bufferIndex; k >= 0 ; k--){
								this.screenMask[bufferIndex].flags[xOffset + i][yOffset + j] = hasChanged;
							}
						}
					}else{
						//throw new Exception("Discarding character '" + newCharacters[i][j] + "' because if was out of bounds at x=" + (xOffset + i) + ", y=" + (yOffset + j));
					}
				}
			}
		}
		return new EmptyWorkItemResult();
	}


	public static void mergeLayerOnto(ScreenLayer bottomLayer, ScreenMask bottomMask, ScreenLayer topLayer, ScreenMask topMask, ScreenLayer outputLayer, ScreenMask outputMask) throws Exception{
		for(int i = 0; i < bottomLayer.width; i++){
			for(int j = 0; j < bottomLayer.height; j++){
				if(topMask.flags[i][j] && topLayer.characters[i][j] != null){
					outputLayer.characterWidths[i][j] = topLayer.characterWidths[i][j];
					outputLayer.colourCodes[i][j] = topLayer.colourCodes[i][j];
					outputLayer.characters[i][j] = topLayer.characters[i][j];
					outputMask.flags[i][j] = true;
				}else if(bottomMask.flags[i][j]){
					outputLayer.characterWidths[i][j] = bottomLayer.characterWidths[i][j];
					outputLayer.colourCodes[i][j] = bottomLayer.colourCodes[i][j];
					outputLayer.characters[i][j] = bottomLayer.characters[i][j];
					outputMask.flags[i][j] = true;
				}
			}
		}
	}

	public void printTerminalTextChanges(boolean resetCursorPosition) throws Exception{
		ScreenLayer toOutputLayer = new ScreenLayer();
		ScreenMask toOutputMask = new ScreenMask();
		toOutputLayer.initialize(this.terminalWidth.intValue(), this.terminalHeight.intValue());
		toOutputMask.initialize(this.terminalWidth.intValue(), this.terminalHeight.intValue(), false);

		ConsoleWriterThreadState.mergeLayerOnto(
			this.screenLayer[BUFFER_INDEX_DEFAULT],
			this.screenMask[BUFFER_INDEX_DEFAULT],
			this.screenLayer[BUFFER_INDEX_MENU],
			this.screenMask[BUFFER_INDEX_MENU],
			toOutputLayer,
			toOutputMask
		);

		boolean useRightToLeftPrint = this.blockManagerThreadCollection.getRightToLeftPrint();
		int startColumn = useRightToLeftPrint ? this.terminalWidth.intValue() -1 : 0;
		int endColumn = useRightToLeftPrint ? -1 : this.terminalWidth.intValue();
		int loopUpdate = useRightToLeftPrint ? -1 : 1;
		boolean resetState = useRightToLeftPrint ? true : false;
		for(int j = 0; j < this.terminalHeight.intValue(); j++){
			boolean mustSetCursorPosition = true;
			boolean mustSetColourCodes = true;
			for(int i = startColumn; i != endColumn; i += loopUpdate){
				//  Try to intelligently issue as few ANSI escape sequences as possible:
				if(!Arrays.equals(toOutputLayer.colourCodes[i][j], lastUsedColourCodes)){
					mustSetColourCodes = true;
				}
				if(
					toOutputMask.flags[i][j] &&
					toOutputLayer.characters[i][j] != null
				){
					if(mustSetCursorPosition){
						String currentPositionSequence = "\033[" + (j+1) + ";" + (i+1) + "H";
						System.out.print(currentPositionSequence);
						mustSetCursorPosition = resetState;
					}
					if(mustSetColourCodes){
						List<String> codes = new ArrayList<String>();
						for(int c : toOutputLayer.colourCodes[i][j]){
							codes.add(String.valueOf(c));
						}
						String currentColorSequence = "\033[0m\033[" + String.join(";", codes) + "m";
						System.out.print(currentColorSequence);
						mustSetColourCodes = resetState;
						lastUsedColourCodes = toOutputLayer.colourCodes[i][j];
					}
					System.out.print(toOutputLayer.characters[i][j]);
				}else{
					mustSetCursorPosition = true;
				}
			}
		}
		this.screenMask[BUFFER_INDEX_DEFAULT].initialize(this.terminalWidth.intValue(), this.terminalHeight.intValue(), false); //TODO: remove this later once I have a way to deactive the menu layer
		if(resetCursorPosition){
			System.out.print("\033[0;0H"); //  Move cursor to 0,0 after every print.
		}
	}

	public final void initializeConsole(Long terminalWidth, Long terminalHeight) throws Exception{
		this.terminalWidth = terminalWidth;
		this.terminalHeight = terminalHeight;
		for(int i = 0; i < this.numScreenLayers; i++){
			this.screenLayer[i].initialize(this.terminalWidth.intValue(), this.terminalHeight.intValue());
			this.screenMask[i].initialize(this.terminalWidth.intValue(), this.terminalHeight.intValue());
		}
	}

	public UserInterfaceFrameThreadState getFocusedFrame() throws Exception{
		return (this.focusedFrameId == null) ? null : this.getFrameStateById(this.focusedFrameId);
	}

	public FrameDimensions getFocusedFrameDimensions() throws Exception{
		if(this.focusedFrameId == null){
			return null;
		}else{
			FrameDimensions fd = currentFrameDimensionsCollection.get(this.focusedFrameId);
			if(fd == null){
				return null;
			}else{
				return new FrameDimensions(fd);
			}
		}
	}

	public FrameDimensions getFrameDimensionsForFrameId(Long frameId) throws Exception{
		FrameDimensions f = this.currentFrameDimensionsCollection.get(frameId);
		return f == null ? null : new FrameDimensions(f);
	}

	public FrameBordersDescription getFrameBordersDescription() throws Exception{
		UserInterfaceSplit rootSplit = this.rootSplitId == null ? null : this.getUserInterfaceSplitById(this.rootSplitId);
		return (rootSplit == null) ? new FrameBordersDescription(new HashSet<Coordinate>()) : new FrameBordersDescription(rootSplit.collectAllConnectionPoints(this.currentTerminalFrameDimensions));
	}

	public void onFrameDimensionsChanged() throws Exception{
		if(this.rootSplitId == null){
			this.currentFrameDimensionsCollection = new HashMap<Long, FrameDimensions>();
		}else{
			this.currentFrameDimensionsCollection = this.getUserInterfaceSplitById(this.rootSplitId).collectFrameDimensions(this.currentTerminalFrameDimensions);
		}
		this.currentFrameDimensionsCollection.put(this.helpMenuFrameThreadState.getFrameId(), this.currentTerminalFrameDimensions);

		if(this.rootSplitId != null){
			this.getUserInterfaceSplitById(this.rootSplitId).sendFrameChangeNotifies(this);
		}
	}

	public Long getTerminalDimensionsChangeId(){
		return terminalDimensionsChangeSeq.get();
	}

	public void onTerminalDimensionsChanged(Long terminalWidth, Long terminalHeight, Long frameCharacterWidth) throws Exception{
		terminalDimensionsChangeSeq.getAndIncrement();
		this.initializeConsole(terminalWidth, terminalHeight);

		if(this.focusedFrameId == null){
			this.focusOnNextFrame();
		}
		this.currentTerminalFrameDimensions = new FrameDimensions(frameCharacterWidth, terminalWidth, terminalHeight, 0L, 0L, terminalWidth, terminalHeight);

		//  When the terminal size changes, send a notify to all of the user interface frames to let them know about it
		this.onFrameDimensionsChanged();

		this.sendFrameChangeWorkItem(this.helpMenuFrameThreadState);

		this.notifyAllFramesOfFocusChange();
	}

	public void sendKeyboardInputToFrame(byte [] bytesToSend, UserInterfaceFrameThreadState destinationFrame) throws Exception {
		if(destinationFrame != null){
			destinationFrame.putWorkItem(new ProcessFrameInputBytesWorkItem(destinationFrame, bytesToSend), WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void doDefaultKeyboardInput(byte [] characters) throws Exception {
		if(this.focusedFrameId == null){
			logger.info("Discarding because no focused frame: " + new String(characters, "UTF-8"));
		}else{
			this.sendKeyboardInputToFrame(characters, this.getFrameStateById(this.focusedFrameId));
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
					this.doDefaultKeyboardInput(new byte [] {b});
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
							this.doDefaultKeyboardInput(new byte [] {b});
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

	public EmptyWorkItemResult setScreenAreaChangeStates(int startX, int startY, int endX, int endY, int bufferIndex, boolean state){
		//  Invalidate a sub-area of screen so that the characters are that location will 
		//  be printed on the next print attempt.
		int width = endX - startX;
		int height = endY - startY;
		
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				if(((i + startX) < this.terminalWidth) && ((j + startY) < this.terminalHeight)){ // TODO:  Remove this check once the synchronize 'render' is finished.
					this.screenMask[bufferIndex].flags[i + startX][j + startY] = state;
				}
			}
		}
		return new EmptyWorkItemResult();
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
						this.addResultForThreadId(result, w.getThreadId());
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
