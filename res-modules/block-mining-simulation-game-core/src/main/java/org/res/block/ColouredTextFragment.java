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
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ColouredTextFragment{

	private String text;
	private int [] ansiColourCodes;

	public ColouredTextFragment(String text, int [] ansiColourCodes) {
		this.text = text;
		this.ansiColourCodes = ansiColourCodes;
	}

	public String getText(){
		return text;
	}

	public int [] getAnsiColourCodes(){
		return ansiColourCodes;
	}

	protected static List<String> splitStringIntoCharactersUnicodeAware(String str){
		List<String> utf16CodePoints = str.codePoints().mapToObj(Character::toString).collect(Collectors.toList());
		List<String> rtn = new ArrayList<String>();
		for(String s : utf16CodePoints){
			//  If any of the characters are 'variation selectors', don't split
			//  them up and keep them associated with the previous character:
			if(s.codePointAt(0) >= 0xFE00 && s.codePointAt(0) <= 0xFE0F && rtn.size() > 0){
				int previousCharIndex = rtn.size() -1;
				String previousChar = rtn.get(previousCharIndex);
				rtn.set(previousCharIndex, previousChar + s);
			}else{
				rtn.add(s);
			}
		}
		return rtn;
	}

	public List<ColouredCharacter> getColouredCharacters(){
		List<ColouredCharacter> rtn = new ArrayList<ColouredCharacter>();
		List<String> charactersToPrint = ColouredTextFragment.splitStringIntoCharactersUnicodeAware(this.getText());
		for(String s : charactersToPrint){
			rtn.add(new ColouredCharacter(s, this.getAnsiColourCodes()));
		}
		return rtn;
	}
}
