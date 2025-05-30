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

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.stream.Collectors;

import java.util.HashSet;
import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class HelpMenu {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private HelpMenuLevel helpMenuLevel;
	private List<HelpMenuLevel> helpMenuLevelStack = new ArrayList<HelpMenuLevel>();
	private List<Integer> helpMenuLevelIndexStack = new ArrayList<Integer>();
	private boolean activeState;
	private boolean requireRedraw = false;

	public HelpMenu(boolean activeState, HelpMenuLevel helpMenuLevel) throws Exception {
		this.activeState= activeState;
		this.helpMenuLevel = helpMenuLevel;
		this.helpMenuLevelStack.add(helpMenuLevel);
		this.helpMenuLevelIndexStack.add(0);
		this.requireRedraw = true;
	}

	public boolean getActiveState(){
		return activeState;
	}

	public void setActiveState(boolean activeState){
		if(this.activeState != activeState){
			this.requireRedraw = true;
			this.activeState = activeState;
		}
	}

	public void descendIntoSubmenu() throws Exception{
		List<HelpMenuOption> options = getDisplayedHelpMenuOptions();
		HelpMenuLevel subLevel = options.get(getCurrentMenuYIndex()).getHelpMenuLevel();
		this.helpMenuLevelStack.add(subLevel);
		this.helpMenuLevelIndexStack.add(0);
		this.requireRedraw = true;
	}

	public void ascendFromSubmenu() throws Exception{
		this.helpMenuLevelStack.remove(this.helpMenuLevelStack.size()-1);
		this.helpMenuLevelIndexStack.remove(this.helpMenuLevelIndexStack.size()-1);
		this.requireRedraw = true;
	}

	public int getCurrentMenuYIndex(){
		return this.helpMenuLevelIndexStack.get(this.helpMenuLevelIndexStack.size()-1);
	}

	public void resetMenuState(){
		this.helpMenuLevelStack.clear();
		this.helpMenuLevelIndexStack.clear();
		this.helpMenuLevelStack.add(helpMenuLevel);
		this.helpMenuLevelIndexStack.add(0);
		this.requireRedraw = true;
	}

	public List<HelpMenuOption> getDisplayedHelpMenuOptions(){
		return this.helpMenuLevelStack.get(this.helpMenuLevelStack.size()-1).getHelpMenuOptions();
	}

	public void moveSelectionDown(){
		int maxIndex = getDisplayedHelpMenuOptions().size() -1;
		int newMenuYIndex = (getCurrentMenuYIndex() >= maxIndex) ? maxIndex : getCurrentMenuYIndex() + 1;
		this.helpMenuLevelIndexStack.set(this.helpMenuLevelIndexStack.size()-1, newMenuYIndex);
	}

	public void moveSelectionUp(){
		int newMenuYIndex = (getCurrentMenuYIndex() <= 0) ? 0 : getCurrentMenuYIndex() - 1;
		this.helpMenuLevelIndexStack.set(this.helpMenuLevelIndexStack.size()-1, newMenuYIndex);
	}

	public boolean getRequiresRedraw(){
		return this.requireRedraw;
	}

	public void setRequiresRedraw(boolean b){
		this.requireRedraw = b;
	}
}
