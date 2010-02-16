package ch.vd.uniregctb.webservices.tiers2.impl.pm;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.interfaces.model.*;
import ch.vd.uniregctb.interfaces.service.PartPM;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.webservices.common.NoOfsTranslator;
import ch.vd.uniregctb.webservices.tiers2.TiersWebService;
import ch.vd.uniregctb.webservices.tiers2.data.*;
import ch.vd.uniregctb.webservices.tiers2.data.Adresse;
import ch.vd.uniregctb.webservices.tiers2.data.Capital;
import ch.vd.uniregctb.webservices.tiers2.data.CompteBancaire;
import ch.vd.uniregctb.webservices.tiers2.data.Date;
import ch.vd.uniregctb.webservices.tiers2.data.EtatPM;
import ch.vd.uniregctb.webservices.tiers2.data.EvenementPM;
import ch.vd.uniregctb.webservices.tiers2.data.FormeJuridique;
import ch.vd.uniregctb.webservices.tiers2.data.PersonneMorale;
import ch.vd.uniregctb.webservices.tiers2.data.RegimeFiscal;
import ch.vd.uniregctb.webservices.tiers2.data.Siege;
import ch.vd.uniregctb.webservices.tiers2.exception.AccessDeniedException;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.exception.TechnicalException;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.RangeHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.RangeImpl;
import ch.vd.uniregctb.webservices.tiers2.params.*;

import javax.jws.WebParam;
import java.util.*;

