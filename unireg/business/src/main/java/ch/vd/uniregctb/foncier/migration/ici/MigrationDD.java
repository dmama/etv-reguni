package ch.vd.uniregctb.foncier.migration.ici;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.foncier.migration.BaseMigrationData;

/**
 * Une ligne du fichier d'import des demandes de dégrèvement (et des valeurs associées)
 */
public class MigrationDD extends BaseMigrationData {

	private String modeRattachement;

	private String motifEnvoi;

	private RegDate dateDebutValidite;

	private int anneeFiscale;

	private RegDate dateEnvoi;

	private RegDate delaiRetour;

	private RegDate dateRetour;

	private RegDate dateRappel;

	private RegDate delaiRappel;

	private int estimationFiscale;

	private int estimationSoumise;

	private int estimationExoneree;

	private int estimationCaractereSocial;

	private boolean etabliParCtb;

	private MigrationDDUsage usage;

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

	public MigrationDDUsage getUsage() {
		return usage;
	}

	public void setUsage(MigrationDDUsage usage) {
		this.usage = usage;
	}

	@Override
	protected String getAttributesToString() {
		return super.getAttributesToString() +
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
				", usage=" + usage;
	}
}
