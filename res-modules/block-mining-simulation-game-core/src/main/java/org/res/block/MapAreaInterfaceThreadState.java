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

public class MapAreaInterfaceThreadState extends UserInterfaceFrameThreadState {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	//  Padding columns on right edge of map area where only part of a cell could fit:
	private Long mapAreaPaddingColumnsRight;
	private Long mapAreaHeightInCells;
	private Long mapAreaWidthInCells;
	private Coordinate playerPosition;
	private CuboidAddress mapAreaCuboidAddress = new CuboidAddress(
		//  Initialize map to empty region at origin
		Coordinate.makeDiagonalCoordinate(0L, 4L),
		Coordinate.makeDiagonalCoordinate(0L, 4L)
	);
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;

	private ClientBlockModelContext clientBlockModelContext;

	private ThreeDimensionalCircularBuffer<IndividualBlock> mapAreaBlocks = new ThreeDimensionalCircularBuffer<IndividualBlock>(IndividualBlock.class, new PendingLoadBlock());

	//  The buffer distance from the player position to the visible map edge 
	//  for each dimension before the map area drags around:
	private Coordinate edgeDistances = new Coordinate(Arrays.asList(10L, 1L, 10L, 1L));

	private byte[] unprocessedInputBytes = new byte[0];
	private ScreenLayer playerIcon;

