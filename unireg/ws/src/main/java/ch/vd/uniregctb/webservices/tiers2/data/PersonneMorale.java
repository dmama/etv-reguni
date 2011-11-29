package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.webservices.tiers2.impl.RangeHelper;

/**
 * Informations associées à une personne morale pour une date donnée.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>corporationType</i> (xml) / <i>Corporation</i> (client java)
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class PersonneMorale extends Contribuable {

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
	 * Le siège valide à la date demandée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>legalSeats</i>.
	 */
	@XmlElement(required = false)
	public Siege siege;

	/**
	 * La forme juridique valide à la date demandée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>legalForms</i>.
	 */
	@XmlElement(required = false)
	public FormeJuridique formeJuridique;

	/**
	 * Le capital valide à la date demandée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>capitals</i>.
	 */
	@XmlElement(required = false)
	public Capital capital;

	/**
	 * Le régime fiscal ICC (Impôt Canton-Commune) valide à la date demandée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>taxSystemsVD</i>.
	 */
	@XmlElement(required = false)
	public RegimeFiscal regimeFiscalICC;

	/**
	 * Le régime fiscal IFD (Impôt Fédéral Direct) valide à la date demandée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>taxSystemsCH</i>.
	 */
	@XmlElement(required = false)
	public RegimeFiscal regimeFiscalIFD;

	/**
	 * L'état de la PM valide à la date demandée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>statuses</i>.
	 */
	@XmlElement(required = false)
	public EtatPM etat;

	public PersonneMorale() {
	}

	public PersonneMorale(PersonneMorale pm, Set<TiersPart> parts) {
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

	public PersonneMorale(PersonneMoraleHisto pmHisto, @Nullable Date date) {
		this.numero = pmHisto.numero;
		this.adresseCourrierElectronique = pmHisto.adresseCourrierElectronique;
		this.adresseEnvoi = pmHisto.adresseEnvoi;
		this.adresseDomicileFormattee = pmHisto.adresseDomicileFormattee;
		this.adresseRepresentationFormattee = pmHisto.adresseRepresentationFormattee;
		this.adressePoursuiteFormattee = pmHisto.adressePoursuiteFormattee;
		this.blocageRemboursementAutomatique = pmHisto.blocageRemboursementAutomatique;
		this.complementNom = pmHisto.complementNom;
		this.comptesBancaires = pmHisto.comptesBancaires;
		this.dateBouclementFutur = pmHisto.dateBouclementFutur;
		this.dateDebutActivite = pmHisto.dateDebutActivite;
		this.dateFinActivite = pmHisto.dateFinActivite;
		this.dateFinDernierExerciceCommercial = pmHisto.dateFinDernierExerciceCommercial;
		this.designationAbregee = pmHisto.designationAbregee;
		this.numeroIPMRO = pmHisto.numeroIPMRO;
		this.numeroTelecopie = pmHisto.numeroTelecopie;
		this.numeroTelPortable = pmHisto.numeroTelPortable;
		this.numeroTelPrive = pmHisto.numeroTelPrive;
		this.numeroTelProf = pmHisto.numeroTelProf;
		this.personneContact = pmHisto.personneContact;
		this.raisonSociale1 = pmHisto.raisonSociale1;
		this.raisonSociale2 = pmHisto.raisonSociale2;
		this.raisonSociale3 = pmHisto.raisonSociale3;

		this.adresseCourrier = RangeHelper.getAt(pmHisto.adressesCourrier, date);
		this.adresseDomicile = RangeHelper.getAt(pmHisto.adressesDomicile, date);
		this.adressePoursuite = RangeHelper.getAt(pmHisto.adressesPoursuite, date);
		this.adresseRepresentation = RangeHelper.getAt(pmHisto.adressesRepresentation, date);
		this.assujettissementLIC = RangeHelper.getAt(pmHisto.assujettissementsLIC, date);
		this.assujettissementLIFD = RangeHelper.getAt(pmHisto.assujettissementsLIFD, date);
		this.autresForsFiscaux = RangeHelper.getAllAt(pmHisto.autresForsFiscaux, date);
		this.capital = RangeHelper.getAt(pmHisto.capitaux, date);
		this.declaration = RangeHelper.getAt(pmHisto.declarations, date);
		this.etat = RangeHelper.getAt(pmHisto.etats, date);
		this.forFiscalPrincipal = RangeHelper.getAt(pmHisto.forsFiscauxPrincipaux, date);
		this.forGestion = RangeHelper.getAt(pmHisto.forsGestions, date);
		this.formeJuridique = RangeHelper.getAt(pmHisto.formesJuridiques, date);
		this.periodeImposition = RangeHelper.getAt(pmHisto.periodesImposition, date);
		this.regimeFiscalIFD = RangeHelper.getAt(pmHisto.regimesFiscauxIFD, date);
		this.regimeFiscalICC = RangeHelper.getAt(pmHisto.regimesFiscauxICC, date);
		this.siege = RangeHelper.getAt(pmHisto.sieges, date);
	}


	private void copyParts(PersonneMorale pm, Set<TiersPart> parts) {

		if (parts != null && parts.contains(TiersPart.SIEGES)) {
			this.siege = pm.siege;
		}

		if (parts != null && parts.contains(TiersPart.FORMES_JURIDIQUES)) {
			this.formeJuridique = pm.formeJuridique;
		}

		if (parts != null && parts.contains(TiersPart.CAPITAUX)) {
			this.capital = pm.capital;
		}

		if (parts != null && parts.contains(TiersPart.REGIMES_FISCAUX)) {
			this.regimeFiscalICC = pm.regimeFiscalICC;
			this.regimeFiscalIFD = pm.regimeFiscalIFD;
		}

		if (parts != null && parts.contains(TiersPart.ETATS_PM)) {
			this.etat = pm.etat;
		}
	}

	@Override
	public Tiers clone(Set<TiersPart> parts) {
		return new PersonneMorale(this, parts);
	}

	@Override
	public void copyPartsFrom(Tiers tiers, Set<TiersPart> parts) {
		super.copyPartsFrom(tiers, parts);
		copyParts((PersonneMorale) tiers, parts);
	}
}
