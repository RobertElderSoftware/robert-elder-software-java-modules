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

	public int startX;
	public int startY;
	public int endX;
	public int endY;

	public ScreenRegion(int startX, int startY, int endX, int endY){
		this.startX = startX;
		this.startY = startY;
		this.endX = endX;
		this.endY = endY;
	}

	public int getStartX(){
		return this.startX;
	}

	public int getStartY(){
		return this.startY;
	}

	public int getEndX(){
		return this.endX;
	}

	public int getEndY(){
		return this.endY;
	}

	@Override
	public String toString(){
		return "startX=" + String.valueOf(this.startX) + ", startY=" + String.valueOf(this.startY) + ", endX=" + String.valueOf(this.endX) + ", endY=" + String.valueOf(this.endY);
	}

	@Override
	public int compareTo(ScreenRegion other) {
		if(this.startX < other.getStartX()){
			return -1;
		}else if(this.startX > other.getStartX()){
			return 1;
		}else{
			if(this.startY < other.getStartY()){
				return -1;
			}else if(this.startY > other.getStartY()){
				return 1;
			}else{
				if(this.endX < other.getEndX()){
					return -1;
				}else if(this.endX > other.getEndX()){
					return 1;
				}else{
					if(this.endY < other.getEndY()){
						return -1;
					}else if(this.endY > other.getEndY()){
						return 1;
					}else{
						return 0;
					}
				}
			}
		}
	}

	@Override
	public final int hashCode(){
		return this.startX;
	}

	@Override
	public boolean equals(Object o){
		ScreenRegion r = (ScreenRegion)o;
		return (
			this.startX == r.getStartX() &&
			this.startY == r.getStartY() &&
			this.endX == r.getEndX() &&
			this.endY == r.getEndY()
		);
	}
}
