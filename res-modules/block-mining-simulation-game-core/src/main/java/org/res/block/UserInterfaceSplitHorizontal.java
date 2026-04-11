//  Copyright (c) 2026 Robert Elder Software Inc.
//   
//  Robert Elder Software Proprietary License (Version 2026-04-09)
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
//  either unmodified, modified, or incorporated into another software product, 
//  except as described in the document "REDISTRIBUTION.md" (a file with SHA256 
//  hash value 'c39a6c8200a22caf30eac97095b78def80c9cab1b6f7ddd3fca7fdae71df43da').
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

import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.Date;
import java.util.TreeSet;
import java.util.Set;
import java.util.HashSet;
import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.util.TreeMap;
import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class UserInterfaceSplitHorizontal extends UserInterfaceSplitMulti {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public UserInterfaceSplitHorizontal() throws Exception {
	}

	public List<FrameDimensions> getOrderedSubframeDimensions(FrameDimensions frameDimensions) throws Exception{
		List<FrameDimensions> sumFrameDimensions = new ArrayList<FrameDimensions>();
		if(this.splitParts.size() > 0){
			Long numSplits = (long)this.splitParts.size();
			Long yOffsetSoFar = 0L;
			Long fcw = 1L;  //  TODO: this is wrong!
			//  In wide character mode, must be two column aligned:
			Long frameWidth = frameDimensions == null ? 0L : frameDimensions.getFrameWidth();
			Long frameHeight = frameDimensions == null ? 0L : frameDimensions.getFrameHeight();
			Long frameOffsetX = frameDimensions == null ? 0L : frameDimensions.getFrameOffsetX();
			Long frameOffsetY = frameDimensions == null ? 0L : frameDimensions.getFrameOffsetY();
			Long allowableFrameWidth = fcw.equals(1L) ? frameWidth : (frameWidth / fcw) * fcw;
			for(int i = 0; i < this.splitParts.size(); i++){
				Long defaultFrameWidth = this.smarterRound(Double.valueOf(frameHeight) * this.splitPercentages.get(i), frameHeight);
				Long currentFrameHeight = (i == (this.splitParts.size() -1)) ? (frameHeight - yOffsetSoFar) : defaultFrameWidth;

				if(currentFrameHeight < 0){
					throw new Exception("currentFrameHeight is negative: " + currentFrameHeight + " for i=" + i + " this.splitParts.size()=" + this.splitParts.size());

				}

				Long x1 = frameOffsetX;
				Long y1 = yOffsetSoFar + frameOffsetY;
				Long x2 = x1 + allowableFrameWidth;
				Long y2 = y1 + currentFrameHeight;

				FrameDimensions subFrameDimensions = new FrameDimensions(
					new CuboidAddress(
						new Coordinate(Arrays.asList(x1, y1)),
						new Coordinate(Arrays.asList(x2, y2))
					),
					frameDimensions == null ? FrameDimensions.makeDefaultDimensions() : frameDimensions.getTerminal()
				);
				sumFrameDimensions.add(subFrameDimensions);
				yOffsetSoFar += currentFrameHeight;
			}
		}
		return sumFrameDimensions;
	}
}
