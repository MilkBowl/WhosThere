package com.sleaker.MemManager;

public class MemCheck implements Runnable {

	public void run() {
		double memUsed = Runtime.getRuntime().totalMemory() / 1048576;
		double memReserved = Runtime.getRuntime().freeMemory() / 1048576;
		double memHeap = Runtime.getRuntime().maxMemory() / 1048576;
		double memAct = memUsed - memReserved;
		System.out.println(MemManager.plugName + " - Total System Mem Usage: " + memUsed + " mb");
		System.out.println(MemManager.plugName + " - Maximum System Usage: " + memHeap + " mb");
		System.out.println(MemManager.plugName + " - Total Reserved Memory (Unused): " + memReserved + " mb");
		System.out.println(MemManager.plugName + " - Actual Used Memory: " + memAct + " mb");
		
	}

}