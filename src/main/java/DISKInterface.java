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
import java.util.UUID;
import org.apache.commons.io.FileUtils;

public class DISKInterface {
	private String baseDir;
	private String archivePrefix;
	
	DISKInterface(String baseDir, String archivePrefix) {
		this.baseDir = baseDir;
		this.archivePrefix = archivePrefix;
	}
	
	private String getBaseDir() {
		return this.baseDir;
	}
	
	private String getArchivePrefix() {
		return this.archivePrefix;
	}
	
	// Called by SQS Consumers to cleanup Temporary Archive Directory
	public static void cleanArchiveContextDirectory(SQSContext archiveCTX) {
		String localDirectory = archiveCTX.getLocalDirectory();
		File file = new File(localDirectory);
		try {
			FileUtils.deleteDirectory(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Called by SQS Consumers when receiving a new context from SQS Queue
	public String createLocalDirectory() {
		UUID uuid = UUID.randomUUID();
		String localDirectory = this.getBaseDir() + uuid.toString() + "/";
		File theDir = new File(localDirectory);
		if (!theDir.exists()){
		    theDir.mkdirs();
		}
		return localDirectory;
	}
	
	// Called by SQS Consumers to build the Archive Name
	public String generateArchiveName(SQSContext ctx) {
		String prefix = ctx.getPrefix();
		String year = ctx.getYear();
		String device = prefix.split("/")[0];
		String archiveName = this.getArchivePrefix() + "_" + device + "_" + year + ".tar.gz";
		return archiveName;
	}
}
