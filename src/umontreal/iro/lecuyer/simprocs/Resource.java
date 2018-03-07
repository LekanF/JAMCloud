

/*
 * Class:        Resource
 * Description:  resources with limited capacity which can be requested 
                 and released by Process objects.
 * Environment:  Java
 * Software:     SSJ 
 * Copyright (C) 2001  Pierre L'Ecuyer and Universite de Montreal
 * Organization: DIRO, Universite de Montreal
 * @author       
 * @since

 * SSJ is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or
 * any later version.

 * SSJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * A copy of the GNU General Public License is available at
   <a href="http://www.gnu.org/licenses">GPL licence site</a>.
 */

package umontreal.iro.lecuyer.simprocs;

import java.util.ListIterator;

import javax.swing.text.Utilities;

import mcgill.Util;
import umontreal.iro.lecuyer.util.PrintfFormat;
// import umontreal.iro.lecuyer.simevents.Simulator;
import umontreal.iro.lecuyer.simevents.LinkedListStat;
import umontreal.iro.lecuyer.simevents.Sim;
import umontreal.iro.lecuyer.simevents.Accumulate;
import umontreal.iro.lecuyer.simprocs.ProcessSimulator;
import umontreal.iro.lecuyer.stat.Tally;


/**
 * Objects of this class are resources having limited capacity,
 * and which can be requested and released by {@link Process} objects.
 * These resources act indirectly as synchonization devices for
 * processes.
 * 
 * <P>
 * A resource is created with a finite capacity, specified when 
 * invoking the {@link #Resource(int) Resource} constructor, and can be changed
 * later on.  A resource also has an infinite-capacity queue
 * (waiting line) and a service policy that defaults to FIFO and
 * can be changed later on.
 * 
 * <P>
 * A process must ask for a certain number of units of the resource
 * ({@link #request request}), and obtain it before using it.
 * When it is done with the resource, the process must release 
 * it so that others can use it ({@link #release release}).
 * A process does not have to request [release] all the resource
 * units that it needs by a single call to the {@link #request request}
 * [{@link #release release}] method.  It can make several successive requests
 * or releases, and can also hold different resources simultaneously.
 * 
 * <P>
 * Each resource maintains two lists: one contains the processes 
 * waiting for the resource (the waiting queue) and the other contains
 * the processes currently holding units of this resource.
 * The methods {@link #waitList waitList} and {@link #servList servList} permit one to
 * access these two lists.
 * These lists actually contain objects of the class {@link UserRecord}
 * instead of {@link SimProcess} objects.
 * 
 */
public class Resource  {

   private static final int FIFO  = 1;
   private static final int LIFO  = 2;
   
   private static final int LOCAL = 1;
   private static final int REMOTE = 2;
   
   private static final int REAL = 1;
   private static final int DUMMY = 2;
   
   private int remoteCount = 0;

        private ProcessSimulator sim;

        private String name;
        private int capacity = 0;
        private int available = 0;
        private int policy = FIFO;

        private LinkedListStat<UserRecord> serviceList;
        private LinkedListStat<UserRecord> waitingList;
        private LinkedListStat<UserRecord> remoteWaitingList;

        private boolean    stats = false;
        private double     initStatTime;
        private Accumulate statUtil;
        private Accumulate statCapacity;
        private Tally      statSojourn;
        
        private double alpha = 0;
        int count = 0;
        protected Util.SaveRealDummy save;// = new Util.SaveRealDummy();
        private double waitTime = 0;

   /**
    * Constructs a new resource linked with the default simulator,
    *   with initial capacity <TT>capacity</TT>, and service policy FIFO.
    * 
    * @param capacity initial capacity of the resource
    * 
    * 
    */
   public Resource (int capacity)  {
      this (capacity, "");
   }


   //Return the dummy and real start times
   public Util.SaveRealDummy getSave(){
	   return save;
   }
   /**
    * Constructs a new resource linked with the simulator
    *  <TT>sim</TT>, with initial capacity <TT>capacity</TT>, and service policy FIFO.
    * 
    * @param sim current simulator of the variable
    * 
    *    @param capacity initial capacity of the resource
    * 
    * 
    */
   public Resource (ProcessSimulator sim, int capacity)  {
      this (sim, capacity, "");
   }


   /**
    * Constructs a new resource linked with the default simulator, with
    *  initial capacity <TT>capacity</TT>, service policy FIFO, and identifier 
    *  <TT>name</TT>.
    * 
    * @param capacity initial capacity of the resource
    * 
    *    @param name identifier or name of the resource
    * 
    * 
    */
   public Resource (int capacity, String name)  {
      try {
         ProcessSimulator.initDefault();
         this.sim = (ProcessSimulator)ProcessSimulator.getDefaultSimulator();
         this.capacity = available = capacity;
         this.name = name;
         serviceList = new LinkedListStat<UserRecord> (
              sim," Service List for " + name);
         waitingList = new LinkedListStat<UserRecord> (
              sim," Waiting List for " + name);
         remoteWaitingList = new LinkedListStat<UserRecord> (
                 sim," Remote Waiting List for " + name);
         save = new Util.SaveRealDummy();
      }
      catch (ClassCastException e) {
         throw new IllegalArgumentException("Wrong default Simulator type");
      }
   }
   
