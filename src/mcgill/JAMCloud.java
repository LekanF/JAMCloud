package mcgill;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;



import umontreal.iro.lecuyer.simevents.*;
import umontreal.iro.lecuyer.simprocs.*;
import umontreal.iro.lecuyer.rng.*;
import umontreal.iro.lecuyer.randvar.*;
import umontreal.iro.lecuyer.stat.*;
import umontreal.iro.lecuyer.util.Chrono;

public class JAMCloud {
	
 static List<Fog> fogN;
 static List<Fog> CLOUD;
	static List<Device> devices;
	static int totalNumberOfRequests = 2000; // Total number of tasks to simulate.
	static int WARMUP = 100; // Number of tasks for warmup
	int nbTasks; // Number of tasks ended so far;
	static DecimalFormat df = new DecimalFormat("#0.0000000");
	
	static int failPoint = 500, bringBackPoint = 800;
	
	static List<Integer> capacityValues ;
	
	static List<Link> fogLinks;
	
	static List<Double> responseNumbers;// = new ArrayList<Double>();
	
	static Tally meanResponse = new Tally("Mean Response Time"); 
	public static final int HOMEFOG = 1;
	public static final int CLOUDALG = 2;	
	public static final int PO2 = 3;
	public static final int MODPO2 = 4;
	public static final int MINDELAY = 5;
	public static final int VFOG = 6;
	public static final int WITHOUT_CLOUD_MINDELAY = 7;
	
	public static final int VFR = 8;
	public static final int IMPRPOOL = 9;
	public static final int MODPOOL = 10;
	
	public static final int LOCAL = 1;
	public static final int REMOTE = 2;
	
	public static final int REAL = 1;
	public static final int DUMMY  = 2;
	RandomStream streamArr = new MRG32k3a();
	
	static String RESULTS, SUBMITTED, GOODPUT, FOGUTIL; //Good
	static double ALPHA, DECAY, ORDER;
	static int NUMAPP, OPTION, NUMDEVICES, NUMBER_OF_FOGS_FAILED;
	
	static int numberOfFogsToFail = NUMBER_OF_FOGS_FAILED;
//	static int numberOfFogsToFail = 5;
	static double fog_only = 0, pool_only = 0, cloud_only = 0, fog_to_pool = 0, fog_pool_to_cloud = 0, totalApplicationRequests = 0;
	
	
	static double homevfogcount = 0, poolvfogcount = 0, vfogequalcount = 0, cloudvfogcount = 0;
	static double percentHome, percentCloud, percentPool, totalPercent, homeGoodput, poolGoodput, totalGoodput, minHome, minNeighbor, minCloud, cloudGoodput;
	
	static String HOMEREP, HOMEUTIL, HOMEWAIT, HOMESERV, HOMETHRU, HOMESOJ, HOMERESP;
	static String POOLREP, POOLUTIL, POOLWAIT, POOLSERV, POOLTHRU, POOLSOJ, POOLRESP, DUMMYREAL;
	static String PO2REP, PO2UTIL, PO2WAIT, PO2SERV, PO2THRU, PO2SOJ, PO2RESP;
	static String MODPO2REP, MODPO2UTIL, MODPO2WAIT, MODPO2SERV, MODPO2THRU, MODPO2SOJ, MODPO2RESP;
	static String MINREP, MINUTIL, MINWAIT, MINSERV, MINTHRU, MINSOJ, MINRESP;
	
	static String HOME_RESP, POOL_RESP, CLOUD_RESP; //good
	static String REPORT, UTILIZATION, THROUGHPUT, WAIT, SERV, SOJ, RESPTIME, CLOUDUTIL; //Good
	
	
	static List <Application> applicationList = new ArrayList<Application>();
	static int totalFogRequests = 0, totalDeviceRequests = 0;
	
	public Tally waitTimeAvg, servTimeAvg, sojTimeAvg, devRespTime;	
	public Tally remoteWaitAvg;
	
	
	
	public JAMCloud(int choice) throws FileNotFoundException, IOException{
			
		// Creates devices first from previous work but note that getNodeEdge was needed to create fogs
			devices = Creator.createDevicesForFogs();
			// Creates the fogs
			fogN = Creator.googleFogs;
			CLOUD = Creator.cloudNodes;
			
			responseNumbers = new ArrayList<Double>();
//			responseNumbers.add(1.0);
				
			double j = 0;
			for (Device d : devices){
				for (int i = 0; i < NUMAPP; i++){
					
					if (j > 0.5 ) j = 0.0;
					Application t = new Application (d, choice, j); // Just assigning a device to a terminal
					j += 0.1;
					applicationList.add(t);
				}
			}
			
			// clears the myMap and myEdge from memory
//			Creator.clear();	
			System.out.println("Number of Clouds: " + CLOUD.size());
			System.out.println("Number of Fogs: " + fogN.size());
			System.out.println("Number of Devices: " + devices.size());
			System.out.println("Number of Applications: " + applicationList.size());

			
			for (Iterator<Fog> iter = fogN.listIterator(); iter.hasNext(); ) {
			    Fog a = iter.next();
			    if (a.equals(null)) {
			        iter.remove();
			        System.out.println("Removed Fog successfully");
			    }
			}
			
			fogLinks = new ArrayList<Link>();
			// Use code below for 2 distinct links between nodes
//			for (Fog source : fogN){
//				for (Fog destination : fogN){
//					// if source is not equal to destination
//					if (!source.equals(destination)){						
//						Link link = new Link(source, destination, 10); // bandwidth = 10
//						fogLinks.add(link);
//					}
//				}
//			}
			
			// Code below is for a single distinct link between 2 fogs
			
			for (int source = 0; source < fogN.size()-1; source++){
				for (int destination = source + 1; destination  < fogN.size(); destination++ ){
					Link link = new Link(fogN.get(source), fogN.get(destination), 1000); // bandwidth = 1GB
					fogLinks.add(link);
				}
			}
			
			// Fault Tolerance
			
			capacityValues = new ArrayList<Integer>();
			int capacity = 0;
			for (int k = 0; k < NUMBER_OF_FOGS_FAILED; k++){
				capacity = fogN.get(k).jresource.getCapacity();
				capacityValues.add(capacity);
			}
		}
	
		 static class Application extends SimProcess{
			
	//		fogN;
			Fog fogServer;
			int choice;
			Device dev;
			double latency, roundtrip;
			static double homefoglat =  0.0, poollat = 0.0, curdelay = 0.0, totaldelay = 0.0, decay = DECAY, pool_probe = 0.0, totalCloudDelay = 0.0, cloudlat = 0.0, cloud_probes = 0.0;
			int id = 0;
			Fog tempFog, tFog;
			double min = 1000000000;
			double responseTime;
			public  Tally appResponseTime; 
			public  Tally taskStatsSojourn;
			public  Tally home_response;
			public  Tally pool_response;
			public  Tally cloud_response;
			int tput = 0;
			Integer nbTasks = 0;

			double arrRate;
			
			double alpha     = 1.0;   // Parameters of the Weibull service times.
			double lambda    = 4.0;   //               ''
			double delta     = 0.0;   //               ''
			RandomStream streamServ   = new MRG32k3a ("Gen. for service requirements");

			RandomVariateGen taskServ = new WeibullGen (streamServ, alpha, lambda, delta);
			
