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
	public int [][] characterWidths = null;
	public int [][][] colourCodes = null;
	public String [][] characters = null;
	public boolean [][] changed = null;
	public boolean [][] active = null;
	private int [] defaultColourCodes = new int [] {};
	private Set<ScreenRegion> changedRegions = new HashSet<ScreenRegion>();
	private final StringBuilder stringBuilder = new StringBuilder();

	public boolean getIsLayerActive(){
		return this.isLayerActive;
	}

	public static CuboidAddress makeDimensionsCA(int startX, int startY, int endX, int endY) throws Exception{
		return new CuboidAddress(
			new Coordinate(Arrays.asList((long)startX, (long)startY)),
			new Coordinate(Arrays.asList((long)endX, (long)endY))
		);
	}

	public boolean setIsLayerActive(boolean isLayerActive) throws Exception{
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

	public void setPlacementOffset(Coordinate placementOffset){
		this.placementOffset = placementOffset;
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
		this.characterWidths = new int [width][height];
		this.colourCodes = new int [width][height][];
		this.characters = new String [width][height];
		this.changed = new boolean [width][height];
		this.active = new boolean [width][height];
		this.dimensions = dimensions;
		this.placementOffset = placementOffset;
	}

	public void setAllChangedFlagStates(boolean state){
		int width = this.getWidth();
		int height = this.getHeight();
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.changed[i][j] = state;
			}
		}
	}

	public void setAllActiveFlagStates(boolean state){
		int width = this.getWidth();
		int height = this.getHeight();
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.active[i][j] = state;
			}
		}
	}

	public ScreenLayer(ScreenLayer l){
		this.placementOffset = l.getPlacementOffset();
		this.dimensions = l.getDimensions();
		int width = l.getWidth();
		int height = l.getHeight();
		this.characterWidths = new int [width][height];
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.characterWidths[i][j] = l.characterWidths[i][j];
			}
		}
		this.colourCodes = new int [width][height][];
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.colourCodes[i][j] = new int [l.colourCodes[i][j].length];
				for(int k = 0; k < l.colourCodes[i][j].length; k++){
					this.colourCodes[i][j][k] = l.colourCodes[i][j][k];
				}
			}
		}
		this.characters = new String [width][height];
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.characters[i][j] = l.characters[i][j];
			}
		}
		this.changed = new boolean [width][height];
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.changed[i][j] = l.changed[i][j];
			}
		}
		this.active = new boolean [width][height];
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.active[i][j] = l.active[i][j];
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

		for(int i = startX; i < endX; i++){
			for(int j = startY; j < endY; j++){
				this.characterWidths[i][j] = chrWidth;
				this.colourCodes[i][j] = colourCodes;
				this.characters[i][j] = s;
				this.changed[i][j] = defaultChangedFlag;
				this.active[i][j] = defaultActiveFlag; //  Require the user to explicitly enable this column.
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
					this.characters[xOffset + i][yOffset] = String.valueOf(msg.charAt(i));
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
				this.changed[i + startX][j + startY] = state;
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

	public void calculateOcclusions(boolean isLeftToRight, int startX, int endX, int startY, int endY, ScreenLayer [] screenLayers, int [][][] occlusions, boolean [][][] activeStates, int [] xO, int [] yO) throws Exception{
		int xWidth = endX-startX;
		int yHeight = endY-startY;
		//  Number of columns remaining in the current active, top character:
		int [][][] columnsRemaining = new int [screenLayers.length][xWidth][yHeight];
		//  The width of a character that we're starting on (in left to right pass)
		//  or ending on (in right to left pass):
		int [][][] activeCharacterLayer = new int [screenLayers.length][xWidth][yHeight]; //  The layer number of what we think the top chr is

		int loopStart = isLeftToRight ? startX : endX - 1;
		int loopEnd = isLeftToRight ? endX : startX - 1;
		int loopChange = isLeftToRight ? 1 : -1;
		int [][][] firstSolidLayers = new int [screenLayers.length][xWidth][yHeight];

		int [][][] currentCharacterWidths = new int [screenLayers.length][xWidth][yHeight];

		//  Pre-calculate character width positions:
		for(int s = screenLayers.length -1; s >= 0; s--){
			for(int j = startY; j < endY; j++){
				for(int i = startX; i < endX; i++){
					if(isLeftToRight){
						int xSrc = i-xO[s];
						int ySrc = j-yO[s];
						int xR = i-startX;
						int yR = j-startY;
						if(xSrc >= 0 && xSrc < screenLayers[s].getWidth() && ySrc >= 0 && ySrc < screenLayers[s].getHeight()){
							currentCharacterWidths[s][xR][yR] = screenLayers[s].characterWidths[xSrc][ySrc];
						}
					}else{
						//  Right to left pass puts width at end:
						int xSrc = i-xO[s];
						int ySrc = j-yO[s];
						if(xSrc >= 0 && xSrc < screenLayers[s].getWidth() && ySrc >= 0 && ySrc < screenLayers[s].getHeight()){
							int chrWidth = screenLayers[s].characterWidths[xSrc][ySrc];
							if(chrWidth > 0){
								int end_i = i + chrWidth -1; //  Offset of end of char
								int xR = end_i - startX;
								int yR = j-startY;
								if(xR >= 0 && xR < xWidth){
									currentCharacterWidths[s][xR][yR] = screenLayers[s].characterWidths[xSrc][ySrc];
								}
							}
						}
					}
				}
			}
		}

		for(int s = screenLayers.length -1; s >= 0; s--){
			for(int j = startY; j < endY; j++){
				for(int iter = startX; iter < endX; iter++){
					int i = isLeftToRight ? iter : startX + xWidth - (iter - startX +1);
					int xR = i-startX;
					int yR = j-startY;

					int xSrc = i-xO[s];
					int ySrc = j-yO[s];

					if(i == loopStart && s == screenLayers.length -1){
						//  Initial value:
						activeCharacterLayer[s][xR][yR] = -1;
					}else if(i == loopStart){
						//  Use whatever was above:
						activeCharacterLayer[s][xR][yR] = activeCharacterLayer[s+1][xR][yR];
					}else if(s == screenLayers.length -1){
						//  Use whatever was before:
						activeCharacterLayer[s][xR][yR] = activeCharacterLayer[s][xR-loopChange][yR];
					}else{
						//  Use active layer above, otherwise, use previous active layer:
						activeCharacterLayer[s][xR][yR] = activeCharacterLayer[s+1][xR][yR] > 0 ? activeCharacterLayer[s+1][xR][yR] : activeCharacterLayer[s][xR-loopChange][yR];
					}

					boolean isAtCharacterStartPosition = currentCharacterWidths[s][xR][yR] > 0;
					if(isAtCharacterStartPosition && activeStates[s][xR][yR]){
						columnsRemaining[s][xR][yR] = currentCharacterWidths[s][xR][yR];
					}else if(i != loopStart){
						columnsRemaining[s][xR][yR] = columnsRemaining[s][xR-loopChange][yR] -1;
					}

					if(
						activeCharacterLayer[s][xR][yR] != -1 && //  If no character is currently active
						columnsRemaining[activeCharacterLayer[s][xR][yR]][xR][yR] <= 0 //  And we're not currently inside a character
					){
						activeCharacterLayer[s][xR][yR] = -1; //  Exit being active from this character.
					}


					boolean hasSolidCharacter = columnsRemaining[s][xR][yR] > 0 && activeStates[s][xR][yR];
					if(s == screenLayers.length -1){
						//  For top layer
						firstSolidLayers[s][xR][yR] = hasSolidCharacter ? s : -1;
					}else{
						//  For layers below, first solid character will with be a higher solid character or the current layer, or nothing:
						firstSolidLayers[s][xR][yR] = firstSolidLayers[s+1][xR][yR] != -1 ? firstSolidLayers[s+1][xR][yR] : (hasSolidCharacter ? s : -1);
					}

					if(hasSolidCharacter){ //  If there is a char here
						//  If the char starts at this position:
						if(activeCharacterLayer[s][xR][yR] < 0 && isAtCharacterStartPosition && s >= firstSolidLayers[s][xR][yR]){
							activeCharacterLayer[s][xR][yR] = s;
						}
					}

					if(firstSolidLayers[s][xR][yR] == -1){
						occlusions[s][xR][yR] = -2; // No Character Found.
					}else if(firstSolidLayers[s][xR][yR] == activeCharacterLayer[s][xR][yR]){
						//  This character is included in left to right pass
						occlusions[s][xR][yR] = activeCharacterLayer[s][xR][yR];
					}else if(firstSolidLayers[s][xR][yR] >=0){
						occlusions[s][xR][yR] = -1; // Ocluded character
					}else{
						throw new Exception("Not expected.");
					}

				}
			}
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
		xO[0] = 0;
		yO[0] = 0;  //  All of the 'placement offsets' are relative to the layer we're merging down onto.
		for(int a = 0; a < aboveLayers.length; a++){
			screenLayers[a+1] = aboveLayers[a];  //  All the layers above to merge down
			xO[a+1] = aboveLayers[a].getPlacementOffset().getX().intValue();
			yO[a+1] = aboveLayers[a].getPlacementOffset().getY().intValue();
		}

		//  Everything gets merged down into a coordinate system based on layer 0
		CuboidAddress baseLayerCuboidAddress = ScreenRegion.makeScreenRegionCA(
			0,
			0,
			this.getWidth(),
			this.getHeight()
		);
		
		Set<ScreenRegion> translatedExpandedRegions = new HashSet<ScreenRegion>();
		Set<ScreenRegion> translatedExpandedClippedRegions = new HashSet<ScreenRegion>();
		for(int l = 0; l < screenLayers.length; l++){
			if(screenLayers[l].getIsLayerActive()){
				for(ScreenRegion sourceRegion : screenLayers[l].getChangedRegions()){
					ScreenRegion translatedRegion = new ScreenRegion(ScreenRegion.makeScreenRegionCA(
						sourceRegion.getStartX() + xO[l],
						sourceRegion.getStartY() + yO[l],
						sourceRegion.getEndX() + xO[l],
						sourceRegion.getEndY() + yO[l]
					));

					ScreenRegion expandedRegion = ScreenLayer.getNonCharacterCuttingChangedRegions(translatedRegion, screenLayers);
					ScreenRegion clippedRegion = new ScreenRegion(expandedRegion.getRegion().getIntersectionCuboidAddress(baseLayerCuboidAddress));

					translatedExpandedClippedRegions.add(clippedRegion);
					translatedExpandedRegions.add(expandedRegion);
				}
			}
			screenLayers[l].clearChangedRegions();
		}

		for(ScreenRegion region : translatedExpandedRegions){
			int startX = region.getStartX();
			int startY = region.getStartY();
			int endX = region.getEndX();
			int endY = region.getEndY();

			int xWidth = endX - startX;
			int yHeight = endY - startY;
			int [][][] rightwardOcclusions = new int [screenLayers.length][xWidth][yHeight];
			int [][][] leftwardOcclusions = new int [screenLayers.length][xWidth][yHeight];
			//  Pre-calculate the active states for all layers in the entire current horizontal strip:
			boolean [][][] changeFlags = new boolean [screenLayers.length][xWidth][yHeight];
			boolean [][][] activeStates = new boolean [screenLayers.length][xWidth][yHeight];

			for(int s = screenLayers.length -1; s >= 0; s--){
				int innerStartX = startX + (-(Math.min(startX - xO[s], 0)));
				int innerStartY = startY + (-(Math.min(startY - yO[s], 0)));
				int innerEndX = endX + Math.min(screenLayers[s].getWidth() - (endX -xO[s]), 0);
				int innerEndY = endY + Math.min(screenLayers[s].getHeight() - (endY -yO[s]), 0);
				for(int j = innerStartY; j < innerEndY; j++){
					boolean layerActive = screenLayers[s].getIsLayerActive();
					for(int i = innerStartX; i < innerEndX; i++){
						int xSrc = i-xO[s];
						int ySrc = j-yO[s];
						int xR = i-startX;
						int yR = j-startY;
						//  For any character in layer 0, always consider it as active regardless of whether it's 'inactive' or not:
						boolean fbls = forcedBottomLayerState.toBoolean();
						activeStates[s][xR][yR] = (s == 0) ? fbls : (layerActive && screenLayers[s].active[xSrc][ySrc]);
						if(screenLayers[s].changed[xSrc][ySrc] && activeStates[s][xR][yR]){
							changeFlags[s][xR][yR] = true; //  Check all layers, just in case a layer underneath has a change of BG colour.
						}
					}
				}
			}

			this.calculateOcclusions(true, startX, endX, startY, endY, screenLayers, rightwardOcclusions, activeStates, xO, yO);
			this.calculateOcclusions(false, startX, endX, startY, endY, screenLayers, leftwardOcclusions, activeStates, xO, yO);

			for(int j = Math.max(0, startY); j < Math.min(screenLayers[0].getHeight(), endY); j++){
				int currentCharacterWidth = 0;
				int offsetIntoCharacter = 0;
				int outputStartX = Math.max(0, startX);
				int outputEndX = Math.min(screenLayers[0].getWidth(), endX);
				boolean rightBoundaryHasSeveredCharacter = false;
				boolean leftBoundaryHasSeveredCharacter = true;

				boolean firstColumnHasChange = true;
				int [] firstColumnColourCodes = new int [] {};
				for(int i = startX; i < outputEndX; i++){
					int xR = i-startX;
					int yR = j-startY;
					String outputCharacters = null;
					int outputCharacterWidths = 0;
					int [] outputColourCodes = new int []{};
					//  Colour codes come from top most coloured character:
					for(int s = screenLayers.length -1; s >= 0; s--){
						int xSrc = i-xO[s];
						int ySrc = j-yO[s];
						if(xSrc >= 0 && xSrc < screenLayers[s].getWidth() && ySrc >= 0 && ySrc < screenLayers[s].getHeight()){
							if(screenLayers[s].colourCodes[xSrc][ySrc].length > 0 && activeStates[s][xR][yR]){
								outputColourCodes = screenLayers[s].colourCodes[xSrc][ySrc];
								break;
							}
						}
					}

					int rightward = rightwardOcclusions[0][xR][yR];
					int leftward = leftwardOcclusions[0][xR][yR];
					boolean trustedChangeFlag = false;
					if(rightward >=0 || leftward >= 0){
						if(rightward == leftward){
							int xSrc = i-xO[rightward];
							int ySrc = j-yO[rightward];
							outputCharacters = screenLayers[rightward].characters[xSrc][ySrc];
							outputCharacterWidths = screenLayers[rightward].characterWidths[xSrc][ySrc];
							trustedChangeFlag  = changeFlags[rightward][xR][yR];
						}else if(rightward >= 0){
							outputCharacters = " ";
							outputCharacterWidths = 1;
							trustedChangeFlag  = true;
						}else if(leftward >= 0){
							outputCharacters = " ";
							outputCharacterWidths = 1;
							trustedChangeFlag  = true;
						}
					}else if(rightward == -1 && leftward == -1){
						//  Empty null column in upper layer exposing center column of multi-column character:
						outputCharacters = " ";
						outputCharacterWidths = 1;
						trustedChangeFlag  = true;
					}else if(rightward == -2 && leftward == -1){
						throw new Exception("not expected");
					}else if(rightward == -1 && leftward == -2){
						throw new Exception("not expected");
					}else if(rightward == -2 && leftward == -2){
						outputCharacters = null;
						outputCharacterWidths = 0;
					}else{
						throw new Exception("not expected");
					}


					//  Check for any changed flags down to the first solid character:
					for(int s = screenLayers.length -1; s >= 0; s--){
						if(activeStates[s][xR][yR]){
							int xSrc = i-xO[s];
							int ySrc = j-yO[s];
							trustedChangeFlag |= changeFlags[s][xR][yR];
							if(screenLayers[s].characters[xSrc][ySrc] != null){
								//  Found solid character:
								break;
							}
						}
					}
					//  Check for any changed flags down to the first non-empty colour code
					for(int s = screenLayers.length -1; s >= 0; s--){
						if(activeStates[s][xR][yR]){
							int xSrc = i-xO[s];
							int ySrc = j-yO[s];
							trustedChangeFlag |= screenLayers[s].colourCodes[xSrc][ySrc].length > 0 && changeFlags[s][xR][yR];
							if(screenLayers[s].colourCodes[xSrc][ySrc].length > 0){
								//  Found colour codes
								break;
							}
						}
					}

					if((outputEndX - i) < outputCharacterWidths){
						rightBoundaryHasSeveredCharacter = true;
					}

					if(offsetIntoCharacter == 0 && !(i < outputStartX)){
						leftBoundaryHasSeveredCharacter = false;
					}

					if((!(i < outputStartX) && leftBoundaryHasSeveredCharacter) || rightBoundaryHasSeveredCharacter){
						outputCharacters = " ";
						outputCharacterWidths = 1;
						trustedChangeFlag  = true;
						firstColumnColourCodes = outputColourCodes;
						firstColumnHasChange = trustedChangeFlag;
						
					}

					if(offsetIntoCharacter == 0){
						if(outputCharacterWidths > 0){
							currentCharacterWidth = outputCharacterWidths;
						}else{
							currentCharacterWidth = 0;
						}
					}

					if(offsetIntoCharacter > 0){
						//  For multi-column characters, use changed flag from first column.
						outputColourCodes = firstColumnColourCodes;
						trustedChangeFlag = firstColumnHasChange;
					}

					if(i < outputStartX){ //  We are before starting boundary of output layer
						if(offsetIntoCharacter == 0){
							firstColumnColourCodes = outputColourCodes;
							firstColumnHasChange = true;
						}
						offsetIntoCharacter++;
						if(offsetIntoCharacter >= currentCharacterWidth){
							offsetIntoCharacter = 0;
						}
						continue;
					}



					boolean hasChange = false;
					if(trustChangedFlags){
						hasChange = trustedChangeFlag;
					}else{
						if(offsetIntoCharacter > 0){
							//  For multi-column characters, use changed flag from first column.
							hasChange = firstColumnHasChange;
						}else{
							hasChange = !(
								(this.characterWidths[i][j] == outputCharacterWidths) &&
								Arrays.equals(this.colourCodes[i][j], outputColourCodes) &&
								Objects.equals(this.characters[i][j], outputCharacters)
							) || this.changed[i][j]; // if there is a pending changed flag that hasn't been printed yet.
						}
					}

					boolean finalActiveState = false;
					for(int s = screenLayers.length -1; s >= 0; s--){
						finalActiveState |= activeStates[s][i-startX][j-startY];
					}


					this.characters[i][j] = outputCharacters;
					this.characterWidths[i][j] = outputCharacterWidths;
					this.colourCodes[i][j] = outputColourCodes;
					this.changed[i][j] = hasChange;
					this.active[i][j] = finalActiveState;
					if(offsetIntoCharacter == 0){
						firstColumnHasChange = this.changed[i][j];
						firstColumnColourCodes = this.colourCodes[i][j];
					}
					offsetIntoCharacter++;
					if(offsetIntoCharacter >= currentCharacterWidth){
						offsetIntoCharacter = 0;
					}
				}
			}
		}
		//  If some of the changed regions overlap, there is a case where
		//  the calculated change flags can be incorrect due to them being
		//  cleared by a previous overlapping changed region.
		for(ScreenRegion region : translatedExpandedRegions){
			for(int s = screenLayers.length -1; s >= 1; s--){
				int startX = Math.max(region.getStartX() - xO[s], 0);
				int startY = Math.max(region.getStartY() - yO[s], 0);
				int endX = Math.min(region.getEndX() - xO[s], screenLayers[s].getWidth());
				int endY = Math.min(region.getEndY() - yO[s], screenLayers[s].getHeight());
				for(int j = startY; j < endY; j++){
					for(int i = startX; i < endX; i++){
						//  Clear the changed flag for any active layer other than the merged layer:
						boolean isLayerActive = screenLayers[s].getIsLayerActive() && screenLayers[s].active[i][j];
						if(isLayerActive){
							screenLayers[s].changed[i][j] = false;
						}
					}
				}
			}
		}
		this.addChangedRegions(translatedExpandedClippedRegions);
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
					if(!Arrays.equals(this.colourCodes[i][j], lastUsedColourCodes)){
						mustSetColourCodes = true;
					}
					if(chrsLeft <= 0){
						chrsLeft = this.characterWidths[i][j];
					}
					if(
						this.changed[i][j]
					){
						if(useCompatibilityWidth){
							String currentPositionSequence = "\033[" + (j+1+yOffset) + ";" + (i+1+xOffset) + "H";
							this.stringBuilder.append(currentPositionSequence);

							List<String> codes = new ArrayList<String>();
							for(int c : this.colourCodes[i][j]){
								codes.add(String.valueOf(c));
							}
							String currentColorSequence = "\033[0m\033[" + String.join(";", codes) + "m";
							this.stringBuilder.append(currentColorSequence);
							mustSetColourCodes = resetState;
							lastUsedColourCodes = this.colourCodes[i][j];

							for(int k = 0; k < this.characterWidths[i][j]; k++){
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
							for(int c : this.colourCodes[i][j]){
								codes.add(String.valueOf(c));
							}
							String currentColorSequence = "\033[0m\033[" + String.join(";", codes) + "m";
							this.stringBuilder.append(currentColorSequence);
							mustSetColourCodes = resetState;
							lastUsedColourCodes = this.colourCodes[i][j];
						}
						if(this.characters[i][j] == null && chrsLeft == 0){
							this.stringBuilder.append("\033[43mX\033[0m"); // Highlight Nulls
						}else if(this.characters[i][j] == null){
						}else{
							this.stringBuilder.append(this.characters[i][j]);
						}
						this.changed[i][j] = false;
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
					if(this.characters[i][j] != null){

						if(this.characterWidths[i][j] > 1){
							//  For multi-column characters show numer of full width:
							characters = "" + (this.characterWidths[i][j] % 10);
						}else{
							characters = this.characters[i][j];
						}
					}else{
						int offset = getNextCharacterStartToLeft(i, j, this);
						if(offset != -1 && ((this.characterWidths[i-offset][j] - offset) > 0)){
							//  A null that's part of a multi-column character:
							characters = "_";
						}else{
							//  A random empty null area that's not part of a character.
							characters = "#";
						}
					}
					if(this.colourCodes[i][j] != null){
						colourCodes = this.colourCodes[i][j];
					}else{
						colourCodes = new int []{UserInterfaceFrameThreadState.CROSSED_OUT_COLOR};
					}
				}else if(debugType.equals("active")){
					if(this.active[i][j] && this.getIsLayerActive()){
						colourCodes = new int []{UserInterfaceFrameThreadState.GREEN_BG_COLOR};
					}else if(this.active[i][j] && !this.getIsLayerActive()){
						colourCodes = new int []{UserInterfaceFrameThreadState.BLUE_BG_COLOR, UserInterfaceFrameThreadState.CROSSED_OUT_COLOR, UserInterfaceFrameThreadState.RED_FG_COLOR};
					}else if(!this.active[i][j] && this.getIsLayerActive()){
						colourCodes = new int []{UserInterfaceFrameThreadState.BLACK_BG_COLOR};
					}else if(!this.active[i][j] && !this.getIsLayerActive()){
						colourCodes = new int []{UserInterfaceFrameThreadState.RED_BG_COLOR};
					}
					if(isInChangedRegion && this.active[i][j]){
						characters = "*";
					}else{
						characters = " ";
					}
				}else if(debugType.equals("changed")){
					if(this.changed[i][j]){
						colourCodes = new int []{UserInterfaceFrameThreadState.GREEN_BG_COLOR};
					}else{
						colourCodes = new int []{UserInterfaceFrameThreadState.BLACK_BG_COLOR};
					}
					if(isInChangedRegion && this.changed[i][j]){
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

	public String validate() throws Exception{
		//   Check for scenarios that should be impossible that
		//   the layer merging algorithms don't account for.
		int columnsRemaining = 0;
		int [] currentColourCodes = new int [] {};
		boolean currentActiveState = false;
		boolean currentChangedState = false;
		for(int j = 0; j < this.getHeight(); j++){
			for(int i = 0; i < this.getWidth(); i++){
				if(this.colourCodes[i][j] == null){
					return "Saw null colour codes at x=" + i + ", y=" + j + ".";
				}
				boolean insideMultiColumnCharacter = columnsRemaining > 0;
				if(insideMultiColumnCharacter){
					if(this.characterWidths[i][j] > 0){
						return "Saw a non zero character width inside another character at x=" + i + ", y=" + j + ".";
					}
					if(!Arrays.equals(this.colourCodes[i][j], currentColourCodes)){
						return "Saw an inconsistent colour code inside a multi-column character that did not match at x=" + i + ", y=" + j + ".";
					}
					if(!Objects.equals(this.active[i][j], currentActiveState)){
						return "Saw an inconsistent active state inside a multi-column character that did not match at x=" + i + ", y=" + j + ".";
					}
					if(!Objects.equals(this.changed[i][j], currentChangedState)){
						return "Saw an inconsistent changed state inside a multi-column character that did not match at x=" + i + ", y=" + j + ".";
					}
					if(this.characters[i][j] != null){
						return "Saw non-null characters inside a multi-column character at x=" + i + ", y=" + j + ".";
					}
					columnsRemaining--;
					if(columnsRemaining == 0){
						// Reset colour codes for next character.
						currentColourCodes = new int [] {};
					}
				}else{
					if(this.characterWidths[i][j] > 0){ // Start of a new character
						if(this.characters[i][j] == null){
							return "Saw character with specified width, but null characters at x=" + i + ", y=" + j + ".";
						}
						//  Started a new character
						columnsRemaining = this.characterWidths[i][j] -1;
						currentColourCodes = this.colourCodes[i][j];
						currentChangedState = this.changed[i][j];
						currentActiveState = this.active[i][j];
					}else{
						//  An empty null character.  This is just an empty area.
						if(this.colourCodes[i][j] == null){
							return "Saw null colour codes inside a stray null character at x=" + i + ", y=" + j + ".";
						}
						if(this.colourCodes[i][j].length > 0){
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
			if(layer.characterWidths[currentX][currentY] > 0){
				return startX - currentX;
			}
			currentX--;
		}
		return -1;
	}

	public static int getNextCharacterStartToRight(int startX, int currentY, ScreenLayer layer){
		int currentX = startX;
		while(
			currentX >= 0 &&
			currentX < layer.getWidth() &&
			currentY >= 0 &&
			currentY < layer.getHeight()
		){
			if(layer.characterWidths[currentX][currentY] > 0){
				return currentX - startX;
			}
			currentX++;
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
					int characterWidth = layers[s].characterWidths[currentX - leftDistance][currentY];
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
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				if(this.characters[i][j] == null){
					return "Saw a null at i=" + i + ", j=" + j;
				}
			}
		}
		return null;
	}
}
