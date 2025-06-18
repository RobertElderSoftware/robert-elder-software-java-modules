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

import java.util.Arrays;

public class ThreeDimensionalCircularBuffer<T>{

	private CuboidAddress cuboidAddress = null;
	private Long circularOffsetX = null;  //  The offset that describes where the circular buffer 'starts'.
	private Long circularOffsetY = null;  //  The offset that describes where the circular buffer 'starts'.

	private Object [][] buffer = new Object [0][0];
	private Object uninitializedObject;
	private Class<T> classType;

	public ThreeDimensionalCircularBuffer(Class<T> classType, T uninitializedObject) {
		this.classType = classType;
		this.uninitializedObject = uninitializedObject;
	}

	public static final Long pm(Long numerator, Long denominator){
		return ((numerator % denominator) + denominator) % denominator;
	}

	public static final int pm(int numerator, int denominator){
		return ((numerator % denominator) + denominator) % denominator;
	}

	public void updateBufferRegion(CuboidAddress newCuboidAddress) throws Exception{
		if(newCuboidAddress == null){
			throw new Exception("newCuboidAddress == null");
		}

		if(newCuboidAddress.equals(new CuboidAddress(new Coordinate(Arrays.asList(-8L, 8L)), new Coordinate(Arrays.asList(7L, 10L))))){
			System.out.println("afdsafds");
		}

		if(this.cuboidAddress == null){
			int sizeX = (int)newCuboidAddress.getWidthForIndex(0L);
			int sizeY = (int)newCuboidAddress.getWidthForIndex(1L);
			Object [][] newBuffer = new Object [sizeX][sizeY];
			for(int i = 0; i < sizeX; i++){
				for(int j = 0; j < sizeY; j++){
					newBuffer[i][j] = uninitializedObject;
				}
			}
			this.buffer = newBuffer;
			this.circularOffsetX = 0L;
			this.circularOffsetY = 0L;
		}else{
			Long oldXSize = this.cuboidAddress.getWidthForIndex(0L);
			Long newXSize = newCuboidAddress.getWidthForIndex(0L);

			Long startXDisplacement = newCuboidAddress.getCanonicalLowerCoordinate().getX() - this.cuboidAddress.getCanonicalLowerCoordinate().getX();
			Long endXDisplacement = newCuboidAddress.getCanonicalUpperCoordinate().getX() - this.cuboidAddress.getCanonicalUpperCoordinate().getX();

			Long oldYSize = this.cuboidAddress.getWidthForIndex(1L);
			Long newYSize = newCuboidAddress.getWidthForIndex(1L);

			Long startYDisplacement = newCuboidAddress.getCanonicalLowerCoordinate().getY() - this.cuboidAddress.getCanonicalLowerCoordinate().getY();
			Long endYDisplacement = newCuboidAddress.getCanonicalUpperCoordinate().getY() - this.cuboidAddress.getCanonicalUpperCoordinate().getY();

			if(
				oldXSize.equals(newXSize) &&
				oldYSize.equals(newYSize)
			){
				//  No re-allocation needed
				int startXElementsToInitialize = Math.min(Math.max(0, startXDisplacement.intValue()), newXSize.intValue());
				int endXElementsToInitialize = Math.min(-(Math.min(0, endXDisplacement.intValue())), newXSize.intValue());
				int startYElementsToInitialize = Math.min(Math.max(0, startYDisplacement.intValue()), newYSize.intValue());
				int endYElementsToInitialize = Math.min(-(Math.min(0, endYDisplacement.intValue())), newYSize.intValue());
				//  Re-set un-initialized elements at start of buffer:
				for(int i = 0; i < startXElementsToInitialize; i++){
					int toX = pm(this.circularOffsetX.intValue() + i, newXSize.intValue());
					for(int j = 0; j < newYSize.intValue(); j++){
						int toY = pm(this.circularOffsetY.intValue() + j, newYSize.intValue());
						this.buffer[toX][toY] = uninitializedObject;
					}
				}
				//  Re-set un-initialized elements at end of buffer:
				for(int i = 0; i < endXElementsToInitialize; i++){
					int toX = pm(this.circularOffsetX.intValue() -1 - i, newXSize.intValue());
					for(int j = 0; j < newYSize.intValue(); j++){
						int toY = pm(this.circularOffsetY.intValue() + j, newYSize.intValue());
						this.buffer[toX][toY] = uninitializedObject;
					}
				}

				for(int i = 0; i < newXSize.intValue(); i++){
					int toX = pm(this.circularOffsetX.intValue() + i, newXSize.intValue());
					for(int j = 0; j < startYElementsToInitialize; j++){
						int toY = pm(this.circularOffsetY.intValue() + j, newYSize.intValue());
						this.buffer[toX][toY] = uninitializedObject;
					}
				}

				for(int i = 0; i < newXSize.intValue(); i++){
					int toX = pm(this.circularOffsetX.intValue() + i, newXSize.intValue());
					for(int j = 0; j < endYElementsToInitialize; j++){
						int toY = pm(this.circularOffsetY.intValue() -1 - j, newYSize.intValue());
						this.buffer[toX][toY] = uninitializedObject;
					}
				}


				this.circularOffsetX = pm(this.circularOffsetX + startXDisplacement, newXSize);
				this.circularOffsetY = pm(this.circularOffsetY + startYDisplacement, newYSize);
			}else{
				Object [][] newBuffer = new Object [newXSize.intValue()][newYSize.intValue()];

				CuboidAddress intersectionAddress = this.cuboidAddress.getIntersectionCuboidAddress(newCuboidAddress);

				if(intersectionAddress == null){ //  No intersection, buffer covers entirely new region with no overlap
					for(int i = 0; i < newXSize.intValue(); i++){
						for(int j = 0; j < newYSize.intValue(); j++){
							newBuffer[i][j] = uninitializedObject;
						}
					}
				}else{
					int intersectionSizeX = (int)intersectionAddress.getWidthForIndex(0L);
					int toCopyXOffset = (int)(intersectionAddress.getCanonicalLowerCoordinate().getX() - newCuboidAddress.getCanonicalLowerCoordinate().getX());
					int fromCopyXOffset = (int)(intersectionAddress.getCanonicalLowerCoordinate().getX() - this.cuboidAddress.getCanonicalLowerCoordinate().getX());

					int intersectionSizeY = (int)intersectionAddress.getWidthForIndex(1L);
					int toCopyYOffset = (int)(intersectionAddress.getCanonicalLowerCoordinate().getY() - newCuboidAddress.getCanonicalLowerCoordinate().getY());
					int fromCopyYOffset = (int)(intersectionAddress.getCanonicalLowerCoordinate().getY() - this.cuboidAddress.getCanonicalLowerCoordinate().getY());

					for(int i = 0; i < intersectionSizeX; i++){
						int fromX = pm(this.circularOffsetX.intValue() + i + fromCopyXOffset, oldXSize.intValue());
						int toX = pm(i + toCopyXOffset, newXSize.intValue());
						for(int j = 0; j < intersectionSizeY; j++){
							int fromY = pm(this.circularOffsetY.intValue() + j + fromCopyYOffset, oldYSize.intValue());
							int toY = pm(j + toCopyYOffset, newYSize.intValue());
							newBuffer[toX][toY] = this.buffer[fromX][fromY];
						}
					}

					int startXElementsToInitialize = (Math.max(0, -startXDisplacement.intValue()) % newXSize.intValue());
					int endXElementsToInitialize = -(Math.min(0, -endXDisplacement.intValue()) % newXSize.intValue());
					int startYElementsToInitialize = (Math.max(0, -startYDisplacement.intValue()) % newYSize.intValue());
					int endYElementsToInitialize = -(Math.min(0, -endYDisplacement.intValue()) % newYSize.intValue());

					//  Re-set un-initialized elements at start of buffer:
					for(int i = 0; i < startXElementsToInitialize; i++){
						int toX = pm(i, newXSize.intValue());
						for(int j = 0; j < newYSize.intValue(); j++){
							int toY = pm(j, newYSize.intValue());
							newBuffer[toX][toY] = uninitializedObject;
						}
					}
					//  Re-set un-initialized elements at end of buffer:
					for(int i = 0; i < endXElementsToInitialize; i++){
						int toX = pm(newXSize.intValue() -1 - i, newXSize.intValue());
						for(int j = 0; j < newYSize.intValue(); j++){
							int toY = pm(j, newYSize.intValue());
							newBuffer[toX][toY] = uninitializedObject;
						}
					}

					for(int i = 0; i < newXSize.intValue(); i++){
						int toX = pm(i, newXSize.intValue());
						for(int j = 0; j < startYElementsToInitialize; j++){
							int toY = pm(j, newYSize.intValue());
							newBuffer[toX][toY] = uninitializedObject;
						}
					}

					for(int i = 0; i < newXSize.intValue(); i++){
						int toX = pm(i, newXSize.intValue());
						for(int j = 0; j < endYElementsToInitialize; j++){
							int toY = pm(newYSize.intValue() -1 - j, newYSize.intValue());
							newBuffer[toX][toY] = uninitializedObject;
						}
					}
				}

				this.circularOffsetX = 0L;
				this.circularOffsetY = 0L;
				this.buffer = newBuffer;
			}
		}
		this.cuboidAddress = newCuboidAddress;
	}

