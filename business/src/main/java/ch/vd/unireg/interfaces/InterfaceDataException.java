package ch.vd.uniregctb.interfaces;

/**
 * Exception levée lorsque les données en provenance des interfaces ne sont pas cohérentes et nécessitent une correction dans une autre
 * application qu'Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class InterfaceDataException extends RuntimeException {

	private static final long serialVersionUID = 1337171054141903196L;

	public InterfaceDataException(Throwable e) {
		super(e);
	}

	public InterfaceDataException(String string) {
		super(string);
	}

	public InterfaceDataException(String string, Throwable e) {
		super(string, e);
	}
}
