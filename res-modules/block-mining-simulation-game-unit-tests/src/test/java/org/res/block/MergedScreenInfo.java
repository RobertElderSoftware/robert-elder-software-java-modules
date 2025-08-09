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
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class MergedScreenInfo{

	private ScreenLayer [] allLayers;
	//  Map of coordinate (in respective layer coordinates) to the
        //  original randomly generated characters in that layer:
	private List<Map<Coordinate, TestScreenCharacter>> layerRelativeCharacters;
	//  Map of characters relative to bottom layer:
	private List<Map<Coordinate, TestScreenCharacter>> bottomRelativeCharacters;
	//  Map of coordinate (in bottom layer coordinates) to layer number
        //  with solid column at that coordinate in final merged layer.
	private Map<Coordinate, Integer> topmostSolidColumnLayers;

	public MergedScreenInfo(ScreenLayer [] allLayers, List<Map<Coordinate, TestScreenCharacter>> layerRelativeCharacters){
		this.allLayers = allLayers;
		this.layerRelativeCharacters = layerRelativeCharacters;
	}

	public void init() throws Exception{
		this.bottomRelativeCharacters = this.calculateBottomRelativeCharacters();
		this.topmostSolidColumnLayers = this.calculateTopmostSolidColumnLayers();
	}

	public List<Map<Coordinate, TestScreenCharacter>> getBottomRelativeCharacters() throws Exception{
		return this.bottomRelativeCharacters;
	}

	public Map<Coordinate, Integer> getTopmostSolidColumnLayers() throws Exception{
		return this.topmostSolidColumnLayers;
	}

	private List<Map<Coordinate, TestScreenCharacter>> calculateBottomRelativeCharacters() throws Exception{
		List<Map<Coordinate, TestScreenCharacter>> rtn = new ArrayList<Map<Coordinate, TestScreenCharacter>>();
		for(int s = 0; s < allLayers.length; s++){
			rtn.add(new TreeMap<Coordinate, TestScreenCharacter>());
			for(int x = 0; x < allLayers[0].getWidth(); x++){
				for(int y = 0; y < allLayers[0].getHeight(); y++){
					Coordinate bottomLayerCoordinate = new Coordinate(Arrays.asList((long)x, (long)y));

					// Placement offset of bottom layer is ignored since placements need to be relative to bottom layer:
					Long xPlacementOffset = s == 0L ? 0L : -allLayers[s].getPlacementOffset().getX();
					Long yPlacementOffset = s == 0L ? 0L : -allLayers[s].getPlacementOffset().getY();
					Coordinate layerLocalCoordinate = bottomLayerCoordinate.changeByDeltaXY(
						xPlacementOffset,
						yPlacementOffset
					);
					if(
						layerRelativeCharacters.get(s).containsKey(layerLocalCoordinate) &&
						layerRelativeCharacters.get(s).get(layerLocalCoordinate).active &&
						allLayers[s].getIsLayerActive()
					){
						TestScreenCharacter c = layerRelativeCharacters.get(s).get(layerLocalCoordinate);
						rtn.get(s).put(bottomLayerCoordinate, c);
					}else{
						//  If there is no corresponding coordinate here, there will just be a gap.
					}
				}
			}
		}
		return rtn;
	}

	private Map<Coordinate, Integer> calculateTopmostSolidColumnLayers() throws Exception{
		Map<Coordinate, Integer> rtn = new HashMap<Coordinate, Integer>();
		for(int x = 0; x < allLayers[0].getWidth(); x++){
			for(int y = 0; y < allLayers[0].getHeight(); y++){
				Coordinate bottomLayerCoordinate = new Coordinate(Arrays.asList((long)x, (long)y));

				TestScreenCharacter topCharacter = null;
				int topMostLayer = allLayers.length -1;
				while(topMostLayer >= 0){
					if(
						bottomRelativeCharacters.get(topMostLayer).containsKey(bottomLayerCoordinate) &&
						bottomRelativeCharacters.get(topMostLayer).get(bottomLayerCoordinate).active &&
						allLayers[topMostLayer].getIsLayerActive()
					){
						topCharacter = bottomRelativeCharacters.get(topMostLayer).get(bottomLayerCoordinate);
						// TODO:  Backtrack to look for start of character in multi-column case.
						if(topCharacter.characters != null){
							rtn.put(bottomLayerCoordinate, topMostLayer);
							break;
						}
					}
					topMostLayer--;
					if(topMostLayer < 0){
						//  No solid character:
						rtn.put(bottomLayerCoordinate, null);
					}
				}
			}
		}
		return rtn;
	}
}
