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

import java.io.BufferedInputStream;
import java.io.IOException;

import org.res.block.Coordinate;
import java.io.IOException;

import java.util.Set;
import java.util.TreeSet;
import java.util.TreeSet;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.ArrayList;

import java.util.Random;


import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.FileOutputStream;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.lang.ProcessBuilder.Redirect;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.security.MessageDigest;
import org.junit.Test;

public class BlockManagerUnitTest {

	private static String noiseTestOutputFolder = "/tmp";

	public BlockManagerUnitTest() {

	}

	public boolean getRandomBoolean(Random rand){
		return rand.nextInt() % 2 == 0 ? true : false;
	}

	public long getRandBetweenRange(Random rand, long lower, long upper){
		// Returns long values >= lower and < upper
		return (long)(rand.nextDouble() * (upper-lower)) + lower;
	}

	public Coordinate getRandomCoordinate(Random rand, CuboidAddress ca) throws Exception{
		Long numDimensions = ca.getNumDimensions();
		List<Long> l = new ArrayList<Long>();
		Coordinate lower = ca.getCanonicalLowerCoordinate();
		Coordinate upper = ca.getCanonicalUpperCoordinate();
		for(long i = 0; i < numDimensions; i++){
			l.add(getRandBetweenRange(rand, lower.getValueAtIndex(i), upper.getValueAtIndex(i)));
		}
		return new Coordinate(l);
	}

	public CuboidAddress getRandomCuboidAddress(Random rand, Long dimensionsToTest, CuboidAddress previousCuboid, CuboidAddress ca) throws Exception{
		if(previousCuboid == null || rand.nextInt(10) < 4){
			//  Returns a random cuboid address within 'ca'
			CuboidAddress rtn = new CuboidAddress(getRandomCoordinate(rand, ca), getRandomCoordinate(rand, ca));
			System.out.println("Created a new random cuboid " + rtn);
			return rtn;
		}else{
			//  Translate the previous cuboid around by a consistent amount for each dimension
			//  so that it will remain the same shape/size.
			//  This will be the most common use case.
			Long [] translation = new Long [dimensionsToTest.intValue()];
			for(int i = 0 ; i < dimensionsToTest.intValue(); i++){
				translation[i] = getRandBetweenRange(rand, -10L, 10L);
			}

			Coordinate lower = previousCuboid.getCanonicalLowerCoordinate();
			Coordinate upper = previousCuboid.getCanonicalUpperCoordinate();

			for(long i = 0 ; i < dimensionsToTest; i++){
				lower = lower.changeValueAtIndex(i, lower.getValueAtIndex(i) + translation[(int)i]);
				upper = upper.changeValueAtIndex(i, upper.getValueAtIndex(i) + translation[(int)i]);
			}
			CuboidAddress rtn = new CuboidAddress(lower, upper);
			System.out.println("Created a test cuboid by translating " + previousCuboid + " by " + String.valueOf(translation) + " to " + rtn);
			return rtn;
		}
	}

	@Test
	public void threeDimensionalCircularBufferTest() throws Exception {
		Random overallRand = new Random(1234);
		for(int iterations = 0; iterations < 100; iterations++){
			Random rand = new Random(overallRand.nextInt());
			Long dimensionsToTest = 3L;
			int numRegionsToTest = 20;
			int numPointsPerRegionToTest = 15;
			List<Long> testRegionLower = new ArrayList<Long>();
			List<Long> testRegionUpper = new ArrayList<Long>();
			for(long i = 0; i < dimensionsToTest; i++){
				testRegionLower.add(-10L);
				testRegionUpper.add(10L);
			}
			CuboidAddress testRegion = new CuboidAddress(new Coordinate(testRegionLower), new Coordinate(testRegionUpper));

			Long uninitializedValue = 0L;
			ThreeDimensionalCircularBuffer<Long> b = new ThreeDimensionalCircularBuffer<Long>(Long.class, uninitializedValue);

			//  Keep track of blocks that should be in the buffer and the values that they should have.
			Map<Coordinate, Long> blocksInBuffer = new TreeMap<Coordinate, Long>();

			CuboidAddress previousCuboid = null;
			for(int i = 0; i < numRegionsToTest; i++){
				CuboidAddress newCuboidAddress = getRandomCuboidAddress(rand, dimensionsToTest, previousCuboid, testRegion);
				CuboidAddress intersectionAddress = previousCuboid == null ? null : newCuboidAddress.getIntersectionCuboidAddress(previousCuboid);
				System.out.println("Updating region from " + String.valueOf(previousCuboid) + " to " + String.valueOf(newCuboidAddress) + ". Intersection was " + String.valueOf(intersectionAddress) + ".");
				previousCuboid = newCuboidAddress;
				b.updateBufferRegion(newCuboidAddress);

				//  Build list of coordinates that were removed by the buffer region change:
				Set<Coordinate> coordinatesToRemove = new TreeSet<Coordinate>();
				for(Map.Entry<Coordinate, Long> e : blocksInBuffer.entrySet()){
					if(!newCuboidAddress.containsCoordinate(e.getKey())){
						coordinatesToRemove.add(e.getKey());
					}
				}

				//  Actually remove them:
				for(Coordinate c : coordinatesToRemove){
					blocksInBuffer.remove(c);
					System.out.println("Removed coordinate " + c + " because it's not contained in the new region " + newCuboidAddress);
				}

				for(int j = 0; j < numPointsPerRegionToTest; j++){
					Coordinate randomCoordinateToUpdate = getRandomCoordinate(rand, testRegion);

					Long valueToStore = getRandBetweenRange(rand, -999999L, 999999L);
					int setExceptionsObserved = 0;
					boolean currentRegionContainsCoordinate = newCuboidAddress.containsCoordinate(randomCoordinateToUpdate);

					//  Add the new coordinate to the set of blocks that should be there:
					if(currentRegionContainsCoordinate){
						blocksInBuffer.put(randomCoordinateToUpdate, valueToStore);
					}

					int setExceptionsExpected = currentRegionContainsCoordinate ? 0 : 1;
					try{
						b.setObjectAtCoordinate(randomCoordinateToUpdate, valueToStore);
					}catch(Exception e){
						setExceptionsObserved++;
					}
					if(setExceptionsObserved == setExceptionsExpected){
						System.out.println("Correctly observed " + setExceptionsObserved + " exceptions when setting coordinate " + randomCoordinateToUpdate + " with value " + valueToStore + " into cuboid " + newCuboidAddress);
					}else{
						throw new Exception("Expected " + setExceptionsExpected + " but saw " + setExceptionsObserved + " exceptions when setting coordinate " + randomCoordinateToUpdate + " into cuboid " + newCuboidAddress);
					}

					int getExceptionsObserved = 0;
					int getExceptionsExpected = newCuboidAddress.containsCoordinate(randomCoordinateToUpdate) ? 0 : 1;
					Long valueRetrieved = null;
					try{
						valueRetrieved = b.getObjectAtCoordinate(randomCoordinateToUpdate);
					}catch(Exception e){
						getExceptionsObserved++;
					}
					if(getExceptionsObserved == getExceptionsExpected){
						System.out.println("Correctly observed " + getExceptionsObserved + " exceptions when getting coordinate " + randomCoordinateToUpdate + " into cuboid " + newCuboidAddress);
					}else{
						throw new Exception("Expected " + getExceptionsExpected + " but saw " + getExceptionsObserved + " exceptions when getting coordinate " + randomCoordinateToUpdate + " into cuboid " + newCuboidAddress);
					}

					if(getExceptionsExpected == 0){
						if(valueToStore.equals(valueRetrieved)){
							System.out.println("Stored value " + valueToStore + " and retrieved value " + valueRetrieved + " at " + randomCoordinateToUpdate + ".");
						}else{
							throw new Exception("Stored value " + valueToStore + " but retrieved value " + valueRetrieved + " at " + randomCoordinateToUpdate + ".");
						}
					}

					//  Cycle through all values that are suppoused to be in the buffer region to make sure they have the values that they're supposed to:
					int numPointsChecked = 0;
					System.out.println("BEGIN checking all points within current buffer region " + newCuboidAddress);
					RegionIteration regionIteration = new RegionIteration(newCuboidAddress.getCanonicalLowerCoordinate(), newCuboidAddress);
					if(newCuboidAddress.getVolume() > 0L){
						do{
							Coordinate currentCoordinate = regionIteration.getCurrentCoordinate();
							if(blocksInBuffer.containsKey(currentCoordinate)){
								Long actualValue = b.getObjectAtCoordinate(currentCoordinate);
								Long expectedValue = blocksInBuffer.get(currentCoordinate);
								if(!actualValue.equals(expectedValue)){
									throw new Exception("Expected value " + expectedValue + " but saw value " + actualValue + " for coordinate " + currentCoordinate);
								}
							}else{ // Uninitialized cell in buffer:
								Long actualValue = b.getObjectAtCoordinate(currentCoordinate);
								Long expectedValue = uninitializedValue;
								if(!actualValue.equals(expectedValue)){
									throw new Exception("Expected default value " + expectedValue + " but saw value " + actualValue + " for coordinate " + currentCoordinate);
								}
							}
							numPointsChecked++;
						}while (regionIteration.incrementCoordinateWithinCuboidAddress());
					}
					System.out.println("END checking all " + numPointsChecked + " points within current buffer region " + newCuboidAddress);


				}
			}
		}
	}

	@Test
	public void hiddenCharactersTest() throws Exception {
		String [] chars = new String [] {"A", "\u2550"};
		System.out.print("\033[2J"); // Clear screen
		int maxSeparation = 4;
		int maxWidth = 60;
		int offsetFromTop = 0;
		int titleSpace = 3;
		for(int chrIndex = 0; chrIndex < chars.length; chrIndex++){
			int yOffset = chrIndex * (maxSeparation * 2) + offsetFromTop + (chrIndex * titleSpace);
			System.out.print("\033[" + (1 + yOffset) + ";0H-----Printing test for character '" + chars[chrIndex] + "'-----");
			for(int lineIndex = 0; lineIndex < maxSeparation; lineIndex++){
				int columnSkip = lineIndex + 1;
				for(int i = 0; i < maxWidth; i += columnSkip){
					System.out.print("\033[" + (titleSpace + yOffset + lineIndex * 2) + ";0H");
					System.out.print("skip=" + lineIndex + " Print ->");
					System.out.print("\033[" + (titleSpace + yOffset + lineIndex * 2) + ";" + (i + 20) + "H");
					System.out.print(chars[chrIndex]);
				}
				for(int i = maxWidth-1; i >= 0 ; i -= columnSkip){
					System.out.print("\033[" + (titleSpace + yOffset + lineIndex * 2 + 1) + ";0H");
					System.out.print("skip=" + lineIndex + " Print <-");
					System.out.print("\033[" + (titleSpace + yOffset + lineIndex * 2 + 1) + ";" + (i + 20) + "H");
					System.out.print(chars[chrIndex]);
				}
			}
		}
                System.out.println("");
                System.out.println("");
                System.out.println("The above test will illustrate disappearing characters in the first two lines for the double wide character.");
                System.out.println("This is a problem GNU screen when run in wide character mode because GNU screen calculates the width of \u2550 as 1 when it's actually 2.");
                System.out.println("Other terminals correctly update the cursor 2 positions, so it normally won't be an issue and will be handled naturally.");
                System.out.println("The only way to handle this case would be to hard-code a width for this character in the game when run with this terminal.");
                System.out.println("Even vim is not able to handle this case correctly.");
                System.out.println("");
	}

	@Test
	public void alignmentProblemTest() throws Exception {
		String [] chars = new String [] {"A", "\u2550"};
		System.out.print("\033[2J"); // Clear screen
		int topOffset = 3;
		int maxWidth = 6;
		int titleSpace = 3;
		int maxSkip = 2;
		for(int chrIndex = 0; chrIndex < chars.length; chrIndex++){
			for(int skipIndex = 0; skipIndex < maxSkip; skipIndex++){
				int skip = skipIndex + 1;
				int chrTestOffset = topOffset + ((maxWidth + titleSpace) * (chrIndex * maxSkip + skipIndex));
				System.out.print("\033[" + (chrTestOffset + 1) + ";0H-----Printing test for character '" + chars[chrIndex] + "', skip=" + skip + "-----");
				for(int lineIndex = 0; lineIndex < maxWidth; lineIndex++){
					for(int i = 0; i < maxWidth; i += 1){
						int xOffset = i * skip;
						System.out.print("\033[" + (chrTestOffset + titleSpace + i) + ";" + (xOffset + 1) + "H");
						String charToUse = ((i % 2) == 0) ? chars[chrIndex] : "=";
						System.out.print(charToUse);
					}
				}
			}
		}
		System.out.println("");
		System.out.println("");
	}

