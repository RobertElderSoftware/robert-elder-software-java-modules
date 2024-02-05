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

import java.util.Map;
import java.util.List;
import java.util.Arrays;


import java.lang.reflect.Constructor;

import java.lang.reflect.InvocationTargetException;


public abstract class IndividualBlock {

	public abstract byte [] getBlockData() throws Exception;
	public abstract boolean isMineable() throws Exception;
	public abstract String getTerminalPresentation() throws Exception;
	public abstract Long getTerminalPresentationWidth() throws Exception;

	public static IndividualBlock makeBlockInstanceFromClassName(String fullClassName, byte [] data) throws Exception {
		if(fullClassName == null){
			throw new Exception("Unable to determine class of block with data = '" + BlockModelContext.convertToHex(data) + "'");
		}else{
			try {
				Class<?> clazz = Class.forName(fullClassName);
				Constructor<?> ctor = clazz.getConstructor(new Class<?>[] { byte[].class });
				return (IndividualBlock)ctor.newInstance(data);
			} catch (java.lang.NoSuchMethodException e) {
				throw new Exception("java.lang.NoSuchMethodException");
			} catch (java.lang.ClassNotFoundException e) {
				throw new Exception("java.lang.ClassNotFoundException: " + fullClassName);
			} catch (InstantiationException e) {
				throw new Exception("InstantiationException");
			} catch (IllegalAccessException e) {
				throw new Exception("IllegalAccessException");
			} catch (IllegalArgumentException e) {
				throw new Exception("IllegalArgumentException");
			} catch (InvocationTargetException e) {
				throw new Exception("InvocationTargetException");
			} catch (Exception e) {
				throw new Exception("Some other exception.");
			}
		}
	}

	public boolean equals(IndividualBlock b) throws Exception{
		if(this.getBlockData() == null){
			return b != null && b.getBlockData() == null;
		}else{
			return b != null && Arrays.equals(this.getBlockData(), b.getBlockData());
		}
	}
}