	public MapAreaInterfaceThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext, ConsoleWriterThreadState consoleWriterThreadState) throws Exception {
		super(blockManagerThreadCollection, consoleWriterThreadState, new int [] {ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT}, new ScreenLayerMergeType [] {ScreenLayerMergeType.PREFER_BOTTOM_LAYER, ScreenLayerMergeType.PREFER_INPUT_TRANSPARENCY});
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;

	}

	protected void init(Object o) throws Exception{
		UIModelProbeWorkItemResult result = (UIModelProbeWorkItemResult)this.clientBlockModelContext.putBlockingWorkItem(
			new UIModelProbeWorkItem(
				this.clientBlockModelContext,
				UINotificationType.PLAYER_POSITION,
				UINotificationSubscriptionType.SUBSCRIBE,
				this
			),
			WorkItemPriority.PRIORITY_LOW
		);

		//  Set initial player position:
		this.onPlayerPositionChange((PlayerPositionXYZ)result.getObject());

		//  Subscribe to new map area flag updates:
		this.clientBlockModelContext.putBlockingWorkItem(
			new UIModelProbeWorkItem(
				this.clientBlockModelContext,
				UINotificationType.UPDATE_MAP_AREA_FLAGS,
				UINotificationSubscriptionType.SUBSCRIBE,
				this
			),
			WorkItemPriority.PRIORITY_LOW
		);
	}

	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		if(ansiEscapeSequence instanceof AnsiEscapeSequenceUpArrowKey){
			this.sendTryPositionChange(new Vector(Arrays.asList(0L, 0L, 1L, 0L)));
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceRightArrowKey){
			this.sendTryPositionChange(new Vector(Arrays.asList(1L, 0L, 0L, 0L)));
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceDownArrowKey){
			this.sendTryPositionChange(new Vector(Arrays.asList(0L, 0L, -1L, 0L)));
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceLeftArrowKey){
			this.sendTryPositionChange(new Vector(Arrays.asList(-1L, 0L, 0L, 0L)));
		}else{
			logger.info("MapAreaInterfaceThreadState, discarding unknown ansi escape sequence of type: " + ansiEscapeSequence.getClass().getName());
		}
	}

	public void onKeyboardInput(byte [] characters) throws Exception {
		logger.info("Processing keyboard input: " + new String(characters, "UTF-8"));
		UserInteractionConfig ki = this.blockManagerThreadCollection.getUserInteractionConfig();
		ByteArrayOutputStream baosBoth = new ByteArrayOutputStream();
		baosBoth.write(this.unprocessedInputBytes);
		baosBoth.write(characters);
		this.unprocessedInputBytes = baosBoth.toByteArray();
		for(int i = 0; i < this.unprocessedInputBytes.length; i++){
			byte c = this.unprocessedInputBytes[i];
			String actionString = new String(new byte [] {c}, "UTF-8");
			boolean is_last = i == this.unprocessedInputBytes.length -1;
			UserInterfaceActionType action = ki.getKeyboardActionFromString(actionString);

			if(action == null){
				logger.info("Discarding " + c);
			}else{
				switch(action){

					case ACTION_Y_PLUS:{
						this.sendTryPositionChange(new Vector(Arrays.asList(0L, 0L, 1L, 0L)));
						break;
					}case ACTION_X_MINUS:{
						this.sendTryPositionChange(new Vector(Arrays.asList(-1L, 0L, 0L, 0L)));
						break;
					}case ACTION_Y_MINUS:{
						this.sendTryPositionChange(new Vector(Arrays.asList(0L, 0L, -1L, 0L)));
						break;
					}case ACTION_X_PLUS:{
						this.sendTryPositionChange(new Vector(Arrays.asList(1L, 0L, 0L, 0L)));
						break;
					}case ACTION_Z_MINUS:{
						this.sendTryPositionChange(new Vector(Arrays.asList(0L, -1L, 0L, 0L)));
						break;
					}case ACTION_Z_PLUS:{
						this.sendTryPositionChange(new Vector(Arrays.asList(0L, 1L, 0L, 0L)));
						break;
					}case ACTION_W_MINUS:{
						this.sendTryPositionChange(new Vector(Arrays.asList(0L, 0L, 0L, -1L)));
						break;
					}case ACTION_W_PLUS:{
						this.sendTryPositionChange(new Vector(Arrays.asList(0L, 0L, 0L, 1L)));
						break;
					}case ACTION_PLACE_BLOCK:{
						this.clientBlockModelContext.putWorkItem(new ClientActionWorkItem(this.clientBlockModelContext, action), WorkItemPriority.PRIORITY_LOW);
						break;
					}case ACTION_CRAFTING:{
						this.clientBlockModelContext.putWorkItem(new ClientActionWorkItem(this.clientBlockModelContext, action), WorkItemPriority.PRIORITY_LOW);
						break;
					}case ACTION_MINING:{
						this.clientBlockModelContext.putWorkItem(new ClientActionWorkItem(this.clientBlockModelContext, action), WorkItemPriority.PRIORITY_LOW);
						break;
					}default:{
						logger.info("Discarding Unexpected action=" + action.toString());
					}
				}
			}
		}
		//  All input bytes have been processed:
		this.unprocessedInputBytes = new byte[0];
	}

	public void sendTryPositionChange(Vector delta) throws Exception{
		//  This frame sends the event up to the client.  If the client model
		//  agrees on position change, the actual change will be propagated
		//  back down to this frame.
		this.clientBlockModelContext.putWorkItem(new ClientModelNotificationWorkItem(this.clientBlockModelContext, delta, ClientModelNotificationType.DO_TRY_POSITION_CHANGE), WorkItemPriority.PRIORITY_LOW);
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public void onRenderFrame(boolean hasThisFrameDimensionsChanged, boolean hasOtherFrameDimensionsChanged) throws Exception{
		if(hasThisFrameDimensionsChanged){
			this.mapAreaWidthInCells = this.getInnerFrameWidth() / this.getMapAreaCellWidth();
			this.mapAreaHeightInCells = this.getInnerFrameHeight();
			//  Don't allow the map area to negative when the window is resized very small:
			this.mapAreaWidthInCells = mapAreaWidthInCells < 1L ? 1L : mapAreaWidthInCells;
			this.mapAreaHeightInCells = mapAreaHeightInCells < 1L ? 1L : mapAreaHeightInCells;
			this.mapAreaPaddingColumnsRight = this.getInnerFrameWidth() - (this.mapAreaWidthInCells * this.getMapAreaCellWidth());
			logger.info("onRenderFrame calculated: mapAreaWidthInCells=" + mapAreaWidthInCells + ", mapAreaHeightInCells=" + mapAreaHeightInCells);
			Long topRightHandX = mapAreaWidthInCells / 2L;
			Long topRightHandZ = mapAreaHeightInCells / 2L;
			Long bottomLeftHandX = topRightHandX - (mapAreaWidthInCells - 1L);
			Long bottomLeftHandZ = topRightHandZ - (mapAreaHeightInCells - 1L);
			logger.info("onRenderFrame calculated: topRightHandX=" + topRightHandX + ", topRightHandZ=" + topRightHandZ + ", bottomLeftHandX=" + bottomLeftHandX + ", bottomLeftHandZ=" + bottomLeftHandZ);

			Coordinate bottomleftHandCorner = playerPosition.add(new Coordinate(Arrays.asList(bottomLeftHandX, -1L, bottomLeftHandZ, 0L)));
			Coordinate topRightHandCorner = playerPosition.add(new Coordinate(Arrays.asList(topRightHandX, 0L, topRightHandZ, 0L)));

			CuboidAddress newMapArea = new CuboidAddress(bottomleftHandCorner, topRightHandCorner.add(Coordinate.makeUnitCoordinate(4L)));
			this.onMapAreaChange(newMapArea);

		}else if(hasOtherFrameDimensionsChanged){
			this.onMapAreaChange(this.mapAreaCuboidAddress); // Refresh map area
		}

		this.drawBorders();
		this.updateFrameCoordinate();
	}

	public void updateMapAreaForPlayerPositionChange(Coordinate previousPosition, Coordinate newPosition) throws Exception{
		if(previousPosition == null){
		}else{
			Coordinate displacement = newPosition.subtract(previousPosition);

			/*  Move camera around if the player tries to move out of bounds. */
			Coordinate lower = this.mapAreaCuboidAddress.getCanonicalLowerCoordinate();
			Coordinate upper = this.mapAreaCuboidAddress.getCanonicalUpperCoordinate();
			Coordinate newLower = lower.copy();
			Coordinate newUpper = upper.copy();

			for(long l = 0; l < displacement.getNumDimensions(); l++){
				if(
					(newPosition.getValueAtIndex(l) - lower.getValueAtIndex(l) < this.edgeDistances.getValueAtIndex(l)) ||
					(upper.getValueAtIndex(l) - 1L - newPosition.getValueAtIndex(l) < this.edgeDistances.getValueAtIndex(l))
				){
					newLower = newLower.changeValueAtIndex(l, lower.getValueAtIndex(l) + displacement.getValueAtIndex(l));
					newUpper = newUpper.changeValueAtIndex(l, upper.getValueAtIndex(l) + displacement.getValueAtIndex(l));
				}
			}

			this.onMapAreaChange(new CuboidAddress(newLower, newUpper));
		}
	}

	public void onPlayerPositionChange(PlayerPositionXYZ newPosition) throws Exception{
		Coordinate newCoordinate = newPosition == null ? null : newPosition.getPosition();
		this.updateMapAreaForPlayerPositionChange(this.playerPosition, newCoordinate);

		Coordinate lastGameInterfacePosition = this.playerPosition;
		this.playerPosition = newCoordinate;

		this.drawBorders(); //  When frame coordinate decreases in size, clear outdated coordinate text.
		this.updateFrameCoordinate();
		this.onFinalizeFrame();
	}

	private void onMapAreaChange(CuboidAddress newMapArea) throws Exception{
		this.clientBlockModelContext.putWorkItem(new MapAreaChangeWorkItem(this.clientBlockModelContext, newMapArea), WorkItemPriority.PRIORITY_LOW);
		CuboidAddress previousMapArea = this.mapAreaCuboidAddress;
		this.mapAreaCuboidAddress = newMapArea;
		this.mapAreaBlocks.updateBufferRegion(newMapArea.getSubDimensions(0L, 3L));
		this.loadMapAreaBlocksFromMemory(newMapArea, previousMapArea);
		this.printMapAreaUpdates(newMapArea);
	}

	public void loadMapAreaBlocksFromMemory(CuboidAddress changedArea, CuboidAddress areaToExclude) throws Exception {
		CuboidAddress areaToUpdate = this.mapAreaCuboidAddress.getIntersectionCuboidAddress(changedArea);
		clientBlockModelContext.loadBlocksFromMemory(this.mapAreaBlocks, areaToUpdate, areaToExclude);
	}

	public void onUpdateMapAreaFlagsNotify(CuboidAddress areaToUpdate) throws Exception {
		this.loadMapAreaBlocksFromMemory(areaToUpdate, null);
		this.printMapAreaUpdates(areaToUpdate);
		this.onFinalizeFrame();
	}

	public void updateFrameCoordinate() throws Exception {
		String playerCoordinateString = this.getPlayerPosition() == null ? "null" : "X=" + this.getPlayerPosition().getX() + ", Y=" + this.getPlayerPosition().getY() + ", Z=" + this.getPlayerPosition().getZ() + ", W=" + this.getPlayerPosition().getValueAtIndex(3L);
		this.printTextAtScreenXY(new ColouredTextFragment(playerCoordinateString, UserInterfaceFrameThreadState.getDefaultTextColors()), 10L, 0L, true);
	}

	public Long getMapXOffsetInCells(CuboidAddress ca) throws Exception{
		return ca.getCanonicalLowerCoordinate().getX() - this.mapAreaCuboidAddress.getCanonicalLowerCoordinate().getX();
		
	}

	public Long getMapYOffsetInCells(CuboidAddress ca)throws Exception{
		return this.mapAreaCuboidAddress.getCanonicalUpperCoordinate().getZ() - ca.getCanonicalUpperCoordinate().getZ();
	}

	public Long getMapXOffsetInScreenCoordinates(CuboidAddress ca) throws Exception{
		return (this.getMapXOffsetInCells(ca) * this.getMapAreaCellWidth()) + this.getFrameCharacterWidth();
	}

	public Long getMapYOffsetInScreenCoordinates(CuboidAddress ca)throws Exception{
		return this.getMapYOffsetInCells(ca) + this.getFrameCharacterHeight();
	}

	public CuboidAddress getVisibleMapArea() throws Exception {
		//  The visible layer to iterate over is only the top of the 2 y layers
		//  that we keep track of the currently loaded map area:
		Coordinate lowerVisibleLayer = this.mapAreaCuboidAddress.getCanonicalLowerCoordinate().changeByDeltaXYZ(0L, 1L, 0L);
		Coordinate upperVisibleLayer = this.mapAreaCuboidAddress.getCanonicalUpperCoordinate();
		return new CuboidAddress(lowerVisibleLayer, upperVisibleLayer);
	}

	public CuboidAddress getChangeAreaExpandedAbove(CuboidAddress changedArea) throws Exception {
		//  When we get an update notification for a chunk, it might be because the 
		//  lower layer chunk loaded and it's 1 y layer below the layer of the player.
		//  Expand this chunk to include one y value above to update background colours
		//  and blocks for the loaded chunks in the layer below:
		Coordinate lowerAreaToUpdate = changedArea.getCanonicalLowerCoordinate();
		Coordinate upperAreaToUpdate = changedArea.getCanonicalUpperCoordinate().changeByDeltaXYZ(0L, 1L, 0L);
		return new CuboidAddress(lowerAreaToUpdate, upperAreaToUpdate);
	}

	public void printMapAreaUpdates(CuboidAddress changedArea) throws Exception {
		logger.info("Doing printMapAreaUpdates with mapArea cuboid as " + this.mapAreaCuboidAddress + ".");
		CuboidAddress visibleArea = this.getVisibleMapArea();
		CuboidAddress expandedChangedArea = this.getChangeAreaExpandedAbove(changedArea);
		CuboidAddress areaToUpdate = visibleArea.getIntersectionCuboidAddress(expandedChangedArea);

		GraphicsMode mode = blockManagerThreadCollection.getGraphicsMode();
		boolean useASCII = mode.equals(GraphicsMode.ASCII);
		Long areaToUpdateWidth = areaToUpdate.getWidthForIndex(0L);
		Long areaToUpdateHeight = areaToUpdate.getWidthForIndex(2L);
		String [][] updatedCellContents = new String [areaToUpdateWidth.intValue()][areaToUpdateHeight.intValue()];
		int [][][] updatedBackgroundColours = new int [areaToUpdateWidth.intValue()][areaToUpdateHeight.intValue()][];

		RegionIteration regionIteration = new RegionIteration(areaToUpdate.getCanonicalLowerCoordinate(), areaToUpdate);
		while(!regionIteration.isDone()){
			Coordinate currentMapAreaCoordinate = regionIteration.getCurrentCoordinate();
			Coordinate underBlockCoordinate = currentMapAreaCoordinate.changeByDeltaXYZ(0L, -1L, 0L);
			Long xCellOffset = currentMapAreaCoordinate.getX() - this.mapAreaCuboidAddress.getCanonicalLowerCoordinate().getX();
			Long yCellOffset = (this.mapAreaCuboidAddress.getWidthForIndex(2L) -1L) - (currentMapAreaCoordinate.getZ() - this.mapAreaCuboidAddress.getCanonicalLowerCoordinate().getZ());

			IndividualBlock currentMapAreaCell = this.mapAreaBlocks.getObjectAtCoordinate(currentMapAreaCoordinate);
			IndividualBlock underBlock = this.mapAreaBlocks.getObjectAtCoordinate(underBlockCoordinate);

			//  If the block underneath is loading, don't display the block on top of it, display the in progress block underneath:
			boolean overStillLoadingBlock = underBlock instanceof UninitializedBlock || underBlock instanceof PendingLoadBlock;

			//logger.info("printMapAreaUpdates() for " + currentMapAreaCoordinate + " xCellOffset=" + xCellOffset + " yCellOffset=" + yCellOffset);

			boolean overSolidBlock = !(
				underBlock instanceof EmptyBlock ||
				underBlock instanceof UninitializedBlock ||
				underBlock instanceof PendingLoadBlock
			);
			boolean overExcitingBlock = overSolidBlock && !(
				underBlock instanceof Rock
			);

			String stringToWrite = overStillLoadingBlock ? BlockSkins.getPresentation(underBlock, useASCII) : BlockSkins.getPresentation(currentMapAreaCell, useASCII);
			int [] colourCodes = UserInterfaceFrameThreadState.getColourCodesForMapCell(useASCII, overStillLoadingBlock, overExcitingBlock, overSolidBlock);

			Long xCellOffsetInUpdateArea = currentMapAreaCoordinate.getX() - areaToUpdate.getCanonicalLowerCoordinate().getX();
			Long yCellOffsetInUpdateArea = (areaToUpdateHeight -1L) -(currentMapAreaCoordinate.getZ() - areaToUpdate.getCanonicalLowerCoordinate().getZ());
			updatedCellContents[xCellOffsetInUpdateArea.intValue()][yCellOffsetInUpdateArea.intValue()] = stringToWrite;
			updatedBackgroundColours[xCellOffsetInUpdateArea.intValue()][yCellOffsetInUpdateArea.intValue()] = colourCodes;
			regionIteration.incrementCoordinateWithinCuboidAddress();
		}

		Long screenDrawX = (this.getMapXOffsetInCells(areaToUpdate) * this.getMapAreaCellWidth()) + this.getFrameCharacterWidth();
		Long screenDrawY = this.getMapYOffsetInCells(areaToUpdate) + this.getFrameCharacterHeight();

		this.sendCellUpdatesInScreenArea(
			areaToUpdate,
			updatedCellContents,
			updatedBackgroundColours,
			this.getMapXOffsetInScreenCoordinates(areaToUpdate),
			this.getMapYOffsetInScreenCoordinates(areaToUpdate),
			new ScreenLayerMergeParameters(
				this.bufferedScreenLayers[ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT],
				ScreenLayerMergeType.PREFER_BOTTOM_LAYER
			)
		);
	}

	private Coordinate getPlayerPosition(){
		return this.playerPosition;
	}

	public UIWorkItem takeWorkItem() throws Exception {
		UIWorkItem workItem = this.workItemQueue.takeWorkItem();
		return workItem;
	}

	public void putWorkItem(MapAreaWorkItem workItem, WorkItemPriority priority) throws Exception{
		this.workItemQueue.putWorkItem(workItem, priority);
	}

	public void putWorkItem(UIWorkItem workItem, WorkItemPriority priority) throws Exception{
		this.workItemQueue.putWorkItem(workItem, priority);
	}

	public boolean doBackgroundProcessing() throws Exception{
		return false;
	}

	public void onUIEventNotification(Object o, UINotificationType notificationType) throws Exception{
		switch(notificationType){
			case PLAYER_POSITION:{
				this.onPlayerPositionChange((PlayerPositionXYZ)o);
				break;
			}case UPDATE_MAP_AREA_FLAGS:{
				this.onUpdateMapAreaFlagsNotify((CuboidAddress)o);
				break;
			}default:{
				throw new Exception("Unknown event notification type: " + notificationType);
			}
		}
	}
}
