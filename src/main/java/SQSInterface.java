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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.google.gson.Gson;

public class SQSInterface {
	private String queueURL;
	private AmazonSQS sqs;
	private ProfileCredentialsProvider credentialsProvider;
	private String region;
	private int maxMessages = 1;
	
	SQSInterface(String queueURL, String region, String authType) {
		this.queueURL = queueURL;
		this.region = region;
		
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
			sqs = AmazonSQSClientBuilder.standard()
	        		.withCredentials(credentialsProvider)
	        		.withRegion(getRegion())
	        		.build();
		}
		else if(authType.compareTo("iam-role")==0) {
			sqs = AmazonSQSClientBuilder.standard()
	        		.withRegion(getRegion())
	        		.build();
		}
	}
	
	private String getQueueURL() {
		return queueURL;
	}
	
	private String getRegion() {
		return region;
	}
	
	private int getMaxMessages() {
		return maxMessages;
	}
	
	public List<Message> readSQSMessages() {
    	ReceiveMessageRequest rcv = new ReceiveMessageRequest()
    		.withQueueUrl(getQueueURL())
    		.withMaxNumberOfMessages(getMaxMessages());
    	return sqs.receiveMessage(rcv).getMessages();
    }
	
	public void deleteSQSMessage(String requestHanderID) {
		sqs.deleteMessage(getQueueURL(), requestHanderID);	
	}
	
    public void sendMessage(String body, long attrSize, Date attrDate) {
    	// Send Message: Attributes
        Map<String, MessageAttributeValue> message = new HashMap<String, MessageAttributeValue>();
        MessageAttributeValue objSize = new MessageAttributeValue().withDataType("Number").withStringValue(Long.toString(attrSize));
        MessageAttributeValue lastMod = new MessageAttributeValue().withDataType("String").withStringValue(attrDate.toString());
        message.put("Size", objSize);
        message.put("Date", lastMod);
        // Send Message: Request
        SendMessageRequest req = new SendMessageRequest()
        		.withQueueUrl(getQueueURL())
        		//.withMessageGroupId(getGroupID())
        		.withMessageBody(body)
        		.withMessageAttributes(message);
       sqs.sendMessage(req);
    }
    
    public void sendMessage(SQSContext ctx) {
    	Gson gson = new Gson();
    	String jsonCTX = gson.toJson(ctx);
        SendMessageRequest req = new SendMessageRequest()
        		.withQueueUrl(getQueueURL())
        		.withMessageBody(jsonCTX);
        sqs.sendMessage(req);
    }
	
	public boolean isSqsQueueEmpty() {
		GetQueueAttributesRequest getQueueAttributesRequest = new GetQueueAttributesRequest()
				.withQueueUrl(getQueueURL())
				.withAttributeNames("ApproximateNumberOfMessages");
		GetQueueAttributesResult getQueueAttributes = sqs.getQueueAttributes(getQueueAttributesRequest);
		String queueLengthString = getQueueAttributes.getAttributes().get("ApproximateNumberOfMessages");
		long queueLengthLong = Long.parseLong(queueLengthString);
		if(queueLengthLong == 0)
			return true;
		else
			return false;
	}
}
