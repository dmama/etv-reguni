package ch.vd.uniregctb.webservices.tiers2.impl.pm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.common.NpaEtLocalite;
import ch.vd.uniregctb.common.RueEtNumero;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Etablissement;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Mandat;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.Pays;
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
import ch.vd.uniregctb.webservices.common.NoOfsTranslator;
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
import ch.vd.uniregctb.webservices.tiers2.data.TiersId;
import ch.vd.uniregctb.webservices.tiers2.data.TiersInfo;
import ch.vd.uniregctb.webservices.tiers2.data.TiersPart;
import ch.vd.uniregctb.webservices.tiers2.exception.AccessDeniedException;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.exception.TechnicalException;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.RangeHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.RangeImpl;
import ch.vd.uniregctb.webservices.tiers2.params.AllConcreteTiersClasses;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetDebiteurInfo;
import ch.vd.uniregctb.webservices.tiers2.params.GetListeCtbModifies;
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
public class TiersWebServiceWithPM implements TiersWebService {

	private static final int OPTIONALITE_COMPLEMENT = 1;

	private TiersDAO tiersDAO;
	private TiersWebService target;
	private ServicePersonneMoraleService servicePM;
	private ServiceCivilService serviceCivil;
	private ServiceInfrastructureService serviceInfra;
	private NoOfsTranslator noOfsTranslator;

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

	public void setNoOfsTranslator(NoOfsTranslator translator) {
		noOfsTranslator = translator;
	}

	@Override
	public List<TiersInfo> searchTiers(SearchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		return target.searchTiers(params);
	}

