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
import java.util.stream.Collectors;
import java.lang.Comparable;

public class Vector implements Comparable<Vector>{

	protected final List<Long> coordinateValues;
	protected final Long numDimensions;

	private final int hashCode;

	public final int getHashCode(){
		/*  Initialize hash code once so it doesn't have to be calculated again: */
		int total  = this.getNumDimensions().intValue() + 1;
		for(long i = 0L; i < this.getNumDimensions(); i++){
			total *= (this.getValueAtIndex(i).intValue() + 1);
		}
		return total;
	}

	@Override
	public int compareTo(Vector other) {
		if(this.getNumDimensions() < other.getNumDimensions()){
			return -1;
		}else if(this.getNumDimensions() > other.getNumDimensions()){
			return 1;
		}else{
			for(long i = 0L; i < this.getNumDimensions(); i++){
				if(this.getValueAtIndex(i) < other.getValueAtIndex(i)){
					return -1;
				}else if(this.getValueAtIndex(i) > other.getValueAtIndex(i)){
					return 1;
				}else{
					//  Continue.
				}
			}
			return 0;
		}
	}

	@Override
	public final int hashCode(){
		return this.hashCode;
	}

	@Override
	public boolean equals(Object o){
		Vector c = (Vector)o;
		if(this.getNumDimensions().equals(c.getNumDimensions())){
			for(long i = 0L; i < c.getNumDimensions(); i++){
				if(!this.getValueAtIndex(i).equals(c.getValueAtIndex(i))){
					return false;
				}
			}
			return true;
		}else{
			return false;
		}
	}

	public Vector copy() {
		List<Long> list = new ArrayList<>();
		for(Long l : this.coordinateValues){
			list.add(l);
		}
		return new Vector(list);
	}

	public Vector(Vector v) {
		this.coordinateValues = v.coordinateValues;
		this.numDimensions = v.numDimensions;
		this.hashCode = getHashCode();
	}

	public Vector(List<Long> coordinateValues) {
		this.coordinateValues = coordinateValues;
		this.numDimensions = Long.valueOf(coordinateValues.size());
		this.hashCode = getHashCode();
	}

	//Don't allow access to coordinate values so that the list cannot be mutated.
	//public List<Long> getCoordinateValues();

	public Vector subtract(Vector other) throws Exception{
		if(this.getNumDimensions().equals(other.getNumDimensions())){
			List<Long> newValues = new ArrayList<Long>();
			for(long i = 0L; i < this.getNumDimensions(); i++){
				newValues.add(this.getValueAtIndex(i) - other.getValueAtIndex(i));
			}
			return new Vector(newValues);
		}else{
			throw new Exception("Dimensions do not match: " + this.getNumDimensions() + " versus " + other.getNumDimensions());
		}
	}

	public final Long getNumDimensions(){
		return this.numDimensions;
	}

	public final Long getValueAtIndex(Long i){
		if(i < this.coordinateValues.size()){
			return this.coordinateValues.get(i.intValue());
		}else{
			return 0L;
		}
	}

	public Vector changeValueAtIndex(Long i, Long v) throws Exception {
		/*
			Don't mutate the existing vector, instead create a new one
		*/
		List<Long> newValues = new ArrayList<Long>();
		for(Long l : this.coordinateValues){
			newValues.add(l);
		}

		if(i < newValues.size()){
			newValues.set(i.intValue(), v);
		}else{
			throw new Exception("Trying to set value to " + v + " beyond end of list at " + i + ".  List length is " + newValues.size());
		}
		return new Vector(newValues);
	}

	public String toString(){
		return "(" + this.coordinateValues.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
	}
}
