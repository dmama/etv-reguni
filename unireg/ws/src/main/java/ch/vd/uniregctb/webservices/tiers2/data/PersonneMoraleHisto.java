package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Etablissement;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Mandat;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.impl.Context;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.RangeHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.RangeImpl;

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

	private static final Logger LOGGER = Logger.getLogger(PersonneMoraleHisto.class);
	
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

	public PersonneMoraleHisto(Entreprise entreprise, Set<TiersPart> parts, Context context) throws BusinessException {
		
		final ch.vd.uniregctb.interfaces.model.PersonneMorale pmHost = context.servicePM.getPersonneMorale(entreprise.getNumero(), web2business(parts));
		if (pmHost == null) {
			final String message = String.format("La PM numéro %d n'existe pas dans le service PM du Host.", entreprise.getNumero());
			LOGGER.error(message);
			throw new BusinessException(message);
		}
		
		this.numero = pmHost.getNumeroEntreprise();
		this.numeroTelProf = pmHost.getTelephoneContact();
		this.numeroTelecopie = pmHost.getTelecopieContact();

		this.personneContact = pmHost.getNomContact();
		this.designationAbregee = pmHost.getDesignationAbregee();
		this.raisonSociale1 = pmHost.getRaisonSociale1();
		this.raisonSociale2 = pmHost.getRaisonSociale2();
		this.raisonSociale3 = pmHost.getRaisonSociale3();
		this.dateDebutActivite = DataHelper.coreToWeb(pmHost.getDateConstitution());
		this.dateFinActivite = DataHelper.coreToWeb(pmHost.getDateFinActivite());
		this.dateBouclementFutur = DataHelper.coreToWeb(pmHost.getDateBouclementFuture());
		this.numeroIPMRO = pmHost.getNumeroIPMRO();

		// [UNIREG-2040] on va chercher l'information de blocage dans notre base si elle existe
		this.blocageRemboursementAutomatique = entreprise.getBlocageRemboursementAutomatique();
		
		if (parts != null && parts.contains(TiersPart.ADRESSES)) {

			ch.vd.uniregctb.adresse.AdressesFiscalesHisto adresses;
			try {
				adresses = context.adresseService.getAdressesFiscalHisto(entreprise, false);
			}
			catch (ch.vd.uniregctb.adresse.AdresseException e) {
				LOGGER.error(e, e);
				throw new BusinessException(e);
			}

			if (adresses != null) {
				this.adressesCourrier = DataHelper.coreToWeb(adresses.courrier, null, context.infraService);
				this.adressesRepresentation = DataHelper.coreToWeb(adresses.representation, null, context.infraService);
				this.adressesDomicile = DataHelper.coreToWeb(adresses.domicile, null, context.infraService);
				this.adressesPoursuite = DataHelper.coreToWeb(adresses.poursuite, null, context.infraService);
				this.adressesPoursuiteAutreTiers = DataHelper.coreToWebAT(adresses.poursuiteAutreTiers, null, context.infraService);
			}
		}

		if (parts != null && parts.contains(TiersPart.ADRESSES_ENVOI)) {
			try {
				this.adresseEnvoi = DataHelper.createAdresseFormattee(entreprise, null, TypeAdresseFiscale.COURRIER, context.adresseService);
				this.adresseRepresentationFormattee = DataHelper.createAdresseFormattee(entreprise, null, TypeAdresseFiscale.REPRESENTATION, context.adresseService);
				this.adresseDomicileFormattee = DataHelper.createAdresseFormattee(entreprise, null, TypeAdresseFiscale.DOMICILE, context.adresseService);
				this.adressePoursuiteFormattee = DataHelper.createAdresseFormattee(entreprise, null, TypeAdresseFiscale.POURSUITE, context.adresseService);
				this.adressePoursuiteAutreTiersFormattee = DataHelper.createAdresseFormatteeAT(entreprise, null, TypeAdresseFiscale.POURSUITE_AUTRE_TIERS, context.adresseService);
			}
			catch (AdresseException e) {
				LOGGER.error(e, e);
				throw new BusinessException(e);
			}
		}

		if (parts != null && parts.contains(TiersPart.ASSUJETTISSEMENTS)) {
			this.assujettissementsLIC = assujettissements2web(pmHost.getAssujettissementsLIC());
			this.assujettissementsLIFD = assujettissements2web(pmHost.getAssujettissementsLIFD());
		}

		if (parts != null && parts.contains(TiersPart.CAPITAUX)) {
			this.capitaux = capitaux2web(pmHost.getCapitaux());
		}

		if (parts != null && parts.contains(TiersPart.COMPTES_BANCAIRES)) {
			this.comptesBancaires = calculateComptesBancaires(pmHost, context);
		}
		
		if (parts != null && parts.contains(TiersPart.ETATS_PM)) {
			this.etats = etats2web(pmHost.getEtats());
		}

		if (parts != null && parts.contains(TiersPart.FORMES_JURIDIQUES)) {
			this.formesJuridiques = formes2web(pmHost.getFormesJuridiques());
		}

		if (parts != null && (parts.contains(TiersPart.FORS_FISCAUX) || parts.contains(TiersPart.FORS_FISCAUX_VIRTUELS))) {
			this.forsFiscauxPrincipaux = forsPrincipaux2web(pmHost.getForsFiscauxPrincipaux(), context);
			this.autresForsFiscaux = forsSecondaires2web(pmHost.getForsFiscauxSecondaires(), context);
		}

		if (parts != null && parts.contains(TiersPart.REGIMES_FISCAUX)) {
			this.regimesFiscauxICC = regimes2web(pmHost.getRegimesVD());
			this.regimesFiscauxIFD = regimes2web(pmHost.getRegimesCH());
		}

		if (parts != null && parts.contains(TiersPart.SIEGES)) {
			this.sieges = sieges2web(pmHost.getSieges(), context);
		}
	}

	public PersonneMoraleHisto(PersonneMoraleHisto pmHisto, int annee) {
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

		final Range periode = new RangeImpl(new Date(annee, 1, 1), new Date(annee, 12, 31));
		this.adressesCourrier = RangeHelper.getAllAt(pmHisto.adressesCourrier, periode);
		this.adressesDomicile = RangeHelper.getAllAt(pmHisto.adressesDomicile, periode);
		this.adressesPoursuite = RangeHelper.getAllAt(pmHisto.adressesPoursuite, periode);
		this.adressesRepresentation = RangeHelper.getAllAt(pmHisto.adressesRepresentation, periode);
		this.assujettissementsLIC = RangeHelper.getAllAt(pmHisto.assujettissementsLIC, periode);
		this.assujettissementsLIFD = RangeHelper.getAllAt(pmHisto.assujettissementsLIFD, periode);
		this.autresForsFiscaux = RangeHelper.getAllAt(pmHisto.autresForsFiscaux, periode);
		this.capitaux = RangeHelper.getAllAt(pmHisto.capitaux, periode);
		this.declarations = RangeHelper.getAllAt(pmHisto.declarations, periode);
		this.etats = RangeHelper.getAllAt(pmHisto.etats, periode);
		this.forsFiscauxPrincipaux = RangeHelper.getAllAt(pmHisto.forsFiscauxPrincipaux, periode);
		this.forsGestions = RangeHelper.getAllAt(pmHisto.forsGestions, periode);
		this.formesJuridiques = RangeHelper.getAllAt(pmHisto.formesJuridiques, periode);
		this.periodesImposition = RangeHelper.getAllAt(pmHisto.periodesImposition, periode);
		this.regimesFiscauxIFD = RangeHelper.getAllAt(pmHisto.regimesFiscauxIFD, periode);
		this.regimesFiscauxICC = RangeHelper.getAllAt(pmHisto.regimesFiscauxICC, periode);
		this.sieges = RangeHelper.getAllAt(pmHisto.sieges, periode);
	}

	private List<CompteBancaire> calculateComptesBancaires(ch.vd.uniregctb.interfaces.model.PersonneMorale pmHost, Context context) {

		// on tient du compte du compte bancaire de la PM elle-même
		List<CompteBancaire> list = comptes2web(pmHost.getComptesBancaires());

		// [UNIREG-2106] on ajoute les comptes bancaires des mandats de type 'T'
		final List<Mandat> mandats = pmHost.getMandats();
		if (mandats != null) {
			for (Mandat m : mandats) {
				if (m.getCode().equals("T")) { // on ignore tous les autres types de mandataire

					CompteBancaire cb = new CompteBancaire();
					cb.numeroTiersTitulaire = m.getNumeroMandataire();

					// on rempli les informations à partir du mandataire
					fillCompteBancaireDepuisMandataire(cb, m, context);

					// on surcharge les informations à partir du mandat, si nécessaire
					fillCompteBancaireDepuisMandat(cb, m, context.infraService);

					if (list == null) {
						list = new ArrayList<CompteBancaire>();
					}
					list.add(cb);
				}
			}
		}

		return list;
	}

	private static void fillCompteBancaireDepuisMandataire(CompteBancaire cb, Mandat m, Context context) {
		switch (m.getTypeMandataire()) {
		case INDIVIDU:
			fillCompteBancaireDepuisMandataireIndividu(cb, m.getNumeroMandataire(), context.serviceCivilService);
			break;
		case PERSONNE_MORALE:
			fillCompteBancaireDepuisMandatairePersonneMorale(cb, m.getNumeroMandataire(), context.servicePM);
			break;
		case ETABLISSEMENT:
			fillCompteBancaireDepuisMandataireEtablissement(cb, m.getNumeroMandataire(), context.servicePM);
			break;
		default:
			throw new IllegalArgumentException("Type de mandataire inconnu =[" + m.getTypeMandataire() + ']');
		}
	}

	/**
	 * Renseigne les numéros de comptes (et les informations y relatives) à partir des valeurs spécifiées.<p> Si plusieurs types de comptes sont spécifiés (IBAN + CCP, par exemple), cette méthode utilise
	 * l'ordre de priorité suivant : l'IBAN, puis le compte bancaire et enfin le compte CCP. Si aucun type de compte n'est spécifié, cette méthode ne fait rien.
	 *
	 * @param cb              le compte bancaire à remplir
	 * @param iban            un numéro de compte au format IBAN
	 * @param comptesBancaire un numéro au format bancaire (banque suisse)
	 * @param ccp             un numéro de compte au format CCP (poste suisse)
	 * @param bicSwift        le code bic swift
	 * @param nomInstitution  le nom de l'institution financière
	 */
	private static void fillCompteBancaire(CompteBancaire cb, String iban, String comptesBancaire, String ccp, String bicSwift, String nomInstitution) {
		if (iban != null) {
			cb.numero = iban;
			cb.format = CompteBancaire.Format.IBAN;
			cb.adresseBicSwift = bicSwift;
			cb.nomInstitution = nomInstitution;
		}
		else if (comptesBancaire != null) {
			cb.numero = comptesBancaire;
			cb.format = CompteBancaire.Format.SPECIFIQUE_CH;
			cb.adresseBicSwift = bicSwift;
			cb.nomInstitution = nomInstitution;
		}
		else if (ccp != null) {
			cb.numero = ccp;
			cb.format = CompteBancaire.Format.SPECIFIQUE_CH;
			cb.adresseBicSwift = bicSwift;
			cb.nomInstitution = nomInstitution;
		}
	}

	private static void fillCompteBancaireDepuisMandataireEtablissement(CompteBancaire cb, long noEtablissement, ServicePersonneMoraleService servicePM) {
		final Etablissement etablissement = servicePM.getEtablissement(noEtablissement);
		if (etablissement != null) {
			cb.titulaire = etablissement.getEnseigne();
			fillCompteBancaire(cb, etablissement.getIBAN(), etablissement.getCompteBancaire(), etablissement.getCCP(), etablissement.getBicSwift(), etablissement.getNomInstitutionFinanciere());
		}
	}

	private static void fillCompteBancaireDepuisMandatairePersonneMorale(CompteBancaire cb, long noPM, ServicePersonneMoraleService servicePM) {
		final ch.vd.uniregctb.interfaces.model.PersonneMorale pm = servicePM.getPersonneMorale(noPM, (PartPM[]) null);
		if (pm != null) {
			cb.titulaire = pm.getRaisonSociale();

			final List<ch.vd.uniregctb.interfaces.model.CompteBancaire> cpm = pm.getComptesBancaires();
			if (cpm != null && !cpm.isEmpty()) {
				final ch.vd.uniregctb.interfaces.model.CompteBancaire c = cpm.get(0); // faut-il vraiment toujours le premier ?
				cb.format = CompteBancaire.Format.valueOf(c.getFormat().name());
				cb.numero = c.getNumero();
				cb.nomInstitution = c.getNomInstitution();
			}
		}
	}

	private static void fillCompteBancaireDepuisMandataireIndividu(CompteBancaire cb, long noIndividu, ServiceCivilService serviceCivil) {
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
		if (individu != null) {
			cb.titulaire = serviceCivil.getNomPrenom(individu);
			// aucune information de compte bancaire sur un individu...
		}
	}

	private void fillCompteBancaireDepuisMandat(CompteBancaire cb, Mandat m, ServiceInfrastructureService serviceInfra) {
		final String nomInstitution = getNomInstitution(m.getNumeroInstitutionFinanciere(), serviceInfra);

		fillCompteBancaire(cb, m.getIBAN(), m.getCompteBancaire(), m.getCCP(), m.getBicSwift(), nomInstitution);
	}

	private String getNomInstitution(Long noInstit, ServiceInfrastructureService serviceInfra) {

		if (noInstit == null) {
			return null;
		}

		final InstitutionFinanciere instit;
		try {
			instit = serviceInfra.getInstitutionFinanciere(noInstit.intValue());
		}
		catch (ServiceInfrastructureException e) {
			throw new RuntimeException("L'institution financière avec le numéro = [" + noInstit + "] n'existe pas.");
		}

		return instit.getNomInstitutionFinanciere();
	}

	private static List<CompteBancaire> comptes2web(List<ch.vd.uniregctb.interfaces.model.CompteBancaire> comptes) {
		if (comptes == null || comptes.isEmpty()) {
			return null;
		}
		final List<CompteBancaire> list = new ArrayList<CompteBancaire>();
		for (ch.vd.uniregctb.interfaces.model.CompteBancaire c : comptes) {
			CompteBancaire compte = new CompteBancaire();
			compte.format = CompteBancaire.Format.valueOf(c.getFormat().name());
			compte.numero = c.getNumero();
			compte.nomInstitution = c.getNomInstitution();
			list.add(compte);
		}
		return list;
	}

	private List<Siege> sieges2web(List<ch.vd.uniregctb.interfaces.model.Siege> sieges, Context context) {
		if (sieges == null || sieges.isEmpty()) {
			return null;
		}
		final ArrayList<Siege> list = new ArrayList<Siege>(sieges.size());
		for (ch.vd.uniregctb.interfaces.model.Siege s : sieges) {
			list.add(host2web(s, context));
		}
		return list;
	}

	private Siege host2web(ch.vd.uniregctb.interfaces.model.Siege s, Context context) {
		Assert.notNull(s);
		Siege siege = new Siege();
		siege.dateDebut = DataHelper.coreToWeb(s.getDateDebut());
		siege.dateFin = DataHelper.coreToWeb(s.getDateFin());
		siege.noOfsSiege = context.noOfsTranslator.translateCommune(s.getNoOfsSiege());
		siege.typeSiege = typeSiege2web(s.getType());
		return siege;
	}

	private static Siege.TypeSiege typeSiege2web(TypeNoOfs type) {
		switch (type) {
		case COMMUNE_CH:
			return Siege.TypeSiege.COMMUNE_CH;
		case PAYS_HS:
			return Siege.TypeSiege.PAYS_HS;
		default:
			throw new IllegalArgumentException("Type de no Ofs inconnu = [" + type + ']');
		}
	}

	private static List<RegimeFiscal> regimes2web(List<ch.vd.uniregctb.interfaces.model.RegimeFiscal> regimes) {
		if (regimes == null || regimes.isEmpty()) {
			return null;
		}
		final ArrayList<RegimeFiscal> list = new ArrayList<RegimeFiscal>(regimes.size());
		for (ch.vd.uniregctb.interfaces.model.RegimeFiscal r : regimes) {
			list.add(host2web(r));
		}
		return list;
	}

	private static RegimeFiscal host2web(ch.vd.uniregctb.interfaces.model.RegimeFiscal r) {
		Assert.notNull(r);
		RegimeFiscal regime = new RegimeFiscal();
		regime.dateDebut = DataHelper.coreToWeb(r.getDateDebut());
		regime.dateFin = DataHelper.coreToWeb(r.getDateFin());
		regime.code = r.getCode();
		return regime;
	}

	private List<ForFiscal> forsSecondaires2web(List<ch.vd.uniregctb.interfaces.model.ForPM> fors, Context context) {
		if (fors == null || fors.isEmpty()) {
			return null;
		}
		final ArrayList<ForFiscal> list = new ArrayList<ForFiscal>(fors.size());
		for (ch.vd.uniregctb.interfaces.model.ForPM f : fors) {
			list.add(secondaire2web(f, context));
		}
		return list;
	}

	private ForFiscal secondaire2web(ch.vd.uniregctb.interfaces.model.ForPM f, Context context) {
		Assert.notNull(f);
		ForFiscal ffs = new ForFiscal();
		ffs.dateOuverture = DataHelper.coreToWeb(f.getDateDebut());
		ffs.dateFermeture = DataHelper.coreToWeb(f.getDateFin());
		ffs.genreImpot = ForFiscal.GenreImpot.BENEFICE_CAPITAL;
		ffs.motifRattachement = ForFiscal.MotifRattachement.ETABLISSEMENT_STABLE;
		ffs.noOfsAutoriteFiscale = context.noOfsTranslator.translateCommune(f.getNoOfsAutoriteFiscale());
		ffs.typeAutoriteFiscale = ForFiscal.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD; // par définition
		return ffs;
	}

	private List<ForFiscal> forsPrincipaux2web(List<ch.vd.uniregctb.interfaces.model.ForPM> fors, Context context) {
		if (fors == null || fors.isEmpty()) {
			return null;
		}
		final ArrayList<ForFiscal> list = new ArrayList<ForFiscal>(fors.size());
		for (ch.vd.uniregctb.interfaces.model.ForPM f : fors) {
			list.add(principal2web(f, context));
		}
		return list;
	}

	private ForFiscal principal2web(ch.vd.uniregctb.interfaces.model.ForPM f, Context context) {
		Assert.notNull(f);
		ForFiscal ffp = new ForFiscal();
		ffp.dateOuverture = DataHelper.coreToWeb(f.getDateDebut());
		ffp.dateFermeture = DataHelper.coreToWeb(f.getDateFin());
		ffp.genreImpot = ForFiscal.GenreImpot.BENEFICE_CAPITAL;
		ffp.motifRattachement = ForFiscal.MotifRattachement.DOMICILE;
		ffp.typeAutoriteFiscale = host2web(f.getTypeAutoriteFiscale(), f.getNoOfsAutoriteFiscale(), context.infraService);
		if (ffp.typeAutoriteFiscale != ForFiscal.TypeAutoriteFiscale.PAYS_HS) {
			ffp.noOfsAutoriteFiscale = context.noOfsTranslator.translateCommune(f.getNoOfsAutoriteFiscale());
		}
		else {
			ffp.noOfsAutoriteFiscale = f.getNoOfsAutoriteFiscale();
		}
		return ffp;
	}

	private ForFiscal.TypeAutoriteFiscale host2web(TypeNoOfs type, int noOfs, ServiceInfrastructureService serviceInfra) {
		switch (type) {
		case COMMUNE_CH:
			final Commune commune;
			try {
				commune = serviceInfra.getCommuneByNumeroOfsEtendu(noOfs, null);
			}
			catch (ServiceInfrastructureException e) {
				throw new RuntimeException("Impossible de récupérer la commune avec le numéro Ofs = [" + noOfs + ']', e);
			}
			if (commune == null) {
				throw new RuntimeException("La commune avec le numéro Ofs = [" + noOfs + "] n'existe pas.");
			}
			// [UNIREG-2641] on doit différencier les communes hors-canton des communes vaudoises
			if (ServiceInfrastructureService.SIGLE_CANTON_VD.equals(commune.getSigleCanton())) {
				return ForFiscal.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
			}
			else {
				return ForFiscal.TypeAutoriteFiscale.COMMUNE_HC;
			}
		case PAYS_HS:
			return ForFiscal.TypeAutoriteFiscale.PAYS_HS;
		default:
			throw new IllegalArgumentException("Type de no Ofs inconnu = [" + type + ']');
		}
	}

	private List<FormeJuridique> formes2web(List<ch.vd.uniregctb.interfaces.model.FormeJuridique> formes) {
		if (formes == null || formes.isEmpty()) {
			return null;
		}
		final ArrayList<FormeJuridique> list = new ArrayList<FormeJuridique>(formes.size());
		for (ch.vd.uniregctb.interfaces.model.FormeJuridique f : formes) {
			list.add(host2web(f));
		}
		return list;
	}

	private FormeJuridique host2web(ch.vd.uniregctb.interfaces.model.FormeJuridique f) {
		Assert.notNull(f);
		FormeJuridique forme = new FormeJuridique();
		forme.dateDebut = DataHelper.coreToWeb(f.getDateDebut());
		forme.dateFin = DataHelper.coreToWeb(f.getDateFin());
		forme.code = f.getCode();
		return forme;
	}

	private static List<EtatPM> etats2web(List<ch.vd.uniregctb.interfaces.model.EtatPM> etats) {
		if (etats == null || etats.isEmpty()) {
			return null;
		}
		final ArrayList<EtatPM> list = new ArrayList<EtatPM>(etats.size());
		for (ch.vd.uniregctb.interfaces.model.EtatPM e : etats) {
			list.add(host2web(e));
		}
		return list;
	}

	private static EtatPM host2web(ch.vd.uniregctb.interfaces.model.EtatPM e) {
		Assert.notNull(e);
		EtatPM etat = new EtatPM();
		etat.dateDebut = DataHelper.coreToWeb(e.getDateDebut());
		etat.dateFin = DataHelper.coreToWeb(e.getDateFin());
		etat.code = e.getCode();
		return etat;
	}

	private static List<Capital> capitaux2web(List<ch.vd.uniregctb.interfaces.model.Capital> capitaux) {
		if (capitaux == null || capitaux.isEmpty()) {
			return null;
		}
		final ArrayList<Capital> list = new ArrayList<Capital>(capitaux.size());
		for (ch.vd.uniregctb.interfaces.model.Capital c : capitaux) {
			list.add(DataHelper.host2web(c));
		}
		return list;
	}

	private static List<Assujettissement> assujettissements2web(List<ch.vd.uniregctb.interfaces.model.AssujettissementPM> lic) {
		if (lic == null || lic.isEmpty()) {
			return null;
		}
		final ArrayList<Assujettissement> list = new ArrayList<Assujettissement>(lic.size());
		for (ch.vd.uniregctb.interfaces.model.AssujettissementPM a : lic) {
			list.add(DataHelper.host2web(a));
		}
		return list;
	}

	private static PartPM[] web2business(Set<TiersPart> parts) {

		if (parts == null || parts.isEmpty()) {
			return null;
		}

		final Set<PartPM> set = new HashSet<PartPM>();
		if (parts.contains(TiersPart.ADRESSES) || parts.contains(TiersPart.ADRESSES_ENVOI)) {
			set.add(PartPM.ADRESSES);
		}
		if (parts.contains(TiersPart.ASSUJETTISSEMENTS)) {
			set.add(PartPM.ASSUJETTISSEMENTS);
		}
		if (parts.contains(TiersPart.CAPITAUX)) {
			set.add(PartPM.CAPITAUX);
		}
		if (parts.contains(TiersPart.COMPTES_BANCAIRES)) {
			set.add(PartPM.MANDATS);
		}
		if (parts.contains(TiersPart.ETATS_PM)) {
			set.add(PartPM.ETATS);
		}
		if (parts.contains(TiersPart.FORMES_JURIDIQUES)) {
			set.add(PartPM.FORMES_JURIDIQUES);
		}
		if (parts.contains(TiersPart.FORS_FISCAUX) || parts.contains(TiersPart.FORS_FISCAUX_VIRTUELS) || parts.contains(TiersPart.FORS_GESTION)) {
			set.add(PartPM.FORS_FISCAUX);
		}
		if (parts.contains(TiersPart.REGIMES_FISCAUX)) {
			set.add(PartPM.REGIMES_FISCAUX);
		}
		if (parts.contains(TiersPart.SIEGES)) {
			set.add(PartPM.SIEGES);
		}

		return set.toArray(new PartPM[set.size()]);
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
