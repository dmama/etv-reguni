package ch.vd.uniregctb.interfaces.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.common.ReflexionUtils;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AdresseAvecCommune;
import ch.vd.uniregctb.interfaces.model.ApplicationFiscale;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.model.LogicielMetier;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.TypeAffranchissement;
import ch.vd.uniregctb.interfaces.model.TypeEtatPM;
import ch.vd.uniregctb.interfaces.model.TypeRegimeFiscal;

/**
 * Service d'infrastructure utilisée par le code métier. Ce service expose toutes les méthodes du service d'infrastructure <i>raw</i> en y ajoutant des méthodes utilitaires.
 */
public class ServiceInfrastructureImpl implements ServiceInfrastructureService {

	private ServiceInfrastructureRaw rawService;

	/*
	 * Note: on se permet de cacher l'ACI, la Suisse et le canton de Vaud à ce niveau, car il n'y a aucune chance que ces deux objets changent sans
	 * une remise en compte majeure des institutions. Tout autre forme de caching doit être déléguée au ServiceInfrastructureCache.
	 */
	private Pays suisse;
	private Canton vaud;
	private CollectiviteAdministrative aci;
	private CollectiviteAdministrative aciSuccessions;
	private CollectiviteAdministrative aciImpotSource;
	private CollectiviteAdministrative cedi;
	private CollectiviteAdministrative cat;
	private Map<Integer, List<Localite>> allLocaliteCommune;

	public ServiceInfrastructureImpl() {
	}

