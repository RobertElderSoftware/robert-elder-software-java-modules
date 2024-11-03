//  Copyright (c) 2024 Robert Elder Software Inc.
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
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Map;
import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class UserInterfaceSplitVertical extends UserInterfaceSplit {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public UserInterfaceSplitVertical(List<UserInterfaceSplit> splitParts) throws Exception {
		this.splitParts = splitParts;
	}

	public List<UserInterfaceFrameThreadState> collectUserInterfaceFrames() throws Exception{
		List<UserInterfaceFrameThreadState> rtn = new ArrayList<UserInterfaceFrameThreadState>();
		for(UserInterfaceSplit split : this.splitParts){
			rtn.addAll(split.collectUserInterfaceFrames());
		}
		return rtn;
	}

	public List<FrameDimensions> getOrderedSubframeDimensions(FrameDimensions frameDimensions) throws Exception{
		List<FrameDimensions> sumFrameDimensions = new ArrayList<FrameDimensions>();
		if(this.splitParts.size() > 0){
			Long numSplits = (long)this.splitParts.size();
			Long xOffsetSoFar = frameDimensions.getFrameOffsetX();
			Long fcw = frameDimensions.getFrameCharacterWidth();
			//  For wide character mode, must make sure that vertical splitParts have an even number of columns:
			Long usedFrameArea = fcw.equals(1L) ? frameDimensions.getFrameWidth() : ((frameDimensions.getFrameWidth() / numSplits) / fcw) * numSplits * fcw;
			for(int i = 0; i < this.splitParts.size(); i++){
				Long currentFrameWidth = i == (this.splitParts.size() -1) ? (frameDimensions.getFrameWidth() - xOffsetSoFar) : (usedFrameArea / numSplits);
				FrameDimensions subFrameDimensions = new FrameDimensions(
					frameDimensions.getFrameCharacterWidth(),
					currentFrameWidth,
					frameDimensions.getFrameHeight(),
					xOffsetSoFar,
					frameDimensions.getFrameOffsetY(),
					frameDimensions.getTerminalWidth(),
					frameDimensions.getTerminalHeight()
				);
				sumFrameDimensions.add(subFrameDimensions);
				xOffsetSoFar += usedFrameArea / numSplits;
			}
		}
		return sumFrameDimensions;
	}

	public void setEquidistantFrameDimensions(FrameDimensions frameDimensions, FrameBordersDescription frameBordersDescription) throws Exception{
		List<FrameDimensions> subFrameDimensions = this.getOrderedSubframeDimensions(frameDimensions);
		for(int i = 0; i < this.splitParts.size(); i++){
			this.splitParts.get(i).setEquidistantFrameDimensions(subFrameDimensions.get(i), frameBordersDescription);
		}
	}

	public FrameBordersDescription collectAllConnectionPoints(FrameDimensions frameDimensions) throws Exception{
		Set<Coordinate> framePoints = new TreeSet<Coordinate>();
		List<FrameDimensions> sumFrameDimensions = this.getOrderedSubframeDimensions(frameDimensions);
		for(int i = 0; i < this.splitParts.size(); i++){
			FrameBordersDescription frameBordersDescription = this.splitParts.get(i).collectAllConnectionPoints(sumFrameDimensions.get(i));
			for(Coordinate c : frameBordersDescription.getFramePoints()){
				framePoints.add(c);
			}
		}
		return new FrameBordersDescription(framePoints);
	}
}