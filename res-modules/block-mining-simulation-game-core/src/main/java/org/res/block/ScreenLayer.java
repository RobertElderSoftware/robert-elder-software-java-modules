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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

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

public class ScreenLayer {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private boolean isLayerActive = true;
	private Coordinate placementOffset;  //  The offset of where the layer should end up once it's merged in.
	private CuboidAddress dimensions;
	private ScreenLayerColumn [][] columns;
	private int [] defaultColourCodes = new int [] {};
	private Set<ScreenRegion> changedRegions = new HashSet<ScreenRegion>();
	private final StringBuilder stringBuilder = new StringBuilder();

	private RecycledArrayBuffer changeFlagsRecycledBuffer = new RecycledArrayBuffer();
	private RecycledArrayBuffer activeStatesRecycledBuffer = new RecycledArrayBuffer();
	private RecycledArrayBuffer finalActiveStatesRecycledBuffer = new RecycledArrayBuffer();
	private RecycledArrayBuffer trustedChangedFlagsRecycledBuffer = new RecycledArrayBuffer();
	private RecycledArrayBuffer rightwardOcclusionsRecycledBuffer = new RecycledArrayBuffer();
	private RecycledArrayBuffer leftwardOcclusionsRecycledBuffer = new RecycledArrayBuffer();

	public final boolean getIsLayerActive(){
		return this.isLayerActive;
	}

	public final ScreenLayerColumn getColumn(final int x, final int y){
		return this.columns[x][y];
	}

	public final String getColumnCharacter(final int x, final int y){
		return this.columns[x][y].getCharacter();
	}

	public void setColumnCharacter(final int x, final int y, final String character){
		this.columns[x][y].setCharacter(character);
	}

	public final int [] getColumnColourCodes(final int x, final int y){
		return this.columns[x][y].getColourCodes();
	}

	public void setColumnColourCodes(final int x, final int y, final int [] colourCodes){
		this.columns[x][y].setColourCodes(colourCodes);
	}

	public final int getColumnCharacterWidth(final int x, final int y){
		return this.columns[x][y].getCharacterWidth();
	}

	public final int getPositiveOnlyColumnCharacterWidth(final int x, final int y){
		int v = this.columns[x][y].getCharacterWidth();
		if(v > 0){
			return v;
		}else{
			return 0;
		}
	}

	public void setMultiColumnCharacter(final int x, final int y, String character, final int characterWidth, final int [] colourCodes){
		this.setMultiColumnCharacter(x, y, character, characterWidth, colourCodes, true, true);
	}

	public void setMultiColumnCharacter(final int x, final int y, String character, final int characterWidth, final int [] colourCodes, boolean changed, boolean active){
		this.columns[x][y].setCharacterWidth(characterWidth);
		this.columns[x][y].setCharacter(character);
		this.columns[x][y].setColourCodes(colourCodes);
		this.columns[x][y].setChanged(changed);
		this.columns[x][y].setActive(active);
		for(int i = 1; i < characterWidth; i++){
			//  For multi-column characters, widths after the first indicate
			//  how many columns back to move to get to the first column.
			this.columns[x+i][y].setCharacterWidth(-i);
			this.columns[x+i][y].setCharacter(null);
			this.columns[x+i][y].setColourCodes(colourCodes);
			this.columns[x+i][y].setChanged(changed);
			this.columns[x+i][y].setActive(active);
		}
	}

	public void setToEmpty(final int x, final int y){
		this.setToEmpty(x, y, true, true);
	}

	public void setToEmpty(final int x, final int y, boolean changed, boolean active){
		this.columns[x][y].setCharacterWidth(0);
		this.columns[x][y].setCharacter(null);
		this.columns[x][y].setColourCodes(new int [] {});
		this.columns[x][y].setChanged(changed);
		this.columns[x][y].setActive(active);
	}

	public void setColumnCharacterWidth(final int x, final int y, final int characterWidth){
		this.columns[x][y].setCharacterWidth(characterWidth);
	}

	public final boolean getColumnChanged(final int x, final int y){
		return this.columns[x][y].getChanged();
	}

	public void setColumnChanged(final int x, final int y, final boolean newState){
		this.columns[x][y].setChanged(newState);
	}

	public final boolean getColumnActive(final int x, final int y){
		return this.columns[x][y].getActive();
	}

	public void setColumnActive(final int x, final int y, final boolean newState){
		this.columns[x][y].setActive(newState);
	}

	public static CuboidAddress makeDimensionsCA(int startX, int startY, int endX, int endY) throws Exception{
		return new CuboidAddress(
			new Coordinate(Arrays.asList((long)startX, (long)startY)),
			new Coordinate(Arrays.asList((long)endX, (long)endY))
		);
	}

	public final boolean setIsLayerActive(boolean isLayerActive) throws Exception{
		if(this.isLayerActive != isLayerActive){
			this.isLayerActive = isLayerActive;
			this.addChangedRegion(new ScreenRegion(ScreenRegion.makeScreenRegionCA(0,0, getWidth(), getHeight())));
			this.setAllChangedFlagStates(true);
			return true;
		}else{
			return false;
		}
	}

	public int getWidth(){
		return (int)this.dimensions.getWidth();
	}

	public int getHeight(){
		return (int)this.dimensions.getHeight();
	}

