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

public class CharacterConstants {
	public static final String SPACE = " ";
	public static final String PLUS_SIGN = "+";
	public static final String EQUALS_SIGN = "=";
	public static final String ASTERISK = "*";
	public static final String VERTICAL_LINE = "|";
	public static final String BOX_DRAWINGS_LIGHT_VERTICAL_AND_HORIZONTAL = "\u253C"; // ┼
	public static final String BOX_DRAWINGS_DOWN_DOUBLE_AND_HORIZONTAL_SINGLE = "\u2565"; // ╥
	public static final String BOX_DRAWINGS_VERTICAL_SINGLE_AND_LEFT_DOUBLE = "\u2561"; // ╡
	public static final String BOX_DRAWINGS_VERTICAL_SINGLE_AND_RIGHT_DOUBLE = "\u255E"; // ╞
	public static final String BOX_DRAWINGS_UP_DOUBLE_AND_HORIZONTAL_SINGLE = "\u2568"; // ╨
	public static final String BOX_DRAWINGS_DOUBLE_DOWN_AND_LEFT = "\u2557"; // ╗
	public static final String BOX_DRAWINGS_DOUBLE_DOWN_AND_RIGHT = "\u2554"; // ╔
	public static final String BOX_DRAWINGS_DOUBLE_HORIZONTAL = "\u2550";//═
	public static final String BOX_DRAWINGS_DOUBLE_DOWN_AND_HORIZONTAL = "\u2566";//╦
	public static final String BOX_DRAWINGS_DOUBLE_VERTICAL = "\u2551";//║
	public static final String BOX_DRAWINGS_DOUBLE_UP_AND_LEFT = "\u255D";//╝
	public static final String BOX_DRAWINGS_DOUBLE_VERTICAL_AND_LEFT = "\u2563";//╣
	public static final String BOX_DRAWINGS_DOUBLE_UP_AND_RIGHT = "\u255A";//╚
	public static final String BOX_DRAWINGS_DOUBLE_VERTICAL_AND_RIGHT = "\u2560";//╠
	public static final String BOX_DRAWINGS_DOUBLE_UP_AND_HORIZONTAL = "\u2569";//╩
	public static final String BOX_DRAWINGS_DOUBLE_VERTICAL_AND_HORIZONTAL = "\u256C";//╬
											  //
	public static final String INVENTORY_ARROW_EMOJI = "\u2501\u27A4";// ━➤
	public static final String INVENTORY_ARROW_ASCII = "->";
}

