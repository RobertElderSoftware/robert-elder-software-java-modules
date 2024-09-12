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

public enum UserInterfaceActionType {

        ACTION_Y_PLUS ("ACTION_Y_PLUS"),
        ACTION_Y_MINUS ("ACTION_Y_MINUS"),
        ACTION_X_PLUS ("ACTION_X_PLUS"),
        ACTION_X_MINUS ("ACTION_X_MINUS"),
        ACTION_Z_PLUS ("ACTION_Z_PLUS"),
        ACTION_Z_MINUS ("ACTION_Z_MINUS"),
        ACTION_MINING ("ACTION_MINING"),
        ACTION_CRAFTING ("ACTION_CRAFTING"),
        ACTION_QUIT ("ACTION_QUIT"),
        ACTION_PLACE_BLOCK ("ACTION_PLACE_BLOCK");

        private final String name;

        private UserInterfaceActionType(String n) {
                name = n;
        }

        public String toString() {
                return this.name;
        }

	private static final Map<String, UserInterfaceActionType> userInterfaceActionTypesByValue = new HashMap<String, UserInterfaceActionType>();

	static {
		for(UserInterfaceActionType type : UserInterfaceActionType.values()) {
			userInterfaceActionTypesByValue.put(type.toString(), type);
		}
	}

	public static UserInterfaceActionType forValue(String value) {
		return userInterfaceActionTypesByValue.get(value);
	}
}