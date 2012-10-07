package de.banbury.remoteserver;

// UDPServer.java
//
// Author: Magnus Uppman - magnus.uppman@gmail.com
// (c) 2010 Linuxfunkar - www.linuxfunkar.se
// License: GPL

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

public abstract class UDPServer extends Thread {

	private int bufferSize; // in bytes
	protected DatagramSocket ds;
	private Logger log;

	public UDPServer(int port, int bufferSize) throws SocketException,
			UnknownHostException {
		log = Logger.getLogger(this.getClass());
		InetAddress localHost = InetAddress.getLocalHost();
		log.info("Server: " + localHost.getHostName() + "("
				+ localHost.getHostAddress() + "): " + port);
		this.bufferSize = bufferSize;
		this.ds = new DatagramSocket(port);
	}

	public UDPServer(int port) throws SocketException, UnknownHostException {
		this(port, 8192);
	}

	public void run() {
		byte[] buffer = new byte[bufferSize];
		while (true) {
			DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
			try {
				ds.receive(incoming);
				this.respond(incoming);
			} catch (IOException e) {
				log.error("Exception", e);

			}
			yield();
		} // end while
	} // end run

	public abstract void respond(DatagramPacket request);
}
