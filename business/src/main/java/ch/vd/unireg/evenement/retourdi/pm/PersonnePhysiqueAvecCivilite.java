package ch.vd.uniregctb.evenement.retourdi.pm;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.adresse.CiviliteSupplier;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.FormulePolitesse;

final class PersonnePhysiqueAvecCivilite extends PersonnePhysique implements CiviliteSupplier {

	private final String titre;

	public PersonnePhysiqueAvecCivilite(@Nullable Long noAvs, String titre, String prenom, String nom) {
		super(Boolean.FALSE);
		if (noAvs != null) {
			setNumeroAssureSocial(Long.toString(noAvs));
		}
		setNom(nom);
		setTousPrenoms(prenom);
		setPrenomUsuel(prenom);
		this.titre = titre;
	}

	@Override
	public String getSalutations() {
		return titre;
	}

	@Override
	public String getFormuleAppel() {
		return FormulePolitesse.MADAME_MONSIEUR.formuleAppel();
	}
}
