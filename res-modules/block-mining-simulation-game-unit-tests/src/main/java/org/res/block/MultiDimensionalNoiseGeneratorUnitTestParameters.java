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
import java.util.Map;
import java.util.HashMap;

import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.security.MessageDigest;

class MultiDimensionalNoiseGeneratorUnitTestParameters {

	private String testName;
	private MessageDigest digest;
	private boolean makeVideo;
	private Integer width;
	private Integer height;
	private Integer xOffset;
	private Integer yOffset;
	private Integer numFrames;
	private MultiDimensionalNoiseGeneratorUnitTestPixelNoiseInterface noiseFunction;

	public MultiDimensionalNoiseGeneratorUnitTestParameters(String testName, MessageDigest digest, boolean makeVideo, int width, int height, int xOffset, int yOffset, int numFrames, MultiDimensionalNoiseGeneratorUnitTestPixelNoiseInterface noiseFunction) throws Exception {
		this.testName = testName;
		this.digest = digest;
		this.makeVideo = makeVideo;
		this.width = width;
		this.height = height;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.numFrames = numFrames;
		this.noiseFunction = noiseFunction;
	}

	public boolean getMakeVideo(){
		return this.makeVideo;
	}

	public MessageDigest getMessageDigest(){
		return this.digest;
	}

	public String getTestName(){
		return testName;
	}

	public Integer getWidth(){
		return width;
	}

	public Integer getHeight(){
		return height;
	}

	public Integer getXOffset(){
		return xOffset;
	}
	
	public Integer getYOffset(){
		return yOffset;
	}

	public Integer getNumFrames(){
		return numFrames;
	}

	public MultiDimensionalNoiseGeneratorUnitTestPixelNoiseInterface getNoiseFunction(){
		return this.noiseFunction;
	}
}
