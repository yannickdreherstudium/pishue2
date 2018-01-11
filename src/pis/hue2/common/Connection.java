package pis.hue2.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public abstract class Connection {

	private Socket socket;
	private Thread clientThread;

	public Connection(Socket socket, PacketManager handler) {
		this.socket = socket;
		clientThread = new Thread(() -> {
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				while (isConnected()) {
					String input = null;
					try{
						input = in.readLine();
					}catch(Exception ex){
						socket.close();
						break;
					}
					if (input != null) {
						input = input.replace("���� ����'������", "");
						if (!input.contains(":")) {
							sendPacket(PacketType.disconnect, "invalid_command");
							socket.close();
							break;
						}
						String command = input.substring(0, input.indexOf(":"));
						PacketType type = null;
						try {
							type = PacketType.valueOf(command);
						} catch (Exception ex) {
						}
						if (type == null) {
							sendPacket(PacketType.disconnect, "invalid_command");
							socket.close();
							break;
						}
						if (getConnectionState() == ConnectionState.Login
								&& !(type == PacketType.connect || type == PacketType.disconnect || type == PacketType.refused)) {
							socket.close();
							break;
						}
						if (!handler.handlePacket(this, type,
								input.substring(input.indexOf(":") + 1, input.length()))) {
							socket.close();
							break;
						}
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			onDisconnect();
		});
		clientThread.start();

	}
	
	public abstract void onDisconnect();

	public abstract ConnectionState getConnectionState();

	public boolean isConnected() {
		return socket.isBound() && !socket.isClosed() && socket.isConnected();
	}

	public void sendPacket(PacketType type, String content) {
		try {
			socket.getOutputStream().write((type.name() + ":" + content + "\n").getBytes());
		} catch (Exception e) {

		}
	}

}
