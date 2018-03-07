package mcgill;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Util {
	
	public static class SaveRealDummy{
		List<String> id;
		List<Double> real;
		List<Double> dummy;
		
		public SaveRealDummy(){
			id = new ArrayList<String>();
			real = new ArrayList<Double>();
			dummy = new ArrayList<Double>();
		}
		
		public Boolean contain(String str){
			
			for (int i = 0; i < id.size(); i++){
				if (str.equals(id.get(i)))
					return true;				
			}
			return false;
		}
		
		public void update(String i, double r, double d){
			id.add(i);
			real.add(r);
			dummy.add(d);
		}
		
		public void clear(int index){
			id.set(index, null);
			real.set(index, null);
			dummy.set(index, null);
			
		}
		
		public String getID(int index){
			return id.get(index);
		}
		
		public Double getReal(int index){
			return real.get(index);
		}
		
		public void setReal(int index, double val){
			real.set(index, val);
		}
		
		public void setDummy(int index, double val){
			dummy.set(index, val);
		}
		
		public Double getDummy(int index){
			return dummy.get(index);
		}
		
		public double getRealAverage(){
			double sum = 0, average = 0;
			for (int i = 0; i < real.size(); i++){
				sum += real.get(i);
			}
			average = sum/real.size();
			return average;
		}
		
		public double getDummyAverage(){
			double sum = 0, average = 0;
			for (int i = 0; i < dummy.size(); i++){
				sum += dummy.get(i);
			}
			average = sum/dummy.size();
			return average;
		}
		
		public int getSize(){
			return id.size();
		}
	}
	
	public static class DeviceFogPool{
		List<Device> device;
		List<Fog> fog;
		
		public DeviceFogPool(){
			
		}
	}
	
	public static class Pool{
		
	}
	
	public static class UtilizationMap{
		List<Double> resourceUsed;
		List<Double> time;
		
		public UtilizationMap(){
			resourceUsed = new ArrayList<Double>();
			time  = new ArrayList<Double>();
		}
		
		public void update(double res, double t){
			resourceUsed.add(res);
			time.add(t);
		}
		
		public Double getResource(int index){
			return resourceUsed.get(index);
		}
		
		public Double getTime(int index){
			return time.get(index);
		}
		
		public Integer getSize(){
			return time.size();
		}
	}
	
	public static class InputMap{
		List<Integer> ids;
		List<Double> lats;
		List<Double> lngs;
		Integer id;
		Double lat, lng;
		
		public InputMap(){
			ids = new ArrayList<Integer>();
			lats = new ArrayList<Double>();
			lngs = new ArrayList<Double>();
		}
		public void insert(int id, double lat, double lng){
			ids.add(id);
			lats.add(lat);
			lngs.add(lng);
		}
		
		public int getId(int index){
			return ids.get(index);
		}
		public double getLat(int index){
			return lats.get(index);
		}
		public double getLong(int index){
			return lngs.get(index);
		}
		
		public double getMaxLong(){
			double max = 0;
			Collections.sort(lngs);
			max = lngs.get(lngs.size()-1);		
			return max;
		}
		
		public double getMinLong(){
			double min = 0;
			Collections.sort(lngs);
			min = lngs.get(0);		
			return min;
		}
		
		public double getMaxLat(){
			double max = 0;
			Collections.sort(lats);
			max = lats.get(lats.size()-1);		
			return max;
		}
		
		public double getMinLat(){
			double min = 0;
			Collections.sort(lats);
			min = lats.get(0);		
			return min;
		}
		
		public int getSize(){
			return ids.size();
		}
	}
	
	
	public static class SourceTarget{
		List<Integer> source;
		List<Integer> target;
		
		int src; int tar;
		
		public SourceTarget(){
			source = new ArrayList<Integer>();
			target = new ArrayList<Integer>();
		}
		
		public void insert(int src, int tar){
			source.add(src);
			target.add(tar);			
		}
		
		public int getSource(int index){
			return source.get(index);
		}
		
		public int getTarget(int index){
			return target.get(index);
		}
		
		public int getSize(){
			return source.size();
		}
	}
	
	public static class TaskCompressor{
		List<Double> jobID;
		List<Double> time;
		List<Float> CPUReq;
		List<Float> memReq;
		List<Float> diskReq;
		
		int jID, tme;
		float cpu, mem, disk;
		
		public TaskCompressor(){
			jobID = new ArrayList<Double>();
			time  = new ArrayList<Double>();
			CPUReq = new ArrayList<Float>();
			memReq = new ArrayList<Float>();
			diskReq = new ArrayList<Float>();
		}
		
		public Double getTime(int index){
			return time.get(index);
		}
		
		public Double getJobID(int index){
			return jobID.get(index);
		}
		
		public float getNumCPU(int index){
			return CPUReq.get(index);
		}
		
		public float getMem(int index){
			return memReq.get(index);
		}

		public float getDisk(int index){
			return diskReq.get(index);
		}
		
		public int getSize(){
			return time.size();
		}
		
		public void clear(){
			time.clear(); jobID.clear(); CPUReq.clear(); memReq.clear(); diskReq.clear();
		}
		
		public void insert( double tme, double jId, float cpu, float mem, float disk){
			time.add(tme);
			jobID.add(jId);
			CPUReq.add(cpu);
			memReq.add(mem);
			diskReq.add(disk);
		}
	}
	
	public static class MachineCompressor{
		List<Double> time;
		List<Float> CPU;
		List<Float> Memory;
		
		public MachineCompressor(){
			time = new ArrayList<Double>();
			CPU = new ArrayList<Float>();
			Memory = new ArrayList<Float>();
		}
		
		public void insert(double tme, float cpu, float mem){
			time.add(tme);
			CPU.add(cpu);
			Memory.add(mem);
		}
		
		public double getTime(int index){
			return time.get(index);
		}
		
		public float getCPU(int index){
			return CPU.get(index);
		}
		
		public float getMemory(int index){
			return Memory.get(index);
		}
		
		public int getSize(){
			return time.size();
		}
	}
	
	public static class Latency{
		double mean, stdev;
		public Latency(){
			
		}
		
		public double queueLatency(double firstLong, double firstLat, double secondLong, double secondLat){
			
			double distanceLatency;
			 
			distanceLatency = Math.sqrt((Math.pow(firstLong - secondLong,2)) + (Math.pow(firstLat - secondLat,2)));
			return distanceLatency;
		}
		
		public double meanLatency(ArrayList<Double> numbers){

			double total = 0;
			
			for (int i = 0; i < numbers.size(); i++){
				
				total += numbers.get(i);
			}
			mean = total/numbers.size();
			return mean;
		}
		
		public double stdLatency(ArrayList<Double> numbers){
			
			double sd = 0;
			double average = meanLatency(numbers);
			
			for (int j = 0; j < numbers.size(); j++){
				sd += Math.pow((numbers.get(j) - average), 2);
			}
			stdev = Math.sqrt(sd/numbers.size());
			
//			Random rno = new Random();
//			System.out.print("DevLatency: " + Math.abs( rno.nextGaussian()*average + stdev));
//			return Math.abs( rno.nextGaussian()* average + stdev);
			return stdev;
		}
		
		// Latencies between the device and fog
		public double deviceLatency(){
			
			Random rno = new Random();
			return Math.abs( rno.nextGaussian()* mean + stdev);
//			return Math.abs( rno.nextGaussian()* 5 + 2);
		}
		
		
	}
	public static class Compress{
		BufferedWriter bWriter;
		protected static TaskCompressor task = new TaskCompressor();
		ArrayList<TaskCompressor> tasks = new ArrayList<Util.TaskCompressor>();
		
		protected static MachineCompressor machine = new MachineCompressor();
		
		protected void fileSorter(String fileInput, String fileOutput, int n)throws IOException, FileNotFoundException{
			
			double T_time = 0; float T_CPURequest = -1f, T_MEMRequest = -1f, T_DiskRequest = -1f;
			double M_Time = 0; float M_CPU = -1f, M_Memory = -1f;
			double jobID = -1;
			
			try(FileWriter f = new FileWriter(fileOutput, false)){
				bWriter = new BufferedWriter(f);
				try (BufferedReader bReader = new BufferedReader(new FileReader(fileInput))){
					String sCrnttLine;
					String[] splits;
					double secondsTime = 6.00026913E8/10000000 + 1;
					
					while((sCrnttLine = bReader.readLine()) != null){
						sCrnttLine.trim();
						int i = 0;
						splits = sCrnttLine.split(",");
						if (n == 4 && splits.length == 13){ // Check to see if it is a taskevent, we need five attributes off the file
						
							T_time = Double.parseDouble(splits[0]) / 10000000;
							jobID = Double.parseDouble(splits[2]);
							T_CPURequest = Float.parseFloat(splits[9]);
							T_MEMRequest = Float.parseFloat(splits[10]);
							T_DiskRequest = Float.parseFloat(splits[11]);
							int count = 0;							
						
							if (T_time != 0 && jobID != -1 && T_CPURequest != -1f && T_MEMRequest != -1f && T_DiskRequest != -1f){
																					
								bWriter.write(T_time + " " + jobID + " " + T_CPURequest + " " + T_MEMRequest + " " + T_DiskRequest);
								bWriter.write("\n");
								task.insert(T_time, jobID, T_CPURequest, T_MEMRequest, T_DiskRequest);
//								bWriter.newLine();
								T_time = 0; T_CPURequest = -1f; T_MEMRequest = -1f; T_DiskRequest = -1f;
								jobID = -1;
							}
					
							
						}
						
						if (n == 3 && splits.length == 6){
							M_Time = Double.parseDouble(splits[0]);
							M_CPU = Float.parseFloat(splits[4]);
							M_Memory = Float.parseFloat(splits[5]);
							
							if (M_Time != 0 && M_CPU != -1f && M_Memory != -1f){
								bWriter.write(M_Time + " " + M_Memory  + " " + M_CPU);
								bWriter.write("\n");
								machine.insert(M_Time, M_CPU, M_Memory);
								M_Time = 0; M_CPU = -1f; M_Memory = -1f;
							}
						}
						if (T_time >= secondsTime){
							secondsTime = secondsTime+1;
							i++;
						}
						
					}
					bReader.close();
				}
				bWriter.flush();
				bWriter.close();
//				for (int k = 0; k<task.getSize(); k++)
//					System.out.println(k + ": " + task.getTime(k) + " " + task.getMem(k));
			}
			
		}
		
		public void taskSplitter(){
			for (int k = 0; k<task.getSize(); k++){
//				System.out.println(k + ": " + task.getTime(k) + " " + task.getMem(k));
				if(task.getTime(k) >= 60 && task.getTime(k) < 70){
					
				}
			}
		}
	}
}


