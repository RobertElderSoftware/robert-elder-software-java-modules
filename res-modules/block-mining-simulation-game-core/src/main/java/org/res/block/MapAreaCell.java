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
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import java.io.IOException;

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

public class MapAreaCell {

	private Set<MapAreaCellFlag> mapAreaCellFlags = new TreeSet<MapAreaCellFlag>();
	private IndividualBlock currentBlock = null;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public MapAreaCell() throws Exception {

	}

	public boolean equals(MapAreaCell other) throws Exception{
		if(this.getCurrentBlock() == null){
			return other.getCurrentBlock() == null && this.mapAreaCellFlags.equals(other.getMapAreaCellFlags());
		}else{
			return this.getCurrentBlock().equals(other.getCurrentBlock()) && this.mapAreaCellFlags.equals(other.getMapAreaCellFlags());
		}
	}

	public void addMapAreaCellFlag(MapAreaCellFlag flag){
		this.mapAreaCellFlags.add(flag);
	}

	public Set<MapAreaCellFlag> getMapAreaCellFlags(){
		return this.mapAreaCellFlags;
	}

	public String renderBlockCell(boolean isUseASCII) throws Exception {
		//  null = block does not exist yet.
		String presentedText = this.currentBlock == null ? "" : BlockSkins.getPresentation(this.currentBlock.getClass(), isUseASCII);
		return presentedText;
	}

	public IndividualBlock getCurrentBlock() {
		return this.currentBlock;
	}

	public void setCurrentBlock(IndividualBlock b) throws Exception {
		if(this.currentBlock == null){
			if(b == null){
				// No change
			}else{
				this.addMapAreaCellFlag(MapAreaCellFlag.BLOCK_CHANGE);
			}
		}else{
			if(b == null){
				//  Last time it was set, but now it's not:
				this.addMapAreaCellFlag(MapAreaCellFlag.BLOCK_CHANGE);
			}else{
				if(Arrays.equals(this.currentBlock.getBlockData(), b.getBlockData())){
					// No change
				}else{
					this.addMapAreaCellFlag(MapAreaCellFlag.BLOCK_CHANGE);
				}
			}
		}
		this.currentBlock = b;
	}

	public boolean hasBlockChangedFlags(){
		return this.mapAreaCellFlags.contains(MapAreaCellFlag.BLOCK_CHANGE);
	}

	public boolean hasPlayerMovementFlags(){
		return this.mapAreaCellFlags.contains(MapAreaCellFlag.PLAYER_MOVEMENT);
	}

	public boolean hasPendingLoadFlags(){
		return this.mapAreaCellFlags.contains(MapAreaCellFlag.PENDING_LOAD);
	}

	public void clearNonLoadingFlags(){
		/*  Remove all flags, except for the pending load flag: */
		this.mapAreaCellFlags.remove(MapAreaCellFlag.PLAYER_MOVEMENT);
		this.mapAreaCellFlags.remove(MapAreaCellFlag.BLOCK_CHANGE);
	}

	public void clearPendingLoadFlags(){
		this.mapAreaCellFlags.remove(MapAreaCellFlag.PENDING_LOAD);
	}
}
