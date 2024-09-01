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
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
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

public class BlockSkins {
	private static final Map<String, String> patterns;
	static {
		Map<String, String> tmp = new HashMap<String, String>();
		tmp.put(Rock.class.getName(), "\uD83E\uDEA8");
		tmp.put(WoodenPick.class.getName(),"\u26CF\uFE0F");
		tmp.put(StonePick.class.getName(),"\u26CF\uFE0F");
		tmp.put(IronPick.class.getName(),"\u26CF\uFE0F");
		tmp.put(TitaniumDioxide.class.getName(),"\u2B1C");
		tmp.put(Ilmenite.class.getName(),"\u2B1B");
		tmp.put(Rock.class.getName(),"\uD83E\uDEA8");
		tmp.put(Bauxite.class.getName(),"\uD83D\uDFEB");
		tmp.put(IronOxide.class.getName(),"\ud83d\udfe5");
		tmp.put(MetallicIron.class.getName(),"\u2699\uFE0F");
		tmp.put(MetallicCopper.class.getName(),"\uD83D\uDFE7");
		tmp.put(Pyrite.class.getName(),"\uD83D\uDFE8");
		tmp.put(WoodenBlock.class.getName(),"\uD83E\uDEB5");
		tmp.put(UnrecognizedBlock.class.getName(),"\uD83D\uDEAB");
		tmp.put(EmptyBlock.class.getName(),"");
		tmp.put(UninitializedBlock.class.getName(),"U");
		tmp.put(PlayerPositionXYZ.class.getName(),"P");
		tmp.put(PlayerInventory.class.getName(),"!");
		patterns = Collections.unmodifiableMap(tmp);
	}


	public static String getPresentation(Class<?> c, boolean restrictedGraphics) throws Exception{
		if(patterns.containsKey(c.getName())){
			if(restrictedGraphics){
				if(c.getName().equals(EmptyBlock.class.getName())){
					return "";
				}else{
					return c.getSimpleName().substring(0, 1);
				}
			}else{
				return patterns.get(c.getName());
			}
		}else{
			throw new Exception("Did not find an entry for " + c.getName());
		}
	}
}
