package ch.vd.uniregctb.tache.view;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.TacheCriteria;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.Date;

public class TacheCriteriaViewBase implements Serializable {

	private static final long serialVersionUID = -1984186456557111180L;
	private static final Logger LOGGER = Logger.getLogger(TacheCriteriaViewBase.class);
	private final String TOUS = "TOUS";

	private String typeTache;
	private String etatTache;
	private Date dateCreationDepuis;
	private Date dateCreationJusqua;
	private String annee;
	private boolean voirTachesAutomatiques;
	private String officeImpot;
	private Long numeroCTB;
	private boolean voirTachesAnnulees;

	public String getTypeTache() {
		return typeTache;
	}

	public void setTypeTache(String typeTache) {
		this.typeTache = typeTache;
	}

	public String getEtatTache() {
		return etatTache;
	}

	public void setEtatTache(String etatTache) {
		this.etatTache = etatTache;
	}

	public Date getDateCreationDepuis() {
		return dateCreationDepuis;
	}

	public void setDateCreationDepuis(Date dateCreationDepuis) {
		this.dateCreationDepuis = dateCreationDepuis;
	}

	public Date getDateCreationJusqua() {
		return dateCreationJusqua;
	}

	public void setDateCreationJusqua(Date dateCreationJusqua) {
		this.dateCreationJusqua = dateCreationJusqua;
	}

	public String getAnnee() {
		return annee;
	}

	public void setAnnee(String annee) {
		this.annee = annee;
	}

	public boolean isVoirTachesAutomatiques() {
		return voirTachesAutomatiques;
	}

	public void setVoirTachesAutomatiques(boolean voirTachesAutomatiques) {
		this.voirTachesAutomatiques = voirTachesAutomatiques;
	}

	public String getOfficeImpot() {
		return officeImpot;
	}

	public void setOfficeImpot(String officeImpot) {
		this.officeImpot = officeImpot;
	}

	public Long getNumeroCTB() {
		return numeroCTB;
	}

	public void setNumeroCTB(Long numeroCTB) {
		this.numeroCTB = numeroCTB;
	}

	public boolean isVoirTachesAnnulees() {
		return voirTachesAnnulees;
	}

	public void setVoirTachesAnnulees(boolean voirTachesAnnulees) {
		this.voirTachesAnnulees = voirTachesAnnulees;
	}

	/**
	 * @return true si aucun paramétre de recherche n'est renseigné. false autrement.
	 */
	public boolean isEmpty() {
		return etatTache == null &&
			dateCreationDepuis == null &&
			dateCreationJusqua == null &&
			!voirTachesAutomatiques &&
			officeImpot == null &&
			numeroCTB == null &&
			!voirTachesAnnulees;
	}

	/**
	 * Interprète les critères reçu par le web en critères compréhensibles par le DAO de core.
	 */
	public TacheCriteria asCoreCriteria() {

		Integer a = null;
		if (annee != null && !TOUS.equals(annee)) {
			try {
				a = Integer.parseInt(annee);
			}
			catch (NumberFormatException ignored) {
				// impossible d'interpéter l'année -> on l'ignore civilement
				LOGGER.warn("Le critère sur l'année avec la valeur [" + annee + "] est incorrect et il est ignoré.");
				a = null;
			}
		}

		TypeEtatTache etat = null;
		if (etatTache != null && !TOUS.equals(etatTache)) {
			try {
				etat = TypeEtatTache.valueOf(etatTache);
			}
			catch (IllegalArgumentException ignored) {
				// etat de la tâche inconnu -> on l'ignore civilement
				LOGGER.warn("Le critère sur l'état de la tâche avec la valeur [" + etatTache + "] est incorrect et il est ignoré.");
				etat = null;
			}
		}

		TypeTache type = null;
		if (typeTache != null && !TOUS.equals(typeTache)) {
			try {
				type = TypeTache.valueOf(typeTache);
			}
			catch (IllegalArgumentException ignored) {
				// type de la tâche inconnu -> on l'ignore civilement
				LOGGER.warn("Le critère sur le type de la tâche avec la valeur [" + typeTache + "] est incorrect et il est ignoré.");
				type = null;
			}
		}

		Integer oid = null;
		if (officeImpot != null && !TOUS.equals(officeImpot)) {
			try {
				oid = Integer.valueOf(officeImpot);
			}
			catch (NumberFormatException ignored) {
				// impossible d'interpéter l'OID -> on l'ignore civilement
				LOGGER.warn("Le critère sur l'OID de la tâche avec la valeur [" + officeImpot + "] est incorrect et il est ignoré.");
				type = null;
			}
		}

		RegDate dateEcheanceJusqua = null;
		if (!voirTachesAutomatiques) {
			/*
			 * Les tâches générées par les jobs ont une date d'échéance de traitement. Passé cette date, elle sont considérées comme échues
			 * et passent en traitement manuel. En conséquence, si l'on ne veut pas voir les tâches automatiques, on spécifie une condition
			 * sur la date d'échéance au jour courant, ce qui a pour effet de filtrer toutes les tâches automatiques non encore échues.
			 */
			dateEcheanceJusqua = RegDate.get();
		}


		TacheCriteria criteria = new TacheCriteria();
		criteria.setAnnee(a);
		criteria.setDateCreationDepuis(dateCreationDepuis);
		criteria.setDateCreationJusqua(dateCreationJusqua);
		criteria.setDateEcheanceJusqua(dateEcheanceJusqua);
		criteria.setEtatTache(etat);
		criteria.setTypeTache(type);
		criteria.setOid(oid);
		criteria.setNumeroCTB(numeroCTB);
		criteria.setInclureTachesAnnulees(voirTachesAnnulees);

		return criteria;
	}
}
