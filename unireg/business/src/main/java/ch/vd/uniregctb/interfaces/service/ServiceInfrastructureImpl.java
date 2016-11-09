package ch.vd.uniregctb.interfaces.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.AdresseAvecCommune;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.LogicielMetier;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.tiers.TiersDAO;

/**
 * Service d'infrastructure utilisée par le code métier. Ce service expose toutes les méthodes du service d'infrastructure <i>raw</i> en y ajoutant des méthodes utilitaires.
 */
public class ServiceInfrastructureImpl implements ServiceInfrastructureService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInfrastructureImpl.class);

	private ServiceInfrastructureRaw rawService;
	private TiersDAO tiersDAO;

	/*
	 * Note: on se permet de cacher l'ACI, la Suisse et le canton de Vaud à ce niveau, car il n'y a aucune chance que ces deux objets changent sans
	 * une remise en compte majeure des institutions. Tout autre forme de caching doit être déléguée au ServiceInfrastructureCache.
	 */
	private Pays suisse;
	private Canton vaud;
	private CollectiviteAdministrative aci;
	private CollectiviteAdministrative aciSuccessions;
	private CollectiviteAdministrative aciImpotSource;
	private CollectiviteAdministrative aciOfficeImpotPersonnesMorales;
	private CollectiviteAdministrative cedi;
	private CollectiviteAdministrative cat;
	private CollectiviteAdministrative rc;
	private Map<Integer, List<Localite>> allLocaliteCommune;

	public ServiceInfrastructureImpl() {
	}

	public ServiceInfrastructureImpl(ServiceInfrastructureRaw rawService, TiersDAO tiersDAO) {
		this.rawService = rawService;
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRawService(ServiceInfrastructureRaw rawService) {
		this.rawService = rawService;
		this.suisse = null;
		this.vaud = null;
		this.aci = null;
		this.aciSuccessions = null;
		this.aciImpotSource = null;
		this.aciOfficeImpotPersonnesMorales = null;
		this.cedi = null;
		this.cat = null;
		this.allLocaliteCommune = null;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Canton getCantonBySigle(String sigle) throws ServiceInfrastructureException {
		Canton canton = null;
		for (Canton c : getAllCantons()) {
			if (c.getSigleOFS().equals(sigle)) {
				canton = c;
			}
		}
		if (canton == null) {
			throw new ServiceInfrastructureException("Le canton " + sigle + " n'existe pas");
		}
		return canton;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Commune> getListeCommunesByOID(int oid) throws ServiceInfrastructureException {
		List<Commune> communes = new LinkedList<>();
		for (Commune c : getCommunes()) {
			if (c.isVaudoise()) {
				CollectiviteAdministrative oi = getOfficeImpotDeCommune(c.getNoOFS());
				if (oi != null && oi.getNoColAdm() == oid) {
					communes.add(c);
				}
			}
		}
		return Collections.unmodifiableList(communes);
	}

	@Override
	public List<Commune> getCommunesDeVaud() throws ServiceInfrastructureException {
		final List<Commune> list = new LinkedList<>();
		for (Commune commune : getCommunes()) {
			if (commune.isVaudoise()) {
				list.add(commune);
			}
		}
		return list;
	}

	@Override
	public List<Commune> getCommunesHorsCanton() throws ServiceInfrastructureException {
		final List<Commune> list = new LinkedList<>();
		for (Commune commune : getCommunes()) {
			if (!commune.isVaudoise()) {
				list.add(commune);
			}
		}
		return list;
	}

	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		return rawService.getCommunes();
	}

	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		return rawService.getLocalites();
	}

	private static final class Warner {
		private final ThreadLocal<String> lastMsg = new ThreadLocal<>();
		public void warn(String msg) {
			final String previousMsg = lastMsg.get();
			if (previousMsg == null || !previousMsg.equals(msg)) {
				LOGGER.warn(msg);
				lastMsg.set(msg);
			}
		}
	}
	private final Warner warner = new Warner();

	@Override
	public Localite getLocaliteByONRP(int onrp, RegDate dateReference) throws ServiceInfrastructureException {
		if (dateReference == null) {
			dateReference = RegDate.get();
		}

		final List<Localite> candidates = rawService.getLocalitesByONRP(onrp);
		if (candidates == null || candidates.isEmpty()) {
			LOGGER.warn("Aucune localité trouvée pour le numéro " + onrp);
			return null;
		}
		if (candidates.size() == 1) {
			// il n'y en a qu'une, c'est la bonne
			final Localite seule = candidates.get(0);
			if (!seule.isValidAt(dateReference)) {
				warner.warn("La validité de la seule localité avec le numéro " + onrp + " (" + DateRangeHelper.toDisplayString(seule) + ") a été étendue au " + RegDateHelper.dateToDisplayString(dateReference));
			}
			return seule;
		}

		// si la date correspond exactement à une localité, pas de question à se poser, c'est la bonne
		final Localite exactOne = DateRangeHelper.rangeAt(candidates, dateReference);
		if (exactOne != null) {
			return exactOne;
		}

		// il faut prendre la plus proche... Les localités étant triées dans l'ordre chronologique, on peut facilement trouver la localité avant et celle après
		Localite before = null;
		Localite after = null;
		for (Localite candidate : candidates) {
			if (RegDateHelper.isBefore(candidate.getDateFin(), dateReference, NullDateBehavior.LATEST)) {
				before = candidate;
			}
			else if (RegDateHelper.isAfter(candidate.getDateDebut(), dateReference, NullDateBehavior.EARLIEST)) {
				after = candidate;
				break;      // puisque les candidats sont triés, on peut (= on doit !) s'arrêter là...
			}
		}

		final Localite res;
		if (before == null) {
			res = after;
		}
		else if (after == null) {
			res = before;
		}
		else {
			final int diffBefore = RegDateHelper.getDaysBetween(before.getDateFin(), dateReference) - 1;      // -1 car la date de fin est entièrement comprise dans l'intervale
			final int diffAfter = RegDateHelper.getDaysBetween(dateReference, after.getDateDebut());
			res = (diffBefore < diffAfter ? before : after);
		}
		warner.warn("Localité la plus proche du " + RegDateHelper.dateToDisplayString(dateReference) + " pour le numéro " + onrp + " trouvée pour la période " + DateRangeHelper.toDisplayString(res));
		return res;
	}

	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		return rawService.getPays();
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws ServiceInfrastructureException {
		return rawService.getPays(numeroOFS, date);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pays getPays(String codePays, @Nullable RegDate date) throws ServiceInfrastructureException {
		return rawService.getPays(codePays, date);
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws ServiceInfrastructureException {
		return rawService.getPaysHisto(numeroOFS);
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		return rawService.getCollectivite(noColAdm);
	}

	@Override
	public CollectiviteAdministrative getACI() throws ServiceInfrastructureException {
		if (aci == null) {
			aci = rawService.getCollectivite(ServiceInfrastructureService.noACI);
		}
		return aci;
	}

	@Override
	public CollectiviteAdministrative getACIImpotSource() throws ServiceInfrastructureException {
		if (aciImpotSource == null) {
			aciImpotSource = rawService.getCollectivite(ServiceInfrastructureService.noACIImpotSource);
		}
		return aciImpotSource;
	}

	@Override
	public CollectiviteAdministrative getACIOIPM() throws ServiceInfrastructureException {
		if (aciOfficeImpotPersonnesMorales == null) {
			aciOfficeImpotPersonnesMorales = rawService.getCollectivite(ServiceInfrastructureService.noOIPM);
		}
		return aciOfficeImpotPersonnesMorales;
	}

	@Override
	public CollectiviteAdministrative getCEDI() throws ServiceInfrastructureException {
		if (cedi == null) {
			cedi = rawService.getCollectivite(ServiceInfrastructureService.noCEDI);
		}
		return cedi;
	}

	@Override
	public CollectiviteAdministrative getCAT() throws ServiceInfrastructureException {
		if (cat == null) {
			cat = rawService.getCollectivite(ServiceInfrastructureService.noCAT);
		}
		return cat;
	}

	@Override
	public CollectiviteAdministrative getRC() throws ServiceInfrastructureException {
		if (rc == null) {
			rc = rawService.getCollectivite(ServiceInfrastructureService.noRC);
		}
		return rc;
	}

	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		return rawService.getAllCantons();
	}

	@Override
	public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
		return rawService.getListeFractionsCommunes();
	}

	/**
	 * Si la collection de candidats ne contient aucun élément, renvoie <code>null</code>, si elle contient 1 élément, renvoie celui-là, et si elle contient plus d'un élément, renvoie le premier élément
	 * trouvé valide à la date donnée (<code>null</code> si aucun n'est valide à la date donnée).
	 *
	 * @param candidats    liste des communes potentielles
	 * @param dateValidite date déterminante en cas de possibilités multiples
	 * @return une commune
	 */
	private static Commune choisirCommune(List<Commune> candidats, RegDate dateValidite) {
		Commune resultat = null;
		if (candidats != null && !candidats.isEmpty()) {
			if (candidats.size() == 1) {
				resultat = candidats.get(0);
			}
			else {
				// date de validité de chacune des communes...
				for (Commune commune : candidats) {
					final DateRange range = new DateRangeHelper.Range(commune.getDateDebutValidite(), commune.getDateFinValidite());
					if (range.isValidAt(dateValidite)) {
						resultat = commune;
						break;
					}
				}
			}
		}
		return resultat;
	}

	@Override
	public synchronized List<Localite> getLocaliteByCommune(int commune) throws ServiceInfrastructureException {
		if (allLocaliteCommune == null) {
			allLocaliteCommune = new HashMap<>();
		}
		List<Localite> list = allLocaliteCommune.get(commune);
		if (list == null) {
			list = new LinkedList<>();
			for (Localite loc : getLocalites()) {
				if (loc.getNoCommune() != null && loc.getNoCommune() == commune) {
					list.add(loc);
				}
			}
			allLocaliteCommune.put(commune, list);
		}
		return list;
	}

	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		return rawService.getRues(localite);
	}

	private Commune getCommuneByLocaliteAdresse(Integer numeroOrdrePostal, RegDate date) throws ServiceInfrastructureException {

		// Recherche de la commune
		final Commune commune;

		if (numeroOrdrePostal == null) {
			// adresse hors-Suisse
			commune = null;
		}
		else {
			final Localite localite = getLocaliteByONRP(numeroOrdrePostal, date);
			if (localite == null) {
				throw new ServiceInfrastructureException("Aucune localité trouvée avec le numéro " + numeroOrdrePostal);
			}
			commune = getCommuneByLocalite(localite);
		}

		return commune;
	}

	/**
	 * Récupère la commune attachée à une adresse, et si aucune n'est présente, ou si la commune attachée est fractionnée, déduit la commune de la localité déterminée par un numéro de rue (si disponible)
	 * ou un numéro d'ordre poste
	 *
	 *
	 * @param adresse           une adresse
	 * @param numeroOrdrePostal un numéro d'ordre postal
	 * @param date              la date de référence
	 * @return la commune qui correspond à l'adresse spécifiée; ou <b>null</b> si aucune commune n'a été trouvée.
	 * @throws ServiceInfrastructureException en cas d'erreur
	 */
	private Commune getCommuneByAdresse(AdresseAvecCommune adresse, Integer numeroOrdrePostal, RegDate date) throws ServiceInfrastructureException {
		if (adresse == null) {
			return null;
		}

		Commune commune = null;

		// 1er choix : l'egid
		final Integer egid = adresse.getEgid();
		if (egid != null) {
			commune = getCommuneByEgid(egid, date);
		}

		// 2ème choix : la commune attachée à l'adresse
		if (commune == null) {
			final Integer noOfs = adresse.getNoOfsCommuneAdresse();
			if (noOfs != null) {
				final Commune candidate = getCommuneByNumeroOfs(noOfs, date);
				// si la commune est attachée et que ce n'est pas une commune fractionnée, on la prend
				// sinon, on prend l'adresse depuis la localité
				if (candidate != null && !candidate.isPrincipale()) {
					commune = candidate;
				}
			}
		}

		// 3ème choix : la commune associée à la localité
		if (commune == null) {
			commune = getCommuneByLocaliteAdresse(numeroOrdrePostal, date);
		}

		return commune;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Commune getCommuneByAdresse(Adresse adresse, RegDate date) throws ServiceInfrastructureException {
		if (adresse != null) {
			return getCommuneByAdresse(adresse, adresse.getNumeroOrdrePostal(), date);
		}
		else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Commune getCommuneByAdresse(AdresseGenerique adresse, RegDate date) throws ServiceInfrastructureException {
		if (adresse != null) {
			return getCommuneByAdresse(adresse, adresse.getNumeroOrdrePostal(), date);
		}
		else {
			return null;
		}
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {
		return rawService.getNoOfsCommuneByEgid(egid, date);
	}

	@Override
	public final Commune getCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {

		// un premier appel pour récupérer le numéro Ofs de la commune
		final Integer noOfs = getNoOfsCommuneByEgid(egid, date);
		if (noOfs == null) {
			return null;
		}

		// un second appel où il y a beaucoup de chances de trouver la commune dans le cache
		return getCommuneByNumeroOfs(noOfs, date);
	}

	@Override
	public Commune getCommuneFaitiere(Commune commune, RegDate dateReference) throws ServiceInfrastructureException {
		if (commune == null || !commune.isFraction()) {
			return commune;
		}

		//
		// C'est bidon ici !! on confond idTechnique et numéro OFS, mais on a de la chance : pour les communes
		// faîtières des fractions vaudoises, c'est la même chose...
		//
		final int idCommuneMere = commune.getOfsCommuneMere();
		return getCommuneByNumeroOfs(idCommuneMere, dateReference);
	}

	@Override
	public OfficeImpot getOfficeImpot(int noColAdm) throws ServiceInfrastructureException {
		final CollectiviteAdministrative coll = rawService.getCollectivite(noColAdm);
		if (coll instanceof OfficeImpot) {
			return (OfficeImpot) coll;
		}
		else {
			return null;
		}
	}

	@Override
	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws ServiceInfrastructureException {
		final Commune commune = getCommuneByNumeroOfs(noCommune, RegDate.get());
		final Integer codeDistrict = commune.getCodeDistrict();
		final OfficeImpot oid;
		if (codeDistrict != null) {
			final ch.vd.uniregctb.tiers.CollectiviteAdministrative collAdm = tiersDAO.getCollectiviteAdministrativeForDistrict(codeDistrict, true);
			if (collAdm != null) {
				oid = getOfficeImpot(collAdm.getNumeroCollectiviteAdministrative());
			}
			else {
				throw new ServiceInfrastructureException("La collectivité administrative du district " + codeDistrict + " pour la commune "  + noCommune + " manque à l'appel!");
			}
		}
		else {
			// vieille commune vaudoise sans district, commune hors-canton...
			oid = null;
		}
		return oid;
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		return rawService.getOfficesImpot();
	}

	@Override
	public List<Rue> getRues(Collection<Localite> localites) throws ServiceInfrastructureException {
		List<Rue> locRues = new LinkedList<>();
		for (Localite localite : localites) {
			locRues.addAll(getRues(localite));
		}
		return locRues;
	}

	@Override
	public Rue getRueByNumero(int numero) throws ServiceInfrastructureException {
		final List<Rue> histo = rawService.getRuesHisto(numero);
		if (histo != null && !histo.isEmpty()) {
			final List<Rue> sorted = new ArrayList<>(histo);
			Collections.sort(sorted, new DateRangeComparator<>(DateRangeComparator.CompareOrder.DESCENDING));
			return sorted.get(0);        // = la plus récente
		}
		return null;
	}

	@Override
	public Pays getSuisse() throws ServiceInfrastructureException {
		if (suisse == null) {
			suisse = getPays(ServiceInfrastructureService.noOfsSuisse, null);
		}
		return suisse;
	}

	@Override
	public Canton getVaud() throws ServiceInfrastructureException {
		if (vaud == null) {
			vaud = getCantonBySigle(ServiceInfrastructureService.SIGLE_CANTON_VD);
		}
		return vaud;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Canton getCantonByCommune(int noOfsCommune) throws ServiceInfrastructureException {
		final Commune commune = getCommuneByNumeroOfs(noOfsCommune, null);
		if (commune == null) {
			throw new ServiceInfrastructureException("La commune avec le numéro Ofs " + noOfsCommune + " n'existe pas");
		}
		final String canton = commune.getSigleCanton();
		return getCantonBySigle(canton);
	}

	@Override
	public Commune getCommuneByNumeroOfs(int noCommune, @Nullable RegDate date) throws ServiceInfrastructureException {
		final List<Commune> list = getCommuneHistoByNumeroOfs(noCommune);
		return choisirCommune(list, date);
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		return rawService.getCommuneHistoByNumeroOfs(noOfsCommune);
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
		return rawService.getCommuneByLocalite(localite);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Canton getCanton(int cantonOFS) throws ServiceInfrastructureException {
		Canton canton = null;
		for (Canton c : getAllCantons()) {
			if (c.getNoOFS() == cantonOFS) {
				canton = c;
			}
		}
		if (canton == null) {
			throw new ServiceInfrastructureException("Le canton " + cantonOFS + " n'existe pas");
		}
		return canton;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean estDansLeCanton(final Rue rue) throws ServiceInfrastructureException {
		final Integer onrp = rue.getNoLocalite();
		final Localite localite = getLocaliteByONRP(onrp, rue.getDateFin());
		return estDansLeCanton(localite.getCommuneLocalite());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean estDansLeCanton(final Commune commune) throws ServiceInfrastructureException {
		final String sigle = commune.getSigleCanton();
		if (sigle == null || sigle.isEmpty()) {
			final int noOfs = commune.getNoOFS();
			final Canton canton = getCantonByCommune(noOfs);
			return getVaud().equals(canton);
		}
		else {
			return SIGLE_CANTON_VD.equals(sigle);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean estDansLeCanton(AdresseGenerique adresse) throws ServiceInfrastructureException {

		if (!estEnSuisse(adresse)) {
			return false;
		}

		final Integer numero = adresse.getNumeroRue();
		if (numero == null || numero == 0) {
			final Integer onrp = adresse.getNumeroOrdrePostal();
			if (onrp == null) {
				return false;
			}
			final Localite localite = getLocaliteByONRP(onrp, adresse.getDateFin());
			Assert.notNull(localite, "Aucune localité trouvée avec le numéro " + onrp);
			return estDansLeCanton(localite.getCommuneLocalite());
		}
		else {
			final Rue rue = getRueByNumero(numero);
			return estDansLeCanton(rue);
		}
	}

	@Override
	public boolean estDansLeCanton(Adresse adresse) throws ServiceInfrastructureException {

		if (!estEnSuisse(adresse)) {
			return false;
		}

		final Integer numero = adresse.getNumeroRue();
		if (numero == null || numero == 0) {
			final Integer noOfs = adresse.getNoOfsCommuneAdresse();
			final Commune commune = (noOfs == null ? null : getCommuneByNumeroOfs(noOfs, adresse.getDateDebut()));
			return commune != null && estDansLeCanton(commune);
		}
		else {
			final Rue rue = getRueByNumero(numero);
			return estDansLeCanton(rue);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean estEnSuisse(AdresseGenerique adresse) throws ServiceInfrastructureException {
		if (adresse == null) {
			throw new ServiceInfrastructureException("L'adresse est nulle");
		}
		final Integer noOfsPays = adresse.getNoOfsPays();
		return noOfsPays == null || noOfsPays == noOfsSuisse; // par défaut, un pays non-renseigné correspond à la Suisse
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean estEnSuisse(Adresse adresse) throws ServiceInfrastructureException {
		if (adresse == null) {
			throw new ServiceInfrastructureException("L'adresse est nulle");
		}
		final Integer noOfsPays = adresse.getNoOfsPays();
		return noOfsPays == null || noOfsPays == noOfsSuisse; // par défaut, un pays non-renseigné correspond à la Suisse
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public Zone getZone(AdresseGenerique adresse) throws ServiceInfrastructureException {

		if (estEnSuisse(adresse)) {
			if (estDansLeCanton(adresse)) {
				return Zone.VAUD;
			}
			else {
				return Zone.HORS_CANTON;
			}
		}
		else {
			return Zone.HORS_SUISSE;
		}
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		return rawService.getCollectivitesAdministratives();
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {
		return rawService.getCollectivitesAdministratives(typesCollectivite);
	}

	@Override
	public Pays getPaysInconnu() throws ServiceInfrastructureException {
		return getPays(ServiceInfrastructureService.noPaysInconnu, null);
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {
		return rawService.getInstitutionFinanciere(id);
	}

	@Override
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
		return rawService.getInstitutionsFinancieres(noClearing);
	}

	@Override
	public List<TypeRegimeFiscal> getRegimesFiscaux() throws ServiceInfrastructureException {
		return rawService.getTousLesRegimesFiscaux();
	}

	@Override
	public List<GenreImpotMandataire> getGenresImpotMandataires() throws ServiceInfrastructureException {
		return rawService.getTousLesGenresImpotMandataires();
	}

	@Override
	public List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws ServiceInfrastructureException {
		return rawService.getLocalitesByNPA(npa, dateReference);
	}

	@Override
	public String getUrlVers(ApplicationFiscale application, Long tiersId) {
		return rawService.getUrlVers(application, tiersId, AuthenticationHelper.getCurrentOID());
	}

	@Override
	public Logiciel getLogiciel(Long idLogiciel) {
		return rawService.getLogiciel(idLogiciel);
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() {
		return rawService.getTousLesLogiciels();
	}

	@Override
	public List<Logiciel> getLogicielsPour(LogicielMetier metier) {
		final List<Logiciel> list = new LinkedList<>();
		for (Logiciel l : getTousLesLogiciels()) {
			if (l.getMetier() == metier) {
				list.add(l);
			}
		}
		return list;
	}

	@Override
	public List<Logiciel> getLogicielsCertifiesPour(LogicielMetier metier) {
		final List<Logiciel> list = new LinkedList<>();
		for (Logiciel l : getLogicielsPour(metier)) {
			if (l.isCertifie()) {
				list.add(l);
			}
		}
		return list;
	}
}
