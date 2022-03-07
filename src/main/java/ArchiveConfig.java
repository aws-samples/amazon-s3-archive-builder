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

public class ArchiveConfig {
	private String type;
	private String authType;
	private String baseDirectory;
	private String sourceBucket;
	private String targetBucket;
	private String archiveFilePrefix;
	private String archiveFileFolder;
	private String region;
	private String queue;
	private String s3ListingPrefix;
	private String s3ListingMarker;
	private String s3ListingFilter;
	private String sqsProducerMode;
	private String s3MaxConCount;
	
	public String getType() {
		return this.type;
	}
	
	public void setType(String type) {
		this.type = type;
	}

	public String getAuthType() {
		return this.authType;
	}
	
	public void setAuthType(String authType) {
		this.authType = authType;
	}

	public String getBaseDirectory() {
		return this.baseDirectory;
	}

	public void setBaseDirectory(String baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	public String getSourceBucket() {
		return this.sourceBucket;
	}

	public void setSourceBucket(String sourceBucket) {
		this.sourceBucket = sourceBucket;
	}

	public String getTargetBucket() {
		return this.targetBucket;
	}

	public void setTargetBucket(String targetBucket) {
		this.targetBucket = targetBucket;
	}

	public String getArchiveFilePrefix() {
		return this.archiveFilePrefix;
	}

	public void setArchiveFilePrefix(String archiveFilePrefix) {
		this.archiveFilePrefix = archiveFilePrefix;
	}

	public String getArchiveFileFolder() {
		return this.archiveFileFolder;
	}

	public void setArchiveFileFolder(String archiveFileFolder) {
		this.archiveFileFolder = archiveFileFolder;
	}

	public String getRegion() {
		return this.region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getQueue() {
		return this.queue;
	}

	public void setQueue(String queue) {
		this.queue = queue;
	}

	public String getS3ListingPrefix() {
		return this.s3ListingPrefix;
	}

	public void setS3ListingPrefix(String s3ListingPrefix) {
		this.s3ListingPrefix = s3ListingPrefix;
	}
	
	public String getS3ListingMarker() {
		return this.s3ListingMarker;
	}

	public void setS3ListingMarker(String s3ListingMarker) {
		this.s3ListingMarker = s3ListingMarker;
	}
	
	public String getS3ListingFilter() {
		return this.s3ListingFilter;
	}

	public void setS3ListingFilter(String s3ListingFilter) {
		this.s3ListingFilter = s3ListingFilter;
	}

	public String getSqsProducerMode() {
		return this.sqsProducerMode;
	}

	public void setSqsProducerMode(String sqsProducerMode) {
		this.sqsProducerMode = sqsProducerMode;
	}

	public String getS3MaxConCount() {
		return this.s3MaxConCount;
	}

	public void setS3ThreadNum(String s3MaxConCount) {
		this.s3MaxConCount = s3MaxConCount;
	}	  
}
