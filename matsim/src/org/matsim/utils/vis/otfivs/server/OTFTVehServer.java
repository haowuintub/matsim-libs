/* *********************************************************************** *
 * project: org.matsim.*
 * OTFTVehServer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.utils.vis.otfivs.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plan;
import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.vis.otfivs.data.OTFDefaultNetWriterFactoryImpl;
import org.matsim.utils.vis.otfivs.data.OTFNetWriterFactory;
import org.matsim.utils.vis.otfivs.data.OTFServerQuad;
import org.matsim.utils.vis.otfivs.handler.OTFAgentsListHandler;
import org.matsim.utils.vis.otfivs.interfaces.OTFServerRemote;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;
import org.matsim.world.World;


public class OTFTVehServer implements OTFServerRemote{
	//private final   String netFileName = "";
	private  String vehFileName = "";
	//private final  String outFileName = "";
	private static final int BUFFERSIZE = 100000000;
	BufferedReader reader = null;
	private double nextTime = -1;
	private List<Double> times = null;
	TreeMap<Integer, byte[]> timesteps = new TreeMap<Integer, byte[]>();
	//private byte[] actBuffer = null;



	private final OTFAgentsListHandler.Writer writer = new OTFAgentsListHandler.Writer();
	//private final ByteArrayOutputStream out;
	//private QueueNetworkLayer net;
	public OTFServerQuad quad;

	public OTFTVehServer(String netFileName, String vehFileName) {
		this.vehFileName = vehFileName;

		if (Gbl.getConfig() == null) Gbl.createConfig(null);

		Gbl.startMeasurement();
		World world = Gbl.createWorld();

		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);
		QueueNetworkLayer qnet = new QueueNetworkLayer(net);

		//this.out = new ByteArrayOutputStream(20000000);
		this.quad = new OTFServerQuad(qnet);
		this.quad.fillQuadTree(new OTFDefaultNetWriterFactoryImpl());
		this.quad.addAdditionalElement(this.writer);
		
//		this.times = buildTimesList(); // Does not work very smoothly, therefore we leave it out until there is demand for this
		this.times =null;
		
		open();
		readOneStep();
	}

	private List<Double>  buildTimesList() {
		Gbl.startMeasurement();
		System.out.println("Scanning timesteps:");

		// Get time Structure
		List<Double> times = new ArrayList<Double>(); 
		open();
		String line = null;
		boolean lineFound = false;
		String lasttime = "-1";

		try {
			line = this.reader.readLine();
			while ( !lineFound && (line != null)) {
				line = line.substring(line.indexOf('\t')+1);
				String tt = line.substring(0, line.indexOf('\t'));
				if(!tt.equals(lasttime)) {
					double time = Double.parseDouble(tt);
					times.add(time);
					lasttime = tt;
					System.out.print(tt);
					System.out.print(", ");
				}
				line = this.reader.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("");
		System.out.println("Nr of timesteps: " + times.size());
		try {
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Gbl.printElapsedTime();
		return times;
	}

	ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);
	//private final int cntPositions=0;
	//private final double lastTime=-1;
	//private final int cntTimesteps=0;
	private OTFAgentsListHandler.ExtendedPositionInfo readVehicle = null;
	private double time;

	public boolean readOneLine(){
		String line = null;
		boolean lineFound = false;

		try {
			line = this.reader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		while ( !lineFound && (line != null)) {
			String[] result = line.split("\t");
			if (result.length == 16) {
				double easting = Double.parseDouble(result[11]);
				double northing = Double.parseDouble(result[12]);
				if ((easting >= this.quad.getMinEasting()) && (easting <= this.quad.getMaxEasting()) && (northing >= this.quad.getMinNorthing()) && (northing <= this.quad.getMaxNorthing())) {
					String agent = result[0];
					String time = result[1];
					String speed = result[6];
					String elevation = result[13];
					String azimuth = result[14];

					lineFound = true;
					this.time = Double.parseDouble(time);
					this.readVehicle = new OTFAgentsListHandler.ExtendedPositionInfo(new IdImpl(agent), easting, northing,
							Double.parseDouble(elevation), Double.parseDouble(azimuth), Double.parseDouble(speed), PositionInfo.VehicleState.Driving, Integer.parseInt(result[7]), Integer.parseInt(result[15]));
					return true;
				}
			}
			try {
				line = this.reader.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return false;
	}

	private boolean finishedReading = false;

	private int newTime;
	synchronized private void preCacheTime() {
		while ((this.time <= this.newTime) && !this.finishedReading)readOneStep();
	}

	synchronized public void readOneStep() {
		if ( this.finishedReading) return;

		double actTime = this.time;

		if (this.readVehicle == null){
			readOneLine();
			this.writer.positions.add(this.readVehicle);
			actTime = this.time;
		} else {
			this.writer.positions.clear();
			this.writer.positions.add(this.readVehicle);
		}

		// collect all vehicles
		while (readOneLine() && (this.time == actTime)) this.writer.positions.add(this.readVehicle);

		// check if file is read to end
		if(this.time == actTime)this.finishedReading = true;

		synchronized (this.buf) {
			// now write this into stream
			this.buf.position(0);
			this.quad.writeDynData(null, this.buf);

			byte [] buffer = new byte[this.buf.position()+1];
			System.arraycopy(this.buf.array(), 0, buffer, 0, buffer.length);
			this.nextTime = actTime;
			this.timesteps.put((int)this.nextTime, buffer);
			//this.actBuffer = buffer;
			//System.out.println("Read timestep: " + actTime);
		}

	}

	public void step() throws RemoteException {
		requestNewTime((int)(this.nextTime+1), TimePreference.LATER);
	}

	public void open() {
		Gbl.startMeasurement();
		try {
			this.reader = IOUtils.getBufferedReader(this.vehFileName);
//			reader = new BufferedReader(new FileReader(this.vehFileName),500000);
//			reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new BufferedInputStream(new FileInputStream(this.vehFileName),50000000))));
			this.reader.readLine(); // Read the commentary line
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	private void finish() {
	try {
		this.reader.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
	Gbl.printElapsedTime();
	}


	public Plan getAgentPlan(String id) throws RemoteException {
		return null;
	}

	public int getLocalTime() throws RemoteException {
		return (int)this.nextTime;
	}


	public OTFServerQuad getQuad(String id, OTFNetWriterFactory writers)
			throws RemoteException {
		return this.quad;
	}

	public byte[] getQuadConstStateBuffer(String id) throws RemoteException {
		this.buf.position(0);
		this.quad.writeConstData(this.buf);
		byte [] result;
		synchronized (this.buf) {
			result = this.buf.array();
		}
		return result;
	}

	public byte[] getQuadDynStateBuffer(String id, Rect bounds)
			throws RemoteException {
		if (this.nextTime == -1) {
			throw new RemoteException("nextTime == -1 in OTFTVehServer");
		}
		return this.timesteps.get((int)this.nextTime);
	}


	public byte[] getStateBuffer() throws RemoteException {
		throw new RemoteException("getStateBuffer not implemented for OTFTVehServer");
	}

	public boolean isLive() throws RemoteException {
		return false;
	}

	public void pause() throws RemoteException {
	}

	public void play() throws RemoteException {
	}

	public void setStatus(int status) throws RemoteException {
	}


	public static void main(String[] args) {

		String netFileName = "../studies/schweiz/2network/ch.xml";
		String vehFileName = "../runs/run168/run168.it210.T.veh";
//		String netFileName = "../../tmp/studies/ivtch/network.xml";
//		String vehFileName = "../../tmp/studies/ivtch/T.veh";

		OTFTVehServer test  = new OTFTVehServer(netFileName, vehFileName);
		try {
			double time = test.getLocalTime();
			test.step();
			while (time != test.getLocalTime()) {
				time = test.getLocalTime();
				test.step();
			}
			test.finish();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean requestNewTime(int time, TimePreference searchDirection) throws RemoteException {
		int lastTime = -1;
		int foundTime = -1;
		this.newTime = time;

		if ((this.timesteps.lastKey() < time) && (searchDirection == TimePreference.LATER)) {
			if(this.finishedReading ) return false;
			else this.newTime = (int)this.time;
		}

		preCacheTime();

		// C else search in buffered timesteps
		for(Integer timestep : this.timesteps.keySet()) {
			if (searchDirection == TimePreference.EARLIER){
				if(timestep >= this.newTime) {
					// take next lesser time than requested, if not exacty the same
					foundTime = lastTime;
					break;
				}
			} else {
				if(timestep >= this.newTime) {
					foundTime = timestep; //the exact time or one biggers
					break;
				}
			}
			lastTime = timestep;
		}
		if (foundTime == -1) return false;

		this.nextTime = foundTime;
		//this.actBuffer = null;
		return true;
	}

	public Collection<Double> getTimeSteps() throws RemoteException {
		return times;
	}

}


