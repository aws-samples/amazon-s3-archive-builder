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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Logger;
import com.google.gson.Gson;

public class ArchiveController {	
	S3Interface s3 = null;
	SQSInterface sqs = null;
	DISKInterface disk = null;
	SQSProducer producer = null;
	SQSConsumer consumer = null;
	Logger logger = null; 
	private static int processors = Runtime.getRuntime().availableProcessors();
	
	ArchiveController(ArchiveConfig configCTX) {
		String type = configCTX.getType();
		String baseDir = configCTX.getBaseDirectory();
		File logDir = new File(baseDir + "/Controller/");
		if (!logDir.exists())
		    logDir.mkdirs();
		
		// Initialize SQS Controller Log
		String logName = "sqs-" + type;
		this.logger = new ArchiveLogger(logName, logDir.getAbsolutePath()).getLogger();
		this.logger.info("SQS Controller invoked on Base Directory: " + baseDir);
		
		// Create S3/SQS Interfaces Required by Producer/Consumer 
		this.s3 = new S3Interface(configCTX.getSourceBucket(), configCTX.getTargetBucket(),configCTX.getRegion(), 
				configCTX.getArchiveFileFolder(),Integer.valueOf(configCTX.getS3MaxConCount()), configCTX.getAuthType());
		this.sqs = new SQSInterface(configCTX.getQueue(), configCTX.getRegion(), configCTX.getAuthType());
		
		// Initialize Producer
		if(type.compareTo("producer") == 0) {
			this.logger.info("SQS Controller Initializing SQS Producer ... ");
			initSQSProducer(configCTX);
		}
		// Initialize Consumer
		else if(type.compareTo("consumer") == 0) {
			this.logger.info("SQS Controller Initializing SQS Consumers ... ");
			initSQSConsumer(configCTX);
		}
		else
			this.logger.info("SQS Controller found an unsupported type = " + type + " Exiting ...");
	}
	
	private void initSQSProducer(ArchiveConfig configCTX) {
	    // Create Directory Structure for Producer:
		this.logger.info("SQS Controller Creating SQS Producer Directory Structure Over Base Directory ... ");
	    String baseDir = configCTX.getBaseDirectory();
	    File logPath = new File(baseDir + "/Producer/");
	    if(!logPath.exists())
	    	logPath.mkdirs();
	    
	    // Create Required Producer Object
	    this.logger.info("SQS Controller Creating Classes required by SQS Producer ... ");
	    this.producer = new SQSProducer(configCTX.getSourceBucket(), this.s3, this.sqs, 
	    		this.logger,logPath.getAbsolutePath(), configCTX.getSqsProducerMode());
	    
	    // Start SQSProducer Thread
	    this.logger.info("SQS Controller Starting SQS Producer Thread  ... ");
	    this.producer.produce(configCTX.getS3ListingPrefix(), configCTX.getS3ListingMarker(), configCTX.getS3ListingFilter());
	}
	
	private void initSQSConsumer(ArchiveConfig configCTX) {
	    // Create Directory Structure for Consumers
		this.logger.info("SQS Controller Creating Classes required by SQS Consumers ... ");
	    String baseDir = configCTX.getBaseDirectory();
	    File archiveDir = new File(baseDir + "/Consumer/Archives/");
	    if(!archiveDir.exists())
	    	archiveDir.mkdirs();
	    
	    // Create Required Consumer Object
	    this.logger.info("SQS Controller Creating SQS Consumers Directory Structure Over Base Directory ... ");
	    this.disk = new DISKInterface(archiveDir.getAbsolutePath() + "/", configCTX.getArchiveFilePrefix());
		this.consumer = new SQSConsumer(this.s3, this.sqs, this.disk, processors, this.logger);
		
		// Start SQSConsumer Threads
		this.logger.info("SQS Controller Starting SQS Consumer Threads  ... ");
		this.consumer.consume();
		
		// Wait for Consumers to Shutdown and Terminate
		while(!this.consumer.executor.isTerminated()){
			this.logger.info("SQS Consumers Actively Working ...");
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				this.logger.info("S3ArchiveBuilder Controller Thread Interrupted");
			}
		}
		this.logger.info("Successfully Shutdown SQS Consumer Threads ...");
	}

	// Main Method
	public static void main(String[] args) {
		// Get Path To Configuration
		ArchiveConfig configCTX = null;
		Gson gson = new Gson();
		ArchiveController controller = null;
		String configurationFile = System.getProperty("configurationFile");
		File configFile = null;
		if(configurationFile != null)
			configFile = new File(configurationFile);
		else
			System.out.println("The path variable received is null");
		
		// Read Json Configuration File and Convert to ArchiveConfig Object
		FileReader reader;
		try {
			reader = new FileReader(configFile);
			configCTX = gson.fromJson(reader, ArchiveConfig.class);
			if(configCTX != null)
				 controller = new ArchiveController(configCTX);
			
		} catch (FileNotFoundException e) {
			System.out.println("Problem with reading Controller Configuration ... Exiting");
			e.printStackTrace();
		}
		
		// Ensure s3 executor is closed
		controller.s3.executor.shutdownNow();
		while(!controller.s3.executor.isTerminated()) {
			controller.logger.info("Waiting for S3 Executors to ShutDown ...");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// Archive Controller Exits
		controller.logger.info("Successfully Shutdown S3 Interface Executors ...");
		controller.logger.info("SQS Controller All " + configCTX.getType() + " threads have exited ...");
		controller.logger.info("SQS Controller Exiting ...");
	}
}
