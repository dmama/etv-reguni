package ch.vd.uniregctb.webservices.tiers3.impl.pm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.webservices.tiers3.DateHelper;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.common.RueEtNumero;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Etablissement;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Mandat;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.TypeAffranchissement;
import ch.vd.uniregctb.interfaces.model.TypeNoOfs;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.FormulePolitesse;
import ch.vd.uniregctb.type.TypeAdressePM;
import ch.vd.uniregctb.webservices.tiers3.Adresse;
import ch.vd.uniregctb.webservices.tiers3.AdresseFormattee;
import ch.vd.uniregctb.webservices.tiers3.BatchTiers;
import ch.vd.uniregctb.webservices.tiers3.BatchTiersEntry;
import ch.vd.uniregctb.webservices.tiers3.Capital;
import ch.vd.uniregctb.webservices.tiers3.CompteBancaire;
import ch.vd.uniregctb.webservices.tiers3.DebiteurInfo;
import ch.vd.uniregctb.webservices.tiers3.EditionFosc;
import ch.vd.uniregctb.webservices.tiers3.EtatPM;
import ch.vd.uniregctb.webservices.tiers3.EvenementPM;
import ch.vd.uniregctb.webservices.tiers3.ForFiscal;
import ch.vd.uniregctb.webservices.tiers3.FormatNumeroCompte;
import ch.vd.uniregctb.webservices.tiers3.FormeJuridique;
import ch.vd.uniregctb.webservices.tiers3.GenreImpot;
import ch.vd.uniregctb.webservices.tiers3.GetBatchTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.GetDebiteurInfoRequest;
import ch.vd.uniregctb.webservices.tiers3.GetListeCtbModifiesRequest;
import ch.vd.uniregctb.webservices.tiers3.GetTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.GetTiersTypeRequest;
import ch.vd.uniregctb.webservices.tiers3.MotifRattachement;
import ch.vd.uniregctb.webservices.tiers3.PeriodeAssujettissement;
import ch.vd.uniregctb.webservices.tiers3.PersonneMorale;
import ch.vd.uniregctb.webservices.tiers3.QuittancerDeclarationsRequest;
import ch.vd.uniregctb.webservices.tiers3.QuittancerDeclarationsResponse;
import ch.vd.uniregctb.webservices.tiers3.RegimeFiscal;
import ch.vd.uniregctb.webservices.tiers3.SearchEvenementsPMRequest;
import ch.vd.uniregctb.webservices.tiers3.SearchEvenementsPMResponse;
import ch.vd.uniregctb.webservices.tiers3.SearchTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.SearchTiersResponse;
import ch.vd.uniregctb.webservices.tiers3.SetTiersBlocRembAutoRequest;
import ch.vd.uniregctb.webservices.tiers3.Siege;
import ch.vd.uniregctb.webservices.tiers3.Tiers;
import ch.vd.uniregctb.webservices.tiers3.TiersPart;
import ch.vd.uniregctb.webservices.tiers3.TiersWebService;
import ch.vd.uniregctb.webservices.tiers3.TypeAutoriteFiscale;
import ch.vd.uniregctb.webservices.tiers3.TypePeriodeAssujettissement;
import ch.vd.uniregctb.webservices.tiers3.TypeSiege;
import ch.vd.uniregctb.webservices.tiers3.TypeTiers;
import ch.vd.uniregctb.webservices.tiers3.WebServiceException;
import ch.vd.uniregctb.webservices.tiers3.data.AdresseBuilder;
import ch.vd.uniregctb.webservices.tiers3.impl.DataHelper;