   //Modified by Noah
   public Resource (int capacity, String name, Util.SaveRealDummy s)  {
	      try {
	         ProcessSimulator.initDefault();
	         this.sim = (ProcessSimulator)ProcessSimulator.getDefaultSimulator();
	         this.capacity = available = capacity;
	         this.name = name;
	         serviceList = new LinkedListStat<UserRecord> (
	              sim," Service List for " + name);
	         waitingList = new LinkedListStat<UserRecord> (
	              sim," Waiting List for " + name);
	         remoteWaitingList = new LinkedListStat<UserRecord> (
	                 sim," Remote Waiting List for " + name);
	         s = new Util.SaveRealDummy();
	         this.save = s;
	      }
	      catch (ClassCastException e) {
	         throw new IllegalArgumentException("Wrong default Simulator type");
	      }
	   }


   /**
    * Constructs a new resource linked with the simulator <TT>sim</TT>,
    *    with initial capacity <TT>capacity</TT>,
    *    service policy FIFO, and identifier (or name) <TT>name</TT>.
    * 
    * @param sim current simulator of the variable
    * 
    *    @param capacity initial capacity of the resource
    * 
    *    @param name identifier or name of the resource
    * 
    */
   public Resource (ProcessSimulator sim, int capacity, String name)  {
      this.capacity = available = capacity;
      this.name = name;
      this.sim = sim;
      serviceList = new LinkedListStat<UserRecord> (
          sim," Service List for " + name);
      waitingList = new LinkedListStat<UserRecord> (
          sim," Waiting List for " + name);
      remoteWaitingList = new LinkedListStat<UserRecord> (
              sim," Remote Waiting List for " + name);
      save = new Util.SaveRealDummy();
   }


   /**
    * Starts or stops collecting statistics on the lists returned
    *   by {@link #waitList waitList} and {@link #servList servList} for this resource.
    *   If the statistical collection is turned ON, the method
    *   also constructs (if not yet done) 
    *   and initializes three additional statistical
    *   collectors for this resource.  These collectors will be updated
    *   automatically.  They can be accessed via {@link #statOnCapacity statOnCapacity},
    *   {@link #statOnUtil statOnUtil}, and {@link #statOnSojourn statOnSojourn}, respectively.
    *   The first two, of class
    *    {@link umontreal.iro.lecuyer.simevents.Accumulate Accumulate},
    *    monitor the evolution of the
    *   capacity and of the utilization (number of units busy) of the resource 
    *   as a function of time.
    *   The third one, of class {@link umontreal.iro.lecuyer.stat.Tally Tally},
    *   collects statistics on the 
    *   processes sojourn times (wait + service);  it samples a new value
    *   each time a process releases all the units of this resource that it
    *   holds (i.e., when its {@link UserRecord} disappears).
    * 
    * @param b <TT>true</TT> to turn statistical collecting ON, <TT>false</TT> to turn it OFF
    * 
    * 
    */
   public void setStatCollecting (boolean b)  {
      if (b) {
         if (stats)
            throw new IllegalStateException ("Already collecting statistics for this resource");
         stats = true;
         waitingList.setStatCollecting (true);
         serviceList.setStatCollecting (true);
         remoteWaitingList.setStatCollecting(true);
         
         if (statUtil != null)
            initStat();
         else {
            statUtil = new Accumulate (sim, "StatOnUtil");
            statUtil.update (capacity - available);
            statCapacity = new Accumulate (sim, "StatOnCapacity");
            statCapacity.update (capacity);
            statSojourn = new Tally ("StatOnSojourn");
         }
      }
      else {
         if (stats)
           throw new IllegalStateException ("Not collecting statistics for this resource");
         stats = false;
         waitingList.setStatCollecting (false);
         serviceList.setStatCollecting (false);
         remoteWaitingList.setStatCollecting(false);
      }
   }


   /**
    * Reinitializes all the statistical collectors for this 
    *    resource.  These collectors must exist, i.e., 
    *    {@link #setStatCollecting setStatCollecting}&nbsp;<TT>(true)</TT> must have been invoked earlier for 
    *    this resource.
    * 
    */
   public void initStat()  {
        if (!stats)  throw new IllegalStateException (
                               "Not collecting statistics for this resource");
        statUtil.init();
        statUtil.update (capacity - available);
        statCapacity.init();
        statCapacity.update (capacity);
        statSojourn.init();
        serviceList.initStat();
        waitingList.initStat();
        remoteWaitingList.initStat();
        initStatTime = sim.time();
   }


   /**
    * Reinitializes this resource by clearing up its waiting list
    *    and service list.  The processes that were in these lists (if any)
    *    remain in the same states.   If statistical collection is ``on'',
    *    {@link #initStat initStat} is invoked as well.
    * 
    */
   public void init()  {
      serviceList.clear();
      waitingList.clear();
      remoteWaitingList.clear();
//      for (int i = 0; i < save.getSize(); i++){
//    	  save.clear(i);
//      }
      available = capacity;
      if (stats) initStat();
   }


