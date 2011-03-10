package ch.vd.uniregctb.webservices.tiers2.mock;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.webservices.tiers2.TiersWebService;
import ch.vd.uniregctb.webservices.tiers2.data.Adresse;
import ch.vd.uniregctb.webservices.tiers2.data.AdresseEnvoi;
import ch.vd.uniregctb.webservices.tiers2.data.Assujettissement;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiers;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersEntry;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHistoEntry;
import ch.vd.uniregctb.webservices.tiers2.data.Capital;
import ch.vd.uniregctb.webservices.tiers2.data.CompteBancaire;
import ch.vd.uniregctb.webservices.tiers2.data.Date;
import ch.vd.uniregctb.webservices.tiers2.data.DebiteurInfo;
import ch.vd.uniregctb.webservices.tiers2.data.EtatPM;
import ch.vd.uniregctb.webservices.tiers2.data.EvenementPM;
import ch.vd.uniregctb.webservices.tiers2.data.ForFiscal;
import ch.vd.uniregctb.webservices.tiers2.data.FormeJuridique;
import ch.vd.uniregctb.webservices.tiers2.data.PersonneMorale;
import ch.vd.uniregctb.webservices.tiers2.data.PersonneMoraleHisto;
import ch.vd.uniregctb.webservices.tiers2.data.Range;
import ch.vd.uniregctb.webservices.tiers2.data.RegimeFiscal;
import ch.vd.uniregctb.webservices.tiers2.data.ReponseQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers2.data.Siege;
import ch.vd.uniregctb.webservices.tiers2.data.Tiers;
import ch.vd.uniregctb.webservices.tiers2.data.TiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.TiersInfo;
import ch.vd.uniregctb.webservices.tiers2.exception.AccessDeniedException;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.exception.TechnicalException;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.params.AllConcreteTiersClasses;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetDebiteurInfo;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersPeriode;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersType;
import ch.vd.uniregctb.webservices.tiers2.params.QuittancerDeclarations;
import ch.vd.uniregctb.webservices.tiers2.params.SearchEvenementsPM;
import ch.vd.uniregctb.webservices.tiers2.params.SearchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.SetTiersBlocRembAuto;

