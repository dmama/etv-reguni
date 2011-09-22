package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;
import java.util.Set;

/**
 * Historique des informations associées à une personne morale.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>corporationType</i> (xml) / <i>Corporation</i> (client java)
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class PersonneMoraleHisto extends ContribuableHisto {

	/**
	 * Désignation abrégée de la PM.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>shortName</i>.
	 */
	@XmlElement(required = true)
	public String designationAbregee;

	/**
	 * Première ligne de la raison sociale.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>name1</i>.
	 */
	@XmlElement(required = true)
	public String raisonSociale1;

	/**
	 * Deuxième ligne de la raison sociale.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>name2</i>.
	 */
	@XmlElement(required = false)
	public String raisonSociale2;

	/**
	 * Troisième ligne de la raison sociale.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>name3</i>.
	 */
	@XmlElement(required = false)
	public String raisonSociale3;

	/**
	 * Date de fin du dernier exercice commercial.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>endDateOfLastBusinessYear</i>.
	 */
	@XmlElement(required = false)
	public Date dateFinDernierExerciceCommercial;

	/**
	 * Date de bouclement futur de la PM.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>endDateOfNextBusinessYear</i>.
	 */
	@XmlElement(required = false)
	public Date dateBouclementFutur;

	/**
	 * Numéro IPMRO (n'existe pas sur toutes les PMs).
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>ipmroNumber</i>.
	 */
	@XmlElement(required = false)
	public String numeroIPMRO;

	/**
	 * L'historique des sièges existant durant la période demandée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>legalSeats</i>.
	 */
	@XmlElement(required = false)
	public List<Siege> sieges;

	/**
	 * L'historique des formes juridiques existant durant la période demandée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>legalForms</i>.
	 */
	@XmlElement(required = false)
	public List<FormeJuridique> formesJuridiques;

	/**
	 * L'historique des capitaux existant durant la période demandée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>capitals</i>.
	 */
	@XmlElement(required = false)
	public List<Capital> capitaux;

	/**
	 * L'historique des régimes fiscaux ICC (Impôt Canton-Commune) existant durant la période demandée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>taxSystemsVD</i>.
	 */
	@XmlElement(required = false)
	public List<RegimeFiscal> regimesFiscauxICC;

	/**
	 * L'historique des régimes fiscaux IFD (Impôt Fédéral Direct) existant durant la période demandée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>taxSystemsCH</i>.
	 */
	@XmlElement(required = false)
	public List<RegimeFiscal> regimesFiscauxIFD;

	/**
	 * L'historique des états de la PM existant durant la période demandée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>statuses</i>.
	 */
	@XmlElement(required = false)
	public List<EtatPM> etats;

	public PersonneMoraleHisto() {
	}

	public PersonneMoraleHisto(PersonneMoraleHisto pm, Set<TiersPart> parts) {
		super(pm, parts);
		this.designationAbregee = pm.designationAbregee;
		this.raisonSociale1 = pm.raisonSociale1;
		this.raisonSociale2 = pm.raisonSociale2;
		this.raisonSociale3 = pm.raisonSociale3;
		this.dateFinDernierExerciceCommercial = pm.dateFinDernierExerciceCommercial;
		this.dateBouclementFutur = pm.dateBouclementFutur;
		this.numeroIPMRO = pm.numeroIPMRO;
		copyParts(pm, parts);
	}

	private void copyParts(PersonneMoraleHisto pm, Set<TiersPart> parts) {

		if (parts != null && parts.contains(TiersPart.SIEGES)) {
			this.sieges = pm.sieges;
		}

		if (parts != null && parts.contains(TiersPart.FORMES_JURIDIQUES)) {
			this.formesJuridiques = pm.formesJuridiques;
		}

		if (parts != null && parts.contains(TiersPart.CAPITAUX)) {
			this.capitaux = pm.capitaux;
		}

		if (parts != null && parts.contains(TiersPart.REGIMES_FISCAUX)) {
			this.regimesFiscauxICC = pm.regimesFiscauxICC;
			this.regimesFiscauxIFD = pm.regimesFiscauxIFD;
		}

		if (parts != null && parts.contains(TiersPart.ETATS_PM)) {
			this.etats = pm.etats;
		}
	}

	@Override
	public TiersHisto clone(Set<TiersPart> parts) {
		return new PersonneMoraleHisto(this, parts);
	}

	@Override
	public void copyPartsFrom(TiersHisto tiers, Set<TiersPart> parts) {
		super.copyPartsFrom(tiers, parts);
		copyParts((PersonneMoraleHisto) tiers, parts);
	}
}
