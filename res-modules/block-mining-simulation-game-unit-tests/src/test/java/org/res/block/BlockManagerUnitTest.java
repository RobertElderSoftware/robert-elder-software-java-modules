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
		if(!Objects.equals(observed, expected)){
			throw new Exception("Expected object was '" + String.valueOf(expected) + "', but saw '" + String.valueOf(observed) + "' instead.");
		}
	}

	public void verifyArray(Object [] observed, Object [] expected) throws Exception{
		if(!Arrays.equals(observed, expected)){
			throw new Exception("Expected array was '" + String.valueOf(expected) + "', but saw '" + String.valueOf(observed) + "' instead.");
		}
	}

	public void verifyArray(int [] observed, int [] expected) throws Exception{
		if(!Arrays.equals(observed, expected)){
			throw new Exception("Expected array was '" + Arrays.toString(expected) + "', but saw '" + Arrays.toString(observed) + "' instead.");
		}
	}

	public void mergeChangesTest1() throws Exception{
		//  Simple merge down test with a change.
		ScreenLayer t = new ScreenLayer(1, 1);
		t.initialize();
		t.characters[0][0] = "A";
		t.characterWidths[0][0] = 1;
		t.colourCodes[0][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
		t.changed[0][0] = true;
		t.active[0][0] = true;
		t.addChangedRegion(new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, t.getWidth(), t.getHeight())));

		ScreenLayer merged = new ScreenLayer(1, 1);
		merged.initialize();
		merged.mergeChanges(t, 0L, 0L);
		this.verifyObject(merged.characters[0][0], "A");
		this.verifyObject(merged.characterWidths[0][0], 1);
		this.verifyArray(merged.colourCodes[0][0], new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.changed[0][0], true);
	}

	public void mergeChangesTest2() throws Exception{
		//  Simple merge down test with no change flag set with region;  Should get ignored:
		ScreenLayer t = new ScreenLayer(1, 1);
		t.initialize();
		t.characters[0][0] = "A";
		t.characterWidths[0][0] = 1;
		t.colourCodes[0][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
		t.changed[0][0] = false;
		t.active[0][0] = false;
		t.addChangedRegion(new ScreenRegion(ScreenLayer.makeDimensionsCA(0, 0, t.getWidth(), t.getHeight())));

		ScreenLayer merged = new ScreenLayer(1, 1);
		merged.initialize();
		merged.mergeChanges(t, 0L, 0L);

		this.verifyObject(merged.characters[0][0], null);
		this.verifyObject(merged.characterWidths[0][0], 0);
		this.verifyArray(merged.colourCodes[0][0], new int [] {});
		this.verifyObject(merged.changed[0][0], true);
	}

        public void mergeChangesTest3() throws Exception{
                //  Simple merge down test with change flag set, but with no region;  Should get ignored:
                ScreenLayer t = new ScreenLayer(1, 1);
                t.initialize();
                t.clearChangedRegions();
                t.characters[0][0] = "A";
                t.characterWidths[0][0] = 1;
                t.colourCodes[0][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
                t.changed[0][0] = true;
                t.active[0][0] = true;

                ScreenLayer merged = new ScreenLayer(1, 1);
                merged.initialize();
                merged.mergeChanges(t, 0L, 0L);

                this.verifyObject(merged.characters[0][0], null);
                this.verifyObject(merged.characterWidths[0][0], 0);
                this.verifyArray(merged.colourCodes[0][0], new int [] {});
                this.verifyObject(merged.changed[0][0], true);
        }

        public void mergeChangesTest4() throws Exception{
                //  Merge down with two column character
                ScreenLayer t = new ScreenLayer(2, 1);
                t.initialize();
                t.characters[0][0] = "A";
                t.characterWidths[0][0] = 2;
                t.colourCodes[0][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
                t.changed[0][0] = true;
                t.active[0][0] = true;

                ScreenLayer merged = new ScreenLayer(2, 1);
                merged.initialize();
                merged.setAllFlagStates(false);
                merged.mergeChanges(t, 0L, 0L);

                this.verifyObject(merged.characters[0][0], "A");
                this.verifyObject(merged.characterWidths[0][0], 2);
                this.verifyArray(merged.colourCodes[0][0], new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                this.verifyObject(merged.changed[0][0], true);

                //  There should be an empty column after:
                this.verifyObject(merged.characters[1][0], null);
                this.verifyObject(merged.characterWidths[1][0], 0);
                this.verifyArray(merged.colourCodes[1][0], new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                this.verifyObject(merged.changed[1][0], true);
        }

        public void mergeChangesTest5() throws Exception{
                //  Multi-column characters that don't fit in last column should just become null characters:
                ScreenLayer t = new ScreenLayer(3, 1);
                t.setAllFlagStates(false);
                t.initialize();
                t.characters[2][0] = "A";
                t.characterWidths[2][0] = 2;
                t.colourCodes[2][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
                t.changed[2][0] = true;
                t.active[2][0] = true;

                ScreenLayer merged = new ScreenLayer(3, 1);
                merged.initialize();
                merged.setAllFlagStates(false);
                merged.mergeChanges(t, 0L, 0L);

                this.verifyObject(merged.characters[0][0], null);
                this.verifyObject(merged.characterWidths[0][0], 0);
                this.verifyArray(merged.colourCodes[0][0], new int [] {});
                this.verifyObject(merged.changed[0][0], false);

                this.verifyObject(merged.characters[1][0], null);
                this.verifyObject(merged.characterWidths[1][0], 0);
                this.verifyArray(merged.colourCodes[1][0], new int [] {});
                this.verifyObject(merged.changed[1][0], false);

                //  There was not enough space to print the last multi-column character:
                this.verifyObject(merged.characters[2][0], " ");
                this.verifyObject(merged.characterWidths[2][0], 1);
                this.verifyArray(merged.colourCodes[2][0], new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                this.verifyObject(merged.changed[2][0], true);
        }

        public void mergeChangesTest6() throws Exception{
                //  Make sure 
                ScreenLayer t = new ScreenLayer(3, 1);
                t.setAllFlagStates(false);
                t.initialize();
                t.characters[0][0] = "A";
                t.characterWidths[0][0] = 2;
                t.colourCodes[0][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
                t.changed[0][0] = true;
                t.active[0][0] = true;

                ScreenLayer merged = new ScreenLayer(3, 1);
                merged.initialize();
                merged.setAllFlagStates(false);
                merged.mergeChanges(t, 2L, 0L); //  Merge in at end, should force character to null
                merged.mergeChanges(t, 0L, 0L); //  Merge in at start, should be written correctly.
                merged.mergeChanges(t, 1L, 0L); //  Should overwrite previous character due to multi-column overlap.

                this.verifyObject(merged.characters[0][0], " ");
                this.verifyObject(merged.characterWidths[0][0], 1);
                this.verifyArray(merged.colourCodes[0][0], new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                this.verifyObject(merged.changed[0][0], true);

                this.verifyObject(merged.characters[1][0], "A");
                this.verifyObject(merged.characterWidths[1][0], 2);
                this.verifyArray(merged.colourCodes[1][0], new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                this.verifyObject(merged.changed[1][0], true);

                //  There was not enough space to print the last multi-column character:
                this.verifyObject(merged.characters[2][0], null);
                this.verifyObject(merged.characterWidths[2][0], 0);
                this.verifyArray(merged.colourCodes[2][0], new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
                this.verifyObject(merged.changed[2][0], true);
        }

        public void testSimpleTwoColumn() throws Exception{
		//  Simplest test of merge together two layers for multi-column character
		ScreenLayer [] layers = new ScreenLayer [1];
		layers[0] = new ScreenLayer(3, 1);
		layers[0].initialize();
		layers[0].setAllFlagStates(false);
		layers[0].characters[0][0] = "A";
		layers[0].characterWidths[0][0] = 2;
		layers[0].colourCodes[0][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
		layers[0].changed[0][0] = true;
		layers[0].active[0][0] = true;
		layers[0].characters[1][0] = null;
		layers[0].characterWidths[1][0] = 0;
		layers[0].colourCodes[1][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
		layers[0].changed[1][0] = true;
		layers[0].active[1][0] = true;

		ScreenLayer merged = new ScreenLayer(3, 1);
		merged.initialize();
		merged.setAllFlagStates(false);
		merged.mergeNonNullChangesDownOnto(layers, true);

		this.verifyObject(merged.characters[0][0], "A");
		this.verifyObject(merged.characterWidths[0][0], 2);
		this.verifyArray(merged.colourCodes[0][0], new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.changed[0][0], true);

		this.verifyObject(merged.characters[1][0], null);
		this.verifyObject(merged.characterWidths[1][0], 0);
		this.verifyArray(merged.colourCodes[1][0], new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.changed[1][0], true);

		this.verifyObject(merged.characters[2][0], null);
		this.verifyObject(merged.characterWidths[2][0], 0);
		this.verifyArray(merged.colourCodes[2][0], new int [] {});
		this.verifyObject(merged.changed[2][0], false);
        }

        public void testAscendingMultiColumn() throws Exception{
		//  Ascending overlap of multi-column characters
		ScreenLayer [] layers = new ScreenLayer [3];
		layers[0] = new ScreenLayer(4, 1);
		layers[0].initialize();
		layers[0].setAllFlagStates(false);
		layers[0].characters[0][0] = "A";
		layers[0].characterWidths[0][0] = 2;
		layers[0].colourCodes[0][0] = new int [] {UserInterfaceFrameThreadState.BLUE_FG_COLOR};
		layers[0].changed[0][0] = true;
		layers[0].active[0][0] = true;
		layers[0].characters[1][0] = null;
		layers[0].characterWidths[1][0] = 0;
		layers[0].colourCodes[1][0] = new int [] {UserInterfaceFrameThreadState.BLUE_FG_COLOR};
		layers[0].changed[1][0] = true;
		layers[0].active[1][0] = true;

		layers[1] = new ScreenLayer(4, 1);
		layers[1].initialize();
		layers[1].setAllFlagStates(false);
		layers[1].characters[1][0] = "A"; //  Overlaps first "A"
		layers[1].characterWidths[1][0] = 2;
		layers[1].colourCodes[1][0] = new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR};
		layers[1].changed[1][0] = true;
		layers[1].active[1][0] = true;
		layers[1].characters[2][0] = null;
		layers[1].characterWidths[2][0] = 0;
		layers[1].colourCodes[2][0] = new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR};
		layers[1].changed[2][0] = true;
		layers[1].active[2][0] = true;

		layers[2] = new ScreenLayer(4, 1);
		layers[2].initialize();
		layers[2].setAllFlagStates(false);
		layers[2].characters[2][0] = "A";
		layers[2].characterWidths[2][0] = 2;
		layers[2].colourCodes[2][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
		layers[2].changed[2][0] = true;
		layers[2].active[2][0] = true;
		layers[2].characters[3][0] = null;
		layers[2].characterWidths[3][0] = 0;
		layers[2].colourCodes[3][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
		layers[2].changed[3][0] = true;
		layers[2].active[3][0] = true;

		ScreenLayer merged = new ScreenLayer(4, 1);
		merged.initialize();
		merged.setAllFlagStates(false);
		merged.mergeNonNullChangesDownOnto(layers, true);

		this.verifyObject(merged.characters[0][0], " ");
		this.verifyObject(merged.characterWidths[0][0], 1);
		this.verifyArray(merged.colourCodes[0][0], new int [] {UserInterfaceFrameThreadState.BLUE_FG_COLOR});
		this.verifyObject(merged.changed[0][0], true);

		this.verifyObject(merged.characters[1][0], " ");
		this.verifyObject(merged.characterWidths[1][0], 1);
		this.verifyArray(merged.colourCodes[1][0], new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR});
		this.verifyObject(merged.changed[1][0], true);

		this.verifyObject(merged.characters[2][0], "A");
		this.verifyObject(merged.characterWidths[2][0], 2);
		this.verifyArray(merged.colourCodes[2][0], new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.changed[2][0], true);

		this.verifyObject(merged.characters[3][0], null);
		this.verifyObject(merged.characterWidths[3][0], 0);
		this.verifyArray(merged.colourCodes[3][0], new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.changed[3][0], true);
        }

        public void testDescendingMultiColumn() throws Exception{
		//  Descending overlap of multi-column characters
		ScreenLayer [] layers = new ScreenLayer [3];

		layers[0] = new ScreenLayer(4, 1);
		layers[0].initialize();
		layers[0].setAllFlagStates(false);
		layers[0].characters[2][0] = "A";
		layers[0].characterWidths[2][0] = 2;
		layers[0].colourCodes[2][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
		layers[0].changed[2][0] = true;
		layers[0].active[2][0] = true;
		layers[0].characters[3][0] = null;
		layers[0].characterWidths[3][0] = 0;
		layers[0].colourCodes[3][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
		layers[0].changed[3][0] = true;
		layers[0].active[3][0] = true;

		layers[1] = new ScreenLayer(4, 1);
		layers[1].initialize();
		layers[1].setAllFlagStates(false);
		layers[1].characters[1][0] = "A";
		layers[1].characterWidths[1][0] = 2;
		layers[1].colourCodes[1][0] = new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR};
		layers[1].changed[1][0] = true;
		layers[1].active[1][0] = true;
		layers[1].characters[2][0] = null;
		layers[1].characterWidths[2][0] = 0;
		layers[1].colourCodes[2][0] = new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR};
		layers[1].changed[2][0] = true;
		layers[1].active[2][0] = true;

		layers[2] = new ScreenLayer(4, 1);
		layers[2].initialize();
		layers[2].setAllFlagStates(false);
		layers[2].characters[0][0] = "A";
		layers[2].characterWidths[0][0] = 2;
		layers[2].colourCodes[0][0] = new int [] {UserInterfaceFrameThreadState.BLUE_FG_COLOR};
		layers[2].changed[0][0] = true;
		layers[2].active[0][0] = true;
		layers[2].characters[1][0] = null;
		layers[2].characterWidths[1][0] = 0;
		layers[2].colourCodes[1][0] = new int [] {UserInterfaceFrameThreadState.BLUE_FG_COLOR};
		layers[2].changed[1][0] = true;
		layers[2].active[1][0] = true;

		ScreenLayer merged = new ScreenLayer(4, 1);
		merged.initialize();
		merged.setAllFlagStates(false);
		merged.mergeNonNullChangesDownOnto(layers, true);

		this.verifyObject(merged.characters[0][0], "A");
		this.verifyObject(merged.characterWidths[0][0], 2);
		this.verifyArray(merged.colourCodes[0][0], new int [] {UserInterfaceFrameThreadState.BLUE_FG_COLOR});
		this.verifyObject(merged.changed[0][0], true);

		this.verifyObject(merged.characters[1][0], null);
		this.verifyObject(merged.characterWidths[1][0], 0);
		this.verifyArray(merged.colourCodes[1][0], new int [] {UserInterfaceFrameThreadState.BLUE_FG_COLOR});
		this.verifyObject(merged.changed[1][0], true);

		this.verifyObject(merged.characters[2][0], " ");
		this.verifyObject(merged.characterWidths[2][0], 1);
		this.verifyArray(merged.colourCodes[2][0], new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR});
		this.verifyObject(merged.changed[2][0], true);

		this.verifyObject(merged.characters[3][0], " ");
		this.verifyObject(merged.characterWidths[3][0], 1);
		this.verifyArray(merged.colourCodes[3][0], new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.changed[3][0], true);
        }

        public void twoColumnOverOneColumn() throws Exception{
		//  Test to ensure that characters in layers under multi-column characters
		//  are properly obscured:
		ScreenLayer [] layers = new ScreenLayer [3];
		layers[0] = new ScreenLayer(4, 1);
		layers[0].initialize();
		layers[0].setAllFlagStates(false);
		layers[0].characters[1][0] = " ";
		layers[0].characterWidths[1][0] = 1; //  Bottom Layer
		layers[0].colourCodes[1][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
		layers[0].changed[1][0] = true;
		layers[0].active[1][0] = true;

		layers[1] = new ScreenLayer(4, 1);
		layers[1].initialize();
		layers[1].setAllFlagStates(false);
		layers[1].characters[0][0] = "A"; //  Middle layer
		layers[1].characterWidths[0][0] = 2;
		layers[1].colourCodes[0][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
		layers[1].changed[0][0] = true;
		layers[1].active[0][0] = true;
		layers[1].characters[1][0] = null; //  Middle layer
		layers[1].characterWidths[1][0] = 0;
		layers[1].colourCodes[1][0] = new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR};
		layers[1].changed[1][0] = true;
		layers[1].active[1][0] = true;

		layers[2] = new ScreenLayer(4, 1);
		layers[2].initialize();
		layers[2].setAllFlagStates(false); //  Top layer, all nulls

		ScreenLayer merged = new ScreenLayer(4, 1);
		merged.initialize();
		merged.setAllFlagStates(false);
		merged.mergeNonNullChangesDownOnto(layers, true);

		this.verifyObject(merged.characters[0][0], "A");
		this.verifyObject(merged.characterWidths[0][0], 2);
		this.verifyArray(merged.colourCodes[0][0], new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.changed[0][0], true);

		this.verifyObject(merged.characters[1][0], null);
		this.verifyObject(merged.characterWidths[1][0], 0);
		this.verifyArray(merged.colourCodes[1][0], new int [] {UserInterfaceFrameThreadState.RED_FG_COLOR});
		this.verifyObject(merged.changed[1][0], true);

		this.verifyObject(merged.characters[2][0], null);
		this.verifyObject(merged.characterWidths[2][0], 0);
		this.verifyArray(merged.colourCodes[2][0], new int [] {});
		this.verifyObject(merged.changed[2][0], false);

		this.verifyObject(merged.characters[3][0], null);
		this.verifyObject(merged.characterWidths[3][0], 0);
		this.verifyArray(merged.colourCodes[3][0], new int [] {});
		this.verifyObject(merged.changed[3][0], false);
        }


        public void testMergeDownNoNeedToPrint() throws Exception{
                ScreenLayer [] layers = new ScreenLayer [1];
		layers[0] = new ScreenLayer(1, 1);
                layers[0].initialize();
                layers[0].setAllFlagStates(false);
                layers[0].characters[0][0] = "M";
                layers[0].characterWidths[0][0] = 1;
                layers[0].colourCodes[0][0] = new int [] {};
                layers[0].changed[0][0] = true;
                layers[0].active[0][0] = true;

                ScreenLayer merged = new ScreenLayer(1, 1);
                merged.initialize();
                merged.setAllFlagStates(false);
                merged.characters[0][0] = "M";
                merged.characterWidths[0][0] = 1;
                merged.colourCodes[0][0] = new int [] {};
                merged.changed[0][0] = false;
                merged.active[0][0] = true;

                merged.mergeNonNullChangesDownOnto(layers, false);

                this.verifyObject(merged.characters[0][0], "M");
                this.verifyObject(merged.characterWidths[0][0], 1);
                this.verifyArray(merged.colourCodes[0][0], new int [] {});
                this.verifyObject(merged.changed[0][0], false);
        }

        public void writeBorderTwiceNoChange() throws Exception{
                ScreenLayer [] layers = new ScreenLayer [1];
		layers[0] = new ScreenLayer(2, 1);
                layers[0].initialize();
                layers[0].setAllFlagStates(false);
                layers[0].characters[0][0] = "=";
                layers[0].characterWidths[0][0] = 2;
                layers[0].colourCodes[0][0] = new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR};
                layers[0].changed[0][0] = true;
                layers[0].active[0][0] = true;
                layers[0].characters[1][0] = null;
                layers[0].characterWidths[1][0] = 0;
                layers[0].colourCodes[1][0] = new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR};
                layers[0].changed[1][0] = true;
                layers[0].active[1][0] = true;

                ScreenLayer merged = new ScreenLayer(2, 1);
                merged.initialize();
                merged.setAllFlagStates(false);
                merged.characters[0][0] = null;
                merged.characterWidths[0][0] = 0;
                merged.colourCodes[0][0] = new int [] {};
                merged.changed[0][0] = false;
                merged.active[0][0] = true;
                merged.characters[1][0] = null;
                merged.characterWidths[1][0] = 0;
                merged.colourCodes[1][0] = new int [] {};
                merged.changed[1][0] = false;
                merged.active[1][0] = true;

                merged.mergeNonNullChangesDownOnto(layers, true);
                //  Merging in the layer is supposed to clear the changed flags:
                this.verifyObject(layers[0].changed[0][0], false);
                this.verifyObject(layers[0].changed[1][0], false);

                //  Check resulting merged layer:
                this.verifyObject(merged.characters[0][0], "=");
                this.verifyObject(merged.characterWidths[0][0], 2);
                this.verifyArray(merged.colourCodes[0][0], new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(merged.changed[0][0], true);
                this.verifyObject(merged.characters[1][0], null);
                this.verifyObject(merged.characterWidths[1][0], 0);
                this.verifyArray(merged.colourCodes[1][0], new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(merged.changed[1][0], true);

                ScreenLayer output = new ScreenLayer(2, 1);
                output.initialize();
                output.setAllFlagStates(false);
                output.characters[0][0] = null;
                output.characterWidths[0][0] = 0;
                output.colourCodes[0][0] = new int [] {};
                output.changed[0][0] = false;
                output.active[0][0] = true;

                output.mergeChanges(merged, 0L, 0L);
                //  Merging in the layer is supposed to clear the changed flags:
                this.verifyObject(merged.changed[0][0], false);
                this.verifyObject(merged.changed[1][0], false);
		

                this.verifyObject(output.characters[0][0], "=");
                this.verifyObject(output.characterWidths[0][0], 2);
                this.verifyArray(output.colourCodes[0][0], new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(output.changed[0][0], true);
                this.verifyObject(output.characters[1][0], null);
                this.verifyObject(output.characterWidths[1][0], 0);
                this.verifyArray(output.colourCodes[1][0], new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(output.changed[1][0], true);

                this.verifyObject(output.changed[0][0], true);
                this.verifyObject(output.changed[1][0], true);
		output.printChanges(false, false, 10, 10); // Clear changed flags
                this.verifyObject(output.changed[0][0], false);
                this.verifyObject(output.changed[1][0], false);

                // Try to Print same thing again:
                merged.mergeNonNullChangesDownOnto(layers, true);
                //  Merging in the layer is supposed to clear the changed flags:
                this.verifyObject(layers[0].changed[0][0], false);
                this.verifyObject(layers[0].changed[1][0], false);

                //  Merged layer should be same, but with change flag set to false because it was cleared during previous merge:
                this.verifyObject(merged.characters[0][0], "=");
                this.verifyObject(merged.characterWidths[0][0], 2);
                this.verifyArray(merged.colourCodes[0][0], new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(merged.changed[0][0], false);
                this.verifyObject(merged.characters[1][0], null);
                this.verifyObject(merged.characterWidths[1][0], 0);
                this.verifyArray(merged.colourCodes[1][0], new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(merged.changed[1][0], false);

                output.mergeChanges(merged, 0L, 0L);

                this.verifyObject(output.characters[0][0], "=");
                this.verifyObject(output.characterWidths[0][0], 2);
                this.verifyArray(output.colourCodes[0][0], new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(output.changed[0][0], false);
                this.verifyObject(output.characters[1][0], null);
                this.verifyObject(output.characterWidths[1][0], 0);
                this.verifyArray(output.colourCodes[1][0], new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
                this.verifyObject(output.changed[1][0], false);
                System.out.println("\033[0m"); //  Reset background colour for following tests.
        }

        public void testInheritBackgroundBelow() throws Exception{
                //  Test merging to inherit whatever background colour is used by layer underneath.
                ScreenLayer [] layers = new ScreenLayer [1];
		layers[0] = new ScreenLayer(3, 1);
                layers[0].initialize();
                layers[0].setAllFlagStates(false);
                layers[0].characters[0][0] = "A";
                layers[0].characterWidths[0][0] = 2;
                layers[0].colourCodes[0][0] = new int [] {};
                layers[0].changed[0][0] = true;
                layers[0].active[0][0] = true;
                layers[0].characters[1][0] = null;
                layers[0].characterWidths[1][0] = 0;
                layers[0].colourCodes[1][0] = new int [] {};
                layers[0].changed[1][0] = true;
                layers[0].active[1][0] = true;

                ScreenLayer merged = new ScreenLayer(3, 1);
                merged.initialize();
                merged.setAllFlagStates(false);
                merged.characters[0][0] = " ";
                merged.characterWidths[0][0] = 1;
                merged.colourCodes[0][0] = new int [] {UserInterfaceFrameThreadState.BLUE_BG_COLOR};
                merged.changed[0][0] = false;
                merged.active[0][0] = true;
                merged.characters[1][0] = null;
                merged.characterWidths[1][0] = 0;
                merged.colourCodes[1][0] = new int [] {UserInterfaceFrameThreadState.BLUE_BG_COLOR};
                merged.changed[1][0] = false;
                merged.active[1][0] = true;

                merged.mergeNonNullChangesDownOnto(layers, true);

                this.verifyObject(merged.characters[0][0], "A");
                this.verifyObject(merged.characterWidths[0][0], 2);
                this.verifyArray(merged.colourCodes[0][0], new int [] {UserInterfaceFrameThreadState.BLUE_BG_COLOR});
                this.verifyObject(merged.changed[0][0], true);

                this.verifyObject(merged.characters[1][0], null);
                this.verifyObject(merged.characterWidths[1][0], 0);
                this.verifyArray(merged.colourCodes[1][0], new int [] {UserInterfaceFrameThreadState.BLUE_BG_COLOR});
                this.verifyObject(merged.changed[1][0], true);

                this.verifyObject(merged.characters[2][0], null);
                this.verifyObject(merged.characterWidths[2][0], 0);
                this.verifyArray(merged.colourCodes[2][0], new int [] {});
                this.verifyObject(merged.changed[2][0], false);
        }

        public void testIgnoreBackgroundFlaggedFalse() throws Exception{
		//  Test for disappearing background colours on layers underneath
		//  are properly obscured:
		ScreenLayer [] layers = new ScreenLayer [3];
		layers[0] = new ScreenLayer(4, 1);
		layers[0].initialize();
		layers[0].setAllFlagStates(false);
		layers[0].characters[0][0] = " ";
		layers[0].characterWidths[0][0] = 1; //  Bottom Layer
		layers[0].colourCodes[0][0] = new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR};
		layers[0].changed[0][0] = true;
		layers[0].active[0][0] = true;
		layers[0].characters[1][0] = " ";
		layers[0].characterWidths[1][0] = 1; 
		layers[0].colourCodes[1][0] = new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR};
		layers[0].changed[1][0] = true;
		layers[0].active[1][0] = true;

		layers[1] = new ScreenLayer(4, 1);
		layers[1].initialize();
		layers[1].setAllFlagStates(false);
		layers[1].characters[0][0] = "A"; //  Middle layer, should merge down and inherit bottom layer's BG colour
		layers[1].characterWidths[0][0] = 2;
		layers[1].colourCodes[0][0] = new int [] {};
		layers[1].changed[0][0] = false;
		layers[1].active[0][0] = true;
		layers[1].characters[1][0] = null;
		layers[1].characterWidths[1][0] = 0;
		layers[1].colourCodes[1][0] = new int [] {};
		layers[1].changed[1][0] = false;
		layers[1].active[1][0] = true;

		layers[2] = new ScreenLayer(4, 1);
		layers[2].initialize();
		layers[2].setAllFlagStates(false); //  Top layer, all nulls

		ScreenLayer merged = new ScreenLayer(4, 1);
		merged.initialize();
		merged.setAllFlagStates(false);
		merged.mergeNonNullChangesDownOnto(layers, true);

		this.verifyObject(merged.characters[0][0], "A");
		this.verifyObject(merged.characterWidths[0][0], 2);
		this.verifyArray(merged.colourCodes[0][0], new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		this.verifyObject(merged.changed[0][0], true);

		this.verifyObject(merged.characters[1][0], null);
		this.verifyObject(merged.characterWidths[1][0], 0);
		this.verifyArray(merged.colourCodes[1][0], new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		this.verifyObject(merged.changed[1][0], true);

		this.verifyObject(merged.characters[2][0], null);
		this.verifyObject(merged.characterWidths[2][0], 0);
		this.verifyArray(merged.colourCodes[2][0], new int [] {});
		this.verifyObject(merged.changed[2][0], false);

		this.verifyObject(merged.characters[3][0], null);
		this.verifyObject(merged.characterWidths[3][0], 0);
		this.verifyArray(merged.colourCodes[3][0], new int [] {});
		this.verifyObject(merged.changed[3][0], false);
        }

        public void testPartiallyCoveredCharacterBackground() throws Exception{
		//  Test for disappearing background colours for partially covered characters
		//  on the right edge.
		ScreenLayer [] layers = new ScreenLayer [3];
		layers[0] = new ScreenLayer(4, 1);
		layers[0].initialize();
		layers[0].setAllFlagStates(false);
		layers[0].characters[0][0] = "A";
		layers[0].characterWidths[0][0] = 2; //  Bottom Layer
		layers[0].colourCodes[0][0] = new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR};
		layers[0].changed[0][0] = true;
		layers[0].active[0][0] = true;
		layers[0].characters[1][0] = null;
		layers[0].characterWidths[1][0] = 0; 
		layers[0].colourCodes[1][0] = new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR};
		layers[0].changed[1][0] = true;
		layers[0].active[1][0] = true;

		layers[1] = new ScreenLayer(4, 1);
		layers[1].initialize();
		layers[1].setAllFlagStates(false);
		layers[1].characters[0][0] = " "; //  Middle layer,
		layers[1].characterWidths[0][0] = 1;
		layers[1].colourCodes[0][0] = new int [] {UserInterfaceFrameThreadState.GREEN_BG_COLOR};
		layers[1].changed[0][0] = true;
		layers[1].active[0][0] = true;

		layers[2] = new ScreenLayer(4, 1);
		layers[2].initialize();
		layers[2].setAllFlagStates(false); //  Top layer, all nulls

		ScreenLayer merged = new ScreenLayer(4, 1);
		merged.initialize();
		merged.setAllFlagStates(false);
		merged.mergeNonNullChangesDownOnto(layers, true);

		this.verifyObject(merged.characters[0][0], " ");
		this.verifyObject(merged.characterWidths[0][0], 1);
		this.verifyArray(merged.colourCodes[0][0], new int [] {UserInterfaceFrameThreadState.GREEN_BG_COLOR});
		this.verifyObject(merged.changed[0][0], true);

		this.verifyObject(merged.characters[1][0], " ");
		this.verifyObject(merged.characterWidths[1][0], 1);
		this.verifyArray(merged.colourCodes[1][0], new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR});
		this.verifyObject(merged.changed[1][0], true);

		this.verifyObject(merged.characters[2][0], null);
		this.verifyObject(merged.characterWidths[2][0], 0);
		this.verifyArray(merged.colourCodes[2][0], new int [] {});
		this.verifyObject(merged.changed[2][0], false);

		this.verifyObject(merged.characters[3][0], null);
		this.verifyObject(merged.characterWidths[3][0], 0);
		this.verifyArray(merged.colourCodes[3][0], new int [] {});
		this.verifyObject(merged.changed[3][0], false);
        }

        public void printRandomCharactersTest() throws Exception{
		Random rand = new Random(1234);
		int numTestCharacters = 1000;
		int numLayers = 1;
		int layerWidth = 80;
		int layerHeight = 30;

		ScreenLayer [] layers = new ScreenLayer [numLayers];
		List<Map<Coordinate, TestScreenCharacter>> layerCharacters = new ArrayList<Map<Coordinate, TestScreenCharacter>>();

		CuboidAddress layerRegionAddress = new CuboidAddress(
			new Coordinate(Arrays.asList(0L,0L)),
			new Coordinate(Arrays.asList((long)layerWidth, (long)layerHeight))
		);

		for(int l = 0; l < numLayers; l++){
			layerCharacters.add(new HashMap<Coordinate, TestScreenCharacter>());
			layers[l] = new ScreenLayer(layerWidth, layerHeight);
			layers[l].initialize();
			layers[l].setAllFlagStates(false);
			for(int n = 0; n < numTestCharacters; n++){
				Coordinate randomCoordinate = getRandomCoordinate(rand, layerRegionAddress);
				int x = randomCoordinate.getX().intValue();
				int y = randomCoordinate.getY().intValue();
				int randomCodePoint = (int)getRandBetweenRange(rand, 97L, 113L);
				String currentCharacters = Character.toString(randomCodePoint);
				int currentCharacterWidths = 1;
				int [] currentColourCodes = new int [] {};
				boolean currentChanged = true;
				boolean currentActive = true;
				layers[l].characters[x][y] = currentCharacters;
				layers[l].characterWidths[x][y] = currentCharacterWidths;
				layers[l].colourCodes[x][y] = currentColourCodes;
				layers[l].changed[x][y] = currentChanged;
				layers[l].active[x][y] = currentActive;

				TestScreenCharacter cc = new TestScreenCharacter(currentCharacters, currentCharacterWidths, currentColourCodes, currentChanged, currentActive);
				layerCharacters.get(l).put(randomCoordinate, cc);
			}
		}

		ScreenLayer merged = new ScreenLayer(layerWidth, layerHeight);
		merged.initialize();
		merged.setAllFlagStates(false);
		merged.mergeNonNullChangesDownOnto(layers, true);

		for(int l = 0; l < numLayers; l++){
			for(int n = 0; n < numTestCharacters; n++){
				RegionIteration regionIteration = new RegionIteration(layerRegionAddress.getCanonicalLowerCoordinate(), layerRegionAddress);
				if(layerRegionAddress.getVolume() > 0L){
					do{
						Coordinate currentCoordinate = regionIteration.getCurrentCoordinate();
						int x = currentCoordinate.getX().intValue();
						int y = currentCoordinate.getY().intValue();
						if(layerCharacters.get(l).containsKey(currentCoordinate)){
							TestScreenCharacter cc = layerCharacters.get(l).get(currentCoordinate);
							this.verifyObject(merged.characters[x][y], cc.characters);
							this.verifyObject(merged.characterWidths[x][y], cc.characterWidths);
							this.verifyArray(merged.colourCodes[x][y], cc.colourCodes);
							this.verifyObject(merged.changed[x][y], cc.changed);
							this.verifyObject(merged.active[x][y], cc.active);
						}else{
							this.verifyObject(merged.characters[x][y], null);
							this.verifyObject(merged.characterWidths[x][y], 0);
							this.verifyArray(merged.colourCodes[x][y], new int []{});
							this.verifyObject(merged.changed[x][y], false);
							this.verifyObject(merged.active[x][y], true);
						}
					}while (regionIteration.incrementCoordinateWithinCuboidAddress());
				}
			}
		}

		System.out.print("\033[2J");
		merged.printChanges(false, false, 0, 0);
		System.out.print("\n");
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
        	this.writeBorderTwiceNoChange();
		this.testMergeDownNoNeedToPrint();
		this.testInheritBackgroundBelow();
		this.testIgnoreBackgroundFlaggedFalse();
		this.testPartiallyCoveredCharacterBackground();
		this.printRandomCharactersTest();
	}
}
