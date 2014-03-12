package ch.vd.uniregctb.declaration;

public class DeclarationException extends Exception {

	private static final long serialVersionUID = 2738302136542915345L;

	public DeclarationException(Throwable e) {
		super(e);
	}

	public DeclarationException(String string) {
		super(string);
	}

	public DeclarationException(String string, Throwable e) {
		super(string, e);
	}
}
