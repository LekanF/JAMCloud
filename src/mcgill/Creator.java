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
	
	static List<Fog> remFogs = new ArrayList<Fog>(); // the remaining fogs that we want to convert to devices for allocation optimizer 
		
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
//			for (int i = 0; i < myMap.getSize(); i ++){		
//				
//				bufferedWriter.write(myMap.getId(i) + " " + myMap.getLong(i) + " " + myMap.getLat(i));
//				
//				bufferedWriter.write("\n");
//			}
//			bufferedWriter.flush();
//			bufferedWriter.close();

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
		int co = 1; int id = 1;
		//How many nodes has how many edges e.g 21 nodes have 1 edge connection. 1 -21, 2 - 96, 3 - 46, 4 - 16, 5 - 4, 6 -1, 7,8  - 1. total is 196 nodes
		for (Entry<Integer, Integer> entry: nodeDegree.entrySet()){
			// Checks if the node has more than 4 connections, make it a fog
//			if (entry.getValue() >= 0){ // PUT 1 OR 0 HERE AND YOULL GET THE WHOLE 196
			if (entry.getValue() >= 4 && entry.getValue() < 6){ 

				co++;
				for (int i = 0; i < myMap.getSize(); i++){
					if(entry.getKey() == myMap.getId(i)){//assign fogs to nodes which have more than 4 node degrees
//						Fog tempFog = new Fog(myMap.getId(i), myMap.getLat(i), myMap.getLong(i));
						Fog tempFog = new Fog(id++, myMap.getLat(i), myMap.getLong(i));
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
		
		for (int i = 0; i < myMap.getSize(); i++){
			Fog tempFog = new Fog(myMap.getId(i), myMap.getLat(i), myMap.getLong(i));
			remFogs.add(tempFog);
		}
		
		for (int c = 0; c < clouds.size(); c++){
			Fog cloudNode = new Fog (clouds.get(c).getId(), clouds.get(c), 100.0, 100.0);
			cloudNodes.add(cloudNode);
		}
				
		Util.Compress Mcapacity = new Util.Compress();
		Util.Latency latent = new Util.Latency();
		
		// It seems too much reading and writing to files is slowing down the computation
//		Mcapacity.fileSorter("machine_events.csv", "machine_output1.txt", 3);
//
//		List<Float> machineCPU = new ArrayList<Float>();
//		List<Float> machineMem = new ArrayList<Float>();
//		float total = 0; float totalMem = 0;
//		int counter = 0;
//		
//		// This aggregates the CPUs
//		for(int i = 0; i < 700; i++ ){ //500/20 gives 25 Fogs
//			total += Mcapacity.machine.getCPU(i);
//			totalMem += Mcapacity.machine.getMemory(i);
//			counter++;
//			
//			if(counter == 10){
//				machineCPU.add(total);
//				machineMem.add(totalMem);
//				total = 0;
//				counter = 0;
//			}
//		}
	
		
		for(int i = 0; i < fogs.size();i++){ //  Adds data from google to cogent nodes, only 23

//			Fog capacitatedFog = new Fog(fogs.get(i).getId(), fogs.get(i), machineCPU.get(i), machineMem.get(i) );
			Fog capacitatedFog = new Fog(fogs.get(i).getId(), fogs.get(i), 10, 10 );
			
			//we are using ids from 1-20 instead of cogent ids to accomodate alloc optimizer
//			Fog capacitatedFog = new Fog(id++, fogs.get(i), 10, 10 );

				googleFogs.add(capacitatedFog);                                             
		}		
	}
	
	public static boolean Contains(List<Fog>fogs, Fog f) {
		for (Fog fog : fogs) {
			if(f.equals(fog.fog)) // We use fog.fog because of googlefogs/fogN in calling method
				return true;
			else
				return false;
		}
		return false;
	}
	
//	public static List<Fog> 
	
	public static List<Device> createDevicesForFogs() throws FileNotFoundException, IOException{
		getNodeEdge();
		int j = 0;
		List<Device> allDevices = new ArrayList<Device>();


		
		//Using uneven  number of devices 23 *24/2
		for (Integer i = 0; i < googleFogs.size(); i++){
			Integer numDev = i+5;
			for (Integer k = 0; k < numDev; k++){
//			for (Integer k = 0; k < 2; k++){ // Used for testing purposes
				String id = googleFogs.get(i).fog.getId().toString() + k.toString();
				// Here we are creating devices around a fog and giving it latency to that particular fog
				Device myDevice = new Device(Integer.parseInt(id), googleFogs.get(i).fog.getLongitude(), googleFogs.get(i).fog.getLatitude(), calEdgeLatencies());
//				myDevice.insertLatency(calEdgeLatencies() + Latency.DFLatency(myDevice, fog));
				allDevices.add(myDevice);
			}
		}
		
		return allDevices;
	}
	
	public static List<Device> createNewDevicesFromFile() throws FileNotFoundException, IOException{
		
		getNodeEdge();
		List<Device> newDevices = new ArrayList<Device>();
		
		try(BufferedReader reader = new BufferedReader(new FileReader("newDevices.txt"))){
			String curLine;
			String[] splits;
			
			int id = 1; double lng, lat;
			while ((curLine = reader.readLine()) != null) {
				curLine.trim();
				splits = curLine.split(" ");
//				id = Integer.parseInt(splits[0]);
				lng = Double.parseDouble(splits[1]);
				lat = Double.parseDouble(splits[2]);
				
				// we are using id from 1 - 166 instead of cogent ids so as to accomodate allocation optimizer ids
				Device myDevice = new Device(id++, lng, lat, calEdgeLatencies());

				newDevices.add(myDevice);
			}
		}
		return newDevices;
	}
	
	static LinkedList<Double> waitList;
	static LinkedList<Double> servList;


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
		
	}
	
}