			static double homeCount = 0, poolCount = 0, dummyPoolCount = 0;
			static double homeMinCount = 0, neighborCount = 0, cloudCount = 0;
			static boolean visit_cloud = false, takeHome = false, takePool = false, takeCloud = false;
			
			static boolean probe_cloud = false;
			static double Tmin = 0, Tmax = 0.0, cloud_decay = 0.5, cap_level = 0.8, probe_pool = 0, cloud_probe_prob = 0.5;
			static double homeLat = 0, poolLat = 0, cloudLat = 0, tPoolDelay = 0, cloudDelay = 0, Pinc = 0.1;
			double minimum = 1000000;
			
			public Application(Fog f, int choice){
				this.fogServer = f;
				this.choice = choice;
			}
			
			public Application(int choice){
				this.choice = choice;
			}
			
			public Application (Device d, int choice, double arr){
				this.dev = d;
				this.choice = choice;
				taskStatsSojourn = new Tally(d.getDevice_id().toString());
				appResponseTime =  new Tally(d.getDevice_id().toString());
				home_response = new Tally(d.getDevice_id().toString());
				pool_response = new Tally(d.getDevice_id().toString());
				cloud_response = new Tally(d.getDevice_id().toString());
				this.arrRate = arr;
				
			}
			
			 public Integer getID(){
				 return this.id++;
			 }
			 
			 public String getTaskID(){
				 
				 String id = dev.getDeviceID().toString() +  getID().toString();
				 return id;
			 }			
	
			 public double minimum(double a, double b){
				 return Math.min(a, b);
			 }
			 
			 public double minimum(double a, double b, double c){
				 return Math.min(Math.min(a, b), c);
			 }
			 
			 public List<Fog> APPPO2 (List<Fog> Fogs){
				 
				 List<Fog> po2 = new ArrayList<Fog>();

				 int random  = (int)(Math.random() * Fogs.size() );
		
				 po2.add(Fogs.get(random));
				 
				 int anotherRandom = (int)(Math.random() * Fogs.size() );
		
				 while (random == anotherRandom){
					 anotherRandom = (int)(Math.random() * Fogs.size() );
				 }
				 po2.add(Fogs.get(anotherRandom));		 
				 
				 return po2;
			 }
			 // HomeFog and other algorithms performTask function
			
			public double performTask(Fog source, double servTime, double arrTime, Fog destination){
				double latency = 0, returnLatency = 0, roundtrip = 0;
				double requestResponse = 0, releaseResponse = 0;
				
				if (destination.jresource.getCapacity() == 0){
					roundtrip = 12000;
					return roundtrip;
				}
				else{
					
					
					destination.jresource.setAlpha(ALPHA);
					requestResponse = destination.jresource.request(2, servTime);
					delay(servTime);
					releaseResponse = destination.jresource.release(2);
					
					if (source.equals(destination)){
						latency = dev.getDevLatatency() + Creator.Latency.DFLatency(dev, destination);
						// Here we should use dynamic network latency, probably make the method foglatency static 
						
					}
					else{
						for (Link i : fogLinks){
							if (source.equals(i.getSource()) && destination.equals(i.getDestination()) || source.equals(i.getDestination()) && destination.equals(i.getSource())){
								i.incrementRequestMb(1);
								latency = dev.getDevLatatency() + i.FogLatency();
							}
						}
					}
					
					
					if (nbTasks > WARMUP){
						double res = (double) ((double)(destination.jresource.getCapacity()- (double)destination.jresource.getAvailable())/(double)destination.jresource.getCapacity());
						destination.utilise.update(res, Sim.time());		
						
						 double avg = destination.jresource.statOnUtil().average() / destination.jresource.statOnUtil().max();
						 destination.util_values.add(avg);
					}
					
					if (source.equals(destination)){
						roundtrip = (Sim.time() - arrTime + (2 * latency));
					}
					else{//  we want return latency
						for (Link i : fogLinks){
							if (source.equals(i.getSource()) && destination.equals(i.getDestination()) || source.equals(i.getDestination()) && destination.equals(i.getSource())){
								i.incrementRequestMb(1);
								returnLatency = dev.getDevLatatency() + i.FogLatency();
								i.decrementRequestMB(2);
							}
						}
						roundtrip = (Sim.time() - arrTime )+ latency + returnLatency;
					}
					return roundtrip + requestResponse + releaseResponse;

				}
				
			}
			
// Function for VFR
			public double performTask(String id, int queue, int classifier, double servTime, double arrTime, Fog source, Fog destination){
				double latency = 0, returnLatency = 0, roundtrip = 0; 
				double reqestResponse = 0, releaseResponse = 0;
				if (destination.jresource.getCapacity() == 0){
					roundtrip = 12000;
					return roundtrip;
				}
				else {
					
					if (source.equals(destination)){
						latency = dev.getDevLatatency() + Creator.Latency.DFLatency(dev, destination);
					}
					else {
						// For 2 disticnt links between 2 Fogs (Fog A -> FogB is different from FogB -> FogA), use code below
	//					for (Link i : fogLinks){
	//						if (source.equals(i.getSource()) && destination.equals(i.getDestination())){
	//							i.incrementRequestMb(1);
	//							latency = dev.getDevLatatency() + i.FogLatency();
	//							i.decrementRequestMB(1);
	//						}
	//					}
						
						// For a single link between 2 fogs, no difference btn fogA -> fogB and fogB -> fogA
						for (Link i : fogLinks){
							if (source.equals(i.getSource()) && destination.equals(i.getDestination()) || source.equals(i.getDestination()) && destination.equals(i.getSource())){
								i.incrementRequestMb(100);
								latency = dev.getDevLatatency() + i.FogLatency();
	//							i.decrementRequestMB(1);
							}
						}
					}
									
					destination.jresource.setAlpha(ALPHA);
					reqestResponse = destination.jresource.request(2, queue, classifier, id, servTime, arrTime);
					if (classifier != DUMMY){

						delay(servTime);					 
						releaseResponse = destination.jresource.release(2, "S");
						
						if (nbTasks > WARMUP){
							double res = (double) ((double)(destination.jresource.getCapacity()- (double)destination.jresource.getAvailable())/(double)destination.jresource.getCapacity());
							 destination.utilise.update(res, Sim.time());
							 
							 double avg = destination.jresource.statOnUtil().average() / destination.jresource.statOnUtil().max();
							 destination.util_values.add(avg);
						}
						if (source.equals(destination)){
							roundtrip = (Sim.time() - arrTime + (2 * latency));
						}
						else{//  we want return latency
							for (Link i : fogLinks){
								if (source.equals(i.getSource()) && destination.equals(i.getDestination()) || source.equals(i.getDestination()) && destination.equals(i.getSource())){
									i.incrementRequestMb(100);
									returnLatency = dev.getDevLatatency() + i.FogLatency();
									i.decrementRequestMB(200);
								}
							}
							roundtrip = Sim.time() - arrTime + latency + returnLatency;
						}
					 }
					else 
						roundtrip = 1000000000;
					return roundtrip + reqestResponse + releaseResponse;
				}

			}
			