	@Test
	public void noiseGeneratorUnitTests() throws Exception {
		//  Just uncomment whichever unit test you want to run:
		/*
		runMultiDimensionalNoiseGeneratorUnitTest(
			new MultiDimensionalNoiseGeneratorUnitTestParameters(
				"sha512-patterns-around-origin",
				MessageDigest.getInstance("SHA-512"),
				false,      // Make video or not?
				1000,        // width
				1000,        // height
				-500,       // x offset
				-500,       // y offset
				1,          // num frames
				(p, noiseGenerator, x, y, z) -> {
					long [] coordinate = new long [3];
					coordinate[0] = x + p.getXOffset();
					coordinate[1] = y + p.getYOffset();
					coordinate[2] = z;

					int num_octaves = 40;
					double [] frequencies = new double [num_octaves];
					double [] amplitudes = new double [num_octaves];
					double frequency = 0.01;
					double total_amplitude = 0.0;
					for(int i = 0; i < num_octaves; i++){
						frequencies[i] = frequency;
						amplitudes[i] = 1.0 / Math.pow(frequency, 0.8);
						total_amplitude += amplitudes[i];
						frequency *= 1.2;
					}

					double noiseAtPixel = noiseGenerator.multiOctaveNoiseAtCoordinate(coordinate, frequencies, amplitudes) / Math.pow(total_amplitude, 0.7);
					return noiseAtPixel;
				}
			)
		);
		runMultiDimensionalNoiseGeneratorUnitTest(
			new MultiDimensionalNoiseGeneratorUnitTestParameters(
				"example-ore-islands",
				MessageDigest.getInstance("SHA-512"),
				false,      // Make video or not?
				600,        // width
				600,        // height
				-200,       // x offset
				-200,       // y offset
				1,          // num frames
				(p, noiseGenerator, x, y, z) -> {
					long [] coordinate = new long [3];
					coordinate[0] = x + p.getXOffset();
					coordinate[1] = y + p.getYOffset();
					coordinate[2] = z;
					double smallWaveNoise = noiseGenerator.multiOctaveNoiseAtCoordinate(coordinate, new double [] {0.08}, new double [] {Math.pow(0.08, 1.2)}) * 100;
					double largeWaveNoise = (noiseGenerator.multiOctaveNoiseAtCoordinate(coordinate, new double [] {0.01}, new double [] {Math.pow(0.01, 1.2)}) * 10000) - 3.0;
					double positiveLargeWaveNoise = largeWaveNoise < 0.0 ? 0.0 : largeWaveNoise;
					double noiseAtPixel = smallWaveNoise * positiveLargeWaveNoise;
					return noiseAtPixel;
				}
			)
		);
		runMultiDimensionalNoiseGeneratorUnitTest(
			new MultiDimensionalNoiseGeneratorUnitTestParameters(
				"two-frequencies",
				MessageDigest.getInstance("SHA-512"),
				false,      // Make video or not?
				600,        // width
				600,        // height
				-200,       // x offset
				-200,       // y offset
				1,          // num frames
				(p, noiseGenerator, x, y, z) -> {
					long [] coordinate = new long [3];
					coordinate[0] = x + p.getXOffset();
					coordinate[1] = y + p.getYOffset();
					coordinate[2] = z;
					double [] frequencies = new double [] {0.08, 0.01};
					double [] amplitudes = new double [] {1.0 / Math.pow(0.08, 1.2), 1.0 / Math.pow(0.01, 1.2)};
					double noiseAtPixel = noiseGenerator.multiOctaveNoiseAtCoordinate(coordinate, frequencies, amplitudes) / 8;
					return noiseAtPixel;
				}
			)
		);
		runMultiDimensionalNoiseGeneratorUnitTest(
			new MultiDimensionalNoiseGeneratorUnitTestParameters(
				"pink-noise",
				MessageDigest.getInstance("SHA-512"),
				false,      // Make video or not?
				1920,        // width
				1080,        // height
				-200,       // x offset
				-200,       // y offset
				1,          // num frames
				(p, noiseGenerator, x, y, z) -> {
					long [] coordinate = new long [3];
					coordinate[0] = x + p.getXOffset();
					coordinate[1] = y + p.getYOffset();
					coordinate[2] = z;

					int num_octaves = 1;
					double [] frequencies = new double [num_octaves];
					double [] amplitudes = new double [num_octaves];
					double frequency = 0.01;
					double total_amplitude = 0.0;
					for(int i = 0; i < num_octaves; i++){
						frequencies[i] = frequency;
						amplitudes[i] = 1.0 / Math.pow(frequency, 1.2);
						total_amplitude += amplitudes[i];
						frequency *= 2;
					}

					double noiseAtPixel = noiseGenerator.multiOctaveNoiseAtCoordinate(coordinate, frequencies, amplitudes) / Math.pow(total_amplitude, 0.9);
					return noiseAtPixel;
				}
			)
		);
                runMultiDimensionalNoiseGeneratorUnitTest(
                        new MultiDimensionalNoiseGeneratorUnitTestParameters(
                                "4k-interesting-demo",
				MessageDigest.getInstance("SHA-512"),
				true,       // Make video or not?
                                3840,        // width
                                2160,        // height
                                10000,       // x offset
                                10000,       // y offset
                                //120,         // num frames
                                1,         // num frames
                                (p, noiseGenerator, x, y, z) -> {
                                        long [] coordinate = new long [3];
                                        coordinate[0] = x + p.getXOffset();
                                        coordinate[1] = y + p.getYOffset();
                                        coordinate[2] = z;

                                        int num_octaves = 6;
                                        double [] frequencies = new double [num_octaves];
                                        double [] amplitudes = new double [num_octaves];
                                        double frequency_base = 0.07;
                                        double current_frequency = frequency_base;
                                        double total_amplitude = 0.0;
                                        for(int i = 0; i < num_octaves; i++){
                                                frequencies[i] = current_frequency;
                                                double denominator = Math.pow(current_frequency, 0.9);
                                                amplitudes[i] = 1.0 / denominator;
                                                total_amplitude += amplitudes[i];
                                                current_frequency /= 2;
                                        }

                                        double noiseAtPixel = noiseGenerator.multiOctaveNoiseAtCoordinate(coordinate, frequencies, amplitudes) / Math.pow(total_amplitude, 0.65);
                                        return noiseAtPixel;
                                }
                        )
                );
                */
	}

	@Test
	public void runWorkItemQueueTest() throws Exception {
		WorkItemQueue<UnitTestWorkItem> workItemQueue = new WorkItemQueue<UnitTestWorkItem>();

		Random rand = new Random(1234);
		int numItems = 5000;
		for(int i = 0; i < numItems; i++){
			WorkItemPriority randomPriority = WorkItemPriority.valueOf(rand.nextInt(WorkItemPriority.size));
			UnitTestWorkItem workItem = new UnitTestWorkItem(randomPriority,i);
			workItemQueue.putWorkItem(workItem, randomPriority);
		}

		WorkItemPriority lastWorkItemPriority = WorkItemPriority.PRIORITY_HIGH;
		int lastCreationOrder = -1;
		for(int i = 0; i < numItems; i++){
			UnitTestWorkItem retrievedWorkItem = workItemQueue.takeWorkItem();
			System.out.print("Got this work item: " + retrievedWorkItem.toString());
			if(lastWorkItemPriority.getPriorityValue() == retrievedWorkItem.getWorkItemPriority().getPriorityValue()){
				System.out.print(", same priority as last, ");
			}else if(lastWorkItemPriority.getPriorityValue() < retrievedWorkItem.getWorkItemPriority().getPriorityValue()){
				System.out.print(", moving to lower priority items, ");
				lastCreationOrder = -1;
			}else{
				throw new Exception("Priority was lower?  This should not happen.");
			}
			lastWorkItemPriority = retrievedWorkItem.getWorkItemPriority();

			if(lastCreationOrder < retrievedWorkItem.getCreationOrder()){
				System.out.print(" and has higher creation index.");
			}else{
				throw new Exception("Creation index was lower or equal?  This should not happen.");
			}
			lastCreationOrder = retrievedWorkItem.getCreationOrder();

			System.out.print("\n");
		}
		System.out.println("Looks like all " + numItems + " came out of the priority queue in the expected order.");
		System.out.println("TEST PASSED.");
	}

	public void runMultiDimensionalNoiseGeneratorUnitTest(MultiDimensionalNoiseGeneratorUnitTestParameters params) throws Exception {

		int width = params.getWidth();
		int height = params.getHeight();
		int x_offset = params.getXOffset();
		int y_offset = params.getYOffset();
		int num_frames = params.getNumFrames();
		int[] flattenedData = new int[width*height*3];

		ShellProcessRunner r = null;
		OutputStream inputForffmpegProcess = null;
		if(params.getMakeVideo()){
			//  This will produce a video file that actually shows you drifting through the noise field.
			//  The 'success' for this test is whether the noise actually looks smooth or not.
			List<String> commandParts = Arrays.asList(
				new String [] {
					"ffmpeg",
					"-y",
					"-f",
					"rawvideo",
					"-pix_fmt",
					"rgb24",
					"-s",
					width + "x" + height,
					"-r",
					"30",
					"-i",
					"-",
					"-c:v",
					"libx264",
					"-crf",
					"0",
					"-preset",
					"ultrafast",
					"-qp",
					"0",
					"-an",
					BlockManagerUnitTest.noiseTestOutputFolder + "/" + params.getTestName() + ".mp4"
				}
			);

			r = new ShellProcessRunner(commandParts, null, null, true);
			inputForffmpegProcess = r.getOutputStreamForStdin();
		}

		MultiDimensionalNoiseGenerator noiseGenerator = new MultiDimensionalNoiseGenerator(0L, params.getMessageDigest());

		for(int z = 0; z < num_frames; z++){
			long frameStartTime = System.currentTimeMillis();
			BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			for(int x = 0; x < width; x++){
				for(int y = 0; y < height; y++){
					double noiseAtPixel = params.getNoiseFunction().noiseFunction(params, noiseGenerator, x, y, z);
					noiseAtPixel = noiseAtPixel > 1.0 ? 1.0 : noiseAtPixel;
					noiseAtPixel = noiseAtPixel < -1.0 ? -1.0 : noiseAtPixel;
					if(noiseAtPixel < -1.0 || noiseAtPixel > 1.0){
						System.out.println("nosieAtPixel out of range: " + noiseAtPixel);
					}
					double normalizedPositiveNose = (noiseAtPixel + 1.0) / 2.0;
					int greyShade = ((int)(normalizedPositiveNose * (double)255));
					int alpha = 0xFF;
					img.setRGB(x, (height - y -1), greyShade + (greyShade << 8) + (greyShade << 16) + (alpha << 24));
				}
				System.out.println("Finished " + params.getTestName() + " x=" + x + " of " + width + ", frame z=" + z + " of " + num_frames);
			}
			long frameEndTime = System.currentTimeMillis();
			System.out.println("Finished frame z=" + z + " in " + (frameEndTime - frameStartTime) + " milliseconds.");
			Graphics2D g2d = img.createGraphics();
			int fontSize = params.getHeight() / 30;
			g2d.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
			int xOffsetText = params.getWidth() / 60;
			int yOffsetText = params.getHeight() / 30;
			g2d.drawString("z=" + z, xOffsetText, yOffsetText);

			byte [] imageData = ((DataBufferByte)img.getRaster().getDataBuffer()).getData();

			if(params.getMakeVideo()){
				inputForffmpegProcess.write(imageData, 0, imageData.length);

				ShellProcessPartialResult pr = r.getPartialResult();
				System.out.print(new String(pr.getStdoutOutput(), "UTF-8"));
				System.err.print(new String(pr.getStderrOutput(), "UTF-8"));
			}else{
				//  For inspection of individual noise frames:
				String currentTestFolder = BlockManagerUnitTest.noiseTestOutputFolder + "/" + params.getTestName();
				String rawFilesLocation = currentTestFolder + "/raw";
				String bitmapFilesLocation = currentTestFolder + "/bitmap";
				Files.createDirectories(Paths.get(rawFilesLocation));
				Files.createDirectories(Paths.get(bitmapFilesLocation));
				String rawFilename = rawFilesLocation + "/" + z + ".dat";
				try (FileOutputStream rawFileOutputStream = new FileOutputStream(rawFilename)) {
					rawFileOutputStream.write(imageData);
				}
				String bitmapFilename = bitmapFilesLocation + "/" + z + ".bmp";
				ImageIO.write(img, "BMP", new File(bitmapFilename));
				System.out.println("Wrote out to " + bitmapFilename);
			}
		}
		if(params.getMakeVideo()){
			inputForffmpegProcess.close();

			ShellProcessFinalResult f = r.getFinalResult();
			System.out.print(new String(f.getOutput().getStdoutOutput(), "UTF-8"));
			System.err.print(new String(f.getOutput().getStderrOutput(), "UTF-8"));

			System.out.println("Process exited with return code " + f.getReturnValue());
		}
	}

	@Test
	public void runIntersectingChunkSetUnitTest() throws Exception {
		Random rand = new Random(1234);
		Long numTestIterations = 1000L;
		int maxNumDimensions = 5;
		System.out.println("Begin testing " + numTestIterations + " rounds runIntersectingChunkSetUnitTest to " + maxNumDimensions + " dimensions each.");
		for(long l = 0L; l < numTestIterations; l++){
			Long numDimensions = (long)rand.nextInt(maxNumDimensions) + 1;

			CuboidAddress tmpA = new CuboidAddress(
				Coordinate.getRandomCoordinate(rand, numDimensions, -8, 8),
				Coordinate.getRandomCoordinate(rand, numDimensions, -6, 6)
			);

			CuboidAddress randomRegion = new CuboidAddress(
				tmpA.getCanonicalLowerCoordinate(),
				tmpA.getCanonicalUpperCoordinate().add(Coordinate.makeUnitCoordinate(numDimensions))
			);

			CuboidAddress tmpB = new CuboidAddress(
				Coordinate.makeOriginCoordinate(numDimensions),
				Coordinate.getRandomCoordinate(rand, numDimensions, 1, 12)
			);

			CuboidAddress chunkSize = new CuboidAddress(
				tmpB.getCanonicalLowerCoordinate(),
				tmpB.getCanonicalUpperCoordinate().add(Coordinate.makeUnitCoordinate(numDimensions))
			);



			System.out.println("Random region is " + randomRegion + " chunkSize is " + chunkSize);

			Set<CuboidAddress> inefficientlyCalculatedRequiredRegions = new TreeSet<CuboidAddress>();

			if(randomRegion.getVolume() > 0L){
				RegionIteration regionIteration = new RegionIteration(randomRegion.getCanonicalLowerCoordinate(), randomRegion);
				do{
					Coordinate currentCoordinate = regionIteration.getCurrentCoordinate();
					CuboidAddress chunkCuboidAddress = CuboidAddress.blockCoordinateToChunkCuboidAddress(currentCoordinate, chunkSize);
					inefficientlyCalculatedRequiredRegions.add(chunkCuboidAddress);
				}while (regionIteration.incrementCoordinateWithinCuboidAddress());
			}
			//System.out.println("inefficientlyCalculatedRequiredRegions: " + inefficientlyCalculatedRequiredRegions);

			Set<CuboidAddress> efficientlyCalculatedRequiredRegions = new TreeSet<CuboidAddress>();
			efficientlyCalculatedRequiredRegions.addAll(randomRegion.getIntersectingChunkSet(chunkSize));
			//System.out.println("efficientlyCalculatedRequiredRegions: " + efficientlyCalculatedRequiredRegions);

			for(CuboidAddress efficient : efficientlyCalculatedRequiredRegions){
				if(!inefficientlyCalculatedRequiredRegions.contains(efficient)){
					throw new Exception("Efficiently calculated region " + efficient + " was not present in inefficientlyCalculatedRequiredRegions=" + inefficientlyCalculatedRequiredRegions + ", randomRegion=" + randomRegion + ", chunkSize=" + chunkSize);
				}
			}

			for(CuboidAddress inefficient : inefficientlyCalculatedRequiredRegions){
				if(!efficientlyCalculatedRequiredRegions.contains(inefficient)){
					throw new Exception("Inefficiently calculated region " + inefficient + " was not present in " + efficientlyCalculatedRequiredRegions + ", randomRegion=" + randomRegion + ", chunkSize=" + chunkSize);
				}
			}
		}
		System.out.println("Finished testing " + numTestIterations + " rounds runIntersectingChunkSetUnitTest to " + maxNumDimensions + " dimensions each.");
	}