   /**
    * Returns the current capacity of the resource.
    * 
    * @return the capacity of the resource
    * 
    */
   public int getCapacity()  {
      return capacity;
   }

   public void setAlpha(double a) {
	   alpha = a;
   }
   
   public double getAlpha(){
	   return alpha;
   }

   /**
    * Set the service policy to FIFO (<SPAN  CLASS="textit">first in, first out</SPAN>): 
    *    the processes are placed in the
    *    list (and served) according to their order of arrival.
    * 
    */
   public void setPolicyFIFO() {
      policy = FIFO;
   }


   /**
    * Set the service policy to LIFO (<SPAN  CLASS="textit">last in, first out</SPAN>):
    *    the processes are placed in the
    *    list (and served) according to their inverse order of arrival,
    *    the last arrived are served first.
    * 
    */
   public void setPolicyLIFO() {
      policy = LIFO;
   }


   /**
    * Modifies by <TT>diff</TT> units (increases if <TT>diff &gt; 0</TT>,
    *    decreases if <TT>diff &lt; 0</TT>) the capacity (i.e., the number of units)
    *    of the resource.
    *    If <TT>diff &gt; 0</TT> and there are processes in the waiting list whose
    *    request can now be satisfied, they obtain the resource.
    *    If <TT>diff &lt; 0</TT>, there must be at least <TT>diff</TT> units of this
    *    resource available, otherwise an illegal argument exception will be thrown,
    *    printing an  error message (this is not a strong limitation, because one 
    *    can check first and release some units, if needed, before invoking 
    *    <TT>changeCapacity</TT>).
    *    In particular, the capacity of a resource can never become negative.
    * 
    * @param diff number of capacity units to add to the actual capacity,
    *      can be negative to reduce the capacity
    * 
    *    @exception IllegalArgumentException if <TT>diff</TT> is negative and
    *      the capacity cannot be reduced as required
    * 
    *    @exception IllegalStateException if <TT>diff</TT> is negative and
    *     the capacity could not be reduced due to a lack of available units
    * 
    * 
    */
   public void changeCapacity (int diff)  {
             if (diff > 0) {
                available += diff;
                capacity += diff;
                if (waitingList.size() > 0) startNewCust();
             }
             else {
                if (-diff > available) 
//                   throw new IllegalArgumentException("Trying to diminish the capacity "
//                         + "of a resource more than its current availability");
                available -= -diff;
                capacity -= -diff;
                }
                if (stats) statCapacity.update (capacity);
    }


   /**
    * Sets the capacity to <TT>newcap</TT>.
    *    Equivalent to {@link #changeCapacity changeCapacity}&nbsp;<TT>(newcap - old)</TT> if <TT>old</TT> is the
    *    current capacity.
    * 
    * @param newcap new capacity of the resource
    * 
    *    @exception IllegalArgumentException if the passed capacity is negative
    * 
    *    @exception IllegalStateException if <TT>diff</TT> is negative and
    *     the capacity could not be reduced due to a lack of available units
    * 
    * 
    */
   public void setCapacity (int newcap)  {
      if (newcap < 0)
         throw new IllegalArgumentException ("capacity cannot be negative");
      changeCapacity (newcap - capacity);
   }
   
   

   /**
    * Returns the number of available units, i.e., the capacity
    *    minus the number of units busy.
    * 
    * @return the number of available units
    * 
    */
   public int getAvailable()  { 
      return available;
   }


   /**
    * The executing process invoking this method requests for
    *    <TT>n</TT> units of this resource.  If there are enough units available
    *    to fill up the request immediately, the process obtains the desired
    *    number of units and holds them until it invokes {@link #release release}
    *    for this same resource.  The process is also inserted into the 
    *    {@link #servList servList} list for this resource.
    *    On the other hand, if there are less than <TT>n</TT> units of this
    *    resource available, the executing process is placed into the 
    *    {@link #waitList waitList} list (the queue) for this resource and is suspended
    *    until it can obtain the requested number of units of the resource.
    * 
    * @param n number of required units
    * 
    */
   public void request (int n)  {
        SimProcess p = sim.currentProcess();
        UserRecord record = new UserRecord (n, p, sim.time());
        if (n <= available) {
            // The process gets the resource right away.
            available -= n;
            serviceList.addLast (record);
            if (stats) {
               waitingList.statSojourn().add (0.0);
               statUtil.update (capacity - available);
            }
        }
        else {
            // Not enough units of the resource are available.
            // The process joins the queue waitingList;
            switch (policy) {
                case FIFO : waitingList.addLast (record); break;
                case LIFO : waitingList.addFirst (record); break;
                default   : throw new IllegalStateException(
                                                "policy must be FIFO or LIFO");
            }
            p.suspend();
        }
   }
   