	@Override
	@Transactional(readOnly = true)
	public Tiers.Type getTiersType(GetTiersType params) throws BusinessException, AccessDeniedException, TechnicalException {
		if (isEntreprise(params.tiersNumber)) {
			return existsPM(params.tiersNumber) ? Tiers.Type.PERSONNE_MORALE : null;
		}
		else {
			return target.getTiersType(params);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Tiers getTiers(GetTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		if (isEntreprise(params.tiersNumber)) {
			final PersonneMoraleHisto pmHisto = getPmHisto(params.tiersNumber, params.parts);
			return pmHisto == null ? null : getAt(pmHisto, params.date);
		}
		else {
			return target.getTiers(params);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public TiersHisto getTiersPeriode(GetTiersPeriode params) throws BusinessException, AccessDeniedException, TechnicalException {
		if (isEntreprise(params.tiersNumber)) {
			final PersonneMoraleHisto pmHisto = getPmHisto(params.tiersNumber, params.parts);
			return pmHisto == null ? null : getAt(pmHisto, params.periode);
		}
		else {
			return target.getTiersPeriode(params);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public TiersHisto getTiersHisto(GetTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {
		if (isEntreprise(params.tiersNumber)) {
			return getPmHisto(params.tiersNumber, params.parts);
		}
		else {
			return target.getTiersHisto(params);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public BatchTiers getBatchTiers(GetBatchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {

		if (params.tiersNumbers == null || params.tiersNumbers.isEmpty()) {
			return new BatchTiers();
		}

		// sépare les ids PP des ids PM
		final Set<Long> idsPP = new HashSet<Long>();
		final Set<Long> idsPM = new HashSet<Long>();
		for (Long id : params.tiersNumbers) {
			if (id != null && isEntreprise(id)) {
				idsPM.add(id);
			}
			else {
				idsPP.add(id);
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
			batchPM = new BatchTiers();
			for (Long id : idsPM) {
				final PersonneMoraleHisto pmHisto = getPmHisto(id, params.parts);
				if (pmHisto != null) {
					final PersonneMorale pm = getAt(pmHisto, params.date);
					final BatchTiersEntry entry = new BatchTiersEntry();
					entry.number = id;
					entry.tiers = pm;
					batchPM.entries.add(entry);
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
			batchPP.entries.addAll(batchPM.entries);
			batch = batchPP;
		}

		return batch;
	}

	@Override
	@Transactional(readOnly = true)
	public BatchTiersHisto getBatchTiersHisto(GetBatchTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {

		if (params.tiersNumbers == null || params.tiersNumbers.isEmpty()) {
			return new BatchTiersHisto();
		}

		// sépare les ids PP des ids PM
		final Set<Long> idsPP = new HashSet<Long>();
		final Set<Long> idsPM = new HashSet<Long>();
		for (Long id : params.tiersNumbers) {
			if (isEntreprise(id)) {
				idsPM.add(id);
			}
			else {
				idsPP.add(id);
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
			batchPM = new BatchTiersHisto();
			for (Long id : idsPM) {
				final PersonneMoraleHisto pmHisto = getPmHisto(id, params.parts);
				if (pmHisto != null) {
					final BatchTiersHistoEntry entry = new BatchTiersHistoEntry();
					entry.number = id;
					entry.tiers = pmHisto;
					batchPM.entries.add(entry);
				}
			}
		}

		// fusion des données PP et PM si nécessaire
		BatchTiersHisto batch = null;
		if (batchPP != null && batchPM == null) {
			batch = batchPP;
		}
		else if (batchPP == null && batchPM != null) {
			batch = batchPM;
		}
		else if (batchPP != null) {
			batchPP.entries.addAll(batchPM.entries);
			batch = batchPP;
		}

		return batch;
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void setTiersBlocRembAuto(SetTiersBlocRembAuto params) throws BusinessException, AccessDeniedException, TechnicalException {
		
		if (isEntreprise(params.tiersNumber) && !tiersDAO.exists(params.tiersNumber)) {
			// [UNIREG-2040] on crée l'entreprise à la volée
			Entreprise e = new Entreprise();
			e.setNumero(params.tiersNumber);
			tiersDAO.save(e);
		}

		target.setTiersBlocRembAuto(params);
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<EvenementPM> searchEvenementsPM(final SearchEvenementsPM params) throws BusinessException, AccessDeniedException, TechnicalException {

		final List<ch.vd.uniregctb.interfaces.model.EvenementPM> list =
				servicePM.findEvenements(params.tiersNumber, params.codeEvenement, DataHelper.webToCore(params.dateMinimale), DataHelper.webToCore(params.dateMaximale));
		return events2web(list);
	}

	@Override
	public DebiteurInfo getDebiteurInfo(GetDebiteurInfo params) throws
			BusinessException, AccessDeniedException, TechnicalException {
		return target.getDebiteurInfo(params);
	}

	@Override
	public List<ReponseQuittancementDeclaration> quittancerDeclarations(QuittancerDeclarations params) throws BusinessException, AccessDeniedException, TechnicalException {
		return target.quittancerDeclarations(params);
	}

	@Override
	public List<TiersId> getListeCtbModifies(GetListeCtbModifies params) throws BusinessException,	AccessDeniedException, TechnicalException {
		return target.getListeCtbModifies(params);
	}

	@Override
	public void doNothing(AllConcreteTiersClasses dummy) {
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

	private PersonneMoraleHisto getPmHisto(long tiersNumber, Set<TiersPart> parts) {

		final ch.vd.uniregctb.interfaces.model.PersonneMorale pmHost = servicePM.getPersonneMorale(tiersNumber, web2business(parts));
		if (pmHost == null) {
			return null;
		}

		if (parts == null) {
			parts = Collections.emptySet();
		}

		final PersonneMoraleHisto pmHisto = new PersonneMoraleHisto();
		pmHisto.numero = pmHost.getNumeroEntreprise();
		pmHisto.numeroTelProf = pmHost.getTelephoneContact();
		pmHisto.numeroTelecopie = pmHost.getTelecopieContact();

		pmHisto.personneContact = pmHost.getNomContact();
		pmHisto.designationAbregee = pmHost.getDesignationAbregee();
		pmHisto.raisonSociale1 = pmHost.getRaisonSociale1();
		pmHisto.raisonSociale2 = pmHost.getRaisonSociale2();
		pmHisto.raisonSociale3 = pmHost.getRaisonSociale3();
		pmHisto.dateDebutActivite = DataHelper.coreToWeb(pmHost.getDateConstitution());
		pmHisto.dateFinActivite = DataHelper.coreToWeb(pmHost.getDateFinActivite());
		pmHisto.dateBouclementFutur = DataHelper.coreToWeb(pmHost.getDateBouclementFuture());
		pmHisto.numeroIPMRO = pmHost.getNumeroIPMRO();

		// [UNIREG-2040] on va chercher l'information de blocage dans notre base si elle existe
		final ch.vd.uniregctb.tiers.Tiers tiers = tiersDAO.get(tiersNumber);
		if (tiers != null) {
			pmHisto.blocageRemboursementAutomatique = tiers.getBlocageRemboursementAutomatique();
		}

		if (parts.contains(TiersPart.ADRESSES) || parts.contains(TiersPart.ADRESSES_ENVOI)) {
			final Collection<ch.vd.uniregctb.interfaces.model.AdresseEntreprise> adresses = pmHost.getAdresses();
			if (adresses != null) {
				for (ch.vd.uniregctb.interfaces.model.AdresseEntreprise a : adresses) {
					if (a.getType() == TypeAdressePM.COURRIER) {
						if (pmHisto.adressesCourrier == null) {
							pmHisto.adressesCourrier = new ArrayList<Adresse>();
							pmHisto.adressesRepresentation = pmHisto.adressesCourrier;
						}
						pmHisto.adressesCourrier.add(host2web(a));
					}
					else if (a.getType() == TypeAdressePM.SIEGE) {
						if (pmHisto.adressesDomicile == null) {
							pmHisto.adressesDomicile = new ArrayList<Adresse>();
							// [UNIREG-1808] les adresses de poursuite des PMs sont déterminées à partir des adresses siège, en attendant des évolutions dans le host.
							pmHisto.adressesPoursuite = pmHisto.adressesDomicile;
						}
						pmHisto.adressesDomicile.add(host2web(a));
					}
					else if (a.getType() == TypeAdressePM.FACTURATION) {
						// ces adresses sont ignorées
					}
					else {
						throw new IllegalArgumentException("Type d'adresse entreprise inconnue = [" + a.getType() + "]");
					}
				}
			}
		}

		if (parts.contains(TiersPart.ADRESSES_ENVOI)) {
			pmHisto.adresseEnvoi = calculateAdresseEnvoi(tiers, pmHisto, pmHisto.adressesCourrier);
			pmHisto.adresseDomicileFormattee = calculateAdresseEnvoi(tiers, pmHisto, pmHisto.adressesDomicile);
			pmHisto.adresseRepresentationFormattee = calculateAdresseEnvoi(tiers, pmHisto, pmHisto.adressesRepresentation);
			pmHisto.adressePoursuiteFormattee = calculateAdresseEnvoi(tiers, pmHisto, pmHisto.adressesPoursuite);
		}

		if (parts.contains(TiersPart.ASSUJETTISSEMENTS)) {
			pmHisto.assujettissementsLIC = assujettissements2web(pmHost.getAssujettissementsLIC());
			pmHisto.assujettissementsLIFD = assujettissements2web(pmHost.getAssujettissementsLIFD());
		}

		if (parts.contains(TiersPart.CAPITAUX)) {
			pmHisto.capitaux = capitaux2web(pmHost.getCapitaux());
		}

		if (parts.contains(TiersPart.COMPTES_BANCAIRES)) {
			pmHisto.comptesBancaires = calculateComptesBancaires(pmHost);
		}
		
		if (parts.contains(TiersPart.ETATS_PM)) {
			pmHisto.etats = etats2web(pmHost.getEtats());
		}

		if (parts.contains(TiersPart.FORMES_JURIDIQUES)) {
			pmHisto.formesJuridiques = formes2web(pmHost.getFormesJuridiques());
		}

		if (parts.contains(TiersPart.FORS_FISCAUX) || parts.contains(TiersPart.FORS_FISCAUX_VIRTUELS)) {
			pmHisto.forsFiscauxPrincipaux = forsPrincipaux2web(pmHost.getForsFiscauxPrincipaux());
			pmHisto.autresForsFiscaux = forsSecondaires2web(pmHost.getForsFiscauxSecondaires());
		}

		if (parts.contains(TiersPart.REGIMES_FISCAUX)) {
			pmHisto.regimesFiscauxICC = regimes2web(pmHost.getRegimesVD());
			pmHisto.regimesFiscauxIFD = regimes2web(pmHost.getRegimesCH());
		}

		if (parts.contains(TiersPart.SIEGES)) {
			pmHisto.sieges = sieges2web(pmHost.getSieges());
		}

		return pmHisto;
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
					cb.numeroTiersTitulaire = m.getNumeroMandataire();

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

	private void fillCompteBancaireDepuisMandataireEtablissement(CompteBancaire cb, long noEtablissement) {
		final Etablissement etablissement = servicePM.getEtablissement(noEtablissement);
		if (etablissement != null) {
			cb.titulaire = etablissement.getEnseigne();
			fillCompteBancaire(cb, etablissement.getIBAN(), etablissement.getCompteBancaire(), etablissement.getCCP(), etablissement.getBicSwift(), etablissement.getNomInstitutionFinanciere());
		}
	}

	private void fillCompteBancaireDepuisMandatairePersonneMorale(CompteBancaire cb, long noPM) {
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

	private void fillCompteBancaireDepuisMandataireIndividu(CompteBancaire cb, long noIndividu) {
		final Individu individu = serviceCivil.getIndividu(noIndividu, null);
		if (individu != null) {
			cb.titulaire = serviceCivil.getNomPrenom(individu);
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

	private List<EvenementPM> events2web(List<ch.vd.uniregctb.interfaces.model.EvenementPM> events) {
		if (events == null || events.isEmpty()) {
			return null;
		}
		final List<EvenementPM> list = new ArrayList<EvenementPM>();
		for (ch.vd.uniregctb.interfaces.model.EvenementPM e : events) {
			EvenementPM event = new EvenementPM();
			event.tiersNumber = e.getNumeroPM();
			event.dateEvenement = DataHelper.coreToWeb(e.getDate());
			event.codeEvenement = e.getCode();
			list.add(event);
		}
		return list;
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
		siege.dateDebut = DataHelper.coreToWeb(s.getDateDebut());
		siege.dateFin = DataHelper.coreToWeb(s.getDateFin());
		siege.noOfsSiege = noOfsTranslator.translateCommune(s.getNoOfsSiege());
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
		regime.dateDebut = DataHelper.coreToWeb(r.getDateDebut());
		regime.dateFin = DataHelper.coreToWeb(r.getDateFin());
		regime.code = r.getCode();
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
		ffs.dateOuverture = DataHelper.coreToWeb(f.getDateDebut());
		ffs.dateFermeture = DataHelper.coreToWeb(f.getDateFin());
		ffs.genreImpot = ForFiscal.GenreImpot.BENEFICE_CAPITAL;
		ffs.motifRattachement = ForFiscal.MotifRattachement.ETABLISSEMENT_STABLE;
		ffs.noOfsAutoriteFiscale = noOfsTranslator.translateCommune(f.getNoOfsAutoriteFiscale());
		ffs.typeAutoriteFiscale = ForFiscal.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD; // par définition
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
		ffp.dateOuverture = DataHelper.coreToWeb(f.getDateDebut());
		ffp.dateFermeture = DataHelper.coreToWeb(f.getDateFin());
		ffp.genreImpot = ForFiscal.GenreImpot.BENEFICE_CAPITAL;
		ffp.motifRattachement = ForFiscal.MotifRattachement.DOMICILE;
		ffp.typeAutoriteFiscale = host2web(f.getTypeAutoriteFiscale(), f.getNoOfsAutoriteFiscale());
		if (ffp.typeAutoriteFiscale != ForFiscal.TypeAutoriteFiscale.PAYS_HS) {
			ffp.noOfsAutoriteFiscale = noOfsTranslator.translateCommune(f.getNoOfsAutoriteFiscale());
		}
		else {
			ffp.noOfsAutoriteFiscale = f.getNoOfsAutoriteFiscale();
		}
		return ffp;
	}

	private ForFiscal.TypeAutoriteFiscale host2web(TypeNoOfs type, int noOfs) {
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
				return ForFiscal.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
			}
			else {
				return ForFiscal.TypeAutoriteFiscale.COMMUNE_HC;
			}
		case PAYS_HS:
			return ForFiscal.TypeAutoriteFiscale.PAYS_HS;
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
			list.add(host2web(c));
		}
		return list;
	}

	private static Capital host2web(ch.vd.uniregctb.interfaces.model.Capital c) {
		Assert.notNull(c);
		Capital capital = new Capital();
		capital.dateDebut = DataHelper.coreToWeb(c.getDateDebut());
		capital.dateFin = DataHelper.coreToWeb(c.getDateFin());
		capital.capitalAction = c.getCapitalAction();
		capital.capitalLibere = c.getCapitalLibere();
		capital.editionFosc = host2web(c.getEditionFosc());
		return capital;
	}

	private static Capital.EditionFosc host2web(ch.vd.uniregctb.interfaces.model.EditionFosc e) {
		Assert.notNull(e);
		Capital.EditionFosc edition = new Capital.EditionFosc();
		edition.anneeFosc = e.getAnnee();
		edition.noFosc = e.getNumero();
		return edition;
	}

	private static List<Assujettissement> assujettissements2web(List<ch.vd.uniregctb.interfaces.model.AssujettissementPM> lic) {
		if (lic == null || lic.isEmpty()) {
			return null;
		}
		final ArrayList<Assujettissement> list = new ArrayList<Assujettissement>(lic.size());
		for (ch.vd.uniregctb.interfaces.model.AssujettissementPM a : lic) {
			list.add(host2web(a));
		}
		return list;
	}

	private static Assujettissement host2web(ch.vd.uniregctb.interfaces.model.AssujettissementPM a) {
		Assert.notNull(a);
		Assujettissement assujet = new Assujettissement();
		assujet.dateDebut = DataHelper.coreToWeb(a.getDateDebut());
		assujet.dateFin = DataHelper.coreToWeb(a.getDateFin());
		assujet.type = Assujettissement.TypeAssujettissement.ILLIMITE;
		return assujet;
	}

	private static Adresse host2web(ch.vd.uniregctb.interfaces.model.AdresseEntreprise a) {
		Assert.notNull(a);
		Adresse adresse = new Adresse();
		adresse.dateDebut = DataHelper.coreToWeb(a.getDateDebutValidite());
		adresse.dateFin = DataHelper.coreToWeb(a.getDateFinValidite());
		adresse.titre = a.getComplement();
		adresse.rue = a.getRue();
		adresse.noRue = a.getNumeroTechniqueRue();
		adresse.numeroRue = a.getNumeroMaison();
		adresse.numeroPostal = a.getNumeroPostal();
		adresse.localite = a.getLocaliteAbregeMinuscule();
		adresse.noOrdrePostal = a.getNumeroOrdrePostal();
		if (a.getPays() == null) {
			adresse.pays = null;
			adresse.noPays = ServiceInfrastructureService.noOfsSuisse;
		}
		else {
			adresse.pays = a.getPays().getNomMinuscule();
			adresse.noPays = a.getPays().getNoOFS();
		}
		return adresse;
	}

	private PartPM[] web2business(Set<TiersPart> parts) {

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

	private AdresseEnvoi calculateAdresseEnvoi(ch.vd.uniregctb.tiers.Tiers destinataire, PersonneMoraleHisto pm, List<Adresse> adresses) {

		final Adresse adresseFiscale = getAt(adresses, null);
		final RegDate dateDebut = adresseFiscale == null ? null : DataHelper.webToCore(adresseFiscale.getDateDebut());
		final RegDate dateFin = adresseFiscale == null ? null : DataHelper.webToCore(adresseFiscale.getDateFin());

		AdresseEnvoiDetaillee adresse = new AdresseEnvoiDetaillee(destinataire, AdresseGenerique.SourceType.PM, dateDebut, dateFin);

		// [UNIREG-2302]
		adresse.addFormulePolitesse(FormulePolitesse.PERSONNE_MORALE);

		// [UNIREG-1974] On doit utiliser la raison sociale sur 3 lignes dans les adresses d'envoi des PMs (et ne pas prendre la raison sociale abbrégée)
		if (pm.raisonSociale1 != null) {
			adresse.addRaisonSociale(pm.raisonSociale1);
		}

		if (pm.raisonSociale2 != null) {
			adresse.addRaisonSociale(pm.raisonSociale2);
		}

		if (pm.raisonSociale3 != null) {
			adresse.addRaisonSociale(pm.raisonSociale3);
		}

// [UNIREG-1973] il ne faut pas utiliser la personne de contact dans les adresses
//		if (pm.personneContact != null) {
//			adresse.addPourAdresse(pm.personneContact);
//		}

		if (adresseFiscale != null) {

			if (adresseFiscale.titre != null) {
				// [UNIREG-1974] Le complément est optionnel
				adresse.addComplement(adresseFiscale.titre, OPTIONALITE_COMPLEMENT);
			}

			if (adresseFiscale.rue != null) {
				adresse.addRueEtNumero(new RueEtNumero(adresseFiscale.rue, adresseFiscale.numeroRue));
			}

			if (adresseFiscale.numeroPostal != null || adresseFiscale.localite != null) {
				adresse.addNpaEtLocalite(new NpaEtLocalite(adresseFiscale.numeroPostal, adresseFiscale.localite));
			}

			if (adresseFiscale.noPays != null) {
				final Pays pays = serviceInfra.getPays(adresseFiscale.noPays);
				if (pays != null) {
					final TypeAffranchissement typeAffranchissement = serviceInfra.getTypeAffranchissement(adresseFiscale.noPays);
					adresse.addPays(pays, typeAffranchissement);
				}
			}
		}

		return new AdresseEnvoi(adresse);
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

	/**
	 * Construit la vue durant une période fiscale donnée des informations d'une personne morale à partir des informations historiques.
	 *
	 * @param pmHisto la personne morale avec toutes les informations historiques
	 * @param annee   la période fiscale de validité des informations de la personne morale retournées
	 * @return une personne morale
	 */
	private PersonneMoraleHisto getAt(PersonneMoraleHisto pmHisto, int annee) {
		final PersonneMoraleHisto pm;
		pm = new PersonneMoraleHisto();
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

		pm.adressesCourrier = getAllAt(pmHisto.adressesCourrier, annee);
		pm.adressesDomicile = getAllAt(pmHisto.adressesDomicile, annee);
		pm.adressesPoursuite = getAllAt(pmHisto.adressesPoursuite, annee);
		pm.adressesRepresentation = getAllAt(pmHisto.adressesRepresentation, annee);
		pm.assujettissementsLIC = getAllAt(pmHisto.assujettissementsLIC, annee);
		pm.assujettissementsLIFD = getAllAt(pmHisto.assujettissementsLIFD, annee);
		pm.autresForsFiscaux = getAllAt(pmHisto.autresForsFiscaux, annee);
		pm.capitaux = getAllAt(pmHisto.capitaux, annee);
		pm.declarations = getAllAt(pmHisto.declarations, annee);
		pm.etats = getAllAt(pmHisto.etats, annee);
		pm.forsFiscauxPrincipaux = getAllAt(pmHisto.forsFiscauxPrincipaux, annee);
		pm.forsGestions = getAllAt(pmHisto.forsGestions, annee);
		pm.formesJuridiques = getAllAt(pmHisto.formesJuridiques, annee);
		pm.periodesImposition = getAllAt(pmHisto.periodesImposition, annee);
		pm.regimesFiscauxIFD = getAllAt(pmHisto.regimesFiscauxIFD, annee);
		pm.regimesFiscauxICC = getAllAt(pmHisto.regimesFiscauxICC, annee);
		pm.sieges = getAllAt(pmHisto.sieges, annee);

		return pm;
	}

	private static <T extends Range> T getAt(List<T> ranges, @Nullable Date date) {
		T range = null;
		if (ranges != null) {
			for (T r : ranges) {
				if (RangeHelper.isDateInRange(date, r)) {
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
			for (T r : ranges) {
				if (RangeHelper.isDateInRange(date, r)) {
					if (list == null) {
						list = new ArrayList<T>();
					}
					list.add(r);
				}
			}
		}
		return list;
	}

	private static <T extends Range> List<T> getAllAt(List<T> ranges, int annee) {
		final RangeImpl periode = new RangeImpl(new Date(annee, 1, 1), new Date(annee, 12, 31));
		List<T> list = null;
		if (ranges != null) {
			for (T r : ranges) {
				if (RangeHelper.intersect(r, periode)) {
					if (list == null) {
						list = new ArrayList<T>();
					}
					list.add(r);
				}
			}
		}
		return list;
	}
}