	@Test
	public void runCuboidAddressIntersectionUnitTest() throws Exception {
		Random rand = new Random(1234);
		Long numTestIterations = 10000L;
		int maxNumDimensions = 8;
		System.out.println("Begin testing " + numTestIterations + " rounds of region intersections with up to " + maxNumDimensions + " dimensions each.");
		for(long l = 0L; l < numTestIterations; l++){
			Long numDimensions = (long)rand.nextInt(maxNumDimensions) + 1;

			System.out.println(rand.nextInt());
			System.out.println(rand.nextInt());
			System.out.println(rand.nextInt());
			System.out.println(rand.nextInt());
			CuboidAddress addressA = new CuboidAddress(
				Coordinate.getRandomCoordinate(rand, numDimensions, -2, 2),
				Coordinate.getRandomCoordinate(rand, numDimensions, -2, 2)
			);

			CuboidAddress addressB = new CuboidAddress(
				Coordinate.getRandomCoordinate(rand, numDimensions, -2, 2),
				Coordinate.getRandomCoordinate(rand, numDimensions, -2, 2)
			);

			System.out.println("BEGIN-----");

			boolean nullOnIntersectFail = rand.nextInt() == 0 ? true : false;

			CuboidAddress calculatedIntersection = addressA.getIntersectionCuboidAddress(addressB, nullOnIntersectFail);

			System.out.println("Made these two random addresses: " + addressA + " and " + addressB + ", calculatedIntersection was calculated as: " + calculatedIntersection);

			Set<Coordinate> insideIntersection = new TreeSet<Coordinate>();
			Set<Coordinate> outsideIntersection = new TreeSet<Coordinate>();

			System.out.println("addressA.getCanonicalLowerCoordinate()=" + addressA.getCanonicalLowerCoordinate() + ".");
			if(addressA.getVolume() > 0L){
				RegionIteration regionIterationA = new RegionIteration(addressA.getCanonicalLowerCoordinate(), addressA);
				do{
					Coordinate currentCoordinate = regionIterationA.getCurrentCoordinate();
					System.out.println("currentCoordinate=" + currentCoordinate + " in A.");
					if(addressA.containsCoordinate(currentCoordinate) && addressB.containsCoordinate(currentCoordinate)){
						System.out.println(currentCoordinate + " is inside intersectionA.");
						insideIntersection.add(currentCoordinate.copy());
					}else{
						System.out.println(currentCoordinate + " is outside intersectionA.");
						outsideIntersection.add(currentCoordinate.copy());
					}
				}while (regionIterationA.incrementCoordinateWithinCuboidAddress());
			}

			if(addressB.getVolume() > 0L){
				RegionIteration regionIterationB = new RegionIteration(addressB.getCanonicalLowerCoordinate(), addressB);
				do{
					Coordinate currentCoordinate = regionIterationB.getCurrentCoordinate();
					System.out.println("currentCoordinate=" + currentCoordinate + " in B.");
					if(addressA.containsCoordinate(currentCoordinate) && addressB.containsCoordinate(currentCoordinate)){
						System.out.println(currentCoordinate + " is inside intersectionB.");
						insideIntersection.add(currentCoordinate.copy());
					}else{
						System.out.println(currentCoordinate + " is outside intersectionB.");
						outsideIntersection.add(currentCoordinate.copy());
					}
				}while (regionIterationB.incrementCoordinateWithinCuboidAddress());
			}

			System.out.println("Here are the points that are in intersection: " + insideIntersection + ".");
			System.out.println("Here are the points that are outside intersection: " + outsideIntersection + ".");

			CuboidAddress empiricalIntersection = null;
			if(insideIntersection.size() > 0){
				Long [] lowest = new Long[numDimensions.intValue()];
				Long [] highest = new Long[numDimensions.intValue()];
				for(Long i = 0L; i < numDimensions; i++){
					for(Coordinate c : insideIntersection){
						if(lowest[i.intValue()] == null || c.getValueAtIndex(i) < lowest[i.intValue()]){
							lowest[i.intValue()] = c.getValueAtIndex(i);
						}

						if(highest[i.intValue()] == null || c.getValueAtIndex(i) > highest[i.intValue()]){
							highest[i.intValue()] = c.getValueAtIndex(i);
						}
					}
				}

				
				List<Long> lowestGuessedIntersection = new ArrayList<Long>();
				List<Long> highestGuessedIntersection = new ArrayList<Long>();
				for(Long i = 0L; i < numDimensions; i++){
					lowestGuessedIntersection.add(lowest[i.intValue()]);
					highestGuessedIntersection.add(highest[i.intValue()] + 1L);
				}
				empiricalIntersection = new CuboidAddress(new Coordinate(lowestGuessedIntersection), new Coordinate(highestGuessedIntersection));
				System.out.println("The cuboid address intersection was empirically shown to be " + empiricalIntersection);
			}else{
				System.out.println("There was no intersection cuboid address to calculate.");
			}

			if(empiricalIntersection == null){
				/*
					This is super inefficient, and technically this way of calculating
					the 'empirical' intersection would work for the previous case as 
					well, but the point here is to have a robust test that is simple
					to verify for correctness.  Also having two different way of calculating
					the same thing gives better test coverage.
				*/
				boolean hasSomeZeroSizeIntersection = true;
				//  This case can happen when the intersection is a zero sized region.
				Long [] lowest = new Long[numDimensions.intValue()];
				Long [] highest = new Long[numDimensions.intValue()];
				Coordinate lowerA = addressA.getCanonicalLowerCoordinate();
				Coordinate upperA = addressA.getCanonicalUpperCoordinate();
				Coordinate lowerB = addressB.getCanonicalLowerCoordinate();
				Coordinate upperB = addressB.getCanonicalUpperCoordinate();
				for(Long i = 0L; i < numDimensions; i++){
					long aMin = Math.min(lowerA.getValueAtIndex(i), upperA.getValueAtIndex(i));
					long aMax = Math.max(lowerA.getValueAtIndex(i), upperA.getValueAtIndex(i));
					long bMin = Math.min(lowerB.getValueAtIndex(i), upperB.getValueAtIndex(i));
					long bMax = Math.max(lowerB.getValueAtIndex(i), upperB.getValueAtIndex(i));


					if(aMax <= bMin){
						/*  No intersection */
						hasSomeZeroSizeIntersection = false;
					}else{
						if(aMin >= bMax){
							/*  No intersection */
							hasSomeZeroSizeIntersection = false;
						}else{
							//  Intersection starts at the highest 'low' coordinate:
							Long lowerOverlap = aMin >= bMin ? aMin : bMin;
							//  And goes to the lowest 'upper' coordinate:
							Long upperOverlap = aMax <= bMax ? aMax : bMax;
							lowest[i.intValue()] = lowerOverlap;
							highest[i.intValue()] = upperOverlap;
						}
					}
				}
				
				if(hasSomeZeroSizeIntersection){
					List<Long> lowestGuessedIntersection = new ArrayList<Long>();
					List<Long> highestGuessedIntersection = new ArrayList<Long>();
					for(Long i = 0L; i < numDimensions; i++){
						lowestGuessedIntersection.add(lowest[i.intValue()]);
						highestGuessedIntersection.add(highest[i.intValue()]);
					}
					empiricalIntersection = new CuboidAddress(new Coordinate(lowestGuessedIntersection), new Coordinate(highestGuessedIntersection));
				}else{
					if(nullOnIntersectFail){
						empiricalIntersection = null;
					}else{
						empiricalIntersection = new CuboidAddress(
							Coordinate.makeOriginCoordinate(numDimensions),
							Coordinate.makeOriginCoordinate(numDimensions)
						);
					}
				}
			}

			if(empiricalIntersection == null){
				if(calculatedIntersection == null){
					System.out.println("Pass: Empirical and calculated intersection were both null.");
				}else{
					throw new Exception("There was a difference between empirical intersection which was null: and the calculated intersection was: " + calculatedIntersection + ".  A was " + addressA + ", B was " + addressB);
				}
			}else{
				if(empiricalIntersection.equals(calculatedIntersection)){
					System.out.println("Pass, : " + empiricalIntersection + " is the same as " + calculatedIntersection + ".");
				}else{
					throw new Exception("There was a difference between empirical intersection: " + empiricalIntersection + " and the calculated intersection: " + calculatedIntersection + ".  A was " + addressA + ", B was " + addressB);
				}
			}
			
			System.out.println("END-----");
		}

		System.out.println("Finished " + numTestIterations + " rounds of testing region intersections with up to " + maxNumDimensions + " dimensions each.");
	}

	public void verifyObject(Object observed, Object expected) throws Exception{
		this.verifyObject(observed, expected, "");
	}

	public void verifyObject(Object observed, Object expected, String msg) throws Exception{
		if(!Objects.equals(observed, expected)){
			throw new Exception("Expected object was '" + String.valueOf(expected) + "', but saw '" + String.valueOf(observed) + "' instead, msg=" + msg);
		}
	}

	public void verifyArray(Object [] observed, Object [] expected) throws Exception{
		this.verifyArray(observed,  expected, "");
	}

	public void verifyArray(Object [] observed, Object [] expected, String msg) throws Exception{
		if(!Arrays.equals(observed, expected)){
			throw new Exception("Expected array was '" + String.valueOf(expected) + "', but saw '" + String.valueOf(observed) + "' instead, msg=" + msg);
		}
	}

	public void verifyArray(int [] observed, int [] expected) throws Exception{
		this.verifyArray(observed, expected, "");
	}

	public void verifyArray(int [] observed, int [] expected, String msg) throws Exception{
		if(!Arrays.equals(observed, expected)){
			throw new Exception("Expected array was '" + Arrays.toString(expected) + "', but saw '" + Arrays.toString(observed) + "' instead, msg=" + msg);
		}
	}

