/**
 * Copyright 2011 Christian Decker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is part the BitDroidNetwork Project.
 */

package net.bitdroid.network;

import java.io.IOException;
import java.net.InetAddress;

import net.bitdroid.network.wire.LittleEndianInputStream;
import net.bitdroid.network.wire.LittleEndianOutputStream;

public class PeerAddress extends Message {
	private long services;
	private byte reserved[];
	byte[] getReserved() {
		return reserved;
	}

	void setReserved(byte[] reserved) {
		this.reserved = reserved;
	}

	private InetAddress address;
	private int port;
	
	PeerAddress(LittleEndianInputStream in) throws IOException{
		super(in);
	}
	
	public PeerAddress(BitcoinClientSocket clientSocket) {
		super(clientSocket);
	}

	/**
	 * @param services the services to set
	 */
	public void setServices(long services) {
		this.services = services;
	}

	/**
	 * @return the services
	 */
	public long getServices() {
		return services;
	}

	/**
	 * @param address the address to set
	 */
	public void setAddress(InetAddress address) {
		this.address = address;
	}

	/**
	 * @return the address
	 */
	public InetAddress getAddress() {
		return address;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	@Override
	void read(LittleEndianInputStream in) throws IOException {
		setServices(in.readLong());
		reserved = new byte[12];
		in.read(reserved);
		byte[] b = new byte[4];
		in.read(b);
		setAddress(InetAddress.getByAddress(b));
		// Port uses network byte order, goddamn mix of ordering...
		b = new byte[2];
		in.read(b);
		setPort((b[0] & 0xFF) << 8 | (b[1] & 0xFF));
		//setPort(in.readUnsignedShort());
	}

	@Override
	void toWire(LittleEndianOutputStream leos) throws IOException {
		leos.writeLong(services);
		leos.write(reserved);
		leos.write(address.getAddress());
		// Again: this is big-endian...
		leos.write(new byte[]{(byte)(port >> 8 & 0xFF), (byte)(port & 0xFF)});
	}

	@Override
	public String getCommand() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(address.getHostAddress());
		sb.append(":").append(port).append("[Services=").append(services).append("]");
		return sb.toString();
	}

}
