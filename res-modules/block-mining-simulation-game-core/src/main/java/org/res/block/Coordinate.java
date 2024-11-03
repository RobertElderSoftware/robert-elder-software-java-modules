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

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Random;
import java.lang.Comparable;

public class Coordinate extends Vector{

	public Coordinate(Vector v) {
		super(v);
	}

	public Coordinate(List<Long> coordinateValues) {
		super(coordinateValues);
	}

	public Long getX() throws Exception {
		return this.getValueAtIndex(0L);
	}

	public Long getY() throws Exception {
		return this.getValueAtIndex(1L);
	}

	public Long getZ() throws Exception {
		return this.getValueAtIndex(2L);
	}

	public Coordinate changeX(Long v) throws Exception {
		return new Coordinate(super.changeValueAtIndex(0L, v));
	}

	public Coordinate changeY(Long v) throws Exception {
		return new Coordinate(super.changeValueAtIndex(1L, v));
	}

	public Coordinate changeZ(Long v) throws Exception {
		return new Coordinate(super.changeValueAtIndex(2L, v));
	}

	public Coordinate changeByDeltaXYZ(Long deltaX, Long deltaY, Long deltaZ) throws Exception {
		Coordinate c = this;
		c = c.changeX(c.getX() + deltaX);
		c = c.changeY(c.getY() + deltaY);
		c = c.changeZ(c.getZ() + deltaZ);
		return c;
	}

	public Coordinate changeByDeltaXY(Long deltaX, Long deltaY) throws Exception {
		Coordinate c = this;
		c = c.changeX(c.getX() + deltaX);
		c = c.changeY(c.getY() + deltaY);
		return c;
	}

	public Coordinate subtract(Coordinate other) throws Exception {
		return new Coordinate(super.subtract(other));
	}

	public Coordinate changeValueAtIndex(Long i, Long v) throws Exception {
		return new Coordinate(super.changeValueAtIndex(i, v));
	}

	@Override
	public boolean equals(Object o){
		return super.equals(o);
	}

	public Coordinate copy() {
		return new Coordinate(super.copy());
	}

	public boolean isLowerThanOrEqualTo(Coordinate c){
		for(long i = 0L; i < c.getNumDimensions(); i++){
			if(this.getValueAtIndex(i) <= c.getValueAtIndex(i)){
			}else{
				return false;
			}
		}
		return true;
	}

	public boolean isHigherThanOrEqualTo(Coordinate c){
		for(long i = 0L; i < c.getNumDimensions(); i++){
			if(this.getValueAtIndex(i) >= c.getValueAtIndex(i)){
			}else{
				return false;
			}
		}
		return true;
	}

	public static Coordinate makeOriginCoordinate(Long numDimensions){
		List<Long> values = new ArrayList<Long>();
		for(long l = 0L; l < numDimensions; l++){
			values.add(0L);
		}
		return new Coordinate(values);
	}

	public static Coordinate readCoordinate(BlockMessageBinaryBuffer buffer, long numDimensions){
		List<Long> rtn = new ArrayList<Long>();
		long [] values = buffer.readNLongValues((int)numDimensions);
		for(long l : values){
			rtn.add(l);
		}
		return new Coordinate(rtn);
	}

	public static void writeCoordinate(BlockMessageBinaryBuffer buffer, Coordinate c){
		long [] values = new long [c.getNumDimensions().intValue()];
		for(long i = 0L; i < c.getNumDimensions(); i++){
			values[(int)i] = c.getValueAtIndex(i);
		}

		buffer.writeLongValues(values);
	}

	public static Coordinate getRandomCoordinate(Random rand, Long numDimensions, int minCoordinateValue, int maxCoordinateValue) throws Exception {
		List<Long> values = new ArrayList<Long>();
		for(long i = 0; i < numDimensions; i++){
			values.add((long)(rand.nextInt(maxCoordinateValue - minCoordinateValue) + minCoordinateValue));
		}
		return new Coordinate(values);
	}
}