			public void actions(){
				double arriveTime;
				double serviceTime;

				
				
				while (nbTasks < totalNumberOfRequests){
					totalApplicationRequests++;
					arriveTime = Sim.time();

					serviceTime = taskServ.nextDouble(); // Exponential
					
					if (nbTasks >= failPoint && nbTasks < bringBackPoint){
//		
						for (int i = 0; i < NUMBER_OF_FOGS_FAILED; i++){
							fogN.get(i).jresource.setCapacity(0);

						}
						
					}
					else if (nbTasks >= bringBackPoint){
						for (int i = 0; i < capacityValues.size(); i++){
							fogN.get(i).jresource.setCapacity(capacityValues.get(i));

						}
					}
					
					// Implementation of the algorithms
					switch (choice){
					
					case VFOG: // The VFR algorithm without routing requests to the cloud
	
						
						String idcW = null;
						if (idcW == null){
							idcW = getTaskID();
						}
						
						// send to homeFog
						Fog homecwfog = selectHomeFog(dev,fogN);
						List<Fog> poolw = poolFogs(dev, homecwfog,fogN, 3);
						
						homeLat = performTask(idcW, LOCAL, REAL, serviceTime, arriveTime, homecwfog, homecwfog);

						// send to pool
						if (homeLat > tPoolDelay){
//							poolCount++; //pool_only++;
							if (tFog == null){
								for (Fog p : poolw){
									poolLat = performTask(idcW, REMOTE, REAL, serviceTime, arriveTime, homecwfog, p);//poolCount++;
									if (poolLat < min){
										minimum = poolLat;
										tFog = p;
									}
									poolLat = minimum;
								}
								
							}
							else{
								poolLat = performTask(idcW, REMOTE, REAL, serviceTime, arriveTime, homecwfog, tFog);//poolCount++;
							}
							probe_pool++;
							tPoolDelay = Math.abs(poolLat - homeLat) * Math.pow(probe_pool, ORDER); 
							
							if (homeLat < poolLat){
								fog_only++; homeCount++;
								if (nbTasks > WARMUP){
									home_response.add(homeLat);
	//								taskStatsSojourn.add(homeLat); 
									appResponseTime.add(homeLat);
									meanResponse.add(homeLat);
									responseNumbers.add(homeLat);
								}
							}
							else{
								pool_only++; poolCount++;
								if (nbTasks > WARMUP){
									pool_response.add(poolLat); 
	//								taskStatsSojourn.add(poolLat); 
									appResponseTime.add(poolLat);
									meanResponse.add(poolLat);
									responseNumbers.add(poolLat);
								}
							}
								
						}
						// Here means if we did not probe the pool, then the homeFog was good
						else{
							tPoolDelay *= decay;
							homeCount++; fog_only++; 
							if (nbTasks > WARMUP){
								home_response.add(homeLat);
								appResponseTime.add(homeLat);
								meanResponse.add(homeLat);
								responseNumbers.add(homeLat);
							}
						}
						
						
						
						nbTasks++;
						break;
					
					
					case CLOUDALG:
						
						//Let us initialize Tmin and Tmax to be the average and max of the response times seen so far
						if (taskStatsSojourn.numberObs() != 0){
							Tmin = taskStatsSojourn.average();
							Tmax = taskStatsSojourn.max();
						}
						
						if (Tmin > Tmax){
							System.out.println("Bad");
						}
						

						//send probe to home fog + pool
						
						String idc = null;
						if (idc == null){
							idc = getTaskID();
						}
						
						// send to home fog
						Fog homecfog = selectHomeFog(dev,fogN);
						List<Fog> pool = poolFogs(dev, homecfog,fogN, 3);
						
						homeLat = performTask(idc, LOCAL, REAL, serviceTime, arriveTime, homecfog, homecfog);
//						homeCount++;
						
						
						// send to pool
						if (homeLat > tPoolDelay){
//							poolCount++; //pool_only++;
							if (tFog == null){
								for (Fog p : pool){
									poolLat = performTask(idc, REMOTE, REAL, serviceTime, arriveTime, homecfog, p);//poolCount++;
									if (poolLat < min){
										minimum = poolLat;
										tFog = p;
									}
									poolLat = minimum;
								}
								
							}
							else{
								poolLat = performTask(idc, REMOTE, REAL, serviceTime, arriveTime, homecfog, tFog);//poolCount++;
							}
							probe_pool++;
							tPoolDelay = Math.abs(poolLat - homeLat) * Math.pow(probe_pool, ORDER); 
							
							if (homeLat < poolLat){
								 
								if (nbTasks > WARMUP){	
									home_response.add(homeLat);
									homeCount++; fog_only++;
								}
//								homeCount++; fog_only++;
								taskStatsSojourn.add(homeLat); 
							}
							else{
								 
								if (nbTasks > WARMUP){	
									pool_response.add(poolLat); 
									poolCount++; pool_only++; 
								} 
//								poolCount++; pool_only++; 
								taskStatsSojourn.add(poolLat); 
							}
								
						}
						// Here means if we did not probe the pool, then the homefog was good
						else{
							tPoolDelay *= decay;
							
							if (nbTasks > WARMUP){	
								home_response.add(homeLat);
								homeCount++; fog_only++; 
							}
							taskStatsSojourn.add(homeLat); 

						}
						
						if (probe_cloud){
							
							cloudLat = performTask(idc, LOCAL, REAL, serviceTime, arriveTime, CLOUD.get(CLOUD.size()-1),  CLOUD.get(CLOUD.size()-1));
							
							if (homeLat < cloudLat || poolLat < cloudLat){
								cloud_probe_prob *= cloud_decay;
								double r = Math.random(); // a random number

								if (r < (1-cloud_probe_prob)){
									probe_cloud = false;
								}
								
								if (homeLat < poolLat){
									if (nbTasks > WARMUP){
										appResponseTime.add(homeLat);
										meanResponse.add(homeLat); 
										responseNumbers.add(homeLat);
										if (homeLat == 0) System.out.println("HOMELAT IS ZERO");
									}
								}
								else{
									if (nbTasks > WARMUP){
										appResponseTime.add(poolLat);
										meanResponse.add(poolLat);
										responseNumbers.add(poolLat);	
										if (poolLat == 0) System.out.println("POOLLAT IS ZERO");
									}
								}
									
							}
							// If cloud response is better
							else{

								if (nbTasks > WARMUP){
									cloud_only++; cloudCount++;
									cloud_response.add(cloudLat); 
									appResponseTime.add(cloudLat);
									meanResponse.add(cloudLat);
									responseNumbers.add(cloudLat);
									if (cloudLat == 0) System.out.println("CLOUDLAT IS ZERO");
								}
							}
						}
						else{

							if (homeLat < Tmin || poolLat < Tmin){
								if (cloud_probe_prob < cap_level){
									cloud_probe_prob += Pinc;
								}
								else{
									cloud_probe_prob = cap_level;
								}
								
								if (homeLat < poolLat){
									if (nbTasks > WARMUP){
										appResponseTime.add(homeLat);
										meanResponse.add(homeLat);
										responseNumbers.add(homeLat);
										if (homeLat == 0) System.out.println("HOMELAT IS ZERO");
									}
								}
								else{
									if (nbTasks > WARMUP){
										appResponseTime.add(poolLat);
										meanResponse.add(poolLat);
										responseNumbers.add(poolLat);
										if (poolLat == 0) System.out.println("POOLLAT IS ZERO");
									}
								}
							}
							else if (homeLat > Tmin && poolLat > Tmin){
								double r  = Math.random();

								if (r < cloud_probe_prob){
									probe_cloud = true;
									cloudLat = performTask(idc, LOCAL, REAL, serviceTime, arriveTime, CLOUD.get(CLOUD.size()-1),  CLOUD.get(CLOUD.size()-1));

									if (nbTasks > WARMUP){
										cloud_only++; cloudCount++;
										cloud_response.add(cloudLat); 
										appResponseTime.add(cloudLat); meanResponse.add(cloudLat);
										responseNumbers.add(cloudLat);
										if (cloudLat == 0) System.out.println("CLOUDLAT IS ZERO");
									}
								}
							}
							else if (homeLat > Tmax && poolLat > Tmax){
									probe_cloud = true;
									cloudLat = performTask(idc, LOCAL, REAL, serviceTime, arriveTime, CLOUD.get(CLOUD.size()-1),  CLOUD.get(CLOUD.size()-1));
																		
//									cloud_only++; cloudCount++;
										if (nbTasks > WARMUP){
											cloud_only++; cloudCount++;
										cloud_response.add(cloudLat); 
										appResponseTime.add(cloudLat); meanResponse.add(cloudLat);
										responseNumbers.add(cloudLat);
										if (cloudLat == 0) System.out.println("CLOUDLAT IS ZERO");
									}
							}
						}

						
						nbTasks++;
						break;
						
						// HOMEFOG ALGORITHM
					case HOMEFOG:
						
						
						Fog hFog = selectHomeFog(dev, fogN);
						responseTime = performTask(hFog, serviceTime, arriveTime, hFog);
						
						nbTasks++;
						break;
						
					case PO2:						
						List <Fog> po2 = APPPO2(fogN); // Gets two random fogs
						int first = po2.get(0).jresource.waitList().size(); // number of requests on the wait queue of the first fog
						int second = po2.get(1).jresource.waitList().size();
						
						if (first < second){
							responseTime = performTask(po2.get(0), serviceTime, arriveTime, po2.get(0));
						 }
						 else if (first > second)
						 {
							 responseTime = performTask(po2.get(1), serviceTime, arriveTime, po2.get(1));

						 } 
						 else if (first == second){ // break arbitrary ties
							 int random = (int)(Math.random() * 1 + 1);
							 responseTime = performTask(po2.get(random), serviceTime, arriveTime, po2.get(random));
//
						 }
						nbTasks++;
						break;
						
					case MODPO2:
						
						
						Fog home_zone_fog = selectHomeFog(dev, fogN);
						List<Fog> zones = poolFogs(dev, home_zone_fog, fogN, 5); // make 5 fogs per zone

						 List<Fog> modPo2 = APPPO2(zones);
						  			 				 
						 int first1 = modPo2.get(0).jresource.waitList().size(); // check number on the queue
						 int second1 = modPo2.get(1).jresource.waitList().size();
						 
						 if (first1 < second1){							 
							 responseTime = performTask(modPo2.get(0), serviceTime, arriveTime, modPo2.get(0));
						 }
						 else if (first1 > second1)
						 {
							 responseTime = performTask(modPo2.get(1), serviceTime, arriveTime, modPo2.get(1));
						 } 
						 else if (first1 == second1){ // break arbitrary ties
							 int random = (int)(Math.random() * 1 + 1);
							 responseTime = performTask(modPo2.get(random), serviceTime, arriveTime, modPo2.get(random));
						 }
						 nbTasks++;
						break;
						
					case VFR : // Our proposed algorithm. Includes sending request to the cloud if vFog is bbehaving badly
						
						
						String id = null;
						if (id == null){
							id = getTaskID();
						}

						// Send to home fog 
						
						Fog homevfog = selectHomeFog(dev, fogN);

						List<Fog> tempPool = poolFogs(dev, homevfog, fogN, 3);
						// Send to homefog if waiting time is less than threshold
						if (homevfog == null){
							homefoglat = 1000000.0;
							System.out.println("Failed  " + homevfog.fog.toGString() );
						}

						homefoglat = performTask(id, LOCAL, REAL, serviceTime, arriveTime, homevfog, homevfog);
						takeHome = true; // We used home fog
						
						if (homevfog != null)
							homeCount++;
						
						Double dumlat = 0.0;
						double poolResponse = 0;
						//No reply from home fog after waiting for pool_delay
						if (homefoglat > totaldelay || homevfog == null){
							poolCount++; homeCount--;
							
							if (tempFog == null){ // If tempfog had not been previous assigned
								for (Fog p : tempPool){
									poollat = performTask(id, REMOTE, REAL, serviceTime, arriveTime, homevfog, p);
									visit_cloud = true; takePool = true;

									dummyPoolCount++;

									if (poollat < min){
										min = poollat;
										tempFog = p;
									}
									poollat = min;
								}
								
							}
							else{
								poollat = performTask(id, REMOTE, REAL, serviceTime, arriveTime, homevfog, tempFog);
								visit_cloud = true; takePool = true;
								dummyPoolCount++;
							}
							pool_probe++;
							totaldelay = (Math.abs(poollat - homefoglat)) * Math.pow(pool_probe,ORDER);
							
						}
						else{
							totaldelay = totaldelay * decay; takePool = false; visit_cloud = false;
						}
//						
						if (visit_cloud){
							
						// USe the cloud if no reply from homefog and pool
							double relative_lat = minimum (homefoglat, poollat); // Takes the fstest respone i.e., the node rel
							if (homefoglat > totalCloudDelay && poollat > totalCloudDelay){
								cloudCount++;  

								cloudlat = performTask(id, LOCAL, REAL, serviceTime, arriveTime, CLOUD.get(CLOUD.size()-1),  CLOUD.get(CLOUD.size()-1));
								cloud_probes++;  visit_cloud = false; takeCloud = true;
								totalCloudDelay = (Math.abs(cloudlat - relative_lat)) * Math.pow(cloud_probes,ORDER);
							}
							else {
								totalCloudDelay *= decay;

							}
						}
						
						double minTime = 0;
						if (cloudlat != 0)
							minTime = minimum(homefoglat, poollat, cloudlat);
						else 
							minTime = minimum(homefoglat, poollat);

						
						if (minTime == homefoglat){
							if (nbTasks > WARMUP){
								meanResponse.add(homefoglat); homevfogcount++;
								appResponseTime.add(homefoglat);
							}
						}
						else if (minTime == poollat){
							if (nbTasks > WARMUP){
								meanResponse.add(poollat); poolvfogcount++;
								appResponseTime.add(poollat);
							}
						}else if (minTime == cloudlat){
							if (nbTasks > WARMUP){
								meanResponse.add(cloudlat); cloudvfogcount++;
								appResponseTime.add(cloudlat);
							}
						}


						id = null;
						nbTasks++;
						break;
						
					case WITHOUT_CLOUD_MINDELAY : // Related Algorithm implementation without the sending requests to the cloud
						//Get domains.
						// Domains can be unique or not, unique is harder as we have to have a specified number, better to go with non unique
						
//						List<Fog> domain = poolFogs(dev, fogN, 4);
						int numOffm = 0, offLimitm = 500;
						double thresholdm = 0.05, minm = 100000000;
						
						Fog tempFogm = null;
						int countm = 0;
						Fog homefogm = selectHomeFog(dev, fogN);
						// Send to homefog if waiting time is less than threshold
						if (homefogm == null)
							throw new IllegalArgumentException ("HomeFog is null");
						List<Fog> domainm = poolFogs(dev, homefogm, fogN, 4);

						double waitTimem = homefogm.jresource.getWaitingTime();
						List<Fog> neighborsm = getSortedNeighbors(dev, homefogm, domainm);

						if ( waitTimem < thresholdm){

							responseTime = performTask(homefogm, serviceTime, arriveTime, homefogm);
							homeMinCount++; fog_only++;
							if (nbTasks > WARMUP){
								home_response.add(responseTime); 
								meanResponse.add(responseTime);
								responseNumbers.add(responseTime);
								appResponseTime.add(responseTime);
							}
							
						}
						else{
							
							
							for (countm = 0; countm < neighborsm.size() - 1; countm++){

								if (neighborsm.get(countm).jresource.getWaitingTime() < thresholdm){

									responseTime = performTask(neighborsm.get(countm), serviceTime, arriveTime, neighborsm.get(countm));
									neighborCount++; pool_only++;
									if (nbTasks > WARMUP){
										pool_response.add(responseTime); 
										meanResponse.add(responseTime);
										responseNumbers.add(responseTime);
										appResponseTime.add(responseTime);
									}

									break;
								}	
								
							}
							
							if (countm == neighborsm.size() - 1){

								responseTime = performTask(neighborsm.get(countm), serviceTime, arriveTime, neighborsm.get(countm));
								pool_only++;
								if (nbTasks > WARMUP){
									pool_response.add(responseTime);
									meanResponse.add(responseTime);
									responseNumbers.add(responseTime);
									appResponseTime.add(responseTime);
								}
							}
					
						}
													
						nbTasks++;
					break;
						
						// Related Algorithm implementation starts here
						case MINDELAY :
							//Get domains.
							// Domains can be unique or not, unique is harder as we have to have a specified number, better to go with non unique
							
//							List<Fog> domain = poolFogs(dev, fogN, 4);
							int numOff = 0, offLimit = 500;
							double threshold = 0.05, min = 100000000;
							
							Fog tempFog = null;
							int count = 0;
//							Map <Fog, Double> domainFogs = new HashMap<Fog, Double>();
							Fog homefog = selectHomeFog(dev, fogN);
							// Send to homefog if waiting time is less than threshold
							if (homefog == null)
								throw new IllegalArgumentException ("HomeFog is null");
							List<Fog> domain = poolFogs(dev, homefog, fogN, 4);
//							List<Fog> domain = poolFogs(dev, fogN, 4);
							double waitTime = homefog.jresource.getWaitingTime();
							List<Fog> neighbors = getSortedNeighbors(dev, homefog, domain);

							if ( waitTime < threshold){

								responseTime = performTask(homefog, serviceTime, arriveTime, homefog);

								if (nbTasks > WARMUP){
									homeMinCount++; fog_only++;
									home_response.add(responseTime); 
									meanResponse.add(responseTime);
									responseNumbers.add(responseTime);
									appResponseTime.add(responseTime);
								}

							}
							else{
								
								
								for (count = 0; count < neighbors.size(); count++){

									if (neighbors.get(count).jresource.getWaitingTime() < threshold){

										responseTime = performTask(neighbors.get(count), serviceTime, arriveTime, neighbors.get(count));

										if (nbTasks > WARMUP){
											neighborCount++; pool_only++;
											pool_response.add(responseTime); 
											meanResponse.add(responseTime);
											responseNumbers.add(responseTime);
											appResponseTime.add(responseTime);
										}

										break;
									}	
									
								}
								
								if (count == neighbors.size()){
									responseTime = performTask(CLOUD.get(CLOUD.size()-1), serviceTime, arriveTime, CLOUD.get(CLOUD.size()-1));
									if (nbTasks > WARMUP){
										cloudCount++; cloud_only++;
										cloud_response.add(responseTime); 
										meanResponse.add(responseTime);
										responseNumbers.add(responseTime);
										appResponseTime.add(responseTime);
									}
								}
						
							}
														
							nbTasks++;
						break;
					}	
					
					
					if (choice == HOMEFOG || choice == PO2 || choice == MODPO2){
						
						// Take observation after warmup is over
						if (nbTasks > WARMUP){

							meanResponse.add(responseTime);
							appResponseTime.add(responseTime);
							responseNumbers.add(responseTime);
							
						}
					}
				}
				 
				 

				 if (choice == VFR ){

					 
					homeGoodput = homevfogcount/(homevfogcount+poolvfogcount + cloudvfogcount) * 100;
					poolGoodput = poolvfogcount/(homevfogcount+poolvfogcount + cloudvfogcount) * 100;
					cloudGoodput = cloudvfogcount/(homevfogcount+poolvfogcount + cloudvfogcount) * 100; 			
					
					double total = fog_only + pool_only + cloud_only;
					System.out.println("Total Application Requests : " + totalApplicationRequests);
					System.out.println("HomeFog Application Requests : " + fog_only + "Percentage : " + (fog_only/total * 100));
					System.out.println("Pool Application Requests : " + pool_only + "Percentage : " + (pool_only/total * 100));
					System.out.println("Cloud Application Requests : " + cloud_only + "Percentage : " + (cloud_only/total * 100));
					
					System.out.println();
					
					System.out.println("HomeFog Response Average : " + home_response.average());
					System.out.println("Pool Response Average : " + pool_response.average());
					System.out.println("Cloud Response Average : " + cloud_response.average());
					
					
					totalPercent = homeCount + poolCount + cloudCount;
					percentHome = ((homeCount)/ totalPercent ) * 100;
					percentPool = (poolCount / totalPercent) * 100;
					percentCloud = (cloudCount / totalPercent) * 100;
					
					System.out.println("Home requests submission : " + homeCount + " % : " + percentHome);
					System.out.println("Pool requests submission : " + poolCount + " % : " + percentPool + " DummyCount : " + dummyPoolCount);
					System.out.println("Cloud requests submission : " + cloudCount + " % : " + percentCloud);
					System.out.println("Total Home and Pool submission " + totalPercent);
				}
				
				Sim.stop(); // N tasks have now completed
				
				 if (choice == MINDELAY || choice == CLOUDALG){
					 double total = fog_only + pool_only + cloud_only;
					 minHome =  fog_only/total * 100;
					 minNeighbor = pool_only/total * 100;
					 minCloud = cloud_only/total * 100;
					 
						System.out.println("HomeFog Application Requests : " + fog_only + "Percentage : " + (fog_only/total * 100));
						System.out.println("Pool Application Requests : " + pool_only + "Percentage : " + (pool_only/total * 100));
						System.out.println("Cloud Application Requests : " + cloud_only + "Percentage : " + (cloud_only/total * 100));
						
						System.out.println();
						
						System.out.println("HomeFog Response Average : " + home_response.average());
						System.out.println("Pool Response Average : " + pool_response.average());
						System.out.println("Cloud Response Average : " + cloud_response.average());
				 }
				
			}	
			
		}
		 
