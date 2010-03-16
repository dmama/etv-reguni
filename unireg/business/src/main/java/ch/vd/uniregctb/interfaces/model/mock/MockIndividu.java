package ch.vd.uniregctb.interfaces.model.mock;

import java.util.ArrayList;
import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.AdoptionReconnaissance;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivilList;
import ch.vd.uniregctb.interfaces.model.HistoriqueIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.Tutelle;

public class MockIndividu extends MockEntiteCivile implements Individu {

	private Collection<AdoptionReconnaissance> adoptionsReconnaissances;
	private Individu conjoint;
	private RegDate dateDeces;
	private RegDate dateNaissance;
	private HistoriqueIndividu dernierHistoriqueIndividu;
	private Collection<Individu> enfants;
	private EtatCivilList etatsCivils;
	private Collection<HistoriqueIndividu> historiqueIndividu;
	private Individu mere;
	private Collection<Nationalite> nationalites;
	private long noTechnique;
	private String nouveauNoAVS;
	private String numeroRCE;
	private Origine origine;
	private Individu pere;
	private Collection<Permis> permis;
	private Tutelle tutelle;
	private boolean sexeMasculin;

	public Collection<AdoptionReconnaissance> getAdoptionsReconnaissances() {
		return adoptionsReconnaissances;
	}

	public void setAdoptionsReconnaissances(Collection<AdoptionReconnaissance> adoptionsReconnaissances) {
		this.adoptionsReconnaissances = adoptionsReconnaissances;
	}

	public Individu getConjoint() {
		return conjoint;
	}

	public void setConjoint(Individu conjoint) {
		this.conjoint = conjoint;
	}

	public RegDate getDateDeces() {
		return dateDeces;
	}

	public void setDateDeces(RegDate dateDeces) {
		this.dateDeces = dateDeces;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(RegDate dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	public HistoriqueIndividu getDernierHistoriqueIndividu() {
		return dernierHistoriqueIndividu;
	}

	public void setDernierHistoriqueIndividu(HistoriqueIndividu dernierHistoriqueIndividu) {
		this.dernierHistoriqueIndividu = dernierHistoriqueIndividu;
	}

	public Collection<Individu> getEnfants() {
		return enfants;
	}

	public void setEnfants(Collection<Individu> enfants) {
		this.enfants = enfants;
	}

	public EtatCivilList getEtatsCivils() {
		return etatsCivils;
	}

	public EtatCivil getEtatCivilCourant() {

		EtatCivil etatCivilCourant = null;

		int noSequence = -1;
		for (EtatCivil etatCivil : getEtatsCivils()) {
			if (etatCivil.getNoSequence() > noSequence) {
				etatCivilCourant = etatCivil;
				noSequence = etatCivil.getNoSequence();
			}
		}

		return etatCivilCourant;
	}

	public EtatCivil getEtatCivil(RegDate date) {
		if (etatsCivils == null) {
			return null;
		}
		return etatsCivils.getEtatCivilAt(date);
	}

	public void setEtatsCivils(Collection<EtatCivil> etatsCivils) {
		if (etatsCivils instanceof EtatCivilList) {
			this.etatsCivils = (EtatCivilList) etatsCivils;
		}
		else {
			this.etatsCivils = new EtatCivilList(getNoTechnique(), etatsCivils);
		}
	}

	public Collection<HistoriqueIndividu> getHistoriqueIndividu() {
		return historiqueIndividu;
	}

	public void setHistoriqueIndividu(Collection<HistoriqueIndividu> historiqueIndividu) {
		this.historiqueIndividu = historiqueIndividu;
	}

	/**
	 * Ajoute un historique à la liste, et défini cet historique comme le dernier.
	 */
	public void addHistoriqueIndividu(HistoriqueIndividu h) {
		if (historiqueIndividu == null) {
			historiqueIndividu = new ArrayList<HistoriqueIndividu>();
		}
		historiqueIndividu.add(h);
		dernierHistoriqueIndividu = h;
	}

	public Individu getMere() {
		return mere;
	}

	public void setMere(Individu mere) {
		this.mere = mere;
	}

	public Collection<Nationalite> getNationalites() {
		return nationalites;
	}

	public void setNationalites(Collection<Nationalite> nationalites) {
		this.nationalites = nationalites;
	}

	public long getNoTechnique() {
		return noTechnique;
	}

	public void setNoTechnique(long noTechnique) {
		this.noTechnique = noTechnique;
	}

	public String getNouveauNoAVS() {
		return nouveauNoAVS;
	}

	public void setNouveauNoAVS(String nouveauNoAVS) {
		this.nouveauNoAVS = nouveauNoAVS;
	}

	public String getNumeroRCE() {
		return numeroRCE;
	}

	public void setNumeroRCE(String numeroRCE) {
		this.numeroRCE = numeroRCE;
	}

	public Origine getOrigine() {
		return origine;
	}

	public void setOrigine(Origine origine) {
		this.origine = origine;
	}

	public Individu getPere() {
		return pere;
	}

	public void setPere(Individu pere) {
		this.pere = pere;
	}

	public Collection<Permis> getPermis() {
		return permis;
	}

	public void setPermis(Collection<Permis> permis) {
		this.permis = permis;
	}

	public Tutelle getTutelle() {
		return tutelle;
	}

	public void setTutelle(Tutelle tutelle) {
		this.tutelle = tutelle;
	}

	public boolean isSexeMasculin() {
		return sexeMasculin;
	}

	public void setSexeMasculin(boolean sexeMasculin) {
		this.sexeMasculin = sexeMasculin;
	}
}
