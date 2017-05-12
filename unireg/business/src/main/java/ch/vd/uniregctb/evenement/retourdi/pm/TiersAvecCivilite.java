package ch.vd.uniregctb.evenement.retourdi.pm;

import java.util.Collections;

import ch.vd.uniregctb.adresse.CiviliteSupplier;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;
import ch.vd.uniregctb.type.FormulePolitesse;

final class TiersAvecCivilite extends Entreprise implements CiviliteSupplier {

	private final String salutations;

	public TiersAvecCivilite(String salutations, String nomRaisonSociale) {
		this.salutations = salutations;
		setDonneesCiviles(Collections.singleton(new RaisonSocialeFiscaleEntreprise(null, null, nomRaisonSociale)));
	}

	@Override
	public String getSalutations() {
		return salutations;
	}

	@Override
	public String getFormuleAppel() {
		return FormulePolitesse.MADAME_MONSIEUR.formuleAppel();
	}
}
