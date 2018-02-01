package ch.vd.unireg.organisation;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 *
 */
public class OrganisationView implements Serializable {

	private static final long serialVersionUID = 2125635768219037077L;

	private Long numeroOrganisation;
	private String numeroIDE;
	private String nom;
	private String formeJuridique;
	private Integer noOFSSiege;
	private TypeAutoriteFiscale typeSiege;

	private boolean canceled;
	private Long numeroOrganisationRemplacant;

	public OrganisationView(final Organisation organisation, RegDate date) {
		this.setNumeroOrganisation(organisation.getNumeroOrganisation());
		nom = organisation.getNom(date);
		final Domicile siegePrincipal = organisation.getSiegePrincipal(date);
		noOFSSiege = siegePrincipal == null ? null : siegePrincipal.getNumeroOfsAutoriteFiscale();
		typeSiege = siegePrincipal == null ? null : siegePrincipal.getTypeAutoriteFiscale();
		formeJuridique = organisation.getFormeLegale(date) == null ? null : organisation.getFormeLegale(date).toString();
		numeroIDE = organisation.getNumeroIDE().isEmpty() ? null : organisation.getNumeroIDE().get(0).getPayload();
		final StatusRegistreIDE statusRegistreIDE = organisation.getSitePrincipal(date).getPayload().getDonneesRegistreIDE().getStatus(date);
		canceled = statusRegistreIDE != null && statusRegistreIDE == StatusRegistreIDE.RADIE;
		numeroOrganisationRemplacant = organisation.getSitePrincipal(date).getPayload().getIdeRemplacePar(date);
	}

	@SuppressWarnings("UnusedDeclaration")
	public Long getNumeroOrganisation() {
		return numeroOrganisation;
	}

	public void setNumeroOrganisation(Long numeroOrganisation) {
		this.numeroOrganisation = numeroOrganisation;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isCanceled() {
		return canceled;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	@SuppressWarnings("UnusedDeclaration")
	public Long getNumeroOrganisationRemplacant() {
		return numeroOrganisationRemplacant;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setNumeroOrganisationRemplacant(Long numeroOrganisationRemplacant) {
		this.numeroOrganisationRemplacant = numeroOrganisationRemplacant;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getNumeroIDE() {
		return numeroIDE;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setNumeroIDE(String numeroIDE) {
		this.numeroIDE = numeroIDE;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getFormeJuridique() {
		return formeJuridique;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setFormeJuridique(String formeJuridique) {
		this.formeJuridique = formeJuridique;
	}

	public Integer getNoOFSSiege() {
		return noOFSSiege;
	}

	public void setNoOFSSiege(Integer noOFSSiege) {
		this.noOFSSiege = noOFSSiege;
	}

	public TypeAutoriteFiscale getTypeSiege() {
		return typeSiege;
	}

	public void setTypeSiege(TypeAutoriteFiscale typeSiege) {
		this.typeSiege = typeSiege;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final OrganisationView that = (OrganisationView) o;

		if (isCanceled() != that.isCanceled()) return false;
		if (!getNumeroOrganisation().equals(that.getNumeroOrganisation())) return false;
		if (getNumeroIDE() != null ? !getNumeroIDE().equals(that.getNumeroIDE()) : that.getNumeroIDE() != null) return false;
		if (!getNom().equals(that.getNom())) return false;
		if (getFormeJuridique() != null ? !getFormeJuridique().equals(that.getFormeJuridique()) : that.getFormeJuridique() != null) return false;
		if (getNoOFSSiege() != null ? !getNoOFSSiege().equals(that.getNoOFSSiege()) : that.getNoOFSSiege() != null) return false;
		return !(getNumeroOrganisationRemplacant() != null ? !getNumeroOrganisationRemplacant().equals(that.getNumeroOrganisationRemplacant()) : that.getNumeroOrganisationRemplacant() != null);

	}

	@Override
	public int hashCode() {
		int result = getNumeroOrganisation().hashCode();
		result = 31 * result + (getNumeroIDE() != null ? getNumeroIDE().hashCode() : 0);
		result = 31 * result + getNom().hashCode();
		result = 31 * result + (getFormeJuridique() != null ? getFormeJuridique().hashCode() : 0);
		result = 31 * result + (getNoOFSSiege() != null ? getNoOFSSiege().hashCode() : 0);
		result = 31 * result + (isCanceled() ? 1 : 0);
		result = 31 * result + (getNumeroOrganisationRemplacant() != null ? getNumeroOrganisationRemplacant().hashCode() : 0);
		return result;
	}
}
