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


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonNull;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class BlockSchema {

	protected final Long version;
	protected final String json;
	protected final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
	protected final List<BlockMatchDescription> blockMatchDescriptions;
	protected final boolean allowUnrecognizedBlockTypes;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public BlockSchema(String json, boolean allowUnrecognizedBlockTypes) throws Exception {
		this.json = json;
		this.allowUnrecognizedBlockTypes = allowUnrecognizedBlockTypes;
		JsonElement topElement = new Gson().fromJson(json, JsonElement.class);
		JsonObject blockSchemaObject = (JsonObject)topElement;

		JsonArray blockMatchDescriptionsJson = (JsonArray)blockSchemaObject.get("block_match_descriptions");
		List<BlockMatchDescription> l = new ArrayList<BlockMatchDescription>();
		for(JsonElement e : blockMatchDescriptionsJson){
			JsonObject o = (JsonObject)e;
			if(o.get("match_type").getAsString().equals("json")){
				l.add(new JsonBlockMatchDescription(e));
			}else if(o.get("match_type").getAsString().equals("byte_comparison")){
				l.add(new ByteComparisonBlockMatchDescription(e));
			}else{
				throw new Exception("Unknown match type:" + o.get("match_type").getAsString());
			}
		}
		this.blockMatchDescriptions = l;
		this.version = blockSchemaObject.get("version").getAsLong();
	}

	public String getInputJsonBlockSchema(){
		return json;
	}

	public JsonElement asJsonElement() throws Exception{
		JsonObject o = new JsonObject();
		o.add("version", new JsonPrimitive(this.version));
		return o;
	}

	public String asJsonString() throws Exception {
		return gson.toJson(this.asJsonElement());
	}

	public List<BlockMatchDescription> getBlockMatchDescriptions(){
		return this.blockMatchDescriptions;
	}

	public String getFirstBlockMatchDescriptionForByteArray(byte [] data) throws Exception{
		for(BlockMatchDescription bmd : this.blockMatchDescriptions){
			if(bmd.doesMatch(data)){
				logger.info("Block class name is: " + bmd.getBlockInstanceClassName());
				return bmd.getBlockInstanceClassName();
			}
		}
		logger.info("No match found...");
		if(this.allowUnrecognizedBlockTypes){
			return UnrecognizedBlock.class.getName();
		}else{
			return null;
		}
	}

	public byte [] getBinaryDataForByteComparisonBlockForClass(Class<?> c) throws Exception{
		for(BlockMatchDescription bmd : this.blockMatchDescriptions){
			if(bmd instanceof ByteComparisonBlockMatchDescription){
				ByteComparisonBlockMatchDescription bcbmd = (ByteComparisonBlockMatchDescription)bmd;
				if(c.getName().equals(bmd.getBlockInstanceClassName())){
					return bcbmd.getBytePattern();
				}
			}
		}
		throw new Exception("Cannot get byte pattern for unknown class: " + c.getName());
	}
}
