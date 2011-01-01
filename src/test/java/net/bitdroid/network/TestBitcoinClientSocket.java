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
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Arrays;

import junit.framework.TestCase;

import net.bitdroid.network.BitcoinClientSocket.ClientState;
import net.bitdroid.network.wire.LittleEndianInputStream;
import net.bitdroid.network.wire.LittleEndianOutputStream;

import org.junit.Test;

public class TestBitcoinClientSocket extends TestCase {

	protected BitcoinClientSocket prepareWithDump(String filename){
		BitcoinClientSocket s = new BitcoinClientSocket();
		s.inputStream = ClassLoader.getSystemResourceAsStream(filename);
		return s;
	}
	
	protected byte[] readDump(String filename, int length) throws IOException{
		byte[] buffer = new byte[length];
		ClassLoader.getSystemResourceAsStream(filename).read(buffer);
		return buffer;
	}
	
	@Test
	public void testReadDump() throws IOException{
		InputStream is = ClassLoader.getSystemResourceAsStream("bitcoin-version-0.dump");
		is.read();
	}
	
	@Test
	public void testReadVerackMessage() throws IOException {
		BitcoinClientSocket s = prepareWithDump("bitcoin-verack-1.dump");
		// There's not much to test, it's an empty message.
		Message m = s.readMessage();
		assert(m instanceof VerackMessage);
		assertEquals(m.getSize(), 0);
		assertTrue("Checksum is set on the socket", s.currentState == ClientState.OPEN);
	}
	
	@Test
	public void testReadVersionMessage() throws IOException {
		BitcoinClientSocket s = prepareWithDump("bitcoin-version-1.dump");
		VersionMessage m = (VersionMessage)s.readMessage();
		assert(m instanceof VersionMessage);
		assertEquals(m.getSize(), 85);
		assertEquals(31700, m.getProtocolVersion());
		assertEquals(1292970988, m.getTimestamp());
		assertEquals("Checksum is set not yet enabled on the socket", ClientState.HANDSHAKE, s.currentState);
		assertEquals("/87.118.94.169", m.getYourAddress().getAddress().toString());
		assertEquals("/213.200.193.129", m.getMyAddress().getAddress().toString());
		assertEquals("", m.getClientVersion());
		assertEquals(98806, m.getHeight());
	}
	
	@Test
	public void testFullVersionMessageCycle() throws IOException {
		// Read the original
		BitcoinClientSocket s = prepareWithDump("bitcoin-version-1.dump");
		VersionMessage m = (VersionMessage)s.readMessage();
		byte[] buf = readDump("bitcoin-version-1.dump", 105);
		byte[] output = new byte[105];
		s.outputStream = LittleEndianOutputStream.wrap(output);
		s.sendMessage(m);
		assert(Arrays.equals(buf, output));
	}
	
	@Test
	public void testReadAddress() throws IOException {
		LittleEndianInputStream leis = new LittleEndianInputStream(ClassLoader.getSystemResourceAsStream("address.dump"));
		PeerAddress a = new PeerAddress((BitcoinClientSocket)null);
		a.read(leis);
		assertEquals("/213.200.193.129", a.getAddress().toString());
		assertEquals(1, a.getServices());
		assertEquals(36747, a.getPort());
	}
	
	@Test
	public void testWriteAddress() throws IOException {
		PeerAddress a = new PeerAddress((BitcoinClientSocket)null);
		a.setAddress(InetAddress.getByName("213.200.193.129"));
		a.setServices(1);
		a.setPort(36747);
		a.setReserved(new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
				(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
				(byte)0x00,(byte)0xFF,(byte)0xFF});
		byte b[] = new byte[26];
		LittleEndianOutputStream leos = LittleEndianOutputStream.wrap(b);
		a.toWire(leos);
		byte c[] = new byte[26];
		ClassLoader.getSystemResourceAsStream("address.dump").read(c);
		for(byte bt : b)
			System.out.print((int)bt);
		System.out.println();
		for(byte ct : c)
			System.out.print((int)ct);
		System.out.println();
		assertTrue(Arrays.equals(c, b));
	}
	
	public void testReadInvMessage() throws IOException {
		BitcoinClientSocket s = prepareWithDump("bitcoin-inv-2.dump");
		s.currentState = ClientState.OPEN;
		InventoryMessage m = (InventoryMessage)s.readMessage();
		assertEquals(8, m.getItems().size());
	}
}
