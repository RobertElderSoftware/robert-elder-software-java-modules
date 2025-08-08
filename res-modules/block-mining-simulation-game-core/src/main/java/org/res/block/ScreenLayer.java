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

	public void clearFlags(){
		this.setAllChangedFlagStates(false);
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
		this.initializeInRegion(chrWidth, s, colourCodes, msg, new ScreenRegion(ScreenRegion.makeScreenRegionCA(0,0, getWidth(), getHeight())), true);
	}

	public void initializeInRegion(int chrWidth, String s, int [] colourCodes, String msg, ScreenRegion region, boolean defaultMaskState) throws Exception{
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
				this.changed[i][j] = defaultMaskState;
				this.active[i][j] = false;
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
		int [][][] currentCharacterWidths = new int [screenLayers.length][xWidth][yHeight];
		int [][][] activeCharacterLayer = new int [screenLayers.length][xWidth][yHeight]; //  The layer number of what we think the top chr is

		int loopStart = isLeftToRight ? startX : endX - 1;
		int loopEnd = isLeftToRight ? endX : startX - 1;
		int loopChange = isLeftToRight ? 1 : -1;
		int [][][] firstSolidLayers = new int [screenLayers.length][xWidth][yHeight];
		for(int s = screenLayers.length -1; s >= 0; s--){
			for(int j = startY; j < endY; j++){
				for(int iter = startX; iter < endX; iter++){
					int i = isLeftToRight ? iter : startX + xWidth - (iter - startX +1);
					int xR = i-startX;
					int yR = j-startY;
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
					int xSrc = i-xO[s];
					int ySrc = j-yO[s];
					if(isLeftToRight){
						//  For any characters that start in this position, start tracking them:
						if(xSrc >= 0 && xSrc < screenLayers[s].getWidth() && ySrc >= 0 && ySrc < screenLayers[s].getHeight()){
							currentCharacterWidths[s][xR][yR] = screenLayers[s].characterWidths[xSrc][ySrc];
						}
					}else{
						//  For any characters that start in this position, start tracking them:
						int backtrack = 0;
						while(
							(
								(xSrc-backtrack) >=0 &&
								(xSrc-backtrack) < screenLayers[s].getWidth() &&
								ySrc >=0 && 
								ySrc < screenLayers[s].getHeight()
							) && screenLayers[s].characterWidths[xSrc-backtrack][ySrc] == 0
						){
							backtrack++;
						}
						if((xSrc-backtrack) >=0 && activeStates[s][xR][yR]){
							int expectedWidth = backtrack + 1;
							if(xSrc >= 0 && xSrc < screenLayers[s].getWidth() && ySrc >= 0 && ySrc < screenLayers[s].getHeight()){
								if(screenLayers[s].characterWidths[xSrc-backtrack][ySrc] == expectedWidth){
									currentCharacterWidths[s][xR][yR] = expectedWidth;
								}
							}
						}
					}
					if(currentCharacterWidths[s][xR][yR] > 0 && activeStates[s][xR][yR]){
						columnsRemaining[s][xR][yR] = currentCharacterWidths[s][xR][yR];
					}else if(i != loopStart){
						columnsRemaining[s][xR][yR] = columnsRemaining[s][xR-loopChange][yR] -1;
					}
					if(activeCharacterLayer[s][xR][yR] != -1 && columnsRemaining[activeCharacterLayer[s][xR][yR]][xR][yR] <= 0 && activeStates[s][xR][yR]){
						activeCharacterLayer[s][xR][yR] = -1;
					}

					boolean hasSolidCharacter = columnsRemaining[s][xR][yR] > 0 && activeStates[s][xR][yR];
					if(hasSolidCharacter){ //  If there is a char here
						//  If the char starts at this position:
						if(activeCharacterLayer[s][xR][yR] < 0 && currentCharacterWidths[s][xR][yR] > 0){
							activeCharacterLayer[s][xR][yR] = s;
						}
					}

					if(s == screenLayers.length -1){
						//  For top layer
						firstSolidLayers[s][xR][yR] = hasSolidCharacter ? s : -1;
					}else{
						//  For layers below, first solid character will with be a higher solid character or the current layer, or nothing:
						firstSolidLayers[s][xR][yR] = firstSolidLayers[s+1][xR][yR] != -1 ? firstSolidLayers[s+1][xR][yR] : (hasSolidCharacter ? s : -1);
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

	public void mergeNonNullChangesDownOnto(ScreenLayer [] aboveLayers, boolean trustChangedFlags) throws Exception{

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
		
		Set<ScreenRegion> nonClippedRegions = new HashSet<ScreenRegion>();
		Set<ScreenRegion> clippedTranslatedRegions = new HashSet<ScreenRegion>();
		for(int l = 0; l < screenLayers.length; l++){
			if(screenLayers[l].getIsLayerActive()){
				for(ScreenRegion sourceRegion : screenLayers[l].getChangedRegions()){
					CuboidAddress destinationRegionCA = ScreenRegion.makeScreenRegionCA(
						sourceRegion.getStartX() + xO[l],
						sourceRegion.getStartY() + yO[l],
						sourceRegion.getEndX() + xO[l],
						sourceRegion.getEndY() + yO[l]
					);

					nonClippedRegions.add(new ScreenRegion(destinationRegionCA));

					CuboidAddress consideredRegion = destinationRegionCA.getIntersectionCuboidAddress(baseLayerCuboidAddress);
					ScreenRegion region = new ScreenRegion(consideredRegion);
					clippedTranslatedRegions.add(region);
				}
			}
			screenLayers[l].clearChangedRegions();
		}

		for(ScreenRegion region : clippedTranslatedRegions){
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
						activeStates[s][xR][yR] = layerActive && screenLayers[s].active[xSrc][ySrc];
						if(screenLayers[s].changed[xSrc][ySrc] && activeStates[s][xR][yR]){
							changeFlags[s][xR][yR] = true; //  Check all layers, just in case a layer underneath has a change of BG colour.
						}
					}
				}
			}

			this.calculateOcclusions(true, startX, endX, startY, endY, screenLayers, rightwardOcclusions, activeStates, xO, yO);
			this.calculateOcclusions(false, startX, endX, startY, endY, screenLayers, leftwardOcclusions, activeStates, xO, yO);

			for(int j = startY; j < endY; j++){
				for(int i = startX; i < endX; i++){
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
						throw new Exception("not expected");
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

					boolean hasChange = trustChangedFlags ? trustedChangeFlag : (
						!(
							(this.characterWidths[i][j] == outputCharacterWidths) &&
							Arrays.equals(this.colourCodes[i][j], outputColourCodes) &&
							Objects.equals(this.characters[i][j], outputCharacters)
						) || this.changed[i][j] // if there is a pending changed flag that hasn't been printed yet.
					);

					boolean finalActiveState = false;
					for(int s = screenLayers.length -1; s >= 0; s--){
						finalActiveState |= activeStates[s][i-startX][j-startY];
					}

					this.characters[i][j] = outputCharacters;
					this.characterWidths[i][j] = outputCharacterWidths;
					this.colourCodes[i][j] = outputColourCodes;
					this.changed[i][j] = hasChange;
					this.active[i][j] = finalActiveState;
				}
			}
		}
		//  If some of the changed regions overlap, there is a case where
		//  the calculated change flags can be incorrect due to them being
		//  cleared by a previous overlapping changed region.
		for(ScreenRegion region : nonClippedRegions){
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
		this.addChangedRegions(clippedTranslatedRegions);
	}

	private void nullifyPrecendingOverlappedCharacters(int x, int y){
		//  Look for any previous multi-column characters that would
		//  overlap with the current character.  If they exist, 
		//  overwrite them.
		int backtrack = 1;
		while((x - backtrack) >= 0){
			if(this.characterWidths[x-backtrack][y] > 0){
				int requiredCharacters = this.characterWidths[x-backtrack][y];
				int availableCharacters = backtrack;
				if(requiredCharacters > availableCharacters){
					// No space for found character, overwrite.
					for(int g = x - backtrack; g < x; g++){
						this.characterWidths[g][y] = 1;
						this.characters[g][y] = " ";
						this.changed[g][y] = true;
					}
					//  Continue in case there are multiple
					//  multi-column characters that would
					//  have overlapped.
				}else{
					// No overlap, do nothing
					break;
				}
			}
			backtrack++;
		}
	}

	public boolean mergeChanges(ScreenLayer changes) throws Exception{
		Set<ScreenRegion> regions = changes.getChangedRegions();
		int xOffset = changes.getPlacementOffset().getX().intValue();
		int yOffset = changes.getPlacementOffset().getY().intValue();
		for(ScreenRegion sourceRegion : regions){
			int changesInRegion = 0;
			//  Determine the subset of the change that's actually lands within the destination layer
			CuboidAddress destinationRegionCA = ScreenRegion.makeScreenRegionCA(
				sourceRegion.getStartX() + xOffset,
				sourceRegion.getStartY() + yOffset,
				sourceRegion.getEndX() + xOffset,
				sourceRegion.getEndY() + yOffset
			);

			CuboidAddress consideredRegion = destinationRegionCA.getIntersectionCuboidAddress(this.getDimensions());
			ScreenRegion region = new ScreenRegion(consideredRegion);

			int startX = region.getStartX();
			int startY = region.getStartY();
			int endX = region.getEndX();
			int endY = region.getEndY();
			for(int j = startY; j < endY; j++){
				int i = startX;
				while(i < endX){
					int x = i;
					int y = j;
					int xF = i - xOffset;
					int yF = j - yOffset;
					String newCharacter = changes.characters[xF][yF];
					int newCharacterWidth = changes.characterWidths[xF][yF];

					if(changes.active[xF][yF]){
						//  If a multi-column character is falling off the edge of the screen, just set it to null:
						if(!((i+newCharacterWidth) <= endX)){
							newCharacter = " ";
							newCharacterWidth = 1;
						}
						boolean hasChanged = !(
							(this.characterWidths[x][y] == newCharacterWidth) &&
							Arrays.equals(this.colourCodes[x][y], changes.colourCodes[xF][yF]) &&
							Objects.equals(this.characters[x][y], newCharacter)
						) || this.changed[x][y]; //  In case multiple writes happened since last commit
						this.changed[x][y] = hasChanged;
						this.characterWidths[x][y] = newCharacterWidth;
						this.colourCodes[x][y] = changes.colourCodes[xF][yF];
						this.characters[x][y] = newCharacter;
						
						//  For multi-column characters, explicitly initialize any 'covered' characters as null to resolve printing glitches:
						for(int k = 1; (k < newCharacterWidth) && (k+x) < endX; k++){
							this.characterWidths[x+k][y] = 0;
							this.colourCodes[x+k][y] = this.colourCodes[x][y];
							this.characters[x+k][y] = null;
							this.changed[x+k][y] = this.changed[x][y];
							changes.changed[xF+k][yF] = false; //  Clear changed flag.
						}
						this.nullifyPrecendingOverlappedCharacters(x, y);
						changes.changed[xF][yF] = false; //  Clear changed flag.
						if(hasChanged){
							changesInRegion++;
						}
					}
					i += (newCharacterWidth < 1) ? 1 : newCharacterWidth; 
					this.active[x][y] = true;
				}
			}

			if(changesInRegion > 0){
				this.addChangedRegion(region);
			}
		}
		changes.clearChangedRegions();
		return this.setIsLayerActive(changes.getIsLayerActive());
	}

	public void printChanges(boolean useRightToLeftPrint, boolean resetCursorPosition, int xOffset, int yOffset) throws Exception{
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
				for(int i = startColumn; i != endColumn; i += loopUpdate){
					//  Try to intelligently issue as few ANSI escape sequences as possible:
					if(!Arrays.equals(this.colourCodes[i][j], lastUsedColourCodes)){
						mustSetColourCodes = true;
					}
					//  These should always be initialized to empty array.
					if(this.colourCodes[i][j] == null){
						throw new Exception("this.colourCodes[i][j] == null");
					}
					if(
						this.changed[i][j]
					){
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
						if(this.characters[i][j] != null){
							this.stringBuilder.append(this.characters[i][j]);
						}
						this.changed[i][j] = false;
					}else{
						mustSetCursorPosition = true;
					}
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

						characters = this.characters[i][j];
					}else{
						characters = "#";
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
}