   public double request (int n, double servTime)  {
	   if (capacity == 0){
		   return 12000.0;
	   }
	   else {
	       SimProcess p = sim.currentProcess();
	       UserRecord record = new UserRecord (n, p, sim.time(), servTime);
	       if (n <= available) {
	           // The process gets the resource right away.
	           available -= n;
	           serviceList.addLast (record);
	           if (stats) {
	              waitingList.statSojourn().add (0.0);
	              statUtil.update (capacity - available);
	           }
	       }
	       else {
	           // Not enough units of the resource are available.
	           // The process joins the queue waitingList;
	           switch (policy) {
	               case FIFO : waitingList.addLast (record); waitTime += servTime; break;
	               case LIFO : waitingList.addFirst (record); waitTime += servTime; break;
	               default   : throw new IllegalStateException(
	                                               "policy must be FIFO or LIFO");
	           }
	           p.suspend();
	       }
	       	return 0.0;
	   }
  }
   
   public double request (int n, int queue, int classifier, String id, double executionTime, double arrTime) {
      if (capacity == 0){
    	  return 12000.0;
      }
      else {
		   SimProcess p = sim.currentProcess();
	       UserRecord record = new UserRecord (n, p, sim.time(), id, classifier, executionTime, arrTime);
	       if (classifier == REAL){
	    	   double time = 0;
		       if (n <= available) {
		    	   if (save.contain(record.id)){
		    		   for(int i=0; i < save.getSize(); i++){
	    				   if (id.equals(save.getID(i))){
	    					   if (save.getReal(i) == 0) 
	    						   save.setReal(i, Sim.time() + executionTime -arrTime);
		    			   }
	    			   }
		    	   }
		    	   else{ 
	    			   time = Sim.time() + executionTime - arrTime;
					   save.update(id, time, 0);
	    		   }
		    	   
		           // The process gets the resource right away.
		           available -= n;
		           serviceList.addLast (record);
		           if (stats) {
		              waitingList.statSojourn().add (0.0);
		              statUtil.update (capacity - available);
		           }
		       }
		       else {
		           // Not enough units of the resource are available.
		           // The process joins the queue waitingList;
		           switch (policy) {
	               case FIFO : 
	            	   if (queue == LOCAL) 
	            	   {
	            	   		waitingList.addLast (record); 
	            	   }
	               	else if (queue == REMOTE)
	               		{
	               			remoteWaitingList.addLast(record); 
	               		}
	               break;
	               case LIFO : 
	            	   if (queue == LOCAL) 
	            	   {
	            	   		waitingList.addFirst (record); 
	            	   }
	               	else if (queue == REMOTE)
	               		{
	               			remoteWaitingList.addFirst(record); 
	               		}

	               default   : throw new IllegalStateException(
	                                               "policy must be FIFO or LIFO");
	           }
	           p.suspend();
		       }
	       }
	       if (classifier == DUMMY){
	    	   if (n < available){
	    		   if (save.contain(record.id)){
	    			   for(int i=0; i < save.getSize(); i++){
	    				   if (id.equals(save.getID(i))){
	    					   if (save.getDummy(i) == 0) // If that val is 0, update otherwise leave it
	    						   save.setDummy(i, Sim.time() + executionTime -arrTime);
		    			   }
	    			   }
	    		   }
	    		   else{
	    			   save.update(record.id, 0, Sim.time() + executionTime - arrTime);
	    		   }
	    	   }
	    	   else{
	    		    // Not enough units of the resource are available.
		           // The process joins the queue waitingList;
		           switch (policy) {
	               case FIFO : 
	            	   if (queue == LOCAL) 
	            	   {
	            	   		waitingList.addLast (record); 
	            	   }
	               	else if (queue == REMOTE)
	               		{
	               			remoteWaitingList.addLast(record); 
	               		}
	               break;
	               case LIFO : 
	            	   if (queue == LOCAL) 
	            	   {
	            	   		waitingList.addFirst (record); 
	            	   }
	               	else if (queue == REMOTE)
	               		{
	               			remoteWaitingList.addFirst(record); 
	               		}
	//            	   waitingList.addFirst (record); break;
	               default   : throw new IllegalStateException(
	                                               "policy must be FIFO or LIFO");
	           }
	           p.suspend();
	    	   }
	       }
	       return 0;
      }
  }
   
   public void request(int n, int queue)  {
       SimProcess p = sim.currentProcess();
       UserRecord record = new UserRecord (n, p, sim.time());
       if (n <= available) {
           // The process gets the resource right away.
           available -= n;
           serviceList.addLast (record);
           if (stats) {
        	   if (queue == LOCAL)
              waitingList.statSojourn().add (0.0);
        	   else if (queue == REMOTE)
        		   remoteWaitingList.statSojourn().add(0.0);
              statUtil.update (capacity - available);
           }
       }
       else {
           // Not enough units of the resource are available.
           // The process joins the queue waitingList;
           switch (policy) {
               case FIFO : 
            	   if (queue == LOCAL) 
            	   {
            	   		waitingList.addLast (record); 
            	   }
               	else if (queue == REMOTE)
               		{
               			remoteWaitingList.addLast(record); 
               		}
               break;
               case LIFO : 
            	   if (queue == LOCAL) 
            	   {
            	   		waitingList.addFirst (record); 
            	   }
               	else if (queue == REMOTE)
               		{
               			remoteWaitingList.addFirst(record); 
               		}
//            	   waitingList.addFirst (record); break;
               default   : throw new IllegalStateException(
                                               "policy must be FIFO or LIFO");
           }
           p.suspend();
       }
  }
   
