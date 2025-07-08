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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;
import java.io.ByteArrayOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Collections;
import java.util.Comparator;

import java.lang.Thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class ScreenRegion implements Comparable<ScreenRegion>{

	private CuboidAddress region;

	public static CuboidAddress makeScreenRegionCA(int startX, int startY, int endX, int endY) throws Exception{
		return new CuboidAddress(
			new Coordinate(Arrays.asList((long)startX, (long)startY)),
			new Coordinate(Arrays.asList((long)endX, (long)endY))
		);
	}

	public ScreenRegion(CuboidAddress region) throws Exception{
		this.region = region;
	}

	public int getStartX() throws Exception{
		return this.region.getCanonicalLowerCoordinate().getX().intValue();
	}

	public int getStartY()throws Exception{
		return this.region.getCanonicalLowerCoordinate().getY().intValue();
	}

	public int getEndX()throws Exception{
		return this.region.getCanonicalUpperCoordinate().getX().intValue();
	}

	public int getEndY()throws Exception{
		return this.region.getCanonicalUpperCoordinate().getY().intValue();
	}

	public CuboidAddress getRegion(){
		return this.region;
	}

	@Override
	public String toString(){
		return "region=" + this.getRegion().toString();
	}

	@Override
	public int compareTo(ScreenRegion other) {
		return this.region.compareTo(other.getRegion());
	}

	@Override
	public final int hashCode(){
		return this.getRegion().hashCode();
	}

	@Override
	public boolean equals(Object o){
		ScreenRegion r = (ScreenRegion)o;
		return r.getRegion().equals(this.region);
	}
}
