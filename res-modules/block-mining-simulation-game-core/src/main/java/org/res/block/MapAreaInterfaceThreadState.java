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
	private Long edgeDistanceScreenX = 10L;
	private Long edgeDistanceScreenY = 10L;

	private byte[] unprocessedInputBytes = new byte[0];

	public MapAreaInterfaceThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception {
		super(blockManagerThreadCollection, clientBlockModelContext, new int [] {ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT, ConsoleWriterThreadState.BUFFER_INDEX_OVERLAY});
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
	}

	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		if(ansiEscapeSequence instanceof AnsiEscapeSequenceUpArrowKey){
			this.onTryPositionChange(0L, 0L, 1L, true);
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceRightArrowKey){
			this.onTryPositionChange(1L, 0L, 0L, true);
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceDownArrowKey){
			this.onTryPositionChange(0L, 0L, -1L, true);
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceLeftArrowKey){
			this.onTryPositionChange(-1L, 0L, 0L, true);
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
						this.onTryPositionChange(0L, 0L, 1L, is_last);
						break;
					}case ACTION_X_MINUS:{
						this.onTryPositionChange(-1L, 0L, 0L, is_last);
						break;
					}case ACTION_Y_MINUS:{
						this.onTryPositionChange(0L, 0L, -1L, is_last);
						break;
					}case ACTION_X_PLUS:{
						this.onTryPositionChange(1L, 0L, 0L, is_last);
						break;
					}case ACTION_Z_MINUS:{
						this.onTryPositionChange(0L, -1L, 0L, is_last);
						break;
					}case ACTION_Z_PLUS:{
						this.onTryPositionChange(0L, 1L, 0L, is_last);
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

	public void onTryPositionChange(Long deltaX, Long deltaY, Long deltaZ, boolean is_last) throws Exception{
		Coordinate currentPosition = this.getPlayerPosition();
		if(currentPosition != null){
			Coordinate newCandiatePosition = currentPosition.copy().changeByDeltaXYZ(deltaX, deltaY, deltaZ);

			IndividualBlock blockAtCandidatePlayerPosition = clientBlockModelContext.readBlockAtCoordinate(newCandiatePosition);
			if(blockAtCandidatePlayerPosition == null){
				logger.info("Not moving to " + newCandiatePosition + " because block at that location is not loaded by client yet and we don't know what's there.");
			}else{
				boolean disableCollisionDetection = false;
				if(
					disableCollisionDetection ||
					blockAtCandidatePlayerPosition instanceof EmptyBlock
				){
					this.onPlayerPositionChange(currentPosition, newCandiatePosition);
				}else{
					//  Don't move, there is something in the way.
					logger.info("Not moving to " + newCandiatePosition + " because block at that location has class '" + blockAtCandidatePlayerPosition.getClass().getName() + "'");
				}
			}
		}
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public void onRenderFrame(boolean hasThisFrameDimensionsChanged, boolean hasOtherFrameDimensionsChanged) throws Exception{
		if(hasThisFrameDimensionsChanged){
			this.clearFrame();
			FrameDimensions currentFrameDimensions = new FrameDimensions(this.getFrameDimensions());
			Long totalXBorderSize = this.getTotalXBorderSize();
			Long totalYBorderSize = this.getTotalYBorderSize();
			Long printableInnerWidth = this.getFrameWidth() - totalXBorderSize;
			this.mapAreaWidthInCells = printableInnerWidth / this.getMapAreaCellWidth();
			this.mapAreaHeightInCells = this.getFrameHeight() - totalYBorderSize;
			//  Don't allow the map area to negative when the window is resized very small:
			this.mapAreaWidthInCells = mapAreaWidthInCells < 1L ? 1L : mapAreaWidthInCells;
			this.mapAreaHeightInCells = mapAreaHeightInCells < 1L ? 1L : mapAreaHeightInCells;
			this.mapAreaPaddingColumnsRight = printableInnerWidth - (this.mapAreaWidthInCells * this.getMapAreaCellWidth());
			logger.info("onRenderFrame calculated: mapAreaWidthInCells=" + mapAreaWidthInCells + ", mapAreaHeightInCells=" + mapAreaHeightInCells);
			Long topRightHandX = mapAreaWidthInCells / 2L;
			Long topRightHandZ = mapAreaHeightInCells / 2L;
			Long bottomLeftHandX = topRightHandX - (mapAreaWidthInCells - 1L);
			Long bottomLeftHandZ = topRightHandZ - (mapAreaHeightInCells - 1L);
			logger.info("onRenderFrame calculated: topRightHandX=" + topRightHandX + ", topRightHandZ=" + topRightHandZ + ", bottomLeftHandX=" + bottomLeftHandX + ", bottomLeftHandZ=" + bottomLeftHandZ);

			Coordinate bottomleftHandCorner = new Coordinate(Arrays.asList(bottomLeftHandX + playerPosition.getX(), playerPosition.getY() -1L, bottomLeftHandZ + playerPosition.getZ(), 0L));
			Coordinate topRightHandCorner = new Coordinate(Arrays.asList(topRightHandX + playerPosition.getX(), playerPosition.getY(), topRightHandZ + playerPosition.getZ(), 0L));

			CuboidAddress newMapArea = new CuboidAddress(bottomleftHandCorner, topRightHandCorner.add(Coordinate.makeUnitCoordinate(4L)));
			this.onMapAreaChange(newMapArea);

		}else if(hasOtherFrameDimensionsChanged){
			this.clearFrame();
			this.onMapAreaChange(this.mapAreaCuboidAddress); // Refresh map area
		}

		this.reprintFrame();
	}

	public void updateMapAreaForPlayerPositionChange(Coordinate previousPosition, Coordinate newPosition) throws Exception{
		if(previousPosition == null){
		}else{
			Long deltaX = newPosition.getX() - previousPosition.getX();
			Long deltaY = newPosition.getY() - previousPosition.getY();
			Long deltaZ = newPosition.getZ() - previousPosition.getZ();

			/*  Move camera around if the player tries to move out of bounds. */
			Coordinate lower = this.mapAreaCuboidAddress.getCanonicalLowerCoordinate();
			Coordinate upper = this.mapAreaCuboidAddress.getCanonicalUpperCoordinate();

			CuboidAddress mapAreaAfter = this.mapAreaCuboidAddress.copy();

			if((deltaX < 0L) && (newPosition.getX() - lower.getValueAtIndex(0L) < this.edgeDistanceScreenX)){
				Coordinate newLower = lower.changeValueAtIndex(0L, lower.getValueAtIndex(0L) + deltaX);
				Coordinate newUpper = upper.changeValueAtIndex(0L, upper.getValueAtIndex(0L) + deltaX);
				mapAreaAfter = new CuboidAddress(newLower, newUpper);
			}
			if((deltaX > 0L) && (upper.getValueAtIndex(0L) - 1L - newPosition.getX() < this.edgeDistanceScreenX)){
				Coordinate newLower = lower.changeValueAtIndex(0L, lower.getValueAtIndex(0L) + deltaX);
				Coordinate newUpper = upper.changeValueAtIndex(0L, upper.getValueAtIndex(0L) + deltaX);
				mapAreaAfter = new CuboidAddress(newLower, newUpper);
			}

			if(deltaY != 0L){
				Coordinate newLower = lower.changeValueAtIndex(1L, lower.getValueAtIndex(1L) + deltaY);
				Coordinate newUpper = upper.changeValueAtIndex(1L, upper.getValueAtIndex(1L) + deltaY);
				mapAreaAfter = new CuboidAddress(newLower, newUpper);
			}

			if((deltaZ < 0L) && (newPosition.getZ() - lower.getValueAtIndex(2L) < this.edgeDistanceScreenY)){
				Coordinate newLower = lower.changeValueAtIndex(2L, lower.getValueAtIndex(2L) + deltaZ);
				Coordinate newUpper = upper.changeValueAtIndex(2L, upper.getValueAtIndex(2L) + deltaZ);
				mapAreaAfter = new CuboidAddress(newLower, newUpper);
			}

			if((deltaZ > 0L) && (upper.getValueAtIndex(2L) - 1L - newPosition.getZ() < this.edgeDistanceScreenY)){
				Coordinate newLower = lower.changeValueAtIndex(2L, lower.getValueAtIndex(2L) + deltaZ);
				Coordinate newUpper = upper.changeValueAtIndex(2L, upper.getValueAtIndex(2L) + deltaZ);
				mapAreaAfter = new CuboidAddress(newLower, newUpper);
			}
			this.onMapAreaChange(mapAreaAfter);
		}
	}

	public void onPlayerPositionChange(Coordinate previousPosition, Coordinate newPosition) throws Exception{
		this.updateMapAreaForPlayerPositionChange(this.playerPosition, newPosition);
		if(this.playerPosition != null){
			if(!this.playerPosition.equals(previousPosition)){
				logger.info("Client player position " + (previousPosition == null ? "null" : previousPosition) + " was out of sync with game interface player position: " + this.playerPosition + ".  I don't think this is a problem right now, but log this in case it's important in the future.");
			}
		}

		//logger.info("Changed player position from " + (this.playerPosition == null ? "null" : this.playerPosition.toString()) + " to " + newPosition);
		this.clientBlockModelContext.putWorkItem(new ClientNotifyPlayerPositionChangeWorkItem(this.clientBlockModelContext, this.playerPosition == null ? null : this.playerPosition.copy(), newPosition.copy()), WorkItemPriority.PRIORITY_LOW);
		Coordinate lastGameInterfacePosition = this.playerPosition;
		this.playerPosition = newPosition;

		if(lastGameInterfacePosition != null){
			this.updatePlayerOverlay(new CuboidAddress(lastGameInterfacePosition, lastGameInterfacePosition.add(Coordinate.makeUnitCoordinate(4L))), false);
		}
		if(newPosition != null){
			this.updatePlayerOverlay(new CuboidAddress(newPosition, newPosition.add(Coordinate.makeUnitCoordinate(4L))), true);
		}

		this.reprintFrame();
		this.onFinalizeFrame();
	}

	private void onMapAreaChange(CuboidAddress newMapArea) throws Exception{
		this.clientBlockModelContext.putWorkItem(new MapAreaChangeWorkItem(this.clientBlockModelContext, newMapArea), WorkItemPriority.PRIORITY_LOW);
		CuboidAddress previousMapArea = this.mapAreaCuboidAddress;
		this.mapAreaCuboidAddress = newMapArea;
		this.updatePlayerOverlay(new CuboidAddress(this.playerPosition, this.playerPosition.add(Coordinate.makeUnitCoordinate(4L))), true);

		this.mapAreaBlocks.updateBufferRegion(newMapArea.getSubDimensions(0L, 3L));
		this.loadMapAreaBlocksFromMemory(newMapArea, previousMapArea);
		this.printMapAreaUpdates(newMapArea);
	}

	public void loadMapAreaBlocksFromMemory(CuboidAddress areaToUpdate, CuboidAddress areaToExclude) throws Exception {
		logger.info("Doing loadMapAreaBlocksFromMemory for area " + areaToUpdate + " with mapArea cuboid as " + this.mapAreaCuboidAddress + ".");
	
		RegionIteration regionIteration = new RegionIteration(areaToUpdate.getCanonicalLowerCoordinate(), areaToUpdate);
		do{
			Coordinate currentMapAreaCoordinate = regionIteration.getCurrentCoordinate();
			if(
				(areaToExclude == null || (!areaToExclude.containsCoordinate(currentMapAreaCoordinate))) &&
				this.mapAreaCuboidAddress.containsCoordinate(currentMapAreaCoordinate)
			){
				IndividualBlock b = clientBlockModelContext.readBlockAtCoordinate(currentMapAreaCoordinate);
				if(b != null){ /* Chunk not even loaded. */
					this.mapAreaBlocks.setObjectAtCoordinate(currentMapAreaCoordinate, b);
				}
			}
		}while (regionIteration.incrementCoordinateWithinCuboidAddress());
	}

	public void onUpdateMapAreaFlagsNotify(CuboidAddress areaToUpdate) throws Exception {
		this.loadMapAreaBlocksFromMemory(areaToUpdate, null);
		this.printMapAreaUpdates(areaToUpdate);
		this.onFinalizeFrame();
	}

	public void reprintFrame() throws Exception {
		this.drawBorders();
		//  Player coordinate:
		String playerCoordinateString = "X=" + this.getPlayerPosition().getX() + ", Y=" + this.getPlayerPosition().getY() + ", Z=" + this.getPlayerPosition().getZ();
		this.printTextAtScreenXY(new ColouredTextFragment(playerCoordinateString, new int [] {DEFAULT_TEXT_BG_COLOR, DEFAULT_TEXT_FG_COLOR}), 10L, 0L, true);
	}

	public void updatePlayerOverlay(CuboidAddress playerPositionCA, boolean isPlayerLocation) throws Exception{
		GraphicsMode mode = blockManagerThreadCollection.getGraphicsMode();
		boolean useASCII = mode.equals(GraphicsMode.ASCII);

		String [][] currentPositionContents = new String [1][1];
		String playerIcon = useASCII ? "P" : "\uD83D\uDE0A";
		currentPositionContents[0][0] = isPlayerLocation ? playerIcon : null;
		int [][][] currentPositionColours = new int [1][1][0];
		currentPositionColours[0][0] = isPlayerLocation ? new int [] {PLAYER_BG_COLOR} : new int [] {};

		this.sendCellUpdatesInScreenArea(
			playerPositionCA,
			currentPositionContents,
			currentPositionColours,
			this.getMapXOffsetInScreenCoordinates(playerPositionCA),
			this.getMapYOffsetInScreenCoordinates(playerPositionCA),
			ConsoleWriterThreadState.BUFFER_INDEX_OVERLAY
		);
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
		
	public void printMapAreaUpdates(CuboidAddress areaToUpdate) throws Exception {
		logger.info("Doing printMapAreaUpdates with mapArea cuboid as " + this.mapAreaCuboidAddress + ".");


		GraphicsMode mode = blockManagerThreadCollection.getGraphicsMode();
		boolean useASCII = mode.equals(GraphicsMode.ASCII);
		Long areaToUpdateWidth = areaToUpdate.getWidthForIndex(0L);
		Long areaToUpdateHeight = areaToUpdate.getWidthForIndex(2L);
		Long mapAreaWidthInCells = this.mapAreaCuboidAddress.getWidthForIndex(0L);
		Long mapAreaHeightInCells = this.mapAreaCuboidAddress.getWidthForIndex(2L);
		String [][] updatedCellContents = new String [areaToUpdateWidth.intValue()][areaToUpdateHeight.intValue()];
		int [][][] updatedBackgroundColours = new int [areaToUpdateWidth.intValue()][areaToUpdateHeight.intValue()][2];

		//  Expand the 'areaToUpate' to include one y value above to update background colours for the loaded chunks in the layer below
		Coordinate lowerAreaToUpdate = areaToUpdate.getCanonicalLowerCoordinate();
		Coordinate upperAreaToUpdate = areaToUpdate.getCanonicalUpperCoordinate().changeByDeltaXYZ(0L, 1L, 0L);
		CuboidAddress expandedAreaToUpdate = new CuboidAddress(lowerAreaToUpdate, upperAreaToUpdate);
		//  The visible layer to iterate over is only the top of the 2 y layers:
		Coordinate lowerVisibleLayer = this.mapAreaCuboidAddress.getCanonicalLowerCoordinate().changeByDeltaXYZ(0L, 1L, 0L);
		Coordinate upperVisibleLayer = this.mapAreaCuboidAddress.getCanonicalUpperCoordinate();
		CuboidAddress visibleArea = new CuboidAddress(lowerVisibleLayer, upperVisibleLayer);
		RegionIteration regionIteration = new RegionIteration(expandedAreaToUpdate.getCanonicalLowerCoordinate(), expandedAreaToUpdate);
		do{
			Coordinate currentMapAreaCoordinate = regionIteration.getCurrentCoordinate();
			Coordinate underBlockCoordinate = currentMapAreaCoordinate.changeByDeltaXYZ(0L, -1L, 0L);
			if(visibleArea.containsCoordinate(currentMapAreaCoordinate)){
				Long xCellOffset = currentMapAreaCoordinate.getX() - this.mapAreaCuboidAddress.getCanonicalLowerCoordinate().getX();
				Long yCellOffset = (this.mapAreaCuboidAddress.getWidthForIndex(2L) -1L) - (currentMapAreaCoordinate.getZ() - this.mapAreaCuboidAddress.getCanonicalLowerCoordinate().getZ());

				IndividualBlock currentMapAreaCell = this.mapAreaBlocks.getObjectAtCoordinate(currentMapAreaCoordinate);
				IndividualBlock underBlock = this.mapAreaBlocks.getObjectAtCoordinate(underBlockCoordinate);

				//logger.info("printMapAreaUpdates() for " + currentMapAreaCoordinate + " xCellOffset=" + xCellOffset + " yCellOffset=" + yCellOffset);

				boolean overSolidBlock = !(
					underBlock instanceof EmptyBlock ||
					underBlock instanceof UninitializedBlock ||
					underBlock instanceof PendingLoadBlock
				);
				boolean overExcitingBlock = overSolidBlock && !(
					underBlock instanceof Rock
				);
				int underBlockColour = useASCII ? MAGENTA_BG_COLOR : GRAY_BG_COLOR;
				int emptyBlockBGColour = useASCII ? BLACK_BG_COLOR : MAP_CELL_BG_COLOR2;
				int loadingBlockBGColour = useASCII ? BLACK_BG_COLOR : MAP_CELL_BG_COLOR2;
				int blockBGColour = overSolidBlock ? underBlockColour : emptyBlockBGColour;
				blockBGColour = overExcitingBlock ? RED_BG_COLOR : blockBGColour;

				String stringToWrite = BlockSkins.getPresentation(currentMapAreaCell.getClass(), useASCII);
				if(
					//  If the block underneath is loading, don't display the block on top of it, display the in progress block underneath:
					underBlock instanceof UninitializedBlock ||
					underBlock instanceof PendingLoadBlock
				){
					stringToWrite = BlockSkins.getPresentation(underBlock.getClass(), useASCII);
					blockBGColour = loadingBlockBGColour;
				}

				int backgroundColour = blockBGColour;
				int textColour = useASCII ? YELLOW_FG_COLOR : DEFAULT_TEXT_FG_COLOR;

				Long xCellOffsetInUpdateArea = currentMapAreaCoordinate.getX() - areaToUpdate.getCanonicalLowerCoordinate().getX();
				Long yCellOffsetInUpdateArea = (areaToUpdateHeight -1L) -(currentMapAreaCoordinate.getZ() - areaToUpdate.getCanonicalLowerCoordinate().getZ());
				updatedCellContents[xCellOffsetInUpdateArea.intValue()][yCellOffsetInUpdateArea.intValue()] = stringToWrite;
				updatedBackgroundColours[xCellOffsetInUpdateArea.intValue()][yCellOffsetInUpdateArea.intValue()][0] = backgroundColour;
				updatedBackgroundColours[xCellOffsetInUpdateArea.intValue()][yCellOffsetInUpdateArea.intValue()][1] = textColour;
			}
		}while (regionIteration.incrementCoordinateWithinCuboidAddress());

		Long screenDrawX = (this.getMapXOffsetInCells(areaToUpdate) * this.getMapAreaCellWidth()) + this.getFrameCharacterWidth();
		Long screenDrawY = this.getMapYOffsetInCells(areaToUpdate) + this.getFrameCharacterHeight();
	
		this.sendCellUpdatesInScreenArea(
			areaToUpdate,
			updatedCellContents,
			updatedBackgroundColours,
			this.getMapXOffsetInScreenCoordinates(areaToUpdate),
			this.getMapYOffsetInScreenCoordinates(areaToUpdate),
			ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT
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
}
