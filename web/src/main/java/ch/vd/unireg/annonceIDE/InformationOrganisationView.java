package ch.vd.unireg.annonceIDE;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;

/**
 * Vue web des informations sur les établissements concernés par une annonce à l'IDE.
 */
public class InformationOrganisationView {
	private Long numeroSite;
	private Long numeroOrganisation;
	private Long numeroSiteRemplacant;

	public InformationOrganisationView(Long numeroSite, Long numeroOrganisation, Long numeroSiteRemplacant) {
		this.numeroSite = numeroSite;
		this.numeroOrganisation = numeroOrganisation;
		this.numeroSiteRemplacant = numeroSiteRemplacant;
	}

	public InformationOrganisationView(@NotNull BaseAnnonceIDE.InformationOrganisation info) {
		this.numeroSite = info.getNumeroSite();
		this.numeroOrganisation = info.getNumeroOrganisation();
		this.numeroSiteRemplacant = info.getNumeroSiteRemplacant();
	}

	public Long getNumeroSite() {
		return numeroSite;
	}

	public void setNumeroSite(Long numeroSite) {
		this.numeroSite = numeroSite;
	}

	public Long getNumeroOrganisation() {
		return numeroOrganisation;
	}

	public void setNumeroOrganisation(Long numeroOrganisation) {
		this.numeroOrganisation = numeroOrganisation;
	}

	public Long getNumeroSiteRemplacant() {
		return numeroSiteRemplacant;
	}

	public void setNumeroSiteRemplacant(Long numeroSiteRemplacant) {
		this.numeroSiteRemplacant = numeroSiteRemplacant;
	}

	@Nullable
	public static InformationOrganisationView get(@Nullable BaseAnnonceIDE.InformationOrganisation info) {
		if (info == null) {
			return null;
		}
		return new InformationOrganisationView(info);
	}
}
