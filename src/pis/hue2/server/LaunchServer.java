package pis.hue2.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import pis.hue2.common.Connection;
import pis.hue2.common.PacketHandler;
import pis.hue2.common.PacketManager;
import pis.hue2.common.PacketType;

/**
 * stellt einen Server zu Verfügung
 * 
 * @author Johannes Mahn, Yannick Dreher
 *
 */
public class LaunchServer {

	public static LaunchServer instance;

	/**
	 * Erzeugt einen neuen Server
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		instance = new LaunchServer();
		try {
			instance.startServer();
		} catch (IOException ex) {
			System.err.println("Error trying to bind port!");
			ex.printStackTrace();
		}
		System.out.println("Server started!");
		while (instance.isRunning()) {
		}
		System.out.println("Server closed!");
	}

	private ServerSocket socket;
	public TeilnehmerListe teilnehmer = new TeilnehmerListe();
	public PacketManager packetManager = new PacketManager();

	/**
	 * Startet den Server in einem neuen Thread und wartet auf Verbingungseingänge
	 * 
	 * @throws IOException
	 */
	public void startServer() throws IOException {
		registerHandler();
		socket = new ServerSocket(25565);
		new Thread(() -> {
			System.out.println("Waiting for connections");
			while (isRunning()) {
				try {
					Socket client = socket.accept();
					System.out.println("New client connected");
					teilnehmer.newConnection(client);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * Bearbeitet die einkommenden Nachrichten nach Typ
	 */
	private void registerHandler() {
		packetManager.registerPacketHandler(PacketType.connect, new PacketHandler() {

			@Override
			public boolean handlePacket(Connection con, String packet) {
				ClientConnection ccon = (ClientConnection) con;
				if (packet.isEmpty() || packet.contains(":") || packet.length() > 30) {
					ccon.sendPacket(PacketType.refused, "invalid_name");
					return false;
				}
				boolean answer = ccon.setName(packet);
				if (!answer) {
					return false;
				}
				ccon.sendPacket(PacketType.connect, "ok");
				teilnehmer.broadcastUserList();
				System.out.println("Client renamed to '" + packet + "'");
				return true;
			}
		});
		packetManager.registerPacketHandler(PacketType.message, new PacketHandler() {

			@Override
			public boolean handlePacket(Connection con, String packet) {
				teilnehmer.broadcast(PacketType.message, ((ClientConnection) con).getName() + ":" + packet);
				return true;
			}
		});
		packetManager.registerPacketHandler(PacketType.disconnect, new PacketHandler() {

			@Override
			public boolean handlePacket(Connection con, String packet) {
				con.sendPacket(PacketType.disconnect, "ok");
				return false;
			}
		});
	}

	/**
	 * testet ob der Server lauft, also ob der Socket noch angebunden ist und noch
	 * nicht geschlossen wurde
	 * 
	 * @return boolean
	 */
	public boolean isRunning() {
		return socket.isBound() && !socket.isClosed();
	}

}
