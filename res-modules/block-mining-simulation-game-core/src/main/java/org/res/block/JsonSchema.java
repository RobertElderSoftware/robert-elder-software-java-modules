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

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
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

public class JsonSchema{
	private String type;
	private Map<String, JsonSchema> properties;
	private List<String> required;
	private JsonSchema arrayItemsSchema = null;
	protected final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public JsonSchema(JsonElement e) {
		JsonObject o = (JsonObject)e;
		this.type = o.get("type").getAsString();
		if(o.has("items")){
			this.arrayItemsSchema = new JsonSchema(o.get("items"));
		}
		if(o.has("properties")){
			Map<String, JsonSchema> m = new HashMap<String, JsonSchema>();
			for(Map.Entry<String, JsonElement> oneProperty: ((JsonObject)o.get("properties")).entrySet()){
				m.put(oneProperty.getKey(), new JsonSchema(oneProperty.getValue()));
			}
			this.properties = m;
		}

		if(o.has("required")){
			List<String> l = new ArrayList<String>();
			for(JsonElement r : (JsonArray)o.get("required")){
				l.add(((JsonPrimitive)r).getAsString());
			}
			this.required = l;
		}
	}

	public JsonElement asJsonElement() throws Exception{
		JsonObject o = new JsonObject();
		return o;
	}

	public String asJsonString() throws Exception {
		return gson.toJson(this.asJsonElement());
	}

	public boolean doesMatchElement(JsonElement e) throws Exception {
		if(e == null){
			return false;
		}
		if(e.isJsonObject()){
			if(this.type.equals("object")){
				JsonObject o = (JsonObject)e;
				Set<String> observedKeys = new HashSet<String>();
				for(Map.Entry<String, JsonElement> p: o.entrySet()){
					String propertyKey = p.getKey();
					observedKeys.add(propertyKey);
					if(this.properties.containsKey(propertyKey)){
						JsonSchema propertySchema = this.properties.get(propertyKey);
						if(!propertySchema.doesMatchElement(p.getValue())){
							return false;
						}
					}else{
						logger.info("Saw a property " + propertyKey + " that was not supposed to be there.");
						return false;
					}
				}
				for(String requiredProperty : this.required){
					if(!observedKeys.contains(requiredProperty)){
						logger.info("Requied property " + requiredProperty + " not seen.");
						return false;
					}
				}
				return true;
			}else{
				logger.info("Excpected type to be " + this.type + ", but it was JSON Object.");
				return false;
			}
		}else if(e.isJsonArray()){
			if(this.type.equals("array")){
				JsonArray arrayItems = (JsonArray)e;
				for(int i = 0; i < arrayItems.size(); i++){
					JsonElement arrayItem = arrayItems.get(i);
					if(!this.arrayItemsSchema.doesMatchElement(arrayItem)){
						return false;
					}
				}
				return true;
			}else{
				logger.info("Excpected type to be " + this.type + ", but it was JSON Array.");
				return false;
			}
		}else if(e.isJsonPrimitive()){
			JsonPrimitive p = (JsonPrimitive)e;
			if(p.isBoolean()){
				if(this.type.equals("boolean")){
					return true;
				}else{
					logger.info("Excpected type to be " + this.type + ", but it was boolean.");
					return false;
				}
			}else if(p.isNumber()){
				if(this.type.equals("number")){
					return true;
				}else{
					logger.info("Excpected type to be " + this.type + ", but it was number.");
					return false;
				}
			}else if(p.isString()){
				if(this.type.equals("string")){
					return true;
				}else{
					logger.info("Excpected type to be " + this.type + ", but it was string.");
					return false;
				}
			}else{
				logger.info("Excpected type to be " + this.type + ", but it was something else.");
				return false;
			}
		}else if(e.isJsonNull()){
			if(this.type.equals("null")){
				return true;
			}else{
				logger.info("Excpected type to be " + this.type + ", but it was JSON Null.");
				return false;
			}
		}else{
			logger.info("JsonElement was something other than object, array or primitive?");
			return false;
		}
	}
}
