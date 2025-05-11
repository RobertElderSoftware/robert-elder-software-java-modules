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

import java.io.BufferedInputStream;
import java.io.IOException;

import org.res.block.Coordinate;
import java.io.IOException;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.lang.Thread;

import java.util.concurrent.TimeUnit;

import org.res.block.ServerBlockModelContext;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.util.StatusPrinter;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import org.slf4j.LoggerFactory;

class CommandLineArgumentCollection {
	
	private Map<String, List<String>> parsedCommandlineArguments;
	private Map<String, List<String>> defaultArgumentValues;

	public CommandLineArgumentCollection(Map<String, List<String>> parsedCommandlineArguments, Map<String, List<String>> defaultArgumentValues) throws Exception {
		this.parsedCommandlineArguments = parsedCommandlineArguments;
		this.defaultArgumentValues = defaultArgumentValues;
	}

	public String getUsedSingleValue(String key) throws Exception{
		if(parsedCommandlineArguments.containsKey(key)){
			List<String> l = parsedCommandlineArguments.get(key);
			if(l == null){
				//Coninue and check defaults.
			}else{
				if(l.size() == 1){
					return l.get(0);
				}else{
					throw new Exception("List has wrong size: " + l.size());
				}
			}
		}

		if(defaultArgumentValues.containsKey(key)){
			List<String> l = defaultArgumentValues.get(key);
			if(l == null){
				//Coninue...
			}else{
				if(l.size() == 1){
					return l.get(0);
				}else{
					throw new Exception("List has wrong size: " + l.size());
				}
			}
		}

		return null;
	}

	public boolean hasUsedKey(String key) {
		return (
			(parsedCommandlineArguments.containsKey(key) && parsedCommandlineArguments.get(key) != null) ||
			(defaultArgumentValues.containsKey(key) && defaultArgumentValues.get(key) != null)
		);
	}

	public void printHelpMenu(boolean printDebugInfo) throws Exception {
		ArgumentParser.printHelpMenu();
		if(printDebugInfo){
			System.out.println("");
			System.out.println("Debugging information follows...");
			System.out.println("");
			ArgumentParser.printArgs(this.parsedCommandlineArguments, "Arguments that were parsed in from the command-line");
			System.out.println("");
			ArgumentParser.printArgs(this.defaultArgumentValues, "Internal default arguments");
		}
	}
}
