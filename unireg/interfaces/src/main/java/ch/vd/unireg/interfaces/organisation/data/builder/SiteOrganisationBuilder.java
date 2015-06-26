package ch.vd.unireg.interfaces.organisation.data.builder;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.Fonction;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;

public class SiteOrganisationBuilder implements DataBuilder<SiteOrganisation> {

	private final long cantonalId;
	@NotNull
	private final List<DateRanged<String>> nom;

	public DonneesRC rc;
	public DonneesRegistreIDE ide;

	private Map<String,List<DateRanged<String>>> identifiants;
	private List<DateRanged<String>> nomsAdditionnels;
	private List<DateRanged<TypeDeSite>> typeDeSite;
	/**
	 * municipalityId du SwissMunicipality
	 */
	private List<DateRanged<Integer>> siege;
	private List<DateRanged<Fonction>> fonction;
	private List<DateRanged<Long>> remplacePar;
	private List<DateRanged<Long>> enRemplacementDe;

	public SiteOrganisationBuilder(long cantonalId, @NotNull List<DateRanged<String>> nom) {
		this.cantonalId = cantonalId;
		this.nom = nom;
	}

	@Override
	public SiteOrganisation build() {
		return new SiteOrganisation(cantonalId, nom, rc, ide, identifiants, nomsAdditionnels, typeDeSite,
		                            siege, fonction, remplacePar, enRemplacementDe);
	}

	public void setEnRemplacementDe(List<DateRanged<Long>> enRemplacementDe) {
		this.enRemplacementDe = enRemplacementDe;
	}

	public void setFonction(List<DateRanged<Fonction>> fonction) {
		this.fonction = fonction;
	}

	public void setIde(DonneesRegistreIDE ide) {
		this.ide = ide;
	}

	public void setIdentifiants(Map<String, List<DateRanged<String>>> identifiants) {
		this.identifiants = identifiants;
	}

	public void setNomsAdditionnels(List<DateRanged<String>> nomsAdditionnels) {
		this.nomsAdditionnels = nomsAdditionnels;
	}

	public void setRc(DonneesRC rc) {
		this.rc = rc;
	}

	public void setRemplacePar(List<DateRanged<Long>> remplacePar) {
		this.remplacePar = remplacePar;
	}

	public void setSiege(List<DateRanged<Integer>> siege) {
		this.siege = siege;
	}

	public void setTypeDeSite(List<DateRanged<TypeDeSite>> typeDeSite) {
		this.typeDeSite = typeDeSite;
	}

}
