package ch.vd.uniregctb.interfaces.service.host;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ch.vd.infrastructure.fiscal.service.ServiceInfrastructureFiscal;
import ch.vd.infrastructure.model.EnumPays;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.infrastructure.service.ServiceInfrastructure;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.JvmVersionHelper;
import ch.vd.uniregctb.interfaces.model.ApplicationFiscale;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.CommuneId;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.TypeEtatPM;
import ch.vd.uniregctb.interfaces.model.TypeRegimeFiscal;
import ch.vd.uniregctb.interfaces.model.impl.CantonImpl;
import ch.vd.uniregctb.interfaces.model.impl.CollectiviteAdministrativeImpl;
import ch.vd.uniregctb.interfaces.model.impl.CommuneImpl;
import ch.vd.uniregctb.interfaces.model.impl.InstitutionFinanciereImpl;
import ch.vd.uniregctb.interfaces.model.impl.LocaliteImpl;
import ch.vd.uniregctb.interfaces.model.impl.PaysImpl;
import ch.vd.uniregctb.interfaces.model.impl.RueImpl;
import ch.vd.uniregctb.interfaces.model.impl.TypeEtatPMImpl;
import ch.vd.uniregctb.interfaces.model.impl.TypeRegimeFiscalImpl;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureBase;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * @author Jean-Eric CUENDET
 *
 */
public class ServiceInfrastructureHostInterfaces extends ServiceInfrastructureBase {

	private static final Logger LOGGER = Logger.getLogger(ServiceInfrastructureHostInterfaces.class);

	private ServiceInfrastructure serviceInfrastructure;
	private ServiceInfrastructureFiscal serviceInfrastructureFiscal;

	/**
	 * Type de collectivite administrative OID
	 */
	public static final Integer TYPE_COLLECTIVITE_OID = 2;

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

