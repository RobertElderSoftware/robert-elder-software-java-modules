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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.lang.ProcessBuilder.Redirect;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;



public class ShellProcessRunner {

	private List<String> commandParts = null;
	private Map<String, String> environmentVariables = null;
	private File commandWorkingDirectory = null;
	private boolean expectsStdin = false;
	private Process process = null;
	private ExecutorService es = Executors.newFixedThreadPool(2);

	private ShellProcessReaderThread stdOutThread = null;
	private ShellProcessReaderThread stdErrThread = null;

	private Future<byte []> stdOutFuture = null;
	private Future<byte []> stdErrFuture = null;

	public ShellProcessRunner(List<String> commandParts) throws Exception {
		this.commandParts = commandParts;
		this.commonSetup();
	}

	public ShellProcessRunner(List<String> commandParts, Map<String, String> environmentVariables, File commandWorkingDirectory, boolean expectsStdin) throws Exception {
		this.commandParts = commandParts;
		this.environmentVariables = environmentVariables;
		this.commandWorkingDirectory = commandWorkingDirectory;
		this.expectsStdin = expectsStdin;
		this.commonSetup();
	}

	public void commonSetup() throws Exception {
		ProcessBuilder processBuilder = new ProcessBuilder(this.commandParts);
		if(this.expectsStdin){
			processBuilder.redirectInput(Redirect.PIPE);
		}

		if(this.commandWorkingDirectory != null){
			processBuilder.directory(this.commandWorkingDirectory);
		}

		if(this.environmentVariables != null){
			Map<String, String> env = processBuilder.environment();
			env.clear(); //  Clear existing environment variables.
			for(Map.Entry<String, String> e : environmentVariables.entrySet()){
				env.put(e.getKey(), e.getValue());
			}
		}

		this.process = processBuilder.start();
      
		this.stdOutThread = new ShellProcessReaderThread("stdout", this.process.getInputStream());
		this.stdErrThread = new ShellProcessReaderThread("stderr", this.process.getErrorStream());

		this.stdOutFuture = es.submit(this.stdOutThread);
		this.stdErrFuture = es.submit(this.stdErrThread);
	}

	public OutputStream getOutputStreamForStdin() throws Exception {
		if(this.expectsStdin){
			return process.getOutputStream();
		}else{
			throw new Exception("Was declared with this.expectsStdin, but we're trying to send stdin?");
		}
	}

	public ShellProcessPartialResult getPartialResult() throws Exception{
		/* Call this periodically for a long-running stream process: */
		return new ShellProcessPartialResult(this.stdOutThread.readPartialResult(), this.stdErrThread.readPartialResult());
	}

	public ShellProcessFinalResult getFinalResult() throws Exception{
		/* Call this once to obtain the final result after the process has exited: */
		while(!(stdOutFuture.isDone() && stdErrFuture.isDone())) {}
		ShellProcessPartialResult r = new ShellProcessPartialResult(this.stdOutFuture.get(), this.stdErrFuture.get());
		this.es.shutdown();
		int exitCode = process.waitFor();
		return new ShellProcessFinalResult(r, exitCode);
	}
}
