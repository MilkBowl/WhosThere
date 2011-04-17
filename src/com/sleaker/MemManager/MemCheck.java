package com.sleaker.MemManager;

public class MemCheck implements Runnable {

	public void run() {
		double memUsed = Runtime.getRuntime().totalMemory() / 1048576;
		double memReserved = Runtime.getRuntime().freeMemory() / 1048576;
		System.out.println(MemManager.plugName + " - Memory Used: " + memUsed + " mb");
		System.out.println(MemManager.plugName + " - Memory Reserved: " + memReserved + " mb");
		
	}

}
