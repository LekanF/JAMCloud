package mcgill;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Device {
	

	double cpu, memory, disk, lng, lat;
	private Util.TaskCompressor tasks;
	
	Util.Compress deviceTasks;
	Integer deviceId;
	double deviceLongitude, deviceLatitude;
	double devLatency;
	Util.SaveRealDummy save;
	
	public Device(int id, double lng, double lat, double latency){
		this.deviceId = id;
		this.deviceLongitude = lng;
		this.deviceLatitude = lat;
		this.devLatency = latency;
		save = new Util.SaveRealDummy();
	}
	
	public String toString()
	{
		return this.deviceId.toString();
	}
	
	public Integer getDeviceID(){
		return this.deviceId;
	}
	public double getDevLatatency(){
		return devLatency;
	}
	
	public Task generateTasksRequest() throws FileNotFoundException, IOException{
		Util.Compress numTasks;
		numTasks = new Util.Compress();
		numTasks.fileSorter("task_events.csv", "task_output.txt", 4);
		
		Task myTasks  = new Task();
		int random = (int)(Math.random()*800 + 1);
		
		for (int i = 0; i < random; i++){
			int ran = (int)(Math.random()*1000+1);
			myTasks.insert(numTasks.task.getNumCPU(ran+i), numTasks.task.getMem(ran+i));		
		}		
		return myTasks;		
	}
	
	public Device(int dID, double devlat, Util.Compress t){
		this.deviceId = dID;
		this.devLatency = devlat;
		this.deviceTasks = t;		
	}
	
	
	public void insertTask(int taskId, double cpu, double mem){
		this.deviceId = taskId;
		this.cpu = cpu;
		this.memory = mem;
	}
	
	public Integer getDevice_id() {
		return deviceId;
	}

	public void setDevice_id(int dev_id) {
		this.deviceId = dev_id;
	}

	public Double getDeviceLongitude() {
		return deviceLongitude;
	}

	public void setDeviceLongitude(double lng) {
		this.deviceLongitude = lng;
	}

	public Double getDeviceLatitude() {
		return deviceLatitude;
	}

	public void setDeviceLatitude(double lat) {
		this.deviceLatitude = lat;
	}
	
	public Util.Compress getDeviceTasks(){
		return deviceTasks;
	}
	
	public void setDeviceTasks(Util.Compress t){
		this.deviceTasks = t;
	}
	
	private String id;
	
	public Device(){
		
	}

	public Device(double latency, double taskCPU, double taskMem){
		this.devLatency = latency;
		this.cpu = taskCPU;
		this.memory = taskMem;		
	}
	
	public static class Task{
		List<Double> taskCPU; List<Double> taskMem;
		private double cpu, mem;
		private final int duration = 3;
		
		public Task(){
			taskCPU = new ArrayList<Double>();
			taskMem = new ArrayList<Double>();
		}
		public void insert(double cpu, double mem){
			taskCPU.add(cpu);
			taskMem.add(mem);
		}
		public void setCPU(int index){
			this.cpu = index;
		}
		public double getCPU(int index){
			return taskCPU.get(index);
		}
		public void setMem(int index){
			
		}
		public double getMem(int index){
			return taskMem.get(index);
		}
		public int getSize(){
			return taskCPU.size();
		}
		
	}
	
}
