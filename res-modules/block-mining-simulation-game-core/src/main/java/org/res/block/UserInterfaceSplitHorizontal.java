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
			Long fcw = frameDimensions.getFrameCharacterWidth();
			//  In wide character mode, must be two column aligned:
			Long allowableFrameWidth = fcw.equals(1L) ? frameDimensions.getFrameWidth() : (frameDimensions.getFrameWidth() / fcw) * fcw;
			for(int i = 0; i < this.splitParts.size(); i++){
				Long currentFrameHeight = (i == (this.splitParts.size() -1)) ? (frameDimensions.getFrameHeight() - yOffsetSoFar) : Math.round(Double.valueOf(frameDimensions.getFrameHeight()) * this.splitPercentages.get(i));

				if(currentFrameHeight < 0){
					throw new Exception("currentFrameHeight is negative: " + currentFrameHeight);

				}
				FrameDimensions subFrameDimensions = new FrameDimensions(
					frameDimensions.getFrameCharacterWidth(),
					allowableFrameWidth,
					currentFrameHeight,
					frameDimensions.getFrameOffsetX(),
					yOffsetSoFar + frameDimensions.getFrameOffsetY(),
					frameDimensions.getTerminalWidth(),
					frameDimensions.getTerminalHeight()
				);
				sumFrameDimensions.add(subFrameDimensions);
				yOffsetSoFar += currentFrameHeight;
			}
		}
		return sumFrameDimensions;
	}
}
