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

public abstract class UserInterfaceSplitMulti extends UserInterfaceSplit {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected List<Double> splitPercentages = new ArrayList<Double>();

	public UserInterfaceSplitMulti() throws Exception {
	}

	public void addPart(UserInterfaceSplit part) throws Exception {
		Double sizeBefore = Double.valueOf(this.splitPercentages.size());
		Double sizeAfter = sizeBefore + 1.0;
		Double total = 0.0;
		for(int i = 0; i < this.splitPercentages.size(); i++){
			Double scaledDownPercent = this.splitPercentages.get(i) * (sizeBefore / sizeAfter);
			total += scaledDownPercent;
			this.splitPercentages.set(i, scaledDownPercent);
		}
		Double newPercent = 1.0 - total;
		if(newPercent < 0.0){
			throw new Exception("Impossible?");
		}
		this.splitPercentages.add(newPercent);
		this.splitParts.add(part);
	}

	public void removeSplitAtIndex(int i)throws Exception{
		this.splitParts.remove(i);
		Double extra = this.splitPercentages.get(i);
		this.splitPercentages.remove(i);
		//  Distribute removed percentage to other splits:
		if(this.splitPercentages.size() > 0){
			Double toDistribute = extra / this.splitPercentages.size();
			for(int j = 0; j < this.splitPercentages.size(); j++){
				this.splitPercentages.set(j, this.splitPercentages.get(j) + toDistribute);
			}
		}
	}

	public void addParts(List<UserInterfaceSplit> parts) throws Exception {
		for(UserInterfaceSplit part : parts){
			this.splitParts.add(part);
		}
	}

	public void setSplitPercentages(List<Double> splitPercentages) throws Exception {
		if(splitPercentages.size() == this.splitParts.size()){
			this.splitPercentages = splitPercentages;
		}else{
			throw new Exception("splitPercentages.size() != this.splitParts.size()");
		}
	}

	public List<UserInterfaceFrameThreadState> collectUserInterfaceFrames() throws Exception{
		List<UserInterfaceFrameThreadState> rtn = new ArrayList<UserInterfaceFrameThreadState>();
		for(UserInterfaceSplit split : this.splitParts){
			rtn.addAll(split.collectUserInterfaceFrames());
		}
		return rtn;
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
