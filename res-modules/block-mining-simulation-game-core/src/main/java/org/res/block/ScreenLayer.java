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

	public void setIsLayerActive(boolean isLayerActive) throws Exception{
		if(this.isLayerActive != isLayerActive){
			this.isLayerActive = isLayerActive;
			this.addChangedRegion(new ScreenRegion(this.getDimensions()));
			this.setAllFlagStates(true);
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

	public void setAllFlagStates(boolean state){
		int width = this.getWidth();
		int height = this.getHeight();
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.changed[i][j] = state;
			}
		}
	}

	public void clearFlags(){
		this.setAllFlagStates(false);
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

	public void calculateOcclusions(boolean isLeftToRight, int j, int startX, int endX, ScreenLayer [] screenLayers, boolean [] changeFlags, int [] occlusions, boolean [][] activeStates) throws Exception{
		//  Number of columns remaining in the current active, top character:
		int [] columnsRemaining = new int [screenLayers.length];
		//  The width of a character that we're starting on (in left to right pass)
		//  or ending on (in right to left pass):
		int [] currentCharacterWidths = new int [screenLayers.length];
		for(int s = screenLayers.length -1; s >= 0; s--){
			columnsRemaining[s] = 0;
		}
		int activeCharacterLayer = -1; //  The layer number of what we think the top chr is

		int loopStart = isLeftToRight ? startX : endX - 1;
		int loopEnd = isLeftToRight ? endX : startX - 1;
		int loopChange = isLeftToRight ? 1 : -1;
		for(int i = loopStart; i != loopEnd; i += loopChange){
			if(isLeftToRight){
				//  For any characters that start in this position,
				//  start tracking them:
				for(int s = screenLayers.length -1; s >= 0; s--){
					currentCharacterWidths[s] = screenLayers[s].characterWidths[i][j];
				}
			}else{
				//  For any characters that start in this position,
				//  start tracking them:
				for(int s = screenLayers.length -1; s >= 0; s--){
					int backtrack = 0;
					while(((i-startX-backtrack) >=0) && screenLayers[s].characterWidths[i-startX-backtrack][j] == 0){
						backtrack++;
					}
					if((i-startX-backtrack) >=0 && activeStates[s][i-startX]){
						int expectedWidth = backtrack + 1;
						currentCharacterWidths[s] = screenLayers[s].characterWidths[i-startX-backtrack][j] == expectedWidth ? expectedWidth : 0;
					}else{
						currentCharacterWidths[s] = 0;
					}
				}
			}

			for(int s = screenLayers.length -1; s >= 0; s--){
				if(currentCharacterWidths[s] > 0 && activeStates[s][i-startX]){
					columnsRemaining[s] = currentCharacterWidths[s];
				}
			}

			int firstSolidLayer = -1;
			//  Find the layer of the top solid character:
			for(int s = screenLayers.length -1; s >= 0; s--){
				if(columnsRemaining[s] > 0 && activeStates[s][i-startX]){ //  If there is a char here
					firstSolidLayer = s;
					//  If the char starts at this position:
					if(currentCharacterWidths[s] > 0){
						activeCharacterLayer = s;
					}
					break;
				}
			}

			if(firstSolidLayer == -1){  
				occlusions[i-startX] = -2; // No Character Found.
			}else if(firstSolidLayer == activeCharacterLayer){
				//  This character is included in left to right pass
				occlusions[i-startX] = activeCharacterLayer;
			}else if(firstSolidLayer >=0){
				occlusions[i-startX] = -1; // Ocluded character
			}else{
				throw new Exception("Not expected.");
			}

			for(int s = screenLayers.length -1; s >= 0; s--){
				columnsRemaining[s]--;
				if(activeCharacterLayer != -1 && columnsRemaining[activeCharacterLayer] <= 0 && activeStates[s][i-startX]){
					activeCharacterLayer = -1;
				}
			}
		}
	}

	public void mergeNonNullChangesDownOnto(ScreenLayer [] aboveLayers, boolean trustChangedFlags) throws Exception{
		ScreenLayer [] screenLayers = new ScreenLayer [aboveLayers.length +1];
		screenLayers[0] = this;  //  Bottom layer should be current layer.
		for(int a = 0; a < aboveLayers.length; a++){
			screenLayers[a+1] = aboveLayers[a];  //  All the layers above to merge down
		}
		
		Set<ScreenRegion> aboveRegions = new HashSet<ScreenRegion>();
		aboveRegions.addAll(this.getChangedRegions());
		for(int l = 0; l < aboveLayers.length; l++){
			aboveRegions.addAll(aboveLayers[l].getChangedRegions());
			aboveLayers[l].clearChangedRegions();
		}
		for(ScreenRegion region : aboveRegions){
			int startX = region.getStartX();
			int startY = region.getStartY();
			int endX = region.getEndX();
			int endY = region.getEndY();
			for(int j = startY; j < endY; j++){
				int xWidth = endX - startX;
				boolean [] changeFlags = new boolean [xWidth];
				int [] rightwardOcclusions = new int [xWidth];
				int [] leftwardOcclusions = new int [xWidth];
				//  Pre-calculate the active states for all layers in the entire current horizontal strip:
				boolean [][] activeStates = new boolean [screenLayers.length][xWidth];
				boolean [] finalActiveStates = new boolean [xWidth];
				for(int i = startX; i < endX; i++){
					finalActiveStates[i-startX] = false;
				}

				for(int s = screenLayers.length -1; s >= 0; s--){
					boolean layerActive = screenLayers[s].getIsLayerActive();
					for(int i = startX; i < endX; i++){
						activeStates[s][i-startX] = layerActive && screenLayers[s].active[i-startX][j];
						finalActiveStates[i-startX] |= activeStates[s][i-startX];
					}
				}

				for(int i = startX; i < endX; i++){
					changeFlags[i-startX] = false;
					for(int s = screenLayers.length -1; s >= 0; s--){
						if(screenLayers[s].changed[i][j] && activeStates[s][i-startX]){
							changeFlags[i-startX] = true; //  Check all layers, just in case a layer underneath has a change of BG colour.
						}
						//  For any layer above the final 'merged' result:
						if(s > 0){
							//  Only clear the changed flag if it's an active layer.
							if(screenLayers[s].active[i-startX][j] && screenLayers[s].getIsLayerActive()){
								screenLayers[s].changed[i][j] = false; // Clear the pending changed flag for this layer.
							}
						}
					}
				}

				this.calculateOcclusions(true, j, startX, endX, screenLayers, changeFlags, rightwardOcclusions, activeStates);
				this.calculateOcclusions(false, j, startX, endX, screenLayers, changeFlags, leftwardOcclusions, activeStates);

				for(int i = startX; i < endX; i++){
					String outputCharacters = null;
					int outputCharacterWidths = 0;
					int [] outputColourCodes = this.colourCodes[i][j];
					//  Colour codes come from top most coloured character:
					int [] colours = new int []{};
					for(int s = screenLayers.length -1; s >= 0; s--){
						if(screenLayers[s].colourCodes[i][j].length > 0 && activeStates[s][i-startX]){
							colours = screenLayers[s].colourCodes[i][j];
							break;
						}
					}

					if(!Arrays.equals(this.colourCodes[i][j], colours)){
						outputColourCodes = colours;
					}

					int rightward = rightwardOcclusions[i-startX];
					int leftward = leftwardOcclusions[i-startX];
					if(rightward >=0 || leftward >= 0){
						if(rightward == leftward){
							outputCharacters = screenLayers[rightward].characters[i][j];
							outputCharacterWidths = screenLayers[rightward].characterWidths[i][j];
						}else if(rightward >= 0){
							outputCharacters = " ";
							outputCharacterWidths = 1;
						}else if(leftward >= 0){
							outputCharacters = " ";
							outputCharacterWidths = 1;
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
					boolean hasChange = trustChangedFlags ? changeFlags[i-startX] : (
						!(
							(this.characterWidths[i][j] == outputCharacterWidths) &&
							Arrays.equals(this.colourCodes[i][j], outputColourCodes) &&
							Objects.equals(this.characters[i][j], outputCharacters)
						) || this.changed[i][j] // if there is a pending changed flag that hasn't been printed yet.
					);

					this.characters[i][j] = outputCharacters;
					this.characterWidths[i][j] = outputCharacterWidths;
					this.colourCodes[i][j] = outputColourCodes;
					this.changed[i][j] = hasChange;
					this.active[i][j] = finalActiveStates[i-startX];
				}
			}
		}
		this.addChangedRegions(aboveRegions);
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

	public void mergeChanges(ScreenLayer changes) throws Exception{
		Set<ScreenRegion> regions = changes.getChangedRegions();
		int xOffset = changes.getPlacementOffset().getX().intValue();
		int yOffset = changes.getPlacementOffset().getY().intValue();
		for(ScreenRegion sourceRegion : regions){
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
					}
					i += (newCharacterWidth < 1) ? 1 : newCharacterWidth; 
					this.active[x][y] = true;
				}
			}

			if(region.getRegion().getVolume() > 0){
				this.addChangedRegion(region);
			}
		}
		this.setIsLayerActive(changes.getIsLayerActive());
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
				String characters = " ";
				boolean isInChangedRegion = ScreenLayer.isInChangedRegion(i, j, this.changedRegions);
				if(debugType.equals("characters")){
					if(this.characters[i][j] != null){
						if(this.colourCodes[i][j] != null){
							colourCodes = this.colourCodes[i][j];
						}else{
							colourCodes = new int []{UserInterfaceFrameThreadState.RED_BG_COLOR};
						}
						characters = this.characters[i][j];
					}
				}else if(debugType.equals("active")){
					if(this.active[i][j] && this.getIsLayerActive()){
						colourCodes = new int []{UserInterfaceFrameThreadState.GREEN_BG_COLOR};
					}else if(this.active[i][j] && !this.getIsLayerActive()){
						colourCodes = new int []{UserInterfaceFrameThreadState.YELLOW_BG_COLOR};
					}else if(!this.active[i][j] && this.getIsLayerActive()){
						colourCodes = new int []{UserInterfaceFrameThreadState.BLACK_BG_COLOR};
					}else if(!this.active[i][j] && !this.getIsLayerActive()){
						colourCodes = new int []{UserInterfaceFrameThreadState.RED_BG_COLOR};
					}
				}else if(debugType.equals("changed")){
					if(this.changed[i][j]){
						colourCodes = new int []{UserInterfaceFrameThreadState.RED_BG_COLOR};
					}else{
						colourCodes = new int []{UserInterfaceFrameThreadState.BLACK_BG_COLOR};
					}
				}else if(debugType.equals("in_changed_region")){
					if(isInChangedRegion){
						colourCodes = new int []{UserInterfaceFrameThreadState.GREEN_BG_COLOR};
					}else{
						colourCodes = new int []{UserInterfaceFrameThreadState.BLACK_BG_COLOR};
					}
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
