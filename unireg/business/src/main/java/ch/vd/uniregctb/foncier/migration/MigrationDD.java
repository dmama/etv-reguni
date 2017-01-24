package ch.vd.uniregctb.foncier.migration;

import java.util.HashSet;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;

/**
 * Valeurs importées de SIMPA concernant les demandes de dégrèvement actives.
 */
public class MigrationDD {

	private long numeroEntreprise;

	private String nomEntreprise;

	private long noAciCommune;

	private String nomCommune;

	private String noBaseParcelle;

	private String noParcelle;

	private String noLotPPE;

	private RegDate dateDebutRattachement;

	private RegDate dateFinRattachement;

	private String modeRattachement;

	private String motifEnvoi;

	private RegDate dateDebutValidite;

	private int anneeFiscale;

	private RegDate dateEnvoi;

	private RegDate delaiRetour;

	private RegDate dateRetour;

	private RegDate dateRappel;

	private RegDate delaiRappel;

	// taxation (données stockées pour d'éventuele besoins ultérieurs)

	private int estimationFiscale;

	private int estimationSoumise;

	private int estimationExoneree;

	private int estimationCaractereSocial;

	private boolean etabliParCtb;

	private Set<MigrationDDUsage> usages;

	public long getNumeroEntreprise() {
		return numeroEntreprise;
	}

	public void setNumeroEntreprise(long numeroEntreprise) {
		this.numeroEntreprise = numeroEntreprise;
	}

	public String getNomEntreprise() {
		return nomEntreprise;
	}

	public void setNomEntreprise(String nomEntreprise) {
		this.nomEntreprise = nomEntreprise;
	}

	public long getNoAciCommune() {
		return noAciCommune;
	}

	public void setNoAciCommune(long noAciCommune) {
		this.noAciCommune = noAciCommune;
	}

	public String getNomCommune() {
		return nomCommune;
	}

	public void setNomCommune(String nomCommune) {
		this.nomCommune = nomCommune;
	}

	public String getNoBaseParcelle() {
		return noBaseParcelle;
	}

	public void setNoBaseParcelle(String noBaseParcelle) {
		this.noBaseParcelle = noBaseParcelle;
	}

	public String getNoParcelle() {
		return noParcelle;
	}

	public void setNoParcelle(String noParcelle) {
		this.noParcelle = noParcelle;
	}

	public String getNoLotPPE() {
		return noLotPPE;
	}

	public void setNoLotPPE(String noLotPPE) {
		this.noLotPPE = noLotPPE;
	}

	public RegDate getDateDebutRattachement() {
		return dateDebutRattachement;
	}

	public void setDateDebutRattachement(RegDate dateDebutRattachement) {
		this.dateDebutRattachement = dateDebutRattachement;
	}

	public RegDate getDateFinRattachement() {
		return dateFinRattachement;
	}

	public void setDateFinRattachement(RegDate dateFinRattachement) {
		this.dateFinRattachement = dateFinRattachement;
	}

	public String getModeRattachement() {
		return modeRattachement;
	}

	public void setModeRattachement(String modeRattachement) {
		this.modeRattachement = modeRattachement;
	}

	public String getMotifEnvoi() {
		return motifEnvoi;
	}

	public void setMotifEnvoi(String motifEnvoi) {
		this.motifEnvoi = motifEnvoi;
	}

	public RegDate getDateDebutValidite() {
		return dateDebutValidite;
	}

	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
	}

	public int getAnneeFiscale() {
		return anneeFiscale;
	}

	public void setAnneeFiscale(int anneeFiscale) {
		this.anneeFiscale = anneeFiscale;
	}

	public RegDate getDateEnvoi() {
		return dateEnvoi;
	}

	public void setDateEnvoi(RegDate dateEnvoi) {
		this.dateEnvoi = dateEnvoi;
	}

	public RegDate getDelaiRetour() {
		return delaiRetour;
	}

	public void setDelaiRetour(RegDate delaiRetour) {
		this.delaiRetour = delaiRetour;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public void setDateRetour(RegDate dateRetour) {
		this.dateRetour = dateRetour;
	}

	public RegDate getDateRappel() {
		return dateRappel;
	}

	public void setDateRappel(RegDate dateRappel) {
		this.dateRappel = dateRappel;
	}

	public RegDate getDelaiRappel() {
		return delaiRappel;
	}

	public void setDelaiRappel(RegDate delaiRappel) {
		this.delaiRappel = delaiRappel;
	}

	public int getEstimationFiscale() {
		return estimationFiscale;
	}

	public void setEstimationFiscale(int estimationFiscale) {
		this.estimationFiscale = estimationFiscale;
	}

	public int getEstimationSoumise() {
		return estimationSoumise;
	}

	public void setEstimationSoumise(int estimationSoumise) {
		this.estimationSoumise = estimationSoumise;
	}

	public int getEstimationExoneree() {
		return estimationExoneree;
	}

	public void setEstimationExoneree(int estimationExoneree) {
		this.estimationExoneree = estimationExoneree;
	}

	public int getEstimationCaractereSocial() {
		return estimationCaractereSocial;
	}

	public void setEstimationCaractereSocial(int estimationCaractereSocial) {
		this.estimationCaractereSocial = estimationCaractereSocial;
	}

	public boolean isEtabliParCtb() {
		return etabliParCtb;
	}

	public void setEtabliParCtb(boolean etabliParCtb) {
		this.etabliParCtb = etabliParCtb;
	}

	public Set<MigrationDDUsage> getUsages() {
		return usages;
	}

	public void setUsages(Set<MigrationDDUsage> usages) {
		this.usages = usages;
	}

	public void addUsage(MigrationDDUsage usage) {
		if (usages == null) {
			usages = new HashSet<>();
		}
		usages.add(usage);
	}

	@Override
	public String toString() {
		return "MigrationDD{" +
				", numeroEntreprise=" + numeroEntreprise +
				", nomEntreprise='" + nomEntreprise + '\'' +
				", noAciCommune=" + noAciCommune +
				", nomCommune='" + nomCommune + '\'' +
				", noBaseParcelle='" + noBaseParcelle + '\'' +
				", noParcelle='" + noParcelle + '\'' +
				", noLotPPE='" + noLotPPE + '\'' +
				", dateDebutRattachement=" + dateDebutRattachement +
				", dateFinRattachement=" + dateFinRattachement +
				", modeRattachement='" + modeRattachement + '\'' +
				", motifEnvoi='" + motifEnvoi + '\'' +
				", dateDebutValidite=" + dateDebutValidite +
				", anneeFiscale=" + anneeFiscale +
				", dateEnvoi=" + dateEnvoi +
				", delaiRetour=" + delaiRetour +
				", dateRetour=" + dateRetour +
				", dateRappel=" + dateRappel +
				", delaiRappel=" + delaiRappel +
				", estimationFiscale=" + estimationFiscale +
				", estimationSoumise=" + estimationSoumise +
				", estimationExoneree=" + estimationExoneree +
				", estimationCaractereSocial=" + estimationCaractereSocial +
				", etabliParCtb=" + etabliParCtb +
				"}";
	}
}
