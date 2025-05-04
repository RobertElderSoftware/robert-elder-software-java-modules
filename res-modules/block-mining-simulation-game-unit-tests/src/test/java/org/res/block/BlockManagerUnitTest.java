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

import java.io.BufferedInputStream;
import java.io.IOException;

import org.res.block.Coordinate;
import java.io.IOException;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
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
	public void workItemQueueTest() throws Exception {
		runWorkItemQueueTest();
	}

	@Test
	public void intersectingChunkSetUnitTest() throws Exception {
		runIntersectingChunkSetUnitTest();
	}

	@Test
	public void cuboidAddressIntersectionUnitTest() throws Exception {
		runCuboidAddressIntersectionUnitTest();
	}

	public static void main(String[] args) throws Exception {
		//  Just uncomment whichever unit test you want to run:
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
	}

	public static void runWorkItemQueueTest() throws Exception {
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

	public static void runMultiDimensionalNoiseGeneratorUnitTest(MultiDimensionalNoiseGeneratorUnitTestParameters params) throws Exception {

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

	public static void runIntersectingChunkSetUnitTest() throws Exception {
		Random rand = new Random(1234);
		Long numTestIterations = 100L;
		int maxNumDimensions = 5;
		System.out.println("Begin testing " + numTestIterations + " rounds runIntersectingChunkSetUnitTest to " + maxNumDimensions + " dimensions each.");
		for(long l = 0L; l < numTestIterations; l++){
			Long numDimensions = (long)rand.nextInt(maxNumDimensions) + 1;

			CuboidAddress randomRegion = new CuboidAddress(
				Coordinate.getRandomCoordinate(rand, numDimensions, -8, 8),
				Coordinate.getRandomCoordinate(rand, numDimensions, -6, 6)
			);

			CuboidAddress chunkSize = new CuboidAddress(
				Coordinate.makeOriginCoordinate(numDimensions),
				Coordinate.getRandomCoordinate(rand, numDimensions, 1, 12)
			);
			//System.out.println("Random region is " + randomRegion + " chunkSize is " + chunkSize);

			Set<CuboidAddress> inefficientlyCalculatedRequiredRegions = new HashSet<CuboidAddress>();

			RegionIteration regionIteration = new RegionIteration(randomRegion.getCanonicalLowerCoordinate(), randomRegion);
			do{
				Coordinate currentCoordinate = regionIteration.getCurrentCoordinate();
				CuboidAddress chunkCuboidAddress = CuboidAddress.blockCoordinateToChunkCuboidAddress(currentCoordinate, chunkSize);
				inefficientlyCalculatedRequiredRegions.add(chunkCuboidAddress);
			}while (regionIteration.incrementCoordinateWithinCuboidAddress());
			//System.out.println("inefficientlyCalculatedRequiredRegions: " + inefficientlyCalculatedRequiredRegions);

			Set<CuboidAddress> efficientlyCalculatedRequiredRegions = new HashSet<CuboidAddress>();
			efficientlyCalculatedRequiredRegions.addAll(randomRegion.getIntersectingChunkSet(chunkSize));
			//System.out.println("efficientlyCalculatedRequiredRegions: " + efficientlyCalculatedRequiredRegions);

			for(CuboidAddress efficient : efficientlyCalculatedRequiredRegions){
				if(!inefficientlyCalculatedRequiredRegions.contains(efficient)){
					throw new Exception("Efficiently calculated region " + efficient + " was not present in " + inefficientlyCalculatedRequiredRegions);
				}
			}

			for(CuboidAddress inefficient : inefficientlyCalculatedRequiredRegions){
				if(!efficientlyCalculatedRequiredRegions.contains(inefficient)){
					throw new Exception("Inefficiently calculated region " + inefficient + " was not present in " + efficientlyCalculatedRequiredRegions);
				}
			}
		}
		System.out.println("Finished testing " + numTestIterations + " rounds runIntersectingChunkSetUnitTest to " + maxNumDimensions + " dimensions each.");
	}

	public static void runCuboidAddressIntersectionUnitTest() throws Exception {
		Random rand = new Random(1234);
		Long numTestIterations = 1000L;
		int maxNumDimensions = 8;
		System.out.println("Begin testing " + numTestIterations + " rounds of region intersections with up to " + maxNumDimensions + " dimensions each.");
		for(long l = 0L; l < numTestIterations; l++){
			Long numDimensions = (long)rand.nextInt(maxNumDimensions) + 1;

			CuboidAddress addressA = new CuboidAddress(
				Coordinate.getRandomCoordinate(rand, numDimensions, -2, 2),
				Coordinate.getRandomCoordinate(rand, numDimensions, -2, 2)
			);

			CuboidAddress addressB = new CuboidAddress(
				Coordinate.getRandomCoordinate(rand, numDimensions, -2, 2),
				Coordinate.getRandomCoordinate(rand, numDimensions, -2, 2)
			);

			//System.out.println("BEGIN-----");

			CuboidAddress calculatedIntersection = addressA.getIntersectionCuboidAddress(addressB);

			//System.out.println("Made these two random addresses: " + addressA + " and " + addressB + ", calculatedIntersection was calculated as: " + calculatedIntersection);

			Set<Coordinate> insideIntersection = new HashSet<Coordinate>();
			Set<Coordinate> outsideIntersection = new HashSet<Coordinate>();

			RegionIteration regionIterationA = new RegionIteration(addressA.getCanonicalLowerCoordinate(), addressA);
			do{
				Coordinate currentCoordinate = regionIterationA.getCurrentCoordinate();
				if(addressA.containsCoordinate(currentCoordinate) && addressB.containsCoordinate(currentCoordinate)){
					//System.out.println(currentCoordinate + " is inside intersection.");
					insideIntersection.add(currentCoordinate.copy());
				}else{
					//System.out.println(currentCoordinate + " is outside intersection.");
					outsideIntersection.add(currentCoordinate.copy());
				}
			}while (regionIterationA.incrementCoordinateWithinCuboidAddress());

			RegionIteration regionIterationB = new RegionIteration(addressB.getCanonicalLowerCoordinate(), addressB);
			do{
				Coordinate currentCoordinate = regionIterationB.getCurrentCoordinate();
				if(addressA.containsCoordinate(currentCoordinate) && addressB.containsCoordinate(currentCoordinate)){
					//System.out.println(currentCoordinate + " is inside intersection.");
					insideIntersection.add(currentCoordinate.copy());
				}else{
					//System.out.println(currentCoordinate + " is outside intersection.");
					outsideIntersection.add(currentCoordinate.copy());
				}
			}while (regionIterationB.incrementCoordinateWithinCuboidAddress());

			//System.out.println("Here are the points that are in intersection: " + insideIntersection + ".");
			//System.out.println("Here are the points that are outside intersection: " + outsideIntersection + ".");

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
					highestGuessedIntersection.add(highest[i.intValue()]);
				}
				empiricalIntersection = new CuboidAddress(new Coordinate(lowestGuessedIntersection), new Coordinate(highestGuessedIntersection));
				//System.out.println("The cuboid address intersection was empirically shown to be " + empiricalIntersection);
			}else{
				//System.out.println("There was no intersection cuboid address to calculate.");
			}

			if(empiricalIntersection == null){
				if(calculatedIntersection == null){
					//System.out.println("Pass: Empirical and calculated intersection were both null.");
				}else{
					throw new Exception("There was a difference between empirical intersection which was null: and the obtained intersection was: " + calculatedIntersection);
				}
			}else{
				if(empiricalIntersection.equals(calculatedIntersection)){
					//System.out.println("Pass, : " + empiricalIntersection + " is the same as " + calculatedIntersection + ".");
				}else{
					throw new Exception("There was a difference between empirical intersection: " + empiricalIntersection + " and the obtained intersection: " + calculatedIntersection);
				}
			}
			
			//System.out.println("END-----");
		}

		System.out.println("Finished " + numTestIterations + " rounds of testing region intersections with up to " + maxNumDimensions + " dimensions each.");
	}
}