		 private static List<Fog> getSortedNeighbors(Device dev, Fog homefog, List<Fog> domain){
			 List<Fog> myNeighbors;
			 double latency = 0, delay = 0;
			 Map <Fog, Double> domainFogs = new HashMap<Fog, Double>();
			 
			 for (Fog neighbor : domain){
				 for (Link link : fogLinks){
					 	if (homefog.equals(link.getSource()) && neighbor.equals(link.getDestination()) || homefog.equals(link.getDestination()) && neighbor.equals(link.getSource())){
						latency = dev.getDevLatatency() + link.FogLatency();
						delay = latency + neighbor.jresource.getWaitingTime();
						domainFogs.put(neighbor, delay);	
					 }
				 }
				}
				Stream<Map.Entry<Fog, Double>> sorted = domainFogs.entrySet().stream().sorted(Map.Entry.comparingByValue());
				
				Map<Fog, Double> sortedFogs = domainFogs.entrySet().stream().sorted(Map.Entry.comparingByValue()).limit(domain.size()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)); 
				 
				 myNeighbors = new ArrayList<>(sortedFogs.keySet());
				 return myNeighbors;
		 }
		 
		 // Selects homefog in order of latencies in case of failure
		 private static Fog selectHomeFog(Device dev, List<Fog> allFogs){
			 double latency = 0;
			 
			 
			 List<Fog> myHomeFog;
			 
			 HashMap<Fog, Double> myFogMap = new HashMap<Fog, Double>();
			 		 
			 Fog homefog = null;
			 for (Fog f: allFogs){
				 if (f != null){
					 latency = Creator.Latency.DFLatency(dev, f);
					 myFogMap.put(f, latency);
				 }
			 }
			 Stream<Map.Entry<Fog, Double>> sorted = myFogMap.entrySet().stream().sorted(Map.Entry.comparingByValue());
			 Map<Fog, Double> closestFog = myFogMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).limit(1).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			 myHomeFog = new ArrayList<>(closestFog.keySet());
			 
			 homefog = myHomeFog.get(0);
			 return homefog;
		 }
		private void simulOneRun() {
		   SimProcess.init();
		  
		   for (Fog c : CLOUD){
			   c.jresource.init();
			   c.jresource.setStatCollecting(true);
		   }
		   for (Fog f : fogN){
			   f.jresource.init();
			  
			   f.jresource.setStatCollecting(true); 
		   }
		   
		   for (Application t : applicationList){

			   t.taskStatsSojourn.init();

			   t.schedule(t.arrRate);
		   }
		   
		   Sim.start();
		    
		   
		 }
		
		public void printReport(int choice) throws IOException{
			double average = 0, sum = 0;
			double totalUtil = 0, totalServ = 0, totalSoj = 0, totalWait = 0, totalThru = 0, remoteWait = 0;
			double remoteCount = 0;
			double cloudUtil = 0;
			
			
			double jResUtil = 0; double realUtil = 0;
			double avg_util = 0, max_util = 0, fog_utilization = 0;

			FileWriter respTime =  new FileWriter (RESPTIME, false);
			FileWriter result = new FileWriter(RESULTS, false);
			FileWriter requests_submitted = new FileWriter(SUBMITTED, false);
			FileWriter cloudutil = new FileWriter(CLOUDUTIL, false);
			FileWriter home_resp = new FileWriter(HOME_RESP, false);
			FileWriter pool_resp = new FileWriter(POOL_RESP, false);
			FileWriter cloud_resp = new FileWriter(CLOUD_RESP, false);
			

			if (choice == VFR || choice == MINDELAY || choice == CLOUDALG){

				
				cloudUtil = CLOUD.get(CLOUD.size() - 1).jresource.statOnUtil().average() / CLOUD.get(CLOUD.size() - 1).jresource.statOnUtil().max();
				System.out.println("Cloud Utilization : " + cloudUtil);
				
				double val;
				for (int i = 0; i < CLOUD.get(CLOUD.size() - 1).util_values.size(); i++){
					val = CLOUD.get(CLOUD.size() - 1).util_values.get(i); 
					cloudutil.write(df.format(val) + "\n");					
				}
				cloudutil.write(df.format(cloudUtil) + "\n");
				double sumHome = 0, sumPool = 0, sumCloud = 0;
				int countHome = 0, countPool = 0,  countCloud = 0;
				for (int t = 0; t < applicationList.size(); t++){

					if (applicationList.get(t).home_response.numberObs() == 0 ){
						home_resp.write(0 + "\n");
						
					}
					else{					
						home_resp.write(df.format(applicationList.get(t).home_response.average()) + "\n");
						sumHome += applicationList.get(t).home_response.average();
						countHome++;
					}
					if (applicationList.get(t).pool_response.numberObs() == 0 ){
						pool_resp.write(0 + "\n");
					}
					else {
						pool_resp.write(df.format(applicationList.get(t).pool_response.average()) + "\n");
						sumPool += applicationList.get(t).pool_response.average();
						countPool++;
					}
					
					if (applicationList.get(t).cloud_response.numberObs() == 0){
						cloud_resp.write(0 + "\n");
					}
					else{
						cloud_resp.write(df.format(applicationList.get(t).cloud_response.average()) + "\n");
						sumCloud += applicationList.get(t).cloud_response.average();
						countCloud++;
					}
														
				}
				
				double avgHome = 0, avgPool = 0, avgCloud = 0;
				avgHome = sumHome/countHome;
				avgPool = sumPool/countPool;
				avgCloud = sumCloud/countCloud;
				double meanResp = (sumHome + sumPool + sumCloud)/ (countHome + countPool + countCloud);
				
				System.out.println("Correct Home Resp Avg " + avgHome);
				System.out.println("Correct Pool Resp Avg " + avgPool);
				System.out.println("Correct Cloud Resp Avg " + avgCloud);
				System.out.println("Total mean Resp Avg " + meanResp);
				
			}
			
			// Write the response times to file
			for (int t = 0; t < applicationList.size(); t++){
				 respTime.write(df.format(applicationList.get(t).appResponseTime.average()) +"\n");
			}
			
			

			for (int i = 0; i < fogN.size(); i++){
				sum = 0;
				
				waitTimeAvg = fogN.get(i).jresource.waitList().statSojourn();
				servTimeAvg = fogN.get(i).jresource.servList().statSojourn();
				sojTimeAvg = fogN.get(i).jresource.statOnSojourn();
				remoteWaitAvg = fogN.get(i).jresource.remoteWaitList().statSojourn();
								
				avg_util += fogN.get(i).jresource.statOnUtil().average();
				max_util += fogN.get(i).jresource.statOnUtil().max();
				
				jResUtil = fogN.get(i).jresource.statOnUtil().average() / fogN.get(i).jresource.statOnUtil().max();
				realUtil += jResUtil;
				

				totalWait += waitTimeAvg.average();

				totalServ += servTimeAvg.average();

				totalSoj += sojTimeAvg.average();
				
				
				if (!fogN.get(i).jresource.remoteWaitList().isEmpty())
					remoteWait += remoteWaitAvg.average();
				
				for (int j = 0; j < fogN.get(i).utilise.getSize(); j++){
					sum += fogN.get(i).utilise.getResource(j);
				}
				average = sum / fogN.get(i).utilise.getSize();
				totalFogRequests +=  fogN.get(i).utilise.getSize();
				totalUtil += average;
				totalThru += fogN.get(i).utilise.getSize();
				


			 remoteCount += fogN.get(i).jresource.getRemoteCount();

			}
			realUtil /= fogN.size(); 
			totalUtil = totalUtil / fogN.size();
			totalWait = totalWait / fogN.size();
			totalServ = totalServ / fogN.size();
			totalSoj = totalSoj / fogN.size();
			totalThru = totalThru / Sim.time();
			
			fog_utilization = avg_util/max_util;
			
			remoteWait = remoteWait / remoteCount;
			


			totalDeviceRequests = meanResponse.numberObs();
			if (choice == VFR ){
				 requests_submitted.write( df.format(percentHome) + " " + df.format(percentPool) + " " + df.format(percentCloud) + "\n");
			}
			
			if (choice == MINDELAY || choice  == CLOUDALG){
				requests_submitted.write( df.format(minHome) + "\n" + df.format(minNeighbor) + "\n" + df.format(minCloud) + "\n");				
			}
			
			System.out.println("Total Fog Requests: " + totalFogRequests);
			System.out.println("Total Device Requests: " + totalDeviceRequests);
			System.out.println("Mean Response : " + df.format(meanResponse.average())); result.write(df.format(meanResponse.average()) + "\n");
			System.out.println("Total util : " + realUtil);
			System.out.println("Correct utilization : " + fog_utilization);
			System.out.println("Average Utilization : " + df.format(totalUtil)); result.write(df.format(realUtil) + "\n");
			System.out.println("Average Wait : " + df.format(totalWait)); result.write(df.format(totalWait) +"\n");
			System.out.println("Average Serv : " + df.format(totalServ)); result.write(df.format(totalServ) + "\n");
			System.out.println("Average Soj : " + df.format(totalSoj)); result.write(df.format(totalSoj) + "\n");
			System.out.println("Average Thru : " + df.format(totalThru)); result.write(df.format(totalThru) + "\n");
			if (choice == VFR) {
				System.out.println("Remote Wait: " + remoteWait + " Remote count : " + remoteCount);
				result.write( df.format(remoteWait) + " " + df.format(remoteCount));
			}
			
			result.close(); respTime.close();//fogutil.close();
			if (choice == VFR || choice == MINDELAY || choice  == CLOUDALG) {
				requests_submitted.close(); 
				home_resp.close(); pool_resp.close(); cloud_resp.close();
				cloudutil.close();
			}
			System.out.println("Number of responses : " + responseNumbers.size());
			double sumResp = 0;
			for (int i = 0; i < responseNumbers.size(); i++){
				sumResp += responseNumbers.get(i);
			}
			double avgResp = sumResp / responseNumbers.size();
			System.out.println("Average Response Time :  " + avgResp);		
		}
		
		public void printLongReport(int choice) throws IOException{
			double average = 0, sum = 0;
			
			switch (choice){
				case HOMEFOG:
					FileWriter homerep = new FileWriter(HOMEREP, false);
					FileWriter homeutil = new FileWriter(HOMEUTIL, false);
					FileWriter homewait = new FileWriter (HOMEWAIT, false);
					FileWriter homeserv = new FileWriter (HOMESERV, false);
					FileWriter homesoj = new FileWriter (HOMESOJ, false);
					FileWriter homethru = new FileWriter (HOMETHRU, false);
					FileWriter homeresp =  new FileWriter (HOMERESP, false);
					
					write(homerep, homeutil, homewait, homeserv, homesoj, homethru, homeresp);
					
					homerep.close(); homeutil.close(); homewait.close(); homeserv.close();
					homesoj.close(); homethru.close(); homeresp.close();
					
					break;
				case VFR:
					FileWriter poolrep = new FileWriter(POOLREP, false);
					FileWriter poolutil = new FileWriter(POOLUTIL, false);
					FileWriter poolwait = new FileWriter (POOLWAIT, false);
					FileWriter poolserv = new FileWriter (POOLSERV, false);
					FileWriter poolsoj = new FileWriter (POOLSOJ, false);
					FileWriter poolthru = new FileWriter (POOLTHRU, false);
					FileWriter poolresp =  new FileWriter (POOLRESP, false);
					FileWriter dummyReal = new FileWriter(DUMMYREAL, false);
					
					write(poolrep, poolutil, poolwait, poolserv, poolsoj, poolthru, poolresp);
						for (int i = 0; i < fogN.size(); i++){
						 dummyReal.write("Fog " + i + ": " + fogN.get(i).jresource.getSave().getSize() + "\n");
						 dummyReal.write(df.format(fogN.get(i).jresource.getSave().getDummyAverage()) + " " + df.format(fogN.get(i).jresource.getSave().getRealAverage()) + "\n");
						 System.out.println("Fog " + i + "\n");
						 System.out.println(df.format(fogN.get(i).jresource.getSave().getDummyAverage()) + " " + df.format(fogN.get(i).jresource.getSave().getRealAverage()) + "\n");						
						}
					
					poolrep.close(); poolutil.close(); poolwait.close(); poolserv.close();
					poolsoj.close(); poolthru.close(); poolresp.close(); dummyReal.close();
					
					break;
				case PO2:
					FileWriter po2rep = new FileWriter(PO2REP, false);
					FileWriter po2util = new FileWriter(PO2UTIL, false);
					FileWriter po2wait = new FileWriter (PO2WAIT, false);
					FileWriter po2serv = new FileWriter (PO2SERV, false);
					FileWriter po2soj = new FileWriter (PO2SOJ, false);
					FileWriter po2thru = new FileWriter (PO2THRU, false);
					FileWriter po2resp =  new FileWriter (PO2RESP, false);
					
					write(po2rep, po2util, po2wait, po2serv, po2soj, po2thru, po2resp);
					
					po2rep.close(); po2util.close(); po2wait.close(); po2serv.close();
					po2soj.close(); po2thru.close(); po2resp.close();
					
					break;
				case MODPO2:
					FileWriter modpo2rep = new FileWriter(MODPO2REP, false);
					FileWriter modpo2util = new FileWriter(MODPO2UTIL, false);
					FileWriter modpo2wait = new FileWriter (MODPO2WAIT, false);
					FileWriter modpo2serv = new FileWriter (MODPO2SERV, false);
					FileWriter modpo2soj = new FileWriter (MODPO2SOJ, false);
					FileWriter modpo2thru = new FileWriter (MODPO2THRU, false);
					FileWriter modpo2resp =  new FileWriter (MODPO2RESP, false);
					
					write(modpo2rep, modpo2util, modpo2wait, modpo2serv, modpo2soj, modpo2thru, modpo2resp);
					
					modpo2rep.close(); modpo2util.close(); modpo2wait.close(); modpo2serv.close();
					modpo2soj.close(); modpo2thru.close(); modpo2resp.close();
					
					break;
				case MINDELAY:
					FileWriter minrep = new FileWriter(MINREP, false);
					FileWriter minutil = new FileWriter(MINUTIL, false);
					FileWriter minwait = new FileWriter (MINWAIT, false);
					FileWriter minserv = new FileWriter (MINSERV, false);
					FileWriter minsoj = new FileWriter (MINSOJ, false);
					FileWriter minthru = new FileWriter (MINTHRU, false);
					FileWriter minresp =  new FileWriter (MINRESP, false);
					
					write(minrep, minutil, minwait, minserv, minsoj, minthru, minresp);
					
					minrep.close(); minutil.close(); minwait.close(); minserv.close();
					minsoj.close(); minthru.close(); minresp.close();
					break;
			}
		}
		
		public void write(FileWriter report, FileWriter util, FileWriter wait, FileWriter serv, FileWriter soj, FileWriter thru, FileWriter resp) throws IOException{
			double average = 0, sum = 0;
			
			report.write("FOG RESOURCE REPORT \n ========================= \n");
			for (int i = 0; i < fogN.size(); i++){
				sum = 0;
				report.write(fogN.get(i).jresource.report());
				report.write("\n");
				
				waitTimeAvg = fogN.get(i).jresource.waitList().statSojourn();
				servTimeAvg = fogN.get(i).jresource.servList().statSojourn();
				sojTimeAvg = fogN.get(i).jresource.statOnSojourn();
				
				wait.write(df.format(waitTimeAvg.average()) + "\n");
				serv.write(df.format(servTimeAvg.average()) + "\n");
				soj.write(df.format(sojTimeAvg.average()) + "\n");
				
				for (int j = 0; j < fogN.get(i).utilise.getSize(); j++){
					sum += fogN.get(i).utilise.getResource(j);
				}
				average = sum / fogN.get(i).utilise.getSize();
				totalFogRequests +=  fogN.get(i).utilise.getSize();
				report.write("Sum: " + sum + " Size " + fogN.get(i).utilise.getSize() + " Average : " + average + "\n");
				
				util.write(df.format(average) + "\n");
				thru.write(fogN.get(i).utilise.getSize()+ "\n");
							

			}
			report.write("END OF FOG RESOURCE REPORT:=========== \n");
			report.write("Device Task REPORT:=========== \n");
		
				for (int j = 0; j < applicationList.size(); j++){
						report.write(applicationList.get(j).taskStatsSojourn.report() + "\n");
					
					totalDeviceRequests += applicationList.get(j).taskStatsSojourn.numberObs();
					 resp.write(df.format(applicationList.get(j).taskStatsSojourn.average()) +"\n");					 						 				 	
				}
			report.write("End of Device Task REPORT:=========== \n");
		}
	
