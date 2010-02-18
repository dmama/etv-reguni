package ch.vd.uniregctb.interfaces.service;

import java.rmi.RemoteException;
import java.util.*;

import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ch.vd.infrastructure.model.EnumPays;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.model.impl.CantonImpl;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.infrastructure.service.ServiceInfrastructure;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.JvmVersionHelper;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.wrapper.CantonWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.CollectiviteAdministrativeWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.CommuneWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.InstitutionFinanciereWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.LocaliteWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.PaysWrapper;
import ch.vd.uniregctb.interfaces.model.wrapper.RueWrapper;

/**
 * @author Jean-Eric CUENDET
 *
 */
public class ServiceInfrastructureServiceImpl extends AbstractServiceInfrastructureService {

	private static final Logger LOGGER = Logger.getLogger(ServiceInfrastructureServiceImpl.class);

	private ServiceInfrastructure serviceInfrastructure;

	/**
	 * Type de collectivite administrative OID
	 */
	public static final Integer TYPE_COLLECTIVITE_OID = new Integer(2);

	/*
	 * Note: on se permet de cacher l'ACI, la Suisse et le canton de Vaud à ce niveau, car il n'y a aucune chance que ces deux objets changent sans
	 * une remise en compte majeure des institutions. Tout autre forme de caching doit être déléguée au ServiceInfrastructureCache.
	 */
	private Pays suisse;
	private Canton vaud;
	private CollectiviteAdministrative aci;
	private CollectiviteAdministrative cedi;
	private CollectiviteAdministrative cat;

	private Map<Integer,Localite> localitesByNPA;

	/**
	 * @return Returns the serviceInfrastructure.
	 */
	public ServiceInfrastructure getServiceInfrastructure() {
		return serviceInfrastructure;
	}

	/**
	 * @param serviceInfrastructure
	 *            The serviceInfrastructure to set.
	 */
	public void setServiceInfrastructure(ServiceInfrastructure serviceInfrastructure) {
		this.serviceInfrastructure = serviceInfrastructure;
	}

