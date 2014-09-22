import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;


public class MSB_EXAM {
	private static final String ANDREW_ID = "kailianc";
	private static final String SECURITY_GROUP_NAME = "launch-wizard-15";
	private static final String TEST_ID = "loadbalance";

	private static final String REQUESTS_PER_SECOND = "Requests per second";
	private static final String DELIMITER = "[#/sec]";
	private static final double RPS_REQUIREMENT = 3600.0;
	
	private static final int TWO_MINUTE_TICK = 120;
	private static final int HALF_MINUTE_TICK = 30;
	private static final int ONE_SEC = 1000;
	
	//---------------------------------------------------------
	private static final String IMAGE_ID_LOAD_GENERATOR = "ami-1810b270";
	private static final String DATA_TYPE_LOAD_GENERATOR = "m3.medium";

	private static final int MIN_COUNT_LOAD_GENERATOR = 1;
	private static final int MAX_COUNT_LOAD_GENERATOR = 1;
	
	//---------------------------------------------------------
	private static final String IMAGE_ID_DATA_CENTER = "ami-324ae85a";
	private static final String DATA_TYPE_DATA_CENTER = "m3.medium";
	
	private static final int MIN_COUNT_DATA_CENTER = 1;
	private static final int MAX_COUNT_DATA_CENTER = 1;
	
	
    public static void main(String[] args) {
    	long startTime = System.currentTimeMillis();
    	
    	// Step 1. Load the Properties File with AWS Credentials
    	Properties properties = new Properties();
    	try {
			properties.load(MSB_EXAM.class.getResourceAsStream("/AwsCredentials.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
    	System.out.println("accessKey = " + properties.getProperty("accessKey"));
    	System.out.println("secretKey = " + properties.getProperty("secretKey"));
    	
    	BasicAWSCredentials bawsc = new BasicAWSCredentials(properties.getProperty("accessKey"), 
    			properties.getProperty("secretKey"));
    	 
    	// Step 2. Create an Amazon EC2 Client
    	AmazonEC2Client ec2 = new AmazonEC2Client(bawsc);
    	
    	// Step 3. Run Load Generator
    	Instance loadGenerator = runInstance(ec2, IMAGE_ID_LOAD_GENERATOR, DATA_TYPE_LOAD_GENERATOR,
    			MIN_COUNT_LOAD_GENERATOR, MAX_COUNT_LOAD_GENERATOR);
    	
    	waitPerSec(TWO_MINUTE_TICK);
    
    	loadGenerator = waitForInstanceState(ec2, loadGenerator.getInstanceId());
    	String dnsLG = loadGenerator.getPublicDnsName();
    	
    	// Step 4. Submit id to Load Generator
    	String idSubmitLG = "http://" + dnsLG + "/username?username=" + ANDREW_ID;
    	System.out.println("idSubmitLG = " + idSubmitLG);
    	
    	URL urlLG = null;
		boolean isUrlReady = false;
    	while(!isUrlReady) {
	    	try {
				urlLG = new URL(idSubmitLG);
				waitforUrl(urlLG, "This is the MSB's secret web management console");
				break;
		    } catch (IOException e) { 
		    	System.out.println("url = " + urlLG + " is not ready");
			}
    	}
        
        System.out.println("Load Generator id submit succeed");
    	
        // ------------------- Loop Start -------------------
        // Step 5. Loop to Scale Up
        int numOfDC = 0;
        double prevSumRPS = 0.0;
        double curSumRPS = 0.0;
        HashMap<String, Double> map = new HashMap<String, Double>();
        
        Instance dataCenter = null;
        while(curSumRPS < RPS_REQUIREMENT) {
        	// Step 6. Run Data Center
        	dataCenter = runInstance(ec2, IMAGE_ID_DATA_CENTER, DATA_TYPE_DATA_CENTER,
	    			MIN_COUNT_DATA_CENTER, MAX_COUNT_DATA_CENTER);
        	dataCenter = waitForInstanceState(ec2, dataCenter.getInstanceId());
        	String dnsDC = dataCenter.getPublicDnsName();        	
        	
        	System.out.println("One more Data Center is added.");
        	numOfDC++;
        	System.out.println("There are " + numOfDC + " Data Centers now.");
        	
	    	// Step 7. Submit id to Data Center
	    	String idSubmitDC = "http://" + dnsDC + "/username?username=" + ANDREW_ID;
	    	System.out.println("idSubmitDC = " + idSubmitDC);
	    	
	    	URL urlDC = null;
			isUrlReady = false;
	    	while(!isUrlReady) {
			    try {	
			    	urlDC = new URL(idSubmitDC);
			        waitforUrl(urlDC, "This is the MSB's secret web service");
			        break;
		        } catch (IOException e) {
		        	System.out.println("url = " + urlDC + " is not ready");
				}
	    	}
	        
	        System.out.println("Data Center id submit succeed");
	        waitPerSec(HALF_MINUTE_TICK);
	        
	        // Step 8. Submit DNS Name
	        String testId = TEST_ID;
	        String dnsSubmitDC = "http://" + dnsLG + "/part/one/i/want/more?dns=" + dnsDC + "&testId=" + testId;
	        System.out.println("dnsSubmitDC = " + dnsSubmitDC);
	        
	        URL urlDnsDC = null;
			isUrlReady = false;
	    	while(!isUrlReady) {	    		
				try {
					urlDnsDC = new URL(dnsSubmitDC);
			    	waitforUrl(urlDnsDC, "INSTANCE ADDED SUCCESSFULLY TO THE FIREHOSE");
					break;
				} catch (IOException e1) {
					System.out.println("url = " + urlDnsDC + " is not ready");
				}
	    	}
	    	
	    	System.out.println("Data Center DNS submit succeed");
	    	
	    	// Wait for 1 minute for URL refresh
	    	waitPerSec(TWO_MINUTE_TICK);
	    	
	    	// Step 9. Parse the RPS
	        String rps = "http://" + dnsLG + "/view-logs?name=result_" + ANDREW_ID + "_" + testId + ".txt";
	        System.out.println("rps = " + rps);
			
	        while(curSumRPS <= prevSumRPS) {
		        InputStream ins = null;
		        URL urlRPS = null;
				isUrlReady = false;
		    	while(!isUrlReady) {
					try {
						urlRPS = new URL(rps);
						HttpURLConnection connRpsDC = (HttpURLConnection) urlRPS.openConnection();
						connRpsDC.setRequestMethod("GET");
				    	
						ins = urlRPS.openStream();
						break;
					} catch (IOException e1) {
						System.out.println("url = " + urlRPS + " is not ready");
					} 
		    	}
				    
			    try {
			    	BufferedReader in = new BufferedReader(new InputStreamReader(ins));
				    String inputLine;
				    while ((inputLine = in.readLine()) != null) {
				    	if(inputLine.contains("minute")) {
				    		map.clear();
				    		continue;
				    	}
				    	
				    	if(inputLine.contains(REQUESTS_PER_SECOND)) {
				     		System.out.println(inputLine);
				    		String[] str = inputLine.split(" ");
							int i = 0;
							for(i = 0; i < str.length; i++) {
								if(str[i].equals(DELIMITER)) {
									System.out.println("RPS found");
									break;
								}
							}
							
							double valueRPS = Double.parseDouble(str[i - 1]);
							System.out.println("RPS = " + valueRPS);
							map.put(str[0], valueRPS);
				    	}
				    }
				    in.close();
				    
				    // Step 10. Compute the cumulative RPS
				    curSumRPS = 0.0;
				    for(Map.Entry<String, Double> entry : map.entrySet()) {
		        	    curSumRPS += entry.getValue();
				    }
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }
	        prevSumRPS = curSumRPS;
        }
        // ------------------- Loop End -------------------
        
        System.out.println(numOfDC + " Data Center Instances are used.");
    	long estimatedTime = System.currentTimeMillis() - startTime;
    	System.out.println("The overall time elapsed is " + estimatedTime + " msec");
        
    	// terminate the instance if necessary 
    	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    	String userRespone = null;
		try {
			userRespone = bufferedReader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	if(userRespone.toLowerCase().equals("y")) {
    		TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
    		List<String> instances = new ArrayList<String>();
    		instances.add(loadGenerator.getInstanceId());
    		instances.add(dataCenter.getInstanceId());
    		terminateInstancesRequest.setInstanceIds(instances);
    		ec2.terminateInstances(terminateInstancesRequest);
    	}
    }
    
    /**
     * launch a new instance, if preferred security group not exists, creates one 
     * @param ec2   EC2 client
     * @param imageId    default imageId provided by the writeup
     * @param instanceType    default type provided by the writeup
     * @param minCount      default minimum count
     * @param maxCount      default maximum count
     * @return          created instance
     */
    private static Instance runInstance(AmazonEC2Client ec2, String imageId, String instanceType, int minCount, int maxCount) {
    	//Create Instance Request
    	RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
    	
    	DescribeSecurityGroupsRequest secGroupRequest = new DescribeSecurityGroupsRequest();
		DescribeSecurityGroupsResult secGroupResponse = ec2.describeSecurityGroups(secGroupRequest);
		List<SecurityGroup> secGroups = secGroupResponse.getSecurityGroups();
		
		boolean isSecurityGroupExist = false;
		for(SecurityGroup sg : secGroups) {
			if(sg.getGroupName().equals(SECURITY_GROUP_NAME)) {
				isSecurityGroupExist = true;
				break;
			}
		} 
		
		if(!isSecurityGroupExist) {
		    // SecurityGroup
	    	CreateSecurityGroupRequest createSecurityGroupRequest = 
	    			new CreateSecurityGroupRequest();
	    		        	
			createSecurityGroupRequest.withGroupName(SECURITY_GROUP_NAME)
				.withDescription("Kailianc Java Security Group");
			
			ec2.createSecurityGroup(createSecurityGroupRequest);
			
			// IP Permission
			IpPermission ipPermission = new IpPermission();
				    	
			ipPermission.withIpRanges("0.0.0.0/0")
			            .withIpProtocol("tcp")
			            .withFromPort(80)
			            .withToPort(80);
			
			AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest =
					new AuthorizeSecurityGroupIngressRequest();
				    	
			authorizeSecurityGroupIngressRequest.withGroupName(SECURITY_GROUP_NAME).withIpPermissions(ipPermission);
			ec2.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);		
		}
    	 
    	//Configure Instance Request
    	runInstancesRequest.withImageId(imageId)
    	.withInstanceType(instanceType)
    	.withMinCount(minCount)
    	.withMaxCount(maxCount)
    	.withSecurityGroups(SECURITY_GROUP_NAME);
    	
    	//Launch Instance
    	RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);  
    	 
    	//Return the Object Reference of the Instance just Launched
    	Instance instance = runInstancesResult.getReservation().getInstances().get(0);
    	instance.getInstanceId();
    	
    	// Create the list of tags we want to create
        ArrayList<Tag> instanceTags = new ArrayList<Tag>();
        instanceTags.add(new Tag("Name","kailianc"));
        instanceTags.add(new Tag("Project","2.1"));

        // Create a tag request for instances.
        CreateTagsRequest createTagsRequestInstances = new CreateTagsRequest();
        createTagsRequestInstances.withResources(instance.getInstanceId());
        createTagsRequestInstances.setTags(instanceTags);

        // Try to tag the Spot instance started.
        try {
            ec2.createTags(createTagsRequestInstances);
        } catch (AmazonServiceException e) {
            // Write out any exceptions that may have occurred.
            System.out.println("Error terminating instances");
            System.out.println("Caught Exception: " + e.getMessage());
            System.out.println("Reponse Status Code: " + e.getStatusCode());
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("Request ID: " + e.getRequestId());
        }
    	              
    	ec2.createTags(createTagsRequestInstances);
    	System.out.println("Just launched an Instance with ID: " + instance.getInstanceId());
    	
    	return instance;
    }
    
    /**
     * wait for instance created successfully, which means the status is running(terminated instance is omitted)
     * @param ec2    EC2 client
     * @param instanceId     instanceId newly created
     * @return   updated instance when status becomes running
     */
    private static Instance waitForInstanceState(AmazonEC2Client ec2, String instanceId) {
    	Instance retInstance = null;
    	boolean isCompleted = false;
    	
    	while(!isCompleted) {    		
	    	List<Reservation> reservations = ec2.describeInstances().getReservations();
	        int reservationCount = reservations.size();
	         
	        for(int i = 0; i < reservationCount; i++) {
	            List<Instance> instances = reservations.get(i).getInstances();
	    	    int instanceCount = instances.size();
	             
	            //Print the instance IDs of every instance in the reservation.
	            for(int j = 0; j < instanceCount; j++) { 
	                Instance instance = instances.get(j);
	                
	                if(!instance.getInstanceId().equals(instanceId)) {
	                	continue;
	                }
	                
	                if(instance.getState().getName().equals("terminated") 
	                		|| instance.getState().getName().equals("running")) {
	                	retInstance = instance;
	                	isCompleted = true;
	                } else if(instance.getState().getName().equals("pending")) {
	                	isCompleted = false;
	                } else {
	                	System.out.println("Instance = " + instance + " is in state = " + instance.getState().getName());
	                	isCompleted = false;
	                }
	            }
	        }
    	}
    	return retInstance;
    }
    
    /**
     * wait for url ready, if exception happens because the url is being refreshed, retry GET again.
     * @param url   url link
     * @param keyword   keyword displayed when url access successfully
     * @throws IOException   exception happens when url is being refreshed  
     */
    private static void waitforUrl(URL url, String keyword) throws IOException {
    	while(true) {
    		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    	conn.setRequestMethod("GET");    		
	    	InputStream ins = null;
	        try {
				ins = url.openStream();
				BufferedReader in = new BufferedReader(new InputStreamReader(ins));
			    String inputLine;
			    while ((inputLine = in.readLine()) != null) {
			    	System.out.println(inputLine);
			    	if(inputLine.contains(keyword)) {
			    		in.close();
			    		return;
			    	}
			    }
				ins.close();
			} catch (IOException e) {
				System.out.println("Url error, retry.");
			}
    	}    	
    }
    
    /**
     * wrap sleep function to display "." message per second
     * @param times   the overall times for one time a second, which means the sleep time is (times) seconds.
     */
    private static void waitPerSec(int times) {
    	System.out.print("sleep start");
    	for(int i = 0; i < times; i++) {
    		System.out.print(".");
    	    try {
				Thread.sleep(ONE_SEC);
			} catch (InterruptedException e) {
				System.out.println("Sleep error.");
				System.exit(0);
			}	
    	}
    	System.out.println("sleep end");
    }
}
