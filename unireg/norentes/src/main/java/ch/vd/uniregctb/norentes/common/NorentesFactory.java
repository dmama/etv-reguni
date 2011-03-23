package ch.vd.uniregctb.norentes.common;

public class NorentesFactory {


	public static NorentesManager getNorentesManager() {
		return NorentesManagerImpl.getInstance();
	}
}
