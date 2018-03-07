package mcgill;

import java.io.BufferedReader;
import java.util.Random;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.sun.javafx.css.CalculatedValue;

import mcgill.Util.Compress;
import umontreal.iro.lecuyer.simprocs.*;

public class Creator  extends JFrame {
	public static final String FILENAME = "Cogentco.gml";
	public static Util.InputMap myMap = new Util.InputMap();;
	public static Util.SourceTarget myEdge =  new Util.SourceTarget();
	private static Point2D.Double[] points;
	
	static List<Fog> fogs = new ArrayList<Fog>(); // Fogs with cogentData only
	static List<Fog> googleFogs = new ArrayList<Fog>(); // Fogs with location Data and google capacities.
	static List<Fog> clouds = new ArrayList<Fog>(); // Cloud with no google capacity (no '.fog' needed)
	static List<Fog> cloudNodes = new ArrayList<Fog>(); // Cloud with no google capacity ('.fog' needed)
		
	//Here, not really necessary, trying to get the rectangles
	public Creator(){
		super("JamCloud Simulation");
		
		getContentPane().setBackground(Color.WHITE);
		setSize(1580, 1300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
	}
	
	public static void clear(){
		myMap = null;
		myEdge = null;
	}
	
	public static void reader() throws IOException, FileNotFoundException{
		
		// Variables help to identify the id, longitude and latitude in the cogent file
		int id = -1;
		double lng = 0;
		double lat = 0;
		
		// helps to identify the edges
		int source = -1;
		int target = -1;
		BufferedWriter bufferedWriter;
		
		// trying to write output to a file called fogResults.txt
		try(FileWriter fw = new FileWriter("fogResults.txt", false)){
			bufferedWriter = new BufferedWriter(fw);
			
			// read the contents of the cogentco file
			try (BufferedReader bufferedReader = new BufferedReader(new FileReader(FILENAME))) {
				String currentLine;
				
				
				while((currentLine = bufferedReader.readLine()) != null){
					currentLine = currentLine.trim();
					
					//Nodes
					//while reading line by line, we look out for lines with id and take the value after the space
					if (currentLine.startsWith("id")){
						String [] splits = currentLine.split(" ");
						id = Integer.parseInt(splits[1]);					
					}
					if (currentLine.startsWith("Longitude")){
						String [] splits = currentLine.split(" ");
						lng = Double.parseDouble(splits[1]);
					
					}
					if (currentLine.startsWith("Latitude")){
						String [] splits = currentLine.split(" ");
						lat = Double.parseDouble(splits[1]);
					}
					
					// Here, we insert the id, long, and lat into a custom array myMap
					if (id != -1 && lng != 0 && lat != 0){
						myMap.insert(id, lat, lng);
						id = -1;
						lng = 0;
						lat = 0;						
					}
					
					// Edges
					//Checks the edges data for source and data and take their ids.
					if (currentLine.startsWith("source")){
						String [] splits = currentLine.split(" ");
						source = Integer.parseInt(splits[1]);					
					}

					if (currentLine.startsWith("target")){
						String [] splits = currentLine.split(" ");
						target = Integer.parseInt(splits[1]);					
					}
					
					if (source != -1 && target != -1 ){
						myEdge.insert(source, target);
						source = -1;
						target = -1;									
					}
				}
				bufferedReader.close();	
			} 
			
			//Here we write to file, not really necessarily but it is for bigger files.
			//Dont use myMap because of memnory constraints going forward.
			for (int i = 0; i < myMap.getSize(); i ++){		
				
				bufferedWriter.write(myMap.getId(i) + " " + myMap.getLong(i) + " " + myMap.getLat(i));
				
				bufferedWriter.write("\n");
			}
			bufferedWriter.flush();
			bufferedWriter.close();

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
		
	public static double calEdgeLatencies() throws FileNotFoundException, IOException{
		reader();
		ArrayList<Double> numbers = new ArrayList<Double>();
		double srcLong = 0, srcLat = 0, tarLong = 0, tarLat = 0;
		Util.Latency myLat = new Util.Latency();
				
			for (int i = 0; i < myEdge.getSize(); i++){				
			
			int src = myEdge.getSource(i);	
			int dest = myEdge.getTarget(i);
		
			for (int j = 0; j < myMap.getSize(); j++){
				// Gets long and lat for edge src
				if (myMap.getId(j) == src){
					srcLong = myMap.getLong(j);
					srcLat = myMap.getLat(j);
				}				
				// Gets long and lat for edge destination
		
				if(myMap.getId(j) == dest){
					tarLong = myMap.getLong(j);
					tarLat = myMap.getLat(j);
				}
			}			
			
			numbers.add(myLat.queueLatency(srcLong, srcLat, tarLong, tarLat));
			}
			
		myLat.meanLatency(numbers);
		myLat.stdLatency(numbers);
		return myLat.deviceLatency();		
	}
	
	public static void getNodeEdge() throws FileNotFoundException, IOException{
		reader();
		int outDegree = 0, inDegree = 0;		
		
		Map<Integer, Integer> nodeDegree = new HashMap<Integer, Integer>();
						
		for (int i = 0; i < myMap.getSize(); i ++){

			for (int j = 0; j < myEdge.getSize(); j++){
			
				if (myMap.getId(i) == myEdge.getSource(j)){ // Gets the outDegree - how many connections leaving the node
					outDegree += 1;
				}
				if (myMap.getId(i) == myEdge.getTarget(j)){ // Gets the inDegree - how many connections coming into the node
					inDegree += 1;
				}				
			}
			nodeDegree.put(myMap.getId(i), outDegree + inDegree);
			outDegree = 0; inDegree = 0;

		}
		int co = 1;
		//How many nodes has how many edges e.g 21 nodes have 1 edge connection. 1 -21, 2 - 96, 3 - 46, 4 - 16, 5 - 4, 6 -1, 7,8  - 1. total is 196 nodes
		for (Entry<Integer, Integer> entry: nodeDegree.entrySet()){
			// Checks if the node has more than 4 connections, make it a fog
//			if (entry.getValue() >= 0){ // PUT 1 OR 0 HERE AND YOULL GET THE WHOLE 196
			if (entry.getValue() >= 4 && entry.getValue() < 6){ 

				co++;
				for (int i = 0; i < myMap.getSize(); i++){
					if(entry.getKey() == myMap.getId(i)){//assign fogs to nodes which have more than 4 node degrees
						Fog tempFog = new Fog(myMap.getId(i), myMap.getLat(i), myMap.getLong(i));
						fogs.add(tempFog);
					}
				}
			}
			if (entry.getValue() >= 6){
				for (int j = 0; j < myMap.getSize(); j++){
					if (entry.getKey() == myMap.getId(j)){ // Assign clouds to nodes which have 3 node degrees
						Fog tempCloud = new Fog(myMap.getId(j), myMap.getLat(j), myMap.getLong(j));
						clouds.add(tempCloud);
					}
				}
			}
		}
		
		for (int c = 0; c < clouds.size(); c++){
			Fog cloudNode = new Fog (clouds.get(c).getId(), clouds.get(c), 100.0, 100.0);
			cloudNodes.add(cloudNode);
		}
				
		Util.Compress Mcapacity = new Util.Compress();
		Util.Latency latent = new Util.Latency();
		
		Mcapacity.fileSorter("machine_events.csv", "machine_output1.txt", 3);

		List<Float> machineCPU = new ArrayList<Float>();
		List<Float> machineMem = new ArrayList<Float>();
		float total = 0; float totalMem = 0;
		int counter = 0;
		
		// This aggregates the CPUs
		for(int i = 0; i < 700; i++ ){ //500/20 gives 25 Fogs
			total += Mcapacity.machine.getCPU(i);
			totalMem += Mcapacity.machine.getMemory(i);
			counter++;
			
			if(counter == 10){
				machineCPU.add(total);
				machineMem.add(totalMem);
				total = 0;
				counter = 0;
			}
		}
	
		
		for(int i = 0; i < fogs.size();i++){ //  Adds data from google to cogent nodes, only 23

			Fog capacitatedFog = new Fog(fogs.get(i).getId(), fogs.get(i), machineCPU.get(i), machineMem.get(i) );
				googleFogs.add(capacitatedFog);                                             
		}		
	}
	
	
	
	public static List<Device> createDevicesForFogs() throws FileNotFoundException, IOException{
		getNodeEdge();
		int j = 0;
		List<Device> allDevices = new ArrayList<Device>();


		
		//Using uneven  number of devices 23 *24/2
		for (Integer i = 0; i < googleFogs.size(); i++){
			Integer numDev = i+5;
			for (Integer k = 0; k < numDev; k++){
//			for (Integer k = 0; k < 2; k++){ // Used for testing purposes
				String id = googleFogs.get(i).fog.getId().toString() + i.toString();
				// Here we are creating devices around a fog and giving it latency to that particular fog
				Device myDevice = new Device(Integer.parseInt(id), googleFogs.get(i).fog.getLongitude(), googleFogs.get(i).fog.getLatitude(), calEdgeLatencies());
//				myDevice.insertLatency(calEdgeLatencies() + Latency.DFLatency(myDevice, fog));
				allDevices.add(myDevice);
			}
		}
		
		return allDevices;
	}
	static LinkedList<Double> waitList;
	static LinkedList<Double> servList;

	
	
//	public static void mapTasksToFogs() throws FileNotFoundException, IOException{
////		reader();
//		getNodeEdge();
//		Util.Compress numTasks;
//		
//		Pool myPool;
//		
//		numTasks = new Util.Compress();
//		numTasks.fileSorter("task_events.csv", "task_output.txt", 4);
//		
////		Device myDev = new Device();
//		int random;
//		double remCPU = 0, remMem = 0;
//		int count = 1; 
//		ArrayList<Double> fogLatencyNumbers = new ArrayList<Double>();
//		double res;
//		Util.Latency latent = new Util.Latency();
//		
//		double average = 0, total = 0, sd = 0, stdev = 0;
//		//Get latency with respect to 0;
//		for (Fog fog : googleFogs) {
////		for ( int i = 0 ; i < googleFogs.size(); i ++){
//			// this is wrong, I need to get the edges and compute the mean and std
//			
////			res = latent.queueLatency(googleFogs.get(0).fog.getLongitude(), googleFogs.get(0).fog.getLatitude() ,googleFogs.get(i).fog.getLongitude(), googleFogs.get(i).fog.getLatitude());
////			Above, I mapped the first fog as a relative to other fogs.
//			res = latent.queueLatency(0, 0, fog.fog.getLongitude(), fog.fog.getLatitude());
//			fogLatencyNumbers.add(res);
//		}
//		latent.meanLatency(fogLatencyNumbers);
//		latent.stdLatency(fogLatencyNumbers);
//		double devlat = latent.deviceLatency();
//		
//		List<Device> allDevices = new ArrayList<Device>();
//		
//		for (Fog fog : googleFogs) {			
//			
//			int global = 0;
//			remCPU = fog.cpu; remMem = fog.memory;
//			random = (int)(Math.random() * 100 + 1); // create random number of tasks per fog up to 100.
//			// Create a random number of devices per fog
//			for (int j = 0; j < random; j++){
//			// Create a task per fog
//				Device fogDevice  = new Device(devlat, numTasks.task.getNumCPU(j), numTasks.task.getMem(j));
////				Device fogDevice  = new Device(Latency.DFLatency(fogDevice, fog), numTasks.task.getNumCPU(j), numTasks.task.getMem(j));
//				allDevices.add(fogDevice);
//			}
//			
//			for(int i = 0; i < random; i++){			
//				remCPU = remCPU - numTasks.task.getNumCPU(i);			
//				remMem = remMem - numTasks.task.getMem(i);	
//				
//				if (remCPU <= 0 || remMem <= 0){
//					global = i;
//					break;
//				}
//			}			 
//			
//		}
////		myPool = new Pool();
////		myPool.cPool(googleFogs);
////		System.out.println("CPU Pool : " + myPool.cpuPool);
//	}

	static class Latency {
		static double bandwidth = 50; //10Gbps
		static double requestSize = 1; // Size R comes from the task
		double delay;
		double latency;
		static int numberOfTasks=0;
		
		public double computeDistance(double devLong, double devLat, double fogLong, double fogLat){
			double distanceLatency;
			 
			distanceLatency = Math.sqrt((Math.pow(devLong - fogLong,2)) + (Math.pow(devLat - fogLat,2)));
			return distanceLatency;
		}
		
		public double calLatency(double bandwidth, double taskSize, double delay){
			
			latency = taskSize/bandwidth + delay;
			return latency;
		}
		
		public static double DFLatency(Device d, Fog f){
			
			double delay =  Math.sqrt(	Math.pow(d.getDeviceLongitude()- f.fog.getLongitude(),2) + 
					Math.pow(d.getDeviceLatitude() - f.fog.getLatitude(), 2)	);
			double latency;
			if (numberOfTasks <= bandwidth){

				return delay;
			}
			if (numberOfTasks > bandwidth){
				latency =((double)numberOfTasks / (double) bandwidth) * delay; 

				return latency;
			}
			return delay;
		}
		
		
		
//		public void algorithm(){
//			 schedule next task
//			if tasks is first from device{
//			sendTasktoFog(time 0) {task uses a link to reach fog and back and also gets served}
//			sendTaskToPool(time 0)
//			get response times and calc delay delay = pool - fog 
//			}
//			
//			else 
//				send to fog at arrival time
//			if fog returns result before delay, dont send to pool
//			otherwise schedule sendtopool(arrTiem + delay)
//			subsequent tasks is sent to fog at arrival time but  to pool at arrivaltime + delay
//			get response times and calculate delay delay = delay + (p-f)
//			
//			if fog returns result before delay dont send to pool
//			keep sending to both until delay caps out
//			if fog response time > delay
//			sendnexttasktoFogOnly()
//			if fog responsetime < delay
//			repeat process and send to both 
//			if fogresponse time 
//			}
			
		//}
		
		
		
	}
	
//	 class NonStaticLatency {
//			double bandwidth = 10; //10Gbps
//			double requestSize = 1; // Size R comes from the task
//			double delay;
//			double latency;
//			int numberOfTasks=0;
//			
//			public double computeDistance(double devLong, double devLat, double fogLong, double fogLat){
//				double distanceLatency;
//				 
//				distanceLatency = Math.sqrt((Math.pow(devLong - fogLong,2)) + (Math.pow(devLat - fogLat,2)));
//				return distanceLatency;
//			}
//			
//			public double calLatency(double bandwidth, double taskSize, double delay){
//				
//				latency = taskSize/bandwidth + delay;
//				return latency;
//			}
//			
//			public double DFLatency(Device d, Fog f){
//				
//				double delay =  Math.sqrt((Math.pow(d.getDeviceLongitude()- f.fog.getLongitude(),2)) + 
//						Math.pow(d.getDeviceLatitude() - f.fog.getLatitude(), 2));
//				double latency = requestSize/bandwidth + delay;
//				
//				if (numberOfTasks < bandwidth) // link can handle the request
//					return delay;
//				else{ // link has overflowing requests
//					latency =((double)numberOfTasks / (double) bandwidth) * delay; 
//					return latency;
//				}
////				return latency;
////				return delay;
//			}
//		 }
//	
	class FogResource {
		int capacity;
		String name;
		
		public FogResource(int arg0, String arg1) {
//			super(arg0, arg1);
			this.capacity = arg0;
			this.name = arg1;
			// TODO Auto-generated constructor stub
		}
		
		public void FogProcessorSharing(){
			int total = 0;
			
			while (total < capacity){
				break;
			}
		}
	}
	
//	public static void main(String args[]){
//		LinkedList<Integer> linkedlist = new LinkedList<Integer>();
//				linkedlist.add(1);
//				linkedlist.add(2);
//				linkedlist.add(3);
//				linkedlist.add(4);
//				linkedlist.add(5);
//				
//				System.out.println("Linked List Content : " + linkedlist);
//	}
	
}
