package com.vladi.gae1.gsod;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Parallelizer {
	public static interface P {
		void process(List data, int idx) throws Exception;
	}

	class Worker extends Thread {
		private P func;
		private List data;
		private int idx;
		public Worker(P func) {
			this.func = func;
			
		}
		public void startWork(List data, int idx) {
			this.data = data;
			this.idx  = idx;
			synchronized (this) {
				this.notify();
			}
		}
		public void run() {
			try {
				for (;;) {
					synchronized (this)  {
						workerComplete(this);
						wait();
					}
					try {
						func.process(data, idx);
					} catch (Exception e) {
						onException(e);
					}
				}
			
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	List data;
	P func;
	private Class workerClass;
	private int numWorkers;
	public Parallelizer(List data, Class workerClass, int workers) {
		this.data=data;
		this.workerClass=workerClass;
		this.numWorkers = workers;
		
	}
	Exception processingException;
	public void onException(Exception e) {
		processingException = e;
	}
	
	Stack<Worker> workers = new Stack<Parallelizer.Worker>();
	public void workerComplete(Worker w) {
		synchronized (workers) {
			workers.push(w);
			workers.notify();
		}
	}
	
	public void execute() throws Exception {
		for (int i=0; i<numWorkers; i++) {
			Worker w;
			w = new Worker(((P)workerClass.newInstance()));
			w.start();
//			workers.push(w);
		}
	//	Thread.sleep(500);
		for (int i=0; i < data.size(); i++) {
			System.out.println("" + i +  "/" + data.size());
			synchronized (workers) {
				
				if (workers.isEmpty()) workers.wait();
			}
			System.out.println("aa " + i +  "/" + data.size());
			if (processingException != null) throw processingException;
			workers.pop().startWork(data, i);
			if (i%100 ==0) System.out.println((int)((float)i/data.size() * 100) + "% completed");
		}
		System.out.println("Workers waiting to complete");

		while (workers.size() < numWorkers) Thread.sleep(100);
		System.out.println("Paralell execution completed");
		
		
	}
	public static Parallelizer start(List data, Class workerClass, int workers) throws Exception {
		Parallelizer par = new Parallelizer(data, workerClass, workers);
		par.execute();
		return par;
	}
	
	public static void main(String[] args) throws Exception {
		ArrayList<Integer> arr = new ArrayList<Integer>();
		for (int i=0; i< 10000; i++) {
			arr.add(i);
		}
		
		class MyWorker implements P {
			@Override
			public void process(List data, int idx) {
				System.out.println("processing: " + data.get(idx));
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		Parallelizer.start(arr, MyWorker.class, 10);
	}
}
