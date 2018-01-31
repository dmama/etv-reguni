package ch.vd.uniregctb.scheduler;

/**
 * Paramètre de batch qui permet de spécifier le contenu d'un fichier.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class JobParamFile extends JobParamType {

	public JobParamFile() {
		super(byte[].class);
	}

	@Override
	public Object stringToValue(String s) throws IllegalArgumentException {
		throw new IllegalArgumentException("Pas de conversion String -> byte[] pour les fichiers.");
	}

	@Override
	public String valueToString(Object o) throws IllegalArgumentException {
		throw new IllegalArgumentException("Pas de conversion byte[] -> String pour les fichiers.");
	}
}