	private Map<Integer,Localite> localitesByNPA;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfrastructure(ServiceInfrastructure serviceInfrastructure) {
		this.serviceInfrastructure = serviceInfrastructure;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfrastructureFiscal(ServiceInfrastructureFiscal serviceInfrastructureFiscal) {
		this.serviceInfrastructureFiscal = serviceInfrastructureFiscal;
	}

	public ServiceInfrastructureHostInterfaces() {
		JvmVersionHelper.checkJvmWrtHostInterfaces();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Canton> getAllCantons() throws InfrastructureException {
		List<Canton> cantons = new ArrayList<Canton>();
		try {
			List<?> list = serviceInfrastructure.getCantons(serviceInfrastructure.getPays(EnumPays.SIGLE_CH));
			for (Object o : list) {
				ch.vd.infrastructure.model.Canton c = (ch.vd.infrastructure.model.Canton) o;
				cantons.add(CantonImpl.get(c));
			}
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces a la liste des cantons impossible", e);
		}
		return Collections.unmodifiableList(cantons);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Commune> getListeCommunes(final Canton canton) throws InfrastructureException {
		try {
			final List<?> list = serviceInfrastructure.getCommunes(canton.getSigleOFS());
			List<Commune> communes = new ArrayList<Commune>();
			for (Object o : list) {
				ch.vd.infrastructure.model.Commune co = (ch.vd.infrastructure.model.Commune) o;
				communes.add(CommuneImpl.get(co));
			}
			return Collections.unmodifiableList(communes);
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces a la liste des communes impossible", e);
		}
	}

	@Override
	public CommuneId getCommuneIdByEgid(long egid, RegDate date, Long hintNoOfsCommune) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getCommuneByEgid' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public CommuneSimple getCommuneById(CommuneId id) throws InfrastructureException {

		// on essaie d'abord avec Vaud
		CommuneSimple commune = getCommuneByIdForCanton(id, ServiceInfrastructureService.SIGLE_CANTON_VD);

		if (commune == null) {
			// on essaie ensuite avec les autres cantons
			for (Canton canton : getAllCantons()) {
				final String sigle = canton.getSigleOFS();
				if (!sigle.equals(ServiceInfrastructureService.SIGLE_CANTON_VD)) {
					commune = getCommuneByIdForCanton(id, sigle);
					if (commune != null) {
						break;
					}
				}
			}
		}

		return commune;
	}

	private CommuneSimple getCommuneByIdForCanton(CommuneId id, String sigleCanton) throws InfrastructureException {
		try {
			// comme il peut y avoir plusieurs communes avec les mêmes numéros Ofs, on construit la liste des candidats.
			final List<?> list = serviceInfrastructure.getCommunes(sigleCanton);
			List<ch.vd.infrastructure.model.Commune> candidats = new ArrayList<ch.vd.infrastructure.model.Commune>(2);
			for (Object o : list) {
				ch.vd.infrastructure.model.Commune co = (ch.vd.infrastructure.model.Commune) o;
				int noOFSEtendu = (co.isFraction() ? co.getNoTechnique() : co.getNoOFS());
				if (noOFSEtendu == id.getNoOfs()) {
					candidats.add(co);
				}
			}
			if (candidats.isEmpty()) {
				// pas trouvé de candidat -> inutile d'aller plus loin
				return null;
			}
			else if (candidats.size() == 1) {
				// trouvé juste un candidat -> c'est tout bon
				return CommuneImpl.get(candidats.get(0));
			}
			else {
				// trouvé 2 ou plus candidats, on filtre sur le numéro technique
				for (ch.vd.infrastructure.model.Commune candidat : candidats) {
					if (candidat.getNoTechnique() == id.getNumeroTechnique()) {
						return CommuneImpl.get(candidat);
					}
				}
				throw new InfrastructureException(
						"Trouvé " + candidats.size() + " communes avec le numéro Ofs = [" + id.getNoOfs() + "], mais aucune parmis elles ne possèdent le numéro technique = [" +
								id.getNumeroTechnique() + "]");
			}
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Accès à la liste des communes impossible", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({"unchecked"})
	public List<Commune> getListeFractionsCommunes() throws InfrastructureException {
		try {
			final List<ch.vd.infrastructure.model.Commune> list = serviceInfrastructure.getCommunes(ServiceInfrastructureService.SIGLE_CANTON_VD);
			final List<Commune> communes = new ArrayList<Commune>();
			for (ch.vd.infrastructure.model.Commune co : list) {
				if (!co.isPrincipale()) {
					communes.add(CommuneImpl.get(co));
				}
			}
			return Collections.unmodifiableList(communes);
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces a la liste des fractions de communes impossible", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Commune> getCommunesDeVaud() throws InfrastructureException {
		return getListeCommunes(getVaud());
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Commune> getCommunesHorsCanton() throws InfrastructureException {
		List<Commune> communes = new ArrayList<Commune>();
		for (Canton canton : getAllCantons()) {
			if (!canton.getSigleOFS().equals(ServiceInfrastructureService.SIGLE_CANTON_VD)) {
				List<Commune> liste = getListeCommunes(canton);
				communes.addAll(liste);
			}
		}
		return Collections.unmodifiableList(communes);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Commune> getCommunes() throws InfrastructureException {
		List<Commune> communes = new ArrayList<Commune>();
		for (Canton canton : getAllCantons()) {
			List<Commune> liste = getListeCommunes(canton);
			communes.addAll(liste);
		}
		return Collections.unmodifiableList(communes);
	}


	/**
	 * {@inheritDoc}
	 */
	public Pays getSuisse() throws ServiceInfrastructureException {
		if (suisse == null) {
			try {
				suisse = PaysImpl.get(serviceInfrastructure.getPays(EnumPays.SIGLE_CH));
			}
			catch (RemoteException e) {
				LOGGER.error(e);
				throw new ServiceInfrastructureException("Erreur en essayant de récupérer la Suisse (tous aux abris !)", e);
			}
			catch (InfrastructureException e) {
				LOGGER.error(e);
				throw new ServiceInfrastructureException("Erreur en essayant de récupérer la Suisse (tous aux abris !)", e);
			}
		}
		return suisse;
	}


	public Localite getLocaliteByNPA(int npa) throws InfrastructureException{
		if (localitesByNPA==null) {
			initLocaliteByNPA();
		}
		return localitesByNPA.get(npa);
	}


	private void initLocaliteByNPA() throws InfrastructureException {
		List<Localite> localites = getLocalites();
		localitesByNPA = new HashMap<Integer, Localite>();
		for (Localite localite : localites) {
			if (localite.getNPA()!=null) {
				localitesByNPA.put(localite.getNPA(), localite);
			}

		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Canton getVaud() throws InfrastructureException {

		if (vaud == null) {
			for (Canton c : getAllCantons()) {
				if (c.getSigleOFS().equals(ServiceInfrastructureService.SIGLE_CANTON_VD)) {
					vaud = c;
				}
			}
			Assert.notNull(vaud);
		}
		return vaud;
	}

	public Commune getCommuneByNumeroOfsEtendu(int noCommune, RegDate date) throws InfrastructureException {
		List<Commune> candidates = new ArrayList<Commune>(2);
		final List<Commune> communes = getCommunes();
		for (Commune commune : communes) {
			if (commune.getNoOFSEtendu() == noCommune) {
				candidates.add(commune);
			}
		}
		return choisirCommune(candidates, date);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Pays> getPays() throws InfrastructureException {
		List<Pays> pays = new ArrayList<Pays>();
		try {
			List<?> list = serviceInfrastructure.getListePays();
			for (Object o : list) {
				ch.vd.infrastructure.model.Pays p = (ch.vd.infrastructure.model.Pays) o;
				pays.add(PaysImpl.get(p));
			}
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces a la liste des pays", e);
		}
		return Collections.unmodifiableList(pays);
	}

	/**
	 * {@inheritDoc}
	 */
	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException {
		int numOrdreP = localite.getNoOrdre();
		try {
			if (numOrdreP == 540) {// 1341 Orient -> fraction l'orient 8002
				return CommuneImpl.get(serviceInfrastructure.getCommuneById(String.valueOf(8002)));
			}
			else if (numOrdreP == 541) {// 1346 les Bioux -> fraction les Bioux 8012
				return CommuneImpl.get(serviceInfrastructure.getCommuneById(String.valueOf(8012)));
			}
			else if (numOrdreP == 542) {// 1344 l'Abbaye -> commune de l'Abbaye 5871
				return CommuneImpl.get(serviceInfrastructure.getCommuneById(String.valueOf(5871)));
			}
			else if (numOrdreP == 543) {// 1342 le Pont -> commune de l'Abbaye 5871
				return CommuneImpl.get(serviceInfrastructure.getCommuneById(String.valueOf(5871)));

			}
			else if (numOrdreP == 546) {// 1347 le Sentier -> fraction le Sentier 8000
				return CommuneImpl.get(serviceInfrastructure.getCommuneById(String.valueOf(8000)));
			}
			else if (numOrdreP == 547) {// 1347 le Solliat -> commune Chenit 5872
				return CommuneImpl.get(serviceInfrastructure.getCommuneById(String.valueOf(5872)));
			}
			else if (numOrdreP == 550) {// 1348 le Brassus -> fraction le Brassus 8001
				return CommuneImpl.get(serviceInfrastructure.getCommuneById(String.valueOf(8001)));
			}
			else { // commune normale sans fraction
				return CommuneImpl.get(serviceInfrastructure.getCommuneById(localite.getNoCommune().toString()));
			}
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces a la commune " + numOrdreP, e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Localite> getLocalites() throws InfrastructureException {

		List<Localite> localites = new ArrayList<Localite>();
		try {
			for (Canton c : getAllCantons()) {
				List<?> localitesTmp = serviceInfrastructure.getLocalites(c.getSigleOFS());
				for (Object o : localitesTmp) {
					ch.vd.infrastructure.model.Localite l = (ch.vd.infrastructure.model.Localite) o;
					localites.add(LocaliteImpl.get(l));
				}
			}
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Impossible de récupérer les liste des localites", e);
		}
		return Collections.unmodifiableList(localites);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Rue> getRues(Localite localite) throws InfrastructureException {
		List<Rue> rues = new ArrayList<Rue>();
		try {
			final List<?> list = serviceInfrastructure.getRues(localite.getNoOrdre());
			for (Object o : list) {
				ch.vd.infrastructure.model.Rue r = (ch.vd.infrastructure.model.Rue) o;
				rues.add(RueImpl.get(r));
			}
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces a la liste des rues", e);
		}
		return Collections.unmodifiableList(rues);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Rue> getRues(Canton canton) throws InfrastructureException {
		try {
			ArrayList<Rue> rues = new ArrayList<Rue>();
			final List<?> list = serviceInfrastructure.getRues(canton.getSigleOFS());
			for (Object o : list) {
				ch.vd.infrastructure.model.Rue r = (ch.vd.infrastructure.model.Rue) o;
				rues.add(RueImpl.get(r));
			}
			return Collections.unmodifiableList(rues);
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces a la liste des rues", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Rue getRueByNumero(int numero) throws InfrastructureException {
		try {
			return RueImpl.get(serviceInfrastructure.getRueByNumero(numero));
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces a la liste des rues", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Localite getLocaliteByONRP(int numeroOrdre) throws InfrastructureException {
		try {
			return LocaliteImpl.get(serviceInfrastructure.getLocalite(numeroOrdre));
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces a la localite", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException {
		try {
			return CollectiviteAdministrativeImpl.get(serviceInfrastructure.getCollectivite(noColAdm));
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces a la collectivite administrative", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public OfficeImpot getOfficeImpot(int noColAdm) throws InfrastructureException {
		final CollectiviteAdministrative coll = getCollectivite(noColAdm);
		if (coll instanceof OfficeImpot) {
			return (OfficeImpot) coll;
		}
		else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws InfrastructureException {
		try {
			CollectiviteAdministrativeImpl oid = CollectiviteAdministrativeImpl.get(serviceInfrastructure.getOidDeCommune(noCommune));
			return (OfficeImpot) oid;
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces a la collectivite administrative", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException {

		List<OfficeImpot> offices = new ArrayList<OfficeImpot>();
		try {
			List<?> list = serviceInfrastructure.getCollectivites(TYPE_COLLECTIVITE_OID);
			for (Object o : list) {
				ch.vd.infrastructure.model.CollectiviteAdministrative c = (ch.vd.infrastructure.model.CollectiviteAdministrative) o;
				if (isValid(c.getDateFinValidite())) {
					CollectiviteAdministrative oid = CollectiviteAdministrativeImpl.get(c);
					offices.add((OfficeImpot) oid);
				}
			}
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces aux collectivites administratives", e);
		}
		return Collections.unmodifiableList(offices);
	}

	private static boolean isValid(Date dateFinValidite) {
		final boolean valide;
		if (dateFinValidite != null) {
			final RegDate finValidite = RegDate.get(dateFinValidite);
			final RegDate now = RegDate.get();
			valide = RegDateHelper.isAfterOrEqual(finValidite, now, NullDateBehavior.LATEST);
		}
		else {
			valide = true;
		}
		return valide;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({"unchecked"})
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {

		final List<CollectiviteAdministrative> collectivites = new ArrayList<CollectiviteAdministrative>();
		try {
			final List<ch.vd.infrastructure.model.CollectiviteAdministrative> list = serviceInfrastructure.getCollectivitesAdministratives(ServiceInfrastructureService.SIGLE_CANTON_VD);
			for (ch.vd.infrastructure.model.CollectiviteAdministrative c : list) {
				if (isValid(c.getDateFinValidite())) {
					collectivites.add(CollectiviteAdministrativeImpl.get(c));
				}
			}
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces aux collectivites administratives", e);
		}
		return Collections.unmodifiableList(collectivites);

	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({"unchecked"})
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite)
			throws InfrastructureException {

		final List<CollectiviteAdministrative> collectivites = new ArrayList<CollectiviteAdministrative>();
		try {
			final EnumTypeCollectivite[] tabTypesCollectivite = typesCollectivite.toArray(new EnumTypeCollectivite[typesCollectivite.size()]);
			final List<ch.vd.infrastructure.model.CollectiviteAdministrative> list = serviceInfrastructure.getCollectivitesAdministratives(tabTypesCollectivite);
			for (ch.vd.infrastructure.model.CollectiviteAdministrative c : list) {
				if (isValid(c.getDateFinValidite())) {
					collectivites.add(CollectiviteAdministrativeImpl.get(c));
				}
			}
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces aux collectivites administratives", e);
		}
		return Collections.unmodifiableList(collectivites);

	}

	/**
	 * {@inheritDoc}
	 */
	public Pays getPaysInconnu() throws InfrastructureException {
		return getPays(8999);
	}

	/**
	 * @return la collectivite administrative de l'ACI
	 * @throws InfrastructureException en cas d'erreur lors de l'accès à la collectivité
	 */
	public CollectiviteAdministrative getACI() throws InfrastructureException {
		if (aci == null) {
			try {
				aci = CollectiviteAdministrativeImpl.get(serviceInfrastructure.getCollectivite(noACI));
			}
			catch (RemoteException e) {
				throw new InfrastructureException("Acces a la collectivite administrative", e);
			}
		}
		return aci;
	}

	public CollectiviteAdministrative getACIImpotSource() throws InfrastructureException {

		if (aciImpotSource == null) {
			try {
				aciImpotSource = CollectiviteAdministrativeImpl.get(serviceInfrastructure.getCollectivite(noACIImpotSource));
			}
			catch (RemoteException e) {
				throw new InfrastructureException("Acces a la collectivite administrative", e);
			}
		}
		return aciImpotSource;
	}

	public CollectiviteAdministrative getACISuccessions() throws InfrastructureException {
		if (aciSuccessions == null) {
			try {
				aciSuccessions = CollectiviteAdministrativeImpl.get(serviceInfrastructure.getCollectivite(noACISuccessions));
			}
			catch (RemoteException e) {
				throw new InfrastructureException("Acces a la collectivite administrative", e);
			}
		}
		return aciSuccessions;
	}

	/**
	 * @return la collectivite administrative du CEDI
	 * @throws InfrastructureException en cas d'erreur lors de l'accès à la collectivité
	 */
	public CollectiviteAdministrative getCEDI() throws InfrastructureException {
		if (cedi == null) {
			try {
				cedi = CollectiviteAdministrativeImpl.get(serviceInfrastructure.getCollectivite(noCEDI));
			}
			catch (RemoteException e) {
				throw new InfrastructureException("Acces a la collectivite administrative", e);
			}
		}
		return cedi;
	}

	/**
	 * @return la collectivite administrative du CAT
	 * @throws InfrastructureException en cas d'erreur lors de l'accès à la collectivité
	 */
	public CollectiviteAdministrative getCAT() throws InfrastructureException {
		if (cat == null) {
			try {
				cat = CollectiviteAdministrativeImpl.get(serviceInfrastructure.getCollectivite(noCAT));
			}
			catch (RemoteException e) {
				throw new InfrastructureException("Acces a la collectivite administrative", e);
			}
		}
		return cat;
	}

	public InstitutionFinanciere getInstitutionFinanciere(int id) throws InfrastructureException {
		try {
			return InstitutionFinanciereImpl.get(serviceInfrastructure.getInstitutionFinanciere(id));
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces à l'institution financière", e);
		}
	}

	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws InfrastructureException {
		try {
			List<?> l = serviceInfrastructure.getInstitutionsFinancieres(noClearing);
			List<InstitutionFinanciere> list = new ArrayList<InstitutionFinanciere>(l.size());
			for (Object o : l) {
				ch.vd.registre.common.model.InstitutionFinanciere i = (ch.vd.registre.common.model.InstitutionFinanciere) o;
				list.add(InstitutionFinanciereImpl.get(i));
			}
			return list;
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces à l'institution financière", e);
		}
	}

	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws InfrastructureException {
		try {
			ch.vd.infrastructure.fiscal.model.TypeRegimeFiscal[] types = serviceInfrastructureFiscal.getTypeRegimesFiscaux();
			List<TypeRegimeFiscal> list = new ArrayList<TypeRegimeFiscal>(types.length);
			for (ch.vd.infrastructure.fiscal.model.TypeRegimeFiscal type : types) {
				list.add(TypeRegimeFiscalImpl.get(type));
			}
			return list;
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces aux types de régimes fiscaux", e);
		}
	}

	public TypeRegimeFiscal getTypeRegimeFiscal(String code) throws InfrastructureException {
		final List<TypeRegimeFiscal> list = getTypesRegimesFiscaux();
		if (list != null) {
			for (TypeRegimeFiscal type : list) {
				if (type.getCode().equals(code)) {
					return type;
				}
			}
		}
		return null;
	}

	public List<TypeEtatPM> getTypesEtatsPM() throws InfrastructureException {
		try {
			ch.vd.infrastructure.fiscal.model.TypeEtatPM[] types = serviceInfrastructureFiscal.getTypesEtatsPM();
			List<TypeEtatPM> list = new ArrayList<TypeEtatPM>(types.length);
			for (ch.vd.infrastructure.fiscal.model.TypeEtatPM type : types) {
				list.add(TypeEtatPMImpl.get(type));
			}
			return list;
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces aux types des états PM", e);
		}
	}

	public TypeEtatPM getTypeEtatPM(String code) throws InfrastructureException {
		final List<TypeEtatPM> list = getTypesEtatsPM();
		if (list != null) {
			for (TypeEtatPM type : list) {
				if (type.getCode().equals(code)) {
					return type;
				}
			}
		}
		return null;
	}

	public String getUrlVers(ApplicationFiscale application, Long tiersId) {
		throw new NotImplementedException("La méthode 'getUrlVers' ne doit pas être appelée sur le service host-interfaces.");
	}

	public Logiciel getLogiciel(Long idLogiciel) {
		throw new NotImplementedException("La méthode 'getLogiciel' ne doit pas être appelée sur le service host-interfaces.");
	}

	public List<Logiciel> getTousLesLogiciels() {
		throw new NotImplementedException("La méthode 'getTousLesLogiciels' ne doit pas être appelée sur le service host-interfaces.");
	}
}