//		public static List<Fog> poolFogs(Device dev, List<Fog> fog, int choice){
//			// return the 3 closest fogs
//			 List<Fog> myPool3;// = new ArrayList<Fog>();
//			 
//			 HashMap<Fog, Double> myFogMap = new HashMap<Fog, Double>();
//			 
//			 //Lets randomly pick 3 fogs
//			 for (Fog g : fog){
//				 if (!dev.getDeviceLongitude().equals(g.fog.getLongitude()))
//				 {
//					 double lat = Creator.Latency.DFLatency(dev, g);				 
//					 myFogMap.put(g, dev.getDevLatatency() + lat);
//				 }
//			 }
//			 
//			 // For ascending order of latencies
//			 Stream<Map.Entry<Fog, Double>> sorted = myFogMap.entrySet().stream().sorted(Map.Entry.comparingByValue());
//			 // To take the top 3 fogs for a pool
//			 Map<Fog, Double> top3 = myFogMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).limit(choice).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)); 
//			 
//			 myPool3 = new ArrayList<>(top3.keySet());
//	
//			 return myPool3;		
//		 }		
//		
		public static List<Fog> poolFogs(Device dev, Fog homefog, List<Fog> fog, int choice){
			// return the 3 closest fogs
			 List<Fog> myPool3;// = new ArrayList<Fog>();
			 double latency = 0;
			 HashMap<Fog, Double> myFogMap = new HashMap<Fog, Double>();
			 
			 //Lets randomly pick 3 fogs
			 for (Fog g : fog){
				 
				 for (Link link : fogLinks){
					 	if (homefog.equals(link.getSource()) && g.equals(link.getDestination()) || homefog.equals(link.getDestination()) && g.equals(link.getSource())){
						latency = dev.getDevLatatency() + link.FogLatency();
//						System.out.println("Im here in poolFogs " );
						 myFogMap.put(g, latency);						
					 }
				 }
			 }
			 
			 // For ascending order of latencies
			 Stream<Map.Entry<Fog, Double>> sorted = myFogMap.entrySet().stream().sorted(Map.Entry.comparingByValue());		 
			 // To take the top 3 fogs for a pool
			 Map<Fog, Double> top3 = myFogMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).limit(choice).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)); 
			 
			 myPool3 = new ArrayList<>(top3.keySet());
	
			 return myPool3;		
		 }
		
	 public static List<Fog> PO2 (List<Fog> Fogs){
			 
			 List<Fog> po2 = new ArrayList<Fog>();
			 
			 int random  = (int)(Math.random() * Fogs.size() );
	
			 po2.add(Fogs.get(random));
			 
			 
			 int anotherRandom = (int)(Math.random() * Fogs.size() );
	
			 while (random == anotherRandom){
				 anotherRandom = (int)(Math.random() * Fogs.size() );
			 }
			 po2.add(Fogs.get(anotherRandom));		 
			 
			 return po2;
		 }
	 
	 public static void main (String[] args) throws IOException{
		 Chrono timer = new Chrono();
		 RESULTS = args[0];
		 CLOUDUTIL = args[1];
		 SUBMITTED = args[2];
		 HOME_RESP = args[3];
		 POOL_RESP = args[4];
		 CLOUD_RESP = args[5];
		 RESPTIME = args[6];
		 ALPHA = Double.valueOf(args[7]);// Integer.valueOf(args[1]);
		 DECAY = Double.valueOf(args[8]);
		 ORDER = Double.valueOf(args[9]);
		 NUMAPP = Integer.valueOf(args[10]);
		 OPTION = Integer.valueOf(args[11]);
		 NUMBER_OF_FOGS_FAILED = Integer.valueOf(args[12]);

		
			 JAMCloud sim = new JAMCloud(OPTION);		 
			 sim.simulOneRun(); 
			 sim.printReport(OPTION);
			 System.out.println("Home % : " + df.format(JAMCloud.percentHome));
			 System.out.println("Pool % : " + df.format(JAMCloud.percentPool));
			 System.out.println("Cloud % : " + df.format(JAMCloud.percentCloud));

		 System.out.println ("Total CPU time: " + timer.format() + "Algorithm : " + OPTION);

	 }
	 
	 class Link{
		 Fog source;
		 Fog destination;
		 double total_mb_used;
		 double bandwidth;
		 
		 public Link(Fog A, Fog B, double bandwidth){
			 this.source = A;
			 this.destination = B;
			 total_mb_used = 0;
			 this.bandwidth = bandwidth;
		 }
		 
		 public Fog getSource(){
			 return source;
		 }
		 
		 public Fog getDestination(){
			 return destination;
		 }
		// At the start of the transfer, we increment the total_mb by the specified mb required for the task
		 public double incrementRequestMb(int mb_required){
			 total_mb_used += mb_required; 
			 return total_mb_used; 
		 }
		 
		 // At the end of the transfer, we decrement the total_mb by the mb used by the task
		 public double decrementRequestMB(int mb_required){
			 total_mb_used -= mb_required;
			 return total_mb_used;
		 }
		 
		 public double FogLatency(){					
				
				double delay =  Math.sqrt((Math.pow(source.fog.getLongitude()- source.fog.getLongitude(),2)) + 
						Math.pow(source.fog.getLatitude() - destination.fog.getLatitude(), 2));

				double latency =total_mb_used/bandwidth * delay;


				if(total_mb_used <= bandwidth){

					return delay;
				}
				else{

					return latency;
				}

			}
	 }
}