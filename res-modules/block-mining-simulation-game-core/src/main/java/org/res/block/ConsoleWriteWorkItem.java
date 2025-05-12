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

import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class ConsoleWriteWorkItem extends ConsoleQueueableWorkItem {

	private String text;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private int [][] characterWidths;
	private int [][][] colourCodes;
	private String [][] characters;
	private boolean [][] hasChange;
	private int xOffset;
	private int yOffset;
	private int xSize;
	private int ySize;
	private FrameDimensions frameDimensions;
	private int bufferIndex;

	public ConsoleWriteWorkItem(ConsoleWriterThreadState consoleWriterThreadState, int [][] characterWidths, int [][][] colourCodes, String [][] characters, boolean [][] hasChange, int xOffset, int yOffset, int xSize, int ySize, FrameDimensions frameDimensions, int bufferIndex){
		super(consoleWriterThreadState, false);
		this.characterWidths = characterWidths;
		this.colourCodes = colourCodes;
		this.characters = characters;
		this.hasChange = hasChange;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.xSize = xSize;
		this.ySize = ySize;
		this.frameDimensions = frameDimensions;
		this.bufferIndex = bufferIndex;
	}

	public int [][] getCharacterWidths(){
		return characterWidths;
	}

	public int [][][] getColourCodes(){
		return colourCodes;
	}

	public String [][] getCharacters(){
		return characters;
	}

	public int getXOffset(){
		return xOffset;
	}

	public int getYOffset(){
		return yOffset;
	}

	public int getXSize(){
		return xSize;
	}

	public int getYSize(){
		return ySize;
	}

	public WorkItemResult executeQueuedWork() throws Exception{
		this.consoleWriterThreadState.prepareTerminalTextChange(characterWidths, colourCodes, characters, hasChange, xOffset, yOffset, xSize, ySize, frameDimensions, bufferIndex);
		return null;
	}

	public void doWork() throws Exception{
		this.consoleWriterThreadState.addPendingQueueableWorkItem(this);
	}
}
