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
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashSet;
import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class UserInterfaceSplitLeafNode extends UserInterfaceSplit {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private UserInterfaceFrameThreadState userInterfaceFrameThreadState;

	public UserInterfaceSplitLeafNode(UserInterfaceFrameThreadState userInterfaceFrameThreadState) throws Exception {
		this.userInterfaceFrameThreadState = userInterfaceFrameThreadState;
	}

	public void removeSplitAtIndex(int i)throws Exception{
		this.splitParts.remove(i);
	}

	public UserInterfaceFrameThreadState getUserInterfaceFrameThreadState(){
		return this.userInterfaceFrameThreadState;
	}

	public List<UserInterfaceFrameThreadState> collectUserInterfaceFrames() throws Exception{
		List<UserInterfaceFrameThreadState> rtn = new ArrayList<UserInterfaceFrameThreadState>();
		rtn.add(this.userInterfaceFrameThreadState);
		return rtn;
	}

	public void sendFrameChanceNotifies() throws Exception{
		this.userInterfaceFrameThreadState.putWorkItem(new FrameChangeWorkItem(this.userInterfaceFrameThreadState), WorkItemPriority.PRIORITY_LOW);
	}

	public Map<Long, FrameDimensions> collectFrameDimensions(FrameDimensions frameDimensions) throws Exception{
		Map<Long, FrameDimensions> rtn = new HashMap<Long, FrameDimensions>();
		rtn.put(this.userInterfaceFrameThreadState.getFrameId(), frameDimensions);
		return rtn;
	}

	public List<FrameDimensions> getOrderedSubframeDimensions(FrameDimensions frameDimensions) throws Exception{
		List<FrameDimensions> sumFrameDimensions = new ArrayList<FrameDimensions>();
		return sumFrameDimensions;
	}

	public FrameBordersDescription collectAllConnectionPoints(FrameDimensions fd) throws Exception{
		Set<Coordinate> connectionPoints = new TreeSet<Coordinate>();
		Long fcw = fd.getFrameCharacterWidth();

		//  End of the frame that connect to whatever is on the right:
		//  Frames that are on the right edge of the terminal are responsible for drawing the right border,
		//  and frames that are on the bottom edge must draw their own lower border:
		boolean hasRightTerminalBorder = (fd.getFrameOffsetX() + fd.getFrameWidth() + fd.getFrameCharacterWidth()) >= fd.getTerminalWidth();
		boolean hasBottomTerminalBorder = fd.getTerminalHeight().equals(fd.getFrameOffsetY() + fd.getFrameHeight());
		logger.info("getTerminalWidth()=" + fd.getTerminalWidth() + " fd.getFrameOffsetX()=" + fd.getFrameOffsetX() + " fd.getFrameWidth()=" + fd.getFrameWidth() + " hasBottomTerminalBorder=" + hasRightTerminalBorder);
		Long fid = this.userInterfaceFrameThreadState.getFrameId();
		if(hasRightTerminalBorder && hasBottomTerminalBorder){
			//  Top border
			for(long l = fd.getFrameOffsetX(); l < fd.getFrameOffsetX() + fd.getFrameWidth(); l += fcw){
				connectionPoints.add(new Coordinate(Arrays.asList(l, fd.getFrameOffsetY())));
			}

			//  Bottom border
			for(long l = fd.getFrameOffsetX(); l < fd.getFrameOffsetX() + fd.getFrameWidth(); l += fcw){
				connectionPoints.add(new Coordinate(Arrays.asList(l, fd.getFrameOffsetY() + fd.getFrameHeight() -1L)));
			}

			//  Left border
			for(long l = fd.getFrameOffsetY(); l < fd.getFrameOffsetY() + fd.getFrameHeight() -1L; l++){
				connectionPoints.add(new Coordinate(Arrays.asList(fd.getFrameOffsetX(), l)));
			}

			//  Right border
			for(long l = fd.getFrameOffsetY(); l < fd.getFrameOffsetY() + fd.getFrameHeight() -1L; l++){
				connectionPoints.add(new Coordinate(Arrays.asList(fd.getFrameOffsetX() + fd.getFrameWidth() -fcw, l)));
			}
		}else if(hasRightTerminalBorder && !hasBottomTerminalBorder){
			//  Top border
			for(long l = fd.getFrameOffsetX(); l < fd.getFrameOffsetX() + fd.getFrameWidth(); l += fcw){
				connectionPoints.add(new Coordinate(Arrays.asList(l, fd.getFrameOffsetY())));
			}

			//  Left border
			for(long l = fd.getFrameOffsetY(); l < fd.getFrameOffsetY() + fd.getFrameHeight(); l++){
				connectionPoints.add(new Coordinate(Arrays.asList(fd.getFrameOffsetX(), l)));
			}

			//  Right border
			for(long l = fd.getFrameOffsetY(); l < fd.getFrameOffsetY() + fd.getFrameHeight(); l++){
				connectionPoints.add(new Coordinate(Arrays.asList(fd.getFrameOffsetX() + fd.getFrameWidth() -fcw, l)));
			}
		}else if(!hasRightTerminalBorder && hasBottomTerminalBorder){
			//  Top border
			for(long l = fd.getFrameOffsetX(); l < fd.getFrameOffsetX() + fd.getFrameWidth(); l += fcw){
				connectionPoints.add(new Coordinate(Arrays.asList(l, fd.getFrameOffsetY())));
			}

			//  Bottom border
			for(long l = fd.getFrameOffsetX(); l < fd.getFrameOffsetX() + fd.getFrameWidth(); l += fcw){
				connectionPoints.add(new Coordinate(Arrays.asList(l, fd.getFrameOffsetY() + fd.getFrameHeight() -1L)));
			}

			//  Left border
			for(long l = fd.getFrameOffsetY(); l < fd.getFrameOffsetY() + fd.getFrameHeight() -1L; l++){
				connectionPoints.add(new Coordinate(Arrays.asList(fd.getFrameOffsetX(), l)));
			}
		}else if(!hasRightTerminalBorder && !hasBottomTerminalBorder){
			//  Top border
			for(long l = fd.getFrameOffsetX(); l < fd.getFrameOffsetX() + fd.getFrameWidth(); l += fcw){
				connectionPoints.add(new Coordinate(Arrays.asList(l, fd.getFrameOffsetY())));
			}

			//  Left border
			for(long l = fd.getFrameOffsetY(); l < fd.getFrameOffsetY() + fd.getFrameHeight(); l++){
				connectionPoints.add(new Coordinate(Arrays.asList(fd.getFrameOffsetX(), l)));
			}
		}else{
			throw new Exception("Impossible");
		}
		return new FrameBordersDescription(connectionPoints);
	}
}