/**
 * Classe qui retourne des données bouchonnées concernant les personnes morales. Les appels concernant les personnes physiques sont simplement délégués plus loin.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class TiersWebServiceWithPM implements TiersWebService {

	private TiersWebService target;
	private ServicePersonneMoraleService servicePM;
	private NoOfsTranslator noOfsTranslator;

	public void setTarget(TiersWebService target) {
		this.target = target;
	}

	public void setServicePM(ServicePersonneMoraleService servicePM) {
		this.servicePM = servicePM;
	}

	public void setNoOfsTranslator(NoOfsTranslator translator) {
		noOfsTranslator = translator;
	}

	public List<TiersInfo> searchTiers(@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2",
			partName = "params", name = "SearchTiers") SearchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		return target.searchTiers(params);
	}

	public Tiers.Type getTiersType(@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2",
			partName = "params", name = "GetTiersType") GetTiersType params) throws BusinessException, AccessDeniedException, TechnicalException {
		if (isEntreprise(params.tiersNumber)) {
			return existsPM(params.tiersNumber) ? Tiers.Type.PERSONNE_MORALE : null;
		}
		else {
			return target.getTiersType(params);
		}
	}

	public Tiers getTiers(@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2",
			partName = "params", name = "GetTiers") GetTiers params) throws BusinessException, AccessDeniedException, TechnicalException {
		if (isEntreprise(params.tiersNumber)) {
			final PersonneMoraleHisto pmHisto = getPmHisto(params.tiersNumber, params.parts);
			return pmHisto == null ? null : getAt(pmHisto, params.date);
		}
		else {
			return target.getTiers(params);
		}
	}

	public TiersHisto getTiersPeriode(@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2",
			partName = "params", name = "GetTiersPeriode") GetTiersPeriode params) throws BusinessException, AccessDeniedException, TechnicalException {
		if (isEntreprise(params.tiersNumber)) {
			final PersonneMoraleHisto pmHisto = getPmHisto(params.tiersNumber, params.parts);
			return pmHisto == null ? null : getAt(pmHisto, params.periode);
		}
		else {
			return target.getTiersPeriode(params);
		}
	}

	public TiersHisto getTiersHisto(@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2",
			partName = "params", name = "GetTiersHisto") GetTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {
		if (isEntreprise(params.tiersNumber)) {
			return getPmHisto(params.tiersNumber, params.parts);
		}
		else {
			return target.getTiersHisto(params);
		}
	}

	public BatchTiers getBatchTiers(@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2",
			partName = "params", name = "GetBatchTiers") GetBatchTiers params) throws BusinessException, AccessDeniedException, TechnicalException {

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
			batchPP.entries.addAll(batchPP.entries);
			batch = batchPP;
		}

		return batch;
	}

	public BatchTiersHisto getBatchTiersHisto(@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2",
			partName = "params", name = "GetBatchTiersHisto") GetBatchTiersHisto params) throws BusinessException, AccessDeniedException, TechnicalException {

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
			batchPP.entries.addAll(batchPP.entries);
			batch = batchPP;
		}

		return batch;
	}

	public void setTiersBlocRembAuto(@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2",
			partName = "params", name = "SetTiersBlocRembAuto") SetTiersBlocRembAuto params) throws BusinessException, AccessDeniedException, TechnicalException {
		target.setTiersBlocRembAuto(params);
	}

	@SuppressWarnings({"unchecked"})
	public List<EvenementPM> searchEvenementsPM(@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2",
			partName = "params", name = "SearchEvenementsPM") final SearchEvenementsPM params) throws BusinessException, AccessDeniedException, TechnicalException {

		final List<ch.vd.uniregctb.interfaces.model.EvenementPM> list =
				servicePM.findEvenements(params.tiersNumber, params.codeEvenement, DataHelper.webToCore(params.dateMaximale), DataHelper.webToCore(params.dateMinimale));
		return events2web(list);
	}

	public void doNothing(AllConcreteTiersClasses dummy) {
	}

	/**
	 * Vérifie l'existence d'une personne morale.
	 *
	 * @param id l'id de la personne morale.
	 * @return <i>vrai</i> si la personne morale existe; <i>faux</i> autrement.
	 */
	public boolean existsPM(Long id) {
		return servicePM.getPersonneMorale(id, new PartPM[]{}) != null;
	}

	/**
	 * @return la liste de tous les ids des PM existantes.
	 */
	public List<Long> getAllIdsPM() {
		return servicePM.getAllIds();
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

		final PersonneMoraleHisto pmHisto = new PersonneMoraleHisto();
		pmHisto.numero = pmHost.getNumeroEntreprise();
		pmHisto.numeroTelProf = pmHost.getTelephoneContact();
		pmHisto.numeroTelecopie = pmHost.getTelecopieContact();
		pmHisto.comptesBancaires = comptes2web(pmHost.getComptesBancaires());

		pmHisto.personneContact = pmHost.getNomContact();
		pmHisto.designationAbregee = pmHost.getDesignationAbregee();
		pmHisto.raisonSociale1 = pmHost.getRaisonSociale1();
		pmHisto.raisonSociale2 = pmHost.getRaisonSociale2();
		pmHisto.raisonSociale3 = pmHost.getRaisonSociale3();
		pmHisto.dateBouclementFutur = DataHelper.coreToWeb(pmHost.getDateBouclementFuture());
		pmHisto.numeroIPMRO = pmHost.getNumeroIPMRO();

		if (parts.contains(TiersPart.ADRESSES) || parts.contains(TiersPart.ADRESSES_ENVOI)) {
			final Collection<ch.vd.uniregctb.interfaces.model.AdresseEntreprise> adresses = pmHost.getAdresses();
			if (adresses != null) {
				for (ch.vd.uniregctb.interfaces.model.AdresseEntreprise a : adresses) {
					final List<Adresse> list;
					if (a.getType().equals(EnumTypeAdresseEntreprise.COURRIER)) {
						if (pmHisto.adressesCourrier == null) {
							pmHisto.adressesCourrier = new ArrayList<Adresse>();
						}
						list = pmHisto.adressesCourrier;
					}
					else if (a.getType().equals(EnumTypeAdresseEntreprise.SIEGE)) {
						if (pmHisto.adressesDomicile == null) {
							pmHisto.adressesDomicile = new ArrayList<Adresse>();
						}
						list = pmHisto.adressesDomicile;
					}
					else if (a.getType().equals(EnumTypeAdresseEntreprise.FACTURATION)) {
						if (pmHisto.adressesPoursuite == null) {
							pmHisto.adressesPoursuite = new ArrayList<Adresse>();
						}
						list = pmHisto.adressesPoursuite;
					}
					else {
						throw new IllegalArgumentException("Type d'adresse entreprise inconnue = [" + a.getType().getName() + "]");
					}
					list.add(host2web(a));
				}
			}
		}

		if (parts.contains(TiersPart.ADRESSES_ENVOI)) {
			pmHisto.adresseEnvoi = calculateAdresseEnvoi(pmHisto, pmHisto.adressesCourrier);
			pmHisto.adresseDomicileFormattee = calculateAdresseEnvoi(pmHisto, pmHisto.adressesDomicile);
			pmHisto.adresseRepresentationFormattee = calculateAdresseEnvoi(pmHisto, pmHisto.adressesRepresentation);
			pmHisto.adressePoursuiteFormattee = calculateAdresseEnvoi(pmHisto, pmHisto.adressesPoursuite);
		}

		if (parts.contains(TiersPart.ASSUJETTISSEMENTS)) {
			pmHisto.assujettissementsLIC = assujettissements2web(pmHost.getAssujettissementsLIC());
			pmHisto.assujettissementsLIFD = assujettissements2web(pmHost.getAssujettissementsLIFD());
		}

		if (parts.contains(TiersPart.CAPITAUX)) {
			pmHisto.capitaux = capitaux2web(pmHost.getCapitaux());
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
		ffp.typeAutoriteFiscale = host2web(f.getTypeAutoriteFiscale());
		if (ffp.typeAutoriteFiscale != ForFiscal.TypeAutoriteFiscale.PAYS_HS) {
			ffp.noOfsAutoriteFiscale = noOfsTranslator.translateCommune(f.getNoOfsAutoriteFiscale());
		}
		else {
			ffp.noOfsAutoriteFiscale = f.getNoOfsAutoriteFiscale();
		}
		return ffp;
	}

	private static ForFiscal.TypeAutoriteFiscale host2web(TypeNoOfs type) {
		switch (type) {
		case COMMUNE_CH:
			// comment distinguer une commune VD d'une commune hors-canton ?
			return ForFiscal.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
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
		adresse.localite = a.getLocaliteCompletMinuscule();
		adresse.noOrdrePostal = a.getNumeroOrdrePostal();
		adresse.pays = (a.getPays() == null ? null : a.getPays().getNomMinuscule());
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
			// toujours renseigné
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

	private AdresseEnvoi calculateAdresseEnvoi(PersonneMoraleHisto pm, List<Adresse> adresses) {
		AdresseEnvoiDetaillee adresse = new AdresseEnvoiDetaillee();

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
				adresse.addPays(adresseFiscale.pays);
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

	private static <T extends Range> T getAt(List<T> ranges, Date date) {
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