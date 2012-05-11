package ch.vd.unireg.interfaces.infra.host;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.infrastructure.fiscal.service.ServiceInfrastructureFiscal;
import ch.vd.infrastructure.model.EnumPays;
import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.infrastructure.service.ServiceInfrastructure;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CantonImpl;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeImpl;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.CommuneImpl;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciereImpl;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.LocaliteImpl;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.PaysImpl;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.RueImpl;
import ch.vd.unireg.interfaces.infra.data.TypeEtatPM;
import ch.vd.unireg.interfaces.infra.data.TypeEtatPMImpl;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscalImpl;
import ch.vd.uniregctb.common.JvmVersionHelper;

/**
 * @author Jean-Eric CUENDET
 *
 */
public class ServiceInfrastructureHostInterfaces implements ServiceInfrastructureRaw {

	//private static final Logger LOGGER = Logger.getLogger(ServiceInfrastructureHostInterfaces.class);

	private ServiceInfrastructure serviceInfrastructure;
	private ServiceInfrastructureFiscal serviceInfrastructureFiscal;

	/**
	 * Type de collectivite administrative OID
	 */
	public static final Integer TYPE_COLLECTIVITE_OID = 2;

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
	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		List<Canton> cantons = new ArrayList<Canton>();
		try {
			List<?> list = serviceInfrastructure.getCantons(serviceInfrastructure.getPays(EnumPays.SIGLE_CH));
			for (Object o : list) {
				ch.vd.infrastructure.model.Canton c = (ch.vd.infrastructure.model.Canton) o;
				cantons.add(CantonImpl.get(c));
			}
		}
		catch (RemoteException e) {
			throw new ServiceInfrastructureException("Acces a la liste des cantons impossible", e);
		}
		catch (InfrastructureException e) {
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
			List<Commune> communes = new ArrayList<Commune>();
			for (Object o : list) {
				ch.vd.infrastructure.model.Commune co = (ch.vd.infrastructure.model.Commune) o;
				communes.add(CommuneImpl.get(co));
			}
			return Collections.unmodifiableList(communes);
		}
		catch (RemoteException e) {
			throw new ServiceInfrastructureException("Acces a la liste des communes impossible", e);
		}
		catch (InfrastructureException e) {
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
			final List<Commune> communes = new ArrayList<Commune>();
			for (ch.vd.infrastructure.model.Commune co : list) {
				if (!co.isPrincipale()) {
					communes.add(CommuneImpl.get(co));
				}
			}
			return Collections.unmodifiableList(communes);
		}
		catch (RemoteException e) {
			throw new ServiceInfrastructureException("Acces a la liste des fractions de communes impossible", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la liste des fractions de communes impossible", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		List<Commune> communes = new ArrayList<Commune>();
		for (Canton canton : getAllCantons()) {
			List<Commune> liste = getListeCommunes(canton);
			communes.addAll(liste);
		}
		return Collections.unmodifiableList(communes);
	}

	@Override
	public Localite getLocaliteByNPA(int npa) throws ServiceInfrastructureException {
		if (localitesByNPA==null) {
			initLocaliteByNPA();
		}
		return localitesByNPA.get(npa);
	}


	private void initLocaliteByNPA() throws ServiceInfrastructureException {
		List<Localite> localites = getLocalites();
		localitesByNPA = new HashMap<Integer, Localite>();
		for (Localite localite : localites) {
			if (localite.getNPA()!=null) {
				localitesByNPA.put(localite.getNPA(), localite);
			}

		}
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		final List<Commune> list = new ArrayList<Commune>(2);
		final List<Commune> communes = getCommunes();
		for (Commune commune : communes) {
			if (commune.getNoOFSEtendu() == noOfsCommune) {
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
		List<Pays> pays = new ArrayList<Pays>();
		try {
			List<?> list = serviceInfrastructure.getListePays();
			for (Object o : list) {
				ch.vd.infrastructure.model.Pays p = (ch.vd.infrastructure.model.Pays) o;
				pays.add(PaysImpl.get(p));
			}
		}
		catch (RemoteException e) {
			throw new ServiceInfrastructureException("Acces a la liste des pays", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la liste des pays", e);
		}
		return Collections.unmodifiableList(pays);
	}

	@Override
	public Pays getPays(int numeroOFS) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getPays' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public Pays getPays(String codePays) throws ServiceInfrastructureException {
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
		catch (RemoteException e) {
			throw new ServiceInfrastructureException("Acces a la commune " + numOrdreP, e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la commune " + numOrdreP, e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {

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
			throw new ServiceInfrastructureException("Impossible de récupérer les liste des localites", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Impossible de récupérer les liste des localites", e);
		}
		return Collections.unmodifiableList(localites);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		List<Rue> rues = new ArrayList<Rue>();
		try {
			final List<?> list = serviceInfrastructure.getRues(localite.getNoOrdre());
			for (Object o : list) {
				ch.vd.infrastructure.model.Rue r = (ch.vd.infrastructure.model.Rue) o;
				rues.add(RueImpl.get(r));
			}
		}
		catch (RemoteException e) {
			throw new ServiceInfrastructureException("Acces a la liste des rues", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la liste des rues", e);
		}
		return Collections.unmodifiableList(rues);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Rue> getRues(Canton canton) throws ServiceInfrastructureException {
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
			throw new ServiceInfrastructureException("Acces a la liste des rues", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la liste des rues", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Rue getRueByNumero(int numero) throws ServiceInfrastructureException {
		try {
			return RueImpl.get(serviceInfrastructure.getRueByNumero(numero));
		}
		catch (RemoteException e) {
			throw new ServiceInfrastructureException("Acces a la liste des rues", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la liste des rues", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Localite getLocaliteByONRP(int numeroOrdre) throws ServiceInfrastructureException {
		try {
			return LocaliteImpl.get(serviceInfrastructure.getLocalite(numeroOrdre));
		}
		catch (RemoteException e) {
			throw new ServiceInfrastructureException("Acces a la localite", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la localite", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		try {
			return CollectiviteAdministrativeImpl.get(serviceInfrastructure.getCollectivite(noColAdm));
		}
		catch (RemoteException e) {
			throw new ServiceInfrastructureException("Acces a la collectivite administrative", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la collectivite administrative", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws ServiceInfrastructureException {
		try {
			CollectiviteAdministrativeImpl oid = CollectiviteAdministrativeImpl.get(serviceInfrastructure.getOidDeCommune(noCommune));
			return (OfficeImpot) oid;
		}
		catch (RemoteException e) {
			throw new ServiceInfrastructureException("Acces a la collectivite administrative", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces a la collectivite administrative", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {

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
			throw new ServiceInfrastructureException("Acces aux collectivites administratives", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces aux collectivites administratives", e);
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
	@Override
	@SuppressWarnings({"unchecked"})
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {

		final List<CollectiviteAdministrative> collectivites = new ArrayList<CollectiviteAdministrative>();
		try {
			final List<ch.vd.infrastructure.model.CollectiviteAdministrative> list = serviceInfrastructure.getCollectivitesAdministratives(ServiceInfrastructureRaw.SIGLE_CANTON_VD);
			for (ch.vd.infrastructure.model.CollectiviteAdministrative c : list) {
				if (isValid(c.getDateFinValidite())) {
					collectivites.add(CollectiviteAdministrativeImpl.get(c));
				}
			}
		}
		catch (RemoteException e) {
			throw new ServiceInfrastructureException("Acces aux collectivites administratives", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces aux collectivites administratives", e);
		}
		return Collections.unmodifiableList(collectivites);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings({"unchecked"})
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite)
			throws ServiceInfrastructureException {

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
			throw new ServiceInfrastructureException("Acces aux collectivites administratives", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces aux collectivites administratives", e);
		}
		return Collections.unmodifiableList(collectivites);

	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {
		try {
			return InstitutionFinanciereImpl.get(serviceInfrastructure.getInstitutionFinanciere(id));
		}
		catch (RemoteException e) {
			throw new ServiceInfrastructureException("Acces à l'institution financière", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces à l'institution financière", e);
		}
	}

	@Override
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
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
			throw new ServiceInfrastructureException("Acces à l'institution financière", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces à l'institution financière", e);
		}
	}

	@Override
	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws ServiceInfrastructureException {
		try {
			ch.vd.infrastructure.fiscal.model.TypeRegimeFiscal[] types = serviceInfrastructureFiscal.getTypeRegimesFiscaux();
			List<TypeRegimeFiscal> list = new ArrayList<TypeRegimeFiscal>(types.length);
			for (ch.vd.infrastructure.fiscal.model.TypeRegimeFiscal type : types) {
				list.add(TypeRegimeFiscalImpl.get(type));
			}
			return list;
		}
		catch (RemoteException e) {
			throw new ServiceInfrastructureException("Acces aux types de régimes fiscaux", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces aux types de régimes fiscaux", e);
		}
	}

	@Override
	public TypeRegimeFiscal getTypeRegimeFiscal(String code) throws ServiceInfrastructureException {
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

	@Override
	public List<TypeEtatPM> getTypesEtatsPM() throws ServiceInfrastructureException {
		try {
			ch.vd.infrastructure.fiscal.model.TypeEtatPM[] types = serviceInfrastructureFiscal.getTypesEtatsPM();
			List<TypeEtatPM> list = new ArrayList<TypeEtatPM>(types.length);
			for (ch.vd.infrastructure.fiscal.model.TypeEtatPM type : types) {
				list.add(TypeEtatPMImpl.get(type));
			}
			return list;
		}
		catch (RemoteException e) {
			throw new ServiceInfrastructureException("Acces aux types des états PM", e);
		}
		catch (InfrastructureException e) {
			throw new ServiceInfrastructureException("Acces aux types des états PM", e);
		}
	}

	@Override
	public TypeEtatPM getTypeEtatPM(String code) throws ServiceInfrastructureException {
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

	@Override
	public String getUrlVers(ApplicationFiscale application, Long tiersId) {
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
}
