package edu.gatech.protocol;

//import android.util.Log;
import edu.gatech.protocol.Log;
import edu.gatech.util.Utility;



// Here, we assume MetaData ID is consistent with TaskID, but this constraintly is not explicitly enforced.
public class MetaData {
	// public static final int METADATA_LENGTH = 30;
	public static final int METADATA_LENGTH = 13;

	private static final String TAG = "MetaData";
	// String cID, jID;
	// char ex;
	// int fileLen;

	String ID;
	OffloadingMode offMode;
	int contentLength;

	public MetaData(String metadata) {
		// this.cID = metadata.substring(0, 17);
		// this.jID = metadata.substring(17, 19);
		// this.ex = metadata.charAt(19); //metadata.substring(19, 20);
		// this.fileLen = Integer.parseInt(metadata.substring(20,
		// METADATA_LENGTH));

		this.ID = metadata.substring(0, 2);
		this.offMode = Utility.charToOffloadingMode(metadata.charAt(2));
		this.contentLength = Integer.parseInt(metadata.substring(3,
				METADATA_LENGTH));
	}

	public MetaData(String _ID, OffloadingMode _offMode, int _fileLen) {
		// this.cID= _cID;
		// this.jID = _jID;
		// this.ex = _ex;
		// this.fileLen = _fileLen;

		if (_ID.length() != 2)
			Log.d(TAG,
					"job id format is wrong, exactly two digits are required");

		this.ID = _ID;
		this.offMode = _offMode;
		this.contentLength = _fileLen;
	}

	public OffloadingMode getOffloadingMode() {
		return offMode;
		// return Utility.charToOffloadingMode( ex );
	}

	public String getLogString() {
		// return "cId=" + cID + ", jId=" + jID + ",ex=" + ex + ", bLen="+
		// fileLen + ",END";

		return "ID=" + ID + ", offMode=" + offMode + "("
				+ Utility.offloadingModeToChar(offMode) + "), content length="
				+ contentLength;
	}

	public String toString() {
		// return cID + jID + ex + fileLen;
		return ID + offMode + contentLength;
	}

	public byte[] getBytes() {
		// String bLen = String.format("%010d", fileLen);
		// String res = cID + jID + ex + bLen;

		String res = ID + Utility.offloadingModeToChar(offMode)
				+ String.format("%010d", contentLength);
		
		if (res.length() != METADATA_LENGTH)
			Log.d(TAG, "Error: MetaData getBytes() is not exactly "
					+ METADATA_LENGTH + " bytes");
		return res.getBytes();
	}

	// public String getcID(){ return cID; }
	// public String getjID(){ return jID; }
	// public char getEX(){ return ex; }
	// public int getfileLen(){ return fileLen; }
	// public void setfileLen(int len){
	// this.fileLen = len;
	// }

	public void setContentLength(int k){
		contentLength = k;
	}
	public int getContentLength(){
		return contentLength;
	}
	
	public String getID(){
		return ID;
	}
}