   public void requestNoahFirst(int n, int queue, int classifier, String id, double executionTime, double arrTime)  {
       SimProcess p = sim.currentProcess();
//       UserRecord record = new UserRecord (n, p, sim.time()); // Original record
       UserRecord record = new UserRecord (n, p, sim.time(), id, classifier, executionTime, arrTime);
       double realStartTime, dummyStartTime;// = sim.time();
       double time = 0;
	       if (n <= available) {
	    	   //check if it is dummy or real
	    	   if (classifier == REAL){
	    		   
	    		   // if save is null, nothing happens
	    		   if (save.contain(id)){ // checks if dummy val had been posted in save
	    			   for(int i=0; i < save.getSize(); i++){
	    				   if (id.equals(save.getID(i))){
	    					   if (save.getReal(i) == 0) // If that val is 0, update otherwise leave it
	    						   save.setReal(i, Sim.time() + executionTime -arrTime);
		    			   }
	    			   }
	    		   }
	    		   else{ // Real came first before dummy, give them same times
	    			   time = Sim.time() + executionTime - arrTime;
    				   save.update(id, time, time);
	    		   }
//	    		   save.update(id, 0, 0);
	    		   
	    		  
	           // The process gets the resource right away.
		           available -= n;
		           serviceList.addLast (record);
		           if (stats) {
		        	   if (queue == LOCAL)
		              waitingList.statSojourn().add (0.0);
		        	   else if (queue == REMOTE)
		        		   remoteWaitingList.statSojourn().add(0.0);
		              statUtil.update (capacity - available);
		           }
	    	   }
	    	    if (classifier == DUMMY){ // If it is dummy	    
//	    		   System.out.println("DUMMY Here " + id);
	    		   if(save.contain(record.id)){ // if id is in save, means real is there.
	    			   // do nothing
	    		   }else
	    		   {
	    			   save.update(record.id, 0, Sim.time() + executionTime - arrTime);
	    		   }	    		   	       		    			 
	    	   }
	       }
	       else {
	           // Not enough units of the resource are available.
	           // The process joins the queue waitingList;
	           switch (policy) {
	               case FIFO : 
	            	   if (queue == LOCAL) 
	            	   {
	            	   		waitingList.addLast (record); 
	            	   }
	               	else if (queue == REMOTE)
	               		{
	               			remoteWaitingList.addLast(record); 
	               		}
	               break;
	               case LIFO : 
	            	   if (queue == LOCAL) 
	            	   {
	            	   		waitingList.addFirst (record); 
	            	   }
	               	else if (queue == REMOTE)
	               		{
	               			remoteWaitingList.addFirst(record); 
	               		}
	//            	   waitingList.addFirst (record); break;
	               default   : throw new IllegalStateException(
	                                               "policy must be FIFO or LIFO");
	           }
	           p.suspend();
	       }
//       }
       
  }


   // Called by \texttt{release}. 
   private void startNewCustOriginalDefinition() {
       UserRecord record;
       ListIterator<UserRecord> iterWait = waitingList.listIterator();
       while (iterWait.hasNext() && available > 0) {
           record = iterWait.next();
           if (record.process.getState() == SimProcess.DEAD)  {iterWait.remove(); record.process.kill();}
              // the process was killed, so we remove it from the waiting list.
              // or maybe we stop the program by throwing IllegalStateException
              //"Resource.startNewCust: process not alive");

               // The thread for this process is still alive.
           else if (record.numUnits <= available) {
               // This request can now be satisfied.
               serviceList.addLast (record);
               record.process.resume();
               available -= record.numUnits;
               iterWait.remove();
           }
       }
    }
   
   public double getWaitingTime(){
	   
	   return waitTime;
   }
   
   
   private void startLocalCust() {
       UserRecord record;
       ListIterator<UserRecord> iterWait = waitingList.listIterator();
       
       
       while (iterWait.hasNext() && available > 0) {
           record = iterWait.next();
           if (record.classifier == DUMMY){
        	   
//        	   if (save.contain(record.id)){
//        		   for(int i=0; i < save.getSize(); i++){
//    				   if (record.id.equals(save.getID(i))){
//    					   if (save.getDummy(i) == 0) // If that val is 0, update otherwise leave it
//    						   save.setDummy(i, Sim.time() + record.execTime - record.arTime);
//	    			   }
//    			   }
//    		   }
//    		   else{
//    			   save.update(record.id, 0, Sim.time() + record.execTime - record.arTime);
//    		   }
        	   
	           for (UserRecord r : remoteWaitingList){ // Checks the remote queue for the same task, if there, keep simtime of dummy and break or continue for next task, 
	        	   if (record.id.equals(r.id)){
	        		   save.update(record.id, 0, Sim.time() + record.execTime - record.arTime); // Real thing here
//	        		   save.update(record.id, 0, 0); 
	        		   iterWait.remove();
	        	   }
	        	   // Otherwise do nothing, because remote could have come and gone
	           }

           }
           else {
	           if (record.process.getState() == SimProcess.DEAD) {iterWait.remove(); record.process.kill();} 
	              // the process was killed, so we remove it from the waiting list.
	              // or maybe we stop the program by throwing IllegalStateException
	              //"Resource.startNewCust: process not alive");
	
	               // The thread for this process is still alive.
	           else if (record.numUnits <= available) {
	               // This request can now be satisfied.
	               serviceList.addLast (record);
	               record.process.resume();
	               available -= record.numUnits;
	               iterWait.remove();
	           }
	       }
       }
    }
   
