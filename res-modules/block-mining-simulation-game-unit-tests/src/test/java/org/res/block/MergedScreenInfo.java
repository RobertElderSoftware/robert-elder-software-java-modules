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
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;
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
	//  Keep track of which columns are cut off by the start or end of the base layer:
	private Map<Coordinate, Map<Integer, SolidColumnType>> isColumnSolidAndActiveStates;
	//  A character is 'occluded' if any column in the character is covered by some
	//  other column is an upper layer.
	private Map<Coordinate, Map<Integer, Boolean>> isColumnOccludedStates;
	//  Map of coordinate (in bottom layer coordinates) to layer number
        //  with solid column at that coordinate in final merged layer.
	private Map<Coordinate, Integer> topmostSolidActiveColumnLayers;
	//  Map that indicates whether there is *any* active column with a changed
	//  flag set that's located above the topmost solid character:
	private Map<Coordinate, Boolean> hasAboveActiveChangedFlags;
	//  Map that indicates if there is any solid character with an active flag.
	private Map<Coordinate, Boolean> hasActiveFlags;
	private Map<Coordinate, int []> topColourCodes;

	private Set<ScreenRegion> allActiveTranslatedClippedExpandedChangedRegions;
	private Set<ScreenRegion> allActiveTranslatedChangedRegions;

	//  Save a copy of bottom layer before merge:
	private Map<Coordinate, TestScreenCharacter> beforeMergeCharacters;
	private Boolean randomizedForcedBottomLayerState;

	public MergedScreenInfo(ScreenLayer [] allLayers, List<Map<Coordinate, TestScreenCharacter>> layerRelativeCharacters, boolean randomizedForcedBottomLayerState){
		this.allLayers = allLayers;
		this.layerRelativeCharacters = layerRelativeCharacters;
		this.randomizedForcedBottomLayerState = randomizedForcedBottomLayerState;
	}

	public void init() throws Exception{
		this.bottomRelativeCharacters = this.calculateBottomRelativeCharacters();
		this.isColumnSolidAndActiveStates = this.calculateIsColumnSolidAndActiveStates();
		this.isColumnOccludedStates = this.calculateIsColumnOccludedStates();
		this.topmostSolidActiveColumnLayers = this.calculateTopmostSolidActiveColumnLayers();
		this.hasAboveActiveChangedFlags = this.calculateHasAboveActiveChangedFlags();
		this.hasActiveFlags = this.calculateHasActiveFlags();
		this.topColourCodes = this.calculateTopColourCodes();
		this.beforeMergeCharacters = this.calculateBeforeMergeCharacters();
		this.allActiveTranslatedClippedExpandedChangedRegions = this.computeAllActiveTranslatedClippedExpandedChangedRegions();
		this.allActiveTranslatedChangedRegions = this.computeAllActiveTranslatedChangedRegions();
	}

	public Map<Coordinate, TestScreenCharacter> getBeforeMergeCharacters() throws Exception{
		return this.beforeMergeCharacters;
	}

	public Map<Coordinate, Map<Integer, SolidColumnType>> getIsColumnSolidAndActiveStates(){
		return this.isColumnSolidAndActiveStates;
	}

	public Map<Coordinate, Map<Integer, Boolean>> getIsColumnOccludedStates(){
		return this.isColumnOccludedStates;
	}

	public List<Map<Coordinate, TestScreenCharacter>> getBottomRelativeCharacters() throws Exception{
		return this.bottomRelativeCharacters;
	}

	public Map<Coordinate, Integer> getTopmostSolidActiveColumnLayers() throws Exception{
		return this.topmostSolidActiveColumnLayers;
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

	public Set<ScreenRegion> getAllActiveTranslatedClippedExpandedChangedRegions(){
		return this.allActiveTranslatedClippedExpandedChangedRegions;
	}

	public Set<ScreenRegion> getAllActiveTranslatedChangedRegions(){
		return this.allActiveTranslatedChangedRegions;
	}


	public boolean isCharacterActive(Coordinate coordinate, int s, List<Map<Coordinate, TestScreenCharacter>> characterMap, ScreenLayer [] allLayers){
		if(s == 0){
			return this.randomizedForcedBottomLayerState;
		}else{
			return (
				characterMap.get(s).get(coordinate).active &&
				allLayers[s].getIsLayerActive()
				
			);
		}
	}

	private Map<Coordinate, TestScreenCharacter> calculateBeforeMergeCharacters(){
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

	private Coordinate layerToBottomCoordinate(Coordinate layerRelativeCoordinate, int s) throws Exception{
		// Placement offset of bottom layer is ignored since placements need to be relative to bottom layer:
		Long xPlacementOffset = s == 0L ? 0L : allLayers[s].getPlacementOffset().getX();
		Long yPlacementOffset = s == 0L ? 0L : allLayers[s].getPlacementOffset().getY();
		Coordinate bottomRelativeCoordinate = layerRelativeCoordinate.changeByDeltaXY(
			xPlacementOffset,
			yPlacementOffset
		);
		return bottomRelativeCoordinate;
	}

	private List<Map<Coordinate, TestScreenCharacter>> calculateBottomRelativeCharacters() throws Exception{
		List<Map<Coordinate, TestScreenCharacter>> rtn = new ArrayList<Map<Coordinate, TestScreenCharacter>>();
		for(int s = 0; s < allLayers.length; s++){
			rtn.add(new TreeMap<Coordinate, TestScreenCharacter>());
			for(int x = 0; x < allLayers[s].getWidth(); x++){
				for(int y = 0; y < allLayers[s].getHeight(); y++){
					Coordinate layerRelativeCoordinate = new Coordinate(Arrays.asList((long)x, (long)y));
					if(
						layerRelativeCharacters.get(s).containsKey(layerRelativeCoordinate) &&
						this.isCharacterActive(layerRelativeCoordinate, s, layerRelativeCharacters, allLayers)
					){
						TestScreenCharacter c = layerRelativeCharacters.get(s).get(layerRelativeCoordinate);
						//  Some of these entries will end up outside the boundaries of
						//  the bottom layer, but that's necessary to keep track of
						//  multi-column characters that might start outside the bottom
						//  layer.
						Coordinate bottomRelativeCoordinate = this.layerToBottomCoordinate(layerRelativeCoordinate, s);
						rtn.get(s).put(bottomRelativeCoordinate, c);
					}else{
						//  If there is no corresponding coordinate here, there will just be a gap.
					}
				}
			}
		}
		return rtn;
	}

	private Map<Coordinate, Map<Integer, SolidColumnType>> calculateIsColumnSolidAndActiveStates() throws Exception{
		Map<Coordinate, Map<Integer, SolidColumnType>> rtn = new HashMap<Coordinate, Map<Integer, SolidColumnType>>();
		// Initialization over the boundaries of the bottom layer
		for(int x = 0; x < allLayers[0].getWidth(); x++){
			for(int y = 0; y < allLayers[0].getHeight(); y++){
				Coordinate bottomLayerCoordinate = new Coordinate(Arrays.asList((long)x, (long)y));
				rtn.put(bottomLayerCoordinate, new HashMap<Integer, SolidColumnType>());
				for(int s = 0; s < allLayers.length; s++){
					rtn.get(bottomLayerCoordinate).put(s, SolidColumnType.NULL);
				}
			}
		}

		//  Iterate over original layer coordinates to catch multi-column characters that
		//  start outside the boundary of the bottom layer:
		for(int s = 0; s < allLayers.length; s++){
			for(int x = 0; x < allLayers[s].getWidth(); x++){
				for(int y = 0; y < allLayers[s].getHeight(); y++){
					Coordinate layerRelativeCoordinate = new Coordinate(Arrays.asList((long)x, (long)y));
					if(
						layerRelativeCharacters.get(s).containsKey(layerRelativeCoordinate) &&
						this.isCharacterActive(layerRelativeCoordinate, s, layerRelativeCharacters, allLayers)
					){
						TestScreenCharacter c = layerRelativeCharacters.get(s).get(layerRelativeCoordinate);

						//  For each one of the columns in this multi-column character:
						//  Only consider parts of characters inside the bottom layer region:
						int startingBaseLayerX = this.layerToBottomCoordinate(layerRelativeCoordinate, s).getX().intValue();
						int endingBaseLayerX = startingBaseLayerX + c.characterWidths -1;

						SolidColumnType columnType = null;
						if(startingBaseLayerX < 0){
							columnType = SolidColumnType.SEVERED_LEFT;
						}else if (!(endingBaseLayerX < allLayers[0].getWidth())){
							columnType = SolidColumnType.SEVERED_RIGHT;
						}else{
							columnType = SolidColumnType.SOLID;
						}

						for(int i = 0; i < c.characterWidths; i++){
							Coordinate solidLayerRelativeCoordinate = new Coordinate(
								Arrays.asList(
									(long)(layerRelativeCoordinate.getX().intValue() + i),
									(long)(layerRelativeCoordinate.getY().intValue())
								)
							);
							Coordinate solidBottomRelativeCoordinate = this.layerToBottomCoordinate(solidLayerRelativeCoordinate, s);
							if(
								// Make sure this coordinate is within the bounds of the bottom layer
								solidBottomRelativeCoordinate.getX() >=0L &&
								solidBottomRelativeCoordinate.getX() < allLayers[0].getWidth() &&
								solidBottomRelativeCoordinate.getY() >=0L &&
								solidBottomRelativeCoordinate.getY() < allLayers[0].getHeight()
							){
								rtn.get(solidBottomRelativeCoordinate).put(s, columnType);
							}
						}
					}else{
						//  If there is no corresponding coordinate here, there will just be a gap.
					}
				}
			}
		}
		return rtn;
	}

	private Map<Coordinate, Map<Integer, Boolean>> calculateIsColumnOccludedStates() throws Exception{
		Map<Coordinate, Map<Integer, Boolean>> rtn = new HashMap<Coordinate, Map<Integer, Boolean>>();

		//  Iterate over original layer coordinates to catch multi-column characters that
		//  start outside the boundary of the bottom layer:
		for(int s = allLayers.length -1; s >= 0; s--){
			for(int x = 0; x < allLayers[s].getWidth(); x++){
				for(int y = 0; y < allLayers[s].getHeight(); y++){
					Coordinate layerRelativeCoordinate = new Coordinate(Arrays.asList((long)x, (long)y));
					if(
						layerRelativeCharacters.get(s).containsKey(layerRelativeCoordinate) &&
						this.isCharacterActive(layerRelativeCoordinate, s, layerRelativeCharacters, allLayers)
					){
						TestScreenCharacter c = layerRelativeCharacters.get(s).get(layerRelativeCoordinate);

						if(c.characterWidths > 0){
							int currentCharacterWidth = c.characterWidths;

							//  First, if any of the columns in this character were
							//  occluded by some column above, mark ever column in this
							//  character as occluded:
							boolean hasAnyOccludedColunn = false;
							for(int i = 0; i < currentCharacterWidth; i++){
								Coordinate occluddedLayerRelativeCoordinate = new Coordinate(
									Arrays.asList(
										(long)(layerRelativeCoordinate.getX().intValue() + i),
										(long)(layerRelativeCoordinate.getY().intValue())
									)
								);
								Coordinate occludedBottomRelativeCoordinate = this.layerToBottomCoordinate(occluddedLayerRelativeCoordinate, s);
								if(!rtn.containsKey(occludedBottomRelativeCoordinate)){
									// Initialize coordinate it if it's not already set
									rtn.put(occludedBottomRelativeCoordinate, new TreeMap<Integer, Boolean>());
								}
								if(!rtn.get(occludedBottomRelativeCoordinate).containsKey(s)){
									rtn.get(occludedBottomRelativeCoordinate).put(s, false);
								}
								if(rtn.get(occludedBottomRelativeCoordinate).get(s)){
									hasAnyOccludedColunn = true;
								}
								//  While we're at it, set the occluded flag to true for
								//  every column under this one:
								for(int s_under = s-1; s_under >= 0; s_under--){
									rtn.get(occludedBottomRelativeCoordinate).put(s_under, true);
								}
							}
							//  At least one column was occluded.  Occlude the entire character:
							if(hasAnyOccludedColunn){
								for(int i = 0; i < currentCharacterWidth; i++){
									Coordinate occluddedLayerRelativeCoordinate = new Coordinate(
										Arrays.asList(
											(long)(layerRelativeCoordinate.getX().intValue() + i),
											(long)(layerRelativeCoordinate.getY().intValue())
										)
									);
									Coordinate occludedBottomRelativeCoordinate = this.layerToBottomCoordinate(occluddedLayerRelativeCoordinate, s);
									rtn.get(occludedBottomRelativeCoordinate).put(s, true);
								}
							}
						}

					}else{
						//  If there is no corresponding coordinate here, there will just be a gap.
					}
				}
			}
		}
		return rtn;
	}

	private Map<Coordinate, Integer> calculateTopmostSolidActiveColumnLayers() throws Exception{
		Map<Coordinate, Integer> rtn = new HashMap<Coordinate, Integer>();
		for(int x = 0; x < allLayers[0].getWidth(); x++){
			for(int y = 0; y < allLayers[0].getHeight(); y++){
				Coordinate bottomLayerCoordinate = new Coordinate(Arrays.asList((long)x, (long)y));

				int topMostLayer = allLayers.length -1;
				while(topMostLayer >= 0){
					if(
						isColumnSolidAndActiveStates.get(bottomLayerCoordinate).get(topMostLayer).equals(SolidColumnType.SOLID) ||
						isColumnSolidAndActiveStates.get(bottomLayerCoordinate).get(topMostLayer).equals(SolidColumnType.SEVERED_RIGHT) ||
						isColumnSolidAndActiveStates.get(bottomLayerCoordinate).get(topMostLayer).equals(SolidColumnType.SEVERED_LEFT)
					){
						//  A column that is part of a solid character exists
						//  at this position.
						rtn.put(bottomLayerCoordinate, topMostLayer);
						break;
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
						this.isCharacterActive(bottomLayerCoordinate, topMostLayer, bottomRelativeCharacters, allLayers)
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
						this.isCharacterActive(bottomLayerCoordinate, topMostLayer, bottomRelativeCharacters, allLayers)
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
						this.isCharacterActive(bottomLayerCoordinate, topMostLayer, bottomRelativeCharacters, allLayers)
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

	private Set<ScreenRegion> computeAllActiveTranslatedClippedExpandedChangedRegions() throws Exception{
		//  This function keeps track of the changed regions that are supposed to be
		//  in the final bottom merged layer 0.

		//  Everything gets merged down into a coordinate system based on layer 0
		CuboidAddress baseLayerCuboidAddress = ScreenRegion.makeScreenRegionCA(
			0,
			0,
			allLayers[0].getWidth(),
			allLayers[0].getHeight()
		);

		Set<ScreenRegion> allActiveChangedRegions = new TreeSet<ScreenRegion>();
		for(int i = 0; i < allLayers.length; i++){
			if(allLayers[i].getIsLayerActive()){
				for(ScreenRegion r : allLayers[i].getChangedRegions()){
					ScreenRegion translatedRegion = null;
					if(i == 0){
						translatedRegion = r;
					}else{
						//  For the upper changed regions above the final merged layer, translate them by the placement offset:
						translatedRegion = new ScreenRegion(ScreenLayer.makeDimensionsCA(
							r.getStartX() + allLayers[i].getPlacementOffset().getX().intValue(),
							r.getStartY() + allLayers[i].getPlacementOffset().getY().intValue(),
							r.getEndX() + allLayers[i].getPlacementOffset().getX().intValue(),
							r.getEndY() + allLayers[i].getPlacementOffset().getY().intValue()
						));
					}

					ScreenRegion expandedRegion = ScreenLayer.getNonCharacterCuttingChangedRegions(translatedRegion, allLayers);
					ScreenRegion clippedRegion = new ScreenRegion(expandedRegion.getRegion().getIntersectionCuboidAddress(baseLayerCuboidAddress));

					allActiveChangedRegions.add(clippedRegion);
				}
			}
		}
		return allActiveChangedRegions;
	}

	private Set<ScreenRegion> computeAllActiveTranslatedChangedRegions() throws Exception{
		//  For verifiction of changed flags in upper layers, we need to remember the
		//  original non-clipped changed regions because all of their changed flags
		//  are suppoused to get cleared after merging them in:
		Set<ScreenRegion> allActiveChangedRegions = new TreeSet<ScreenRegion>();
		for(int i = 0; i < allLayers.length; i++){
			if(allLayers[i].getIsLayerActive()){
				for(ScreenRegion r : allLayers[i].getChangedRegions()){
					ScreenRegion translatedRegion = null;
					if(i == 0){
						translatedRegion = r;
					}else{
						//  For the upper changed regions above the final merged layer, translate them by the placement offset:
						translatedRegion = new ScreenRegion(ScreenLayer.makeDimensionsCA(
							r.getStartX() + allLayers[i].getPlacementOffset().getX().intValue(),
							r.getStartY() + allLayers[i].getPlacementOffset().getY().intValue(),
							r.getEndX() + allLayers[i].getPlacementOffset().getX().intValue(),
							r.getEndY() + allLayers[i].getPlacementOffset().getY().intValue()
						));
					}
					ScreenRegion expandedRegion = ScreenLayer.getNonCharacterCuttingChangedRegions(translatedRegion, allLayers);

					allActiveChangedRegions.add(expandedRegion);
				}
			}
		}
		return allActiveChangedRegions;
	}
}
