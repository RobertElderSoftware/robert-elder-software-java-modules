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
import java.util.Arrays;
import java.util.ArrayList;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class RegionIteration {

	private Coordinate coordinate;
	private CuboidAddress cuboidAddress;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private boolean isOverflow = false;

	public RegionIteration(Coordinate coordinate, CuboidAddress cuboidAddress) throws Exception{
		this.coordinate = coordinate;
		this.cuboidAddress = cuboidAddress;
		//  If this iteration is initialized with a zero sized region, overflow immediately:
		this.isOverflow = !cuboidAddress.containsCoordinate(coordinate);
	}

	public boolean isDone(){
		return this.isOverflow;
	}

	public Coordinate getCurrentCoordinate() throws Exception {
		if(this.isOverflow){
			throw new Exception("This region iteration is in overflow state at coordinate " + coordinate + " for cuboid address " + this.cuboidAddress + ".");
		}else{
			return this.coordinate;
		}
	}

	public boolean incrementInRange(Coordinate lower, Coordinate upper) throws Exception {
		/*
		Increment a coordinate in such a way that it's constrained into a region.
		So incrementing the coordinate 9,0 inside the region 0,0, to 10,10 would 
		go to 9, 1 then 9, 2 ... 9,10 then 10,0 then 10,1 ...

		Returns true when value was incremented, returns false on overflow condition.
		*/
		Long i = 0L;
		this.isOverflow = false;
		while(i < this.coordinate.getNumDimensions()){
			/*  Try to increment the coordinate in the lowest position. */
			if(this.coordinate.getValueAtIndex(i) >= (upper.getValueAtIndex(i) -1L)){
				this.coordinate = coordinate.changeValueAtIndex(i, lower.getValueAtIndex(i));
				if(this.coordinate.getNumDimensions().equals(i + 1L)){ /* Overflow condition. */
					//logger.info("Reached overflow condition: " + this.toString());
					this.isOverflow = true;
					break;
				}
			}else{
				this.coordinate = coordinate.changeValueAtIndex(i, this.coordinate.getValueAtIndex(i) + 1L);
				break;
			}
			i++;
		}

		/*  Catch any case where the coordinate ends up outside the cuboid for some reason: */
		if(this.coordinate.getValueAtIndex(i) < lower.getValueAtIndex(i)){
			throw new Exception("Coordinate value " + this.coordinate.getValueAtIndex(i) + " outside lower bound of " + lower.getValueAtIndex(i));
		}
		if(this.coordinate.getValueAtIndex(i) >= upper.getValueAtIndex(i)){
			throw new Exception("Coordinate value " + this.coordinate.getValueAtIndex(i) + " outside upper bound of " + upper.getValueAtIndex(i));
		}

		return !this.isOverflow;
	}

	public boolean incrementCoordinateWithinCuboidAddress() throws Exception{
		if(this.coordinate.getNumDimensions().equals(this.cuboidAddress.getNumDimensions())){
			return this.incrementInRange(this.cuboidAddress.getCanonicalLowerCoordinate(), this.cuboidAddress.getCanonicalUpperCoordinate());
		}else{
			throw new Exception("Cannot increment coordinate.  Coordinate dimension size was " + this.coordinate.getNumDimensions() + " while cuboid dimension size was " + this.cuboidAddress.getNumDimensions());
		}
	}
}