	public ServiceInfrastructureServiceImpl() {
		// l'EJB de HostInterface a besoin d'une version 1.5
		JvmVersionHelper.checkJava_1_5();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Canton> getAllCantons() throws InfrastructureException {
		List<Canton> cantons = new ArrayList<Canton>();
		try {
			List<?> list = getServiceInfrastructure().getCantons(getServiceInfrastructure().getPays(EnumPays.SIGLE_CH));
			for (Object o : list) {
				ch.vd.infrastructure.model.Canton c = (ch.vd.infrastructure.model.Canton) o;
				cantons.add(CantonWrapper.get(c));
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
	public List<Commune> getListeCommunes(Canton canton) throws InfrastructureException {
		try {
			final ch.vd.infrastructure.model.Canton c = ((CantonWrapper) canton).getTarget();
			final List<?> list = getServiceInfrastructure().getCommunes(c);
			List<Commune> communes = new ArrayList<Commune>();
			for (Object o : list) {
				ch.vd.infrastructure.model.Commune co = (ch.vd.infrastructure.model.Commune) o;
				communes.add(CommuneWrapper.get(co));
			}
			return Collections.unmodifiableList(communes);
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces a la liste des communes impossible", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Commune> getListeFractionsCommunes() throws InfrastructureException {
		try {
			final ch.vd.infrastructure.model.Canton c = ((CantonWrapper) getVaud()).getTarget();
			final List<?> list = getServiceInfrastructure().getCommunes(c);
			List<Commune> communes = new ArrayList<Commune>();
			for (Object o : list) {
				ch.vd.infrastructure.model.Commune co = (ch.vd.infrastructure.model.Commune) o;
				if (		(co.getNoTechnique() != 5871)
						&& 	(co.getNoTechnique() != 5872)
						&& 	(co.getNoTechnique() != 5873)) {
					communes.add(CommuneWrapper.get(co));
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
				suisse = PaysWrapper.get(getServiceInfrastructure().getPays(EnumPays.SIGLE_CH));
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

	/**
	 * {@inheritDoc}
	 */
	public List<Pays> getPays() throws InfrastructureException {
		List<Pays> pays = new ArrayList<Pays>();
		try {
			List<?> list = serviceInfrastructure.getListePays();
			for (Object o : list) {
				ch.vd.infrastructure.model.Pays p = (ch.vd.infrastructure.model.Pays) o;
				pays.add(PaysWrapper.get(p));
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
	public Commune getCommuneByNumeroOfsEtendu(int numeroOFS) throws InfrastructureException {
		if (numeroOFS <= 0) {
			return null;
		}

		try {
			switch (numeroOFS) {
			case 8000:
			case 8001:
			case 8002:
			case 8003:
			case 8010:
			case 8011:
			case 8012:
			case 8020:
			case 8021:
			case 8022:
				// ce sont les fractions de communes vaudoises, accès par numéro technique
				return CommuneWrapper.get(serviceInfrastructure.getCommuneById(Integer.toString(numeroOFS)));

			default:
				// sinon accès par numéro OFS
				return CommuneWrapper.get(serviceInfrastructure.getCommune(numeroOFS));
			}
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Accès a la commune " + numeroOFS, e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException {
		int numOrdreP = localite.getNoOrdre();
		try {
			if (numOrdreP == 540) {// 1341 Orient -> fraction l'orient 8002
				return CommuneWrapper.get(serviceInfrastructure.getCommuneById(String.valueOf(8002)));
			}
			else if (numOrdreP == 541) {// 1346 les Bioux -> fraction les Bioux 8012
				return CommuneWrapper.get(serviceInfrastructure.getCommuneById(String.valueOf(8012)));
			}
			else if (numOrdreP == 542) {// 1344 l'Abbaye -> commune de l'Abbaye 5871
				return CommuneWrapper.get(serviceInfrastructure.getCommuneById(String.valueOf(5871)));
			}
			else if (numOrdreP == 543) {// 1342 le Pont -> commune de l'Abbaye 5871
				return CommuneWrapper.get(serviceInfrastructure.getCommuneById(String.valueOf(5871)));

			}
			else if (numOrdreP == 546) {// 1347 le Sentier -> fraction le Sentier 8000
				return CommuneWrapper.get(serviceInfrastructure.getCommuneById(String.valueOf(8000)));
			}
			else if (numOrdreP == 547) {// 1347 le Solliat -> commune Chenit 5872
				return CommuneWrapper.get(serviceInfrastructure.getCommuneById(String.valueOf(5872)));
			}
			else if (numOrdreP == 550) {// 1348 le Brassus -> fraction le Brassus 8001
				return CommuneWrapper.get(serviceInfrastructure.getCommuneById(String.valueOf(8001)));
			}
			else { // commune normale sans fraction
				return CommuneWrapper.get(serviceInfrastructure.getCommuneById(localite.getNoCommune().toString()));
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
				final ch.vd.infrastructure.model.Canton canton = ((CantonWrapper) c).getTarget();
				List<?> localitesTmp = serviceInfrastructure.getLocalites(canton);
				for (Object o : localitesTmp) {
					ch.vd.infrastructure.model.Localite l = (ch.vd.infrastructure.model.Localite) o;
					localites.add(LocaliteWrapper.get(l));
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
	public List<Rue> getAllRues() throws InfrastructureException {

		ArrayList<Rue> rues = new ArrayList<Rue>();
		try {
			for (Canton canton : getAllCantons()) {
				final ch.vd.infrastructure.model.Canton c = ((CantonWrapper) canton).getTarget();
				final List<?> list = serviceInfrastructure.getRues(c);
				for (Object o : list) {
					ch.vd.infrastructure.model.Rue r = (ch.vd.infrastructure.model.Rue) o;
					rues.add(RueWrapper.get(r));
				}
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
	public List<Rue> getRues(Localite localite) throws InfrastructureException {
		List<Rue> rues = new ArrayList<Rue>();
		try {
			final ch.vd.infrastructure.model.Localite l = ((LocaliteWrapper) localite).getTarget();
			final List<?> list = serviceInfrastructure.getRues(l);
			for (Object o : list) {
				ch.vd.infrastructure.model.Rue r = (ch.vd.infrastructure.model.Rue) o;
				rues.add(RueWrapper.get(r));
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
			final ch.vd.infrastructure.model.Canton c = ((CantonWrapper) canton).getTarget();
			final List<?> list = serviceInfrastructure.getRues(c);
			for (Object o : list) {
				ch.vd.infrastructure.model.Rue r = (ch.vd.infrastructure.model.Rue) o;
				rues.add(RueWrapper.get(r));
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
			return RueWrapper.get(serviceInfrastructure.getRueByNumero(numero));
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
			return LocaliteWrapper.get(serviceInfrastructure.getLocalite(numeroOrdre));
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
			return CollectiviteAdministrativeWrapper.get(serviceInfrastructure.getCollectivite(noColAdm));
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
			CollectiviteAdministrativeWrapper oid = CollectiviteAdministrativeWrapper.get(serviceInfrastructure.getOidDeCommune(noCommune));
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
				CollectiviteAdministrative oid = CollectiviteAdministrativeWrapper.get(c);
				offices.add((OfficeImpot) oid);
			}
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces aux collectivites administratives", e);
		}
		return Collections.unmodifiableList(offices);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {

		List<CollectiviteAdministrative> collectivites = new ArrayList<CollectiviteAdministrative>();
		try {
			// TODO (FDE) A changer lors de la prochaine mise en prod des interfaces
			CantonImpl cantonVaud = new CantonImpl();
			cantonVaud.setSigleOFS(ServiceInfrastructureService.SIGLE_CANTON_VD);
			List<?> list = serviceInfrastructure.getCollectivitesAdministratives(cantonVaud);
			for (Object o : list) {
				ch.vd.infrastructure.model.CollectiviteAdministrative c = (ch.vd.infrastructure.model.CollectiviteAdministrative) o;
				collectivites.add(CollectiviteAdministrativeWrapper.get(c));
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
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite)
			throws InfrastructureException {

		List<CollectiviteAdministrative> collectivites = new ArrayList<CollectiviteAdministrative>();
		try {
			final EnumTypeCollectivite[] tabTypesCollectivite = new EnumTypeCollectivite[typesCollectivite.size()] ;
			final Iterator<EnumTypeCollectivite> itTypesCol = typesCollectivite.iterator();
			int i = 0;
			while (itTypesCol.hasNext()) {
				final EnumTypeCollectivite typeCol = itTypesCol.next();
				tabTypesCollectivite[i] = typeCol;
				i++;
			}
			final List<?> list = serviceInfrastructure.getCollectivitesAdministratives(tabTypesCollectivite);
			for (Object o : list) {
				final ch.vd.infrastructure.model.CollectiviteAdministrative c = (ch.vd.infrastructure.model.CollectiviteAdministrative) o;

				boolean inclureCollectivite = true;
				if (c.getDateFinValidite() != null) {
					final RegDate finValidite = RegDate.get(c.getDateFinValidite());
					final RegDate now = RegDate.get();
					if (!RegDateHelper.isAfterOrEqual(finValidite, now, NullDateBehavior.LATEST)) {
						inclureCollectivite = false;
					}
				}
				if (inclureCollectivite) {
					collectivites.add(CollectiviteAdministrativeWrapper.get(c));
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
	 * @throws Exception
	 */
	public CollectiviteAdministrative getACI() throws InfrastructureException {
		if (aci == null) {
			try {
				aci = CollectiviteAdministrativeWrapper.get(serviceInfrastructure.getCollectivite(noACI));
			}
			catch (RemoteException e) {
				throw new InfrastructureException("Acces a la collectivite administrative", e);
			}
		}
		return aci;
	}

	/**
	 * @return la collectivite administrative du CEDI
	 * @throws Exception
	 */
	public CollectiviteAdministrative getCEDI() throws InfrastructureException {
		if (cedi == null) {
			try {
				cedi = CollectiviteAdministrativeWrapper.get(serviceInfrastructure.getCollectivite(noCEDI));
			}
			catch (RemoteException e) {
				throw new InfrastructureException("Acces a la collectivite administrative", e);
			}
		}
		return cedi;
	}

	/**
	 * @return la collectivite administrative du CAT
	 * @throws Exception
	 */
	public CollectiviteAdministrative getCAT() throws InfrastructureException {
		if (cat == null) {
			try {
				cat = CollectiviteAdministrativeWrapper.get(serviceInfrastructure.getCollectivite(noCAT));
			}
			catch (RemoteException e) {
				throw new InfrastructureException("Acces a la collectivite administrative", e);
			}
		}
		return cat;
	}

	public InstitutionFinanciere getInstitutionFinanciere(int id) throws InfrastructureException {
		try {
			return InstitutionFinanciereWrapper.get(serviceInfrastructure.getInstitutionFinanciere(id));
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
				list.add(InstitutionFinanciereWrapper.get(i));
			}
			return list;
		}
		catch (RemoteException e) {
			throw new InfrastructureException("Acces à l'institution financière", e);
		}
	}
}