   private void startRemoteCust() {
       UserRecord record;
       ListIterator<UserRecord> iterWait = remoteWaitingList.listIterator();
       double time = 0;
       while (iterWait.hasNext() && available > 0) {
           record = iterWait.next();
//           if (save.contain(record.id)){
//    		   for(int i=0; i < save.getSize(); i++){
//				   if (record.id.equals(save.getID(i))){
//					   if (save.getReal(i) == 0) // If that val is 0, update otherwise leave it
//						   save.setReal(i, Sim.time() + record.execTime - record.arTime);
//    			   }
//			   }
//    	   }
//    	   else{ // Real came first before dummy, give them same times
//			   time = Sim.time() + record.execTime - record.arTime;
//			   save.update(record.id, time, 0);
//		   }
           
           for(UserRecord r : waitingList){
        	   if (record.id.equals(r.id)){ // Assuming the real job gets here first before the dummy , give them both the same start times
        		   time = Sim.time() + record.execTime - record.arTime;
//        		   save.update(record.id, Sim.time() + record.execTime - record.arTime, Sim.time() + record.execTime - record.arTime);
        		   save.update(record.id, time, time);
        		   
        	   }
        	   else{// If the record isnt there, it means the sim time had been uploaded before, then set the sim time for the record
        		   for (int i = 0; i < save.getSize(); i++){
        			   if (save.getID(i).equals(record.id)){
        				   if (save.getReal(i) == 0)
        					   save.setReal(i, Sim.time() + record.execTime - record.arTime);
        			   }
        		   }
        	   }
           }
           if (record.process.getState() == SimProcess.DEAD) {iterWait.remove(); record.process.kill();} 
              // the process was killed, so we remove it from the waiting list.
              // or maybe we stop the program by throwing IllegalStateException
              //"Resource.startNewCust: process not alive");

               // The thread for this process is still alive.
           else if (record.numUnits <= available) {
               // This request can now be satisfied.
               serviceList.addLast (record);
               record.process.resume();
               available -= record.numUnits;
               iterWait.remove();
           }
       }
    }
   
   
   private void startNewCust() {
	   int placeholder = (int) ( (1 - getAlpha())/getAlpha());
//	   int placeholder = (int) ( (1 - 0.05)/0.05);
	   
	   // Check if there are still items in both
	   if(waitingList.size() > 0 && remoteWaitingList.size() > 0)
	   {
		   if (count < placeholder || count % placeholder != 0){
			   startLocalCust();
			   count++;
//			   System.out.println("Im here starting locals");
		   }
		   else if (count != 0 && count % placeholder == 0){
			   startRemoteCust();
			   count++;
//			   System.out.println("Im here starting remote");
		   }
//		   else if (count % placeholder != 0){
//			   startLocalCust();
//			   count++;
//		   }
		   else{
			   startLocalCust(); count++; System.out.println("Im here starting locals I suppose alpha is 1");
		   }
	   }
	   //check if only remote queue has items 
	   else if (waitingList.size() == 0 && remoteWaitingList.size() > 0){
		   startRemoteCust(); count++;
	   }
	   // If only local queue has items
	   else if (waitingList.size() > 0 && remoteWaitingList.size() == 0){
		   startLocalCust(); count++;
	   }
	   
	   
       
    }

   /**
    * The executing process that invokes this method releases 
    *   <TT>n</TT> units of the resource.  If this process is holding exactly
    *   <TT>n</TT> units, it is removed from the service list of this resource
    *   and its {@link UserRecord} object disappears.
    *   If this process is holding less than <TT>n</TT> units, 
    *   the program throws an illegal argument exception.
    *   If there are other processes waiting for this resource whose requests
    *   can now be satisfied, they obtain the resource.
    * 
    * @param n number of released units
    * 
    *    @exception IllegalArgumentException if the process did not request <TT>n</TT>
    *       units before releasing them
    * 
    * 
    */
   public double release (int n)  {
	   
	   if (capacity == 0){
		   return 12000.0;
	   }
	   else {
	        SimProcess p = sim.currentProcess();
	        int temp = 0;
	        UserRecord record;
	        ListIterator<UserRecord> iterServ = serviceList.listIterator();
	        while (temp<n && iterServ.hasNext()) {
	            record = iterServ.next();
	            if (p == record.process) {
	                temp = temp + record.numUnits;
	                if (temp <= n) {
	                    iterServ.remove();
	                    if (waitTime > 0)
	                    	waitTime -= record.execTime;
	                    if (stats) statSojourn.add
	                                   (sim.time() - record.requestTime);
	                }
	                else {
	                    record.numUnits = temp - n;
	                    temp = n;
	                }
	            }
	            
	            
	        }
	        if (temp < n)  throw new IllegalArgumentException ("trying to release "
	                +"more units of a Resource than the process currently holds");
	        available += temp;
	        if (waitingList.size() > 0 )  startNewCustOriginalDefinition();//
	        if (stats) statUtil.update (capacity - available);
	        return 0;
	   }
    }