	public void mergeChangesTest1() throws Exception{
		//  Simple merge down test with a change.
		ScreenLayer t = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 1, 1));
		t.initialize();
		t.setMultiColumnCharacter(0, 0, "A", 1, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR}, true, true);
		t.addChangedRegion(new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, t.getWidth(), t.getHeight())));

		ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 1, 1));
		merged.initialize();
		merged.mergeDown(t, false);
		this.verifyObject(merged.getColumnCharacter(0, 0), "A");
		this.verifyObject(merged.getColumnCharacterWidth(0, 0), 1);
		this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.getColumnChanged(0, 0), true);
	}

	public void mergeChangesTest2() throws Exception{
		//  Simple merge down test with no change flag set with region;  Should get ignored:
		ScreenLayer t = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 1, 1));
		t.initialize();
		t.setMultiColumnCharacter(0, 0, "A", 1, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR}, false, false);
		t.addChangedRegion(new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, t.getWidth(), t.getHeight())));

		ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 1, 1));
		merged.initialize();
		merged.mergeDown(t, false);

		this.verifyObject(merged.getColumnCharacter(0, 0), null);
		this.verifyObject(merged.getColumnCharacterWidth(0, 0), 0);
		this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {});
		this.verifyObject(merged.getColumnChanged(0, 0), true);
	}

        public void mergeChangesTest3() throws Exception{
                //  Simple merge down test with active flag not set, but with no region;  Should get ignored:
                ScreenLayer t = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 1, 1));
                t.initialize();
                t.clearChangedRegions();
		t.setMultiColumnCharacter(0, 0, "A", 1, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR}, true, false);

                ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 1, 1));
                merged.initialize();
                merged.mergeDown(t, false);

                this.verifyObject(merged.getColumnCharacter(0, 0), null);
                this.verifyObject(merged.getColumnCharacterWidth(0, 0), 0);
                this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {});
                this.verifyObject(merged.getColumnChanged(0, 0), true);
        }

        public void mergeChangesTest4() throws Exception{
                //  Merge down with two column character
                ScreenLayer t = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 2, 1));
                t.initialize();
		t.setMultiColumnCharacter(0, 0, "A", 2, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR}, true, true);
                ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 2, 1));
                merged.initialize();
                merged.setAllChangedFlagStates(false);
                merged.mergeDown(t, false);

                this.verifyObject(merged.getColumnCharacter(0, 0), "A");
                this.verifyObject(merged.getColumnCharacterWidth(0, 0), 2);
                this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                this.verifyObject(merged.getColumnChanged(0, 0), true);

                //  There should be an empty column after:
                this.verifyObject(merged.getColumnCharacter(1, 0), null);
                this.verifyObject(merged.getColumnCharacterWidth(1, 0), 0);
                this.verifyArray(merged.getColumnColourCodes(1, 0), new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                this.verifyObject(merged.getColumnChanged(1, 0), true);
        }

        public void mergeChangesTest5() throws Exception{
                //  Multi-column characters that don't fit in last column should just become null characters:
                ScreenLayer t = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 1));
                t.setAllChangedFlagStates(false);
                t.initialize();
                t.setColumnCharacter(2, 0, "A");
                t.setColumnCharacterWidth(2, 0, 2);
                t.setColumnColourCodes(2, 0, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                t.setColumnChanged(2, 0, true);
                t.setColumnActive(2, 0, true);

                ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 1));
                merged.initialize();
                merged.setAllChangedFlagStates(false);
                merged.mergeDown(t, false);

                this.verifyObject(merged.getColumnCharacter(0, 0), null);
                this.verifyObject(merged.getColumnCharacterWidth(0, 0), 0);
                this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {});
                this.verifyObject(merged.getColumnChanged(0, 0), false);

                this.verifyObject(merged.getColumnCharacter(1, 0), null);
                this.verifyObject(merged.getColumnCharacterWidth(1, 0), 0);
                this.verifyArray(merged.getColumnColourCodes(1, 0), new int [] {});
                this.verifyObject(merged.getColumnChanged(1, 0), false);

                //  There was not enough space to print the last multi-column character:
                this.verifyObject(merged.getColumnCharacter(2, 0), " ");
                this.verifyObject(merged.getColumnCharacterWidth(2, 0), 1);
                this.verifyArray(merged.getColumnColourCodes(2, 0), new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                this.verifyObject(merged.getColumnChanged(2, 0), true);
        }

        public void mergeChangesTest6() throws Exception{
                //  Make sure 
                ScreenLayer t = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 1));
                t.setAllChangedFlagStates(false);
                t.initialize();
                t.setColumnCharacter(0, 0, "A");
                t.setColumnCharacterWidth(0, 0, 2);
                t.setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                t.setColumnChanged(0, 0, true);
                t.setColumnActive(0, 0, true);
                t.setColumnCharacter(1, 0, null);
                t.setColumnCharacterWidth(1, 0, 0);
                t.setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                t.setColumnChanged(1, 0, true);
                t.setColumnActive(1, 0, true);
                t.validate();

                ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 1));
                merged.initialize();
                merged.setAllChangedFlagStates(false);
                t.setPlacementOffset(new Coordinate(Arrays.asList(2L,0L)));
                merged.mergeDown(t, false); //  Merge in at end, should force character to null
                t.setPlacementOffset(new Coordinate(Arrays.asList(0L,0L)));
                merged.mergeDown(t, false); //  Merge in at start, should be written correctly.
                t.setPlacementOffset(new Coordinate(Arrays.asList(1L,0L)));
                merged.mergeDown(t, false); //  Should overwrite previous character due to multi-column overlap.

                this.verifyObject(merged.getColumnCharacter(0, 0), " ");
                this.verifyObject(merged.getColumnCharacterWidth(0, 0), 1);
                this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                this.verifyObject(merged.getColumnChanged(0, 0), true);

                this.verifyObject(merged.getColumnCharacter(1, 0), "A");
                this.verifyObject(merged.getColumnCharacterWidth(1, 0), 2);
                this.verifyArray(merged.getColumnColourCodes(1, 0), new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                this.verifyObject(merged.getColumnChanged(1, 0), true);

                //  There was not enough space to print the last multi-column character:
                this.verifyObject(merged.getColumnCharacter(2, 0), null);
                this.verifyObject(merged.getColumnCharacterWidth(2, 0), 0);
                this.verifyArray(merged.getColumnColourCodes(2, 0), new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                this.verifyObject(merged.getColumnChanged(2, 0), true);
        }

        public void testSimpleTwoColumn() throws Exception{
		//  Simplest test of merge together two layers for multi-column character
		ScreenLayer [] layers = new ScreenLayer [1];
		layers[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 1));
		layers[0].initialize();
		layers[0].setAllChangedFlagStates(false);
		layers[0].setColumnCharacter(0, 0, "A");
		layers[0].setColumnCharacterWidth(0, 0, 2);
		layers[0].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		layers[0].setColumnChanged(0, 0, true);
		layers[0].setColumnActive(0, 0, true);
		layers[0].setColumnCharacter(1, 0, null);
		layers[0].setColumnCharacterWidth(1, 0, 0);
		layers[0].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		layers[0].setColumnChanged(1, 0, true);
		layers[0].setColumnActive(1, 0, true);

		ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 1));
		merged.initialize();
		merged.setAllChangedFlagStates(false);
		merged.mergeDown(layers, true);

		this.verifyObject(merged.getColumnCharacter(0, 0), "A");
		this.verifyObject(merged.getColumnCharacterWidth(0, 0), 2);
		this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.getColumnChanged(0, 0), true);

		this.verifyObject(merged.getColumnCharacter(1, 0), null);
		this.verifyObject(merged.getColumnCharacterWidth(1, 0), 0);
		this.verifyArray(merged.getColumnColourCodes(1, 0), new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.getColumnChanged(1, 0), true);

		this.verifyObject(merged.getColumnCharacter(2, 0), null);
		this.verifyObject(merged.getColumnCharacterWidth(2, 0), 0);
		this.verifyArray(merged.getColumnColourCodes(2, 0), new int [] {});
		this.verifyObject(merged.getColumnChanged(2, 0), false);
        }

        public void testAscendingMultiColumn() throws Exception{
		//  Ascending overlap of multi-column characters
		ScreenLayer [] layers = new ScreenLayer [3];
		layers[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[0].initialize();
		layers[0].setAllChangedFlagStates(false);
		layers[0].setColumnCharacter(0, 0, "A");
		layers[0].setColumnCharacterWidth(0, 0, 2);
		layers[0].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.BLUE_FG_COLOR});
		layers[0].setColumnChanged(0, 0, true);
		layers[0].setColumnActive(0, 0, true);
		layers[0].setColumnCharacter(1, 0, null);
		layers[0].setColumnCharacterWidth(1, 0, 0);
		layers[0].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.BLUE_FG_COLOR});
		layers[0].setColumnChanged(1, 0, true);
		layers[0].setColumnActive(1, 0, true);

		layers[1] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[1].initialize();
		layers[1].setAllChangedFlagStates(false);
		layers[1].setColumnCharacter(1, 0, "A"); //  Overlaps first "A"
		layers[1].setColumnCharacterWidth(1, 0, 2);
		layers[1].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR});
		layers[1].setColumnChanged(1, 0, true);
		layers[1].setColumnActive(1, 0, true);
		layers[1].setColumnCharacter(2, 0, null);
		layers[1].setColumnCharacterWidth(2, 0, 0);
		layers[1].setColumnColourCodes(2, 0, new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR});
		layers[1].setColumnChanged(2, 0, true);
		layers[1].setColumnActive(2, 0, true);

		layers[2] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[2].initialize();
		layers[2].setAllChangedFlagStates(false);
		layers[2].setColumnCharacter(2, 0, "A");
		layers[2].setColumnCharacterWidth(2, 0, 2);
		layers[2].setColumnColourCodes(2, 0, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		layers[2].setColumnChanged(2, 0, true);
		layers[2].setColumnActive(2, 0, true);
		layers[2].setColumnCharacter(3, 0, null);
		layers[2].setColumnCharacterWidth(3, 0, 0);
		layers[2].setColumnColourCodes(3, 0, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		layers[2].setColumnChanged(3, 0, true);
		layers[2].setColumnActive(3, 0, true);

		ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		merged.initialize();
		merged.setAllChangedFlagStates(false);
		merged.mergeDown(layers, true);

		this.verifyObject(merged.getColumnCharacter(0, 0), " ");
		this.verifyObject(merged.getColumnCharacterWidth(0, 0), 1);
		this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {UserInterfaceFrameThreadState.BLUE_FG_COLOR});
		this.verifyObject(merged.getColumnChanged(0, 0), true);

		this.verifyObject(merged.getColumnCharacter(1, 0), " ");
		this.verifyObject(merged.getColumnCharacterWidth(1, 0), 1);
		this.verifyArray(merged.getColumnColourCodes(1, 0), new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR});
		this.verifyObject(merged.getColumnChanged(1, 0), true);

		this.verifyObject(merged.getColumnCharacter(2, 0), "A");
		this.verifyObject(merged.getColumnCharacterWidth(2, 0), 2);
		this.verifyArray(merged.getColumnColourCodes(2, 0), new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.getColumnChanged(2, 0), true);

		this.verifyObject(merged.getColumnCharacter(3, 0), null);
		this.verifyObject(merged.getColumnCharacterWidth(3, 0), 0);
		this.verifyArray(merged.getColumnColourCodes(3, 0), new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.getColumnChanged(3, 0), true);
        }

        public void testDescendingMultiColumn() throws Exception{
		//  Descending overlap of multi-column characters
		ScreenLayer [] layers = new ScreenLayer [3];

		layers[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[0].initialize();
		layers[0].setAllChangedFlagStates(false);
		layers[0].setColumnCharacter(2, 0, "A");
		layers[0].setColumnCharacterWidth(2, 0, 2);
		layers[0].setColumnColourCodes(2, 0, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		layers[0].setColumnChanged(2, 0, true);
		layers[0].setColumnActive(2, 0, true);
		layers[0].setColumnCharacter(3, 0, null);
		layers[0].setColumnCharacterWidth(3, 0, 0);
		layers[0].setColumnColourCodes(3, 0, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		layers[0].setColumnChanged(3, 0, true);
		layers[0].setColumnActive(3, 0, true);

		layers[1] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[1].initialize();
		layers[1].setAllChangedFlagStates(false);
		layers[1].setColumnCharacter(1, 0, "A");
		layers[1].setColumnCharacterWidth(1, 0, 2);
		layers[1].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR});
		layers[1].setColumnChanged(1, 0, true);
		layers[1].setColumnActive(1, 0, true);
		layers[1].setColumnCharacter(2, 0, null);
		layers[1].setColumnCharacterWidth(2, 0, 0);
		layers[1].setColumnColourCodes(2, 0, new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR});
		layers[1].setColumnChanged(2, 0, true);
		layers[1].setColumnActive(2, 0, true);

		layers[2] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[2].initialize();
		layers[2].setAllChangedFlagStates(false);
		layers[2].setColumnCharacter(0, 0, "A");
		layers[2].setColumnCharacterWidth(0, 0, 2);
		layers[2].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.BLUE_FG_COLOR});
		layers[2].setColumnChanged(0, 0, true);
		layers[2].setColumnActive(0, 0, true);
		layers[2].setColumnCharacter(1, 0, null);
		layers[2].setColumnCharacterWidth(1, 0, 0);
		layers[2].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.BLUE_FG_COLOR});
		layers[2].setColumnChanged(1, 0, true);
		layers[2].setColumnActive(1, 0, true);

		ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		merged.initialize();
		merged.setAllChangedFlagStates(false);
		merged.mergeDown(layers, true);

		this.verifyObject(merged.getColumnCharacter(0, 0), "A");
		this.verifyObject(merged.getColumnCharacterWidth(0, 0), 2);
		this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {UserInterfaceFrameThreadState.BLUE_FG_COLOR});
		this.verifyObject(merged.getColumnChanged(0, 0), true);

		this.verifyObject(merged.getColumnCharacter(1, 0), null);
		this.verifyObject(merged.getColumnCharacterWidth(1, 0), 0);
		this.verifyArray(merged.getColumnColourCodes(1, 0), new int [] {UserInterfaceFrameThreadState.BLUE_FG_COLOR});
		this.verifyObject(merged.getColumnChanged(1, 0), true);

		this.verifyObject(merged.getColumnCharacter(2, 0), " ");
		this.verifyObject(merged.getColumnCharacterWidth(2, 0), 1);
		this.verifyArray(merged.getColumnColourCodes(2, 0), new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR});
		this.verifyObject(merged.getColumnChanged(2, 0), true);

		this.verifyObject(merged.getColumnCharacter(3, 0), " ");
		this.verifyObject(merged.getColumnCharacterWidth(3, 0), 1);
		this.verifyArray(merged.getColumnColourCodes(3, 0), new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.getColumnChanged(3, 0), true);
        }

        public void twoColumnOverOneColumn() throws Exception{
		//  Test to ensure that characters in layers under multi-column characters
		//  are properly obscured:
		ScreenLayer [] layers = new ScreenLayer [3];
		layers[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[0].initialize();
		layers[0].setAllChangedFlagStates(false);
		layers[0].setColumnCharacter(1, 0, " ");
		layers[0].setColumnCharacterWidth(1, 0, 1); //  Bottom Layer
		layers[0].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		layers[0].setColumnChanged(1, 0, true);
		layers[0].setColumnActive(1, 0, true);

		layers[1] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[1].initialize();
		layers[1].setAllChangedFlagStates(false);
		layers[1].setColumnCharacter(0, 0, "A"); //  Middle layer
		layers[1].setColumnCharacterWidth(0, 0, 2);
		layers[1].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		layers[1].setColumnChanged(0, 0, true);
		layers[1].setColumnActive(0, 0, true);
		layers[1].setColumnCharacter(1, 0, null); //  Middle layer
		layers[1].setColumnCharacterWidth(1, 0, 0);
		layers[1].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		layers[1].setColumnChanged(1, 0, true);
		layers[1].setColumnActive(1, 0, true);

		layers[2] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[2].initialize();
		layers[2].setAllChangedFlagStates(false); //  Top layer, all nulls

		ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		merged.initialize();
		merged.setAllChangedFlagStates(false);
		merged.mergeDown(layers, true);

		this.verifyObject(merged.getColumnCharacter(0, 0), "A");
		this.verifyObject(merged.getColumnCharacterWidth(0, 0), 2);
		this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.getColumnChanged(0, 0), true);

		this.verifyObject(merged.getColumnCharacter(1, 0), null);
		this.verifyObject(merged.getColumnCharacterWidth(1, 0), 0);
		this.verifyArray(merged.getColumnColourCodes(1, 0), new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.getColumnChanged(1, 0), true);

		this.verifyObject(merged.getColumnCharacter(2, 0), null);
		this.verifyObject(merged.getColumnCharacterWidth(2, 0), 0);
		this.verifyArray(merged.getColumnColourCodes(2, 0), new int [] {});
		this.verifyObject(merged.getColumnChanged(2, 0), false);

		this.verifyObject(merged.getColumnCharacter(3, 0), null);
		this.verifyObject(merged.getColumnCharacterWidth(3, 0), 0);
		this.verifyArray(merged.getColumnColourCodes(3, 0), new int [] {});
		this.verifyObject(merged.getColumnChanged(3, 0), false);
        }


        public void testMergeDownNoNeedToPrint() throws Exception{
                ScreenLayer [] layers = new ScreenLayer [1];
		layers[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 1, 1));
                layers[0].initialize();
                layers[0].setAllChangedFlagStates(false);
                layers[0].setColumnCharacter(0, 0, "M");
                layers[0].setColumnCharacterWidth(0, 0, 1);
                layers[0].setColumnColourCodes(0, 0, new int [] {});
                layers[0].setColumnChanged(0, 0, true);
                layers[0].setColumnActive(0, 0, true);

                ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 1, 1));
                merged.initialize();
                merged.setAllChangedFlagStates(false);
                merged.setColumnCharacter(0, 0, "M");
                merged.setColumnCharacterWidth(0, 0, 1);
                merged.setColumnColourCodes(0, 0, new int [] {});
                merged.setColumnChanged(0, 0, false);
                merged.setColumnActive(0, 0, true);

                merged.mergeDown(layers, false);

                this.verifyObject(merged.getColumnCharacter(0, 0), "M");
                this.verifyObject(merged.getColumnCharacterWidth(0, 0), 1);
                this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {});
                this.verifyObject(merged.getColumnChanged(0, 0), false);
        }

        public void writeBorderTwiceNoChange() throws Exception{
                ScreenLayer [] layers = new ScreenLayer [1];
		layers[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 2, 1));
                layers[0].initialize();
                layers[0].setAllChangedFlagStates(false);
                layers[0].setColumnCharacter(0, 0, "=");
                layers[0].setColumnCharacterWidth(0, 0, 2);
                layers[0].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                layers[0].setColumnChanged(0, 0, true);
                layers[0].setColumnActive(0, 0, true);
                layers[0].setColumnCharacter(1, 0, null);
                layers[0].setColumnCharacterWidth(1, 0, 0);
                layers[0].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                layers[0].setColumnChanged(1, 0, true);
                layers[0].setColumnActive(1, 0, true);

                ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 2, 1));
                merged.initialize();
                merged.setAllChangedFlagStates(false);
                merged.setColumnCharacter(0, 0, null);
                merged.setColumnCharacterWidth(0, 0, 0);
                merged.setColumnColourCodes(0, 0, new int [] {});
                merged.setColumnChanged(0, 0, false);
                merged.setColumnActive(0, 0, true);
                merged.setColumnCharacter(1, 0, null);
                merged.setColumnCharacterWidth(1, 0, 0);
                merged.setColumnColourCodes(1, 0, new int [] {});
                merged.setColumnChanged(1, 0, false);
                merged.setColumnActive(1, 0, true);

                merged.mergeDown(layers, true);
                //  Merging in the layer is supposed to clear the changed flags:
                this.verifyObject(layers[0].getColumnChanged(0, 0), false);
                this.verifyObject(layers[0].getColumnChanged(1, 0), false);

                //  Check resulting merged layer:
                this.verifyObject(merged.getColumnCharacter(0, 0), "=");
                this.verifyObject(merged.getColumnCharacterWidth(0, 0), 2);
                this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(merged.getColumnChanged(0, 0), true);
                this.verifyObject(merged.getColumnCharacter(1, 0), null);
                this.verifyObject(merged.getColumnCharacterWidth(1, 0), 0);
                this.verifyArray(merged.getColumnColourCodes(1, 0), new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(merged.getColumnChanged(1, 0), true);

                ScreenLayer output = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 2, 1));
                output.initialize();
                output.setAllChangedFlagStates(false);
                output.setColumnCharacter(0, 0, null);
                output.setColumnCharacterWidth(0, 0, 0);
                output.setColumnColourCodes(0, 0, new int [] {});
                output.setColumnChanged(0, 0, false);
                output.setColumnActive(0, 0, true);

                output.mergeDown(merged, false);
                //  Merging in the layer is supposed to clear the changed flags:
                this.verifyObject(merged.getColumnChanged(0, 0), false);
                this.verifyObject(merged.getColumnChanged(1, 0), false);
		

                this.verifyObject(output.getColumnCharacter(0, 0), "=");
                this.verifyObject(output.getColumnCharacterWidth(0, 0), 2);
                this.verifyArray(output.getColumnColourCodes(0, 0), new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(output.getColumnChanged(0, 0), true);
                this.verifyObject(output.getColumnCharacter(1, 0), null);
                this.verifyObject(output.getColumnCharacterWidth(1, 0), 0);
                this.verifyArray(output.getColumnColourCodes(1, 0), new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(output.getColumnChanged(1, 0), true);

                this.verifyObject(output.getColumnChanged(0, 0), true);
                this.verifyObject(output.getColumnChanged(1, 0), true);
		output.printChanges(false, 10, 10); // Clear changed flags
                this.verifyObject(output.getColumnChanged(0, 0), false);
                this.verifyObject(output.getColumnChanged(1, 0), false);

                // Try to Print same thing again:
                merged.mergeDown(layers, true);
                //  Merging in the layer is supposed to clear the changed flags:
                this.verifyObject(layers[0].getColumnChanged(0, 0), false);
                this.verifyObject(layers[0].getColumnChanged(1, 0), false);

                //  Merged layer should be same, but with change flag set to false because it was cleared during previous merge:
                this.verifyObject(merged.getColumnCharacter(0, 0), "=");
                this.verifyObject(merged.getColumnCharacterWidth(0, 0), 2);
                this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(merged.getColumnChanged(0, 0), false);
                this.verifyObject(merged.getColumnCharacter(1, 0), null);
                this.verifyObject(merged.getColumnCharacterWidth(1, 0), 0);
                this.verifyArray(merged.getColumnColourCodes(1, 0), new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(merged.getColumnChanged(1, 0), false);

                output.mergeDown(merged, false);

                this.verifyObject(output.getColumnCharacter(0, 0), "=");
                this.verifyObject(output.getColumnCharacterWidth(0, 0), 2);
                this.verifyArray(output.getColumnColourCodes(0, 0), new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(output.getColumnChanged(0, 0), false);
                this.verifyObject(output.getColumnCharacter(1, 0), null);
                this.verifyObject(output.getColumnCharacterWidth(1, 0), 0);
                this.verifyArray(output.getColumnColourCodes(1, 0), new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(output.getColumnChanged(1, 0), false);
                System.out.println("\033[0m"); //  Reset background colour for following tests.
        }

        public void testInheritBackgroundBelow() throws Exception{
                //  Test merging to inherit whatever background colour is used by layer underneath.
                ScreenLayer [] layers = new ScreenLayer [1];
		layers[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 1));
                layers[0].initialize();
                layers[0].setAllChangedFlagStates(false);
                layers[0].setColumnCharacter(0, 0, "A");
                layers[0].setColumnCharacterWidth(0, 0, 2);
                layers[0].setColumnColourCodes(0, 0, new int [] {});
                layers[0].setColumnChanged(0, 0, true);
                layers[0].setColumnActive(0, 0, true);
                layers[0].setColumnCharacter(1, 0, null);
                layers[0].setColumnCharacterWidth(1, 0, 0);
                layers[0].setColumnColourCodes(1, 0, new int [] {});
                layers[0].setColumnChanged(1, 0, true);
                layers[0].setColumnActive(1, 0, true);

                ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 1));
                merged.initialize();
                merged.setAllChangedFlagStates(false);
                merged.setColumnCharacter(0, 0, " ");
                merged.setColumnCharacterWidth(0, 0, 1);
                merged.setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.BLUE_BG_COLOR});
                merged.setColumnChanged(0, 0, false);
                merged.setColumnActive(0, 0, true);
                merged.setColumnCharacter(1, 0, null);
                merged.setColumnCharacterWidth(1, 0, 0);
                merged.setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.BLUE_BG_COLOR});
                merged.setColumnChanged(1, 0, false);
                merged.setColumnActive(1, 0, true);

                merged.mergeDown(layers, true);

                this.verifyObject(merged.getColumnCharacter(0, 0), "A");
                this.verifyObject(merged.getColumnCharacterWidth(0, 0), 2);
                this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {UserInterfaceFrameThreadState.BLUE_BG_COLOR});
                this.verifyObject(merged.getColumnChanged(0, 0), true);

                this.verifyObject(merged.getColumnCharacter(1, 0), null);
                this.verifyObject(merged.getColumnCharacterWidth(1, 0), 0);
                this.verifyArray(merged.getColumnColourCodes(1, 0), new int [] {UserInterfaceFrameThreadState.BLUE_BG_COLOR});
                this.verifyObject(merged.getColumnChanged(1, 0), true);

                this.verifyObject(merged.getColumnCharacter(2, 0), null);
                this.verifyObject(merged.getColumnCharacterWidth(2, 0), 0);
                this.verifyArray(merged.getColumnColourCodes(2, 0), new int [] {});
                this.verifyObject(merged.getColumnChanged(2, 0), false);
        }

        public void testIgnoreBackgroundFlaggedFalse() throws Exception{
		//  Test for disappearing background colours on layers underneath
		//  are properly obscured:
		ScreenLayer [] layers = new ScreenLayer [3];
		layers[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[0].initialize();
		layers[0].setAllChangedFlagStates(false);
		layers[0].setColumnCharacter(0, 0, " ");
		layers[0].setColumnCharacterWidth(0, 0, 1); //  Bottom Layer
		layers[0].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		layers[0].setColumnChanged(0, 0, true);
		layers[0].setColumnActive(0, 0, true);
		layers[0].setColumnCharacter(1, 0, " ");
		layers[0].setColumnCharacterWidth(1, 0, 1); 
		layers[0].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		layers[0].setColumnChanged(1, 0, true);
		layers[0].setColumnActive(1, 0, true);

		layers[1] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[1].initialize();
		layers[1].setAllChangedFlagStates(false);
		layers[1].setColumnCharacter(0, 0, "A"); //  Middle layer, should merge down and inherit bottom layer's BG colour
		layers[1].setColumnCharacterWidth(0, 0, 2);
		layers[1].setColumnColourCodes(0, 0, new int [] {});
		layers[1].setColumnChanged(0, 0, false);
		layers[1].setColumnActive(0, 0, true);
		layers[1].setColumnCharacter(1, 0, null);
		layers[1].setColumnCharacterWidth(1, 0, 0);
		layers[1].setColumnColourCodes(1, 0, new int [] {});
		layers[1].setColumnChanged(1, 0, false);
		layers[1].setColumnActive(1, 0, true);

		layers[2] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[2].initialize();
		layers[2].setAllChangedFlagStates(false); //  Top layer, all nulls

		ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		merged.initialize();
		merged.setAllChangedFlagStates(false);
		merged.mergeDown(layers, true);

		this.verifyObject(merged.getColumnCharacter(0, 0), "A");
		this.verifyObject(merged.getColumnCharacterWidth(0, 0), 2);
		this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		this.verifyObject(merged.getColumnChanged(0, 0), true);

		this.verifyObject(merged.getColumnCharacter(1, 0), null);
		this.verifyObject(merged.getColumnCharacterWidth(1, 0), 0);
		this.verifyArray(merged.getColumnColourCodes(1, 0), new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		this.verifyObject(merged.getColumnChanged(1, 0), true);

		this.verifyObject(merged.getColumnCharacter(2, 0), null);
		this.verifyObject(merged.getColumnCharacterWidth(2, 0), 0);
		this.verifyArray(merged.getColumnColourCodes(2, 0), new int [] {});
		this.verifyObject(merged.getColumnChanged(2, 0), false);

		this.verifyObject(merged.getColumnCharacter(3, 0), null);
		this.verifyObject(merged.getColumnCharacterWidth(3, 0), 0);
		this.verifyArray(merged.getColumnColourCodes(3, 0), new int [] {});
		this.verifyObject(merged.getColumnChanged(3, 0), false);
        }

        public void testPartiallyCoveredCharacterBackground() throws Exception{
		//  Test for disappearing background colours for partially covered characters
		//  on the right edge.
		ScreenLayer [] layers = new ScreenLayer [3];
		layers[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[0].initialize();
		layers[0].setAllChangedFlagStates(false);
		layers[0].setColumnCharacter(0, 0, "A");
		layers[0].setColumnCharacterWidth(0, 0, 2); //  Bottom Layer
		layers[0].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		layers[0].setColumnChanged(0, 0, true);
		layers[0].setColumnActive(0, 0, true);
		layers[0].setColumnCharacter(1, 0, null);
		layers[0].setColumnCharacterWidth(1, 0, 0); 
		layers[0].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		layers[0].setColumnChanged(1, 0, true);
		layers[0].setColumnActive(1, 0, true);

		layers[1] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[1].initialize();
		layers[1].setAllChangedFlagStates(false);
		layers[1].setColumnCharacter(0, 0, " "); //  Middle layer,
		layers[1].setColumnCharacterWidth(0, 0, 1);
		layers[1].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.GREEN_BG_COLOR});
		layers[1].setColumnChanged(0, 0, true);
		layers[1].setColumnActive(0, 0, true);

		layers[2] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[2].initialize();
		layers[2].setAllChangedFlagStates(false); //  Top layer, all nulls

		ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		merged.initialize();
		merged.setAllChangedFlagStates(false);
		merged.mergeDown(layers, true);

		this.verifyObject(merged.getColumnCharacter(0, 0), " ");
		this.verifyObject(merged.getColumnCharacterWidth(0, 0), 1);
		this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {UserInterfaceFrameThreadState.GREEN_BG_COLOR});
		this.verifyObject(merged.getColumnChanged(0, 0), true);

		this.verifyObject(merged.getColumnCharacter(1, 0), " ");
		this.verifyObject(merged.getColumnCharacterWidth(1, 0), 1);
		this.verifyArray(merged.getColumnColourCodes(1, 0), new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		this.verifyObject(merged.getColumnChanged(1, 0), true);

		this.verifyObject(merged.getColumnCharacter(2, 0), null);
		this.verifyObject(merged.getColumnCharacterWidth(2, 0), 0);
		this.verifyArray(merged.getColumnColourCodes(2, 0), new int [] {});
		this.verifyObject(merged.getColumnChanged(2, 0), false);

		this.verifyObject(merged.getColumnCharacter(3, 0), null);
		this.verifyObject(merged.getColumnCharacterWidth(3, 0), 0);
		this.verifyArray(merged.getColumnColourCodes(3, 0), new int [] {});
		this.verifyObject(merged.getColumnChanged(3, 0), false);
        }

        public void testMergeIntoInactiveLayer() throws Exception{
		//  Test what happens when you merge down onto an inactive layer
		ScreenLayer merged = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));

		merged.initialize();
		merged.setIsLayerActive(false);
		merged.setAllChangedFlagStates(false);
		merged.setColumnCharacter(0, 0, "A");
		merged.setColumnCharacterWidth(0, 0, 1);
		merged.setColumnColourCodes(0, 0, new int [] {});
		merged.setColumnChanged(0, 0, true);
		merged.setColumnActive(0, 0, true);
		merged.setColumnCharacter(1, 0, "B");
		merged.setColumnCharacterWidth(1, 0, 1); 
		merged.setColumnColourCodes(1, 0, new int [] {});
		merged.setColumnChanged(1, 0, true);
		merged.setColumnActive(1, 0, false);
		merged.setColumnCharacter(2, 0, "C");
		merged.setColumnCharacterWidth(2, 0, 1); 
		merged.setColumnColourCodes(2, 0, new int [] {});
		merged.setColumnChanged(2, 0, false);
		merged.setColumnActive(2, 0, true);
		merged.setColumnCharacter(3, 0, "D");
		merged.setColumnCharacterWidth(3, 0, 1); 
		merged.setColumnColourCodes(3, 0, new int [] {});
		merged.setColumnChanged(3, 0, false);
		merged.setColumnActive(3, 0, false);

		ScreenLayer [] layers = new ScreenLayer [1];
		layers[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		layers[0].initialize();
		layers[0].setAllChangedFlagStates(false);
		layers[0].setColumnCharacter(0, 0, "A");
		layers[0].setColumnCharacterWidth(0, 0, 1);
		layers[0].setColumnColourCodes(0, 0, new int [] {});
		layers[0].setColumnChanged(0, 0, true);
		layers[0].setColumnActive(0, 0, true);
		layers[0].setColumnCharacter(1, 0, "B");
		layers[0].setColumnCharacterWidth(1, 0, 1); 
		layers[0].setColumnColourCodes(1, 0, new int [] {});
		layers[0].setColumnChanged(1, 0, true);
		layers[0].setColumnActive(1, 0, false);
		layers[0].setColumnCharacter(2, 0, "C");
		layers[0].setColumnCharacterWidth(2, 0, 1); 
		layers[0].setColumnColourCodes(2, 0, new int [] {});
		layers[0].setColumnChanged(2, 0, false);
		layers[0].setColumnActive(2, 0, true);
		layers[0].setColumnCharacter(3, 0, "D");
		layers[0].setColumnCharacterWidth(3, 0, 1); 
		layers[0].setColumnColourCodes(3, 0, new int [] {});
		layers[0].setColumnChanged(3, 0, false);
		layers[0].setColumnActive(3, 0, false);

		merged.mergeDown(layers, false);

		this.verifyObject(merged.getColumnCharacter(0, 0), "A");
		this.verifyObject(merged.getColumnCharacterWidth(0, 0), 1);
		this.verifyArray(merged.getColumnColourCodes(0, 0), new int [] {});
		this.verifyObject(merged.getColumnChanged(0, 0), true);
		this.verifyObject(merged.getColumnActive(0, 0), true);

		this.verifyObject(merged.getColumnCharacter(1, 0), "B");
		this.verifyObject(merged.getColumnCharacterWidth(1, 0), 1);
		this.verifyArray(merged.getColumnColourCodes(1, 0), new int [] {});
		this.verifyObject(merged.getColumnChanged(1, 0), true);
		this.verifyObject(merged.getColumnActive(1, 0), true);

		this.verifyObject(merged.getColumnCharacter(2, 0), "C");
		this.verifyObject(merged.getColumnCharacterWidth(2, 0), 1);
		this.verifyArray(merged.getColumnColourCodes(2, 0), new int [] {});
		this.verifyObject(merged.getColumnChanged(2, 0), false);
		this.verifyObject(merged.getColumnActive(2, 0), true);

		this.verifyObject(merged.getColumnCharacter(3, 0), "D");
		this.verifyObject(merged.getColumnCharacterWidth(3, 0), 1);
		this.verifyArray(merged.getColumnColourCodes(3, 0), new int [] {});
		this.verifyObject(merged.getColumnChanged(3, 0), false);
		this.verifyObject(merged.getColumnActive(3, 0), true);
        }

	public TestScreenCharacter getExpectedTestCharacter(MergedScreenInfo msi, boolean trustChangedFlags, boolean randomizedForcedBottomLayerState, ScreenLayer [] layers, List<Map<Coordinate, TestScreenCharacter>> layerCharacters, int l, Coordinate currentCoordinate, TestScreenCharacter firstColumnOfCharacter) throws Exception{

		//  Check to see if the current point is inside a
		//  changed region for at least one layer.  If so, return
		//  the default character from the bottom 'merged' layer:
		int topMostLayer = layers.length -1;

		if(l > 0){
			//  Other than clearing the change flags, there should be no changes 
			//  to the upper layers after a merge:
			Long xOffset = layers[l].getPlacementOffset().getX();
			Long yOffset = layers[l].getPlacementOffset().getY();
			Coordinate translatedCoordinate = currentCoordinate.changeByDeltaXY(
				xOffset,
				yOffset
			);
			boolean isInChangedRegion = ScreenLayer.isInChangedRegion(translatedCoordinate.getX().intValue(), translatedCoordinate.getY().intValue(), msi.getAllActiveTranslatedChangedRegions());
			TestScreenCharacter rtn = layerCharacters.get(l).get(currentCoordinate);
			//  Merging layers causes the changed flag to be unset:
			boolean updatedChangedFlag = false;
			if(!(layers[l].getIsLayerActive() && rtn.active && isInChangedRegion)){
				//  but it stays the same if it's inactive or not in a 'changed' region
				updatedChangedFlag = rtn.changed;
			}
			return new TestScreenCharacter(
				rtn.characters, // Characters
				rtn.characterWidths,    // Widths
				rtn.colourCodes,  // ColourCodes
				rtn.active,   //  Active
				updatedChangedFlag   //  Changed
			);
		}else{
			boolean isInChangedRegion = ScreenLayer.isInChangedRegion(currentCoordinate.getX().intValue(), currentCoordinate.getY().intValue(), msi.getAllActiveTranslatedClippedExpandedChangedRegions());

			Map<Coordinate, TestScreenCharacter> beforeMergeCharacters = msi.getBeforeMergeCharacters();
			TestScreenCharacter bottomCharacterBefore = beforeMergeCharacters.get(currentCoordinate);
			
			//  Get pre-calculated maps that describe the characters we expect to see:
			List<Map<Coordinate, TestScreenCharacter>> bottomRelativeCharacters = msi.getBottomRelativeCharacters();
			Map<Coordinate, Integer> topmostSolidActiveColumnLayers = msi.getTopmostSolidActiveColumnLayers();
			Map<Coordinate, Boolean> hasAboveActiveChangedFlags = msi.getHasAboveActiveChangedFlags();
			Map<Coordinate, Boolean> hasActiveFlags = msi.getHasActiveFlags();

			Map<Coordinate, Map<Integer, SolidColumnType>> isColumnSolidAndActiveStates = msi.getIsColumnSolidAndActiveStates();

			Map<Coordinate, Map<Integer, Boolean>> isColumnOccludedStates = msi.getIsColumnOccludedStates();

			Map<Coordinate, int []> allTopColourCodes = msi.getTopColourCodes();

			Integer topmostActiveColumnLayer = topmostSolidActiveColumnLayers.get(currentCoordinate);

			boolean hasAnyAboveActiveChangedFlag = hasAboveActiveChangedFlags.get(currentCoordinate);
			boolean hasAnyActiveCharacter = hasActiveFlags.get(currentCoordinate);
			int [] resultColourCodes = allTopColourCodes.get(currentCoordinate);

			String resultCharacters = null;
			int resultCharacterWidths = 0;
			boolean resultActiveFlag = hasAnyActiveCharacter;
			boolean trustedChangedFlag = hasAnyAboveActiveChangedFlag;
			if(topmostActiveColumnLayer == null){
				//  There is no active column 
			}else{
				SolidColumnType solidColumnType = isColumnSolidAndActiveStates.get(currentCoordinate).get(topmostActiveColumnLayer);
				if(
					isColumnOccludedStates.get(currentCoordinate).get(topmostActiveColumnLayer) ||
					solidColumnType.equals(SolidColumnType.SEVERED_RIGHT) ||
					solidColumnType.equals(SolidColumnType.SEVERED_LEFT)
				){
					//  Character was occluded, replace it with a space:
					resultCharacters = " ";
					resultCharacterWidths = 1;
					resultActiveFlag = true;
					trustedChangedFlag = true;  //  For now, just always set to true when occluded.
				}else{
					TestScreenCharacter topmostActiveColumn = bottomRelativeCharacters.get(topmostActiveColumnLayer).get(currentCoordinate);
					resultCharacters = topmostActiveColumn.characters;
					resultCharacterWidths = topmostActiveColumn.characterWidths;
					resultActiveFlag = true;
				}
			}

			boolean calculatedChangedFlag = !(  //  If there is an actual change in the before/after character:
				(bottomCharacterBefore.characterWidths == resultCharacterWidths) &&
				Arrays.equals(bottomCharacterBefore.colourCodes, resultColourCodes) &&
				Objects.equals(bottomCharacterBefore.characters, resultCharacters)
			);

			//  When calculating changed flags for the trailing null characters in a 
			//  multi-column character, use the changed flags from the first column
			//  where the change will actually appear:
			if(firstColumnOfCharacter != null){
				calculatedChangedFlag = firstColumnOfCharacter.changed;
				resultColourCodes = firstColumnOfCharacter.colourCodes;
				trustedChangedFlag = firstColumnOfCharacter.changed;
			}

			boolean resultChangedFlag = (
				trustChangedFlags ?
				(trustedChangedFlag) :
				(
					calculatedChangedFlag ||
					bottomCharacterBefore.changed //  OR, if there was a pending changed flag on this character before
				)
			);

			if(isInChangedRegion){
				//  Whatever the calculated merge was:
				return new TestScreenCharacter(
					resultCharacters,
					resultCharacterWidths,
					resultColourCodes,
					resultActiveFlag,
					resultChangedFlag
				);
			}else{
				//  It's just the same character as before the merge, since the real merge
				//  algorithm we're testing is not even supposed to touch
				//  this area because it's outside a change region:
				return bottomCharacterBefore;
			}
		}
	}

	public int [] makeRandomColourCodes(Random rand){
		int numColours = (int)getRandBetweenRange(rand, 0L, 2L);
		int [] rtn = new int [numColours];
		for(int i = 0; i < numColours; i++){
			rtn[i] = (int)getRandBetweenRange(rand, 40L, 48L);
			
		}
		return rtn;
	}

	public void printScaleForScreenLayer(ScreenLayer layer, int numSpaces){
		int width = layer.getWidth();
		int height = layer.getHeight();
		String [] partNames = new String[]{
			"Characters",
			"Active States",
			"Changed Flags",
			"Changed Regions"
		};
		int numParts = partNames.length;

		String partSeparator = "";
		for(int i = 0; i < numSpaces; i++){
			partSeparator += " ";
		}

		for(int i = 0; i < numParts; i++){
			System.out.print(partNames[i]);
			if(i != (numParts - 1)){
				System.out.print(", ");
			}
		}
		System.out.print("\n");

		for(int part = 0; part < numParts; part++){
			for(int i = 0; i < width; i++){
				String chr = (i == 0) ? ("  " + (i % 10)) : ("" + (i % 10));
				System.out.print(chr);
			}
			System.out.print(" " + partSeparator);
		}
		System.out.print("\n");
		for(int part = 0; part < numParts; part++){
			for(int i = 0; i < width + 1; i++){
				String chr = (i == 0) ? " +" : "-";
				System.out.print(chr);
			}
			System.out.print(" " + partSeparator);
		}
		System.out.print("\n");
		for(int j = 0; j < height; j++){
			for(int part = 0; part < numParts; part++){
				String chr = (j % 10)+"|";
				System.out.print(chr);
				for(int i = 0; i < width; i++){
					System.out.print(" ");
				}
				System.out.print("|" + partSeparator);
			}
			System.out.print("\n");
		}
	}

	private boolean canFitCharacterAtCoordinate(Map<Coordinate, TestScreenCharacter> layerCharacters, int printedCharacterWidth, Coordinate startCoordinate, ScreenLayer layer) throws Exception{
		int columnWidth = printedCharacterWidth == 0 ? 1 : printedCharacterWidth;
		if(startCoordinate.getX() + (columnWidth-1) < layer.getWidth()){
			for(int i = 0; i < columnWidth; i++){
				Coordinate currentCoordinate = startCoordinate.changeByDeltaX((long)i);
				if(layerCharacters.containsKey(currentCoordinate)){
					//System.out.println("Discarding proposed character of width=" + printedCharacterWidth + " at startCoordinate=" + startCoordinate + " because it does not fit.");
					return false;  //  Proposed new character would overlap an existing one.
				}
			}
		}else{
			//System.out.println("Discarding proposed character of width=" + printedCharacterWidth + " at startCoordinate=" + startCoordinate + " because it goes beyond the end of the layer which has width=" + layer.getWidth());
			return false;  //  Character would go beyond layer boundary
		}
		return true; //  It will fit.
	}

        public void printRandomCharactersTest() throws Exception{
		int startingSeed = 1;
		int numDifferentSeeds = 50000;
		int numTestCharacters = 20;
		int maxNumChangedRegions = 5;
		int maxNumLayers = 5;
		Long maxCharacterWidth = 6L;
		boolean randomizePlacementOffset = true;
		Long maxLayerWidth = 10L;
		Long maxLayerHeight = 10L;
		Long placementOffsetXMax = randomizePlacementOffset ? 5L : 0L;
		Long placementOffsetYMax = randomizePlacementOffset ? 5L : 0L;
		for(int currentSeed = startingSeed; currentSeed < (startingSeed + numDifferentSeeds); currentSeed++){
			Random rand = new Random(currentSeed);
			boolean randomizedForcedBottomLayerState = this.getRandomBoolean(rand);
			boolean trustChangedFlags = this.getRandomBoolean(rand);
			System.out.println("Begin testing with seed=" + currentSeed + " and trustChangedFlags=" + trustChangedFlags + ", randomizedForcedBottomLayerState=" + randomizedForcedBottomLayerState);
			int numLayers = rand.nextInt(maxNumLayers) + 1;

			ScreenLayer [] allLayers = new ScreenLayer [numLayers];
			List<Map<Coordinate, TestScreenCharacter>> layerCharacters = new ArrayList<Map<Coordinate, TestScreenCharacter>>();

			//  Randomly set all layers to have random characters:
			for(int l = 0; l < numLayers; l++){
				int layerWidth = (int)getRandBetweenRange(rand, 0L, maxLayerWidth + 1L);
				int layerHeight = (int)getRandBetweenRange(rand, 0L, maxLayerHeight + 1L);
				layerCharacters.add(new HashMap<Coordinate, TestScreenCharacter>());

				//  This dictates the offset where the layer will be merged in:
				Long xPlacementOffset = getRandBetweenRange(rand, -placementOffsetXMax, placementOffsetXMax);
				Long yPlacementOffset = getRandBetweenRange(rand, -placementOffsetXMax, placementOffsetXMax);
				Coordinate layerPlacementOffset = new Coordinate(Arrays.asList(xPlacementOffset, yPlacementOffset));

				//  Apply a random offset to the 'region' that this layer should cover
				//  because this case needs to be handled generically as the screen 'layer'
				//  needs to be able to handle efficient movement through another data 
				//  structure like the map area which can have arbitrarily high coordinates:
				Long randomizedRegionOffsetX = getRandBetweenRange(rand, -10L, 10L);
				Long randomizedRegionOffsetY = getRandBetweenRange(rand, -10L, 10L);

				CuboidAddress layerBackingAddress = new CuboidAddress(
					new Coordinate(Arrays.asList(randomizedRegionOffsetX, randomizedRegionOffsetY)),
					new Coordinate(Arrays.asList(randomizedRegionOffsetX + (long)layerWidth, randomizedRegionOffsetY + (long)layerHeight))
				);

				//  One in 5 chance of being inactive:
				boolean isLayerActive = !(getRandBetweenRange(rand, 0L, 5L) == 0L);
				allLayers[l] = new ScreenLayer(layerPlacementOffset, layerBackingAddress);
				allLayers[l].initialize();
                		allLayers[l].setIsLayerActive(isLayerActive);
				allLayers[l].setAllChangedFlagStates(false);
                		allLayers[l].clearChangedRegions();

				int numChangedRegions = rand.nextInt(maxNumChangedRegions + 1);
				Set<ScreenRegion> changedRegionsToAdd = new TreeSet<ScreenRegion>();
				for(int n = 0; n < numChangedRegions; n++){
					int x1 = rand.nextInt(layerWidth + 1);
					int x2 = rand.nextInt(layerWidth + 1);
					int y1 = rand.nextInt(layerHeight + 1);
					int y2 = rand.nextInt(layerHeight + 1);
					int xMin = Math.min(x1, x2);
					int yMin = Math.min(y1, y2);
					int xMax = Math.max(x1, x2);
					int yMax = Math.max(y1, y2);
					changedRegionsToAdd.add(
						new ScreenRegion(ScreenLayer.makeDimensionsCA(xMin, yMin, xMax, yMax))
					);
				}
				allLayers[l].addChangedRegions(changedRegionsToAdd);

				//  Put some random characters in the test region:
				if(allLayers[l].getWidth() > 0 && allLayers[l].getHeight() > 0){
					for(int n = 0; n < numTestCharacters; n++){
						int x = (int)getRandBetweenRange(rand, 0L, (long)allLayers[l].getWidth());
						int y = (int)getRandBetweenRange(rand, 0L, (long)allLayers[l].getHeight());
						Coordinate randomCoordinate = new Coordinate(Arrays.asList((long)x, (long)y));
						int randomCodePoint = (int)getRandBetweenRange(rand, 97L, 113L);
						boolean currentChanged = this.getRandomBoolean(rand);
						//  One in 3 chance of being inactive:
						boolean currentActive = !(getRandBetweenRange(rand, 0L, 3L) == 0L);
						int randomCharacterWidth = (int)getRandBetweenRange(rand, 0L, maxCharacterWidth + 1L);
						int [] currentColourCodes = randomCharacterWidth == 0 ? new int []{} : this.makeRandomColourCodes(rand);
						int iterationWidth = randomCharacterWidth == 0 ? 1 : randomCharacterWidth;
						boolean fits = this.canFitCharacterAtCoordinate(layerCharacters.get(l), randomCharacterWidth, randomCoordinate, allLayers[l]);
						if(fits){
							for(int i = 0; i < iterationWidth; i++){
								//  Actual character is specified in first column:
								String firstColumnCharacters = randomCharacterWidth == 0 ? null : Character.toString(randomCodePoint);
								String currentCharacters = i == 0 ? firstColumnCharacters : null;
								allLayers[l].setColumnCharacter(x+i, y, currentCharacters);
								//  Character width is only specifed in first column:
								int currentCharacterWidth = i == 0 ? randomCharacterWidth : 0;
								allLayers[l].setColumnCharacterWidth(x+i, y, currentCharacterWidth);
								//  Every other column should have the same values for these members:
								allLayers[l].setColumnColourCodes(x+i, y, currentColourCodes);
								allLayers[l].setColumnActive(x+i, y, currentActive);
								allLayers[l].setColumnChanged(x+i, y, currentChanged);

								TestScreenCharacter cc = new TestScreenCharacter(currentCharacters, currentCharacterWidth, currentColourCodes, currentActive, currentChanged);
								Coordinate currentCoordinate = randomCoordinate.changeByDeltaX((long)i);
								layerCharacters.get(l).put(currentCoordinate, cc);
							}
						}//  Otherwise, discard this character.
					}
				}
				//  Fill up any blank spots where there are no characters:
				for(int x = 0; x < allLayers[l].getWidth(); x++){
					for(int y = 0; y < allLayers[l].getHeight(); y++){
						Coordinate currentCoordinate = new Coordinate(Arrays.asList((long)x, (long)y));
						if(!layerCharacters.get(l).containsKey(currentCoordinate)){
							TestScreenCharacter cc = new TestScreenCharacter(
								null, // Characters
								0, //Character widths
								new int []{}, // Character codes
								false, //active
								false //changed
							);
							layerCharacters.get(l).put(currentCoordinate, cc);
						}
					}
				}
			}

			// The topmost layers are the ones that will be merged down onto the bottom layer:
			ScreenLayer [] aboveLayers = new ScreenLayer[numLayers -1];
			for(int i = 0; i < numLayers -1; i++){
				aboveLayers[i] = allLayers[i+1];
			}

			//  Pick out the result merged layer from our test cases:
			ScreenLayer bottomLayer = allLayers[0];


			int xDrawOffset = 2;
			int yDrawOffset = 2;
			int partsSpacing = 5;
			for(int i = numLayers -1; i >= 0; i--){
				System.out.print("Here is input layer " + i + ", width=" + allLayers[i].getWidth() + ", height=" + allLayers[i].getHeight() + " which as positionOffset=" + allLayers[i].getPlacementOffset() + ":\n");
				System.out.print("\n");
				this.printScaleForScreenLayer(allLayers[i], partsSpacing);
				System.out.print("\033[" + (allLayers[i].getHeight()) + "A"); //  Move cursor back up
				allLayers[i].printDebugStates(xDrawOffset + (0*(allLayers[i].getWidth() + partsSpacing + 3)), yDrawOffset, "characters");
				System.out.print("\033[" + (allLayers[i].getHeight()) + "A"); //  Move cursor back up
				allLayers[i].printDebugStates(xDrawOffset + (1*(allLayers[i].getWidth() + partsSpacing + 3)), yDrawOffset, "active");
				System.out.print("\033[" + (allLayers[i].getHeight()) + "A"); //  Move cursor back up
				allLayers[i].printDebugStates(xDrawOffset + (2*(allLayers[i].getWidth() + partsSpacing + 3)), yDrawOffset, "changed");
				System.out.print("\033[" + (allLayers[i].getHeight()) + "A"); //  Move cursor back up
				allLayers[i].printDebugStates(xDrawOffset + (3*(allLayers[i].getWidth() + partsSpacing + 3)), yDrawOffset, "in_changed_region");
				System.out.print("\n");

				String error = allLayers[i].validate();
				if(error != null){
					throw new Exception("Validation above layer failed: " + error);
				}
			}
			
			MergedScreenInfo msi = new MergedScreenInfo(allLayers, layerCharacters, randomizedForcedBottomLayerState);
			msi.init(); //  Initialize before merge to save a copy of before characters
			//  Do the actual merge process:

			ScreenLayerMergeType mergeType = randomizedForcedBottomLayerState ? ScreenLayerMergeType.PREFER_BOTTOM_LAYER : ScreenLayerMergeType.PREFER_INPUT_TRANSPARENCY;
			bottomLayer.mergeDown(aboveLayers, trustChangedFlags, mergeType);


			System.out.print("Here is the resulting merged layer:\n");
			System.out.print("\n");
			this.printScaleForScreenLayer(bottomLayer, partsSpacing);
			System.out.print("\033[" + (bottomLayer.getHeight()) + "A"); //  Move cursor back up
			bottomLayer.printDebugStates(xDrawOffset + (0*(bottomLayer.getWidth() + partsSpacing + 3)), yDrawOffset, "characters");
			System.out.print("\033[" + (bottomLayer.getHeight()) + "A"); //  Move cursor back up
			bottomLayer.printDebugStates(xDrawOffset + (1*(bottomLayer.getWidth() + partsSpacing + 3)), yDrawOffset, "active");
			System.out.print("\033[" + (bottomLayer.getHeight()) + "A"); //  Move cursor back up
			bottomLayer.printDebugStates(xDrawOffset + (2*(bottomLayer.getWidth() + partsSpacing + 3)), yDrawOffset, "changed");
			System.out.print("\033[" + (bottomLayer.getHeight()) + "A"); //  Move cursor back up
			bottomLayer.printDebugStates(xDrawOffset + (3*(bottomLayer.getWidth() + partsSpacing + 3)), yDrawOffset, "in_changed_region");
			System.out.print("\n");


			//  Verify that everything in the merged layer is exactly as it's supposed to be:
			for(int l = 0; l < numLayers; l++){
				for(int y = 0; y < allLayers[l].getHeight(); y++){
					TestScreenCharacter firstColumnOfCharacter = null;
					int columnsLeftInCharacter = 0;
					for(int x = 0; x < allLayers[l].getWidth(); x++){
						Coordinate currentCoordinate = new Coordinate(Arrays.asList((long)x, (long)y));
						TestScreenCharacter ref = columnsLeftInCharacter > 0 ? firstColumnOfCharacter : null;
						TestScreenCharacter cc = this.getExpectedTestCharacter(msi, trustChangedFlags, randomizedForcedBottomLayerState, allLayers, layerCharacters, l, currentCoordinate, ref);
						if(columnsLeftInCharacter <= 0 && cc.characterWidths > 0){
							firstColumnOfCharacter = cc;
							columnsLeftInCharacter = cc.characterWidths;
						}

						String msg = "currentCoordinate=" + currentCoordinate + ", trustChangedFlags=" + trustChangedFlags + ", randomizedForcedBottomLayerState=" + randomizedForcedBottomLayerState +  ", x=" + x + ", y=" + y + ", l="+l;
						this.verifyObject(allLayers[l].getColumnCharacter(x, y), cc.characters, msg);
						this.verifyObject(allLayers[l].getColumnCharacterWidth(x, y), cc.characterWidths, msg);
						this.verifyArray(allLayers[l].getColumnColourCodes(x, y), cc.colourCodes, msg);
						this.verifyObject(allLayers[l].getColumnChanged(x, y), cc.changed, msg);
						this.verifyObject(allLayers[l].getColumnActive(x, y), cc.active, msg);
						columnsLeftInCharacter--;
					}
				}
			}

			String afterMergeError = bottomLayer.validate();
			if(afterMergeError != null){
				throw new Exception("Validation after merge failed: " + afterMergeError);
			}
		}
        }

	@Test
	public void runScreenLayerTest() throws Exception {
		System.out.println("Begin runScreenLayerTest:");

		this.mergeChangesTest1();
		this.mergeChangesTest2();
		this.mergeChangesTest3();
		this.mergeChangesTest4();
		this.mergeChangesTest5();
		this.mergeChangesTest6();

		this.testSimpleTwoColumn();
		this.testAscendingMultiColumn();
		this.testDescendingMultiColumn();
		this.twoColumnOverOneColumn();
		this.testMergeDownNoNeedToPrint();
		this.testInheritBackgroundBelow();
		this.testIgnoreBackgroundFlaggedFalse();
		this.testPartiallyCoveredCharacterBackground();
		this.testMergeIntoInactiveLayer();
	}

	@Test
	public void runScreenPrintingTest() throws Exception {
        	//  Commenting this out because it breaks the summary output from the unit tests.
        	//this.writeBorderTwiceNoChange();
	}

	@Test
	public void runScreenLayerFuzzTest() throws Exception {
		System.out.println("Begin runScreenLayerFuzzTest:");
		this.printRandomCharactersTest();
		System.out.println("End runScreenLayerFuzzTest:");
	}

	@Test
	public void runScreenLayerValidationTest() throws Exception {
		System.out.println("Begin runScreenLayerValidationTest:");
		ScreenLayer [] passingLayers = new ScreenLayer [1];
		passingLayers[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		passingLayers[0].initialize();
		passingLayers[0].setAllChangedFlagStates(false);
		passingLayers[0].setColumnCharacter(0, 0, " ");
		passingLayers[0].setColumnCharacterWidth(0, 0, 1);
		passingLayers[0].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		passingLayers[0].setColumnCharacter(1, 0, " ");
		passingLayers[0].setColumnCharacterWidth(1, 0, 1);
		passingLayers[0].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});

		for(int i = 0; i < passingLayers.length; i++){
			if(!(passingLayers[i].validate() == null)){
				throw new Exception("Test " + i + ", this layer should have validated: " + passingLayers[i].validate());
			}else{
				System.out.println("test " + i + " validated.");
			}
		}

		ScreenLayer [] failingLayers = new ScreenLayer [9];
		//  Test invalid widths
		failingLayers[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		failingLayers[0].initialize();
		failingLayers[0].setAllChangedFlagStates(false);
		failingLayers[0].setColumnCharacter(0, 0, " ");
		failingLayers[0].setColumnCharacterWidth(0, 0, 2);
		failingLayers[0].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		failingLayers[0].setColumnCharacter(1, 0, null);
		failingLayers[0].setColumnCharacterWidth(1, 0, 2);
		failingLayers[0].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});


		//  Test inconsistent colour codes
		failingLayers[1] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		failingLayers[1].initialize();
		failingLayers[1].setAllChangedFlagStates(false);
		failingLayers[1].setColumnCharacter(0, 0, " ");
		failingLayers[1].setColumnCharacterWidth(0, 0, 2);
		failingLayers[1].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		failingLayers[1].setColumnCharacter(1, 0, null);
		failingLayers[1].setColumnCharacterWidth(1, 0, 0);
		failingLayers[1].setColumnColourCodes(1, 0, new int [] {});


		//  Test non-null characters inside a multi-column character:
		failingLayers[2] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		failingLayers[2].initialize();
		failingLayers[2].setAllChangedFlagStates(false);
		failingLayers[2].setColumnCharacter(0, 0, " ");
		failingLayers[2].setColumnCharacterWidth(0, 0, 2);
		failingLayers[2].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		failingLayers[2].setColumnCharacter(1, 0, " ");
		failingLayers[2].setColumnCharacterWidth(1, 0, 0);
		failingLayers[2].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});

		//  Test inconsistent changed state
		failingLayers[3] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		failingLayers[3].initialize();
		failingLayers[3].setAllChangedFlagStates(false);
		failingLayers[3].setColumnCharacter(0, 0, " ");
		failingLayers[3].setColumnCharacterWidth(0, 0, 2);
		failingLayers[3].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		failingLayers[3].setColumnChanged(0, 0, false);
		failingLayers[3].setColumnCharacter(1, 0, null);
		failingLayers[3].setColumnCharacterWidth(1, 0, 0);
		failingLayers[3].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		failingLayers[3].setColumnChanged(1, 0, true);

		//  Test inconsistent active state
		failingLayers[4] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		failingLayers[4].initialize();
		failingLayers[4].setAllChangedFlagStates(false);
		failingLayers[4].setColumnCharacter(0, 0, " ");
		failingLayers[4].setColumnCharacterWidth(0, 0, 2);
		failingLayers[4].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		failingLayers[4].setColumnActive(0, 0, false);
		failingLayers[4].setColumnCharacter(1, 0, null);
		failingLayers[4].setColumnCharacterWidth(1, 0, 0);
		failingLayers[4].setColumnColourCodes(1, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		failingLayers[4].setColumnActive(1, 0, true);

		//  Test null colour codes
		failingLayers[5] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		failingLayers[5].initialize();
		failingLayers[5].setAllChangedFlagStates(false);
		failingLayers[5].setColumnCharacter(0, 0, " ");
		failingLayers[5].setColumnCharacterWidth(0, 0, 1);
		failingLayers[5].setColumnColourCodes(0, 0, null);


		//  Test blank null character having a colour code:
		failingLayers[6] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		failingLayers[6].initialize();
		failingLayers[6].setAllChangedFlagStates(false);
		failingLayers[6].setColumnCharacter(0, 0, null);
		failingLayers[6].setColumnCharacterWidth(0, 0, 0);
		failingLayers[6].setColumnColourCodes(0, 0, new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});


		//  Test blank null character having null colour codes:
		failingLayers[7] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		failingLayers[7].initialize();
		failingLayers[7].setAllChangedFlagStates(false);
		failingLayers[7].setColumnCharacter(0, 0, null);
		failingLayers[7].setColumnCharacterWidth(0, 0, 0);
		failingLayers[7].setColumnColourCodes(0, 0, null);


		//  Test non-zero width character with null characters
		failingLayers[8] = new ScreenLayer(new Coordinate(Arrays.asList(0L,0L)), ScreenLayer.makeDimensionsCA(0, 0, 4, 1));
		failingLayers[8].initialize();
		failingLayers[8].setAllChangedFlagStates(false);
		failingLayers[8].setColumnCharacter(0, 0, null);
		failingLayers[8].setColumnCharacterWidth(0, 0, 1);
		failingLayers[8].setColumnColourCodes(0, 0, new int [] {});

		for(int i = 0; i < failingLayers.length; i++){
			if(failingLayers[i].validate() != null){
				System.out.println("Test " + i + ", this layer correctly failed validation: " + failingLayers[i].validate());
			}else{
				throw new Exception("test " + i + " validated when it should not have.");
			}
		}

		System.out.println("End runScreenLayerValidationTest:");
	}



	public void doRegionExpansionTest(ScreenLayer [] layers, ScreenRegion inputScreenRegion, ScreenRegion expectedScreenRegion, String message) throws Exception{
		ScreenRegion expandedChangeRegion = ScreenLayer.getNonCharacterCuttingChangedRegions(inputScreenRegion, layers);

		CuboidAddress observedRegion = expandedChangeRegion.getRegion();
		CuboidAddress expectedRegion = expectedScreenRegion.getRegion();
		if(!observedRegion.equals(expectedRegion)){
			throw new Exception("Expected expanded region to be " + expectedRegion + " but it was " + observedRegion + " for test message \"" + message + "\".");
		}else{
			System.out.println("Correctly observed expanded region to be " + expectedRegion + " and it was " + observedRegion + " for test message \"" + message + "\".");
		}
	}

	public void expandChangeRegionTest() throws Exception{


		//  Test 1
		ScreenLayer [] test1 = new ScreenLayer [1];
		test1[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 1, 1));
		test1[0].initialize();
		test1[0].clearChangedRegions();
		test1[0].setMultiColumnCharacter(0, 0, " ", 1, new int [] {});

		doRegionExpansionTest(
			test1,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 1, 1)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 1, 1)),
			"Test for simplest case of no expansion necessary"
		);

		//  Test 2 
		ScreenLayer [] test2 = new ScreenLayer [1];
		test2[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 2, 1));
		test2[0].initialize();
		test2[0].clearChangedRegions();
		test2[0].setMultiColumnCharacter(0, 0, "A", 2, new int [] {});

		doRegionExpansionTest(
			test2,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 1, 1)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 2, 1)),
			"Test for simplest case of expansion necessary due to multi-column character"
		);

		//  Test 3
		ScreenLayer [] test3 = new ScreenLayer [2];
		test3[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 1));
		test3[0].initialize();
		test3[0].clearChangedRegions();
		test3[0].setMultiColumnCharacter(0, 0, "A", 2, new int [] {});

		test3[1] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 1));
		test3[1].initialize();
		test3[1].clearChangedRegions();
		test3[1].setMultiColumnCharacter(1, 0, "A", 2, new int [] {});

		doRegionExpansionTest(
			test3,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 1, 1)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 3, 1)),
			"Test for another expansion that is necessary on right edge due to char in same y line:"
		);


		//  Test 4
		ScreenLayer [] test4 = new ScreenLayer [2];
		test4[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 2, 1));
		test4[0].initialize();
		test4[0].clearChangedRegions();
		test4[0].setMultiColumnCharacter(0, 0, "A", 2, new int [] {});

		test4[1] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 1));
		test4[1].initialize();
		test4[1].clearChangedRegions();
		test4[1].setMultiColumnCharacter(1, 0, "A", 2, new int [] {});

		doRegionExpansionTest(
			test4,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 1, 1)),
			//  Region considered should go beyond bottom layer boundary:
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 3, 1)),
			"Test for another expansion that is necessary on right edge due to char in same y line, BUT expansion goes beyond the edge of the bottom layer due to the last character going off the edge."
		);


		//  Test 5
		ScreenLayer [] test5 = new ScreenLayer [2];
		test5[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 2));
		test5[0].initialize();
		test5[0].clearChangedRegions();
		test5[0].setMultiColumnCharacter(0, 0, "A", 2, new int [] {});

		test5[1] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 2));
		test5[1].initialize();
		test5[1].clearChangedRegions();
		test5[1].setMultiColumnCharacter(1, 1, "A", 2, new int [] {});

		doRegionExpansionTest(
			test5,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 1, 2)),
			//  Region should be restriced to bottom layer boundary:
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 3, 2)),
			"Test for second expansion caused by character a y line that is not the first."
		);

		//  Test 6
		ScreenLayer [] test6 = new ScreenLayer [1];
		test6[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 3));
		test6[0].initialize();
		test6[0].clearChangedRegions();
		test6[0].setMultiColumnCharacter(1, 1, " ", 1, new int [] {});

		doRegionExpansionTest(
			test6,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(1, 1, 2, 2)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(1, 1, 2, 2)),
			"No expansion necessary, character in center of layer."
		);

		//  Test 7
		ScreenLayer [] test7 = new ScreenLayer [1];
		test7[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 3));
		test7[0].initialize();
		test7[0].clearChangedRegions();
		test7[0].setMultiColumnCharacter(1, 1, " ", 1, new int [] {});
		test7[0].setMultiColumnCharacter(0, 2, "A", 2, new int [] {});

		doRegionExpansionTest(
			test7,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(1, 1, 2, 3)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 1, 2, 3)),
			"Test left boundary expansion for character in same layer, next row."
		);

		//  Test 8
		ScreenLayer [] test8 = new ScreenLayer [2];
		test8[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 2));
		test8[0].initialize();
		test8[0].clearChangedRegions();
		test8[0].setMultiColumnCharacter(1, 0, "A", 2, new int [] {});

		test8[1] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 2));
		test8[1].initialize();
		test8[1].clearChangedRegions();
		test8[1].setMultiColumnCharacter(0, 1, "A", 2, new int [] {});

		doRegionExpansionTest(
			test8,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(2, 0, 3, 2)),
			//  Region should be restriced to bottom layer boundary:
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 3, 2)),
			"Test for leftward second expansion caused by character a y line that is not the first."
		);


		//  Test 9
		ScreenLayer [] test9 = new ScreenLayer [3];
		test9[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 10, 3));
		test9[0].initialize();
		test9[0].clearChangedRegions();
		test9[0].setMultiColumnCharacter(0, 0, "A", 2, new int [] {});
                test9[0].setPlacementOffset(new Coordinate(Arrays.asList(99L,99L))); //Should be ignored

		test9[1] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 10, 3));
		test9[1].initialize();
		test9[1].clearChangedRegions();
		test9[1].setMultiColumnCharacter(0, 0, "A", 5, new int [] {});
                test9[1].setPlacementOffset(new Coordinate(Arrays.asList(1L, 1L)));

		test9[2] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 10, 3));
		test9[2].initialize();
		test9[2].clearChangedRegions();
		test9[2].setMultiColumnCharacter(1, 0, "A", 4, new int [] {});
                test9[2].setPlacementOffset(new Coordinate(Arrays.asList(3L, 1L)));

		doRegionExpansionTest(
			test9,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(1, 0, 2, 2)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 8, 2)),
			"Test expansion with placement offsets."
		);

		//  Test 10
		ScreenLayer [] test10 = new ScreenLayer [3];
		test10[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 10, 3));
		test10[0].initialize();
		test10[0].clearChangedRegions();
		test10[0].setMultiColumnCharacter(5, 0, "A", 2, new int [] {});
                test10[0].setPlacementOffset(new Coordinate(Arrays.asList(88L,88L))); //Should be ignored

		test10[1] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 10, 3));
		test10[1].initialize();
		test10[1].clearChangedRegions();
		test10[1].setMultiColumnCharacter(2, 0, "A", 5, new int [] {});
                test10[1].setPlacementOffset(new Coordinate(Arrays.asList(1L, 1L)));

		test10[2] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 10, 3));
		test10[2].initialize();
		test10[2].clearChangedRegions();
		test10[2].setMultiColumnCharacter(1, 0, "A", 9, new int [] {});
                test10[2].setPlacementOffset(new Coordinate(Arrays.asList(-1L, 1L)));

		doRegionExpansionTest(
			test10,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(5, 0, 6, 2)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 9, 2)),
			"Test expansion with placement offsets, and multiple layers."
		);


		//  Test 11
		ScreenLayer [] test11 = new ScreenLayer [1];
		test11[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 1, 1));
		test11[0].initialize();
		test11[0].clearChangedRegions();
		test11[0].setToEmpty(0, 0);

		doRegionExpansionTest(
			test11,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 0, 1)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 0, 1)),
			"Change region with 0 x width, 1 width on a single null character."
		);


		//  Test 12
		ScreenLayer [] test12 = new ScreenLayer [2];
		test12[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 0, 0));
		test12[0].initialize();
		test12[0].clearChangedRegions();

		test12[1] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 1, 1));
		test12[1].initialize();
		test12[1].clearChangedRegions();
		test12[1].setToEmpty(0, 0);

		doRegionExpansionTest(
			test12,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 1, 1)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 1, 1)),
			"Two layers, but bottom layer has zero size and top layer is a null character."
		);

		//  Test 13
		ScreenLayer [] test13 = new ScreenLayer [1];
		test13[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 100, 1));
		test13[0].initialize();
		test13[0].clearChangedRegions();
		test13[0].setMultiColumnCharacter(0, 0, "A", 2, new int [] {});

		doRegionExpansionTest(
			test13,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 1, 1)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 2, 1)),
			"Test for correct expansion for region covering first part of multi-column character."
		);

		//  Test 14
		ScreenLayer [] test14 = new ScreenLayer [1];
		test14[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 100, 1));
		test14[0].initialize();
		test14[0].clearChangedRegions();
		test14[0].setMultiColumnCharacter(0, 0, "A", 2, new int [] {});

		doRegionExpansionTest(
			test14,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(1, 0, 2, 1)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 2, 1)),
			"Test for correct expansion for region covering second part of multi-column character."
		);

		//  Test 15
		ScreenLayer [] test15 = new ScreenLayer [1];
		test15[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 100, 1));
		test15[0].initialize();
		test15[0].clearChangedRegions();
		test15[0].setMultiColumnCharacter(0, 0, " ", 100, new int [] {});

		doRegionExpansionTest(
			test15,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(3, 0, 4, 1)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 100, 1)),
			"Test for correct expansion for region in middle of wide character."
		);

		//  Test 16
		ScreenLayer [] test16 = new ScreenLayer [1];
		test16[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 1));
		test16[0].initialize();
		test16[0].clearChangedRegions();
		test16[0].setMultiColumnCharacter(0, 0, " ", 1, new int [] {});
		test16[0].setMultiColumnCharacter(1, 0, " ", 1, new int [] {});
		test16[0].setMultiColumnCharacter(2, 0, " ", 1, new int [] {});

		doRegionExpansionTest(
			test16,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 1, 1)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, 1, 1)),
			"Test for no change for first space character."
		);

		doRegionExpansionTest(
			test16,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(1, 0, 2, 1)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(1, 0, 2, 1)),
			"Test for no change for second space character."
		);

		doRegionExpansionTest(
			test16,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(2, 0, 3, 1)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(2, 0, 3, 1)),
			"Test for no change for third space character."
		);


		//  Test 17
		ScreenLayer [] test17 = new ScreenLayer [1];
		test17[0] = new ScreenLayer(new Coordinate(Arrays.asList(0L, 0L)), ScreenLayer.makeDimensionsCA(0, 0, 3, 1));
		test17[0].initialize();
		test17[0].clearChangedRegions();
		test17[0].setMultiColumnCharacter(0, 0, " ", 1, new int [] {});
		test17[0].setToEmpty(1, 0);
		test17[0].setMultiColumnCharacter(2, 0, " ", 1, new int [] {});

		doRegionExpansionTest(
			test17,
			new ScreenRegion(ScreenLayer.makeDimensionsCA(1, 0, 2, 1)),
			new ScreenRegion(ScreenLayer.makeDimensionsCA(1, 0, 2, 1)),
			"Test for no change when only over a null character that's beside a 1 width character."
		);
	}

	@Test
	public void runExpandChangeRegionTest() throws Exception {
		System.out.println("Start runExpandChangeRegionTest:");
		this.expandChangeRegionTest();
		System.out.println("End runExpandChangeRegionTest:");
	}
}
