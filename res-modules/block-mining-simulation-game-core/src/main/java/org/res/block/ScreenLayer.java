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

	public void clearFlags(){
		int width = this.getWidth();
		int height = this.getHeight();
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				this.flags[i][j] = false;
			}
		}
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
}