   public double release (int n, String s)  {
	   if (capacity == 0){
		   return 12000.0;
	   }
	   else {
	       SimProcess p = sim.currentProcess();
	       int temp = 0;
	       UserRecord record;
	       ListIterator<UserRecord> iterServ = serviceList.listIterator();
	     
	       while (temp<n && iterServ.hasNext()) {
	           record = iterServ.next();
	           if (p == record.process) {
	               temp = temp + record.numUnits;
	               if (temp <= n) {
	                   iterServ.remove();
	                   if (stats) statSojourn.add
	                                  (sim.time() - record.requestTime);
	               }
	               else {
	                   record.numUnits = temp - n;
	                   temp = n;
	               }
	           }
	       }
	       if (temp < n)  throw new IllegalArgumentException ("trying to release "
	               +"more units of a Resource than the process currently holds");
	       available += temp;
	       if (waitingList.size() > 0 || remoteWaitingList.size() > 0)  startNewCust();//startNewCustOriginalDefinition();//
	       if (stats) statUtil.update (capacity - available);   
	       return 0;
	   } 
     
   }
   

   public Util.SaveRealDummy releaseModified (int n)  {
       SimProcess p = sim.currentProcess();
       int temp = 0;
       UserRecord record;
       ListIterator<UserRecord> iterServ = serviceList.listIterator();
       Util.SaveRealDummy rd = new Util.SaveRealDummy();
       
       while (temp<n && iterServ.hasNext()) {
           record = iterServ.next();
           if (p == record.process) {
               temp = temp + record.numUnits;
               if (temp <= n) {
                   iterServ.remove();
                   if (stats) statSojourn.add
                                  (sim.time() - record.requestTime);
               }
               else {
                   record.numUnits = temp - n;
                   temp = n;
               }
           }
           for (int i = 0; i < save.getSize(); i++){
        	   if (record.id.equals(save.getID(i))){
        		   rd.update(record.id, save.getReal(i), save.getDummy(i));
        		   // if i want each fog to have the values, ill keep updating rd a
        		   save.clear(i);
        		   continue;
        	   }
           }
       }
       if (temp < n)  throw new IllegalArgumentException ("trying to release "
               +"more units of a Resource than the process currently holds");
       available += temp;
       if (waitingList.size() > 0 || remoteWaitingList.size() > 0)  startNewCust();//startNewCustOriginalDefinition();//
       if (stats) statUtil.update (capacity - available);       
       
       
       
       return rd;
   }
   
   
   /**
    * Returns the list that contains the
    *   {@link UserRecord} objects
    *   for the processes in the waiting list for this resource.
    * 
    * @return the list of process user records waiting for the resource
    * 
    */
   public LinkedListStat waitList()  { 
      return waitingList;
   }

   public LinkedListStat remoteWaitList(){
	   return remoteWaitingList;
   }

   /**
    * Returns the list that contains the 
    *   {@link UserRecord} objects
    *   for the processes in the service list for this resource.
    * 
    * @return the list of process user records using this resource
    * 
    */
   public LinkedListStat servList()  { 
      return serviceList;
   }


   /**
    * Returns the statistical collector that measures the evolution of
    *   the capacity of the resource as a function of time.
    *   This collector exists only if {@link #setStatCollecting setStatCollecting}&nbsp;<TT>(true)</TT> has been invoked
    *   previously.  
    * 
    * @return the probe for resource capacity
    * 
    */
   public Accumulate statOnCapacity()  {
      return statCapacity;
   }


   /**
    * Returns the statistical collector for the utilization
    *   of the resource (number of units busy) as a function of time.
    *   This collector exists only if {@link #setStatCollecting setStatCollecting}&nbsp;<TT>(true)</TT>
    *   has been invoked previously.  
    *   The <EM>utilization rate</EM> of a resource can be obtained as the
    *   <EM>time average</EM> computed by this collector, divided by the
    *   capacity of the resource.
    *   The collector returned by {@link #servList() servList()}<TT>.</TT>{@link umontreal.iro.lecuyer.simevents.LinkedListStat#statSize() statSize()} 
    *   tracks the number of {@link UserRecord}
    *   in the service list;
    *   it differs from this collector because a process may hold more than one
    *   unit of the resource by given {@link UserRecord}.
    * 
    * @return the probe for resource utilization
    * 
    */
   public Accumulate statOnUtil()  {
      return statUtil;
   }


