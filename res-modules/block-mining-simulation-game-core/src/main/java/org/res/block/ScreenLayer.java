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

	private boolean isActive = true;
	private CuboidAddress dimensions;
	public int [][] characterWidths = null;
	public int [][][] colourCodes = null;
	public String [][] characters = null;
	public boolean [][] flags = null;
	private int [] defaultColourCodes = new int [] {};
	private Set<ScreenRegion> changedRegions = new HashSet<ScreenRegion>();
	private final StringBuilder stringBuilder = new StringBuilder();

	public boolean getIsActive(){
		return this.isActive;
	}

	public static CuboidAddress makeDimensionsCA(int startX, int startY, int endX, int endY) throws Exception{
		return new CuboidAddress(
			new Coordinate(Arrays.asList((long)startX, (long)startY)),
			new Coordinate(Arrays.asList((long)endX, (long)endY))
		);
	}

	public void setIsActive(boolean isActive) throws Exception{
		if(this.isActive != isActive){
			this.isActive = isActive;
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

	public ScreenLayer(int width, int height) throws Exception{
		this.characterWidths = new int [width][height];
		this.colourCodes = new int [width][height][];
		this.characters = new String [width][height];
		this.flags = new boolean [width][height];
		this.dimensions = ScreenLayer.makeDimensionsCA(0, 0, width, height);
	}

	public void setAllFlagStates(boolean state){
		int width = this.getWidth();
		int height = this.getHeight();
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.flags[i][j] = state;
			}
		}
	}

	public void clearFlags(){
		this.setAllFlagStates(false);
	}

	public ScreenLayer(ScreenLayer l){
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
		this.flags = new boolean [width][height];
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.flags[i][j] = l.flags[i][j];
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
				this.flags[i][j] = defaultMaskState;
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

	public void mergeNonNullChangesDownOnto(ScreenLayer [] screenLayers) throws Exception{
		Set<ScreenRegion> allRegions = new HashSet<ScreenRegion>();
		allRegions.addAll(this.getChangedRegions());
		for(int l = 0; l < ConsoleWriterThreadState.numScreenLayers; l++){
			allRegions.addAll(screenLayers[l].getChangedRegions());
			screenLayers[l].clearChangedRegions();
		}
		for(ScreenRegion region : allRegions){
			int startX = region.getStartX();
			int startY = region.getStartY();
			int endX = region.getEndX();
			int endY = region.getEndY();
			for(int j = startY; j < endY; j++){
				int i = startX;
				while(i < endX){
					int increment = 1;
					for(int l = ConsoleWriterThreadState.numScreenLayers -1; l >= 0; l--){
						ScreenLayer sl = screenLayers[l];
						if(!sl.getIsActive() || sl.characters[i][j] == null){

							//Continue down to next layer undeaneath
							if(sl.flags[i][j]){ // If null/non-active character above has changed, need to keep changed signal to actually perform update to whatever is below:
								this.flags[i][j] = true;
							}

							sl.flags[i][j] = false;
						}else{
							this.characterWidths[i][j] = sl.characterWidths[i][j];
							this.colourCodes[i][j] = sl.colourCodes[i][j];
							this.characters[i][j] = sl.characters[i][j];
							this.flags[i][j] = sl.flags[i][j] || this.flags[i][j];
							sl.flags[i][j] = false;

							for(int k = 1; (k < sl.characterWidths[i][j]) && (k+i) < endX; k++){
								this.characterWidths[i+k][j] = 0;
								this.colourCodes[i+k][j] = this.colourCodes[i][j];
								this.characters[i+k][j] = null;
								this.flags[i+k][j] = this.flags[i][j];
								sl.flags[i+k][j] = false;
							}
							increment = (this.characterWidths[i][j] < 1) ? 1 : this.characterWidths[i][j]; 
							//  Solid character found, break out of loop
							break;
						}
					}
					i += increment;
				}
			}
		}
		this.addChangedRegions(allRegions);
	}

	public void mergeChanges(ScreenLayer changes, Long mergeOffsetX, Long mergeOffsetY) throws Exception{
		int mergeOffsetXInt = mergeOffsetX.intValue();
		int mergeOffsetYInt = mergeOffsetY.intValue();
		Set<ScreenRegion> regions = changes.getChangedRegions();
		for(ScreenRegion sourceRegion : regions){
			//  Determine the subset of the change that's actually lands within the destination layer
			CuboidAddress destinationRegionCA = ScreenRegion.makeScreenRegionCA(
				sourceRegion.getStartX() + mergeOffsetXInt,
				sourceRegion.getStartY() + mergeOffsetYInt,
				sourceRegion.getEndX() + mergeOffsetXInt,
				sourceRegion.getEndY() + mergeOffsetYInt
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
					int xF = i - mergeOffsetXInt;
					int yF = j - mergeOffsetYInt;

					if(changes.flags[xF][yF]){
						boolean hasChanged = !(
							(this.characterWidths[x][y] == changes.characterWidths[xF][yF]) &&
							Arrays.equals(this.colourCodes[x][y], changes.colourCodes[xF][yF]) &&
							Objects.equals(this.characters[x][y], changes.characters[xF][yF])
						) || this.flags[x][y]; //  In case multiple writes happened since last commit
						this.flags[x][y] = hasChanged;
						this.characterWidths[x][y] = changes.characterWidths[xF][yF];
						this.colourCodes[x][y] = changes.colourCodes[xF][yF];
						this.characters[x][y] = changes.characters[xF][yF];
						
						//  For multi-column characters, explicitly initialize any 'covered' characters as null to resolve printing glitches:
						for(int k = 1; (k < changes.characterWidths[xF][yF]) && (k+x) < endX; k++){
							this.characterWidths[x+k][y] = 0;
							this.colourCodes[x+k][y] = this.colourCodes[x][y];
							this.characters[x+k][y] = null;
							this.flags[x+k][y] = this.flags[x][y];
						}
					}
					i += (changes.characterWidths[xF][yF] < 1) ? 1 : changes.characterWidths[xF][yF]; 
				}
			}

			if(region.getRegion().getVolume() > 0){
				this.addChangedRegion(region);
			}
		}
		this.setIsActive(changes.getIsActive());
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
						this.flags[i][j]
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
						this.flags[i][j] = false;
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
}