	public ServiceInfrastructureImpl(ServiceInfrastructureRaw rawService) {
		this.rawService = rawService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRawService(ServiceInfrastructureRaw rawService) {
		this.rawService = rawService;
		this.suisse = null;
		this.vaud = null;
		this.aci = null;
		this.aciSuccessions = null;
		this.aciImpotSource = null;
		this.cedi = null;
		this.cat = null;
		this.allLocaliteCommune = null;
	}

	/**
	 * {@inheritDoc}
	 */
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
	 * @return la liste des communes du canton de Vaud
	 */
	public List<Commune> getListeCommunes(int cantonOFS) throws ServiceInfrastructureException {
		Canton canton = getCanton(cantonOFS);
		if (canton == null) {
			return null;
		}
		return getListeCommunes(canton);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Commune> getListeCommunesByOID(int oid) throws ServiceInfrastructureException {
		List<Commune> communes = new ArrayList<Commune>();
		for (Commune c : getCommunesDeVaud()) {
			CollectiviteAdministrative oi = getOfficeImpotDeCommune(c.getNoOFSEtendu());
			if (oi != null && oi.getNoColAdm() == oid) {
				communes.add(c);
			}
		}
		return Collections.unmodifiableList(communes);
	}

	@Override
	public List<Commune> getCommunesDeVaud() throws ServiceInfrastructureException {
		final List<Commune> list = new ArrayList<Commune>();
		for (Commune commune : getCommunes()) {
			if (ServiceInfrastructureService.SIGLE_CANTON_VD.equals(commune.getSigleCanton())) {
				list.add(commune);
			}
		}
		return list;
	}

	@Override
	public List<Commune> getCommunesHorsCanton() throws ServiceInfrastructureException {
		final List<Commune> list = new ArrayList<Commune>();
		for (Commune commune : getCommunes()) {
			if (!ServiceInfrastructureService.SIGLE_CANTON_VD.equals(commune.getSigleCanton())) {
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

	@Override
	public Localite getLocaliteByONRP(int onrp) throws ServiceInfrastructureException {
		return rawService.getLocaliteByONRP(onrp);
	}

	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		return rawService.getPays();
	}

	@Override
	public Pays getPays(int numeroOFS) throws ServiceInfrastructureException {
		return rawService.getPays(numeroOFS);
	}

	/**
	 * {@inheritDoc}
	 */
	public Pays getPays(String codePays) throws ServiceInfrastructureException {

		// note : cette méthode est horriblement inefficace, mais comme le service infrastructure est sensé se trouver derrière un cache, on
		// va comme ça.

		Pays pays = null;

		final List<Pays> payss = getPays();
		for (Pays p : payss) {
			if (p.getSigleOFS() != null && p.getSigleOFS().equals(codePays)) {
				pays = p;
				break;
			}
		}

		return pays;
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
	public CollectiviteAdministrative getACISuccessions() throws ServiceInfrastructureException {
		if (aciSuccessions == null) {
			aciSuccessions = rawService.getCollectivite(ServiceInfrastructureService.noACISuccessions);
		}
		return aciSuccessions;
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
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		return rawService.getAllCantons();
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
		return rawService.getListeCommunes(canton);
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
		if (candidats != null && candidats.size() > 0) {
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

	/**
	 * {@inheritDoc}
	 */
	public synchronized List<Localite> getLocaliteByCommune(int commune) throws ServiceInfrastructureException {
		if (allLocaliteCommune == null) {
			allLocaliteCommune = new HashMap<Integer, List<Localite>>();
		}
		List<Localite> list = allLocaliteCommune.get(commune);
		if (list == null) {
			list = new ArrayList<Localite>();
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

	private Commune getCommuneByLocaliteAdresse(Integer numeroRue, int numeroOrdrePostal) throws ServiceInfrastructureException {

		final int numeroLocalite;
		if (numeroRue != null && numeroRue > 0) {
			final Rue rue = getRueByNumero(numeroRue);
			final Integer noLocalite = rue.getNoLocalite();
			Assert.notNull(noLocalite);
			numeroLocalite = noLocalite;
		}
		else {
			numeroLocalite = numeroOrdrePostal;
		}

		// Recherche de la commune
		final Commune commune;

		if (numeroLocalite == 0) {
			// adresse hors-Suisse
			commune = null;
		}
		else {
			final Localite localite = getLocaliteByONRP(numeroLocalite);
			if (localite == null) {
				throw new ServiceInfrastructureException("La localité avec le numéro " + numeroLocalite + " n'existe pas");
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
	 * @param numeroRue         une numéro de rue
	 * @param numeroOrdrePostal un numéro d'ordre postal
	 * @param date              la date de référence
	 * @return la commune qui correspond à l'adresse spécifiée; ou <b>null</b> si aucune commune n'a été trouvée.
	 * @throws ServiceInfrastructureException en cas d'erreur
	 */
	private Commune getCommuneByAdresse(AdresseAvecCommune adresse, Integer numeroRue, int numeroOrdrePostal, RegDate date) throws ServiceInfrastructureException {
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
			final Commune candidate = adresse.getCommuneAdresse();
			// si la commune est attachée et que ce n'est pas une commune fractionnée, on la prend
			// sinon, on prend l'adresse depuis la localité
			if (candidate != null && !candidate.isPrincipale()) {
				commune = candidate;
			}
		}

		// 3ème choix : la commune associée à la localité
		if (commune == null) {
			commune = getCommuneByLocaliteAdresse(numeroRue, numeroOrdrePostal);
		}

		return commune;
	}

	/**
	 * {@inheritDoc}
	 */
	public Commune getCommuneByAdresse(Adresse adresse, RegDate date) throws ServiceInfrastructureException {
		if (adresse != null) {
			return getCommuneByAdresse(adresse, adresse.getNumeroRue(), adresse.getNumeroOrdrePostal(), date);
		}
		else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Commune getCommuneByAdresse(AdresseGenerique adresse, RegDate date) throws ServiceInfrastructureException {
		if (adresse != null) {
			return getCommuneByAdresse(adresse, adresse.getNumeroRue(), adresse.getNumeroOrdrePostal(), date);
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
		return getCommuneByNumeroOfsEtendu(noOfs, date);
	}

	public Commune getCommuneFaitiere(Commune commune, RegDate dateReference) throws ServiceInfrastructureException {
		if (commune == null || !commune.isFraction()) {
			return commune;
		}

		//
		// C'est bidon ici !! on confond idTechnique et numéro OFS, mais on a de la chance : pour les communes
		// faîtières des fractions vaudoises, c'est la même chose...
		//
		final int idCommuneMere = commune.getNumTechMere();
		return getCommuneByNumeroOfsEtendu(idCommuneMere, dateReference);
	}

	@Override
	public OfficeImpot getOfficeImpot(int noColAdm) throws ServiceInfrastructureException {
		return rawService.getOfficeImpot(noColAdm);
	}

	@Override
	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws ServiceInfrastructureException {
		return rawService.getOfficeImpotDeCommune(noCommune);
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		return rawService.getOfficesImpot();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Rue> getRues(Collection<Localite> localites) throws ServiceInfrastructureException {
		List<Rue> locRues = new ArrayList<Rue>();
		for (Localite localite : localites) {
			locRues.addAll(getRues(localite));
		}
		return locRues;
	}

	@Override
	public List<Rue> getRues(Canton canton) throws ServiceInfrastructureException {
		return rawService.getRues(canton);
	}

	@Override
	public Rue getRueByNumero(int numero) throws ServiceInfrastructureException {
		return rawService.getRueByNumero(numero);
	}

	@Override
	public Pays getSuisse() throws ServiceInfrastructureException {
		if (suisse == null) {
			suisse = getPays(ServiceInfrastructureService.SIGLE_SUISSE);
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
	public Canton getCantonByCommune(int noOfsCommune) throws ServiceInfrastructureException {
		final Commune commune = getCommuneByNumeroOfsEtendu(noOfsCommune, null);
		if (commune == null) {
			throw new ServiceInfrastructureException("La commune avec le numéro Ofs " + noOfsCommune + " n'existe pas");
		}
		final String canton = commune.getSigleCanton();
		return getCantonBySigle(canton);
	}

	@Override
	public Commune getCommuneByNumeroOfsEtendu(int noCommune, RegDate date) throws ServiceInfrastructureException {
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
	public boolean estDansLeCanton(final Rue rue) throws ServiceInfrastructureException {
		final Integer onrp = rue.getNoLocalite();
		final Localite localite = getLocaliteByONRP(onrp);
		return estDansLeCanton(localite.getCommuneLocalite());
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean estDansLeCanton(final Commune commune) throws ServiceInfrastructureException {
		final String sigle = commune.getSigleCanton();
		if (sigle == null || sigle.equals("")) {
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
	public boolean estDansLeCanton(AdresseGenerique adresse) throws ServiceInfrastructureException {

		if (!estEnSuisse(adresse)) {
			return false;
		}

		final Integer numero = adresse.getNumeroRue();
		if (numero == null || numero == 0) {
			int onrp = adresse.getNumeroOrdrePostal();
			if (onrp == 0) {
				// la valeur 0 veut dire 'hors suisse' dans le host
				return false;
			}
			final Localite localite = getLocaliteByONRP(onrp);
			Assert.notNull(localite, "La localité avec onrp = " + onrp + " est introuvable.");
			return estDansLeCanton(localite.getCommuneLocalite());
		}
		else {
			final Rue rue = getRueByNumero(numero);
			return estDansLeCanton(rue);
		}
	}

	public boolean estDansLeCanton(Adresse adresse) throws ServiceInfrastructureException {

		if (!estEnSuisse(adresse)) {
			return false;
		}

		final Integer numero = adresse.getNumeroRue();
		if (numero == null || numero == 0) {
			final Commune commune = adresse.getCommuneAdresse();
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
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {
		return rawService.getCollectivitesAdministratives(typesCollectivite);
	}

	@Override
	public Pays getPaysInconnu() throws ServiceInfrastructureException {
		return getPays(ServiceInfrastructureService.noPaysInconnu);
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
	public Localite getLocaliteByNPA(int npa) throws ServiceInfrastructureException {
		return rawService.getLocaliteByNPA(npa);
	}

	/**
	 * [UNIREG-2243] liste hard-codée parce que l'infrastructure du host ne possède pas cette info pour l'instant
	 */
	private static Set<Integer> affEurope = new HashSet<Integer>();

	static {
		affEurope.add(8201); // Albanie
		affEurope.add(8207); // Allemagne
		affEurope.add(8202); // Andorre
		affEurope.add(8229); // Autriche
		affEurope.add(8266); // Bélarus
		affEurope.add(8204); // Belgique
		affEurope.add(8252); // Bosnie-Herzégovine
		affEurope.add(8205); // Bulgarie
		affEurope.add(8242); // Chypre
		affEurope.add(8250); // Croatie
		affEurope.add(8206); // Danemark
		affEurope.add(8236); // Espagne
		affEurope.add(8260); // Estonie
		affEurope.add(8210); // Féroé
		affEurope.add(8211); // Finlande
		affEurope.add(8212); // France
		affEurope.add(8213); // Gibraltar
		affEurope.add(8215); // Grande-Bretagne
		affEurope.add(8214); // Grèce
		affEurope.add(8413); // Groenland
		affEurope.add(8240); // Hongrie
		affEurope.add(8216); // Irlande
		affEurope.add(8217); // Islande
		affEurope.add(8218); // Italie
		affEurope.add(8261); // Lettonie
		affEurope.add(8262); // Lituanie
		affEurope.add(8223); // Luxembourg
		affEurope.add(8255); // Macédoine
		affEurope.add(8224); // Malte
		affEurope.add(8263); // Moldavie, République
		affEurope.add(8226); // Monaco
		affEurope.add(8254); // Monténégro, République
		affEurope.add(8228); // Norvège
		affEurope.add(8227); // Pays-Bas
		affEurope.add(8230); // Pologne
		affEurope.add(8231); // Portugal
		affEurope.add(8232); // Roumanie
		affEurope.add(8264); // Russie, Fédération de
		affEurope.add(8233); // St-Marin
		affEurope.add(8248); // Serbie, République
		affEurope.add(8243); // Slovaque, République
		affEurope.add(8251); // Slovénie
		affEurope.add(8234); // Suède
		affEurope.add(8244); // Tchèque, République
		affEurope.add(8239); // Turquie
		affEurope.add(8265); // Ukraine
		affEurope.add(8241); // Vatican
	}

	public TypeAffranchissement getTypeAffranchissement(int noOfsPays) {

		if (noOfsPays == noOfsSuisse) {
			// là haut sur nos montagnes, était un beau chalet
			return TypeAffranchissement.SUISSE;
		}

		if (affEurope.contains(Integer.valueOf(noOfsPays))) {
			// ich bin ein berliner
			return TypeAffranchissement.EUROPE;
		}

		// here be dragons
		return TypeAffranchissement.MONDE;
	}

	@Override
	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws ServiceInfrastructureException {
		return rawService.getTypesRegimesFiscaux();
	}

	@Override
	public TypeRegimeFiscal getTypeRegimeFiscal(String code) throws ServiceInfrastructureException {
		return rawService.getTypeRegimeFiscal(code);
	}

	@Override
	public List<TypeEtatPM> getTypesEtatsPM() throws ServiceInfrastructureException {
		return rawService.getTypesEtatsPM();
	}

	@Override
	public TypeEtatPM getTypeEtatPM(String code) throws ServiceInfrastructureException {
		return rawService.getTypeEtatPM(code);
	}

	@Override
	public String getUrlVers(ApplicationFiscale application, Long tiersId) {
		return rawService.getUrlVers(application, tiersId);
	}

	@Override
	public Logiciel getLogiciel(Long idLogiciel) {
		return rawService.getLogiciel(idLogiciel);
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() {
		return rawService.getTousLesLogiciels();
	}

	public List<Logiciel> getLogicielsPour(LogicielMetier metier) {
		final List<Logiciel> list = new ArrayList<Logiciel>();
		for (Logiciel l : getTousLesLogiciels()) {
			if (l.getMetier() == metier) {
				list.add(l);
			}
		}
		return list;
	}
}
