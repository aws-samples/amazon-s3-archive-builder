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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SQSContext implements Serializable {
	private static final long serialVersionUID = 6529685098267757690L;
	private List<S3ArchiveObject> s3ArchiveObjects = null;
	private String prefix;
	private String year;
	private String localDirectory;
	private String localArchiveName;
	private String deleteRequestHandle;
	
	// Constructor
	SQSContext(String prefix, String year, String localDirectory, String localArchiveName) {
		this.s3ArchiveObjects = new ArrayList<S3ArchiveObject>();
		this.setPrefix(prefix);
		this.setYear(year);
		this.setLocalDirectory(localDirectory);
		this.setLocalArchiveName(localArchiveName);
	}
	
	// Get Methods for S3ArchiveControllerContext
	public String getPrefix() {
		return this.prefix;
	}
	public String getYear() {
		return this.year;
	}
	public String getLocalDirectory() {
		return this.localDirectory;
	}
	public String getLocalArchiveName() {
		return this.localArchiveName;
	}
	public List<S3ArchiveObject> getS3ArchiveObjects() {
		return this.s3ArchiveObjects;
	}
	public String getDeleteRequestHandle() {
		return this.deleteRequestHandle;
	}
	
	// Set Methods for S3ArchiveControllerContext
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	public void setYear(String year) {
		this.year = year;
	}
	public void setLocalDirectory(String localDirectory) {
		this.localDirectory = localDirectory;
	}
	public void setLocalArchiveName(String localArchiveName) {
		this.localArchiveName = localArchiveName;
	}
	public void setDeleteRequestHandle(String deleteRequestHandle) {
		this.deleteRequestHandle = deleteRequestHandle;
	}
	
	// S3ArchiveObject List Methods
	public void addS3ArchiveObject(S3ArchiveObject object) {
		this.s3ArchiveObjects.add(object);
	}
	public void removeS3ArchiveObject(int index) {
		this.s3ArchiveObjects.remove(index);
	}
}
