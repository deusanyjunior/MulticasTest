package br.usp.ime.compmus.multicastest;

import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.NetworkInterface;
import java.io.IOException;

public class UDPMulticast{

	private String ip;
	private int port;
	private NetworkInterface netInterface;

	private InetAddress group;
	private MulticastSocket s;

	public UDPMulticast(String ipMulticast, int portMulticast, NetworkInterface netInterfaceMulticast) throws IOException {
		this.ip = ipMulticast;
		this.port = portMulticast;
		this.netInterface = netInterfaceMulticast;

		this.group = InetAddress.getByName(ip);
		this.s = new MulticastSocket(port);
		this.s.joinGroup(new InetSocketAddress(this.group, this.port), netInterface);

	}

	void send(String msg) throws IOException {

		DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(),
				this.group, this.port);
		this.s.send(hi);
	}

	void receive(byte[] buf) throws IOException{
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
		this.s.receive(recv);
	}

}

