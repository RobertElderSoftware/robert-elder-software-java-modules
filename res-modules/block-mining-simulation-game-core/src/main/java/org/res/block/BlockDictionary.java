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

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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

public class BlockDictionary extends IndividualBlock {

	protected final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
	private Map<String, Coordinate> dictionary = new HashMap<String, Coordinate>();

	public BlockDictionary() {

	}

	public final void initializeFromJson(String json) {
		JsonElement rootElement = new Gson().fromJson(json, JsonElement.class);
		JsonObject rootObject = (JsonObject)rootElement;
		this.dictionary = new HashMap<String, Coordinate>();

		for(Map.Entry<String, JsonElement> entry: rootObject.entrySet()){
			JsonObject coordinateJsonObject = (JsonObject)entry.getValue();
			List<Long> l = new ArrayList<Long>();
			Long n = 0L;
			while(true){
				String key = "x" + n;
				if(coordinateJsonObject.has(key)){
					l.add(coordinateJsonObject.get(key).getAsLong());
				}else{
					break; //  End of coordinate
				}
				n++;
			}
			this.dictionary.put(entry.getKey(), new Coordinate(l));
		}
	}

	public void put(String key, Coordinate value){
		this.dictionary.put(key, value.copy());
	}

	public Coordinate get(String key){
		return this.dictionary.get(key);
	}

	public BlockDictionary(String json) throws Exception {
		this.initializeFromJson(json);
	}

	public BlockDictionary(byte [] data) throws Exception {
		this.initializeFromJson(new String(data, "UTF-8"));
	}

	public Set<Map.Entry<String, Coordinate>> entrySet(){
		return this.dictionary.entrySet();
	}

	public JsonElement asJsonElement() throws Exception{
		JsonObject o = new JsonObject();
		for(Map.Entry<String, Coordinate> entry: this.dictionary.entrySet()){
			o.add(entry.getKey(), entry.getValue().asJsonElement());
		}
		return o;
	}

	public String asJsonString() throws Exception {
		return gson.toJson(this.asJsonElement());
	}

	public byte [] getBlockData() throws Exception {
		return this.asJsonString().getBytes("UTF-8");
	}

	public boolean isMineable() throws Exception{
		return false;
	}
}
