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

public enum HelpMenuOptionType {
        OPEN_NEW_FRAME (1L),
        QUIT_GAME (2L),
        BACK_UP_LEVEL (3L),
        DO_SUBMENU_OPEN_CUSTOM_FRAME (4L),
        CLOSE_CURRENT_FRAME (5L);

        private final long id;

        private HelpMenuOptionType(long i) {
                id = i;
        }

        public boolean equalsId(long i) {
                return id == i;
        }

        public long toLong() {
                return this.id;
        }

	private static final Map<Long, HelpMenuOptionType> helpMenuOptionTypesByValue = new HashMap<Long, HelpMenuOptionType>();

	static {
		for(HelpMenuOptionType type : HelpMenuOptionType.values()) {
			helpMenuOptionTypesByValue.put(type.toLong(), type);
		}
	}

	public static HelpMenuOptionType forValue(long value) {
		return helpMenuOptionTypesByValue.get(value);
	}
}
