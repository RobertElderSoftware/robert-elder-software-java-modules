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
import java.util.Random;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.nio.LongBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class MultiDimensionalNoiseGenerator {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private Long seed;
	private MessageDigest digest;

	public MultiDimensionalNoiseGenerator(Long seed, MessageDigest digest) throws Exception {
		this.seed = seed;
		this.digest = digest;
	}

	double getNoiseDotProductAtVertex(long [] coordinateLong, double [] coordinateDouble) throws Exception {
		long [] hashInput = new long [coordinateLong.length + 1];
		hashInput[0] = this.seed;
		for(int i = 0; i < coordinateLong.length; i++){
			hashInput[i + 1] = coordinateLong[i];
		}

		ByteBuffer hashInputByteBuffer = ByteBuffer.allocate((coordinateLong.length + 1) * Long.BYTES);
		hashInputByteBuffer.order(ByteOrder.nativeOrder());
		hashInputByteBuffer.asLongBuffer().put(hashInput);

		byte [] hashOutput = this.digest.digest(hashInputByteBuffer.array());
		//logger.info("Hash output was: " + BlockModelContext.convertToHex(this.digest.digest(hashInputByteBuffer.array())));

		byte [] bytesForVector = Arrays.copyOfRange(hashOutput, 0, (coordinateLong.length) * Long.BYTES);

		ByteBuffer hashOutputByteBuffer = ByteBuffer.wrap(bytesForVector);
		LongBuffer longHashOutputByteBuffer = hashOutputByteBuffer.asLongBuffer();

		double [] randomGradientVector = new double [coordinateLong.length];
		for(int i = 0; i < coordinateLong.length; i++){
			randomGradientVector[i] = (double)longHashOutputByteBuffer.get(i) / (double)Long.MAX_VALUE;
		}

		//logger.info("randomGradientVector[0]=" + randomGradientVector[0] + " randomGradientVector[1]=" + randomGradientVector[1]);
		double subtraction [] = doubleVectorSubtraction(coordinateDouble, longVectorToDouble(coordinateLong));
		//logger.info(coordinateDouble[0] + " - " + coordinateLong[0] + " = subtraction[0]=" + subtraction[0] + ", " + coordinateDouble[1] + " - " + coordinateLong[1] + " = subtraction[1]=" + subtraction[1]);
		double dotProduct = dotProduct(randomGradientVector, doubleVectorSubtraction(coordinateDouble, longVectorToDouble(coordinateLong))) / randomGradientVector.length;
		if(dotProduct > 1.0 || dotProduct < -1.0){
			throw new Exception("getNoiseDotProductAtVertex returning dotProduct out of range: " + dotProduct + " Numerical Imprecision?");
		}
		return dotProduct;
	}

	double smoothInterploation(double start, double end, double c) {
		double result = (end - start) * ((c * (c * 6.0 - 15.0) + 10.0) * c * c * c) + start;
		//logger.info("Interpolation a=" + a + " b=" + b + " offset=" + offset + " result=" + result);
		return result;
	}

	double dotProduct(double [] u, double [] v){
		double total = 0;
		for(int i = 0; i < v.length; i++){
			total += u[i] * v[i];
		}
		return total;
	}

	long [] makeVertexCoordinate(long [] vertexOffset, long l){
		//  Add one position to the coordinate that's being built up to specify the n-dimensional vertex of the hypercube:
		long [] newVertexOffset = new long [vertexOffset.length + 1];
		for(int i = 0; i < vertexOffset.length; i++){
			newVertexOffset[i] = vertexOffset[i];
		}
		newVertexOffset[vertexOffset.length] = l;
		return newVertexOffset;
	}

	double getInterpolatedGradientForDimension(long coordinateLong [], double [] coordinateDouble, long [] vertexOffset, int dimensionIndex, double [] offsetInSquare) throws Exception{
		if(dimensionIndex == coordinateLong.length -1){
			double x0 = getNoiseDotProductAtVertex(longVectorAddition(coordinateLong, makeVertexCoordinate(vertexOffset, 0L)), coordinateDouble);
			double x1 = getNoiseDotProductAtVertex(longVectorAddition(coordinateLong, makeVertexCoordinate(vertexOffset, 1L)), coordinateDouble);
			return smoothInterploation(x0, x1, offsetInSquare[dimensionIndex]);
		}else{
			return smoothInterploation(
				getInterpolatedGradientForDimension(coordinateLong, coordinateDouble, makeVertexCoordinate(vertexOffset, 0L), dimensionIndex + 1, offsetInSquare),
				getInterpolatedGradientForDimension(coordinateLong, coordinateDouble, makeVertexCoordinate(vertexOffset, 1L), dimensionIndex + 1, offsetInSquare),
				offsetInSquare[dimensionIndex]
			);
		}
	}

	double noiseAtCoordinate(double [] coordinateDouble) throws Exception {
		long [] coordinateLong = doubleVectorToLong(coordinateDouble);

		double [] offsetInSquare = doubleVectorSubtraction(coordinateDouble, longVectorToDouble(coordinateLong));

		double r = getInterpolatedGradientForDimension(coordinateLong, coordinateDouble, new long [] {}, 0, offsetInSquare);
		//logger.info("ix0: " + ix0 + " ix1: " + ix1 + " smothed=" + r);
		return r;
	}

	long [] doubleVectorToLong(double [] v) throws Exception{
		long [] u = new long [v.length];
		for(int i = 0; i < v.length; i++){
			u[i] = (long)Math.floor(v[i]);
			if(u[i] == Long.MAX_VALUE){
				throw new Exception("Possible overflow detected: u[i] == Long.MAX_VALUE == " + u[i]);
			}
			if(u[i] == Long.MIN_VALUE){
				throw new Exception("Possible underflow detected: u[i] == Long.MIN_VALUE == " + u[i]);
			}
		}
		return u;
	}

	double [] doubleVectorAddition(double [] u, double [] v){
		double [] w = new double [u.length];
		for(int i = 0; i < u.length; i++){
			w[i] = u[i] + v[i];
		}
		return w;
	}

	double [] doubleVectorSubtraction(double [] u, double [] v){
		double [] w = new double [u.length];
		for(int i = 0; i < u.length; i++){
			w[i] = u[i] - v[i];
		}
		return w;
	}

	long [] longVectorAddition(long [] u, long [] v){
		long [] w = new long [u.length];
		for(int i = 0; i < u.length; i++){
			w[i] = u[i] + v[i];
		}
		return w;
	}

	double [] longVectorToDouble(long [] v){
		double [] u = new double [v.length];
		for(int i = 0; i < v.length; i++){
			u[i] = (double)v[i];
		}
		return u;
	}

	double [] multiplyDoubleVectorByScalar(double [] v, double s){
		double [] w = new double [v.length];
		for(int i = 0; i < v.length; i++){
			w[i] = v[i] * s;
		}
		return w;
	}

	/* Values returned by this function are NOT normalized to any fixed range.  If you submit large amplitudes, the return values will also be large.  */
	double multiOctaveNoiseAtCoordinate(long [] coordinateLong, double [] frequencies, double [] amplitudes) throws Exception{
		if(frequencies.length != amplitudes.length){
			throw new Exception("Length missmatch between frequencies: " + frequencies.length + " and amplitudes:" + amplitudes.length);
		}
		int requiredHashOutputSizeBytes = ((coordinateLong.length + 1) * Long.BYTES);
		int actualHashOutputSizeBytes = this.digest.getDigestLength();
		if(requiredHashOutputSizeBytes > actualHashOutputSizeBytes){
			throw new Exception("Cannot handle this many dimensions: " + coordinateLong.length + ".  You could easily fix this by using a different noise hash function with a larger output size. requiredHashOutputSizeBytes=" + requiredHashOutputSizeBytes + ", actualHashOutputSizeBytes=" + actualHashOutputSizeBytes);
		}
		double totalSoFar = 0.0;

		double [] coordinateDouble = longVectorToDouble(coordinateLong);

		for(int i = 0; i < frequencies.length; i++) {
			//logger.info("Multiplying coordinate by frequency: " + frequency);
			double n = noiseAtCoordinate(multiplyDoubleVectorByScalar(coordinateDouble, frequencies[i]));
			if(n < -1.0 || n > 1.0){
				throw new Exception("Nose value of range: " + n + ".  Numerical precision error?");
			}
			totalSoFar += n * amplitudes[i];
		}

		return totalSoFar;
	}
}
