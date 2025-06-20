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
	private Long circularOffsetZ = null;  //  The offset that describes where the circular buffer 'starts'.

	private Object [][][] buffer = new Object [0][0][0];
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

	public static void initializeEmptyRegions(Object [][][] a, Long circularOffsetX, Long circularOffsetY, Long circularOffsetZ, Long sizeX, Long sizeY, Long sizeZ, Object uninitializedObject, CuboidAddress intersection, CuboidAddress regionToInitialize) throws Exception{

		RegionIteration regionIteration = new RegionIteration(regionToInitialize.getCanonicalLowerCoordinate(), regionToInitialize);
		do{
			Coordinate currentCoordinate = regionIteration.getCurrentCoordinate();
			if(intersection == null || (!intersection.containsCoordinate(currentCoordinate))){
				Coordinate lower = regionToInitialize.getCanonicalLowerCoordinate();
				int xIndexInBuffer = (int)((circularOffsetX + currentCoordinate.getX() - lower.getX()) % sizeX);
				int yIndexInBuffer = (int)((circularOffsetY + currentCoordinate.getY() - lower.getY()) % sizeY);
				int zIndexInBuffer = (int)((circularOffsetZ + currentCoordinate.getZ() - lower.getZ()) % sizeZ);
				a[xIndexInBuffer][yIndexInBuffer][zIndexInBuffer] = uninitializedObject;
			}
		}while (regionIteration.incrementCoordinateWithinCuboidAddress());
	}

	public void updateBufferRegion(CuboidAddress newCuboidAddress) throws Exception{
		if(newCuboidAddress == null){
			throw new Exception("newCuboidAddress == null");
		}

		CuboidAddress intersectionAddress = this.cuboidAddress == null ? null : this.cuboidAddress.getIntersectionCuboidAddress(newCuboidAddress);

		Long newXSize = newCuboidAddress.getWidthForIndex(0L);
		Long newYSize = newCuboidAddress.getWidthForIndex(1L);
		Long newZSize = newCuboidAddress.getWidthForIndex(2L);
		if(
			intersectionAddress != null && 
			newXSize.equals(this.cuboidAddress.getWidthForIndex(0L)) &&
			newYSize.equals(this.cuboidAddress.getWidthForIndex(1L)) &&
			newZSize.equals(this.cuboidAddress.getWidthForIndex(2L))
		){
			//  Size has not changed, simply update pointers to advance through circular buffer
			Long startXDisplacement = newCuboidAddress.getCanonicalLowerCoordinate().getX() - this.cuboidAddress.getCanonicalLowerCoordinate().getX();
			Long startYDisplacement = newCuboidAddress.getCanonicalLowerCoordinate().getY() - this.cuboidAddress.getCanonicalLowerCoordinate().getY();
			Long startZDisplacement = newCuboidAddress.getCanonicalLowerCoordinate().getZ() - this.cuboidAddress.getCanonicalLowerCoordinate().getZ();
			this.circularOffsetX = pm(this.circularOffsetX + startXDisplacement, newXSize);
			this.circularOffsetY = pm(this.circularOffsetY + startYDisplacement, newYSize);
			this.circularOffsetZ = pm(this.circularOffsetZ + startZDisplacement, newZSize);
		}else{
			Object [][][] newBuffer = new Object [newXSize.intValue()][newYSize.intValue()][newZSize.intValue()];

			if(intersectionAddress != null){ //  No intersection, buffer covers entirely new region with no overlap
				int intersectionSizeX = (int)intersectionAddress.getWidthForIndex(0L);
				int toCopyXOffset = (int)(intersectionAddress.getCanonicalLowerCoordinate().getX() - newCuboidAddress.getCanonicalLowerCoordinate().getX());
				int fromCopyXOffset = (int)(intersectionAddress.getCanonicalLowerCoordinate().getX() - this.cuboidAddress.getCanonicalLowerCoordinate().getX());

				int intersectionSizeY = (int)intersectionAddress.getWidthForIndex(1L);
				int toCopyYOffset = (int)(intersectionAddress.getCanonicalLowerCoordinate().getY() - newCuboidAddress.getCanonicalLowerCoordinate().getY());
				int fromCopyYOffset = (int)(intersectionAddress.getCanonicalLowerCoordinate().getY() - this.cuboidAddress.getCanonicalLowerCoordinate().getY());

				int intersectionSizeZ = (int)intersectionAddress.getWidthForIndex(2L);
				int toCopyZOffset = (int)(intersectionAddress.getCanonicalLowerCoordinate().getZ() - newCuboidAddress.getCanonicalLowerCoordinate().getZ());
				int fromCopyZOffset = (int)(intersectionAddress.getCanonicalLowerCoordinate().getZ() - this.cuboidAddress.getCanonicalLowerCoordinate().getZ());

				Long oldXSize = this.cuboidAddress.getWidthForIndex(0L);
				Long oldYSize = this.cuboidAddress.getWidthForIndex(1L);
				Long oldZSize = this.cuboidAddress.getWidthForIndex(2L);

				for(int i = 0; i < intersectionSizeX; i++){
					int fromX = pm(this.circularOffsetX.intValue() + i + fromCopyXOffset, oldXSize.intValue());
					int toX = pm(i + toCopyXOffset, newXSize.intValue());
					for(int j = 0; j < intersectionSizeY; j++){
						int fromY = pm(this.circularOffsetY.intValue() + j + fromCopyYOffset, oldYSize.intValue());
						int toY = pm(j + toCopyYOffset, newYSize.intValue());
						for(int k = 0; k < intersectionSizeZ; k++){
							int fromZ = pm(this.circularOffsetZ.intValue() + k + fromCopyZOffset, oldZSize.intValue());
							int toZ = pm(k + toCopyZOffset, newZSize.intValue());
							newBuffer[toX][toY][toZ] = this.buffer[fromX][fromY][fromZ];
						}
					}
				}
			}
			this.circularOffsetX = 0L;
			this.circularOffsetY = 0L;
			this.circularOffsetZ = 0L;
			this.buffer = newBuffer;
		}
		ThreeDimensionalCircularBuffer.initializeEmptyRegions(this.buffer, this.circularOffsetX, this.circularOffsetY, this.circularOffsetZ, newXSize, newYSize, newZSize, uninitializedObject, intersectionAddress, newCuboidAddress);
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
			Long lowerBoundZ = lower.getZ();
			Long upperBoundZ = upper.getZ() + 1L;
			if(
				(c.getX() >= lowerBoundX && c.getX() < upperBoundX) &&
				(c.getY() >= lowerBoundY && c.getY() < upperBoundY) &&
				(c.getZ() >= lowerBoundZ && c.getZ() < upperBoundZ)
			){
				Long sizeX = this.cuboidAddress.getWidthForIndex(0L);
				Long sizeY = this.cuboidAddress.getWidthForIndex(1L);
				Long sizeZ = this.cuboidAddress.getWidthForIndex(2L);
				int xIndexInBuffer = (int)((this.circularOffsetX + c.getX() - lowerBoundX) % sizeX);
				int yIndexInBuffer = (int)((this.circularOffsetY + c.getY() - lowerBoundY) % sizeY);
				int zIndexInBuffer = (int)((this.circularOffsetZ + c.getZ() - lowerBoundZ) % sizeZ);
				this.buffer[xIndexInBuffer][yIndexInBuffer][zIndexInBuffer] = object;
			}else{
				throw new Exception(
					"(c.getX() = " + c.getX() + ", but c.getX() >= lowerBoundX && c.getX() < upperBoundX) &&" +
					"(c.getY() = " + c.getY() + ", but c.getY() >= lowerBoundY && c.getY() < upperBoundY) &&" +
					"(c.getZ() = " + c.getZ() + ", but c.getZ() >= lowerBoundZ && c.getZ() < upperBoundZ)"
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
			Long lowerBoundZ = lower.getZ();
			Long upperBoundZ = upper.getZ() + 1L;
			if(
				(c.getX() >= lowerBoundX && c.getX() < upperBoundX) &&
				(c.getY() >= lowerBoundY && c.getY() < upperBoundY) &&
				(c.getZ() >= lowerBoundZ && c.getZ() < upperBoundZ)
			){
				Long sizeX = this.cuboidAddress.getWidthForIndex(0L);
				Long sizeY = this.cuboidAddress.getWidthForIndex(1L);
				Long sizeZ = this.cuboidAddress.getWidthForIndex(2L);
				int xIndexInBuffer = (int)((this.circularOffsetX + c.getX() - lowerBoundX) % sizeX);
				int yIndexInBuffer = (int)((this.circularOffsetY + c.getY() - lowerBoundY) % sizeY);
				int zIndexInBuffer = (int)((this.circularOffsetZ + c.getZ() - lowerBoundZ) % sizeZ);
				if(this.buffer[xIndexInBuffer][yIndexInBuffer][zIndexInBuffer] == null){
					throw new Exception("this.buffer[xIndexInBuffer][yIndexInBuffer][zIndexInBuffer] == null");
				}else{
					return classType.cast(this.buffer[xIndexInBuffer][yIndexInBuffer][zIndexInBuffer]);
				}
			}else{
				throw new Exception(
					"(c.getX() = " + c.getX() + ", but c.getX() >= lowerBoundX && c.getX() < upperBoundX) &&" +
					"(c.getY() = " + c.getY() + ", but c.getY() >= lowerBoundY && c.getY() < upperBoundY) &&" +
					"(c.getZ() = " + c.getZ() + ", but c.getZ() >= lowerBoundZ && c.getZ() < upperBoundZ)"
				);
			}
		}
	}
}
