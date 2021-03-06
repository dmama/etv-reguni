package ch.vd.unireg.interfaces.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.common.AdresseAvecCommune;
import ch.vd.unireg.interfaces.infra.InfrastructureConnector;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.LogicielMetier;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.tiers.TiersDAO;

/**
 * Service d'infrastructure utilisée par le code métier. Ce service expose toutes les méthodes du connecteur d'infrastructure en y ajoutant des méthodes utilitaires.
 */
public class ServiceInfrastructureImpl implements ServiceInfrastructureService {

	//private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInfrastructureImpl.class);

	private InfrastructureConnector connector;
	private TiersDAO tiersDAO;

	/*
	 * Note: on se permet de cacher la Suisse et le canton de Vaud à ce niveau, car il n'y a aucune chance que ces deux objets changent sans
	 * une remise en compte majeure des institutions. Tout autre forme de caching doit être déléguée au InfrastructureConnectorCache.
	 */
	private Pays suisse;
	private Canton vaud;

	public ServiceInfrastructureImpl() {
	}

	public ServiceInfrastructureImpl(InfrastructureConnector connector, TiersDAO tiersDAO) {
		this.connector = connector;
		this.tiersDAO = tiersDAO;
	}

	public void setConnector(InfrastructureConnector connector) {
		this.connector = connector;
		this.suisse = null;
		this.vaud = null;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public Canton getCantonBySigle(String sigle) throws InfrastructureException {
		Canton canton = null;
		for (Canton c : getAllCantons()) {
			if (c.getSigleOFS().equals(sigle)) {
				canton = c;
			}
		}
		if (canton == null) {
			throw new InfrastructureException("Le canton " + sigle + " n'existe pas");
		}
		return canton;
	}

	@Override
	public List<Commune> getListeCommunesByOID(int oid) throws InfrastructureException {
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
	public List<Commune> getCommunesDeVaud() throws InfrastructureException {
		final List<Commune> list = new LinkedList<>();
		for (Commune commune : getCommunes()) {
			if (commune.isVaudoise()) {
				list.add(commune);
			}
		}
		return list;
	}

	@Override
	public List<Commune> getCommunesHorsCanton() throws InfrastructureException {
		final List<Commune> list = new LinkedList<>();
		for (Commune commune : getCommunes()) {
			if (!commune.isVaudoise()) {
				list.add(commune);
			}
		}
		return list;
	}

	@Override
	public List<Commune> getCommunes() throws InfrastructureException {
		return connector.getCommunes();
	}

	@Override
	public List<Localite> getLocalites() throws InfrastructureException {
		return connector.getLocalites();
	}

	@Override
	public Localite getLocaliteByONRP(int onrp, RegDate dateReference) throws InfrastructureException {
		return connector.getLocaliteByONRP(onrp, dateReference);
	}

	@Override
	public List<Pays> getPays() throws InfrastructureException {
		return connector.getPays();
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws InfrastructureException {
		return connector.getPays(numeroOFS, date);
	}

	@Override
	public Pays getPays(String codePays, @Nullable RegDate date) throws InfrastructureException {
		return connector.getPays(codePays, date);
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws InfrastructureException {
		return connector.getPaysHisto(numeroOFS);
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException {
		return connector.getCollectivite(noColAdm);
	}

	@Override
	public CollectiviteAdministrative getACI() throws InfrastructureException {
		return connector.getCollectivite(ServiceInfrastructureService.noACI);
	}

	@Override
	public CollectiviteAdministrative getACIImpotSource() throws InfrastructureException {
		return connector.getCollectivite(ServiceInfrastructureService.noACIImpotSource);
	}

	@Override
	public CollectiviteAdministrative getACIOIPM() throws InfrastructureException {
		return connector.getCollectivite(ServiceInfrastructureService.noOIPM);
	}

	@Override
	public CollectiviteAdministrative getCEDI() throws InfrastructureException {
		return connector.getCollectivite(ServiceInfrastructureService.noCEDI);
	}

	@Override
	public CollectiviteAdministrative getCAT() throws InfrastructureException {
		return connector.getCollectivite(ServiceInfrastructureService.noCAT);
	}

	@Override
	public CollectiviteAdministrative getRC() throws InfrastructureException {
		return connector.getCollectivite(ServiceInfrastructureService.noRC);
	}

	@Override
	public List<CollectiviteAdministrative> findCollectivitesAdministratives(@NotNull Collection<Integer> codeCollectivites, boolean inactif) {
		return connector.findCollectivitesAdministratives(codeCollectivites, inactif);
	}

	@Override
	public List<Canton> getAllCantons() throws InfrastructureException {
		return connector.getAllCantons();
	}

	@Override
	public List<Commune> getCommunesVD() throws InfrastructureException {
		return connector.getCommunesVD();
	}

	@Override
	public List<Commune> getListeCommunesFaitieres() throws InfrastructureException {
		return connector.getListeCommunesFaitieres();
	}

	/**
	 * Si la collection de candidats ne contient aucun élément, renvoie <code>null</code>, si elle contient 1 élément, renvoie celui-là, et si elle contient plus d'un élément, renvoie le premier élément trouvé valide à la date donnée
	 * (<code>null</code> si aucun n'est valide à la date donnée).
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
	public List<Localite> getLocaliteByCommune(int commune) throws InfrastructureException {
		return connector.getLocalites().stream()  // cet appel est normalement caché
				.filter(loc -> loc.getNoCommune() != null && loc.getNoCommune() == commune)
				.collect(Collectors.toList());
	}

	@Override
	public List<Rue> getRues(Localite localite) throws InfrastructureException {
		return connector.getRues(localite);
	}

	private Commune getCommuneByLocaliteAdresse(Integer numeroOrdrePostal, RegDate date) throws InfrastructureException {

		// Recherche de la commune
		final Commune commune;

		if (numeroOrdrePostal == null) {
			// adresse hors-Suisse
			commune = null;
		}
		else {
			final Localite localite = getLocaliteByONRP(numeroOrdrePostal, date);
			if (localite == null) {
				throw new InfrastructureException("Aucune localité trouvée avec le numéro " + numeroOrdrePostal);
			}
			commune = getCommuneByLocalite(localite);
		}

		return commune;
	}

	/**
	 * Récupère la commune attachée à une adresse, et si aucune n'est présente, ou si la commune attachée est fractionnée, déduit la commune de la localité déterminée par un numéro de rue (si disponible) ou un numéro d'ordre poste
	 *
	 * @param adresse           une adresse
	 * @param numeroOrdrePostal un numéro d'ordre postal
	 * @param date              la date de référence
	 * @return la commune qui correspond à l'adresse spécifiée; ou <b>null</b> si aucune commune n'a été trouvée.
	 * @throws InfrastructureException en cas d'erreur
	 */
	private Commune getCommuneByAdresse(AdresseAvecCommune adresse, Integer numeroOrdrePostal, RegDate date) throws InfrastructureException {
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

	@Override
	public Commune getCommuneByAdresse(Adresse adresse, RegDate date) throws InfrastructureException {
		if (adresse != null) {
			return getCommuneByAdresse(adresse, adresse.getNumeroOrdrePostal(), date);
		}
		else {
			return null;
		}
	}

	@Override
	public Commune getCommuneByAdresse(AdresseGenerique adresse, RegDate date) throws InfrastructureException {
		if (adresse != null) {
			return getCommuneByAdresse(adresse, adresse.getNumeroOrdrePostal(), date);
		}
		else {
			return null;
		}
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws InfrastructureException {
		return connector.getNoOfsCommuneByEgid(egid, date);
	}

	@Override
	public final Commune getCommuneByEgid(int egid, RegDate date) throws InfrastructureException {

		// un premier appel pour récupérer le numéro Ofs de la commune
		final Integer noOfs = getNoOfsCommuneByEgid(egid, date);
		if (noOfs == null) {
			return null;
		}

		// un second appel où il y a beaucoup de chances de trouver la commune dans le cache
		return getCommuneByNumeroOfs(noOfs, date);
	}

	@Override
	public Commune getCommuneFaitiere(Commune commune, RegDate dateReference) throws InfrastructureException {
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
	public OfficeImpot getOfficeImpot(int noColAdm) throws InfrastructureException {
		final CollectiviteAdministrative coll = connector.getCollectivite(noColAdm);
		if (coll instanceof OfficeImpot) {
			return (OfficeImpot) coll;
		}
		else {
			return null;
		}
	}

	@Override
	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws InfrastructureException {
		final Commune commune = getCommuneByNumeroOfs(noCommune, RegDate.get());
		final Integer codeDistrict = commune.getCodeDistrict();
		final OfficeImpot oid;
		if (codeDistrict != null) {
			final ch.vd.unireg.tiers.CollectiviteAdministrative collAdm = tiersDAO.getCollectiviteAdministrativeForDistrict(codeDistrict, true);
			if (collAdm != null) {
				oid = getOfficeImpot(collAdm.getNumeroCollectiviteAdministrative());
			}
			else {
				throw new InfrastructureException("La collectivité administrative du district " + codeDistrict + " pour la commune " + noCommune + " manque à l'appel!");
			}
		}
		else {
			// vieille commune vaudoise sans district, commune hors-canton...
			oid = null;
		}
		return oid;
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException {
		return connector.getOfficesImpot();
	}

	@Override
	public List<Rue> getRues(Collection<Localite> localites) throws InfrastructureException {
		List<Rue> locRues = new LinkedList<>();
		for (Localite localite : localites) {
			locRues.addAll(getRues(localite));
		}
		return locRues;
	}

	@Override
	public Rue getRueByNumero(int numero) throws InfrastructureException {
		final List<Rue> histo = connector.getRuesHisto(numero);
		if (histo != null && !histo.isEmpty()) {
			final List<Rue> sorted = new ArrayList<>(histo);
			sorted.sort(new DateRangeComparator<>(DateRangeComparator.CompareOrder.DESCENDING));
			return sorted.get(0);        // = la plus récente
		}
		return null;
	}

	@Override
	public Pays getSuisse() throws InfrastructureException {
		if (suisse == null) {
			suisse = getPays(ServiceInfrastructureService.noOfsSuisse, null);
		}
		return suisse;
	}

	@Override
	public Canton getVaud() throws InfrastructureException {
		if (vaud == null) {
			vaud = getCantonBySigle(ServiceInfrastructureService.SIGLE_CANTON_VD);
		}
		return vaud;
	}

	@Override
	public Canton getCantonByCommune(int noOfsCommune) throws InfrastructureException {
		final Commune commune = getCommuneByNumeroOfs(noOfsCommune, null);
		if (commune == null) {
			throw new InfrastructureException("La commune avec le numéro Ofs " + noOfsCommune + " n'existe pas");
		}
		final String canton = commune.getSigleCanton();
		return getCantonBySigle(canton);
	}

	@Override
	public Commune getCommuneByNumeroOfs(int noCommune, @Nullable RegDate date) throws InfrastructureException {
		final List<Commune> list = getCommuneHistoByNumeroOfs(noCommune);
		return choisirCommune(list, date);
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws InfrastructureException {
		return connector.getCommuneHistoByNumeroOfs(noOfsCommune);
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException {
		return connector.getCommuneByLocalite(localite);
	}

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws InfrastructureException {
		return connector.findCommuneByNomOfficiel(nomOfficiel, includeFaitieres, includeFractions, date);
	}

	@Override
	public Canton getCanton(int cantonOFS) throws InfrastructureException {
		Canton canton = null;
		for (Canton c : getAllCantons()) {
			if (c.getNoOFS() == cantonOFS) {
				canton = c;
			}
		}
		if (canton == null) {
			throw new InfrastructureException("Le canton " + cantonOFS + " n'existe pas");
		}
		return canton;
	}

	@Override
	public boolean estDansLeCanton(final Rue rue) throws InfrastructureException {
		final Integer onrp = rue.getNoLocalite();
		final Localite localite = getLocaliteByONRP(onrp, rue.getDateFin());
		return estDansLeCanton(localite.getCommuneLocalite());
	}

	@Override
	public boolean estDansLeCanton(final Commune commune) throws InfrastructureException {
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

	@Override
	public boolean estDansLeCanton(AdresseGenerique adresse) throws InfrastructureException {

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
			if (localite == null) {
				throw new IllegalArgumentException("Aucune localité trouvée avec le numéro " + onrp);
			}
			return estDansLeCanton(localite.getCommuneLocalite());
		}
		else {
			final Rue rue = getRueByNumero(numero);
			if (rue == null) {
				throw new InfrastructureException("La rue avec l'estrid=[" + numero + "] n'existe pas dans le service d'infrastructure.");
			}
			return estDansLeCanton(rue);
		}
	}

	@Override
	public boolean estDansLeCanton(Adresse adresse) throws InfrastructureException {

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
			if (rue == null) {
				throw new InfrastructureException("La rue avec l'estrid=[" + numero + "] n'existe pas dans le service d'infrastructure.");
			}
			return estDansLeCanton(rue);
		}
	}

	@Override
	public boolean estEnSuisse(AdresseGenerique adresse) throws InfrastructureException {
		if (adresse == null) {
			throw new InfrastructureException("L'adresse est nulle");
		}
		final Integer noOfsPays = adresse.getNoOfsPays();
		return noOfsPays == null || noOfsPays == noOfsSuisse; // par défaut, un pays non-renseigné correspond à la Suisse
	}

	@Override
	public boolean estEnSuisse(Adresse adresse) throws InfrastructureException {
		if (adresse == null) {
			throw new InfrastructureException("L'adresse est nulle");
		}
		final Integer noOfsPays = adresse.getNoOfsPays();
		return noOfsPays == null || noOfsPays == noOfsSuisse; // par défaut, un pays non-renseigné correspond à la Suisse
	}

	@Override
	public Zone getZone(AdresseGenerique adresse) throws InfrastructureException {

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
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {
		return connector.getCollectivitesAdministratives();
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws InfrastructureException {
		return connector.getCollectivitesAdministratives(typesCollectivite);
	}

	@Override
	public Pays getPaysInconnu() throws InfrastructureException {
		return getPays(ServiceInfrastructureService.noPaysInconnu, null);
	}

	@Override
	public List<TypeRegimeFiscal> getRegimesFiscaux() throws InfrastructureException {
		return connector.getTousLesRegimesFiscaux();
	}

	@Override
	@Nullable
	public TypeRegimeFiscal getRegimeFiscal(@NotNull String code) throws InfrastructureException {
		// TODO (msi) faut-il tenir compte des périodes de validité et retourner le régime courant ?
		return connector.getTousLesRegimesFiscaux().stream()
				.filter(r -> Objects.equals(r.getCode(), code))
				.findFirst()
				.orElse(null);
	}

	@Override
	public List<GenreImpotMandataire> getGenresImpotMandataires() throws InfrastructureException {
		return connector.getTousLesGenresImpotMandataires();
	}

	@Override
	public List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws InfrastructureException {
		return connector.getLocalitesByNPA(npa, dateReference);
	}

	@Override
	public String getUrlInteroperabilite(ApplicationFiscale application, Long tiersId) {
		final Map<String, String> parameters = new HashMap<>(2);
		parameters.put("NOCTB", String.valueOf(tiersId));
		parameters.put("OID", String.valueOf(AuthenticationHelper.getCurrentOID()));
		return connector.getUrl(application, parameters);
	}

	@Override
	public String getUrlVisualisationDocument(Long tiersId, @Nullable Integer pf, String cleDocument) {
		final Map<String, String> parameters = new HashMap<>(6);
		parameters.put("NOCTB", String.valueOf(tiersId));
		parameters.put("PFI", pf != null ? pf.toString() : null);
		parameters.put("TOKEN", null);
		parameters.put("CONTEXT", "DOC_UNIREG_PILOTE");
		parameters.put("OID", String.valueOf(AuthenticationHelper.getCurrentOID()));
		parameters.put("ID", cleDocument);
		return connector.getUrl(ApplicationFiscale.DPERM_DOCUMENT, parameters);
	}

	@Override
	public String getUrlBrutte(ApplicationFiscale application) {
		return connector.getUrl(application, null);
	}

	@Override
	public Logiciel getLogiciel(Long idLogiciel) {
		return connector.getLogiciel(idLogiciel);
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() {
		return connector.getTousLesLogiciels();
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
