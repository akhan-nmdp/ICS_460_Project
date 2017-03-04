package main;

import java.nio.ByteBuffer;

public class Packet {
    
    short cksum; //16-bit 2-byte
    short length;    //16-bit 2-byte
    int ackno;    //32-bit 4-byte
    int seqno ;   //32-bit 4-byte Data packet Only
    byte[] data= new byte[500]; //0-500 bytes. Data packet only. Variable

    public Packet(short cksum, short len, int ackno)
	{
		this.cksum = cksum;
		this.length = len;
		this.ackno = ackno;

		ByteBuffer b = ByteBuffer.allocate(8);
		b.putShort(0, cksum);
		b.putShort(2, len);
		b.putInt(4, ackno);
		byte seq[] = b.array();

		this.data = seq;
	}

	public Packet(short cksum, short len, int ackno, int seqno, byte[] data)
	{
		this.cksum = cksum;
		this.length = len;
		this.ackno = ackno;
		this.seqno = seqno;


		ByteBuffer b = ByteBuffer.allocate(12);
		b.putShort(0, cksum);
		b.putShort(2, len);
		b.putInt(4, ackno);
		b.putInt(8, seqno);
		byte seq[] = b.array();
		byte[] combined = new byte[seq.length + data.length];

		System.arraycopy(seq, 0, combined, 0, seq.length);
		System.arraycopy(data, 0, combined, seq.length, data.length);

		this.data = combined;
	}

	public Packet( int seqno)
	{
		this.seqno = seqno;
	}

	public byte[] getData(){
		return data;
	}

	public int getAckno(){
		return ackno;
	}

	public int getSeqno(){
		return seqno;
	}
	public short getLength() {
		return length;
	}
	public String toString() {
		String dataString = "";
		for(int i = 0; i < length; i++) {
			dataString += (char) data[i];
		}
		return "Data: " + dataString + ". Length: " + length + ".";
	}

	public void setCksum(short value) {
		this.cksum = value;
		if(value == 1) {
			data[1] = 1;
		}
		else {
			data[1] = 0;
		}
	}

	public short getCksum() {
		return cksum;
	}
}
