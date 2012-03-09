package ch.vd.uniregctb.interfaces.service;

/**
 * Exception renvoyée par le service civil dans le d'une erreur de réseau ou d'un problème de droit d'accès.
 */
public class PersonneMoraleException extends RuntimeException {

	private static final long serialVersionUID = 5805219563957084601L;

	public PersonneMoraleException(Throwable e) {
		super(e);
	}

	public PersonneMoraleException(String string, Throwable e) {
		super(string, e);
	}
}
