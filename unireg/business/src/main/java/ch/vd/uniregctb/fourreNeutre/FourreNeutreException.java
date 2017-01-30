package ch.vd.uniregctb.fourreNeutre;

public class FourreNeutreException extends Exception {

	private static final long serialVersionUID = 2738302136731915345L;

	public FourreNeutreException(Throwable e) {
		super(e);
	}

	public FourreNeutreException(String string) {
		super(string);
	}

	public FourreNeutreException(String string, Throwable e) {
		super(string, e);
	}
}
