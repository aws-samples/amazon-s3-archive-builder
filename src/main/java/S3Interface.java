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
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

public class S3Interface {
	static ProfileCredentialsProvider credentialsProvider;
	private static AmazonS3 s3;
	private static String bucket;
	private static String targetBucket;
	private static String region;
	private static String s3ArchiveFolder;
	ThreadPoolExecutor executor = null;
	
	S3Interface(String bucket, String targetBucket, String region, String s3ArchiveFolder, int s3MaxConCount, String authType) {
		S3Interface.bucket = bucket;
		S3Interface.region = region;
		S3Interface.s3ArchiveFolder = s3ArchiveFolder;
		S3Interface.targetBucket = targetBucket;
		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(s3MaxConCount);
		
		if(authType.compareTo("iam-keys")==0) {
			// Credentials in ~/.aws/credentials
			credentialsProvider = new ProfileCredentialsProvider();
			try {
				credentialsProvider.getCredentials();
			} catch (Exception e) {
				throw new AmazonClientException(
						"Cannot load the credentials from the credential profiles file. " +
						"Please make sure that your credentials file is at the correct " +
						"location (~/.aws/credentials), and is in valid format.",e);
			}
	        s3 = AmazonS3ClientBuilder.standard()
	        		.withCredentials(credentialsProvider)
	        		.withRegion(getRegion())
	        		.build();
        }
		else if(authType.compareTo("iam-role")==0) {
	        s3 = AmazonS3ClientBuilder.standard()
	        		.withRegion(getRegion())
	        		.build();
		}
	}
	
	private static String getBucket() {
		return bucket;
	}
	
	private static String getTargetBucket() {
		return targetBucket;
	}
	
	private static String getRegion(){
		return region;
	}
	
	private static String getS3ArchiveFolder(){
		return s3ArchiveFolder;
	}
	
	private class migrationCallable implements Callable<InputStream> {
		private S3ArchiveObject obj = null;
		
		migrationCallable(S3ArchiveObject obj) {
			this.obj = obj;
		}

		@Override
		public InputStream call() throws Exception {
			return s3GetObject(getBucket(), obj.getKey());
		}
	}

	public static void uploadS3Archive(SQSContext archiveCTX) {
		String localArchiveName = archiveCTX.getLocalDirectory() + archiveCTX.getLocalArchiveName();
		String key = archiveCTX.getLocalArchiveName();
		String folder = getS3ArchiveFolder();
		s3PutObjectMultiPart(key, folder, localArchiveName);
	}
	
	public static InputStream s3GetObject(String bucket, String key) {
		S3Object obj = s3.getObject(bucket, key);
		InputStream inStream = obj.getObjectContent();
		return inStream;
	}
	
	public static void s3PutObjectMultiPart(String key, String folder, String fileName) {
		TransferManager tm = TransferManagerBuilder.standard()
                .withS3Client(s3)
                .withMinimumUploadPartSize((long) 3000000)  // Upload 3MB Parts
                .build();
		String fullKeyName = null;
		if(folder.contains("/"))
			fullKeyName = folder + key;
		else
			fullKeyName = folder + "/" + key;
		
		// Set Archive Storage Class:
		//  - DeepArchive
		//  - Glacier
		//  - GlacierInstantRetrieval
		PutObjectRequest putObjectRequest = new PutObjectRequest(getTargetBucket(), fullKeyName, new File(fileName));
		putObjectRequest.withStorageClass(StorageClass.Glacier);
		Upload upload = tm.upload(putObjectRequest);
		try {
			// Wait for upload to complete, when done; shutdown Transfer Manager
			upload.waitForCompletion();
			if(upload.isDone())
				tm.shutdownNow(false);
		} catch (AmazonServiceException e) {
			e.printStackTrace();
		} catch (AmazonClientException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void s3DeleteObject(String bucket, String key) {
		s3.deleteObject(bucket, key);
	}
	
	public ListObjectsV2Result s3ListObjects(ListObjectsV2Request req) {
        ListObjectsV2Result listing;
        listing=s3.listObjectsV2(req);
        // Set next listing Token in ListObjectV2Request req
        String token = listing.getNextContinuationToken();
        req.setContinuationToken(token);
        return listing;
	}
	
	public Future<InputStream> submitObjectIntoTar(S3ArchiveObject obj) {
		migrationCallable job = new migrationCallable(obj);
		Future<InputStream> result = this.executor.submit(job);
		return result;
	}
}
