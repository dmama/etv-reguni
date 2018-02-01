package ch.vd.unireg.tache.view;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

public class TacheCriteriaViewBase implements Serializable {

	private static final long serialVersionUID = -5764421738041828312L;

	private static final Logger LOGGER = LoggerFactory.getLogger(TacheCriteriaViewBase.class);

	private TypeTache typeTache;
	private String commentaire;
	private TypeEtatTache etatTache;
	private Date dateCreationDepuis;
	private Date dateCreationJusqua;
	private Integer annee;
	private boolean voirTachesAutomatiques;
	private String officeImpot;
	private Long numeroCTB;
	private boolean voirTachesAnnulees;

	public TypeTache getTypeTache() {
		return typeTache;
	}

	public void setTypeTache(TypeTache typeTache) {
		this.typeTache = typeTache;
	}

	public TypeEtatTache getEtatTache() {
		return etatTache;
	}

	public void setEtatTache(TypeEtatTache etatTache) {
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

	public Integer getAnnee() {
		return annee;
	}

	public void setAnnee(Integer annee) {
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

	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}

	/**
	 * @return true si aucun paramètre de recherche n'est renseigné. false autrement.
	 */
	public boolean isEmpty() {
		return etatTache == null &&
				dateCreationDepuis == null &&
				dateCreationJusqua == null &&
				!voirTachesAutomatiques &&
				officeImpot == null &&
				numeroCTB == null &&
				!voirTachesAnnulees &&
				StringUtils.isBlank(commentaire);
	}

	/**
	 * Interprète les critères reçu par le web en critères compréhensibles par le DAO de core.
	 */
	public TacheCriteria asCoreCriteria() {

		Integer oid = null;
		if (StringUtils.isNotBlank(officeImpot)) {
			try {
				oid = Integer.valueOf(officeImpot);
			}
			catch (NumberFormatException ignored) {
				// impossible d'interpéter l'OID -> on l'ignore civilement
				LOGGER.warn("Le critère sur l'OID de la tâche avec la valeur [" + officeImpot + "] est incorrect et il est ignoré.");
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


		final TacheCriteria criteria = new TacheCriteria();
		criteria.setAnnee(annee);
		criteria.setDateCreationDepuis(dateCreationDepuis);
		criteria.setDateCreationJusqua(dateCreationJusqua);
		criteria.setDateEcheanceJusqua(dateEcheanceJusqua);
		criteria.setEtatTache(etatTache);
		criteria.setTypeTache(typeTache);
		criteria.setOid(oid);
		criteria.setNumeroCTB(numeroCTB);
		criteria.setInclureTachesAnnulees(voirTachesAnnulees);
		criteria.setCommentaire(commentaire);
		return criteria;
	}
}