	public void setObjectAtCoordinate(Coordinate c, T object) throws Exception{
		if(this.cuboidAddress == null){
			throw new Exception("this.cuboidAddress == null");
		}else{
			Coordinate lower = this.cuboidAddress.getCanonicalLowerCoordinate();
			Coordinate upper = this.cuboidAddress.getCanonicalUpperCoordinate();
			Long lowerBoundX = lower.getX();
			Long upperBoundX = upper.getX() + 1L;
			Long lowerBoundY = lower.getY();
			Long upperBoundY = upper.getY() + 1L;
			if(
				(c.getX() >= lowerBoundX && c.getX() < upperBoundX) &&
				(c.getY() >= lowerBoundY && c.getY() < upperBoundY)
			){
				Long sizeX = this.cuboidAddress.getWidthForIndex(0L);
				Long sizeY = this.cuboidAddress.getWidthForIndex(1L);
				int xIndexInBuffer = (int)((this.circularOffsetX + c.getX() - lowerBoundX) % sizeX);
				int yIndexInBuffer = (int)((this.circularOffsetY + c.getY() - lowerBoundY) % sizeY);
				this.buffer[xIndexInBuffer][yIndexInBuffer] = object;
			}else{
				throw new Exception(
					"(c.getX() = " + c.getX() + ", but c.getX() >= lowerBoundX && c.getX() < upperBoundX) &&" +
					"(c.getY() = " + c.getY() + ", but c.getY() >= lowerBoundY && c.getY() < upperBoundY)"
				);
			}
		}
	}

