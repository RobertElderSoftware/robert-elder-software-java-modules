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
	//  Map that indicates whether there is *any* active column with a changed
	//  flag set that's located above the topmost solid character:
	private Map<Coordinate, Boolean> hasAboveActiveChangedFlags;
	//  Map that indicates if there is any solid character with an active flag.
	private Map<Coordinate, Boolean> hasActiveFlags;
	private Map<Coordinate, int []> topColourCodes;

	//  Save a copy of bottom layer before merge:
	private Map<Coordinate, TestScreenCharacter> beforeMergeCharacters;

	public MergedScreenInfo(ScreenLayer [] allLayers, List<Map<Coordinate, TestScreenCharacter>> layerRelativeCharacters){
		this.allLayers = allLayers;
		this.layerRelativeCharacters = layerRelativeCharacters;
	}

	public void init() throws Exception{
		this.bottomRelativeCharacters = this.calculateBottomRelativeCharacters();
		this.topmostSolidColumnLayers = this.calculateTopmostSolidColumnLayers();
		this.hasAboveActiveChangedFlags = this.calculateHasAboveActiveChangedFlags();
		this.hasActiveFlags = this.calculateHasActiveFlags();
		this.topColourCodes = this.calculateTopColourCodes();
		this.beforeMergeCharacters = this.calculateBeforeMergeCharacters();
	}

	public Map<Coordinate, TestScreenCharacter> getBeforeMergeCharacters() throws Exception{
		return this.beforeMergeCharacters;
	}

	public List<Map<Coordinate, TestScreenCharacter>> getBottomRelativeCharacters() throws Exception{
		return this.bottomRelativeCharacters;
	}

	public Map<Coordinate, Integer> getTopmostSolidColumnLayers() throws Exception{
		return this.topmostSolidColumnLayers;
	}

	public Map<Coordinate, Boolean> getHasAboveActiveChangedFlags() throws Exception{
		return this.hasAboveActiveChangedFlags;
	}

	public Map<Coordinate, Boolean> getHasActiveFlags() throws Exception{
		return this.hasActiveFlags;
	}

	public Map<Coordinate, int []> getTopColourCodes() throws Exception{
		return this.topColourCodes;
	}

	public Map<Coordinate, TestScreenCharacter> calculateBeforeMergeCharacters(){
		Map<Coordinate, TestScreenCharacter> rtn = new HashMap<Coordinate, TestScreenCharacter>();
		//  Remember exactly what was in the bottom result later for later comparison
		for(int x = 0; x < allLayers[0].getWidth(); x++){
			for(int y = 0; y < allLayers[0].getHeight(); y++){
				Coordinate coordinate = new Coordinate(Arrays.asList((long)x, (long)y));
				rtn.put(
					coordinate,
					new TestScreenCharacter(
						allLayers[0].characters[x][y],
						allLayers[0].characterWidths[x][y],
						allLayers[0].colourCodes[x][y],
						allLayers[0].active[x][y],
						allLayers[0].changed[x][y]
					)
				);
			}
		}
		return rtn;
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
				int topMostLayer = allLayers.length -1;
				while(topMostLayer >= 0){
					if(
						bottomRelativeCharacters.get(topMostLayer).containsKey(bottomLayerCoordinate) &&
						bottomRelativeCharacters.get(topMostLayer).get(bottomLayerCoordinate).active &&
						allLayers[topMostLayer].getIsLayerActive()
					){
						TestScreenCharacter c = bottomRelativeCharacters.get(topMostLayer).get(bottomLayerCoordinate);
						// TODO:  Backtrack to look for start of character in multi-column case.
						if(c.characters != null){
							rtn.put(bottomLayerCoordinate, topMostLayer);
							break;
						}
					}
					topMostLayer--;
				}
				// Did not find any solid character
				if(topMostLayer < 0){
					//  No solid character:
					rtn.put(bottomLayerCoordinate, null);
				}
			}
		}
		return rtn;
	}

	private Map<Coordinate, Boolean> calculateHasAboveActiveChangedFlags() throws Exception{
		Map<Coordinate, Boolean> rtn = new HashMap<Coordinate, Boolean>();
		for(int x = 0; x < allLayers[0].getWidth(); x++){
			for(int y = 0; y < allLayers[0].getHeight(); y++){
				Coordinate bottomLayerCoordinate = new Coordinate(Arrays.asList((long)x, (long)y));

				int topMostLayer = allLayers.length -1;
				boolean foundTopSolidCharacter = false;
				boolean foundTopColourCodes = false;
				while(topMostLayer >= 0){
					if(
						bottomRelativeCharacters.get(topMostLayer).containsKey(bottomLayerCoordinate) &&
						bottomRelativeCharacters.get(topMostLayer).get(bottomLayerCoordinate).active &&
						allLayers[topMostLayer].getIsLayerActive()
					){
						TestScreenCharacter c = bottomRelativeCharacters.get(topMostLayer).get(bottomLayerCoordinate);
						if(c.changed && (!foundTopSolidCharacter)){
							//  This case can happen if an empty null character
							//  changes above a solid character column.
							rtn.put(bottomLayerCoordinate, true);
						}
						if(c.characters != null && c.changed && !foundTopSolidCharacter){
							//  Topmost visible character has changed
							rtn.put(bottomLayerCoordinate, true);
						}
						if(c.colourCodes.length > 0 && c.changed && !foundTopColourCodes){
							//  Topmost visible colour codes havechanged
							rtn.put(bottomLayerCoordinate, true);
						}
						if(c.colourCodes.length > 0){
							foundTopColourCodes = true;
						}
						if(c.characters != null){
							foundTopSolidCharacter = true;
						}
					}
					topMostLayer--;
				}
				if(!rtn.containsKey(bottomLayerCoordinate)){
					//  No solid character:
					rtn.put(bottomLayerCoordinate, false);
				}
			}
		}
		return rtn;
	}

	private Map<Coordinate, Boolean> calculateHasActiveFlags() throws Exception{
		Map<Coordinate, Boolean> rtn = new HashMap<Coordinate, Boolean>();
		for(int x = 0; x < allLayers[0].getWidth(); x++){
			for(int y = 0; y < allLayers[0].getHeight(); y++){
				Coordinate bottomLayerCoordinate = new Coordinate(Arrays.asList((long)x, (long)y));

				int topMostLayer = allLayers.length -1;
				while(topMostLayer >= 0){
					if(
						bottomRelativeCharacters.get(topMostLayer).containsKey(bottomLayerCoordinate) &&
						bottomRelativeCharacters.get(topMostLayer).get(bottomLayerCoordinate).active &&
						allLayers[topMostLayer].getIsLayerActive()
					){
						TestScreenCharacter c = bottomRelativeCharacters.get(topMostLayer).get(bottomLayerCoordinate);
						rtn.put(bottomLayerCoordinate, true);
						if(c.characters != null){
							break;
						}
					}
					topMostLayer--;
				}
				if(!rtn.containsKey(bottomLayerCoordinate)){
					//  No solid character:
					rtn.put(bottomLayerCoordinate, false);
				}
			}
		}
		return rtn;
	}

	private Map<Coordinate, int []> calculateTopColourCodes() throws Exception{
		Map<Coordinate, int []> rtn = new HashMap<Coordinate, int []>();
		for(int x = 0; x < allLayers[0].getWidth(); x++){
			for(int y = 0; y < allLayers[0].getHeight(); y++){
				Coordinate bottomLayerCoordinate = new Coordinate(Arrays.asList((long)x, (long)y));

				int topMostLayer = allLayers.length -1;
				while(topMostLayer >= 0){
					if(
						bottomRelativeCharacters.get(topMostLayer).containsKey(bottomLayerCoordinate) &&
						bottomRelativeCharacters.get(topMostLayer).get(bottomLayerCoordinate).active &&
						allLayers[topMostLayer].getIsLayerActive()
					){
						TestScreenCharacter c = bottomRelativeCharacters.get(topMostLayer).get(bottomLayerCoordinate);
						if(c.colourCodes.length > 0){
							rtn.put(bottomLayerCoordinate, c.colourCodes);
							break;
						}
					}
					topMostLayer--;
				}
				if(!rtn.containsKey(bottomLayerCoordinate)){
					//  No solid character:
					rtn.put(bottomLayerCoordinate, new int [] {});
				}
			}
		}
		return rtn;
	}
}
