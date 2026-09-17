//  Copyright (c) 2026 Robert Elder Software Inc.
//   
//  Robert Elder Software Proprietary License (Version 2026-04-09)
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
//  either unmodified, modified, or incorporated into another software product, 
//  except as described in the document "REDISTRIBUTION.md" (a file with SHA256 
//  hash value 'c39a6c8200a22caf30eac97095b78def80c9cab1b6f7ddd3fca7fdae71df43da').
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
	private static final Map<String, String> presentationPatterns;
	private static final Map<String, String> descriptions;
	static {
		Map<String, String> presentationPatternsTmp = new HashMap<String, String>();
		presentationPatternsTmp.put(UnrecognizedBlock.class.getName(),"\uD83D\uDEAB");
		presentationPatternsTmp.put(EmptyBlock.class.getName(),"");
		presentationPatternsTmp.put(PendingLoadBlock.class.getName(),"?");
		presentationPatternsTmp.put(UninitializedBlock.class.getName(),"U");
		presentationPatternsTmp.put(PlayerPositionXYZ.class.getName(),"P");
		presentationPatternsTmp.put(PlayerInventory.class.getName(),"!");
		presentationPatternsTmp.put(WoodenBlock.class.getName(),"\uD83E\uDEB5");
		presentationPatternsTmp.put(WoodenPick.class.getName(),"\u26CF\uFE0F");
		presentationPatternsTmp.put(StonePick.class.getName(),"\u26CF\uFE0F");
		presentationPatternsTmp.put(IronPick.class.getName(),"\u26CF\uFE0F");
		presentationPatternsTmp.put(Rock.class.getName(),"\uD83E\uDEA8");
		presentationPatternsTmp.put(IronOxide.class.getName(),"\uD83D\uDFE5");
		presentationPatternsTmp.put(Hematite.class.getName(),"\uD83D\uDFE5");
		presentationPatternsTmp.put(MetallicIron.class.getName(),"\u2699\uFE0F");
		presentationPatternsTmp.put(MetallicCopper.class.getName(),"\uD83D\uDFE7");
		presentationPatternsTmp.put(Bauxite.class.getName(),"\uD83D\uDFEB");
		presentationPatternsTmp.put(Ilmenite.class.getName(),"\u2B1B");
		presentationPatternsTmp.put(Pyrite.class.getName(),"\uD83D\uDFE8");
		presentationPatternsTmp.put(TitaniumDioxide.class.getName(),"\u2B1C");
		presentationPatternsTmp.put(MetallicTitanium.class.getName(),"\u2B1C");
		presentationPatternsTmp.put(SiliconDioxide.class.getName(),"\u2B1C");
		presentationPatternsTmp.put(Chrysoberyl.class.getName(),"\uD83D\uDC8E");
		presentationPatternsTmp.put(MetallicSilver.class.getName(),"\u2B1C");
		presentationPatternsTmp.put(Malachite.class.getName(),"\uD83D\uDFE9");
		presentationPatternsTmp.put(Taconite.class.getName(),"\u2B1B");
		presentationPatternsTmp.put(Goethite.class.getName(),"\uD83D\uDFEB");
		presentationPatternsTmp.put(Magnetite.class.getName(),"\u2B1B");
		presentationPatternsTmp.put(Limonite.class.getName(),"\uD83D\uDFE7");
		presentationPatternsTmp.put(Siderite.class.getName(),"\uD83D\uDFE8");
		presentationPatternsTmp.put(Wuestite.class.getName(),"\u2B1B");
		presentationPatternsTmp.put(Kaolin.class.getName(),"\u26AA");
		presentationPatternsTmp.put(CalcinedAnthracite.class.getName(),"\u26AB");
		presentationPatternsTmp.put(Smectite.class.getName(),"\u26AA");
		presentationPatternsTmp.put(Montmorillonite.class.getName(),"\u26AA");
		presentationPatternsTmp.put(Attapulgite.class.getName(),"\u26AA");
		presentationPatternsTmp.put(Azurite.class.getName(),"\uD83D\uDFE6");
		presentationPatternsTmp.put(Cuprite.class.getName(),"\uD83D\uDFE5");
		presentationPatternsTmp.put(Bornite.class.getName(),"\uD83D\uDFE8");
		presentationPatternsTmp.put(Dioptase.class.getName(),"\uD83D\uDFE9");
		presentationPatternsTmp.put(Chalcopyrite.class.getName(),"\uD83D\uDFE8");
		presentationPatternsTmp.put(Covellite.class.getName(),"\u2B1B");
		presentationPatternsTmp.put(Tenorite.class.getName(),"\u2B1B");
		presentationPatternsTmp.put(Chalcocite.class.getName(),"\u2B1B");
		presentationPatterns = Collections.unmodifiableMap(presentationPatternsTmp);

		Map<String, String> descriptionsTmp = new HashMap<String, String>();
		descriptionsTmp.put(UnrecognizedBlock.class.getName(),"An unrecognized block that is not found existing block schema.");
		descriptionsTmp.put(EmptyBlock.class.getName(),"An empty block that represents a region of the world that contains nothing.");
		descriptionsTmp.put(PendingLoadBlock.class.getName(),"Pending Load block.");
		descriptionsTmp.put(UninitializedBlock.class.getName(),"Uninitialized block.");
		descriptionsTmp.put(PlayerPositionXYZ.class.getName(),"Player position block.");
		descriptionsTmp.put(PlayerInventory.class.getName(),"Player inventory block.");
		descriptionsTmp.put(WoodenBlock.class.getName(),"One cubic meter of wooden block.");
		descriptionsTmp.put(WoodenPick.class.getName(),"A wooden pick axe.  Capable of mining all blocks within a radius of 1 from the player.");
		descriptionsTmp.put(StonePick.class.getName(),"A stone pick axe.  Capable of mining all blocks within a radius of 2 from the player.");
		descriptionsTmp.put(IronPick.class.getName(),"An iron pick axe.  Capable of mining all blocks within a radius of 3 from the player.");
		descriptionsTmp.put(Rock.class.getName(),"One cubic meter of Rock, a mineral with chemical formula CaCO₃.");
		descriptionsTmp.put(IronOxide.class.getName(),"One cubic meter of IronOxide, a mineral with chemical formula Fe₂O₃.");
		descriptionsTmp.put(Hematite.class.getName(),"One cubic meter of Hematite, a mineral with chemical formula Fe₂O₃.");
		descriptionsTmp.put(MetallicIron.class.getName(),"One cubic meter of MetallicIron, a mineral with chemical formula Fe.");
		descriptionsTmp.put(MetallicCopper.class.getName(),"One cubic meter of MetallicCopper, a mineral with chemical formula Cu.");
		descriptionsTmp.put(Bauxite.class.getName(),"One cubic meter of Bauxite, a mineral with chemical formula Al(OH)₃.");
		descriptionsTmp.put(Ilmenite.class.getName(),"One cubic meter of Ilmenite, a mineral with chemical formula FeTiO₃.");
		descriptionsTmp.put(Pyrite.class.getName(),"One cubic meter of Pyrite, a mineral with chemical formula FeS₂.");
		descriptionsTmp.put(TitaniumDioxide.class.getName(),"One cubic meter of TitaniumDioxide, a mineral with chemical formula TiO₂.");
		descriptionsTmp.put(MetallicTitanium.class.getName(),"One cubic meter of MetallicTitanium, a mineral with chemical formula Ti.");
		descriptionsTmp.put(SiliconDioxide.class.getName(),"One cubic meter of SiliconDioxide, a mineral with chemical formula SiO₂.");
		descriptionsTmp.put(Chrysoberyl.class.getName(),"One cubic meter of Chrysoberyl, a mineral with chemical formula BeAl₂O₄.");
		descriptionsTmp.put(MetallicSilver.class.getName(),"One cubic meter of MetallicSilver, a mineral with chemical formula Ag.");
		descriptionsTmp.put(Malachite.class.getName(),"One cubic meter of Malachite, a mineral with chemical formula Cu₂CO₃(OH)₂.");
		descriptionsTmp.put(Taconite.class.getName(),"One cubic meter of Taconite, a mineral with chemical formula Fe₃O₄.");
		descriptionsTmp.put(Goethite.class.getName(),"One cubic meter of Goethite, a mineral with chemical formula FeO(OH).");
		descriptionsTmp.put(Magnetite.class.getName(),"One cubic meter of Magnetite, a mineral with chemical formula Fe₃O₄.");
		descriptionsTmp.put(Limonite.class.getName(),"One cubic meter of Limonite, a mineral with chemical formula FeO(OH)·nH₂O.");
		descriptionsTmp.put(Siderite.class.getName(),"One cubic meter of Siderite, a mineral with chemical formula FeCO₃.");
		descriptionsTmp.put(Wuestite.class.getName(),"One cubic meter of Wuestite, a mineral with chemical formula FeO.");
		descriptionsTmp.put(Kaolin.class.getName(),"One cubic meter of Kaolin, a mineral with chemical formula Al₂Si₂O₅(OH)₄.");
		descriptionsTmp.put(CalcinedAnthracite.class.getName(),"One cubic meter of CalcinedAnthracite, a mineral with chemical formula C.");
		descriptionsTmp.put(Smectite.class.getName(),"One cubic meter of Smectite.");
		descriptionsTmp.put(Montmorillonite.class.getName(),"One cubic meter of Montmorillonite.");
		descriptionsTmp.put(Attapulgite.class.getName(),"One cubic meter of Attapulgite.");
		descriptionsTmp.put(Azurite.class.getName(),"One cubic meter of Azurite, a mineral with chemical formula Cu₃(CO₃)₂(OH)₂.");
		descriptionsTmp.put(Cuprite.class.getName(),"One cubic meter of Cuprite, a mineral with chemical formula Cu₂O.");
		descriptionsTmp.put(Bornite.class.getName(),"One cubic meter of Bornite, a mineral with chemical formula Cu₅FeS₄.");
		descriptionsTmp.put(Dioptase.class.getName(),"One cubic meter of Dioptase, a mineral with chemical formula CuSiO₃·H₂O.");
		descriptionsTmp.put(Chalcopyrite.class.getName(),"One cubic meter of Chalcopyrite, a mineral with chemical formula CuFeS₂.");
		descriptionsTmp.put(Covellite.class.getName(),"One cubic meter of Covellite, a mineral with chemical formula CuS.");
		descriptionsTmp.put(Tenorite.class.getName(),"One cubic meter of Tenorite, a mineral with chemical formula CuO.");
		descriptionsTmp.put(Chalcocite.class.getName(),"One cubic meter of Chalcocite, a mineral with chemical formula Cu₂S.");
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
		if(presentationPatterns.containsKey(c.getName())){
			if(useASCII){
				if(c.getName().equals(EmptyBlock.class.getName())){
					return "";
				}else{
					return c.getSimpleName().substring(0, 1);
				}
			}else{
				return presentationPatterns.get(c.getName());
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
