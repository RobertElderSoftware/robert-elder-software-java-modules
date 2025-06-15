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
	private Long mapAreaWidthInCells;
	//  Padding columns on right edge of map area where only part of a cell could fit:
	private Long mapAreaPaddingColumnsRight;
	private Long mapAreaHeightInCells;
	private Coordinate playerPosition;
	private CuboidAddress mapAreaCuboidAddress;
	private Set<Coordinate> stalePlayerPositions = new HashSet<Coordinate>();
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;

	private ClientBlockModelContext clientBlockModelContext;

	private MapAreaCell[][] mapAreaCells = null;
	private Long edgeDistanceScreenX = 10L;
	private Long edgeDistanceScreenY = 10L;

	private byte[] unprocessedInputBytes = new byte[0];

	public MapAreaInterfaceThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception {
		super(blockManagerThreadCollection, clientBlockModelContext, new int [] {ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT});
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

	public void onRenderFrame(boolean requiresRefresh) throws Exception{
		if(requiresRefresh){
			this.clearFrame();
		}
		FrameDimensions currentFrameDimensions = this.getFrameDimensions() == null ? null : new FrameDimensions(this.getFrameDimensions());
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

		Coordinate bottomleftHandCorner = new Coordinate(Arrays.asList(bottomLeftHandX + playerPosition.getX(), playerPosition.getY(), bottomLeftHandZ + playerPosition.getZ(), 0L));
		Coordinate topRightHandCorner = new Coordinate(Arrays.asList(topRightHandX + playerPosition.getX(), playerPosition.getY(), topRightHandZ + playerPosition.getZ(), 0L));

		CuboidAddress newMapArea = new CuboidAddress(bottomleftHandCorner, topRightHandCorner);

		this.onMapAreaChange(newMapArea);
		this.render();
	}

	public void render() throws Exception{
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

			CuboidAddress mapAreaBefore = this.mapAreaCuboidAddress.copy();
			CuboidAddress mapAreaAfter = this.mapAreaCuboidAddress.copy();

			if((deltaX < 0L) && (newPosition.getX() - lower.getValueAtIndex(0L) < this.edgeDistanceScreenX)){
				Coordinate newLower = lower.changeValueAtIndex(0L, lower.getValueAtIndex(0L) - 1L);
				Coordinate newUpper = upper.changeValueAtIndex(0L, upper.getValueAtIndex(0L) - 1L);
				mapAreaAfter = new CuboidAddress(newLower, newUpper);
			}
			if((deltaX > 0L) && (upper.getValueAtIndex(0L) - newPosition.getX() < this.edgeDistanceScreenX)){
				Coordinate newLower = lower.changeValueAtIndex(0L, lower.getValueAtIndex(0L) + 1L);
				Coordinate newUpper = upper.changeValueAtIndex(0L, upper.getValueAtIndex(0L) + 1L);
				mapAreaAfter = new CuboidAddress(newLower, newUpper);
			}

			if(deltaY != 0L){
				Coordinate newLower = lower.changeValueAtIndex(1L, lower.getValueAtIndex(1L) + deltaY);
				Coordinate newUpper = upper.changeValueAtIndex(1L, upper.getValueAtIndex(1L) + deltaY);
				mapAreaAfter = new CuboidAddress(newLower, newUpper);
			}

			if((deltaZ < 0L) && (newPosition.getZ() - lower.getValueAtIndex(2L) < this.edgeDistanceScreenY)){
				Coordinate newLower = lower.changeValueAtIndex(2L, lower.getValueAtIndex(2L) - 1L);
				Coordinate newUpper = upper.changeValueAtIndex(2L, upper.getValueAtIndex(2L) - 1L);
				mapAreaAfter = new CuboidAddress(newLower, newUpper);
			}

			if((deltaZ > 0L) && (upper.getValueAtIndex(2L) - newPosition.getZ() < this.edgeDistanceScreenY)){
				Coordinate newLower = lower.changeValueAtIndex(2L, lower.getValueAtIndex(2L) + 1L);
				Coordinate newUpper = upper.changeValueAtIndex(2L, upper.getValueAtIndex(2L) + 1L);
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

		if(previousPosition != null){
			this.stalePlayerPositions.add(this.playerPosition);
		}
		if(newPosition != null){
			this.stalePlayerPositions.add(newPosition);
		}

		//logger.info("Changed player position from " + (this.playerPosition == null ? "null" : this.playerPosition.toString()) + " to " + newPosition);
		this.clientBlockModelContext.putWorkItem(new ClientNotifyPlayerPositionChangeWorkItem(this.clientBlockModelContext, this.playerPosition == null ? null : this.playerPosition.copy(), newPosition.copy()), WorkItemPriority.PRIORITY_LOW);
		Coordinate lastGameInterfacePosition = this.playerPosition;
		this.playerPosition = newPosition;

		if(lastGameInterfacePosition != null){
			this.updateMapAreaFlags(new CuboidAddress(lastGameInterfacePosition, lastGameInterfacePosition));
		}
		if(newPosition != null){
			this.updateMapAreaFlags(new CuboidAddress(newPosition, newPosition));
		}

		this.reprintFrame();
		this.onFinalizeFrame();
	}

	private void resizeMapAreaCells() throws Exception{
		this.mapAreaCells = new MapAreaCell[this.mapAreaWidthInCells.intValue()][this.mapAreaHeightInCells.intValue()];
		for(int i = 0; i < this.mapAreaWidthInCells.intValue(); i++){
			for(int j = 0; j < this.mapAreaHeightInCells.intValue(); j++){
				this.mapAreaCells[i][j] = new MapAreaCell();
			}
		}
	}

	private void onMapAreaChange(CuboidAddress newMapArea) throws Exception{
		this.clientBlockModelContext.putWorkItem(new MapAreaChangeWorkItem(this.clientBlockModelContext, newMapArea), WorkItemPriority.PRIORITY_LOW);
		CuboidAddress previousMapArea = this.mapAreaCuboidAddress;
		this.mapAreaCuboidAddress = newMapArea;

		//logger.info("Game area changed from " + (this.mapAreaCuboidAddress == null ? "null" : this.mapAreaCuboidAddress) + " to " + newMapArea);

		if(previousMapArea == null){
			this.resizeMapAreaCells();
			this.updateMapAreaFlags(newMapArea);
			this.printMapAreaUpdates(newMapArea);
		}else{
			Coordinate beforeLower = previousMapArea.getCanonicalLowerCoordinate();
			Coordinate beforeUpper = previousMapArea.getCanonicalUpperCoordinate();
			Coordinate afterLower = newMapArea.getCanonicalLowerCoordinate();
			Coordinate afterUpper = newMapArea.getCanonicalUpperCoordinate();
			
			Coordinate diffLower = afterLower.subtract(beforeLower);
			Coordinate diffUpper = afterUpper.subtract(beforeUpper);
			if(diffLower.equals(diffUpper)){
				Long diffX = diffLower.getX();
				Long diffY = diffLower.getY();
				Long diffZ = diffLower.getZ();

				/* MapArea flags can be changed by just shifting the array around: */
				if(diffX >= 0){
					//  Move all cells to the left by diffX
					for(int i = 0; i < (this.mapAreaWidthInCells.intValue() - diffX.intValue()); i++){
						for(int j = 0; j < this.mapAreaHeightInCells.intValue(); j++){
							this.mapAreaCells[i][j] = this.mapAreaCells[i + diffX.intValue()][j];
						}
					}
					//  Initialize new cells in the empty space
					for(int i = (this.mapAreaWidthInCells.intValue() - diffX.intValue()); i < this.mapAreaWidthInCells.intValue(); i++){
						for(int j = 0; j < this.mapAreaHeightInCells.intValue(); j++){
							this.mapAreaCells[i][j] = new MapAreaCell();
							Coordinate theCoordinate = afterLower.changeByDeltaXYZ(Long.valueOf(i), 0L, (this.mapAreaCuboidAddress.getWidthForIndex(2L) - 1L) - Long.valueOf(j));
							this.recalculateMapAreaCellsAtCoordinate(theCoordinate);
						}
					}

					this.printMapAreaUpdates(this.mapAreaCuboidAddress);
				}else if(diffX < 0){
					//  Move all cells to the right by diffX
					for(int i = this.mapAreaWidthInCells.intValue() -1; i >= (-diffX.intValue()); i--){
						for(int j = 0; j < this.mapAreaHeightInCells.intValue(); j++){
							this.mapAreaCells[i][j] = this.mapAreaCells[i + diffX.intValue()][j];
						}
					}
					//  Initialize new cells in the empty space
					for(int i = (-diffX.intValue()) -1; i >= 0; i--){
						for(int j = 0; j < this.mapAreaHeightInCells.intValue(); j++){
							this.mapAreaCells[i][j] = new MapAreaCell();
							Coordinate theCoordinate = afterLower.changeByDeltaXYZ(Long.valueOf(i), 0L, (this.mapAreaCuboidAddress.getWidthForIndex(2L) - 1L) - Long.valueOf(j));
							this.recalculateMapAreaCellsAtCoordinate(theCoordinate);
						}
					}
					this.printMapAreaUpdates(this.mapAreaCuboidAddress);
				}

				if(!diffY.equals(0L)){
					//  Just update all mapArea flags when go to a different level:
					this.updateMapAreaFlags(this.mapAreaCuboidAddress);
				}

				if(diffZ >= 0){
					//  Move all cells down by diffZ (up in mapArea cell array)
					for(int i = 0; i < this.mapAreaWidthInCells.intValue(); i++){
						for(int j = this.mapAreaHeightInCells.intValue() -1; j >= diffZ.intValue(); j--){
							this.mapAreaCells[i][j] = this.mapAreaCells[i][j - diffZ.intValue()];
						}
					}
					//  Initialize new cells in the empty space
					for(int i = 0; i < this.mapAreaWidthInCells.intValue(); i++){
						for(int j = 0; j < diffZ.intValue(); j++){
							this.mapAreaCells[i][j] = new MapAreaCell();
							Coordinate theCoordinate = afterLower.changeByDeltaXYZ(Long.valueOf(i), 0L, (this.mapAreaCuboidAddress.getWidthForIndex(2L) - 1L) - Long.valueOf(j));
							this.recalculateMapAreaCellsAtCoordinate(theCoordinate);
						}
					}
					this.printMapAreaUpdates(this.mapAreaCuboidAddress);
				}else if(diffZ < 0){
					//  Move all cells up by diffZ (down in mapArea cell array)
					for(int i = 0; i < this.mapAreaWidthInCells.intValue(); i++){
						for(int j = 0; j < this.mapAreaHeightInCells.intValue() + diffZ.intValue(); j++){
							this.mapAreaCells[i][j] = this.mapAreaCells[i][j - diffZ.intValue()];
						}
					}
					//  Initialize new cells in the empty space
					for(int i = 0; i < this.mapAreaWidthInCells.intValue(); i++){
						for(int j = this.mapAreaHeightInCells.intValue() + diffZ.intValue(); j < this.mapAreaHeightInCells.intValue(); j++){
							this.mapAreaCells[i][j] = new MapAreaCell();
							Coordinate theCoordinate = afterLower.changeByDeltaXYZ(Long.valueOf(i), 0L, (this.mapAreaCuboidAddress.getWidthForIndex(2L) - 1L) - Long.valueOf(j));
							this.recalculateMapAreaCellsAtCoordinate(theCoordinate);
						}
					}
					this.printMapAreaUpdates(this.mapAreaCuboidAddress);
				}
			}else{
				this.resizeMapAreaCells();
				//  This case happens when terminal window is resized.  Just redraw everything for now:
				this.updateMapAreaFlags(this.mapAreaCuboidAddress);
				this.printMapAreaUpdates(this.mapAreaCuboidAddress);
			}
		}
		this.onFinalizeFrame();
	}

	public void recalculateMapAreaCellsAtCoordinate(Coordinate currentMapAreaCoordinate) throws Exception {
		IndividualBlock b = clientBlockModelContext.readBlockAtCoordinate(currentMapAreaCoordinate);
		Long xCellOffset = currentMapAreaCoordinate.getX() - this.mapAreaCuboidAddress.getCanonicalLowerCoordinate().getX();
		Long yCellOffset = (this.mapAreaCuboidAddress.getWidthForIndex(2L) - 1L) - (currentMapAreaCoordinate.getZ() - this.mapAreaCuboidAddress.getCanonicalLowerCoordinate().getZ());
		if(xCellOffset < this.mapAreaWidthInCells && yCellOffset < this.mapAreaHeightInCells){
			MapAreaCell currentMapAreaCell = this.mapAreaCells[xCellOffset.intValue()][yCellOffset.intValue()];

			if(b != null){ /* Chunk not even loaded. */
				currentMapAreaCell.setCurrentBlock(b);
			}
		}else{
			logger.info("Saw xCellOffset=" + xCellOffset + " < this.mapAreaWidthInCells=" + this.mapAreaWidthInCells + " || yCellOffset=" + yCellOffset + " < this.mapAreaHeightInCells=" + this.mapAreaHeightInCells + ".  TODO: Fix this error case with a more robust design.");
		}
	}

	public void updateMapAreaFlags(CuboidAddress areaToUpdate) throws Exception {
		if(this.mapAreaCuboidAddress != null){
			logger.info("Doing updateMapAreaFlags for area " + areaToUpdate + " with mapArea cuboid as " + this.mapAreaCuboidAddress + ".");
		
			RegionIteration regionIteration = new RegionIteration(areaToUpdate.getCanonicalLowerCoordinate(), areaToUpdate);
			do{
				Coordinate currentMapAreaCoordinate = regionIteration.getCurrentCoordinate();
				if(this.mapAreaCuboidAddress.containsCoordinate(currentMapAreaCoordinate)){
					this.recalculateMapAreaCellsAtCoordinate(currentMapAreaCoordinate);
				}
			}while (regionIteration.incrementCoordinateWithinCuboidAddress());

			this.printMapAreaUpdates(areaToUpdate);
			return;
		}else{
			logger.info("missing updateMapAreaFlags because this.mapAreaCuboidAddress was null.");
			return;
		}
	}

	public void onUpdateMapAreaFlagsNotify(CuboidAddress areaToUpdate) throws Exception {
		this.updateMapAreaFlags(areaToUpdate);
		this.onFinalizeFrame();
	}

	public void reprintFrame() throws Exception {
		if(this.mapAreaCells != null){
			this.drawBorders();
			//  Player coordinate:
			String playerCoordinateString = "X=" + this.getPlayerPosition().getX() + ", Y=" + this.getPlayerPosition().getY() + ", Z=" + this.getPlayerPosition().getZ();
			this.printTextAtScreenXY(new ColouredTextFragment(playerCoordinateString, new int [] {DEFAULT_TEXT_BG_COLOR, DEFAULT_TEXT_FG_COLOR}), 10L, 0L, true);
		}
	}

	public void printMapAreaUpdates(CuboidAddress areaToUpdate) throws Exception {
		if(this.mapAreaCuboidAddress != null){
			logger.info("Doing printMapAreaUpdates with mapArea cuboid as " + this.mapAreaCuboidAddress + ".");

	
			GraphicsMode mode = blockManagerThreadCollection.getGraphicsMode();
			boolean useASCII = mode.equals(GraphicsMode.ASCII);
			Long areaToUpdateWidth = areaToUpdate.getWidthForIndex(0L);
			Long areaToUpdateHeight = areaToUpdate.getWidthForIndex(2L);
			Long mapAreaWidthInCells = this.mapAreaCuboidAddress.getWidthForIndex(0L);
			Long mapAreaHeightInCells = this.mapAreaCuboidAddress.getWidthForIndex(2L);
			String [][] updatedCellContents = new String [areaToUpdateWidth.intValue()][areaToUpdateHeight.intValue()];
			int [][][] updatedBackgroundColours = new int [areaToUpdateWidth.intValue()][areaToUpdateHeight.intValue()][2];

			RegionIteration regionIteration = new RegionIteration(areaToUpdate.getCanonicalLowerCoordinate(), areaToUpdate);
			do{
				Coordinate currentMapAreaCoordinate = regionIteration.getCurrentCoordinate();
				if(this.mapAreaCuboidAddress.containsCoordinate(currentMapAreaCoordinate)){

					/*  Does player position need to be re-rendered? */
					if(this.stalePlayerPositions.contains(currentMapAreaCoordinate)){
						this.stalePlayerPositions.remove(currentMapAreaCoordinate);
					}

					boolean isPlayerPosition = false;
					if(
						this.getPlayerPosition() != null &&
						currentMapAreaCoordinate.getX().equals(this.getPlayerPosition().getX()) &&
						currentMapAreaCoordinate.getY().equals(this.getPlayerPosition().getY()) &&
						currentMapAreaCoordinate.getZ().equals(this.getPlayerPosition().getZ())
					){
						isPlayerPosition = true;
					}
					Long xCellOffset = currentMapAreaCoordinate.getX() - this.mapAreaCuboidAddress.getCanonicalLowerCoordinate().getX();
					Long yCellOffset = (this.mapAreaCuboidAddress.getWidthForIndex(2L) -1L) - (currentMapAreaCoordinate.getZ() - this.mapAreaCuboidAddress.getCanonicalLowerCoordinate().getZ());

					MapAreaCell currentMapAreaCell = this.mapAreaCells[xCellOffset.intValue()][yCellOffset.intValue()];
					//logger.info("printMapAreaUpdates() for " + currentMapAreaCoordinate + " xCellOffset=" + xCellOffset + " yCellOffset=" + yCellOffset);

					boolean overSolidBlock = false;
					int defaultBackgroundColour = overSolidBlock ? GRAY_BG_COLOR : MAP_CELL_BG_COLOR2;
					int backgroundColour = isPlayerPosition ? PLAYER_BG_COLOR : defaultBackgroundColour;

					String stringToWrite = currentMapAreaCell.renderBlockCell(useASCII);

					Long xCellOffsetInUpdateArea = currentMapAreaCoordinate.getX() - areaToUpdate.getCanonicalLowerCoordinate().getX();
					Long yCellOffsetInUpdateArea = (areaToUpdateHeight -1L) -(currentMapAreaCoordinate.getZ() - areaToUpdate.getCanonicalLowerCoordinate().getZ());
					updatedCellContents[xCellOffsetInUpdateArea.intValue()][yCellOffsetInUpdateArea.intValue()] = stringToWrite;
					updatedBackgroundColours[xCellOffsetInUpdateArea.intValue()][yCellOffsetInUpdateArea.intValue()][0] = backgroundColour;
					updatedBackgroundColours[xCellOffsetInUpdateArea.intValue()][yCellOffsetInUpdateArea.intValue()][1] = DEFAULT_TEXT_FG_COLOR;
				}
			}while (regionIteration.incrementCoordinateWithinCuboidAddress());

			Long xCellOffsetDraw = areaToUpdate.getCanonicalLowerCoordinate().getX() - this.mapAreaCuboidAddress.getCanonicalLowerCoordinate().getX();
			Long yCellOffsetDraw = ((mapAreaHeightInCells -1L) + this.mapAreaCuboidAddress.getCanonicalLowerCoordinate().getZ()) - ((areaToUpdateHeight -1L) + areaToUpdate.getCanonicalLowerCoordinate().getZ());

			Long screenDrawX = (xCellOffsetDraw * this.getMapAreaCellWidth()) + this.getFrameCharacterWidth();
			Long screenDrawY = yCellOffsetDraw + this.getFrameCharacterHeight();
		
			this.sendCellUpdatesInScreenArea(areaToUpdate, updatedCellContents, updatedBackgroundColours, screenDrawX, screenDrawY);
		}else{
		}
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
