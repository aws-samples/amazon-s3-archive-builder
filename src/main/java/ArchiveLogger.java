/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.joda.time.DateTime;

public class ArchiveLogger {
	private Logger logger = null; 
	
	ArchiveLogger(String loggerName, String loggerPath) {
		System.setProperty("java.util.logging.SimpleFormatter.format",
				"%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
		    
	    //Instantiate the Application Logger
	    Logger logger = Logger.getLogger(loggerName);
	    String date = DateTime.now().toString("M-dd-yyyy.HH-mm-ss");
	    	 
	    // Simple file logging Handler
	    FileHandler fh;
		try {
			fh = new FileHandler(loggerPath + "/" + loggerName + "-" + date + ".log", true);
			logger.addHandler(fh);
		    SimpleFormatter formatter = new SimpleFormatter();	    	
		    fh.setFormatter(formatter);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	    // Ensure nothing is written to stdout
	    Logger parentLog = logger.getParent();
	    if (parentLog!=null &&parentLog.getHandlers().length>0) 
	    	parentLog.removeHandler(parentLog.getHandlers()[0]);
	  	
	    this.logger = logger;
	}
	
	public Logger getLogger() {
		return this.logger;
	}
}
