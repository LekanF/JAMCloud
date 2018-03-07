package mcgill;

import java.util.ArrayList;
import java.util.List;

import umontreal.iro.lecuyer.simprocs.*;
public class Fog {
	Integer id;
	String zone_id;
	Double longitude, latitude, cpu, memory;
	Fog fog;
	Resource jresource;
	Util.UtilizationMap utilise; 
	Util.SaveRealDummy dummyVals = new Util.SaveRealDummy();
	List<Double> util_values;
	
	static double numOff = 0;
	
	public Integer getId() {
		return id;
	}
	
	public void fail(){
		this.fog = null;

	}
	public void setId(int id) {
		this.id = id;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public boolean isAvailable(){
		// Check if it can handle the task, by checking its availability with current run tasks
		return true;
	}
	public Fog(Integer ID, double lng, double lat){ //Cloud initiator
		this.id= ID;
		this.longitude = lng;
		this.latitude = lat;
		jresource = new Resource(100, ID.toString()); 
	}
	
	public Fog(Integer ID, Fog f, double c, double mem, Resource r ){
		this.id = ID;
		this.fog = f;
		this.cpu = c;
		this.memory = mem;
	}
	
	public Fog(Integer ID, Fog f, double c, double mem){
		this.id = ID;
		this.fog = f;
		this.cpu = c;
		this.memory = mem;

		jresource = new Resource(this.cpu.intValue(), ID.toString()); // Use this - Its the Real one

		utilise = new Util.UtilizationMap();
		util_values = new ArrayList<Double>();

	}
	
	public void incrementNumOff(){
		numOff++;
	}
	
	public double getNumOffload(){
		return numOff;
	}
	
	public int getCapacity(){
		return this.cpu.intValue();
		
	}

	
	public String toString()
	{
		return this.id.toString() + " " + this.longitude.toString() + " " + this.latitude.toString();
	}
	
	public String toGString()
	{
		return "Fog capacities:  " + this.fog.toString() + " " + this.cpu.toString() +" " + this.memory.toString();
	}
	private void processRequest() {
		// TODO Auto-generated method stub
		
	}
}
