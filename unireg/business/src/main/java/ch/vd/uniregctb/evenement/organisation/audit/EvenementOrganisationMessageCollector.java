package ch.vd.uniregctb.evenement.organisation.audit;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.evenement.common.EvenementErreur;
import ch.vd.uniregctb.evenement.common.EvenementRegistreErreurFactory;
import ch.vd.uniregctb.type.TypeEvenementErreur;

public class EvenementOrganisationMessageCollector<T extends EvenementErreur>
		implements EvenementOrganisationErreurCollector, EvenementOrganisationWarningCollector , EvenementOrganisationSuiviCollector {

	private final EvenementRegistreErreurFactory<T> factory;
	private final List<T> entrees = new ArrayList<>();
	private boolean hasErreurs = false;
	private boolean hasWarnings = false;
	private boolean hasSuivis = false;

	public EvenementOrganisationMessageCollector(EvenementRegistreErreurFactory<T> factory) {
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
	@Override
	public void addSuivi(String msg) {
		add(factory.createErreur(msg, TypeEvenementErreur.SUIVI));
	}

	private void add(T entree) {
		if (entree != null) {
			switch (entree.getType()) {
				case ERROR:
					entrees.add(entree);
					hasErreurs = true;
					break;
				case WARNING:
					entrees.add(entree);
					hasWarnings = true;
					break;
				case SUIVI:
					entrees.add(entree);
					hasSuivis = true;
					break;
				default:
					throw new IllegalArgumentException("Type d'erreur non supporté : " + entree.getType());
			}
		}
	}

	public List<T> getEntrees() {
		return entrees;
	}

	public List<T> getErreurs() {
		List<T> erreurs = new ArrayList<>();
		for (T entree : this.entrees) {
			if (entree.getType() == TypeEvenementErreur.ERROR) {
				erreurs.add(entree);
			}
		}
		return erreurs;
	}

	public List<T> getWarnings() {
		List<T> warnings = new ArrayList<>();
		for (T entree : this.entrees) {
			if (entree.getType() == TypeEvenementErreur.WARNING) {
				warnings.add(entree);
			}
		}
		return warnings;
	}

	public List<T> getSuivis() {
		List<T> suivis = new ArrayList<>();
		for (T entree : this.entrees) {
			if (entree.getType() == TypeEvenementErreur.SUIVI) {
				suivis.add(entree);
			}
		}
		return suivis;
	}

	@Override
	public boolean hasErreurs() {
		return hasErreurs;
	}

	@Override
	public boolean hasWarnings() {
		return hasWarnings;
	}

	@Override
	public boolean hasSuivis() {
		return hasSuivis;
	}

	public void clear() {
		entrees.clear();
	}
}
