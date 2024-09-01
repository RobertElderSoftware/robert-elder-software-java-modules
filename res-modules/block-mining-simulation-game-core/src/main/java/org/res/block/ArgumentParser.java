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
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

class ArgumentParser {

	private static List<ArgumentDescription> possibleArguments = Arrays.asList(
		new ArgumentDescription("--help", 0, "Display this help menu."),
		new ArgumentDescription("--debug-arguments", 0, "Echo back info about the value of command line argument values were parsed, and what the default values are."),
		new ArgumentDescription("--log-file", 1, "The name of the log file to use.  If not provided, there will be no logging."),
		new ArgumentDescription("--disable-jni", 0, "Disable the use of JNI (may cause some events like to window size changes to be ignored)."),
		new ArgumentDescription("--restricted-graphics", 0, "Use only the simplest ASCII characters to produce graphics.  Required when running on non-graphical display ttys."),
		new ArgumentDescription("--allow-unrecognized-block-types", 0, "Allow the game to run even when there are block types that aren't supported in the block schema."),
		new ArgumentDescription("--block-world-file", 1, "The name of the sqlite database file (SQLITE only)."),
		new ArgumentDescription("--block-schema-file", 1, "If specified, ignore the default built-in block schema and uses the one provided at file/path."),
		new ArgumentDescription("--print-block-schema", 0, "Print current block schema and exit."),
		new ArgumentDescription("--database-subprotocol", 1, "The protocol for the database connection string.  Currently supports 'postgresql' and 'sqlite'."),
		new ArgumentDescription("--database-hostname", 1, "The 'hostname' for the database connection. Can be IP address or DNS name."),
		new ArgumentDescription("--database-port", 1, "The port for the database connection."),
		new ArgumentDescription("--database-name", 1, "The 'name' of the database to connect to for the database connection string."),
		new ArgumentDescription("--database-username", 1, "The username for the database connection."),
		new ArgumentDescription("--database-password", 1, "The password for the database connection.")
	);

	public static void tryToAddParam(Map<String, List<String>> params, String key, List<String> value) throws Exception{
		if(params.containsKey(key)){
			throw new Exception("Duplicate Parameter: " + key + "='" + String.valueOf(params.get(key)) + "', and '" + String.valueOf(value) + "'");
		}else{
			params.put(key, value);
		}
	}

	public static void printHelpMenu(){
		System.out.println("");
		System.out.println("Block Mining Simulation Game - Available Command-line Arguments:");
		System.out.println("");
		for(ArgumentDescription possibleArg : possibleArguments){
			List<String> arglist = new ArrayList<String>();
			for(int i = 0; i < possibleArg.getLength(); i++){
				arglist.add("<arg>");
			}
			System.out.println(String.format("%-33s", possibleArg.getArgumentKey()) + " " + String.format("%-8s", String.join(", ", arglist)) + " - " + possibleArg.getExplanation());
		}
	}

	public static void printArgs(Map<String, List<String>> params, String argumentsType) throws Exception {
		System.out.println("");
		System.out.println("--BEGIN report of '" + argumentsType + "':--");
		for(Map.Entry<String, List<String>> e : params.entrySet()){
			String valuesPart = e.getValue() == null ? "null" : (e.getValue().size() == 0 ? "empty array" : String.join(", ", e.getValue()));
			System.out.println("Key:" + e.getKey() + ", values: " + valuesPart);
		}
		System.out.println("--END report of '" + argumentsType + "'--");
	}

	public static CommandLineArgumentCollection parseArguments(String[] args, Map<String, List<String>> defaultArgumentValues) throws Exception {
		Map<String, List<String>> params = new HashMap<String, List<String>>();

		for (int i = 0; i < args.length; ){
			boolean foundArgMatch = false;
			for (int argNum = 0; argNum < possibleArguments.size(); argNum++){
				ArgumentDescription currentArg = possibleArguments.get(argNum);
				//System.out.println("argNum=" + argNum + "=" + currentArg.getArgumentKey() + ",  possibleArguments.size()=" + possibleArguments.size());

				//  Support arguments of the form --arg 123 and --arg=123
				if (args[i].contains("=")){
					String[] argParts = args[i].split("=");
					if (argParts.length == 2){
						if(argParts[0].equals(currentArg.getArgumentKey())){
							if(currentArg.getLength() == 1){
								List<String> valuesForThisArg = new ArrayList<String>();
								valuesForThisArg.add(argParts[1]);
								ArgumentParser.tryToAddParam(params, argParts[0], valuesForThisArg);
								foundArgMatch = true;
							}else{
								throw new Exception("Argument specified using '=' format, but expected " + currentArg.getLength() + " parts. ");
							}
						}
					}else{
						throw new Exception("Unexpected number of parts in argument: " + argParts.length);
					}
				}else{
					//System.out.println("i=" + i + args[i] + " versus " + currentArg.getArgumentKey());
					if(args[i].equals(currentArg.getArgumentKey())){
						//System.out.println("-here0 for " + currentArg.getArgumentKey());
						if(currentArg.getLength() == 0){
							
							//System.out.println("-here1 for " + currentArg.getArgumentKey());
							ArgumentParser.tryToAddParam(params, currentArg.getArgumentKey(), new ArrayList<String>());
							foundArgMatch = true;
						}else{
							//System.out.println("-here2 for " + currentArg.getArgumentKey());
							if(i + currentArg.getLength() < args.length){
								List<String> valuesForThisArg = new ArrayList<String>();
								for(int l = 0; l < currentArg.getLength(); l++){
									String argValue = args[i+1+l];
									if(possibleArguments.contains(argValue)){ //  Is suppoused 'value' is next arg flag?
										throw new Exception("Missing argument value for " + currentArg.getArgumentKey());
									}else{
										valuesForThisArg.add(argValue);
									}
								}
								ArgumentParser.tryToAddParam(params, currentArg.getArgumentKey(), valuesForThisArg);
								i+= currentArg.getLength();
								foundArgMatch = true;
							}else{
								throw new Exception("End of arguments, missing value for " + currentArg.getArgumentKey());
							}
						}
					}
				}
			}
			if(!foundArgMatch){
				throw new Exception("Unrecognized argument: " + args[i]);
			}
			i++;
		}
		for(ArgumentDescription possibleArg : possibleArguments){
			if(!params.containsKey(possibleArg.getArgumentKey())){
				params.put(possibleArg.getArgumentKey(), null);
			}
		}
		return new CommandLineArgumentCollection(params, defaultArgumentValues);
	}
}