   /**
    * Returns the statistical collector for the sojourn times of
    *   the {@link UserRecord} for this resource.
    *   This collector exists only if {@link #setStatCollecting setStatCollecting}&nbsp;<TT>(true)</TT> has been invoked
    *   previously.  
    *   It gets a new observation each time a process releases all the units
    *   of this resource that it had requested by a single {@link #request request}
    *   call.
    * 
    * @return the probe for the sojourn times
    * 
    */
   public Tally statOnSojourn()  {
      return statSojourn;
   }


   /**
    * Returns the name (or identifier) associated to this
    *   resource.  If it was not given upon resource construction, this returns
    *    <TT>null</TT>.
    * 
    * @return the name associated to this resource, or <TT>null</TT> if it was not given
    * 
    */
   public String getName() {
      return name;
   }
   
   public int getRemoteCount(){
	   if(!remoteWaitingList.isEmpty())
		   remoteCount = 1;
	   else 
		   remoteCount = 0;
	   return remoteCount;
   }

   /**
    * Returns a string containing a complete statistical report on this 
    *   resource.  The method {@link #setStatCollecting setStatCollecting}&nbsp;<TT>(true)</TT> must have been invoked 
    *   previously, otherwise no statistics have been collected.
    *   The report contains statistics on the waiting times, service
    *   times, and waiting times for this resource, on the capacity,
    *   number of units busy, and size of the queue as a function of time,
    *   and on the utilization rate.
    * 
    * @return a statistical report for this resource, represented as a string
    */
   public String report()  {

    if (statUtil == null) throw new IllegalStateException ("Asking a report for a resource "
                          +"for which setStatCollecting (true) has not been called");
  
    Accumulate sizeWait = waitingList.statSize();
    Tally sojWait = waitingList.statSojourn();
    Tally sojServ = serviceList.statSojourn();
    
    Accumulate remoteSizeWait = remoteWaitingList.statSize();
    Tally remoteSojWait = remoteWaitingList.statSojourn();
   
    PrintfFormat str = new PrintfFormat(); 

    str.append ("REPORT ON RESOURCE : ").append (name).append (PrintfFormat.NEWLINE);
    str.append ("   From time : ").append (7, 2, 2, initStatTime);
    str.append ("   to time : ");
    str.append (10,2, 2, sim.time());
    str.append (PrintfFormat.NEWLINE + "                    min        max     average  ");
    str.append ("standard dev.  nb. obs.");

    str.append (PrintfFormat.NEWLINE + "   Capacity    ");
    str.append (8, (int)(statCapacity.min()+0.5));
    str.append (11, (int)(statCapacity.max()+0.5));
    str.append (12, 3, 2, statCapacity.average());
 
    str.append (PrintfFormat.NEWLINE + "   Utilization ");
    str.append (8, (int)(statUtil.min()+0.5));
    str.append (11, (int)(statUtil.max()+0.5));
    str.append (12, 3, 2, statUtil.average());
 
    str.append (PrintfFormat.NEWLINE + "   Local Queue Size  "); 
    str.append (8, (int)(sizeWait.min()+0.5));
    str.append (11, (int)(sizeWait.max()+0.5));
    str.append (12, 3, 2, sizeWait.average());

    str.append (PrintfFormat.NEWLINE + "   Local Wait    ");
    str.append (12, 3, 2, sojWait.min()).append (' ');
    str.append (10, 3, 2, sojWait.max()).append (' ');
    str.append (11, 3, 2, sojWait.average()).append (' ');
    str.append (10, 3, 2, sojWait.standardDeviation());
    str.append (10, sojWait.numberObs());
    
    if(!remoteWaitingList.isEmpty())
    {
    	remoteCount = 1;
	    str.append (PrintfFormat.NEWLINE + "   Remote Queue Size  "); 
	    str.append (8, (int)(remoteSizeWait.min()+0.5));
	    str.append (11, (int)(remoteSizeWait.max()+0.5));
	    str.append (12, 3, 2, remoteSizeWait.average());
	    
	    str.append (PrintfFormat.NEWLINE + "   Remote Wait    ");
	    str.append (12, 3, 2, remoteSojWait.min()).append (' ');
	    str.append (10, 3, 2, remoteSojWait.max()).append (' ');
	    str.append (11, 3, 2, remoteSojWait.average()).append (' ');
	    str.append (10, 3, 2, remoteSojWait.standardDeviation());
	    str.append (10, remoteSojWait.numberObs());
    }
    str.append (PrintfFormat.NEWLINE + "   Service ");
    str.append (12, 3, 2, sojServ.min()).append (' ');  
    str.append (10, 3, 2, sojServ.max()).append (' ');
    str.append (11, 3, 2, sojServ.average()).append (' ');
    str.append (10, 3, 2, sojServ.standardDeviation());
    str.append (10, sojServ.numberObs());
    
    str.append (PrintfFormat.NEWLINE + "   Sojourn ");
    str.append (12, 3, 2, statSojourn.min()).append (' ');
    str.append (10, 3, 2, statSojourn.max()).append (' ');
    str.append (11, 3, 2, statSojourn.average()).append (' ');
    str.append (10, 3, 2, statSojourn.standardDeviation());
    str.append (10, statSojourn.numberObs()).append (PrintfFormat.NEWLINE);
    
    return str.toString();
   }
}
