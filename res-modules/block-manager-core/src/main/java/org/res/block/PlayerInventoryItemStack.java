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
import java.util.Base64;

public class PlayerInventoryItemStack {

	protected final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
	protected Long quantity;
	protected byte [] blockData;

	public PlayerInventoryItemStack(byte [] blockData, Long initialQuantity) {
		this.blockData = blockData;
		this.quantity = initialQuantity;
	}

	public PlayerInventoryItemStack(JsonElement itemStackElement) {
		JsonObject itemStackObject = (JsonObject)itemStackElement;

		this.quantity = itemStackObject.get("quantity").getAsLong();
		this.blockData = Base64.getDecoder().decode(itemStackObject.get("block_data_base_64").getAsString());
	}

	public JsonElement asJsonElement() throws Exception{
		JsonObject o = new JsonObject();
		o.add("quantity", new JsonPrimitive(this.quantity));
		o.add("block_data_base_64", new JsonPrimitive(Base64.getEncoder().encodeToString(this.blockData)));
		return o;
	}

	public IndividualBlock getBlock(BlockSchema blockSchema) throws Exception {
		String blockClassName = blockSchema.getFirstBlockMatchDescriptionForByteArray(this.blockData);
		return IndividualBlock.makeBlockInstanceFromClassName(blockClassName, this.blockData);
	}

	public void addQuantity(Long quantity){
		this.quantity += quantity;
	}

	public String asJsonString() throws Exception {
		return gson.toJson(this.asJsonElement());
	}

	public Long getQuantity() throws Exception {
		return this.quantity;
	}

	public byte [] getBlockData() throws Exception {
		return this.blockData;
	}
}
