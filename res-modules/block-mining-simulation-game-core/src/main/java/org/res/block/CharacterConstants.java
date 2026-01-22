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
	public static final int TOP_CONNECTION = 1 << 3;
	public static final int RIGHT_CONNECTION = 1 << 2;
	public static final int LEFT_CONNECTION = 1 << 1;
	public static final int BOTTOM_CONNECTION = 1 << 0;
	public static final String SPACE = " ";
	public static final String PLUS_SIGN = "+";
	public static final String EQUALS_SIGN = "=";
	public static final String ASTERISK = "*";
	public static final String VERTICAL_LINE = "|";
	public static final String NEWLINE_CHARACTER = "\n";
	public static final String CARRIAGE_RETURN_CHARACTER = "\r";
	public static final String BACKSPACE_CHARACTER = "\u007f";
	public static final String MIDDLE_DOT = "\u00B7"; // ·
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
	public static final String BOX_DRAWINGS_LIGHT_VERTICAL_AND_HORIZONTAL = "\u253C"; // ┼
	public static final String BOX_DRAWINGS_LIGHT_DOWN_AND_RIGHT = "\u250C"; // ┌
	public static final String BOX_DRAWINGS_LIGHT_DOWN_AND_LEFT = "\u2510"; // ┐
	public static final String BOX_DRAWINGS_LIGHT_UP_AND_RIGHT = "\u2514"; // └
	public static final String BOX_DRAWINGS_LIGHT_UP_AND_LEFT = "\u2518"; // ┘
	public static final String BOX_DRAWINGS_LIGHT_VERTICAL = "\u2502"; // │
	public static final String BOX_DRAWINGS_LIGHT_HORIZONTAL = "\u2500"; // ─


	public static final String INVENTORY_ARROW_EMOJI = "\u2501\u27A4";// ━➤
	public static final String INVENTORY_ARROW_ASCII = "->";

	/*
	 * 	Index is based on:
	 * 	         1 << 3 | 1 << 2 | 1 << 1 | 1 << 0
	 * 	index =    top  | right  |  left  |  down
	*/
	public static final String [] emojiDoubleBorderConstants = new String [] {
		MIDDLE_DOT, // ·
		BOX_DRAWINGS_DOWN_DOUBLE_AND_HORIZONTAL_SINGLE, // ╥
		BOX_DRAWINGS_VERTICAL_SINGLE_AND_LEFT_DOUBLE, // ╡
		BOX_DRAWINGS_DOUBLE_DOWN_AND_LEFT, // ╗
		BOX_DRAWINGS_VERTICAL_SINGLE_AND_RIGHT_DOUBLE, // ╞
		BOX_DRAWINGS_DOUBLE_DOWN_AND_RIGHT, // ╔
		BOX_DRAWINGS_DOUBLE_HORIZONTAL, // ═
		BOX_DRAWINGS_DOUBLE_DOWN_AND_HORIZONTAL, // ╦
		BOX_DRAWINGS_UP_DOUBLE_AND_HORIZONTAL_SINGLE, // ╨
		BOX_DRAWINGS_DOUBLE_VERTICAL, // ║
		BOX_DRAWINGS_DOUBLE_UP_AND_LEFT, // ╝
		BOX_DRAWINGS_DOUBLE_VERTICAL_AND_LEFT, // ╣
		BOX_DRAWINGS_DOUBLE_UP_AND_RIGHT, // ╚
		BOX_DRAWINGS_DOUBLE_VERTICAL_AND_RIGHT, // ╠
		BOX_DRAWINGS_DOUBLE_UP_AND_HORIZONTAL, // ╩
		BOX_DRAWINGS_DOUBLE_VERTICAL_AND_HORIZONTAL // ╬
	};

	public static final String [] emojiSingleBorderConstants = new String [] {
		MIDDLE_DOT, // · TODO
		MIDDLE_DOT, // · TODO
		MIDDLE_DOT, // · TODO
		BOX_DRAWINGS_LIGHT_DOWN_AND_LEFT, // ┐
		MIDDLE_DOT, // · TODO
		BOX_DRAWINGS_LIGHT_DOWN_AND_RIGHT, // ┌
		BOX_DRAWINGS_LIGHT_HORIZONTAL, // ─
		MIDDLE_DOT, // · TODO
		MIDDLE_DOT, // · TODO
		BOX_DRAWINGS_LIGHT_VERTICAL, // │
		BOX_DRAWINGS_LIGHT_UP_AND_LEFT, // ┘
		MIDDLE_DOT, // · TODO
		BOX_DRAWINGS_LIGHT_UP_AND_RIGHT, // └
		MIDDLE_DOT, // · TODO
		MIDDLE_DOT, // · TODO
		MIDDLE_DOT // · TODO
	};

	public static final String [] asciiBorderConstants = new String [] {
		CharacterConstants.PLUS_SIGN,
		CharacterConstants.VERTICAL_LINE,
		CharacterConstants.EQUALS_SIGN,
		CharacterConstants.PLUS_SIGN,
		CharacterConstants.EQUALS_SIGN,
		CharacterConstants.PLUS_SIGN,
		CharacterConstants.EQUALS_SIGN,
		CharacterConstants.PLUS_SIGN,
		CharacterConstants.PLUS_SIGN,
		CharacterConstants.VERTICAL_LINE,
		CharacterConstants.PLUS_SIGN,
		CharacterConstants.PLUS_SIGN,
		CharacterConstants.PLUS_SIGN,
		CharacterConstants.PLUS_SIGN,
		CharacterConstants.PLUS_SIGN,
		CharacterConstants.PLUS_SIGN
	};

	public static String makeTopBorder(BlockManagerThreadCollection blockManagerThreadCollection, long maxWidth) throws Exception{
		String startCharacter = singleBorderConnection(blockManagerThreadCollection, RIGHT_CONNECTION | BOTTOM_CONNECTION);
		String middleCharacter = singleBorderConnection(blockManagerThreadCollection, LEFT_CONNECTION | RIGHT_CONNECTION);
		String endCharacter = singleBorderConnection(blockManagerThreadCollection, LEFT_CONNECTION | BOTTOM_CONNECTION);
		ConsoleWriterThreadState cwts = blockManagerThreadCollection.getConsoleWriterThreadState();
		Long lWidth = cwts.measureTextLengthOnTerminal(startCharacter).getDeltaX();
		Long mWidth = cwts.measureTextLengthOnTerminal(middleCharacter).getDeltaX();
		Long rWidth = cwts.measureTextLengthOnTerminal(endCharacter).getDeltaX();

		Long nmc = Math.max(maxWidth - lWidth - rWidth, 0L) / mWidth;
		return startCharacter + middleCharacter.repeat(nmc.intValue()) + endCharacter;
	}

	public static String makeBottomBorder(BlockManagerThreadCollection blockManagerThreadCollection, long maxWidth) throws Exception{
		String startCharacter = singleBorderConnection(blockManagerThreadCollection, RIGHT_CONNECTION | TOP_CONNECTION);
		String middleCharacter = singleBorderConnection(blockManagerThreadCollection, LEFT_CONNECTION | RIGHT_CONNECTION);
		String endCharacter = singleBorderConnection(blockManagerThreadCollection, LEFT_CONNECTION | TOP_CONNECTION);
		ConsoleWriterThreadState cwts = blockManagerThreadCollection.getConsoleWriterThreadState();
		Long lWidth = cwts.measureTextLengthOnTerminal(startCharacter).getDeltaX();
		Long mWidth = cwts.measureTextLengthOnTerminal(middleCharacter).getDeltaX();
		Long rWidth = cwts.measureTextLengthOnTerminal(endCharacter).getDeltaX();

		Long nmc = Math.max(maxWidth - lWidth - rWidth, 0L) / mWidth;
		return startCharacter + middleCharacter.repeat(nmc.intValue()) + endCharacter;
	}

	public static String makeLeftBorder(BlockManagerThreadCollection blockManagerThreadCollection, long maxHeight) throws Exception{
		String startCharacter = singleBorderConnection(blockManagerThreadCollection, RIGHT_CONNECTION | BOTTOM_CONNECTION);
		String middleCharacter = singleBorderConnection(blockManagerThreadCollection, TOP_CONNECTION | BOTTOM_CONNECTION);
		String endCharacter = singleBorderConnection(blockManagerThreadCollection, RIGHT_CONNECTION | TOP_CONNECTION);
		ConsoleWriterThreadState cwts = blockManagerThreadCollection.getConsoleWriterThreadState();
		Long lHeight = 1L;
		Long mHeight = 1L;
		Long rHeight = 1L;

		Long nmc = Math.max(maxHeight - lHeight - rHeight, 0L) / mHeight;
		return startCharacter + middleCharacter.repeat(nmc.intValue()) + endCharacter;
	}

	public static String makeRightBorder(BlockManagerThreadCollection blockManagerThreadCollection, long maxHeight) throws Exception{
		String startCharacter = singleBorderConnection(blockManagerThreadCollection, LEFT_CONNECTION | BOTTOM_CONNECTION);
		String middleCharacter = singleBorderConnection(blockManagerThreadCollection, TOP_CONNECTION | BOTTOM_CONNECTION);
		String endCharacter = singleBorderConnection(blockManagerThreadCollection, LEFT_CONNECTION | TOP_CONNECTION);
		ConsoleWriterThreadState cwts = blockManagerThreadCollection.getConsoleWriterThreadState();
		Long lHeight = 1L;
		Long mHeight = 1L;
		Long rHeight = 1L;

		Long nmc = Math.max(maxHeight - lHeight - rHeight, 0L) / mHeight;
		return startCharacter + middleCharacter.repeat(nmc.intValue()) + endCharacter;
	}

	public static final String doubleBorderConnection(BlockManagerThreadCollection blockManagerThreadCollection, int index) throws Exception{
		boolean rg = blockManagerThreadCollection.getGraphicsMode().equals(GraphicsMode.ASCII);
		return rg ? asciiBorderConstants[index] : emojiDoubleBorderConstants[index];
	}

	public static final String singleBorderConnection(BlockManagerThreadCollection blockManagerThreadCollection, int index) throws Exception{
		boolean rg = blockManagerThreadCollection.getGraphicsMode().equals(GraphicsMode.ASCII);
		return rg ? asciiBorderConstants[index] : emojiSingleBorderConstants[index];
	}
}
