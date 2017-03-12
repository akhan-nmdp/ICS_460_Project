package main;

import java.net.*;

public class ReceiverThread extends Thread {
	private DatagramSocket socket;
	// private Writer writer;
	private volatile boolean stopped = false;

	ReceiverThread(DatagramSocket socket) {
		this.socket = socket;
		// writer = new Writer();
	}

	public void halt() {
		this.stopped = true;
	}

	@Override
	public void run() {
		byte[] buffer = new byte[1024];
		while (true) {
			if (stopped)
				return;
			DatagramPacket dp;
			try {
				dp = new DatagramPacket(buffer, buffer.length);
				socket.receive(dp);
				String t = new String(dp.getData(), 0, dp.getLength(), "UTF-8");
				System.out.println("IRT:" + t);
				Thread.yield();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}
}
