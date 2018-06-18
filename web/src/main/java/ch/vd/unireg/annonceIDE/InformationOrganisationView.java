package ch.vd.unireg.annonceIDE;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;

/**
 * Vue web des informations sur les établissements concernés par une annonce à l'IDE.
 */
public class InformationOrganisationView {
	private Long numeroEtablissement;
	private Long numeroOrganisation;
	private Long numeroEtablissementRemplacant;

	public InformationOrganisationView(Long numeroEtablissement, Long numeroOrganisation, Long numeroEtablissementRemplacant) {
		this.numeroEtablissement = numeroEtablissement;
		this.numeroOrganisation = numeroOrganisation;
		this.numeroEtablissementRemplacant = numeroEtablissementRemplacant;
	}

	public InformationOrganisationView(@NotNull BaseAnnonceIDE.InformationOrganisation info) {
		this.numeroEtablissement = info.getNumeroEtablissement();
		this.numeroOrganisation = info.getNumeroOrganisation();
		this.numeroEtablissementRemplacant = info.getNumeroEtablissementRemplacant();
	}

	public Long getNumeroEtablissement() {
		return numeroEtablissement;
	}

	public void setNumeroEtablissement(Long numeroEtablissement) {
		this.numeroEtablissement = numeroEtablissement;
	}

	public Long getNumeroOrganisation() {
		return numeroOrganisation;
	}

	public void setNumeroOrganisation(Long numeroOrganisation) {
		this.numeroOrganisation = numeroOrganisation;
	}

	public Long getNumeroEtablissementRemplacant() {
		return numeroEtablissementRemplacant;
	}

	public void setNumeroEtablissementRemplacant(Long numeroEtablissementRemplacant) {
		this.numeroEtablissementRemplacant = numeroEtablissementRemplacant;
	}

	@Nullable
	public static InformationOrganisationView get(@Nullable BaseAnnonceIDE.InformationOrganisation info) {
		if (info == null) {
			return null;
		}
		return new InformationOrganisationView(info);
	}
}
