package ch.vd.unireg.interfaces.infra.host;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.infrastructure.model.EnumPays;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.infrastructure.service.ServiceInfrastructure;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CantonImpl;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeImpl;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.CommuneImpl;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciereImpl;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.LocaliteImpl;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.PaysImpl;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.RueImpl;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.common.JvmVersionHelper;

/**
 * @author Jean-Eric CUENDET
 *
 */
public class ServiceInfrastructureHostInterfaces implements ServiceInfrastructureRaw {

	//private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInfrastructureHostInterfaces.class);

	private ServiceInfrastructure serviceInfrastructure;


	/**
	 * Type de collectivite administrative OID
	 */
	public static final Integer TYPE_COLLECTIVITE_OID = 2;

	private Map<Integer,Localite> localitesByNPA;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfrastructure(ServiceInfrastructure serviceInfrastructure) {
		this.serviceInfrastructure = serviceInfrastructure;
	}



	public ServiceInfrastructureHostInterfaces() {
		JvmVersionHelper.checkJvmWrtHostInterfaces();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		List<Canton> cantons = new ArrayList<>();
		try {
			List<?> list = serviceInfrastructure.getCantons(serviceInfrastructure.getPays(EnumPays.SIGLE_CH));
			for (Object o : list) {
				ch.vd.infrastructure.model.Canton c = (ch.vd.infrastructure.model.Canton) o;
				cantons.add(CantonImpl.get(c));
			}
		}
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la liste des cantons impossible", e);
		}
		return Collections.unmodifiableList(cantons);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Commune> getListeCommunes(final Canton canton) throws ServiceInfrastructureException {
		try {
			final List<?> list = serviceInfrastructure.getCommunes(canton.getSigleOFS());
			List<Commune> communes = new ArrayList<>();
			for (Object o : list) {
				ch.vd.infrastructure.model.Commune co = (ch.vd.infrastructure.model.Commune) o;
				communes.add(CommuneImpl.get(co));
			}
			return Collections.unmodifiableList(communes);
		}
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la liste des communes impossible", e);
		}
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getCommuneByEgid' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings({"unchecked"})
	public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
		try {
			final List<ch.vd.infrastructure.model.Commune> list = serviceInfrastructure.getCommunes(ServiceInfrastructureRaw.SIGLE_CANTON_VD);
			final List<Commune> communes = new ArrayList<>();
			for (ch.vd.infrastructure.model.Commune co : list) {
				if (!co.isPrincipale()) {
					communes.add(CommuneImpl.get(co));
				}
			}
			return Collections.unmodifiableList(communes);
		}
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la liste des fractions de communes impossible", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		List<Commune> communes = new ArrayList<>();
		for (Canton canton : getAllCantons()) {
			List<Commune> liste = getListeCommunes(canton);
			communes.addAll(liste);
		}
		return Collections.unmodifiableList(communes);
	}

	@Override
	public List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws ServiceInfrastructureException {
		if (localitesByNPA == null) {
			initLocaliteByNPA();
		}
		final Localite result = localitesByNPA.get(npa);
		return result == null ? null : Collections.singletonList(result);
	}


	private void initLocaliteByNPA() throws ServiceInfrastructureException {
		List<Localite> localites = getLocalites();
		localitesByNPA = new HashMap<>();
		for (Localite localite : localites) {
			if (localite.getNPA()!=null) {
				localitesByNPA.put(localite.getNPA(), localite);
			}

		}
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		final List<Commune> list = new ArrayList<>(2);
		final List<Commune> communes = getCommunes();
		for (Commune commune : communes) {
			if (commune.getNoOFS() == noOfsCommune) {
				list.add(commune);
			}
		}
		return list;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		List<Pays> pays = new ArrayList<>();
		try {
			List<?> list = serviceInfrastructure.getListePays();
			for (Object o : list) {
				ch.vd.infrastructure.model.Pays p = (ch.vd.infrastructure.model.Pays) o;
				pays.add(PaysImpl.get(p));
			}
		}
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la liste des pays", e);
		}
		return Collections.unmodifiableList(pays);
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getPaysHisto' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getPays' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getPays' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
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
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la commune " + numOrdreP, e);
		}
	}

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'findCommuneByNomOfficiel' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {

		List<Localite> localites = new ArrayList<>();
		try {
			for (Canton c : getAllCantons()) {
				List<?> localitesTmp = serviceInfrastructure.getLocalites(c.getSigleOFS());
				for (Object o : localitesTmp) {
					ch.vd.infrastructure.model.Localite l = (ch.vd.infrastructure.model.Localite) o;
					localites.add(LocaliteImpl.get(l));
				}
			}
		}
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Impossible de récupérer les liste des localites", e);
		}
		return Collections.unmodifiableList(localites);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		List<Rue> rues = new ArrayList<>();
		try {
			final List<?> list = serviceInfrastructure.getRues(localite.getNoOrdre());
			for (Object o : list) {
				ch.vd.infrastructure.model.Rue r = (ch.vd.infrastructure.model.Rue) o;
				rues.add(RueImpl.get(r));
			}
		}
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la liste des rues", e);
		}
		return Collections.unmodifiableList(rues);
	}

	@Override
	public List<Rue> getRuesHisto(int numero) throws ServiceInfrastructureException {
		final Rue rue = getRueByNumero(numero, null);
		return rue != null ? Collections.singletonList(rue) : Collections.<Rue>emptyList();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Rue getRueByNumero(int numero, RegDate date) throws ServiceInfrastructureException {
		try {
			return RueImpl.get(serviceInfrastructure.getRueByNumero(numero));
		}
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la liste des rues", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Localite> getLocalitesByONRP(int numeroOrdre) throws ServiceInfrastructureException {
		try {
			final Localite localite = LocaliteImpl.get(serviceInfrastructure.getLocalite(numeroOrdre));
			return localite == null ? Collections.<Localite>emptyList() : Collections.singletonList(localite);
		}
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la localite", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		try {
			return CollectiviteAdministrativeImpl.get(serviceInfrastructure.getCollectivite(noColAdm), serviceInfrastructure);
		}
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la collectivite administrative", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {

		List<OfficeImpot> offices = new ArrayList<>();
		try {
			List<?> list = serviceInfrastructure.getCollectivites(TYPE_COLLECTIVITE_OID);
			for (Object o : list) {
				ch.vd.infrastructure.model.CollectiviteAdministrative c = (ch.vd.infrastructure.model.CollectiviteAdministrative) o;
				if (isValid(c.getDateFinValidite())) {
					CollectiviteAdministrative oid = CollectiviteAdministrativeImpl.get(c, serviceInfrastructure);
					offices.add((OfficeImpot) oid);
				}
			}
		}
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces aux collectivites administratives", e);
		}
		return Collections.unmodifiableList(offices);
	}

	private static boolean isValid(Date dateFinValidite) {
		final boolean valide;
		if (dateFinValidite != null) {
			final RegDate finValidite = RegDateHelper.get(dateFinValidite);
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
	@Override
	@SuppressWarnings({"unchecked"})
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {

		final List<CollectiviteAdministrative> collectivites = new ArrayList<>();
		try {
			final List<ch.vd.infrastructure.model.CollectiviteAdministrative> list = serviceInfrastructure.getCollectivitesAdministratives(ServiceInfrastructureRaw.SIGLE_CANTON_VD);
			for (ch.vd.infrastructure.model.CollectiviteAdministrative c : list) {
				if (isValid(c.getDateFinValidite())) {
					collectivites.add(CollectiviteAdministrativeImpl.get(c, serviceInfrastructure));
				}
			}
		}
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces aux collectivites administratives", e);
		}
		return Collections.unmodifiableList(collectivites);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings({"unchecked"})
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite)
			throws ServiceInfrastructureException {

		final List<CollectiviteAdministrative> collectivites = new ArrayList<>();
		final EnumTypeCollectivite[] tabTypesCollectivite = new EnumTypeCollectivite[typesCollectivite.size()];
		try {
			int i = 0;
			for (TypeCollectivite typeCollectivite : typesCollectivite) {
				tabTypesCollectivite[i] = EnumTypeCollectivite.getEnum(typeCollectivite.getCode());
				i++;
			}

			final List<ch.vd.infrastructure.model.CollectiviteAdministrative> list = serviceInfrastructure.getCollectivitesAdministratives(tabTypesCollectivite);
			for (ch.vd.infrastructure.model.CollectiviteAdministrative c : list) {
				if (isValid(c.getDateFinValidite())) {
					collectivites.add(CollectiviteAdministrativeImpl.get(c, serviceInfrastructure));
				}
			}
		}
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces aux collectivites administratives", e);
		}
		return Collections.unmodifiableList(collectivites);

	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {
		try {
			return InstitutionFinanciereImpl.get(serviceInfrastructure.getInstitutionFinanciere(id));
		}
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces à l'institution financière", e);
		}
	}

	@Override
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
		try {
			List<?> l = serviceInfrastructure.getInstitutionsFinancieres(noClearing);
			List<InstitutionFinanciere> list = new ArrayList<>(l.size());
			for (Object o : l) {
				ch.vd.registre.common.model.InstitutionFinanciere i = (ch.vd.registre.common.model.InstitutionFinanciere) o;
				list.add(InstitutionFinanciereImpl.get(i));
			}
			return list;
		}
		catch (RemoteException | InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces à l'institution financière", e);
		}
	}

	@Override
	public String getUrlVers(ApplicationFiscale application, Long tiersId, Integer oid) {
		throw new NotImplementedException("La méthode 'getUrlVers' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public Logiciel getLogiciel(Long idLogiciel) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getLogiciel' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getTousLesLogiciels' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public District getDistrict(int code) {
		throw new NotImplementedException("La méthode 'getDistrict' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public Region getRegion(int code) {
		throw new NotImplementedException("La méthode 'getRegion' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<TypeRegimeFiscal> getTousLesRegimesFiscaux() {
		throw new NotImplementedException("La méthode 'getTousLesRegimesFiscaux' ne doit pas être appelée sur le service host-interface.");
	}

	@Override
	public List<GenreImpotMandataire> getTousLesGenresImpotMandataires() {
		throw new NotImplementedException("La méthode 'getTousLesGenresImpotMandataires' ne doit pas être appelée sur le service host-interface.");
	}

	@Override
	public void ping() throws ServiceInfrastructureException {
		try {
			serviceInfrastructure.ping();
		}
		catch (Exception e) {
			throw new ServiceInfrastructureException(e);
		}
	}
}