/**
 * Classe qui retourne des données bouchonnées concernant les personnes morales. Les appels concernant les personnes physiques sont simplement délégués plus loin.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class TiersWebServiceWithPM implements TiersWebService {

	private static final int OPTIONALITE_COMPLEMENT = 1;

	private TiersDAO tiersDAO;
	private TiersWebService target;
	private ServicePersonneMoraleService servicePM;
	private ServiceCivilService serviceCivil;
	private ServiceInfrastructureService serviceInfra;

	public void setTarget(TiersWebService target) {
		this.target = target;
	}

	public void setServicePM(ServicePersonneMoraleService servicePM) {
		this.servicePM = servicePM;
	}

	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public SearchTiersResponse searchTiers(SearchTiersRequest params) throws WebServiceException {
		return target.searchTiers(params);
	}

	@Override
	@Transactional(readOnly = true)
	public TypeTiers getTiersType(GetTiersTypeRequest params) throws WebServiceException {
		if (isEntreprise(params.getTiersNumber())) {
			return existsPM(params.getTiersNumber()) ? TypeTiers.PERSONNE_MORALE : null;
		}
		else {
			return target.getTiersType(params);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Tiers getTiers(GetTiersRequest params) throws WebServiceException {
		if (isEntreprise(params.getTiersNumber())) {
			final HashSet<TiersPart> parts = (params.getParts() == null ? null : new HashSet<TiersPart>(params.getParts()));
			return getPersonneMorale(params.getTiersNumber(), parts);
		}
		else {
			return target.getTiers(params);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public BatchTiers getBatchTiers(GetBatchTiersRequest params) throws WebServiceException {

		if (params.getTiersNumbers() == null || params.getTiersNumbers().isEmpty()) {
			return new BatchTiers();
		}

		// sépare les ids PP des ids PM
		final Set<Long> idsPP = new HashSet<Long>();
		final Set<Long> idsPM = new HashSet<Long>();
		for (Long id : params.getTiersNumbers()) {
			if (isEntreprise(id)) {
				idsPM.add(id);
			}
			else {
				idsPP.add(id);
			}

		}

		BatchTiers batchPP = null;

		// récupère les tiers PP
		if (!idsPP.isEmpty()) {
			GetBatchTiersRequest paramsPP = new GetBatchTiersRequest();
			paramsPP.getTiersNumbers().addAll(idsPP);
			paramsPP.setLogin(params.getLogin());
			paramsPP.getParts().addAll(params.getParts());
			batchPP = target.getBatchTiers(paramsPP);
		}

		BatchTiers batchPM = null;

		// récupère les tiers PM
		if (!idsPM.isEmpty()) {
			final HashSet<TiersPart> parts = (params.getParts() == null ? null : new HashSet<TiersPart>(params.getParts()));
			batchPM = new BatchTiers();
			for (Long id : idsPM) {
				final PersonneMorale pmHisto = getPersonneMorale(id, parts);
				if (pmHisto != null) {
					final BatchTiersEntry entry = new BatchTiersEntry();
					entry.setNumber(id);
					entry.setTiers(pmHisto);
					batchPM.getEntries().add(entry);
				}
			}
		}

		// fusion des données PP et PM si nécessaire
		BatchTiers batch = null;
		if (batchPP != null && batchPM == null) {
			batch = batchPP;
		}
		else if (batchPP == null && batchPM != null) {
			batch = batchPM;
		}
		else if (batchPP != null) {
			batchPP.getEntries().addAll(batchPM.getEntries());
			batch = batchPP;
		}

		return batch;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void setTiersBlocRembAuto(SetTiersBlocRembAutoRequest params) throws WebServiceException {

		if (isEntreprise(params.getTiersNumber()) && !tiersDAO.exists(params.getTiersNumber())) {
			// [UNIREG-2040] on crée l'entreprise à la volée
			Entreprise e = new Entreprise();
			e.setNumero(params.getTiersNumber());
			tiersDAO.save(e);
		}

		target.setTiersBlocRembAuto(params);
	}

	@Override
	public SearchEvenementsPMResponse searchEvenementsPM(SearchEvenementsPMRequest params) throws WebServiceException {

		final List<ch.vd.uniregctb.interfaces.model.EvenementPM> list =
				servicePM.findEvenements(params.getTiersNumber(), params.getCodeEvenement(), DataHelper.webToCore(params.getDateMinimale()), DataHelper.webToCore(params.getDateMaximale()));
		return events2web(list);
	}

	@Override
	public DebiteurInfo getDebiteurInfo(GetDebiteurInfoRequest params) throws WebServiceException {
		return target.getDebiteurInfo(params);
	}

	@Override
	public QuittancerDeclarationsResponse quittancerDeclarations(QuittancerDeclarationsRequest params) throws WebServiceException {
		return target.quittancerDeclarations(params);
	}

	@Override
	public void ping() {
		// rien à faire
	}

	@Override
	public Long[] getListeCtbModifies(GetListeCtbModifiesRequest params) throws WebServiceException {
		return target.getListeCtbModifies(params);
	}

	/**
	 * Vérifie l'existence d'une personne morale.
	 *
	 * @param id l'id de la personne morale.
	 * @return <i>vrai</i> si la personne morale existe; <i>faux</i> autrement.
	 */
	private boolean existsPM(Long id) {
		return servicePM.getPersonneMorale(id) != null;
	}

	/**
	 * @param tiersNumber un numéro de tiers
	 * @return <i>vrai</i> si le numéro de tiers spécifié est une entreprise.
	 */
	private static boolean isEntreprise(long tiersNumber) {
		return (Entreprise.FIRST_ID <= tiersNumber && tiersNumber <= Entreprise.LAST_ID);
	}

	private PersonneMorale getPersonneMorale(long tiersNumber, Set<TiersPart> parts) {

		final ch.vd.uniregctb.interfaces.model.PersonneMorale pmHost = servicePM.getPersonneMorale(tiersNumber, web2business(parts));
		if (pmHost == null) {
			return null;
		}

		if (parts == null) {
			parts = Collections.emptySet();
		}

		final PersonneMorale pm = new PersonneMorale();
		pm.setNumero(pmHost.getNumeroEntreprise());
		pm.setNumeroTelProf(pmHost.getTelephoneContact());
		pm.setNumeroTelecopie(pmHost.getTelecopieContact());

		pm.setPersonneContact(pmHost.getNomContact());
		pm.setDesignationAbregee(pmHost.getDesignationAbregee());
		pm.setRaisonSociale1(pmHost.getRaisonSociale1());
		pm.setRaisonSociale2(pmHost.getRaisonSociale2());
		pm.setRaisonSociale3(pmHost.getRaisonSociale3());
		pm.setDateDebutActivite(DataHelper.coreToWeb(pmHost.getDateConstitution()));
		pm.setDateFinActivite(DataHelper.coreToWeb(pmHost.getDateFinActivite()));
		pm.setDateBouclementFutur(DataHelper.coreToWeb(pmHost.getDateBouclementFuture()));
		pm.setNumeroIPMRO(pmHost.getNumeroIPMRO());

		// [UNIREG-2040] on va chercher l'information de blocage dans notre base si elle existe
		final ch.vd.uniregctb.tiers.Tiers tiers = tiersDAO.get(tiersNumber);
		if (tiers != null) {
			pm.setBlocageRemboursementAutomatique(tiers.getBlocageRemboursementAutomatique());
		}
		else {
			pm.setBlocageRemboursementAutomatique(true); // [UNIREG-1266] Blocage des remboursements automatiques sur tous les nouveaux tiers
		}

		if (parts.contains(TiersPart.ADRESSES) || parts.contains(TiersPart.ADRESSES_FORMATTEES)) {
			final Collection<ch.vd.uniregctb.interfaces.model.AdresseEntreprise> adresses = pmHost.getAdresses();
			if (adresses != null) {
				for (ch.vd.uniregctb.interfaces.model.AdresseEntreprise a : adresses) {
					if (a.getType() == TypeAdressePM.COURRIER) {
						pm.getAdressesCourrier().add(host2web(a));
					}
					else if (a.getType() == TypeAdressePM.SIEGE) {
						pm.getAdressesDomicile().add(host2web(a));
					}
					else if (a.getType() == TypeAdressePM.FACTURATION) {
						// ces adresses sont ignorées
					}
					else {
						throw new IllegalArgumentException("Type d'adresse entreprise inconnue = [" + a.getType() + "]");
					}
				}
			}
			pm.getAdressesRepresentation().addAll(pm.getAdressesCourrier());
			// [UNIREG-1808] les adresses de poursuite des PMs sont déterminées à partir des adresses siège, en attendant des évolutions dans le host.
			pm.getAdressesPoursuite().addAll(pm.getAdressesDomicile());
		}

		if (parts.contains(TiersPart.ADRESSES_FORMATTEES)) {
			pm.setAdresseCourrierFormattee(calculateAdresseEnvoi(pm, pm.getAdressesCourrier()));
			pm.setAdresseDomicileFormattee(calculateAdresseEnvoi(pm, pm.getAdressesDomicile()));
			pm.setAdresseRepresentationFormattee(calculateAdresseEnvoi(pm, pm.getAdressesRepresentation()));
			pm.setAdressePoursuiteFormattee(calculateAdresseEnvoi(pm, pm.getAdressesPoursuite()));
		}

		if (parts.contains(TiersPart.PERIODES_ASSUJETTISSEMENT)) {
			pm.getPeriodesAssujettissementLIC().addAll(assujettissements2web(pmHost.getAssujettissementsLIC()));
			pm.getPeriodesAssujettissementLIFD().addAll(assujettissements2web(pmHost.getAssujettissementsLIFD()));
		}

		if (parts.contains(TiersPart.CAPITAUX)) {
			pm.getCapitaux().addAll(capitaux2web(pmHost.getCapitaux()));
		}

		if (parts.contains(TiersPart.COMPTES_BANCAIRES)) {
			pm.getComptesBancaires().addAll(calculateComptesBancaires(pmHost));
		}

		if (parts.contains(TiersPart.ETATS_PM)) {
			pm.getEtats().addAll(etats2web(pmHost.getEtats()));
		}

		if (parts.contains(TiersPart.FORMES_JURIDIQUES)) {
			pm.getFormesJuridiques().addAll(formes2web(pmHost.getFormesJuridiques()));
		}

		if (parts.contains(TiersPart.FORS_FISCAUX) || parts.contains(TiersPart.FORS_FISCAUX_VIRTUELS)) {
			pm.getForsFiscauxPrincipaux().addAll(forsPrincipaux2web(pmHost.getForsFiscauxPrincipaux()));
			pm.getAutresForsFiscaux().addAll(forsSecondaires2web(pmHost.getForsFiscauxSecondaires()));
		}

		if (parts.contains(TiersPart.REGIMES_FISCAUX)) {
			pm.getRegimesFiscauxICC().addAll(regimes2web(pmHost.getRegimesVD()));
			pm.getRegimesFiscauxIFD().addAll(regimes2web(pmHost.getRegimesCH()));
		}

		if (parts.contains(TiersPart.SIEGES)) {
			pm.getSieges().addAll(sieges2web(pmHost.getSieges()));
		}

		return pm;
	}

	private List<CompteBancaire> calculateComptesBancaires(ch.vd.uniregctb.interfaces.model.PersonneMorale pmHost) {

		// on tient du compte du compte bancaire de la PM elle-même
		List<CompteBancaire> list = comptes2web(pmHost.getComptesBancaires());

		// [UNIREG-2106] on ajoute les comptes bancaires des mandats de type 'T'
		final List<Mandat> mandats = pmHost.getMandats();
		if (mandats != null) {
			for (Mandat m : mandats) {
				if (m.getCode().equals("T")) { // on ignore tous les autres types de mandataire

					CompteBancaire cb = new CompteBancaire();
					cb.setNumeroTiersTitulaire(m.getNumeroMandataire());

					// on rempli les informations à partir du mandataire
					fillCompteBancaireDepuisMandataire(cb, m);

					// on surcharge les informations à partir du mandat, si nécessaire
					fillCompteBancaireDepuisMandat(cb, m);

					if (list == null) {
						list = new ArrayList<CompteBancaire>();
					}
					list.add(cb);
				}
			}
		}

		return list;
	}

	private void fillCompteBancaireDepuisMandataire(CompteBancaire cb, Mandat m) {
		switch (m.getTypeMandataire()) {
		case INDIVIDU:
			fillCompteBancaireDepuisMandataireIndividu(cb, m.getNumeroMandataire());
			break;
		case PERSONNE_MORALE:
			fillCompteBancaireDepuisMandatairePersonneMorale(cb, m.getNumeroMandataire());
			break;
		case ETABLISSEMENT:
			fillCompteBancaireDepuisMandataireEtablissement(cb, m.getNumeroMandataire());
			break;
		default:
			throw new IllegalArgumentException("Type de mandataire inconnu =[" + m.getTypeMandataire() + "]");
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
	private void fillCompteBancaire(CompteBancaire cb, String iban, String comptesBancaire, String ccp, String bicSwift, String nomInstitution) {
		if (iban != null) {
			cb.setNumero(iban);
			cb.setFormat(FormatNumeroCompte.IBAN);
			cb.setAdresseBicSwift(bicSwift);
			cb.setNomInstitution(nomInstitution);
		}
		else if (comptesBancaire != null) {
			cb.setNumero(comptesBancaire);
			cb.setFormat(FormatNumeroCompte.SPECIFIQUE_CH);
			cb.setAdresseBicSwift(bicSwift);
			cb.setNomInstitution(nomInstitution);
		}
		else if (ccp != null) {
			cb.setNumero(ccp);
			cb.setFormat(FormatNumeroCompte.SPECIFIQUE_CH);
			cb.setAdresseBicSwift(bicSwift);
			cb.setNomInstitution(nomInstitution);
		}
	}

	private void fillCompteBancaireDepuisMandataireEtablissement(CompteBancaire cb, long noEtablissement) {
		final Etablissement etablissement = servicePM.getEtablissement(noEtablissement);
		if (etablissement != null) {
			cb.setTitulaire(etablissement.getEnseigne());
			fillCompteBancaire(cb, etablissement.getIBAN(), etablissement.getCompteBancaire(), etablissement.getCCP(), etablissement.getBicSwift(), etablissement.getNomInstitutionFinanciere());
		}
	}

	private void fillCompteBancaireDepuisMandatairePersonneMorale(CompteBancaire cb, long noPM) {
		final ch.vd.uniregctb.interfaces.model.PersonneMorale pm = servicePM.getPersonneMorale(noPM, (PartPM[]) null);
		if (pm != null) {
			cb.setTitulaire(pm.getRaisonSociale());

			final List<ch.vd.uniregctb.interfaces.model.CompteBancaire> cpm = pm.getComptesBancaires();
			if (cpm != null && !cpm.isEmpty()) {
				final ch.vd.uniregctb.interfaces.model.CompteBancaire c = cpm.get(0); // faut-il vraiment toujours le premier ?
				cb.setFormat(FormatNumeroCompte.valueOf(c.getFormat().name()));
				cb.setNumero(c.getNumero());
				cb.setNomInstitution(c.getNomInstitution());
			}
		}
	}

	private void fillCompteBancaireDepuisMandataireIndividu(CompteBancaire cb, long noIndividu) {
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
		if (individu != null) {
			cb.setTitulaire(serviceCivil.getNomPrenom(individu));
			// aucune information de compte bancaire sur un individu...
		}
	}

	private void fillCompteBancaireDepuisMandat(CompteBancaire cb, Mandat m) {
		final String nomInstitution = getNomInstitution(m.getNumeroInstitutionFinanciere());

		fillCompteBancaire(cb, m.getIBAN(), m.getCompteBancaire(), m.getCCP(), m.getBicSwift(), nomInstitution);
	}

	private String getNomInstitution(Long noInstit) {

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

	private SearchEvenementsPMResponse events2web(List<ch.vd.uniregctb.interfaces.model.EvenementPM> events) {
		if (events == null || events.isEmpty()) {
			return null;
		}
		final SearchEvenementsPMResponse response = new SearchEvenementsPMResponse();
		for (ch.vd.uniregctb.interfaces.model.EvenementPM e : events) {
			EvenementPM event = new EvenementPM();
			event.setTiersNumber(e.getNumeroPM());
			event.setDateEvenement(DataHelper.coreToWeb(e.getDate()));
			event.setCodeEvenement(e.getCode());
			response.getItem().add(event);
		}
		return response;
	}

	private static List<CompteBancaire> comptes2web(List<ch.vd.uniregctb.interfaces.model.CompteBancaire> comptes) {
		if (comptes == null || comptes.isEmpty()) {
			return null;
		}
		final List<CompteBancaire> list = new ArrayList<CompteBancaire>();
		for (ch.vd.uniregctb.interfaces.model.CompteBancaire c : comptes) {
			CompteBancaire compte = new CompteBancaire();
			compte.setFormat(FormatNumeroCompte.valueOf(c.getFormat().name()));
			compte.setNumero(c.getNumero());
			compte.setNomInstitution(c.getNomInstitution());
			list.add(compte);
		}
		return list;
	}

	private List<Siege> sieges2web(List<ch.vd.uniregctb.interfaces.model.Siege> sieges) {
		if (sieges == null || sieges.isEmpty()) {
			return null;
		}
		final ArrayList<Siege> list = new ArrayList<Siege>(sieges.size());
		for (ch.vd.uniregctb.interfaces.model.Siege s : sieges) {
			list.add(host2web(s));
		}
		return list;
	}

	private Siege host2web(ch.vd.uniregctb.interfaces.model.Siege s) {
		Assert.notNull(s);
		Siege siege = new Siege();
		siege.setDateDebut(DataHelper.coreToWeb(s.getDateDebut()));
		siege.setDateFin(DataHelper.coreToWeb(s.getDateFin()));
		siege.setNoOfsSiege(s.getNoOfsSiege());
		siege.setTypeSiege(typeSiege2web(s.getType()));
		return siege;
	}

	private static TypeSiege typeSiege2web(TypeNoOfs type) {
		switch (type) {
		case COMMUNE_CH:
			return TypeSiege.COMMUNE_CH;
		case PAYS_HS:
			return TypeSiege.PAYS_HS;
		default:
			throw new IllegalArgumentException("Type de no Ofs inconnu = [" + type + "]");
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
		regime.setDateDebut(DataHelper.coreToWeb(r.getDateDebut()));
		regime.setDateFin(DataHelper.coreToWeb(r.getDateFin()));
		regime.setCode(r.getCode());
		return regime;
	}

	private List<ForFiscal> forsSecondaires2web(List<ch.vd.uniregctb.interfaces.model.ForPM> fors) {
		if (fors == null || fors.isEmpty()) {
			return null;
		}
		final ArrayList<ForFiscal> list = new ArrayList<ForFiscal>(fors.size());
		for (ch.vd.uniregctb.interfaces.model.ForPM f : fors) {
			list.add(secondaire2web(f));
		}
		return list;
	}

	private ForFiscal secondaire2web(ch.vd.uniregctb.interfaces.model.ForPM f) {
		Assert.notNull(f);
		ForFiscal ffs = new ForFiscal();
		ffs.setDateDebut(DataHelper.coreToWeb(f.getDateDebut()));
		ffs.setDateFin(DataHelper.coreToWeb(f.getDateFin()));
		ffs.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
		ffs.setMotifRattachement(MotifRattachement.ETABLISSEMENT_STABLE);
		ffs.setNoOfsAutoriteFiscale(f.getNoOfsAutoriteFiscale());
		ffs.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD); // par définition
		return ffs;
	}

	private List<ForFiscal> forsPrincipaux2web(List<ch.vd.uniregctb.interfaces.model.ForPM> fors) {
		if (fors == null || fors.isEmpty()) {
			return null;
		}
		final ArrayList<ForFiscal> list = new ArrayList<ForFiscal>(fors.size());
		for (ch.vd.uniregctb.interfaces.model.ForPM f : fors) {
			list.add(principal2web(f));
		}
		return list;
	}

	private ForFiscal principal2web(ch.vd.uniregctb.interfaces.model.ForPM f) {
		Assert.notNull(f);
		ForFiscal ffp = new ForFiscal();
		ffp.setDateDebut(DataHelper.coreToWeb(f.getDateDebut()));
		ffp.setDateFin(DataHelper.coreToWeb(f.getDateFin()));
		ffp.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
		ffp.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp.setTypeAutoriteFiscale(host2web(f.getTypeAutoriteFiscale(), f.getNoOfsAutoriteFiscale()));
		if (ffp.getTypeAutoriteFiscale() != TypeAutoriteFiscale.PAYS_HS) {
			ffp.setNoOfsAutoriteFiscale(f.getNoOfsAutoriteFiscale());
		}
		else {
			ffp.setNoOfsAutoriteFiscale(f.getNoOfsAutoriteFiscale());
		}
		return ffp;
	}

	private TypeAutoriteFiscale host2web(TypeNoOfs type, int noOfs) {
		switch (type) {
		case COMMUNE_CH:
			final Commune commune;
			try {
				commune = serviceInfra.getCommuneByNumeroOfsEtendu(noOfs, null);
			}
			catch (ServiceInfrastructureException e) {
				throw new RuntimeException("Impossible de récupérer la commune avec le numéro Ofs = [" + noOfs + "]", e);
			}
			if (commune == null) {
				throw new RuntimeException("La commune avec le numéro Ofs = [" + noOfs + "] n'existe pas.");
			}
			// [UNIREG-2641] on doit différencier les communes hors-canton des communes vaudoises
			if (ServiceInfrastructureService.SIGLE_CANTON_VD.equals(commune.getSigleCanton())) {
				return TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
			}
			else {
				return TypeAutoriteFiscale.COMMUNE_HC;
			}
		case PAYS_HS:
			return TypeAutoriteFiscale.PAYS_HS;
		default:
			throw new IllegalArgumentException("Type de no Ofs inconnu = [" + type + "]");
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
		forme.setDateDebut(DataHelper.coreToWeb(f.getDateDebut()));
		forme.setDateFin(DataHelper.coreToWeb(f.getDateFin()));
		forme.setCode(f.getCode());
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
		etat.setDateDebut(DataHelper.coreToWeb(e.getDateDebut()));
		etat.setDateFin(DataHelper.coreToWeb(e.getDateFin()));
		etat.setCode(e.getCode());
		return etat;
	}

	private static List<Capital> capitaux2web(List<ch.vd.uniregctb.interfaces.model.Capital> capitaux) {
		if (capitaux == null || capitaux.isEmpty()) {
			return null;
		}
		final ArrayList<Capital> list = new ArrayList<Capital>(capitaux.size());
		for (ch.vd.uniregctb.interfaces.model.Capital c : capitaux) {
			list.add(host2web(c));
		}
		return list;
	}

	private static Capital host2web(ch.vd.uniregctb.interfaces.model.Capital c) {
		Assert.notNull(c);
		Capital capital = new Capital();
		capital.setDateDebut(DataHelper.coreToWeb(c.getDateDebut()));
		capital.setDateFin(DataHelper.coreToWeb(c.getDateFin()));
		capital.setCapitalAction(c.getCapitalAction());
		capital.setCapitalLibere(c.getCapitalLibere());
		capital.setEditionFosc(host2web(c.getEditionFosc()));
		return capital;
	}

	private static EditionFosc host2web(ch.vd.uniregctb.interfaces.model.EditionFosc e) {
		Assert.notNull(e);
		return new EditionFosc(e.getAnnee(), e.getNumero());
	}

	private static List<PeriodeAssujettissement> assujettissements2web(List<ch.vd.uniregctb.interfaces.model.AssujettissementPM> lic) {
		if (lic == null || lic.isEmpty()) {
			return null;
		}
		final ArrayList<PeriodeAssujettissement> list = new ArrayList<PeriodeAssujettissement>(lic.size());
		for (ch.vd.uniregctb.interfaces.model.AssujettissementPM a : lic) {
			list.add(host2web(a));
		}
		return list;
	}

	private static PeriodeAssujettissement host2web(ch.vd.uniregctb.interfaces.model.AssujettissementPM a) {
		Assert.notNull(a);
		PeriodeAssujettissement assujet = new PeriodeAssujettissement();
		assujet.setDateDebut(DataHelper.coreToWeb(a.getDateDebut()));
		assujet.setDateFin(DataHelper.coreToWeb(a.getDateFin()));
		assujet.setType(TypePeriodeAssujettissement.ILLIMITE);
		return assujet;
	}

	private static Adresse host2web(ch.vd.uniregctb.interfaces.model.AdresseEntreprise a) {
		Assert.notNull(a);
		Adresse adresse = new Adresse();
		adresse.setDateDebut(DataHelper.coreToWeb(a.getDateDebutValidite()));
		adresse.setDateFin(DataHelper.coreToWeb(a.getDateFinValidite()));
		adresse.setTitre(a.getComplement());
		adresse.setRue(a.getRue());
		adresse.setNoRue(a.getNumeroTechniqueRue());
		adresse.setNumeroRue(a.getNumeroMaison());
		adresse.setNumeroPostal(a.getNumeroPostal());
		adresse.setLocalite(a.getLocaliteAbregeMinuscule());
		adresse.setNoOrdrePostal(a.getNumeroOrdrePostal());
		if (a.getPays() == null) {
			adresse.setPays(null);
			adresse.setNoPays(ServiceInfrastructureService.noOfsSuisse);
		}
		else {
			adresse.setPays(a.getPays().getNomMinuscule());
			adresse.setNoPays(a.getPays().getNoOFS());
		}
		return adresse;
	}

	private PartPM[] web2business(Set<TiersPart> parts) {

		if (parts == null || parts.isEmpty()) {
			return null;
		}

		final Set<PartPM> set = new HashSet<PartPM>();
		if (parts.contains(TiersPart.ADRESSES) || parts.contains(TiersPart.ADRESSES_FORMATTEES)) {
			set.add(PartPM.ADRESSES);
		}
		if (parts.contains(TiersPart.PERIODES_ASSUJETTISSEMENT)) {
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

	private AdresseFormattee calculateAdresseEnvoi(PersonneMorale pm, List<Adresse> adresses) {
		AdresseEnvoiDetaillee adresse = new AdresseEnvoiDetaillee(AdresseGenerique.SourceType.PM);

		// [UNIREG-2302]
		adresse.addFormulePolitesse(FormulePolitesse.PERSONNE_MORALE);

		// [UNIREG-1974] On doit utiliser la raison sociale sur 3 lignes dans les adresses d'envoi des PMs (et ne pas prendre la raison sociale abbrégée)
		if (pm.getRaisonSociale1() != null) {
			adresse.addRaisonSociale(pm.getRaisonSociale1());
		}

		if (pm.getRaisonSociale2() != null) {
			adresse.addRaisonSociale(pm.getRaisonSociale2());
		}

		if (pm.getRaisonSociale3() != null) {
			adresse.addRaisonSociale(pm.getRaisonSociale3());
		}

// [UNIREG-1973] il ne faut pas utiliser la personne de contact dans les adresses
//		if (pm.personneContact != null) {
//			adresse.addPourAdresse(pm.personneContact);
//		}

		final Adresse adresseFiscale = DateHelper.getAt(adresses, null);
		if (adresseFiscale != null) {

			if (adresseFiscale.getTitre() != null) {
				// [UNIREG-1974] Le complément est optionnel
				adresse.addComplement(adresseFiscale.getTitre(), OPTIONALITE_COMPLEMENT);
			}

			if (adresseFiscale.getRue() != null) {
				adresse.addRueEtNumero(new RueEtNumero(adresseFiscale.getRue(), adresseFiscale.getNumeroRue()));
			}

			final String npaLocalite;
			if (adresseFiscale.getNumeroPostal() != null) {
				npaLocalite = adresseFiscale.getNumeroPostal() + " " + adresseFiscale.getLocalite();
			}
			else {
				npaLocalite = adresseFiscale.getLocalite();
			}
			adresse.addNpaEtLocalite(npaLocalite);

			if (adresseFiscale.getPays() != null) {
				final TypeAffranchissement typeAffranchissement = serviceInfra.getTypeAffranchissement(adresseFiscale.getNoPays());
				adresse.addPays(adresseFiscale.getPays(), typeAffranchissement);
			}
		}

		return AdresseBuilder.newAdresseFormattee(adresse);
	}
}
