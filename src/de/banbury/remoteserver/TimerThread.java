package de.banbury.remoteserver;

import org.apache.log4j.Logger;

// TimerThread.java
//
// Author: Magnus Uppman - magnus.uppman@gmail.com
// (c) 2010 Linuxfunkar - www.linuxfunkar.se
// License: GPL

public class TimerThread extends Thread {
	int ms, id;
	public boolean running = true, active = false;
	MouseKeysRemoteServer parent;
	private Logger log;

	public TimerThread(int ms, int id, MouseKeysRemoteServer parent) {
		log = Logger.getLogger(getClass());
		this.ms = ms;
		this.id = id;
		this.parent = parent;
	}

	public void run() {
		log.trace("Thread run: " + id);
		wait_awhile();
	}

	void wait_awhile() {
		while (running) {
			try {
				if (active) {
					Thread.sleep(ms);
					active = parent.callback(id);
				} else
					Thread.sleep(10);
			} catch (Exception e) {
				log.error("Exception", e);
			}

		}
	}
}
