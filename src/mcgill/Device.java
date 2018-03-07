package mcgill;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Device {
	
//	private String zone_id;
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
		
		for (int i = 0; i < random; i++){//put random instead of 3
			int ran = (int)(Math.random()*1000+1);
			myTasks.insert(numTasks.task.getNumCPU(ran+i), numTasks.task.getMem(ran+i));		
		}		
		return myTasks;		
	}
	
	public Device(int dID, double devlat, Util.Compress t){
		this.deviceId = dID;
//		this.deviceLongitude = devLong;
		this.devLatency = devlat;
		this.deviceTasks = t;		
	}
	
//	public void insertTask(int taskId, Util.TaskCompressor t){
//		this.tasks = t;
//		this.dId = taskId;
//	}
	
	public void insertTask(int taskId, double cpu, double mem){
//		this.tasks = t;
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
	// A device with just one task
	public Device(double latency, double taskCPU, double taskMem){
		this.devLatency = latency;
		this.cpu = taskCPU;
		this.memory = taskMem;		
	}
	
	public void createDevice(int numberOfTasks) throws FileNotFoundException, IOException{
		deviceTasks = new Util.Compress();
		deviceTasks.fileSorter("task_events.csv", "task_output.txt", 4);
		
		// Create tasks
		for (int i = 0 ; i < numberOfTasks; i ++){
			insertTask(i, deviceTasks.task.getNumCPU(i), deviceTasks.task.getMem(i));
		}
	}
	
	public void submitRequest(Fog f, Device d){
	
		
	}
	
	public void generateTaskForDevice(){
		// get a device and add tasks to it from Util
		// However, we should be able to access the individual tasks
			
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
	
//	class Generator extends Event{
//		Queue taskQueue;
//		@Override
//		void execute(AbstractSimulator simulator) {
//			// TODO Auto-generated method stub
//			Task task  = new Task();
//			
//		}
//		
//	}
	
//	class Queue{
//		private java.util.Vector tasks = new java.util.Vector();
//		
//		Fog fog;
//		/* Add a task to the queue 
//		 * If the fog is available (which also implies that this queue is empty),
//		 * pass the customer to the queue
//		 * Where this becomes interesting is, once there is a task at the queue
//		 * it means the fog is at full capacity, 
//		 * We can determine this moment and pass the task to an available fog in the pool
//		 * note this time */
//		void insert(AbstractSimulator simulator, Task task){
////			if (fog.)
//		}
//	}
	
}