/**
 * Classe qui retourne des données bouchonnées concernant les personnes morales. Les appels concernant les personnes physiques sont simplement délégués plus loin.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class TiersWebServiceMockPM implements TiersWebService, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(TiersWebServiceMockPM.class);

	private TiersWebService target;
	private String adressesPMCsvFile;
	private String assujettissementsPMCsvFile;
	private String capitauxPMCsvFile;
	private String etatsPMCsvFile;
	private String formesPMCsvFile;
	private String forsPrincipauxPMCsvFile;
	private String forsSecondairesPMCsvFile;
	private String pmsCsvFile;
	private String regimesVDPMCsvFile;
	private String regimesCHPMCsvFile;
	private String siegesPMCsvFile;
	private String evenementsPMCsvFile;

	private ServiceInfrastructureService serviceInfra;

	/**
	 * Toutes les personnes morales chargées à partir des fichiers CSV
	 */
	private Map<Long, PersonneMoraleHisto> pmsHisto = new HashMap<Long, PersonneMoraleHisto>();

	/**
	 * Tous les événements des personnes morales chargées à parti des fichiers CSV
	 */
	private List<EvenementPM> pmEvents;

	public void setTarget(TiersWebService target) {
		this.target = target;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public void setAdressesPMCsvFile(String adressesPMCsvFile) {
		this.adressesPMCsvFile = adressesPMCsvFile;
	}

	public void setAssujettissementsPMCsvFile(String assujettissementsPMCsvFile) {
		this.assujettissementsPMCsvFile = assujettissementsPMCsvFile;
	}

	public void setCapitauxPMCsvFile(String capitauxPMCsvFile) {
		this.capitauxPMCsvFile = capitauxPMCsvFile;
	}

	public void setEtatsPMCsvFile(String etatsPMCsvFile) {
		this.etatsPMCsvFile = etatsPMCsvFile;
	}

	public void setFormesPMCsvFile(String formesPMCsvFile) {
		this.formesPMCsvFile = formesPMCsvFile;
	}

	public void setForsPrincipauxPMCsvFile(String forsPrincipauxPMCsvFile) {
		this.forsPrincipauxPMCsvFile = forsPrincipauxPMCsvFile;
	}

	public void setForsSecondairesPMCsvFile(String forsSecondairesPMCsvFile) {
		this.forsSecondairesPMCsvFile = forsSecondairesPMCsvFile;
	}

	public void setPmsCsvFile(String pmsCsvFile) {
		this.pmsCsvFile = pmsCsvFile;
	}

	public void setRegimesVDPMCsvFile(String regimesVDPMCsvFile) {
		this.regimesVDPMCsvFile = regimesVDPMCsvFile;
	}

	public void setRegimesCHPMCsvFile(String regimesCHPMCsvFile) {
		this.regimesCHPMCsvFile = regimesCHPMCsvFile;
	}

	public void setSiegesPMCsvFile(String siegesPMCsvFile) {
		this.siegesPMCsvFile = siegesPMCsvFile;
	}

	public void setEvenementsPMCsvFile(String evenementsPMCsvFile) {
		this.evenementsPMCsvFile = evenementsPMCsvFile;
	}

	public List<TiersInfo> searchTiers(SearchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		return target.searchTiers(params);
	}

	public Tiers.Type getTiersType(GetTiersType params) throws BusinessException, AccessDeniedException, TechnicalException {
		if (isEntreprise(params.tiersNumber)) {
			realLifeFactor();
			return pmsHisto.containsKey(params.tiersNumber) ? Tiers.Type.PERSONNE_MORALE : null;
		}
		else {
			return target.getTiersType(params);
		}
	}

	public Tiers getTiers(GetTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		if (isEntreprise(params.tiersNumber)) {
			realLifeFactor();
			final PersonneMoraleHisto pmHisto = pmsHisto.get(params.tiersNumber);
			PersonneMorale pm = null;
			if (pmHisto != null) {
				pm = getAt(pmHisto, params.date);
				pm = (PersonneMorale) pm.clone(params.parts);
			}
			return pm;
		}
		else {
			return target.getTiers(params);
		}
	}

	public TiersHisto getTiersPeriode(GetTiersPeriode params) throws BusinessException, AccessDeniedException, TechnicalException {
		if (isEntreprise(params.tiersNumber)) {
			realLifeFactor();
			final PersonneMoraleHisto pm = pmsHisto.get(params.tiersNumber);
			return pm == null ? null : pm.clone(params.parts);
		}
		else {
			return target.getTiersPeriode(params);
		}
	}

	public TiersHisto getTiersHisto(GetTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {
		if (isEntreprise(params.tiersNumber)) {
			realLifeFactor();
			final PersonneMoraleHisto pm = pmsHisto.get(params.tiersNumber);
			return pm == null ? null : pm.clone(params.parts);
		}
		else {
			return target.getTiersHisto(params);
		}
	}

	public BatchTiers getBatchTiers(GetBatchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {

		// sépare les ids PP des ids PM
		final Set<Long> idsPP = new HashSet<Long>();
		final Set<Long> idsPM = new HashSet<Long>();
		if (params.tiersNumbers != null) {
			for (Long id : params.tiersNumbers) {
				if (isEntreprise(id)) {
					idsPM.add(id);
				}
				else {
					idsPP.add(id);
				}

			}
		}

		BatchTiers batchPP = null;

		// récupère les tiers PP
		if (!idsPP.isEmpty()) {
			GetBatchTiers paramsPP = new GetBatchTiers();
			paramsPP.tiersNumbers = idsPP;
			paramsPP.date = params.date;
			paramsPP.login = params.login;
			paramsPP.parts = params.parts;
			batchPP = target.getBatchTiers(paramsPP);
		}

		BatchTiers batchPM = null;

		// récupère les tiers PM
		if (!idsPM.isEmpty()) {
			realLifeFactorBatch(idsPM.size());
			batchPM = new BatchTiers();
			for (Long id : idsPM) {
				PersonneMoraleHisto pmHisto = pmsHisto.get(id);
				if (pmHisto != null) {
					final PersonneMorale pm = (PersonneMorale) getAt(pmHisto, params.date).clone(params.parts);
					final BatchTiersEntry entry = new BatchTiersEntry();
					entry.number = id;
					entry.tiers = pm;
					batchPM.entries.add(entry);
				}
			}
		}

		// fusion des données PP et PM si nécessaire
		BatchTiers batch;
		if (batchPP != null && batchPM == null) {
			batch = batchPP;
		}
		else if (batchPP == null && batchPM != null) {
			batch = batchPM;
		}
		else if (batchPP != null) {
			batchPP.entries.addAll(batchPP.entries);
			batch = batchPP;
		}
		else {
			batch = new BatchTiers();
		}

		return batch;
	}

	public BatchTiersHisto getBatchTiersHisto(GetBatchTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {

		// sépare les ids PP des ids PM
		final Set<Long> idsPP = new HashSet<Long>();
		final Set<Long> idsPM = new HashSet<Long>();
		if (params.tiersNumbers != null) {
			for (Long id : params.tiersNumbers) {
				if (isEntreprise(id)) {
					idsPM.add(id);
				}
				else {
					idsPP.add(id);
				}

			}
		}

		BatchTiersHisto batchPP = null;

		// récupère les tiers PP
		if (!idsPP.isEmpty()) {
			GetBatchTiersHisto paramsPP = new GetBatchTiersHisto();
			paramsPP.tiersNumbers = idsPP;
			paramsPP.login = params.login;
			paramsPP.parts = params.parts;
			batchPP = target.getBatchTiersHisto(paramsPP);
		}

		BatchTiersHisto batchPM = null;

		// récupère les tiers PM
		if (!idsPM.isEmpty()) {
			realLifeFactorBatch(idsPM.size());
			batchPM = new BatchTiersHisto();
			for (Long id : idsPM) {
				PersonneMoraleHisto pmHisto = pmsHisto.get(id);
				if (pmHisto != null) {
					final PersonneMoraleHisto pm = (PersonneMoraleHisto) pmHisto.clone(params.parts);
					final BatchTiersHistoEntry entry = new BatchTiersHistoEntry();
					entry.number = id;
					entry.tiers = pm;
					batchPM.entries.add(entry);
				}
			}
		}

		// fusion des données PP et PM si nécessaire
		BatchTiersHisto batch;
		if (batchPP != null && batchPM == null) {
			batch = batchPP;
		}
		else if (batchPP == null && batchPM != null) {
			batch = batchPM;
		}
		else if (batchPP != null) {
			batchPP.entries.addAll(batchPP.entries);
			batch = batchPP;
		}
		else {
			batch = new BatchTiersHisto();
		}

		return batch;
	}

	public void setTiersBlocRembAuto(SetTiersBlocRembAuto params) throws BusinessException, AccessDeniedException, TechnicalException {
		target.setTiersBlocRembAuto(params);
	}

	@SuppressWarnings({"unchecked"})
	public List<EvenementPM> searchEvenementsPM(final SearchEvenementsPM params) throws BusinessException, AccessDeniedException, TechnicalException {

		List<EvenementPM> events = new LinkedList<EvenementPM>();

		// extrait les événements de la collection en fonction des critères demandés

		for (EvenementPM e : pmEvents) {

			if (params.tiersNumber != null && !e.tiersNumber.equals(params.tiersNumber)) {
				continue;
			}

			if (StringUtils.isNotEmpty(params.codeEvenement) && !e.codeEvenement.equals(params.codeEvenement)) {
				continue;
			}

			final RegDate dateEvenement = Date.asRegDate(e.dateEvenement);
			if (dateEvenement != null) {
				final RegDate dateMini = Date.asRegDate(params.dateMinimale);
				final RegDate dateMaxi = Date.asRegDate(params.dateMaximale);

				if (dateMini != null && dateEvenement.isBefore(dateMini)) {
					continue;
				}
				if (dateMaxi != null && dateEvenement.isAfter(dateMaxi)) {
					continue;
				}
			}

			events.add(e);
		}

		sortEvents(events);
		realLifeFactorBatch(events.size());

		return events;
	}

	public DebiteurInfo getDebiteurInfo(GetDebiteurInfo params) throws
			BusinessException, AccessDeniedException, TechnicalException {
		return target.getDebiteurInfo(params);
	}

	public List<ReponseQuittancementDeclaration> quittancerDeclarations(QuittancerDeclarations params) throws BusinessException, AccessDeniedException, TechnicalException {
		return target.quittancerDeclarations(params);
	}

	private void sortEvents(List<EvenementPM> events) {
		Collections.sort(events, new Comparator<EvenementPM>() {
			public int compare(EvenementPM o1, EvenementPM o2) {
				int c = o1.tiersNumber.compareTo(o2.tiersNumber);
				if (c != 0) {
					return c;
				}
				c = o1.dateEvenement.compareTo(o2.dateEvenement);
				if (c != 0) {
					return c;
				}
				return o1.codeEvenement.compareTo(o2.codeEvenement);
			}
		});
	}

	/**
	 * Vérifie l'existence d'une personne morale.
	 *
	 * @param id l'id de la personne morale.
	 * @return <i>vrai</i> si la personne morale existe; <i>faux</i> autrement.
	 */
	public boolean existsPM(Long id) {
		return pmsHisto.containsKey(id);
	}

	/**
	 * @return la liste de tous les ids des PM existantes.
	 */
	public List<Long> getAllIdsPM() {
		return new ArrayList<Long>(pmsHisto.keySet());
	}

	public void doNothing(AllConcreteTiersClasses dummy) {
	}

	/**
	 * @param tiersNumber un numéro de tiers
	 * @return <i>vrai</i> si le numéro de tiers spécifié est une entreprise.
	 */
	private static boolean isEntreprise(long tiersNumber) {
		return (Entreprise.FIRST_ID <= tiersNumber && tiersNumber <= Entreprise.LAST_ID);
	}

	public void afterPropertiesSet() throws Exception {

		LOGGER.info("Initialisation du mock...");

		final Map<Long, List<Adresse>> adressesCourrier = loadAdressesPM("C"); // C =  Courrier
		final Map<Long, List<Adresse>> adressesDomicile = loadAdressesPM("S"); // S = Siège
		final Map<Long, List<Assujettissement>> assujettissementsLIC = loadAssujettissements(AssujettissementType.LIC);
		final Map<Long, List<Assujettissement>> assujettissementsLIFD = loadAssujettissements(AssujettissementType.LIFD);
		final Map<Long, List<Capital>> capitaux = loadCapitauxPM();
		final Map<Long, List<EtatPM>> etats = loadEtatsPM();
		final Map<Long, List<FormeJuridique>> formes = loadFormesJuridiquesPM();
		final Map<Long, List<ForFiscal>> forsPrincipaux = loadForsPrincipauxPM();
		final Map<Long, List<ForFiscal>> forsSecondaires = loadForsSecondairesPM();
		final Map<Long, List<RegimeFiscal>> regimesVD = loadRegimesFiscauxVDPM();
		final Map<Long, List<RegimeFiscal>> regimesCH = loadRegimesFiscauxCHPM();
		final Map<Long, List<Siege>> sieges = loadSiegesPM();

		final List<PersonneMoraleHisto> pms = loadPMs();

		for (PersonneMoraleHisto pm : pms) {
			// associe chacune des PM avec ses données
			pm.adressesCourrier = adressesCourrier.get(pm.numero);
			pm.adressesRepresentation = pm.adressesCourrier;
			pm.adressesDomicile = adressesDomicile.get(pm.numero);
			// [UNIREG-1808] les adresses de poursuite des PMs sont déterminées à partir des adresses siège, en attendant des évolutions dans le host.
			pm.adressesPoursuite = pm.adressesDomicile;
			pm.adresseEnvoi = calculateAdresseEnvoi(pm, pm.adressesCourrier);
			pm.adresseDomicileFormattee = calculateAdresseEnvoi(pm, pm.adressesDomicile);
			pm.adresseRepresentationFormattee = calculateAdresseEnvoi(pm, pm.adressesRepresentation);
			pm.adressePoursuiteFormattee = calculateAdresseEnvoi(pm, pm.adressesPoursuite);
			pm.assujettissementsLIC = assujettissementsLIC.get(pm.numero);
			pm.assujettissementsLIFD = assujettissementsLIFD.get(pm.numero);
			pm.capitaux = capitaux.get(pm.numero);
			pm.etats = etats.get(pm.numero);
			pm.formesJuridiques = formes.get(pm.numero);
			pm.forsFiscauxPrincipaux = forsPrincipaux.get(pm.numero);
			pm.autresForsFiscaux = forsSecondaires.get(pm.numero);
			pm.regimesFiscauxICC = regimesVD.get(pm.numero);
			pm.regimesFiscauxIFD = regimesCH.get(pm.numero);
			pm.sieges = sieges.get(pm.numero);

			// stocke les données
			pmsHisto.put(pm.numero, pm);
		}

		pmEvents = loadEvenementsPM();

		LOGGER.info("Terminé.");
	}

	private AdresseEnvoi calculateAdresseEnvoi(PersonneMoraleHisto pm, List<Adresse> adresses) {
		AdresseEnvoiDetaillee adresse = new AdresseEnvoiDetaillee(AdresseGenerique.SourceType.PM);

		if (pm.designationAbregee != null) {
			adresse.addNomPrenom(pm.designationAbregee);
		}

		if (pm.personneContact != null) {
			adresse.addPourAdresse(pm.personneContact);
		}

		if (pm.complementNom != null) {
			adresse.addComplement(pm.complementNom);
		}

		final Adresse adresseFiscale = getAt(adresses, null);
		if (adresseFiscale != null) {

			if (adresseFiscale.titre != null) {
				adresse.addPourAdresse(adresseFiscale.titre);
			}

			if (adresseFiscale.rue != null) {
				final String rueNumero;
				if (adresseFiscale.numeroRue != null) {
					rueNumero = adresseFiscale.rue + " " + adresseFiscale.numeroRue;
				}
				else {
					rueNumero = adresseFiscale.rue;
				}
				adresse.addRueEtNumero(rueNumero);
			}

			final String npaLocalite;
			if (adresseFiscale.numeroPostal != null) {
				npaLocalite = adresseFiscale.numeroPostal + " " + adresseFiscale.localite;
			}
			else {
				npaLocalite = adresseFiscale.localite;
			}
			adresse.addNpaEtLocalite(npaLocalite);

			if (adresseFiscale.pays != null) {
				final Integer noOfsPays = (adresseFiscale.noPays == null ? ServiceInfrastructureService.noOfsSuisse : adresseFiscale.noPays);
				adresse.addPays(adresseFiscale.pays, serviceInfra.getTypeAffranchissement(noOfsPays));
			}
		}

		return new AdresseEnvoi(adresse);
	}

	private List<PersonneMoraleHisto> loadPMs() throws IOException {

		final List<PersonneMoraleHisto> list = new ArrayList<PersonneMoraleHisto>();

		// 0             1            2      3      4                  5    6         7              8                9             10                     11                     12                     13                     14             15
		// NO_ENTREPRISE;NO_TELEPHONE;NO_FAX;NO_CCP;NO_COMPTE_BANCAIRE;IBAN;BIC_SWIFT;NOM_INSTIT_FIN;PRENOM_CONTACT_1;NOM_CONTACT_1;DESIGN_ABREGEE        ;RAISON_SOC_LGN1       ;RAISON_SOC_LGN2       ;RAISON_SOC_LGN3       ;DA_BOUCL_FUTUR;NO_IPMRO
		// 54020        ;            ;      ;      ;                  ;    ;         ;              :                ;             ;KEPLER HOLDING        ;Kepler Holding SA     ;(Kepler Holding AG)   ;(Kepler Holding Ltd)  ;2009-12-31    ;
		// 51477        ;            ;      ;      ;                  ;    ;         ;              ;                ;             ;ARGOT LAB PATHOLOGIE  ;ARGOT Lab pathologie  ;oculaire SA           ;                      ;2009-12-31    ;
		// 51277        ;            ;      ;      ;                  ;    ;         ;              ;                ;             ;AVENIR TOITURE        ;Avenir toiture Sàrl   ;                      ;                      ;2009-12-31    ;
		// 50034        ;            ;      ;      ;                  ;    ;         ;              ;                ;             ;SCENE CONCEPT         ;SCENE CONCEPT Sàrl    ;                      ;                      ;2009-12-31    ;
		// 44077        ;            ;      ;      ;243-485733.01J    ;    ;         ;UBS SA        ;                ;             ;AU CAVALIER           ;Au Cavalier Sàrl      ;                      ;                      ;2009-12-31    ;
		CSVReader reader = new CSVReader(new FileReader(ResourceUtils.getFile(pmsCsvFile)), ';', '"', 1);
		String[] next;

		while ((next = reader.readNext()) != null) {

			final PersonneMoraleHisto pm = new PersonneMoraleHisto();
			pm.numero = string2Long(next[0]);
			pm.numeroTelProf = string2String(next[1]);
			pm.numeroTelecopie = string2String(next[2]);

			final String ccp = string2String(next[3]);
			if (ccp != null) {
				CompteBancaire compte = new CompteBancaire();
				compte.format = CompteBancaire.Format.SPECIFIQUE_CH;
				compte.numero = ccp;
				compte.nomInstitution = "La Poste Suisse";
				if (pm.comptesBancaires == null) {
					pm.comptesBancaires = new ArrayList<CompteBancaire>();
				}
				pm.comptesBancaires.add(compte);
			}

			final String cb = string2String(next[4]);
			if (cb != null) {
				CompteBancaire compte = new CompteBancaire();
				compte.format = CompteBancaire.Format.SPECIFIQUE_CH;
				compte.numero = cb;
				compte.nomInstitution = string2String(next[7]);
				if (pm.comptesBancaires == null) {
					pm.comptesBancaires = new ArrayList<CompteBancaire>();
				}
				pm.comptesBancaires.add(compte);
			}

			final String iban = string2String(next[5]);
			if (iban != null) {
				CompteBancaire compte = new CompteBancaire();
				compte.format = CompteBancaire.Format.IBAN;
				compte.numero = iban;
				if (pm.comptesBancaires == null) {
					pm.comptesBancaires = new ArrayList<CompteBancaire>();
				}
				pm.comptesBancaires.add(compte);
			}

			final String prenom = string2String(next[8]);
			final String nom = string2String(next[9]);
			if (prenom != null && nom != null) {
				pm.personneContact = prenom + " " + nom;
			}

			pm.designationAbregee = string2String(next[10]);
			pm.raisonSociale1 = string2String(next[11]);
			pm.raisonSociale2 = string2String(next[12]);
			pm.raisonSociale3 = string2String(next[13]);
			pm.dateBouclementFutur = DataHelper.coreToWeb(next[14]);
			pm.numeroIPMRO = string2String(next[15]);

			list.add(pm);
		}

		return list;
	}

	private Map<Long, List<Siege>> loadSiegesPM() throws IOException {

		final Map<Long, List<Siege>> sieges = new HashMap<Long, List<Siege>>();

		//NO_SEQUENCE;DA_VALIDITE;FK_COMMUNENO;FK_PAYSNO;FK_ENTPRNO
		//1;1988-05-02;5938;;5537
		//2;1994-03-22;5516;;8270
		//1;1990-02-05;5523;;8270
		//2;1994-09-02;6608;;15346
		CSVReader reader = new CSVReader(new FileReader(ResourceUtils.getFile(siegesPMCsvFile)), ';', '"', 1);
		String[] next;

		while ((next = reader.readNext()) != null) {

			final Siege siege = new Siege();
			siege.dateDebut = DataHelper.coreToWeb(next[1]);

			final Integer noOfsCommune = string2Integer(next[2]);
			if (noOfsCommune != null) {
				siege.noOfsSiege = noOfsCommune;
				siege.typeSiege = Siege.TypeSiege.COMMUNE_CH;
			}
			else {
				siege.noOfsSiege = string2Integer(next[3]);
				siege.typeSiege = Siege.TypeSiege.PAYS_HS;
			}

			final Long idPM = string2Long(next[4]);
			List<Siege> list = sieges.get(idPM);
			if (list == null) {
				list = new ArrayList<Siege>();
				sieges.put(idPM, list);
			}
			list.add(siege);
		}

		for (List<Siege> list : sieges.values()) {
			sortRanges(list);
			calculateDatesFin(list);
		}

		return sieges;
	}

	private Map<Long, List<RegimeFiscal>> loadRegimesFiscauxCHPM() throws IOException {

		final Map<Long, List<RegimeFiscal>> regimes = new HashMap<Long, List<RegimeFiscal>>();

		//NO_SEQUENCE;DAD;FK_TYREGFISCO;FK_ENTPRNO
		//1;1993-01-01;01;5537
		//1;1993-01-01;01;8270
		//1;1993-01-01;01;16493
		//1;1995-03-20;01;23327
		CSVReader reader = new CSVReader(new FileReader(ResourceUtils.getFile(regimesCHPMCsvFile)), ';', '"', 1);
		String[] next;

		while ((next = reader.readNext()) != null) {

			final RegimeFiscal regime = new RegimeFiscal();
			regime.dateDebut = DataHelper.coreToWeb(next[1]);
			regime.code = string2String(next[2]);

			final Long idPM = string2Long(next[3]);
			List<RegimeFiscal> list = regimes.get(idPM);
			if (list == null) {
				list = new ArrayList<RegimeFiscal>();
				regimes.put(idPM, list);
			}
			list.add(regime);
		}

		for (List<RegimeFiscal> list : regimes.values()) {
			sortRanges(list);
			calculateDatesFin(list);
		}

		return regimes;
	}

	private Map<Long, List<RegimeFiscal>> loadRegimesFiscauxVDPM() throws IOException {

		final Map<Long, List<RegimeFiscal>> regimes = new HashMap<Long, List<RegimeFiscal>>();

		//NO_SEQUENCE;DAD;FK_TYREGFISCO;FK_ENTPRNO
		//1;1993-01-01;01;5537
		//1;1993-01-01;01;8270
		//1;1993-01-01;01;16493
		//1;1995-03-20;01;23327
		CSVReader reader = new CSVReader(new FileReader(ResourceUtils.getFile(regimesVDPMCsvFile)), ';', '"', 1);
		String[] next;

		while ((next = reader.readNext()) != null) {

			final RegimeFiscal regime = new RegimeFiscal();
			regime.dateDebut = DataHelper.coreToWeb(next[1]);
			regime.code = string2String(next[2]);

			final Long idPM = string2Long(next[3]);
			List<RegimeFiscal> list = regimes.get(idPM);
			if (list == null) {
				list = new ArrayList<RegimeFiscal>();
				regimes.put(idPM, list);
			}
			list.add(regime);
		}

		for (List<RegimeFiscal> list : regimes.values()) {
			sortRanges(list);
			calculateDatesFin(list);
		}

		return regimes;
	}

	private Map<Long, List<ForFiscal>> loadForsSecondairesPM() throws IOException {

		final Map<Long, List<ForFiscal>> fors = new HashMap<Long, List<ForFiscal>>();

		//DAD_VALIDITE;DAF_VALIDITE;FK_COMMUNENO;FK_ENTPRNO
		//1988-05-27;0001-01-01;5938;5537
		//1995-06-01;0001-01-01;5568;5537
		//1968-02-26;0001-01-01;5890;15346
		//1936-01-17;0001-01-01;5586;15346
		CSVReader reader = new CSVReader(new FileReader(ResourceUtils.getFile(forsSecondairesPMCsvFile)), ';', '"', 1);
		String[] next;

		while ((next = reader.readNext()) != null) {

			final ForFiscal f = new ForFiscal();
			f.dateOuverture = DataHelper.coreToWeb(next[0]);
			f.dateFermeture = DataHelper.coreToWeb(next[1]);
			f.genreImpot = ForFiscal.GenreImpot.BENEFICE_CAPITAL;
			f.motifRattachement = ForFiscal.MotifRattachement.ETABLISSEMENT_STABLE;
			f.noOfsAutoriteFiscale = Integer.parseInt(next[2]);
			f.typeAutoriteFiscale = ForFiscal.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;

			final Long idPM = string2Long(next[3]);
			List<ForFiscal> list = fors.get(idPM);
			if (list == null) {
				list = new ArrayList<ForFiscal>();
				fors.put(idPM, list);
			}
			list.add(f);
		}

		for (List<ForFiscal> list : fors.values()) {
			sortRanges(list);
		}

		return fors;
	}

	private Map<Long, List<ForFiscal>> loadForsPrincipauxPM() throws IOException {

		final Map<Long, List<ForFiscal>> fors = new HashMap<Long, List<ForFiscal>>();

		//NO_SEQUENCE;DA_VALIDITE;TYPE;FK_COMMUNENO;FK_PAYSNO;FK_ENTPRNO
		//1;1988-05-02;S;5938;;5537
		//2;1994-03-22;S;5516;;8270
		//1;1990-02-05;S;5523;;8270
		//2;1994-09-02;S;6608;;15346
		CSVReader reader = new CSVReader(new FileReader(ResourceUtils.getFile(forsPrincipauxPMCsvFile)), ';', '"', 1);
		String[] next;

		while ((next = reader.readNext()) != null) {

			final ForFiscal f = new ForFiscal();
			f.dateOuverture = DataHelper.coreToWeb(next[1]);
			f.genreImpot = ForFiscal.GenreImpot.BENEFICE_CAPITAL;
			f.motifRattachement = ForFiscal.MotifRattachement.DOMICILE;

			final Integer noOfsCommune = string2Integer(next[3]);
			if (noOfsCommune != null) {
				f.noOfsAutoriteFiscale = noOfsCommune;
				f.typeAutoriteFiscale = ForFiscal.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
			}
			else {
				f.noOfsAutoriteFiscale = Integer.parseInt(next[4]);
				f.typeAutoriteFiscale = ForFiscal.TypeAutoriteFiscale.PAYS_HS;
			}

			final Long idPM = string2Long(next[5]);
			List<ForFiscal> list = fors.get(idPM);
			if (list == null) {
				list = new ArrayList<ForFiscal>();
				fors.put(idPM, list);
			}
			list.add(f);
		}

		for (List<ForFiscal> list : fors.values()) {
			sortRanges(list);
			calculateDatesFin(list);
		}

		return fors;
	}

	private Map<Long, List<Capital>> loadCapitauxPM() throws IOException {

		final Map<Long, List<Capital>> capitaux = new HashMap<Long, List<Capital>>();

		// 0           1                 2              3             4              5               6 
		// NO_SEQUENCE;DA_EVOLUTION_CAP;CAPITAL_ACTION;CAPITAL_LIBERE;FK_P_FOSCANNEE;FK_P_FOSCNO_ANN;FK_ENTPRNO
		// 1          ;1988-05-02      ;250000        ;250000        ;1988          ;114            ;5537
		// 2          ;1997-06-24      ;400000        ;400000        ;1997          ;145            ;5537
		// 1          ;1990-02-05      ;70000         ;70000         ;1990          ;44             ;8270
		// 1          ;1963-12-23      ;250000        ;250000        ;              ;               ;15346
		CSVReader reader = new CSVReader(new FileReader(ResourceUtils.getFile(capitauxPMCsvFile)), ';', '"', 1);
		String[] next;

		while ((next = reader.readNext()) != null) {

			final Capital capital = new Capital();
			capital.dateDebut = DataHelper.coreToWeb(next[1]);
			capital.capitalAction = string2Long(next[2]);
			capital.capitalLibere = string2Long(next[3]);

			final Capital.EditionFosc edition = new Capital.EditionFosc();
			edition.anneeFosc = string2Integer(next[4]);
			edition.noFosc = string2Integer(next[5]);
			capital.editionFosc = edition;

			final Long idPM = string2Long(next[6]);
			List<Capital> list = capitaux.get(idPM);
			if (list == null) {
				list = new ArrayList<Capital>();
				capitaux.put(idPM, list);
			}
			list.add(capital);
		}

		for (List<Capital> list : capitaux.values()) {
			sortRanges(list);
			calculateDatesFin(list);
		}

		return capitaux;
	}

	private enum AssujettissementType {
		LIC("1"),
		LIFD("2");

		private String code;

		private AssujettissementType(String code) {
			this.code = code;
		}
	}

	private Map<Long, List<Assujettissement>> loadAssujettissements(AssujettissementType type) throws IOException {

		final Map<Long, List<Assujettissement>> assujettissements = new HashMap<Long, List<Assujettissement>>();

		//DAD_VALIDITE;DAF_VALIDITE;FK_TYASSUJNO;FK_ENTPRNO
		//1992-12-31;0001-01-01;2;5537
		//1992-12-31;0001-01-01;1;5537
		//1992-12-31;1995-12-31;2;8270
		//1992-12-31;1995-12-31;1;8270
		CSVReader reader = new CSVReader(new FileReader(ResourceUtils.getFile(assujettissementsPMCsvFile)), ';', '"', 1);
		String[] next;

		while ((next = reader.readNext()) != null) {
			if (!type.code.equals(next[2])) {
				continue;
			}

			Assujettissement a = new Assujettissement();
			a.dateDebut = DataHelper.coreToWeb(next[0]);
			a.dateFin = DataHelper.coreToWeb(next[1]);
			a.type = Assujettissement.TypeAssujettissement.ILLIMITE;

			final Long idPM = string2Long(next[3]);
			List<Assujettissement> list = assujettissements.get(idPM);
			if (list == null) {
				list = new ArrayList<Assujettissement>();
				assujettissements.put(idPM, list);
			}
			list.add(a);
		}

		for (List<Assujettissement> l : assujettissements.values()) {
			sortRanges(l);
		}

		return assujettissements;
	}

	private Map<Long, List<Adresse>> loadAdressesPM(String type) throws IOException {

		final Map<Long, List<Adresse>> adresses = new HashMap<Long, List<Adresse>>();

		// 0           1           2                        3                         4         5    6             7                 8                 9        10                     11          12                13              14            15          16
		// DA_VALIDITE;TY_ADRESSE;CHEZ                     ;RUE                      ;NO_POLICE;LIEU;FK_LOC_POSTNO;NO_POSTAL_ACHEMIN;NOM_COMPLET_MIN  ;FK_RUENO;DESIGN_MIN            ;NO_ORDRE_P;NO_POSTAL_ACHEMIN;NOM_COMPLET_MIN;FK_PAYSNO_OFS;NOM_OFS_MIN;FK_ENTPRNO
		// 1997-05-16 ;C         ;                         ;Z.I. Petits-Champs 11 A B;         ;    ;592          ;1400             ;Yverdon-les-Bains;        ;                      ;          ;                 ;               ;             ;           ;5537
		// 1988-05-02 ;S         ;                         ;Petits-Champs            ;         ;    ;592          ;1400             ;Yverdon-les-Bains;        ;                      ;          ;                 ;               ;             ;           ;5537
		// 1995-09-08 ;F         ;p.a. Office des faillites;                         ;         ;    ;185          ;1040             ;Echallens        ;        ;                      ;          ;                 ;               ;             ;           ;8270
		// 1994-03-22 ;S         ;                         ;                         ;3        ;    ;             ;                 ;                 ;96697   ;Orgevaux, chemin de l';215       ;1053             ;Cugy VD        ;             ;           ;8270
		// 2006-10-10 ;C         ;                         ;                         ;54       ;    ;             ;                 ;                 ;97851   ;Acacias, route des    ;443       ;1227             ;Carouge GE     ;             ;           ;15346
		CSVReader reader = new CSVReader(new FileReader(ResourceUtils.getFile(adressesPMCsvFile)), ';', '"', 1);

		String[] row;
		while ((row = reader.readNext()) != null) {
			final Adresse adresse = new Adresse();
			final String t = string2String(row[1]);
			if (t.equals(type)) {
				adresse.dateDebut = DataHelper.coreToWeb(row[0]);
				adresse.titre = string2String(row[2]);

				// la rue
				final Integer noOfsRue = string2Integer(row[9]);
				if (noOfsRue != null) {
					adresse.rue = string2String(row[10]);
					adresse.noRue = noOfsRue;
				}
				else {
					adresse.rue = string2String(row[3]);
				}
				adresse.numeroRue = string2String(row[4]);

				// la localité
				final Integer noOrdreLocalite = string2Integer(row[6]);
				if (noOrdreLocalite != null) {
					adresse.numeroPostal = string2String(row[7]);
					adresse.localite = string2String(row[8]);
					adresse.noOrdrePostal = noOrdreLocalite;
				}
				else if (noOfsRue != null) {
					adresse.numeroPostal = string2String(row[12]);
					adresse.localite = string2String(row[13]);
					adresse.noOrdrePostal = string2Integer(row[11]);
				}
				else {
					adresse.localite = string2String(row[5]);
				}

				// le pays
				final Integer noOfsPays = string2Integer(row[14]);
				if (noOfsPays != null) {
					adresse.pays = string2String(row[15]);
					adresse.noPays = noOfsPays;
				}
				else {
					adresse.noPays = ServiceInfrastructureService.noOfsSuisse;
				}

				final Long idPM = string2Long(row[16]);
				List<Adresse> list = adresses.get(idPM);
				if (list == null) {
					list = new ArrayList<Adresse>();
					adresses.put(idPM, list);
				}
				list.add(adresse);
			}
		}

		for (List<Adresse> list : adresses.values()) {
			sortRanges(list);
			calculateDatesFin(list);
		}

		return adresses;
	}

	private Map<Long, List<FormeJuridique>> loadFormesJuridiquesPM() throws IOException {

		final Map<Long, List<FormeJuridique>> formes = new HashMap<Long, List<FormeJuridique>>();

		//NO_SEQUENCE;DA_VALIDITE;FK_FORMJURCO;FK_ENTPRNO
		//1;1988-05-02;S.A.;5537
		//1;1990-02-05;S.A.;8270
		//1;1936-01-17;S.A.;15346
		CSVReader reader = new CSVReader(new FileReader(ResourceUtils.getFile(formesPMCsvFile)), ';', '"', 1);
		String[] next;

		while ((next = reader.readNext()) != null) {
			final FormeJuridique forme = new FormeJuridique();
			forme.code = string2String(next[2]);
			forme.dateDebut = DataHelper.coreToWeb(next[1]);

			final Long idPM = string2Long(next[3]);
			List<FormeJuridique> list = formes.get(idPM);
			if (list == null) {
				list = new ArrayList<FormeJuridique>();
				formes.put(idPM, list);
			}
			list.add(forme);
		}

		for (List<FormeJuridique> list : formes.values()) {
			sortRanges(list);
			calculateDatesFin(list);
		}

		return formes;
	}

	private Map<Long, List<EtatPM>> loadEtatsPM() throws IOException {

		final Map<Long, List<EtatPM>> etats = new HashMap<Long, List<EtatPM>>();

		//NO_SEQUENCE;DA_VALIDITE;FK_ETATNO;FK_ENTPRNO
		//1;1988-05-02;01;5537
		//1;1990-02-05;01;8270
		//2;1995-09-08;04;8270
		CSVReader reader = new CSVReader(new FileReader(ResourceUtils.getFile(etatsPMCsvFile)), ';', '"', 1);
		String[] next;

		while ((next = reader.readNext()) != null) {
			final EtatPM etat = new EtatPM();
			etat.code = string2String(next[2]);
			etat.dateDebut = DataHelper.coreToWeb(next[1]);

			final Long idPM = string2Long(next[3]);
			List<EtatPM> list = etats.get(idPM);
			if (list == null) {
				list = new ArrayList<EtatPM>();
				etats.put(idPM, list);
			}
			list.add(etat);
		}

		for (List<EtatPM> list : etats.values()) {
			sortRanges(list);
			calculateDatesFin(list);
		}

		return etats;
	}

	private List<EvenementPM> loadEvenementsPM() throws IOException {

		realLifeFactor();

		final List<EvenementPM> events = new ArrayList<EvenementPM>();

		//NO_SEQUENCE;DA_EVENEMENT;FK_EVNMNTNO;FK_ENTPRNO
		//27;2008-08-15;027;5537
		//26;1999-01-01;041;5537
		//25;1999-01-01;041;5537
		CSVReader reader = new CSVReader(new FileReader(ResourceUtils.getFile(evenementsPMCsvFile)), ';', '"', 1);
		String[] next;

		while ((next = reader.readNext()) != null) {
			final EvenementPM etat = new EvenementPM();
			etat.dateEvenement = DataHelper.coreToWeb(next[1]);
			etat.codeEvenement = string2String(next[2]);
			etat.tiersNumber = string2Long(next[3]);
			
			events.add(etat);
		}

		return events;
	}

	/**
	 * Ordonne les événements par ordre chronologique croissant.
	 *
	 * @param list la liste des événements à traiter
	 */
	private void sortEvenements(List<EvenementPM> list) {
		Collections.sort(list, new Comparator<EvenementPM>() {
			public int compare(EvenementPM o1, EvenementPM o2) {
				return Date.asRegDate(o1.dateEvenement).compareTo(Date.asRegDate(o2.dateEvenement));
			}
		});
	}

	/**
	 * Ordonne les ranges par ordre chronologique croissant.
	 *
	 * @param l la liste des ranges à traiter
	 */
	private void sortRanges(List<? extends Range> l) {

		Collections.sort(l, new Comparator<Range>() {
			public int compare(Range o1, Range o2) {

				// Copied-pasted form DateRangeComparator<T>.compareRanges

				// Les objets nuls sont considérés comme plus petits
				if (o1 == null && o2 == null) {
					return 0;
				}
				if (o1 == null) {
					return -1;
				}
				if (o2 == null) {
					return 1;
				}

				Date d1 = o1.getDateDebut();
				Date d2 = o2.getDateDebut();

				if ((d1 == null && d2 == null) || (d1 != null && d2 != null && d1.equals(d2))) {
					/* si les deux dates de début de validité sont nulles ou qu'elles sont égales, on essaie les dates de fin de validité */
					d1 = o1.getDateFin();
					d2 = o2.getDateFin();

					if (d1 == null && d2 == null) {
						return 0;
					}

					// Les dates de fin nulles sont considérées comme plus grandes
					if (d1 == null) {
						return 1;
					}
					if (d2 == null) {
						return -1;
					}
				}
				else {

					// Les dates de début nulles sont considérées comme plus petites
					if (d1 == null) {
						return -1;
					}
					if (d2 == null) {
						return 1;
					}
				}

				return Date.asRegDate(d1).compareTo(Date.asRegDate(d2));
			}
		});
	}

	/**
	 * Calcule et met-à-jour les dates de fin à partir des dates de début suivantes
	 *
	 * @param list la liste triées des ranges à traiter
	 */
	private void calculateDatesFin(List<? extends Range> list) {
		if (list == null) {
			return;
		}

		Date debutPrecedent = null;
		for (int i = list.size() - 1; i >= 0; --i) {
			final Range r = list.get(i);
			if (debutPrecedent != null) {
				final Date fin = new Date(Date.asRegDate(debutPrecedent).getOneDayBefore());
				r.setDateFin(fin);
			}
			debutPrecedent = r.getDateDebut();
		}
	}

	/**
	 * Construit la vue à une date donnée des informations d'une personne morale à partir des informations historiques.
	 *
	 * @param pmHisto la personne morale avec toutes les informations historiques
	 * @param date    la date de validité des informations de la personne morale retournées
	 * @return une personne morale
	 */
	private PersonneMorale getAt(PersonneMoraleHisto pmHisto, Date date) {
		final PersonneMorale pm;
		pm = new PersonneMorale();
		pm.numero = pmHisto.numero;
		pm.adresseCourrierElectronique = pmHisto.adresseCourrierElectronique;
		pm.adresseEnvoi = pmHisto.adresseEnvoi;
		pm.adresseDomicileFormattee = pmHisto.adresseDomicileFormattee;
		pm.adresseRepresentationFormattee = pmHisto.adresseRepresentationFormattee;
		pm.adressePoursuiteFormattee = pmHisto.adressePoursuiteFormattee;
		pm.blocageRemboursementAutomatique = pmHisto.blocageRemboursementAutomatique;
		pm.complementNom = pmHisto.complementNom;
		pm.comptesBancaires = pmHisto.comptesBancaires;
		pm.dateBouclementFutur = pmHisto.dateBouclementFutur;
		pm.dateDebutActivite = pmHisto.dateDebutActivite;
		pm.dateFinActivite = pmHisto.dateFinActivite;
		pm.dateFinDernierExerciceCommercial = pmHisto.dateFinDernierExerciceCommercial;
		pm.designationAbregee = pmHisto.designationAbregee;
		pm.numeroIPMRO = pmHisto.numeroIPMRO;
		pm.numeroTelecopie = pmHisto.numeroTelecopie;
		pm.numeroTelPortable = pmHisto.numeroTelPortable;
		pm.numeroTelPrive = pmHisto.numeroTelPrive;
		pm.numeroTelProf = pmHisto.numeroTelProf;
		pm.personneContact = pmHisto.personneContact;
		pm.raisonSociale1 = pmHisto.raisonSociale1;
		pm.raisonSociale2 = pmHisto.raisonSociale2;
		pm.raisonSociale3 = pmHisto.raisonSociale3;

		pm.adresseCourrier = getAt(pmHisto.adressesCourrier, date);
		pm.adresseDomicile = getAt(pmHisto.adressesDomicile, date);
		pm.adressePoursuite = getAt(pmHisto.adressesPoursuite, date);
		pm.adresseRepresentation = getAt(pmHisto.adressesRepresentation, date);
		pm.assujettissementLIC = getAt(pmHisto.assujettissementsLIC, date);
		pm.assujettissementLIFD = getAt(pmHisto.assujettissementsLIFD, date);
		pm.autresForsFiscaux = getAllAt(pmHisto.autresForsFiscaux, date);
		pm.capital = getAt(pmHisto.capitaux, date);
		pm.declaration = getAt(pmHisto.declarations, date);
		pm.etat = getAt(pmHisto.etats, date);
		pm.forFiscalPrincipal = getAt(pmHisto.forsFiscauxPrincipaux, date);
		pm.forGestion = getAt(pmHisto.forsGestions, date);
		pm.formeJuridique = getAt(pmHisto.formesJuridiques, date);
		pm.periodeImposition = getAt(pmHisto.periodesImposition, date);
		pm.regimeFiscalIFD = getAt(pmHisto.regimesFiscauxIFD, date);
		pm.regimeFiscalICC = getAt(pmHisto.regimesFiscauxICC, date);
		pm.siege = getAt(pmHisto.sieges, date);
		return pm;
	}

	private static <T extends Range> T getAt(List<T> ranges, Date date) {
		final RegDate d = DataHelper.webToCore(date);
		T range = null;
		if (ranges != null) {
			for (T r : ranges) {
				if (RegDateHelper.isBetween(d, DataHelper.webToCore(r.getDateDebut()), DataHelper.webToCore(r.getDateFin()), NullDateBehavior.LATEST)) {
					range = r;
					break;
				}
			}
		}
		return range;
	}

	private static <T extends Range> List<T> getAllAt(List<T> ranges, Date date) {
		List<T> list = null;
		if (ranges != null) {
			final RegDate d = DataHelper.webToCore(date);
			for (T r : ranges) {
				if (RegDateHelper.isBetween(d, DataHelper.webToCore(r.getDateDebut()), DataHelper.webToCore(r.getDateFin()), NullDateBehavior.LATEST)) {
					if (list == null) {
						list = new ArrayList<T>();
					}
					list.add(r);
				}
			}
		}
		return list;
	}

	private static String string2String(String s) {
		return StringUtils.isEmpty(s) ? null : s;
	}

	private static Long string2Long(String s) {
		return StringUtils.isEmpty(s) ? null : Long.valueOf(s);
	}

	private static Integer string2Integer(String s) {
		return StringUtils.isEmpty(s) ? null : Integer.valueOf(s);
	}

	private static void realLifeFactor() {
		try {
			Thread.sleep((long) (150.0 + 100.0 * Math.random()));
		}
		catch (InterruptedException e) {
			// ignored
		}
	}

	private void realLifeFactorBatch(int count) {
		try {
			Thread.sleep((long) (250.0 + ((long)count) * 20.0 * Math.random()));
		}
		catch (InterruptedException e) {
			// ignored
		}
	}
}
