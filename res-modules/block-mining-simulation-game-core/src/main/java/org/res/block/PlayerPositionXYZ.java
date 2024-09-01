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

public class PlayerPositionXYZ extends IndividualBlock {

	protected Coordinate position;
	protected final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
	protected String playerUUID;

	public PlayerPositionXYZ(String playerUUID, Long x, Long y, Long z) {
		this.playerUUID = playerUUID;
		List<Long> l = new ArrayList<Long>();
		l.add(x);
		l.add(y);
		l.add(z);
		l.add(0L); //  4th dimension is always zero.
		this.position = new Coordinate(l);
	}

	public String getPlayerUUID(){
		return this.playerUUID;
	}

	public void initializeFromJson(String json) {
		JsonElement playerPositionElement = new Gson().fromJson(json, JsonElement.class);
		JsonObject playerPositionObject = (JsonObject)playerPositionElement;

		List<Long> l = new ArrayList<Long>();
		l.add(playerPositionObject.get("x").getAsLong());
		l.add(playerPositionObject.get("y").getAsLong());
		l.add(playerPositionObject.get("z").getAsLong());
		l.add(0L); //  4th dimension is always zero.
		this.position = new Coordinate(l);
		this.playerUUID = playerPositionObject.get("player_uuid").getAsString();
	}

	public PlayerPositionXYZ(String json) throws Exception {
		this.initializeFromJson(json);
	}

	public PlayerPositionXYZ(byte [] data) throws Exception {
		this.initializeFromJson(new String(data, "UTF-8"));
	}

	public Coordinate getPosition(){
		return this.position;
	}

	public JsonElement asJsonElement() throws Exception{
		JsonObject o = new JsonObject();
		o.add("x", new JsonPrimitive(this.position.getValueAtIndex(0L)));
		o.add("y", new JsonPrimitive(this.position.getValueAtIndex(1L)));
		o.add("z", new JsonPrimitive(this.position.getValueAtIndex(2L)));
		o.add("player_uuid", new JsonPrimitive(this.playerUUID));
		return o;
	}

	public String asJsonString() throws Exception {
		return gson.toJson(this.asJsonElement());
	}

	public byte [] getBlockData()throws Exception {
		return this.asJsonString().getBytes("UTF-8");
	}

	public boolean isMineable() throws Exception{
		return false;
	}
}