	public void setPlacementOffset(Coordinate newPlacementOffset) throws Exception{
		//  Change region for where the layer was before the new placement offset
		//  (Can possibly be outside the layer, but that's necessary for the
		//  case where this layer gets merged down onto something else)
		Long displacementX = newPlacementOffset.getX() - this.placementOffset.getX();
		Long displacementY = newPlacementOffset.getY() - this.placementOffset.getY();
		this.addChangedRegion(new ScreenRegion(
			ScreenRegion.makeScreenRegionCA(
				-displacementX.intValue(),
				-displacementY.intValue(),
				-displacementX.intValue() + getWidth(),
				-displacementY.intValue() + getHeight()
			)
		));
		//  Change region for entire layer at new offset
		this.addChangedRegion(new ScreenRegion(
			ScreenRegion.makeScreenRegionCA(0, 0, getWidth(), getHeight())
		));
		this.placementOffset = newPlacementOffset;
	}

	public Coordinate getPlacementOffset(){
		return this.placementOffset;
	}

	public CuboidAddress getDimensions(){
		return this.dimensions;
	}

	public void clearChangedRegions(){
		this.changedRegions.clear();
	}

	public void addChangedRegion(ScreenRegion r){
		this.changedRegions.add(r);
	}

	public void addChangedRegions(Set<ScreenRegion> regions){
		this.changedRegions.addAll(regions);
	}

	public Set<ScreenRegion> getChangedRegions(){
		return this.changedRegions;
	}

