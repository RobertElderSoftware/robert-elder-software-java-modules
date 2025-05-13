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
import java.util.TreeSet;
import java.lang.Comparable;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class CuboidAddress implements Comparable<CuboidAddress>{

	private final Coordinate lower;
	private final Coordinate upper;
	private final Long numDimensions;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final int hashCode;

	public final int getHashCode(){
		return this.getCanonicalLowerCoordinate().hashCode() * this.getCanonicalUpperCoordinate().hashCode();
	}

	private static Coordinate canonicalizeLowerCoordinate(Coordinate a, Coordinate b) {
		/*
			Each hypercube will have 2^numDimensions corners, so we'll
			need to canonicalize them for the purposes of doing other calculations.
			The canonical lower coordinate is the coordinate that is inside the
			region being represented by the cube that has the lowest value for 
			every coordinate.  The upper canonical coordinate is the same, but
			it has the highest value for every position.
		*/
		List<Long> canonical = new ArrayList<Long>();
		for(long i = 0L; i < a.getNumDimensions(); i++){
			canonical.add(Math.min(a.getValueAtIndex(i), b.getValueAtIndex(i)));
		}
		return new Coordinate(canonical);
	}

	private static Coordinate canonicalizeUpperCoordinate(Coordinate a, Coordinate b) {
		List<Long> canonical = new ArrayList<Long>();
		for(long i = 0L; i < a.getNumDimensions(); i++){
			canonical.add(Math.max(a.getValueAtIndex(i), b.getValueAtIndex(i)));
		}
		return new Coordinate(canonical);
	}

	public Double getCentroidDistanceFromCoordinate(Coordinate c){
		/*  Calculate the centroid of this cuboid address return the linear distance from a given coordinate. */
		Coordinate lower = this.getCanonicalLowerCoordinate();
		Coordinate upper = this.getCanonicalUpperCoordinate();
		double centroid [] = new double [this.numDimensions.intValue()];
		for(long i = 0L; i < this.numDimensions; i++){
			centroid[(int)i] = (double)lower.getValueAtIndex(i) + ((double)(upper.getValueAtIndex(i) - lower.getValueAtIndex(i)) / 2.0);
		}

		double totalSquare = 0.0;
		for(long i = 0L; i < this.numDimensions; i++){
			double diff = c.getValueAtIndex(i) - centroid[(int)i];
			totalSquare += diff * diff;
		}
		return Math.sqrt(totalSquare);
	}

	public CuboidAddress(Coordinate a, Coordinate b) throws Exception {
		if(a.getNumDimensions().equals(b.getNumDimensions())){
			this.lower = CuboidAddress.canonicalizeLowerCoordinate(a,b);
			this.upper = CuboidAddress.canonicalizeUpperCoordinate(a,b);
			this.numDimensions = lower.getNumDimensions();
			this.hashCode = this.getHashCode();
		}else{
			throw new Exception("Number of dimensions in both coords does not match lower:" + a.getNumDimensions() + " upper:" + a.getNumDimensions());
		}
	}

	public CuboidAddress copy() throws Exception{
		return new CuboidAddress(this.lower.copy(), this.upper.copy());
	}

	public Long getNumDimensions(){
		return this.lower.getNumDimensions();
	}

	public static CuboidAddress readCuboidAddress(BlockMessageBinaryBuffer buffer, long numDimensions) throws Exception {
		Coordinate a = Coordinate.readCoordinate(buffer, numDimensions);
		Coordinate b = Coordinate.readCoordinate(buffer, numDimensions);
		return new CuboidAddress(a,b);
	}

	public static void writeCuboidAddress(BlockMessageBinaryBuffer buffer, CuboidAddress c) {
		Coordinate.writeCoordinate(buffer, c.getCanonicalLowerCoordinate());
		Coordinate.writeCoordinate(buffer, c.getCanonicalUpperCoordinate());
	}

	public boolean containsCoordinate(Coordinate c) throws Exception {
		if(this.getNumDimensions().equals(c.getNumDimensions())){
			Coordinate aLower = this.getCanonicalLowerCoordinate();
			Coordinate aUpper = this.getCanonicalUpperCoordinate();
			for(long i = 0; i < this.getNumDimensions(); i++){
				if(c.getValueAtIndex(i) >= aLower.getValueAtIndex(i) && c.getValueAtIndex(i) <= aUpper.getValueAtIndex(i)){
				}else{
					return false;
				}
			}
		}else{
			throw new Exception("Number of dimensions in both coords does not match a:" + lower.getNumDimensions() + " c:" + c.getNumDimensions());
		}
		return true;
	}

	public Long getLinearArrayIndexForCoordinate(Coordinate coordinate) throws Exception {
		Coordinate lower = this.getCanonicalLowerCoordinate();
		long totalIndex = 0L;
		long dimensionValue = 1L;
		for(long i = 0L; i < coordinate.getNumDimensions(); i++){
			long dimensionOffset = coordinate.getValueAtIndex(i) - lower.getValueAtIndex(i);
			if(dimensionOffset < 0){
				throw new Exception("DimensionOffset was negative? :" + dimensionOffset);
			}
			totalIndex += dimensionOffset * dimensionValue;
			dimensionValue *= this.getWidthForIndex(i);
		}

		return totalIndex;
	}


	public final Coordinate getCanonicalLowerCoordinate() {
		return this.lower;
	}

	public final Coordinate getCanonicalUpperCoordinate() {
		return this.upper;
	}

	public Set<CuboidAddress> getIntersectingChunkSet_h(CuboidAddress chunkSize, Coordinate regionLower, Coordinate regionUpper, List<Vector> previousDimensionRanges, long l) throws Exception {
		if(l < chunkSize.getNumDimensions()){
			long chunkDimensionWidth = chunkSize.getWidthForIndex(l);
			long lowerValue = Math.floorDiv(regionLower.getValueAtIndex(l), chunkDimensionWidth) * chunkDimensionWidth; // Floor of coordinate divide by chunk 
			long rounding = (regionUpper.getValueAtIndex(l) % chunkDimensionWidth) == 0L ? 0L : 1L;
			long upperValue = (Math.floorDiv(regionUpper.getValueAtIndex(l), chunkDimensionWidth) * chunkDimensionWidth) + rounding;
			//logger.info("For dimension " + l + " chunkDimensionWidth=" + chunkDimensionWidth + " lowerValue=" + lowerValue + " upperValue=" + upperValue);

			Set<CuboidAddress> rtn = new TreeSet<CuboidAddress>();
			for(long p = lowerValue; p <= upperValue; p+= chunkDimensionWidth){
				previousDimensionRanges.add(new Vector(Arrays.asList(p, p + chunkDimensionWidth -1L)));
				rtn.addAll(getIntersectingChunkSet_h(chunkSize, regionLower, regionUpper, previousDimensionRanges, l + 1L));
				previousDimensionRanges.remove(previousDimensionRanges.size() - 1);
			}
			return rtn;
		}else{
			List<Long> lower = new ArrayList<Long>();
			List<Long> upper = new ArrayList<Long>();
			for(Vector v : previousDimensionRanges){
				lower.add(v.getValueAtIndex(0L));
				upper.add(v.getValueAtIndex(1L));
			}
			
			Set<CuboidAddress> rtn = new TreeSet<CuboidAddress>();
			rtn.add(new CuboidAddress(new Coordinate(lower), new Coordinate(upper)));
			return rtn;
		}
	}

	public Set<CuboidAddress> getIntersectingChunkSet(CuboidAddress chunkSize) throws Exception {
		/*
			Return the set of chunk cuboids required to cover a region.
			The resulting set of cuboid will over an area of space equal to
			or greater than that of the original region, but they will
			consist of cuboids that are all aligned on a grid with respect
			to the origin chunkSize cuboid.
		*/
		Coordinate chunkSizeLower = chunkSize.getCanonicalLowerCoordinate();
		Coordinate chunkSizeUpper = chunkSize.getCanonicalUpperCoordinate();
		Coordinate regionLower = this.getCanonicalLowerCoordinate();
		Coordinate regionUpper = this.getCanonicalUpperCoordinate();
		for(long l = 0L; l < chunkSizeLower.getNumDimensions(); l++){
			if(!chunkSizeLower.getValueAtIndex(l).equals(0L)){
				throw new Exception("Expecting that reference chunk size will have a lower coordinate at origin, but lower was: " + chunkSizeLower);
			}
		}

		return getIntersectingChunkSet_h(chunkSize, regionLower, regionUpper, new ArrayList<Vector>(), 0L);
	}

	public CuboidAddress getIntersectionCuboidAddress(CuboidAddress other) throws Exception {
		List<Vector> dimensionalOverlapRanges = this.getDimensionalOverlapRangesWith(other);
		//logger.info("Here are the the dimensional overlap ranges: " + dimensionalOverlapRanges + ".");
		return CuboidAddress.calculateIntersectionCuboidAddressFromDimensionalOverlapRanges(dimensionalOverlapRanges);
	}

	@Override
	public int compareTo(CuboidAddress other) {
		int lowerComparison = this.getCanonicalLowerCoordinate().compareTo(other.getCanonicalLowerCoordinate());
		int upperComparison = this.getCanonicalUpperCoordinate().compareTo(other.getCanonicalUpperCoordinate());

		//  They are only equal if both the upper and lower coordinate
		//  are identical.
		if(lowerComparison == 0 && upperComparison == 0){
			return 0;
		}else{
			if(lowerComparison < upperComparison){
				return -1;
			}else if(lowerComparison > upperComparison){
				return 1;
			}else{
				//  The 'differences' are equal, so just return one of them:
				return lowerComparison;
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		CuboidAddress other = (CuboidAddress)o;
		if(other == null){
			return false;
		}else{
			Coordinate aLower = this.getCanonicalLowerCoordinate();
			Coordinate aUpper = this.getCanonicalUpperCoordinate();
			Coordinate bLower = other.getCanonicalLowerCoordinate();
			Coordinate bUpper = other.getCanonicalUpperCoordinate();
			if(aLower.equals(bLower) && aUpper.equals(bUpper)){
				return true;
			}else{
				return false;
			}
		}
	}

	@Override
	public int hashCode(){
		return this.hashCode;
	}

	public long getVolume() {
		long total = 1L;
		Coordinate lower = this.getCanonicalLowerCoordinate();
		Coordinate upper = this.getCanonicalUpperCoordinate();
		for(long i = 0L; i < lower.getNumDimensions(); i++){
			total *= ((upper.getValueAtIndex(i) - lower.getValueAtIndex(i))+1L);
		}
		return total;
	}

	public long getWidthForIndex(Long index) {
		Coordinate lower = this.getCanonicalLowerCoordinate();
		Coordinate upper = this.getCanonicalUpperCoordinate();
		return (upper.getValueAtIndex(index) - lower.getValueAtIndex(index)) + 1L;
	}

	public String toString(){
		return getCanonicalLowerCoordinate().toString() + " -> " + getCanonicalUpperCoordinate().toString();
	}


	public Vector getOverlapRangeVector(long aStart, long aEnd, long bStart, long bEnd){
		/*
			aStart and aEnd denotes one line segment along the axis and
			bStart bEnd denote the second line segment along the same axis.
			The purpose of this function is to calculate the range of overlap between
			these value ranges.  Return null if there is no overlap.
		*/
		long aMin = Math.min(aStart, aEnd);
		long aMax = Math.max(aStart, aEnd);
		long bMin = Math.min(bStart, bEnd);
		long bMax = Math.max(bStart, bEnd);

		if(aMax < bMin){
			return null; /*  No intersection */
		}else{
			if(aMin > bMax){
				return null; /*  No intersection */
			}else{
				//  Intersection starts at the highest 'low' coordinate:
				Long lowerOverlap = aMin >= bMin ? aMin : bMin;
				//  And goes to the lowest 'upper' coordinate:
				Long upperOverlap = aMax <= bMax ? aMax : bMax;
				return new Vector(Arrays.asList(lowerOverlap, upperOverlap));
			}
		}
	}

	public List<Vector> getDimensionalOverlapRangesWith(CuboidAddress other) throws Exception {
		/*
			Find the common over lap region for each dimension
		*/
		if(this.getNumDimensions().equals(other.getNumDimensions())){
			Coordinate aLower = this.getCanonicalLowerCoordinate();
			Coordinate aUpper = this.getCanonicalUpperCoordinate();
			Coordinate bLower = other.getCanonicalLowerCoordinate();
			Coordinate bUpper = other.getCanonicalUpperCoordinate();
			List<Vector> overlapRanges = new ArrayList<Vector>();
			for(long l = 0L; l < this.getNumDimensions(); l++){
				overlapRanges.add(
					this.getOverlapRangeVector(
						aLower.getValueAtIndex(l),
						aUpper.getValueAtIndex(l),
						bLower.getValueAtIndex(l),
						bUpper.getValueAtIndex(l)
					)
				);
			}
			return overlapRanges;
		}else{
			throw new Exception("Number of dimensions in both cuboids not match a:" + lower.getNumDimensions() + " other:" + other.getNumDimensions());
		}
	}


	public static CuboidAddress calculateIntersectionCuboidAddressFromDimensionalOverlapRanges(List<Vector> dimensionalOverlapRanges) throws Exception {
		List<Long> lowerValues = new ArrayList<Long>();
		List<Long> upperValues = new ArrayList<Long>();
		for(long l = 0L; l < dimensionalOverlapRanges.size(); l++){
			Vector v = dimensionalOverlapRanges.get((int)l);
			if(v == null){
				/*  If the overlap range of any single dimension is empty, then there is not intersection at all. */
				return null;
			}else{
				lowerValues.add(v.getValueAtIndex(0L)); /*  First value is range min */
				upperValues.add(v.getValueAtIndex(1L)); /*  First value is range max */
			}
		}
		return new CuboidAddress(new Coordinate(lowerValues), new Coordinate(upperValues));
	}

	public static CuboidAddress blockCoordinateToChunkCuboidAddress(Coordinate blockCoordinate, CuboidAddress chunkSize) throws Exception {
		if(blockCoordinate.getNumDimensions().equals(chunkSize.getNumDimensions())){
			List<Long> chunkCoordsLower = new ArrayList<Long>();
			List<Long> chunkCoordsUpper = new ArrayList<Long>();
			for(long i = 0L; i < chunkSize.getNumDimensions(); i++){
				Coordinate chunkSizeLower = chunkSize.getCanonicalLowerCoordinate();
				if(chunkSizeLower.getValueAtIndex(i).equals(0L)){
					Long startingCuboidOffset = Math.floorDiv(blockCoordinate.getValueAtIndex(i), chunkSize.getWidthForIndex(i)) * chunkSize.getWidthForIndex(i);
					chunkCoordsLower.add(startingCuboidOffset);
					chunkCoordsUpper.add(startingCuboidOffset + (chunkSize.getWidthForIndex(i) -1L));
				}else{
					throw new Exception("Chunk size cuboid address was supposed to be at origin, but it's at " + chunkSizeLower);
				}
			}
			return new CuboidAddress(new Coordinate(chunkCoordsLower), new Coordinate(chunkCoordsUpper));
		}else{
			throw new Exception("Dimension size missmatch.");
		}
	}

}
