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

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import com.amazonaws.util.IOUtils;

public class S3TarGzBuilder {
	String ArchiveName = null;
	String ArchiveDirectory = null;
	TarArchiveOutputStream TarArchiveOutPutStream = null;
	
	S3TarGzBuilder(SQSContext ctx) {
		this.ArchiveName = ctx.getLocalArchiveName();
		this.ArchiveDirectory = ctx.getLocalDirectory();
		this.TarArchiveOutPutStream = createTarGzArchive();
	}
	
	private TarArchiveOutputStream createTarGzArchive() {
		FileOutputStream fOut;
		BufferedOutputStream buffOut;
		GzipCompressorOutputStream gzOut;
		TarArchiveOutputStream tOut;
		try {
			fOut = new FileOutputStream(this.ArchiveDirectory + this.ArchiveName);
			buffOut = new BufferedOutputStream(fOut);
			try {
				gzOut = new GzipCompressorOutputStream(buffOut);
				tOut = new TarArchiveOutputStream(gzOut);
				return tOut;
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void addInputStreamToArchive(InputStream s3Object, String localFileName, String objSize) {
		//Convert Input Stream to Output Stream
	    TarArchiveEntry tarEntry = new TarArchiveEntry(localFileName);
	    tarEntry.setSize(Long.valueOf(objSize));
	    try {
			this.TarArchiveOutPutStream.putArchiveEntry(tarEntry);
			IOUtils.copy(s3Object, this.TarArchiveOutPutStream);
			s3Object.close();
			this.TarArchiveOutPutStream.closeArchiveEntry();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closeTarGzArchive() {
		try {
			this.TarArchiveOutPutStream.flush();
			this.TarArchiveOutPutStream.finish();
			this.TarArchiveOutPutStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