	public ScreenLayer(Coordinate placementOffset, CuboidAddress dimensions) throws Exception{
		int width = (int)dimensions.getWidth();
		int height = (int)dimensions.getHeight();
		this.columns = new ScreenLayerColumn [width][height];
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.columns[i][j] = new ScreenLayerColumn(
					0,
					new int [] {},
					null,
					false,
					false
				);
			}
		}
		this.dimensions = dimensions;
		this.placementOffset = placementOffset;
	}

	public void setAllChangedFlagStates(boolean state){
		int width = this.getWidth();
		int height = this.getHeight();
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.setColumnChanged(i, j, state);
			}
		}
	}

	public void setAllActiveFlagStates(boolean state){
		int width = this.getWidth();
		int height = this.getHeight();
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.setColumnActive(i, j, state);
			}
		}
	}

	public void initialize() throws Exception{
		// By default, make assumptions that minimize screen prints
		this.initialize(0, null, new int [] {} , null);
	}

	public void initialize(int chrWidth, String s, int [] colourCodes) throws Exception{
		this.initialize(chrWidth, s, colourCodes, null);
	}

	public void initialize(int chrWidth, String s, int [] colourCodes, String msg) throws Exception{
		this.initializeInRegion(chrWidth, s, colourCodes, msg, new ScreenRegion(ScreenRegion.makeScreenRegionCA(0,0, getWidth(), getHeight())), true, false);
	}

	public void initializeInRegion(int chrWidth, String s, int [] colourCodes, String msg, ScreenRegion region, boolean defaultChangedFlag, boolean defaultActiveFlag) throws Exception{
		this.addChangedRegion(region);
		int startX = region.getStartX();
		int startY = region.getStartY();
		int endX = region.getEndX();
		int endY = region.getEndY();
		this.defaultColourCodes = colourCodes;

		if(!(chrWidth == 0 && s == null || chrWidth == 1)){
			throw new Exception("chrWidth == 0 && s == null || chrWidth == 1");
		}

		for(int i = startX; i < endX; i++){
			for(int j = startY; j < endY; j++){
				this.setColumnCharacterWidth(i, j, chrWidth);
				this.setColumnColourCodes(i, j, colourCodes);
				this.setColumnCharacter(i, j, s);
				this.setColumnChanged(i, j, defaultChangedFlag);
				this.setColumnActive(i, j, defaultActiveFlag); //  Require the user to explicitly enable this column.
			}
		}
		if(msg != null){
			if(colourCodes == null){
				throw new Exception("colourCodes == null");
			}
			if(s == null){
				throw new Exception("s == null");
			}
			int messageLength = msg.length();
			int xOffset = messageLength > getWidth() ? 0 : ((getWidth() - messageLength) / 2);
			int yOffset = getHeight() / 2;

			for(int i = 0; i < msg.length(); i++){
				if(((xOffset + i)) < getWidth()){
					this.setColumnCharacter(xOffset + i, yOffset, String.valueOf(msg.charAt(i)));
				}
			}
		}
	}

	public void setScreenAreaChangeStates(int startX, int startY, int endX, int endY, boolean state) throws Exception{
		//  Invalidate a sub-area of screen so that the characters are that location will 
		//  be printed on the next print attempt.
		int width = endX - startX;
		int height = endY - startY;
		
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.setColumnChanged(i + startX, j + startY, state);
			}
		}
		this.addChangedRegion(
			new ScreenRegion(ScreenRegion.makeScreenRegionCA(
				startX,
				startY,
				endX,
				endY
			))
		);
	}

	public final int getCharacterWidthForStartingBoundary(final int x, final int y, final boolean isLeftToRight){
		int columnValue = getColumnCharacterWidth(x, y);
		if(isLeftToRight){
			if(columnValue > 0){
				return columnValue;
			}else{
				return 0;
			}
		}else{
			if(columnValue == 1){
				return columnValue;
			}else if(columnValue < 0){
				//  In Right to left pass, the 'start' of the character is where the negative offset is one less than the actual width:
				final int characterStart = x + columnValue;
				final int expectedWidth = (-columnValue) + 1;
				if(getColumnCharacterWidth(characterStart, y) == expectedWidth){
					return getColumnCharacterWidth(characterStart, y);
				}else{
					return 0;
				}
			}else{
				return 0;
			}
		}
	}

	public final boolean isAtInitialColumnOfCharacter(final int x, final int y){
		return this.columns[x][y].isAtInitialColumnOfCharacter();
	}

	public void calculateOcclusions(final boolean isLeftToRight, final int startX, final int endX, final int j, final int startY, final ScreenLayer [] screenLayers, final int [] occlusions, final boolean [][][] activeStates, final int [] xO, final int [] yO, boolean [] trustedChangedFlags) throws Exception{
		int xWidth = endX-startX;

		int loopStart = isLeftToRight ? startX : endX - 1;
		int loopEnd = isLeftToRight ? endX : startX - 1;
		int loopChange = isLeftToRight ? 1 : -1;

		int i;
		int activeCharacterLayer = -1;
		int columnsRemaining = -1;
		for(int iter = 0; iter < xWidth; iter++){
			i = loopStart + (loopChange * iter);
			int xR = i-startX;
			int yR = j-startY;

			int firstSolidLayer = -1;
			boolean sawPopulatedColourCodes = false;

			int currentCharacterWidth = 0;
			for(int s = screenLayers.length -1; s >= 0; s--){
				int xSrc = i-xO[s];
				int ySrc = j-yO[s];
				if(activeStates[s][xR][yR]){
					boolean columnChanged = screenLayers[s].getColumnChanged(xSrc, ySrc);
					if(firstSolidLayer == -1){
						if(xSrc >= 0 && xSrc < screenLayers[s].getWidth() && ySrc >= 0 && ySrc < screenLayers[s].getHeight()){
							trustedChangedFlags[xR] |= columnChanged;
							int currentColumnWidthValue = screenLayers[s].getColumnCharacterWidth(xSrc, ySrc);
							currentCharacterWidth = screenLayers[s].getCharacterWidthForStartingBoundary(xSrc, ySrc, isLeftToRight);
							if(currentColumnWidthValue != 0){ //  Some part of a solid character
								firstSolidLayer = s;
							}

						}
					}
					if(!sawPopulatedColourCodes){
						int [] colours = screenLayers[s].getColumnColourCodes(xSrc, ySrc);
						boolean colourCodesAreEmpty = colours.length == 0;
						if(!colourCodesAreEmpty){
							trustedChangedFlags[xR] |= columnChanged;
							sawPopulatedColourCodes = true;
						}
					}
				}
			}

			boolean isAtCharacterStartPosition = currentCharacterWidth > 0;
			if(isAtCharacterStartPosition){
				columnsRemaining = currentCharacterWidth;
				if(activeCharacterLayer < firstSolidLayer){
					activeCharacterLayer = firstSolidLayer;
				}
			}

			if(firstSolidLayer == -1){
				occlusions[xR] = -2; // No Character Found.
			}else if(firstSolidLayer == activeCharacterLayer){
				//  This character is included in left to right pass
				occlusions[xR] = activeCharacterLayer;
			}else if(firstSolidLayer >=0){
				occlusions[xR] = -1; // Ocluded character
			}else{
				throw new Exception("Not expected.");
			}

			columnsRemaining--;

			if(columnsRemaining <= 0){         //  We're not currently inside a character
				activeCharacterLayer = -1; //  Exit being active in a character.
			}
		}
	}

	public void populateTranslatedExpandedClippedRegions(ScreenLayer [] screenLayers, Set<ScreenRegion> translatedExpandedRegions, Set<ScreenRegion> translatedExpandedClippedRegions, int [] xO, int [] yO) throws Exception{
		//  Everything gets merged down into a coordinate system based on layer 0
		CuboidAddress baseLayerCuboidAddress = ScreenRegion.makeScreenRegionCA(
			0,
			0,
			this.getWidth(),
			this.getHeight()
		);
		for(int l = 0; l < screenLayers.length; l++){
			if(screenLayers[l].getIsLayerActive()){
				for(ScreenRegion sourceRegion : screenLayers[l].getChangedRegions()){
					ScreenRegion translatedRegion = new ScreenRegion(ScreenRegion.makeScreenRegionCA(
						sourceRegion.getStartX() + xO[l],
						sourceRegion.getStartY() + yO[l],
						sourceRegion.getEndX() + xO[l],
						sourceRegion.getEndY() + yO[l]
					));

					//  Expand the region to not cut multi-column characters:
					ScreenRegion expandedRegion = ScreenLayer.getNonCharacterCuttingChangedRegions(translatedRegion, screenLayers);
					//  Clip the region to fit inside the base layer:
					ScreenRegion clippedRegion = new ScreenRegion(expandedRegion.getRegion().getIntersectionCuboidAddress(baseLayerCuboidAddress));

					translatedExpandedClippedRegions.add(clippedRegion);
					translatedExpandedRegions.add(expandedRegion);
				}
			}
			screenLayers[l].clearChangedRegions();
		}
	}

	public void mergeDown(ScreenLayer aboveLayer, boolean trustChangedFlags) throws Exception{
		this.mergeDown(aboveLayer, trustChangedFlags, ScreenLayerMergeType.PREFER_BOTTOM_LAYER);
	}

	public void mergeDown(ScreenLayer aboveLayer, boolean trustChangedFlags, ScreenLayerMergeType forcedBottomLayerState) throws Exception{
		this.mergeDown(new ScreenLayer [] {aboveLayer}, trustChangedFlags, forcedBottomLayerState);
	}

	public void mergeDown(ScreenLayer [] aboveLayers, boolean trustChangedFlags) throws Exception{
		this.mergeDown(aboveLayers, trustChangedFlags, ScreenLayerMergeType.PREFER_BOTTOM_LAYER);
	}

	public void mergeDown(ScreenLayer [] aboveLayers, boolean trustChangedFlags, ScreenLayerMergeType forcedBottomLayerState) throws Exception{
		ScreenLayer [] screenLayers = new ScreenLayer [aboveLayers.length +1];
		int [] xO = new int [aboveLayers.length +1];
		int [] yO = new int [aboveLayers.length +1];
		screenLayers[0] = this;  //  Bottom layer should be current layer.
		//screenLayers[0].throwExceptionOnValidationFailure();
		xO[0] = 0;
		yO[0] = 0;  //  All of the 'placement offsets' are relative to the layer we're merging down onto.
		for(int a = 0; a < aboveLayers.length; a++){
			//aboveLayers[a].throwExceptionOnValidationFailure();
			screenLayers[a+1] = aboveLayers[a];  //  All the layers above to merge down
			xO[a+1] = aboveLayers[a].getPlacementOffset().getX().intValue();
			yO[a+1] = aboveLayers[a].getPlacementOffset().getY().intValue();
		}
		
		//  Regions that have been translated to base layer coordinates, and expanded to not cut multi-column characters:
		Set<ScreenRegion> translatedExpandedRegions = new HashSet<ScreenRegion>(); 
		//  Regions that have been translated to base layer coordinates, and expanded to not cut multi-column characters
		//  and also clipped to be within the base layer:
		Set<ScreenRegion> translatedExpandedClippedRegions = new HashSet<ScreenRegion>();
		this.populateTranslatedExpandedClippedRegions(screenLayers, translatedExpandedRegions, translatedExpandedClippedRegions, xO, yO);

		for(ScreenRegion region : translatedExpandedClippedRegions){
			final int startX = region.getStartX();
			final int startY = region.getStartY();
			final int endX = region.getEndX();
			final int endY = region.getEndY();

			final int xWidth = endX - startX;
			final int yHeight = endY - startY;
			//  Pre-calculate the active states for all layers in the entire current horizontal strip:
			final boolean [][][] changeFlags = this.changeFlagsRecycledBuffer.get3DBooleanArray(screenLayers.length, xWidth, yHeight);
			final boolean [][][] activeStates = this.activeStatesRecycledBuffer.get3DBooleanArray(screenLayers.length, xWidth, yHeight);
			final boolean [][] finalActiveStates = this.finalActiveStatesRecycledBuffer.get2DBooleanArray(xWidth, yHeight);
			final int [][][] topColourCodes = new int [xWidth][yHeight][];
			final int [] emptyColours = new int [] {};

			//  Control whether layer 0 is forced active or inactive:
			final boolean fbls = forcedBottomLayerState.toBoolean();

			for(int s = screenLayers.length -1; s >= 0; s--){
				final boolean layerActive = screenLayers[s].getIsLayerActive();
				final int innerStartX = startX + (-(Math.min(startX - xO[s], 0)));
				final int innerStartY = startY + (-(Math.min(startY - yO[s], 0)));
				final int innerEndX = endX + Math.min(screenLayers[s].getWidth() - (endX -xO[s]), 0);
				final int innerEndY = endY + Math.min(screenLayers[s].getHeight() - (endY -yO[s]), 0);
				for(int j = innerStartY; j < innerEndY; j++){
					for(int i = innerStartX; i < innerEndX; i++){
						final int xSrc = i-xO[s];
						final int ySrc = j-yO[s];
						final int xR = i-startX;
						final int yR = j-startY;
						activeStates[s][xR][yR] = (s == 0) ? fbls : (layerActive && screenLayers[s].getColumnActive(xSrc, ySrc));

						if(topColourCodes[xR][yR] == null){
							topColourCodes[xR][yR] = emptyColours;
						}
						if(activeStates[s][xR][yR]){
							finalActiveStates[xR][yR] = true;
							int [] columnColourCodes = screenLayers[s].getColumnColourCodes(xSrc, ySrc);
							if(topColourCodes[xR][yR].length == 0){
								if(columnColourCodes.length > 0 && activeStates[s][xR][yR]){
									topColourCodes[xR][yR] = columnColourCodes;
								}
							}

							if(screenLayers[s].getColumnChanged(xSrc, ySrc)){
								changeFlags[s][xR][yR] = true; //  Check all layers, just in case a layer underneath has a change of BG colour.
							}
						}
					}
				}
			}

			this.applyOcclusions(screenLayers, startX, endX, startY, endY, xWidth, topColourCodes, activeStates, finalActiveStates, xO, yO, trustChangedFlags);
		}

		this.resetChangedFlags(screenLayers, translatedExpandedRegions, xO, yO);
		this.addChangedRegions(translatedExpandedClippedRegions);
		//this.throwExceptionOnValidationFailure();
	}

	public void applyOcclusions(final ScreenLayer [] screenLayers, final int startX, final int endX, final int startY, final int endY, final int xWidth, final int [][][] topColourCodes, final boolean [][][] activeStates, final boolean [][] finalActiveStates, final int [] xO, final int [] yO, final boolean trustChangedFlags) throws Exception{
		final int [] rightwardOcclusions = rightwardOcclusionsRecycledBuffer.get1DIntArray(xWidth);
		final int [] leftwardOcclusions = leftwardOcclusionsRecycledBuffer.get1DIntArray(xWidth);

		for(int j = Math.max(0, startY); j < Math.min(screenLayers[0].getHeight(), endY); j++){
			final int outputStartX = Math.max(0, startX);
			final int outputEndX = Math.min(screenLayers[0].getWidth(), endX);
			final int outputWidth = outputEndX - outputStartX;
			boolean rightBoundaryHasSeveredCharacter = false;
			boolean leftBoundaryHasSeveredCharacter = true;

			final boolean [] trustedChangedFlags = this.trustedChangedFlagsRecycledBuffer.get1DBooleanArray(xWidth); // Re-initialize boolean array
			this.calculateOcclusions(true, startX, endX, j, startY, screenLayers, rightwardOcclusions, activeStates, xO, yO, trustedChangedFlags);
			this.calculateOcclusions(false, startX, endX, j, startY, screenLayers, leftwardOcclusions, activeStates, xO, yO, trustedChangedFlags);

			for(int i = outputStartX; i < outputEndX; i++){
				final int xR = i-startX;
				final int yR = j-startY;
				String outputCharacters = null;
				int outputCharacterWidths = 0;
				boolean isAtInitialColumnOfCharacter;
				int rightward = rightwardOcclusions[xR];
				int leftward = leftwardOcclusions[xR];
				boolean occludedChangeFlag = false;
				if(rightward >=0 || leftward >= 0){
					if(rightward == leftward){ //  Both directions agree on what layer should be active:
						int xSrc = i-xO[rightward];
						int ySrc = j-yO[rightward];
						outputCharacters = screenLayers[rightward].getColumnCharacter(xSrc, ySrc);
						outputCharacterWidths = screenLayers[rightward].getColumnCharacterWidth(xSrc, ySrc);
						isAtInitialColumnOfCharacter = screenLayers[rightward].isAtInitialColumnOfCharacter(xSrc, ySrc);
					}else{
						isAtInitialColumnOfCharacter = true;
						outputCharacters = " ";
						outputCharacterWidths = 1;
						occludedChangeFlag = true;
					}
				}else if(rightward == -1 && leftward == -1){
					//  Empty null column in upper layer exposing center column of multi-column character:
					outputCharacters = " ";
					outputCharacterWidths = 1;
					occludedChangeFlag = true;
					isAtInitialColumnOfCharacter = true;
				}else if(rightward == -2 && leftward == -1){
					throw new Exception("not expected");
				}else if(rightward == -1 && leftward == -2){
					throw new Exception("not expected");
				}else if(rightward == -2 && leftward == -2){
					outputCharacters = null;
					outputCharacterWidths = 0;
					isAtInitialColumnOfCharacter = true;
				}else{
					throw new Exception("not expected");
				}

				if((outputEndX - i) < outputCharacterWidths){
					rightBoundaryHasSeveredCharacter = true;
				}

				if(isAtInitialColumnOfCharacter){
					leftBoundaryHasSeveredCharacter = false;
				}

				if(leftBoundaryHasSeveredCharacter || rightBoundaryHasSeveredCharacter){
					outputCharacters = " ";
					outputCharacterWidths = 1;
					occludedChangeFlag = true;
					
				}

				if(outputCharacterWidths < 0){ // non-first columns in multi-colun char
					int before_i = i-1;
					if(before_i >= 0){
						topColourCodes[xR][yR] = this.getColumnColourCodes(before_i, j);
						trustedChangedFlags[xR] = this.getColumnChanged(before_i, j);
					}
				}

				boolean hasChange = false;
				if(trustChangedFlags){
					hasChange = trustedChangedFlags[xR] || occludedChangeFlag;
				}else{
					if(!isAtInitialColumnOfCharacter){
						//  For multi-column characters, use changed flag from first column.
						hasChange = trustedChangedFlags[xR] || occludedChangeFlag;
					}else{
						hasChange = this.hasCharacterChanged(
							i,
							j,
							outputCharacters,
							outputCharacterWidths,
							topColourCodes[xR][yR]
						) || this.getColumnChanged(i, j); // if there is a pending changed flag that hasn't been printed yet.
					}
				}

				this.setColumnCharacter(i, j, outputCharacters);
				this.setColumnCharacterWidth(i, j, outputCharacterWidths);
				this.setColumnColourCodes(i, j, topColourCodes[xR][yR]);
				this.setColumnChanged(i, j, hasChange);
				this.setColumnActive(i, j, finalActiveStates[xR][yR]);
			}
		}
	}

	public boolean hasCharacterChanged(final int i, final int j, final String character, final int characterWidth, final int [] colourCodes) throws Exception{
		return !(
			(this.getColumnCharacterWidth(i, j) == characterWidth) &&
			Arrays.equals(this.getColumnColourCodes(i, j), colourCodes) &&
			Objects.equals(this.getColumnCharacter(i, j), character)
		);
	}

	public void resetChangedFlags(ScreenLayer [] screenLayers, Set<ScreenRegion> translatedExpandedRegions, int [] xO, int [] yO) throws Exception{
		//  If some of the changed regions overlap, there is a case where
		//  the calculated change flags can be incorrect due to them being
		//  cleared by a previous overlapping changed region.
		for(ScreenRegion region : translatedExpandedRegions){
			for(int s = screenLayers.length -1; s >= 1; s--){
				final int startX = Math.max(region.getStartX() - xO[s], 0);
				final int startY = Math.max(region.getStartY() - yO[s], 0);
				final int endX = Math.min(region.getEndX() - xO[s], screenLayers[s].getWidth());
				final int endY = Math.min(region.getEndY() - yO[s], screenLayers[s].getHeight());
				for(int j = startY; j < endY; j++){
					for(int i = startX; i < endX; i++){
						//  Clear the changed flag for any active layer other than the merged layer:
						final boolean isLayerActive = screenLayers[s].getIsLayerActive() && screenLayers[s].getColumnActive(i, j);
						if(isLayerActive){
							screenLayers[s].setColumnChanged(i, j, false);
						}
					}
				}
			}
		}
	}

	public void printChanges(boolean resetCursorPosition, int xOffset, int yOffset) throws Exception{
		this.printChanges(false, false, resetCursorPosition, xOffset, yOffset);
	}

	public void printChanges(boolean useCompatibilityWidth, boolean useRightToLeftPrint, boolean resetCursorPosition, int xOffset, int yOffset) throws Exception{
		int loopUpdate = useRightToLeftPrint ? -1 : 1;
		boolean resetState = useRightToLeftPrint ? true : true; // TODO:  Optimize this in the future.
		int [] lastUsedColourCodes = null;
		for(ScreenRegion region : this.getChangedRegions()){
			int startX = region.getStartX();
			int startY = region.getStartY();
			int endX = region.getEndX();
			int endY = region.getEndY();
			int startColumn = useRightToLeftPrint ? endX -1 : startX;
			int endColumn = useRightToLeftPrint ? startX -1 : endX;
			for(int j = startY; j < endY; j++){
				boolean mustSetCursorPosition = true;
				boolean mustSetColourCodes = true;
				int chrsLeft = 0;
				for(int i = startColumn; i != endColumn; i += loopUpdate){
					//  Try to intelligently issue as few ANSI escape sequences as possible:
					if(!Arrays.equals(this.getColumnColourCodes(i, j), lastUsedColourCodes)){
						mustSetColourCodes = true;
					}
					if(chrsLeft <= 0){
						int columnWidthValue = this.getColumnCharacterWidth(i, j);
						chrsLeft = columnWidthValue > 0 ? columnWidthValue : 0;
					}
					if(
						this.getColumnChanged(i, j)
					){
						if(useCompatibilityWidth){
							String currentPositionSequence = "\033[" + (j+1+yOffset) + ";" + (i+1+xOffset) + "H";
							this.stringBuilder.append(currentPositionSequence);

							List<String> codes = new ArrayList<String>();
							for(int c : this.getColumnColourCodes(i, j)){
								codes.add(String.valueOf(c));
							}
							String currentColorSequence = "\033[0m\033[" + String.join(";", codes) + "m";
							this.stringBuilder.append(currentColorSequence);
							mustSetColourCodes = resetState;
							lastUsedColourCodes = this.getColumnColourCodes(i, j);

							int columnWidthValue = this.getColumnCharacterWidth(i, j)  > 0 ? this.getColumnCharacterWidth(i, j) : 0;
							for(int k = 0; k < columnWidthValue; k++){
								this.stringBuilder.append(" ");
							}
						}
						if(mustSetCursorPosition){
							String currentPositionSequence = "\033[" + (j+1+yOffset) + ";" + (i+1+xOffset) + "H";
							this.stringBuilder.append(currentPositionSequence);
							mustSetCursorPosition = resetState;
						}
						if(mustSetColourCodes){
							List<String> codes = new ArrayList<String>();
							for(int c : this.getColumnColourCodes(i, j)){
								codes.add(String.valueOf(c));
							}
							String currentColorSequence = "\033[0m\033[" + String.join(";", codes) + "m";
							this.stringBuilder.append(currentColorSequence);
							mustSetColourCodes = resetState;
							lastUsedColourCodes = this.getColumnColourCodes(i, j);
						}
						if(this.getColumnCharacter(i, j) == null && chrsLeft == 0){
							//this.stringBuilder.append("\033[43mX\033[0m"); // Highlight Nulls
							this.stringBuilder.append("\033[0m "); //  Clear space with default colour space.
						}else if(this.getColumnCharacter(i, j) == null){
						}else{
							this.stringBuilder.append(this.getColumnCharacter(i, j));
						}
						this.setColumnChanged(i, j, false);
					}else{
						mustSetCursorPosition = true;
					}
					chrsLeft--;
				}
			}
		}
		this.clearChangedRegions();
		if(resetCursorPosition){
			this.stringBuilder.append("\033[0;0H"); //  Move cursor to 0,0 after every print.
		}
		System.out.print(this.stringBuilder); //  Print accumulated output
		this.stringBuilder.setLength(0);      //  clear buffer.
	}

	public static boolean isInChangedRegion(int x, int y, Set<ScreenRegion> changedRegions) throws Exception{
		for(ScreenRegion r : changedRegions){
			if(r.getRegion().containsCoordinate(new Coordinate(Arrays.asList((long)x, (long)y)))){
				return true;
			}
		}
		return false;
	}

	public void printDebugStates(int xOffset, int yOffset, String debugType) throws Exception{
		for(int j = 0; j < this.getHeight(); j++){
			for(int i = 0; i < this.getWidth(); i++){
				int [] colourCodes = new int []{UserInterfaceFrameThreadState.RESET_BG_COLOR};
				String characters = null;
				boolean isInChangedRegion = ScreenLayer.isInChangedRegion(i, j, this.changedRegions);
				if(debugType.equals("characters")){
					if(this.getColumnCharacter(i, j) != null){

						if(this.getColumnCharacterWidth(i, j) > 1){
							//  For multi-column characters show numer of full width:
							characters = "" + (this.getColumnCharacterWidth(i, j) % 10);
						}else{
							characters = this.getColumnCharacter(i, j);
						}
					}else{
						int offset = getNextCharacterStartToLeft(i, j, this);
						if(offset != -1 && ((this.getPositiveOnlyColumnCharacterWidth(i-offset, j) - offset) > 0)){
							//  A null that's part of a multi-column character:
							characters = "_";
						}else{
							//  A random empty null area that's not part of a character.
							characters = "#";
						}
					}
					if(this.getColumnColourCodes(i, j) != null){
						colourCodes = this.getColumnColourCodes(i, j);
					}else{
						colourCodes = new int []{UserInterfaceFrameThreadState.CROSSED_OUT_COLOR};
					}
				}else if(debugType.equals("active")){
					if(this.getColumnActive(i, j) && this.getIsLayerActive()){
						colourCodes = new int []{UserInterfaceFrameThreadState.GREEN_BG_COLOR};
					}else if(this.getColumnActive(i, j) && !this.getIsLayerActive()){
						colourCodes = new int []{UserInterfaceFrameThreadState.BLUE_BG_COLOR, UserInterfaceFrameThreadState.CROSSED_OUT_COLOR, UserInterfaceFrameThreadState.RED_FG_COLOR};
					}else if(!this.getColumnActive(i, j) && this.getIsLayerActive()){
						colourCodes = new int []{UserInterfaceFrameThreadState.BLACK_BG_COLOR};
					}else if(!this.getColumnActive(i, j) && !this.getIsLayerActive()){
						colourCodes = new int []{UserInterfaceFrameThreadState.RED_BG_COLOR};
					}
					if(isInChangedRegion && this.getColumnActive(i, j)){
						characters = "*";
					}else{
						characters = " ";
					}
				}else if(debugType.equals("changed")){
					if(this.getColumnChanged(i, j)){
						colourCodes = new int []{UserInterfaceFrameThreadState.GREEN_BG_COLOR};
					}else{
						colourCodes = new int []{UserInterfaceFrameThreadState.BLACK_BG_COLOR};
					}
					if(isInChangedRegion && this.getColumnChanged(i, j)){
						characters = "*";
					}else{
						characters = " ";
					}
				}else if(debugType.equals("in_changed_region")){
					if(isInChangedRegion){
						colourCodes = new int []{UserInterfaceFrameThreadState.GREEN_BG_COLOR};
					}else{
						colourCodes = new int []{UserInterfaceFrameThreadState.BLACK_BG_COLOR};
					}
					characters = " ";
				}else{
					throw new Exception("Not expected.");
				}
				List<String> codes = new ArrayList<String>();
				for(int c : colourCodes){
					codes.add(String.valueOf(c));
				}
				String currentColorSequence = "\033[0m\033[" + String.join(";", codes) + "m";
				String currentPositionSequence = "\033[" + (i+1+xOffset) + "G" + characters;
				this.stringBuilder.append(currentColorSequence + currentPositionSequence);
			}
			this.stringBuilder.append("\033[0m\n");
		}
		this.stringBuilder.append("\033[0m");
		System.out.print(this.stringBuilder); //  Print accumulated output
		this.stringBuilder.setLength(0);      //  clear buffer.
	}

	public void throwExceptionOnValidationFailure() throws Exception{
		String validationResult = this.validate();
		if(validationResult != null){
			throw new Exception(validationResult);
		}
	}

	public String validate() throws Exception{
		//   Check for scenarios that should be impossible that
		//   the layer merging algorithms don't account for.
		int [] currentColourCodes = new int [] {};
		boolean currentActiveState = false;
		boolean currentChangedState = false;
		for(int j = 0; j < this.getHeight(); j++){
			int offsetIntoCharacter = 0;
			int totalCharacterWidth = 0;
			for(int i = 0; i < this.getWidth(); i++){
				offsetIntoCharacter++;
				if(offsetIntoCharacter == totalCharacterWidth){
					// Reset colour codes for next character.
					currentColourCodes = new int [] {};
					offsetIntoCharacter = 0;
					totalCharacterWidth = 0;
				}

				int columnWidthValue = this.getColumnCharacterWidth(i, j);
				if(columnWidthValue >= 0 && (i + columnWidthValue) > this.getWidth()){
					return "Saw character of width " + columnWidthValue + " as x=" + i + ", y=" + j + ", but layer width is only " + this.getWidth();
				}
				if(this.getColumnColourCodes(i, j) == null){
					return "Saw null colour codes at x=" + i + ", y=" + j + ".";
				}
				boolean insideMultiColumnCharacter = offsetIntoCharacter < totalCharacterWidth;
				if(insideMultiColumnCharacter){
					//  Every width inside a multi-column character should provide the x
					//  offset to get back to the first column of this multi-column character:
					if(!(this.getColumnCharacterWidth(i, j) == -offsetIntoCharacter)){
						return "Saw an incorrect width value (" + this.getColumnCharacterWidth(i, j) + "), expected (" + (-offsetIntoCharacter) + ") inside a multi-column character at x=" + i + ", y=" + j + ".";
					}
					if(!Arrays.equals(this.getColumnColourCodes(i, j), currentColourCodes)){
						return "Saw an inconsistent colour code inside a multi-column character that did not match at x=" + i + ", y=" + j + ".";
					}
					if(!Objects.equals(this.getColumnActive(i, j), currentActiveState)){
						return "Saw an inconsistent active state inside a multi-column character that did not match at x=" + i + ", y=" + j + ".";
					}
					if(!Objects.equals(this.getColumnChanged(i, j), currentChangedState)){
						return "Saw an inconsistent changed state inside a multi-column character that did not match at x=" + i + ", y=" + j + ".";
					}
					if(this.getColumnCharacter(i, j) != null){
						return "Saw non-null characters inside a multi-column character at x=" + i + ", y=" + j + ".";
					}

				}else{
					if(this.getColumnCharacterWidth(i, j) > 0){ // Start of a new character
						if(this.getColumnCharacter(i, j) == null){
							return "Saw character with specified width, but null characters at x=" + i + ", y=" + j + ".";
						}
						//  Started a new character
						currentColourCodes = this.getColumnColourCodes(i, j);
						currentChangedState = this.getColumnChanged(i, j);
						currentActiveState = this.getColumnActive(i, j);
						totalCharacterWidth = this.getColumnCharacterWidth(i, j); // First column gives actual character total width
						offsetIntoCharacter = 0;
					}else{
						//  An empty null character.  This is just an empty area.
						if(this.getColumnColourCodes(i, j) == null){
							return "Saw null colour codes inside a stray null character at x=" + i + ", y=" + j + ".";
						}
						if(this.getColumnColourCodes(i, j).length > 0){
							return "Saw null non-empty colour codes inside a stray null character at x=" + i + ", y=" + j + ".";
						}
					}
				}
			}
		}
		return null; //  Successfully validated.
	}

	public static int getNextCharacterStartToLeft(int startX, int currentY, ScreenLayer layer){
		int currentX = startX;
		while(
			currentX >= 0 &&
			currentX < layer.getWidth() &&
			currentY >= 0 &&
			currentY < layer.getHeight()
		){
			if(layer.getColumnCharacterWidth(currentX, currentY) > 0){
				return startX - currentX;
			}
			currentX--;
		}
		return -1;
	}

	public static int getExpansionForCoordinate(boolean isLeftToRight, int startX, int startY, int endY, int [] xO, int [] yO, ScreenLayer [] layers){
		for(int s = 0; s < layers.length; s++){
			//  Start at current x character:
			int currentX = startX - xO[s];
			for(int currentY = startY - yO[s]; currentY < endY - yO[s]; currentY++){
				int leftDistance = getNextCharacterStartToLeft(currentX, currentY, layers[s]);
				if(leftDistance == -1){
					//  No expansion necessary, there is no previous solid character
				}else{
					int characterWidth = layers[s].getPositiveOnlyColumnCharacterWidth(currentX - leftDistance, currentY);
					if(isLeftToRight){
						if(leftDistance > 0){
							int diff = characterWidth - leftDistance;
							if(diff > 0){
								//  Need to expand.  The found char start is beyond the left region boundary by 'leftDistance' amount.
								return leftDistance;
							}else{

							}
						}else{
							//  No expansion necessary, the left boundary is already on a character start
						}
					}else{
						int diff = characterWidth - leftDistance -1;
						if(diff > 0){
							//  Need to expand.  The found char overshoots the region boundary by 'diff' amount.
							return diff;
						}else{
							//  No expansion necessary, there is space for the character.
						}
					}
				}
			}
		}
		return 0;
	}

	public static ScreenRegion getNonCharacterCuttingChangedRegions(ScreenRegion inputRegion, ScreenLayer [] layers) throws Exception{

		int [] xO = new int [layers.length];
		int [] yO = new int [layers.length];
		xO[0] = 0;
		yO[0] = 0;  //  All of the 'placement offsets' are relative to the layer we're merging down onto.
		for(int a = 1; a < layers.length; a++){
			xO[a] = layers[a].getPlacementOffset().getX().intValue();
			yO[a] = layers[a].getPlacementOffset().getY().intValue();
		}
		int initialStartX = inputRegion.getRegion().getCanonicalLowerCoordinate().getX().intValue();
		int initialEndX = inputRegion.getRegion().getCanonicalUpperCoordinate().getX().intValue();

		int initialStartY = inputRegion.getRegion().getCanonicalLowerCoordinate().getY().intValue();
		int initialEndY = inputRegion.getRegion().getCanonicalUpperCoordinate().getY().intValue();

		int expandedStartX = initialStartX;
		int expandedEndX = initialEndX;

		int additionalStartExpansionX = 0;
		do{
			additionalStartExpansionX = getExpansionForCoordinate(true, expandedStartX, initialStartY, initialEndY, xO, yO, layers);
			expandedStartX -= additionalStartExpansionX;
		}while(additionalStartExpansionX > 0);

		int additionalEndExpansionX = 0;
		do{
			//  -1 because 'end' indicates the next character, not the current one:
			additionalEndExpansionX = getExpansionForCoordinate(false, expandedEndX -1, initialStartY, initialEndY, xO, yO, layers);
			expandedEndX += additionalEndExpansionX;
		}while(additionalEndExpansionX > 0);

		// This can potentially expand the region beyond boundaries of any specific layer
		// which is acceptable and necessary when the offset of one layer places characters
		// beyond the boundary of another layer.
		return new ScreenRegion(
			ScreenLayer.makeDimensionsCA(
				expandedStartX,
				initialStartY,
				expandedEndX,
				initialEndY
			)
		);
	}

	public String getMessageIfScreenHasNullCharacters() throws Exception{
		int width = this.getWidth();
		int height = this.getHeight();
		for(int j = 0; j < height; j++){
			for(int i = 0; i < width; ){
				if(this.getColumnCharacter(i, j) == null){
					return "Saw a null at i=" + i + ", j=" + j;
				}
				if(this.getColumnCharacterWidth(i, j) == 0){
					i++;
				}else{
					i += this.getColumnCharacterWidth(i, j) > 0 ? this.getColumnCharacterWidth(i, j) : 0;
				}
			}
		}
		return null;
	}
}