	public T getObjectAtCoordinate(Coordinate c) throws Exception{
		if(this.cuboidAddress == null){
			throw new Exception("this.cuboidAddress == null");
		}else{
			Coordinate lower = this.cuboidAddress.getCanonicalLowerCoordinate();
			Coordinate upper = this.cuboidAddress.getCanonicalUpperCoordinate();
			Long lowerBoundX = lower.getX();
			Long upperBoundX = upper.getX() + 1L;
			Long lowerBoundY = lower.getY();
			Long upperBoundY = upper.getY() + 1L;
			if(
				(c.getX() >= lowerBoundX && c.getX() < upperBoundX) &&
				(c.getY() >= lowerBoundY && c.getY() < upperBoundY)
			){
				Long sizeX = this.cuboidAddress.getWidthForIndex(0L);
				Long sizeY = this.cuboidAddress.getWidthForIndex(1L);
				int xIndexInBuffer = (int)((this.circularOffsetX + c.getX() - lowerBoundX) % sizeX);
				int yIndexInBuffer = (int)((this.circularOffsetY + c.getY() - lowerBoundY) % sizeY);
				if(this.buffer[xIndexInBuffer][yIndexInBuffer] == null){
					throw new Exception("this.buffer[xIndexInBuffer][yIndexInBuffer] == null");
				}else{
					return classType.cast(this.buffer[xIndexInBuffer][yIndexInBuffer]);
				}
			}else{
				throw new Exception(
					"(c.getX() = " + c.getX() + ", but c.getX() >= lowerBoundX && c.getX() < upperBoundX) &&" +
					"(c.getY() = " + c.getY() + ", but c.getY() >= lowerBoundY && c.getY() < upperBoundY)"
				);
			}
		}
	}
}
