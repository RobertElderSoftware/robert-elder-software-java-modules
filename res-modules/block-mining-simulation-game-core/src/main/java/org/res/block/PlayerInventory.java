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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

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

public class PlayerInventory extends IndividualBlock {

	protected final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
	protected List<PlayerInventoryItemStack> inventoryItemList = new ArrayList<PlayerInventoryItemStack>();
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public PlayerInventory() {
	}

	public PlayerInventory(String json) {
		this.initializeFromJson(json);
	}

	public final void initializeFromJson(String json) {
		JsonElement inventoryItemListElement = new Gson().fromJson(json, JsonElement.class);
		JsonArray inventoryItemListArray = (JsonArray)inventoryItemListElement;

		for(JsonElement e : inventoryItemListArray){
			this.inventoryItemList.add(new PlayerInventoryItemStack(e));
		}
	}

	public PlayerInventory(byte [] data) throws Exception {
		this.initializeFromJson(new String(data, "UTF-8"));
	}

	public int addItemCountToInventory(byte [] blockData, Long quantity) throws Exception {
		boolean addedItem = false;
		int i = 0;
		for(i = 0; i < this.inventoryItemList.size(); i++){
			PlayerInventoryItemStack itemStack = this.inventoryItemList.get(i);
			if(Arrays.equals(itemStack.getBlockData(), blockData)){
				//logger.info("in was equals");
				itemStack.addQuantity(quantity);
				addedItem = true;
				return i;
			}
		}
		if(!addedItem){
			//logger.info("made a new item");
			this.inventoryItemList.add(new PlayerInventoryItemStack(blockData.clone(), quantity));
		}
		return i;
	}

	public boolean containsBlockCount(byte [] blockData, Long expectedQuantity) throws Exception {
		for(PlayerInventoryItemStack itemStack : this.inventoryItemList){
			if(Arrays.equals(itemStack.getBlockData(), blockData)){
				if(itemStack.getQuantity() >= expectedQuantity){
					return true;
				}
			}
		}
		return false;
	}

	public List<PlayerInventoryItemStack> getInventoryItemStackList(){
		return this.inventoryItemList;
	}

	public JsonElement asJsonElement() throws Exception{
		JsonArray stacks = new JsonArray();
		for(PlayerInventoryItemStack stack : this.inventoryItemList){
			stacks.add(stack.asJsonElement());
		}
		return stacks;
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
