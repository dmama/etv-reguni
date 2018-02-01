package ch.vd.unireg.evenement.retourdi.pm;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.adresse.CiviliteSupplier;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.FormulePolitesse;

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
