package ch.vd.warsync;

public class WatchedDir {

	public String source;
	public String destination;
	public int watchId = -1;
	public boolean recursive;

	public WatchedDir(String s, String d, boolean r) {

		source = s;
		destination = d;
		recursive = r;
	}

}
