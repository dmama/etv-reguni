package ch.vd.uniregctb.interfaces.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;

public abstract class AbstractServiceInfrastructureService implements ServiceInfrastructureService {


	private Map<Integer, List<Localite>> allLocaliteCommune;

	public AbstractServiceInfrastructureService() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
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

	/**
	 * @return la liste des communes du canton de Vaud
	 */
	public List<Commune> getListeCommunes(int cantonOFS) throws InfrastructureException {
		Canton canton = getCanton(cantonOFS);
		if (canton == null) {
			return null;
		}
		return getListeCommunes(canton);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Commune> getListeCommunesByOID(int oid) throws InfrastructureException {
		List<Commune> communes = new ArrayList<Commune>();
		for (Commune c : getCommunesDeVaud()) {
			CollectiviteAdministrative oi = getOfficeImpotDeCommune(c.getNoOFSEtendu());
			if (oi != null && oi.getNoColAdm() == oid) {
				communes.add(c);
			}
		}
		return Collections.unmodifiableList(communes);
	}

	/**
	 * {@inheritDoc}
	 */
	public Pays getPays(int numeroOFS) throws InfrastructureException {

		Pays pays = null;

		List<Pays> payss = getPays();
		for (Pays p : payss) {
			if (p.getNoOFS() == numeroOFS) {
				pays = p;
				break;
			}
		}
		return pays;
	}

	/**
	 * {@inheritDoc}
	 */
	public Pays getPays(String codePays) throws InfrastructureException {

		// note : cette méthode est horriblement inefficace, mais comme le service infrastructure est sensé se trouver derrière un cache, on
		// va comme ça.

		Pays pays = null;

		final List<Pays> payss = getPays();
		for (Pays p : payss) {
			if (p.getSigleOFS().equals(codePays)) {
				pays = p;
				break;
			}
		}

		return pays;
	}

	/**
	 * {@inheritDoc}
	 */
	public Commune getCommuneVaudByNumACI(Integer numeroACI) throws InfrastructureException {
		Assert.notNull(numeroACI, "Le numero ACI ne peut pas etre nul");

		// Formatte le numero ACI en String sur 3 positions (EX: 052)
		String numeroACIstr = String.format("%03d", numeroACI);

		Commune commune = null;
		for (Commune com : getCommunesDeVaud()) {
			if (com.getNoACI().equals(numeroACIstr)) {
				commune = com;
			}
		}
		return commune;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Localite> getLocaliteByCommune(int commune) throws InfrastructureException {
		if ( allLocaliteCommune == null) {
			allLocaliteCommune = new HashMap<Integer, List<Localite>>();
		}
		Integer key = commune;
		List<Localite> list = allLocaliteCommune.get(key);
		if ( list == null) {
			list = new ArrayList<Localite>();
			for (Localite loc : getLocalites()) {
				if (loc.getNoCommune() != null && loc.getNoCommune().intValue() == commune) {
					list.add(loc);
				}
			}
			allLocaliteCommune.put(key,list);
		}
		return list;
	}

	/**
	 * {@inheritDoc}
	 */
	public Commune getCommuneByAdresse(Adresse adresse) throws InfrastructureException {
		if (adresse == null)
			return null;

		final int numeroLocalite;

		// Recherche de la localité
		final Integer numeroRue = adresse.getNumeroRue();
		if (numeroRue != null && numeroRue > 0) {
			final Rue rue = getRueByNumero(numeroRue);
			final Integer noLocalite = rue.getNoLocalite();
			Assert.notNull(noLocalite);
			numeroLocalite = noLocalite;
		}
		else {
			numeroLocalite = adresse.getNumeroOrdrePostal();
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
				throw new InfrastructureException("La localité avec le numéro " + numeroLocalite + " n'existe pas");
			}

			commune = getCommuneByLocalite(localite);
		}

		return commune;
	}

	/**
	 * {@inheritDoc}
	 */
	public Commune getCommuneByAdresse(AdresseGenerique adresse) throws InfrastructureException {
		if (adresse == null)
			return null;

		// Recherche de la localité
		final int numeroLocalite;

		final Integer numeroRue = adresse.getNumeroRue();
		if (numeroRue != null && numeroRue > 0) {
			final Rue rue = getRueByNumero(numeroRue);
			final Integer noLocalite = rue.getNoLocalite();
			Assert.notNull(noLocalite);
			numeroLocalite = noLocalite;
		}
		else {
			numeroLocalite = adresse.getNumeroOrdrePostal();
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
				throw new InfrastructureException("La localité avec le numéro " + numeroLocalite + " n'existe pas");
			}

			commune = getCommuneByLocalite(localite);
		}

		return commune;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Rue> getRues(Collection<Localite> localites) throws InfrastructureException {
		List<Rue> locRues = new ArrayList<Rue>();
		for (Localite localite : localites) {
			locRues.addAll(getRues(localite));
		}
		return locRues;
	}

	/**
	 * {@inheritDoc}
	 */
	public Canton getCantonByCommune(int noOfsCommune) throws InfrastructureException {
		Commune commune = getCommuneByNumeroOfsEtendu(noOfsCommune);
		if (commune == null) {
			throw new InfrastructureException("La commune avec le numéro Ofs " + noOfsCommune + " n'existe pas");
		}
		final String canton = commune.getSigleCanton();
		return getCantonBySigle(canton);
	}

	/**
	 * {@inheritDoc}
	 */
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

	/**
	 * {@inheritDoc}
	 */
	public boolean estDansLeCanton(final Rue rue) throws InfrastructureException {
		final Integer onrp = rue.getNoLocalite();
		final Localite localite = getLocaliteByONRP(onrp);
		return estDansLeCanton(localite.getCommuneLocalite());
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean estDansLeCanton(final CommuneSimple commune) throws InfrastructureException {
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
	public boolean estDansLeCanton(final Commune commune) throws InfrastructureException {
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
	public boolean estDansLeCanton(AdresseGenerique adresse) throws InfrastructureException {

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

	public boolean estDansLeCanton(Adresse adresse) throws InfrastructureException {

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

	/**
	 * {@inheritDoc}
	 */
	public boolean estEnSuisse(AdresseGenerique adresse) throws InfrastructureException {
		if (adresse == null) {
			throw new InfrastructureException("L'adresse est nulle");
		}
		final Integer noOfsPays = adresse.getNoOfsPays();
		return noOfsPays == null || noOfsPays == noOfsSuisse; // par défaut, un pays non-renseigné correspond à la Suisse
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean estEnSuisse(Adresse adresse) throws InfrastructureException {
		if (adresse == null) {
			throw new InfrastructureException("L'adresse est nulle");
		}
		final Integer noOfsPays = adresse.getNoOfsPays();
		return noOfsPays == null || noOfsPays == noOfsSuisse; // par défaut, un pays non-renseigné correspond à la Suisse
	}


	/**
	 * {@inheritDoc}
	 */
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
}
