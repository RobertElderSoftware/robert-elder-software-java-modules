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

public class ThreeDimensionalCircularBuffer<T>{

        private CuboidAddress cuboidAddress = null;
        private Long startX = null;
        private Long endX = null;

        private Object [] buffer = new Object [0];
        private Object uninitializedObject;

        public ThreeDimensionalCircularBuffer(Object uninitializedObject) {
        	this.uninitializedObject = uninitializedObject;
        }

        public void updateBufferRegion(CuboidAddress newCuboidAddress) throws Exception{
                if(newCuboidAddress == null){
                	throw new Exception("newCuboidAddress == null");
		}
                if(this.cuboidAddress == null){
                	Object [] newBuffer = new Object [(int)newCuboidAddress.getWidthForIndex(0L)];
		}else{
			Coordinate oldLower = this.cuboidAddress.getCanonicalLowerCoordinate();
			Coordinate oldUpper = this.cuboidAddress.getCanonicalUpperCoordinate();

			Coordinate newLower = newCuboidAddress.getCanonicalLowerCoordinate();
			Coordinate newUpper = newCuboidAddress.getCanonicalUpperCoordinate();

			Long oldStartX = oldLower.getX();
			Long oldEndX = oldUpper.getX() + 1L;

			Long newStartX = newLower.getX();
			Long newEndX = newUpper.getX() + 1L;

			Long oldSize = oldEndX - oldStartX;
			Long newSize = newEndX - newStartX;

			Long startXDisplacement = newStartX - oldStartX;
			Long endXDisplacement = newEndX - oldEndX;

			if(oldSize.equals(newSize)){
				//  No re-allocation needed
				this.startX += startXDisplacement;
				this.endX += endXDisplacement;
				int startElementsToInitialize = Math.max(0, startXDisplacement.intValue());
				int endElementsToInitialize = -Math.min(0, endXDisplacement.intValue());
				//  Re-set un-initialized elements at start of buffer:
				for(int i = 0; i < startElementsToInitialize; i++){
        				int index = (this.startX.intValue() + i) % newSize.intValue();
        				this.buffer[index] = uninitializedObject;
				}
				//  Re-set un-initialized elements at end of buffer:
				for(int i = 0; i < endElementsToInitialize; i++){
        				int index = (this.endX.intValue() -1 - i) % newSize.intValue();
        				this.buffer[index] = uninitializedObject;
				}
			}else{
				Object [] newBuffer = new Object [newSize.intValue()];
				for(int i = 0; i < oldSize.intValue(); i++){
        				newBuffer[i] = this.buffer[i];
				}

				this.buffer = newBuffer;
			}
		}
                this.cuboidAddress = cuboidAddress;
        }
}
