
public class Packet {

	short cksum;		//16bit 2-byte
	short len;			//16-bit 2-byte
	int ackno;			//32-bit 4-byte
	int seqno;			//32-bit 4-byte Data packet Only
	byte data[];		//0-500 bytes. Data packet only. Variable
}
