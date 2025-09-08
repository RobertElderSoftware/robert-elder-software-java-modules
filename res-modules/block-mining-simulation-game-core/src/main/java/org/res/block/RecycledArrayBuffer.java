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
import java.util.Objects;

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

public class RecycledArrayBuffer {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private boolean [][][] threeDBooleanArray;
	private int threeDBooleanArray_x;
	private int threeDBooleanArray_y;
	private int threeDBooleanArray_z;

	private boolean [][] twoDBooleanArray;
	private int twoDBooleanArray_x;
	private int twoDBooleanArray_y;

	private boolean [] oneDBooleanArray;
	private int oneDBooleanArray_x;

	private int [][] twoDIntArray;
	private int twoDIntArray_x;
	private int twoDIntArray_y;

	private int [] oneDIntArray;
	private int oneDIntArray_x;

	public RecycledArrayBuffer() throws Exception{

	}

	public boolean [][][] get3DBooleanArray(int x, int y, int z){
		if(this.threeDBooleanArray == null){
			this.threeDBooleanArray = new boolean [x][y][z];
			this.threeDBooleanArray_x = x;
			this.threeDBooleanArray_y = y;
			this.threeDBooleanArray_z = z;
		}else{
			//  If the required array is smaller, don't trigger a re-allocation:
			if(
				x > this.threeDBooleanArray_x ||
				y > this.threeDBooleanArray_y ||
				z > this.threeDBooleanArray_z
			){
				int maxX = Math.max(x, this.threeDBooleanArray_x);
				int maxY = Math.max(y, this.threeDBooleanArray_y);
				int maxZ = Math.max(z, this.threeDBooleanArray_z);
				this.threeDBooleanArray = new boolean [maxX][maxY][maxZ];
				this.threeDBooleanArray_x = maxX;
				this.threeDBooleanArray_y = maxY;
				this.threeDBooleanArray_z = maxZ;
			}else{
				//  Re-initialize the relevant part of the array:
				for (int i = 0; i < x; i++) {
					for (int j = 0; j < y; j++) {
						Arrays.fill(this.threeDBooleanArray[i][j], 0, z, false);
					}
				}
			}
		}
		return this.threeDBooleanArray;
	}

	public boolean [][] get2DBooleanArray(int x, int y){
		if(this.twoDBooleanArray == null){
			this.twoDBooleanArray = new boolean [x][y];
			this.twoDBooleanArray_x = x;
			this.twoDBooleanArray_y = y;
		}else{
			//  If the required array is smaller, don't trigger a re-allocation:
			if(
				x > this.twoDBooleanArray_x ||
				y > this.twoDBooleanArray_y
			){
				int maxX = Math.max(x, this.twoDBooleanArray_x);
				int maxY = Math.max(y, this.twoDBooleanArray_y);
				this.twoDBooleanArray = new boolean [maxX][maxY];
				this.twoDBooleanArray_x = maxX;
				this.twoDBooleanArray_y = maxY;
			}else{
				//  Re-initialize the relevant part of the array:
				for (int i = 0; i < x; i++) {
					Arrays.fill(this.twoDBooleanArray[i], 0, y, false);
				}
			}
		}
		return this.twoDBooleanArray;
	}

	public boolean [] get1DBooleanArray(int x){
		if(this.oneDBooleanArray == null){
			this.oneDBooleanArray = new boolean [x];
			this.oneDBooleanArray_x = x;
		}else{
			//  If the required array is smaller, don't trigger a re-allocation:
			if(
				x > this.oneDBooleanArray_x
			){
				int maxX = Math.max(x, this.oneDBooleanArray_x);
				this.oneDBooleanArray = new boolean [maxX];
				this.oneDBooleanArray_x = maxX;
			}else{
				//  Re-initialize the relevant part of the array:
				Arrays.fill(this.oneDBooleanArray, 0, x, false);
			}
		}
		return this.oneDBooleanArray;
	}

	public int [][] get2DIntArray(int x, int y){
		if(this.twoDIntArray == null){
			this.twoDIntArray = new int [x][y];
			this.twoDIntArray_x = x;
			this.twoDIntArray_y = y;
		}else{
			//  If the required array is smaller, don't trigger a re-allocation:
			if(
				x > this.twoDIntArray_x ||
				y > this.twoDIntArray_y
			){
				int maxX = Math.max(x, this.twoDIntArray_x);
				int maxY = Math.max(y, this.twoDIntArray_y);
				this.twoDIntArray = new int [maxX][maxY];
				this.twoDIntArray_x = maxX;
				this.twoDIntArray_y = maxY;
			}else{
				//  Re-initialize the relevant part of the array:
				for (int i = 0; i < x; i++) {
					Arrays.fill(this.twoDIntArray[i], 0, y, 0);
				}
			}
		}
		return this.twoDIntArray;
	}

	public int [] get1DIntArray(int x){
		if(this.oneDIntArray == null){
			this.oneDIntArray = new int [x];
			this.oneDIntArray_x = x;
		}else{
			//  If the required array is smaller, don't trigger a re-allocation:
			if(
				x > this.oneDIntArray_x
			){
				int maxX = Math.max(x, this.oneDIntArray_x);
				this.oneDIntArray = new int [maxX];
				this.oneDIntArray_x = maxX;
			}else{
				//  Re-initialize the relevant part of the array:
				Arrays.fill(this.oneDIntArray, 0, x, 0);
			}
		}
		return this.oneDIntArray;
	}
}
