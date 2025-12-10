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
	private static final Map<String, String> descriptions;
	static {
		Map<String, String> patternsTmp = new HashMap<String, String>();
		patternsTmp.put(Rock.class.getName(), "\uD83E\uDEA8");
		patternsTmp.put(WoodenPick.class.getName(),"\u26CF\uFE0F");
		patternsTmp.put(StonePick.class.getName(),"\u26CF\uFE0F");
		patternsTmp.put(IronPick.class.getName(),"\u26CF\uFE0F");
		patternsTmp.put(Malachite.class.getName(),"\uD83D\uDFE9");
		patternsTmp.put(TitaniumDioxide.class.getName(),"\u2B1C");
		patternsTmp.put(MetallicTitanium.class.getName(),"\u2B1C");
		patternsTmp.put(SiliconDioxide.class.getName(),"\u2B1C");
		patternsTmp.put(Ilmenite.class.getName(),"\u2B1B");
		patternsTmp.put(Taconite.class.getName(),"\u2B1B");
		patternsTmp.put(Rock.class.getName(),"\uD83E\uDEA8");
		patternsTmp.put(Bauxite.class.getName(),"\uD83D\uDFEB");
		patternsTmp.put(IronOxide.class.getName(),"\ud83d\udfe5");
		patternsTmp.put(Hematite.class.getName(),"\ud83d\udfe5");
		patternsTmp.put(Goethite.class.getName(),"\uD83D\uDFEB");
		patternsTmp.put(Limonite.class.getName(),"\uD83D\uDFE7");
		patternsTmp.put(Siderite.class.getName(),"\uD83D\uDFE8");
		patternsTmp.put(Magnetite.class.getName(),"\u2B1B");
		patternsTmp.put(MetallicIron.class.getName(),"\u2699\uFE0F");
		patternsTmp.put(MetallicCopper.class.getName(),"\uD83D\uDFE7");
		patternsTmp.put(MetallicSilver.class.getName(),"\u2B1C");
		patternsTmp.put(Chrysoberyl.class.getName(),"\uD83D\uDC8E");
		patternsTmp.put(Pyrite.class.getName(),"\uD83D\uDFE8");
		patternsTmp.put(WoodenBlock.class.getName(),"\uD83E\uDEB5");
		patternsTmp.put(UnrecognizedBlock.class.getName(),"\uD83D\uDEAB");
		patternsTmp.put(EmptyBlock.class.getName(),"");
		patternsTmp.put(PendingLoadBlock.class.getName(),"?");
		patternsTmp.put(UninitializedBlock.class.getName(),"U");
		patternsTmp.put(PlayerPositionXYZ.class.getName(),"P");
		patternsTmp.put(PlayerInventory.class.getName(),"!");
		patterns = Collections.unmodifiableMap(patternsTmp);

		Map<String, String> descriptionsTmp = new HashMap<String, String>();
		descriptionsTmp.put(WoodenPick.class.getName(),"A wooden pick axe.  Capable of mining all blocks within a radius of 1 from the player.");
		descriptionsTmp.put(StonePick.class.getName(),"A stone pick axe.  Capable of mining all blocks within a radius of 2 from the player.");
		descriptionsTmp.put(IronPick.class.getName(),"An iron pick axe.  Capable of mining all blocks within a radius of 3 from the player.");
		descriptionsTmp.put(Malachite.class.getName(),"One cubic meter of basic copper carbonate, chemical formula Cu₂CO₃(OH)₂.");
		descriptionsTmp.put(TitaniumDioxide.class.getName(),"One cubic meter of titanium dioxide, chemical formula TiO₂");
		descriptionsTmp.put(MetallicTitanium.class.getName(),"One cubic meter of metallic titanium, the element with atomic number 22.");
		descriptionsTmp.put(SiliconDioxide.class.getName(),"One cubic meter of silicon dioxide, chemical formula SiO₂");
		descriptionsTmp.put(Ilmenite.class.getName(),"One cubic meter of ilmenite, chemical formula FeTiO₃");
		descriptionsTmp.put(Taconite.class.getName(),"One cubic meter of the mineral taconite.");
		descriptionsTmp.put(Rock.class.getName(),"One cubic meter of limestone rocks (calcium carbonate), chemical formula CaCO₃");
		descriptionsTmp.put(Bauxite.class.getName(),"One cubic meter of bauxite (aluminum ore), chemical formula Al₂H₂O₄");
		descriptionsTmp.put(IronOxide.class.getName(),"One cubic meter of iron oxide, chemical formula Fe₂O₃");
		descriptionsTmp.put(Hematite.class.getName(),"One cubic meter of hematite, the natural mineral with chemical formula Fe₂O₃");
		descriptionsTmp.put(Goethite.class.getName(),"One cubic meter of goethite, the natural mineral with chemical formula FeO(OH)");
		descriptionsTmp.put(Magnetite.class.getName(),"One cubic meter of magnetite, the natural mineral with chemical formula Fe²⁺Fe₂³⁺O₄");
		descriptionsTmp.put(Limonite.class.getName(),"One cubic meter of limonite, the natural mineral with chemical formula of the form FeO(OH)nH₂O");
		descriptionsTmp.put(Siderite.class.getName(),"One cubic meter of siderite, the natural mineral with chemical formula of the form FeCO₃");

		descriptionsTmp.put(MetallicIron.class.getName(),"One cubic meter of metallic iron, the element with atomic number 26.");
		descriptionsTmp.put(MetallicCopper.class.getName(),"One cubic meter of metallic copper, the element with atomic number 29.");
		descriptionsTmp.put(MetallicSilver.class.getName(),"One cubic meter of metallic silver, the element with atomic number 47.");
		descriptionsTmp.put(Chrysoberyl.class.getName(),"One cubic meter of the mineral or gemstone chrysoberyl, an aluminate of beryllium with the formula BeAl₂O₄");
		descriptionsTmp.put(Pyrite.class.getName(),"One cubic meter of the mineral pyrite (also known as fool's gold).  Chemical formula FeS₂");
		descriptionsTmp.put(WoodenBlock.class.getName(),"One cubic meter of wooden block.");
		descriptionsTmp.put(UnrecognizedBlock.class.getName(),"An unrecognized block that is not found existing block schema.");
		descriptionsTmp.put(EmptyBlock.class.getName(),"An empty block that represents a region of the world that contains nothing.");
		descriptionsTmp.put(PendingLoadBlock.class.getName(),"Pending Load block.");
		descriptionsTmp.put(UninitializedBlock.class.getName(),"Uninitialized block.");
		descriptionsTmp.put(PlayerPositionXYZ.class.getName(),"Player position block.");
		descriptionsTmp.put(PlayerInventory.class.getName(),"Player inventory block.");
		descriptions = Collections.unmodifiableMap(descriptionsTmp);
	}

	public static String getBlockDescription(Class<?> c) throws Exception{
		if(descriptions.containsKey(c.getName())){
			return descriptions.get(c.getName());
		}else{
			throw new Exception("Did not find an entry for " + c.getName());
		}
	}

	public static String getPresentation(Class<?> c, boolean useASCII) throws Exception{
		if(patterns.containsKey(c.getName())){
			if(useASCII){
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

	public static String getPresentation(IndividualBlock b, boolean useASCII) throws Exception{
		if(b instanceof PlayerObject){
			if(useASCII){
				return "P";
			}else{
				PlayerObject o = (PlayerObject)b;
				switch(o.getPlayerSkinType()){
					case HAPPY_FACE:{
						return "\uD83D\uDE0A";
					}case ANGRY_FACE:{
						return "\uD83D\uDE20";
					}default:{
						throw new Exception("Unknown skin type.");
					}
				}
			}
		}else{
			return getPresentation(b.getClass(), useASCII);
		}
	}
}
