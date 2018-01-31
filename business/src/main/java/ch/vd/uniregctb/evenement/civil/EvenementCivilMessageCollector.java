package ch.vd.uniregctb.evenement.civil;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.evenement.common.EvenementErreur;
import ch.vd.uniregctb.evenement.common.EvenementRegistreErreurFactory;
import ch.vd.uniregctb.type.TypeEvenementErreur;

public class EvenementCivilMessageCollector<T extends EvenementErreur> implements EvenementCivilErreurCollector, EvenementCivilWarningCollector {

	private final EvenementRegistreErreurFactory<T> factory;
	private final List<T> erreurs = new ArrayList<>();
	private final List<T> warnings = new ArrayList<>();

	public EvenementCivilMessageCollector(EvenementRegistreErreurFactory<T> factory) {
		this.factory = factory;
	}

	@Override
	public void addErreur(Exception e) {
		add(factory.createErreur(e));
	}

	@Override
	public void addErreur(String msg) {
		add(factory.createErreur(msg));
	}

	@Override
	public void addWarning(String msg) {
		add(factory.createErreur(msg, TypeEvenementErreur.WARNING));
	}

	private void add(T erreur) {
		if (erreur != null) {
			switch (erreur.getType()) {
				case ERROR:
					erreurs.add(erreur);
					break;
				case WARNING:
					warnings.add(erreur);
					break;
				default:
					throw new IllegalArgumentException("Type d'erreur non supporté : " + erreur.getType());
			}
		}
	}

	public List<T> getErreurs() {
		return erreurs;
	}

	public List<T> getWarnings() {
		return warnings;
	}

	@Override
	public boolean hasErreurs() {
		return !erreurs.isEmpty();
	}

	@Override
	public boolean hasWarnings() {
		return !warnings.isEmpty();
	}

	public void clear() {
		erreurs.clear();
		warnings.clear();
	}
}
