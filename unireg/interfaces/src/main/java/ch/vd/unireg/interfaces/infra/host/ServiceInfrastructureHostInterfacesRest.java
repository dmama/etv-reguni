package ch.vd.unireg.interfaces.infra.host;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.infrastructure.model.rest.ListeCollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.TypeCollectivite;
import ch.vd.infrastructure.registre.common.model.rest.ListeInstitutionsFinancieres;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.base.xml.XmlUtils;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeImpl;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciereImpl;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.wsclient.host.interfaces.ServiceInfrastructureClient;
import ch.vd.unireg.wsclient.host.interfaces.ServiceInfrastructureClientException;
import ch.vd.uniregctb.common.JvmVersionHelper;

/**
 * @author Baba NGOM
 *
 */
public class ServiceInfrastructureHostInterfacesRest implements ServiceInfrastructureRaw {

	//private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInfrastructureHostInterfaces.class);

	private ServiceInfrastructureClient client;

	/**
	 * Type de collectivite administrative OID
	 */
	public static final Integer TYPE_COLLECTIVITE_OID = 2;

	public static final String CODE_SUISSE = "CH";

	private Map<Integer,Localite> localitesByNPA;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setClient(ServiceInfrastructureClient client) {
		this.client = client;
	}


	public ServiceInfrastructureHostInterfacesRest() {
		JvmVersionHelper.checkJvmWrtHostInterfaces();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getAllCantons' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Commune> getListeCommunes(final Canton canton) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getListeCommunes' ne doit pas être appelée sur le service host-interfaces.");
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
		throw new NotImplementedException("La méthode 'getListeFractionsCommunes' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getCommunes' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<ch.vd.unireg.interfaces.infra.data.Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getLocaliteByNPA' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getCommuneHistoByNumeroOfs' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getPays' ne doit pas être appelée sur le service host-interfaces.");
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
		throw new NotImplementedException("La méthode 'getCommuneByLocalite' ne doit pas être appelée sur le service host-interfaces.");
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

		throw new NotImplementedException("La méthode 'getLocalites' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getRues' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Rue> getRuesHisto(int numero) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getRuesHisto' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Rue getRueByNumero(int numero, RegDate date) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getRueByNumero' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Localite> getLocalitesByONRP(int numeroOrdre) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getLocalitesByONRP' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		try {
			return CollectiviteAdministrativeImpl.get(client.getCollectivite(noColAdm), client);
		}
		catch (ServiceInfrastructureClientException e) {
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
			TypeCollectivite type = new TypeCollectivite(CollectiviteAdministrativeImpl.SIGLE_CIR,null,null,0);
			TypeCollectivite[] types = new TypeCollectivite[1];
			types[0] = type;
			final ListeCollectiviteAdministrative collectivitesAdministratives = client.getCollectivitesAdministratives(types);
			for (ch.vd.infrastructure.model.rest.CollectiviteAdministrative c : collectivitesAdministratives.getCollectiviteAdministrative()) {
				if (isValid(XmlUtils.cal2date(c.getDateFinValidite()))) {
					CollectiviteAdministrative oid = CollectiviteAdministrativeImpl.get(c, client);
					offices.add((OfficeImpot) oid);
				}
			}

		}
		catch (ServiceInfrastructureClientException e) {
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
			final ListeCollectiviteAdministrative collectivitesAdministratives = client.getCollectivitesAdministratives(ServiceInfrastructureRaw.SIGLE_CANTON_VD);
			for (ch.vd.infrastructure.model.rest.CollectiviteAdministrative c : collectivitesAdministratives.getCollectiviteAdministrative()) {
				collectivites.add(CollectiviteAdministrativeImpl.get(c, client));
			}
		}
		catch (ServiceInfrastructureClientException e) {
			throw new ServiceInfrastructureException("Acces aux collectivites administratives", e);
		}
		return Collections.unmodifiableList(collectivites);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings({"unchecked"})
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<ch.vd.unireg.interfaces.infra.data.TypeCollectivite> typesCollectivite)
			throws ServiceInfrastructureException {

		final List<CollectiviteAdministrative> collectivites = new ArrayList<>();
		try {

			TypeCollectivite[] tabTypesCollectivite = new TypeCollectivite[typesCollectivite.size()];
			int i=0;

			for (ch.vd.unireg.interfaces.infra.data.TypeCollectivite typeCollectivite : typesCollectivite) {
				tabTypesCollectivite[i]= new ch.vd.infrastructure.model.rest.TypeCollectivite(typeCollectivite.getCode(),null,null,0);
				i++;
			}

			final ListeCollectiviteAdministrative listeCollectiviteAdministrative = client.getCollectivitesAdministratives(tabTypesCollectivite);
			for (ch.vd.infrastructure.model.rest.CollectiviteAdministrative c : listeCollectiviteAdministrative.getCollectiviteAdministrative()) {
				collectivites.add(CollectiviteAdministrativeImpl.get(c, client));
			}
		}
		catch (ServiceInfrastructureClientException e) {
			throw new ServiceInfrastructureException("Acces aux collectivites administratives", e);
		}
		return Collections.unmodifiableList(collectivites);

	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {

		try {
			return InstitutionFinanciereImpl.get(client.getInstitutionFinanciere(id));

		}
		catch (ServiceInfrastructureClientException e) {
			throw new ServiceInfrastructureException("Accès à l'institut financière " + id, e);
		}

	}

	@Override
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
		if (noClearing == null) {
			return Collections.emptyList();
		}
		final List<InstitutionFinanciere> institutions = new ArrayList<>();
		try {
			final ListeInstitutionsFinancieres listeInstitutionsFinancieres = client.getInstitutionsFinancieres(noClearing);
			for (ch.vd.infrastructure.registre.common.model.rest.InstitutionFinanciere ifin : listeInstitutionsFinancieres.getInstitutionFinanciere()) {
				institutions.add(InstitutionFinanciereImpl.get(ifin));
			}
		}
		catch (ServiceInfrastructureClientException e) {
			throw new ServiceInfrastructureException("Acces aux institutions financières " +noClearing, e);
		}
		return Collections.unmodifiableList(institutions);
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
			client.ping();
		}
		catch (Exception e) {
			throw new ServiceInfrastructureException(e);
		}
	}
}
