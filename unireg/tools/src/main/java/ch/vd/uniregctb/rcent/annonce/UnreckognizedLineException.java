package ch.vd.uniregctb.rcent.annonce;

public class UnreckognizedLineException extends Exception {

	private String line;

	public UnreckognizedLineException(String line) {
		this.line = line;
	}

	public String getLine() {
		return line;
	}
}
