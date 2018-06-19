package ch.vd.unireg.annonceIDE;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;

/**
 * Vue web des informations sur les établissements concernés par une annonce à l'IDE.
 */
public class InformationEntrepriseView {
	private Long numeroEtablissement;
	private Long numeroOrganisation;
	private Long numeroEtablissementRemplacant;

	public InformationEntrepriseView(Long numeroEtablissement, Long numeroOrganisation, Long numeroEtablissementRemplacant) {
		this.numeroEtablissement = numeroEtablissement;
		this.numeroOrganisation = numeroOrganisation;
		this.numeroEtablissementRemplacant = numeroEtablissementRemplacant;
	}

	public InformationEntrepriseView(@NotNull BaseAnnonceIDE.InformationEntreprise info) {
		this.numeroEtablissement = info.getNumeroEtablissement();
		this.numeroOrganisation = info.getNumeroEntreprise();
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
	public static InformationEntrepriseView get(@Nullable BaseAnnonceIDE.InformationEntreprise info) {
		if (info == null) {
			return null;
		}
		return new InformationEntrepriseView(info);
	}
}
