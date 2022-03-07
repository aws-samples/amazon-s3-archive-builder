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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.gson.Gson;

/* The SQSProducer has a dual function
 * 		1. Lists an S3 Bucket and
 * 		2. Writes results to AWS SQS Queue
 */
public class SQSProducer {
	private Logger logger = null; //Logger.getLogger("SQSProducerLogger");
	private Logger listLogger = null;
	private Logger contextLogger = null;
	private String bucket;
	private S3Interface s3 = null;
	private SQSInterface sqs = null;
	private String command;
	private SortedSet<S3ArchiveObject> dateSortedSet = null; 
	
	@SuppressWarnings("unchecked")
	SQSProducer(String bucket, S3Interface s3, SQSInterface sqs, Logger logger, String logPath, String command) {
		this.bucket = bucket;
		this.s3 = s3;
		this.sqs = sqs;
		this.logger = logger;
		this.command = command;
		this.dateSortedSet = new TreeSet<S3ArchiveObject>(new DateComparator());
		this.listLogger = new ArchiveLogger("sqs-listing-results", logPath + "/").getLogger();
		
		// Log Contexts only for Dry-Runs
		if(command.compareTo("run") != 0)
			this.contextLogger = new ArchiveLogger("sqs-contexts-results", logPath + "/").getLogger();
	}
	
	private String getCommand() {
		return this.command;
	}
	
	@SuppressWarnings("rawtypes")
	private class DateComparator implements Comparator {  
		@Override
		public int compare(Object o1, Object o2) {
			S3ArchiveObject obj1 = (S3ArchiveObject)o1;
			S3ArchiveObject obj2 = (S3ArchiveObject)o2;
			if(obj1.getDate().equals(obj2.getDate()))  
				return 0;  
			else if(obj1.getDate().after(obj2.getDate()))
				return 1;  
			else  
				return -1; 
		}
	}  
    
    private String generatePrefix(String key) {
    	File absPath = new File(key);
    	return absPath.getParent();
    }
    
    private String generateFileName(String key) {
    	File absPath = new File(key);
    	return absPath.getName();
    }
    
    @SuppressWarnings("deprecation")
	private Date generateDate(String fileName) {
    	Date date = new Date();
    	String dateString = fileName.split("[.]")[0];
    	String dateValues [] = dateString.split("-");
    	// Build Date Object for S3ArchiveObject
    	date.setMonth(Integer.valueOf(dateValues[0]));
    	date.setDate(Integer.valueOf(dateValues[1]));
    	date.setYear(Integer.valueOf(dateValues[2]));
    	return date;
    }
    
    private S3ArchiveObject generateS3ArchiveObject(String key, long size, String fileName, Date date) {
    	S3ArchiveObject obj = new S3ArchiveObject(key, String.valueOf(size), null, null, fileName, date);
    	return obj;
    }
    
	private void insertSortedSet(S3ArchiveObject obj) {
		if(this.dateSortedSet.isEmpty())
			this.dateSortedSet.add(obj);
		else {
			S3ArchiveObject lastObj = this.dateSortedSet.last();
			String lastPrefix = generatePrefix(lastObj.getKey());
			String curPrefix = generatePrefix(obj.getKey());
			if(lastPrefix.compareTo(curPrefix) != 0) {
				// Got a new prefix, build context and send into SQS Queue
				try {
					buildSendS3ArchiveContext();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}		
		}
		this.dateSortedSet.add(obj);
	}
    
	@SuppressWarnings("deprecation")
	private void buildSendS3ArchiveContext() throws IOException {
		S3ArchiveObject first = this.dateSortedSet.first();
		String prefix = generatePrefix(first.getKey());
		String year = String.valueOf(first.getDate().getYear());
		SQSContext ctx = new SQSContext(prefix, year, null, null);
		String prevYear = year;
				
		// Build S3 Archive Context 
		Iterator<S3ArchiveObject> itr = this.dateSortedSet.iterator();
		while(itr.hasNext()) {
			S3ArchiveObject obj = itr.next();
			String curYear = String.valueOf(obj.getDate().getYear());
			if(curYear.compareTo(prevYear)!=0){
				if(this.getCommand().compareTo("run") == 0) {              
					sqs.sendMessage(ctx);
					logger.info("Sent S3 Archive Context to SQS Queue");
				}
				else
					printContext(ctx);
				prefix = generatePrefix(obj.getKey());
				ctx = new SQSContext(prefix, curYear, null, null);
			}
			ctx.addS3ArchiveObject(obj);
			prevYear = curYear;
		}
		
		if(!ctx.getS3ArchiveObjects().isEmpty()) {
			if(this.getCommand().compareTo("run") == 0) {
				sqs.sendMessage(ctx);
				logger.info("Sent S3 Archive Context to SQS Queue");
			}
			else
				printContext(ctx);
		}	
		
		// Clear the current TreeSort List
		this.dateSortedSet.clear();
	}
	
	private void printContext(SQSContext ctx) throws IOException {
		Gson gson = new Gson();
		String ctx_string = gson.toJson(ctx);
		this.contextLogger.info(ctx_string);
	}
	
	private void writeListingLog(List<String> listLog) throws IOException {
		ListIterator<String> itr = listLog.listIterator();
		while(itr.hasNext()) {
			this.listLogger.info(itr.next());
		}
	}
    
    public void produce(String listingPrefix, String listingMarker, String listingFilter) {
    	//S3 list files by lexigraphic sorting
    	//If files are named as 8-14-2018, then 8-14-2019 will come before 8-15-2018
    	//As it is desired to archive a years worth of data these structures need to be sorted locally
        ListObjectsV2Request req = new ListObjectsV2Request()
        		.withBucketName(this.bucket)
        		.withPrefix(listingPrefix)
        		.withStartAfter(listingMarker);
        ListObjectsV2Result listing;
        logger.info("Starting S3 Object Listing on: [" + bucket + "] including only Keys containing: " + listingFilter);
        do {	
        	listing=s3.s3ListObjects(req);
            List<S3ObjectSummary> results = listing.getObjectSummaries();
            logger.info("Got: [" + results.size() + "] Keys in listing response from S3");
            List<String> listLog = new ArrayList<String>();
        	for(S3ObjectSummary summary : results) {
        		// Get Parameters of Listing Response
        		String key = summary.getKey();
        		if(key.contains(listingFilter)) {
        			long size = summary.getSize();
        			String fileName = generateFileName(key);
        			Date date = generateDate(fileName);
        			S3ArchiveObject obj = generateS3ArchiveObject(key, size, fileName, date);
        			insertSortedSet(obj);
        			listLog.add(key);
        		}
        	}  
        	try {
				writeListingLog(listLog);
			} catch (IOException e) {
				e.printStackTrace();
			}
        } while (listing.isTruncated());
        
        // Send the last element if the current Tree is noe Empty
        if(!this.dateSortedSet.isEmpty())
			try {
				buildSendS3ArchiveContext();
			} catch (IOException e) {
				e.printStackTrace();
			}
        
        this.s3.executor.shutdown();
        logger.info("Finished Listing from S3 Bucket ...");
    }
}
