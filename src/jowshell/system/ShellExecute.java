// Copyright (c) 2016 Per Malmberg
// Licensed under MIT, see LICENSE file.

package jowshell.system;

import jowshell.StreamReader;
import logging.ILogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ShellExecute implements IExecute {
	private final List<String> myCommand = new ArrayList<>();
	private Exception myLastException = null;
	private final List<String> myOutput = new ArrayList<>();
	private int myTimeout = 1000;
	private final ILogger myLogger;

	public ShellExecute(ILogger logger) {
		myLogger = logger;
	}

	private int execute(int defaultReturnValue) {
		myLastException = null;
		myOutput.clear();

		int res = defaultReturnValue;
		StreamReader std = null;

		try {
			ProcessBuilder pb = new ProcessBuilder();
			pb.command(myCommand);
			pb.redirectErrorStream(true);
			Process p = pb.start();

			std = new StreamReader(p.getInputStream());
			std.start();

			myLogger.debug("Executing with timeout of " + myTimeout + "ms: " + myCommand.toString());

			if (p.waitFor(myTimeout, TimeUnit.MILLISECONDS)) {
				res = p.exitValue();
				myLogger.debug("Exit code:" + res);
			} else {
				myLogger.error("Command timed out");
			}
		} catch (InterruptedException | IOException ex) {
			myLogger.error("Execution failed.");
			myLogger.error(ex);
			myLastException = ex;
		} finally {
			if (std != null) {
				try {
					std.join();
					myOutput.addAll(std.getOutput());
				} catch (InterruptedException e) {
					myLogger.error(e);
				}
			}
		}

		return res;
	}

	@Override
	public int execute(int defaultReturnValue, String... commandArray) {
		myCommand.clear();
		Collections.addAll(myCommand, commandArray);
		return execute(defaultReturnValue);
	}

	@Override
	public void setTimeout(int timeoutInMilliSeconds) {
		myTimeout = timeoutInMilliSeconds;
	}

	@Override
	public Exception getLastException() {
		return myLastException;
	}

	public List<String> getOutput() {
		return myOutput;
	}
}