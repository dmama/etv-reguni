package ch.vd.unireg.interfaces.infra.host;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.infrastructure.model.rest.ListeCollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.TypeCollectivite;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.interfaces.infra.InfrastructureConnector;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrativeImpl;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.wsclient.host.interfaces.ServiceInfrastructureClient;
import ch.vd.unireg.wsclient.host.interfaces.ServiceInfrastructureClientException;

/**
 * @author Baba NGOM
 */
public class InfrastructureConnectorHostInterfaces implements InfrastructureConnector {

	//private static final Logger LOGGER = LoggerFactory.getLogger(ServiceInfrastructureHostInterfaces.class);

	private ServiceInfrastructureClient client;

	/**
	 * Type de collectivite administrative OID
	 */
	public static final Integer TYPE_COLLECTIVITE_OID = 2;

	public static final String CODE_SUISSE = "CH";

	private Map<Integer, Localite> localitesByNPA;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setClient(ServiceInfrastructureClient client) {
		this.client = client;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Canton> getAllCantons() throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getAllCantons' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Commune> getListeCommunes(final Canton canton) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getListeCommunes' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getCommuneByEgid' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings({"unchecked"})
	public List<Commune> getCommunesVD() throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getCommunesVD' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Commune> getListeCommunesFaitieres() throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getListeCommunesFaitieres' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Commune> getCommunes() throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getCommunes' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<ch.vd.unireg.interfaces.infra.data.Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getLocaliteByNPA' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getCommuneHistoByNumeroOfs' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Pays> getPays() throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getPays' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getPaysHisto' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getPays' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getPays' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getCommuneByLocalite' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'findCommuneByNomOfficiel' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Localite> getLocalites() throws InfrastructureException {

		throw new NotImplementedException("La méthode 'getLocalites' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Rue> getRues(Localite localite) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getRues' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Rue> getRuesHisto(int numero) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getRuesHisto' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Rue getRueByNumero(int numero, RegDate date) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getRueByNumero' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Localite> getLocalitesByONRP(int numeroOrdre) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getLocalitesByONRP' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public Localite getLocaliteByONRP(int onrp, RegDate dateReference) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getLocaliteByONRP' ne doit pas être appelée sur le service host-interfaces.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException {
		try {
			return CollectiviteAdministrativeImpl.get(client.getCollectivite(noColAdm));
		}
		catch (ServiceInfrastructureClientException e) {
			throw new InfrastructureException("Acces a la collectivite administrative", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException {

		List<OfficeImpot> offices = new ArrayList<>();
		try {
			TypeCollectivite type = new TypeCollectivite(CollectiviteAdministrativeImpl.SIGLE_OID, null, null, 0);
			TypeCollectivite[] types = new TypeCollectivite[1];
			types[0] = type;
			final ListeCollectiviteAdministrative collectivitesAdministratives = client.getCollectivitesAdministratives(types);
			for (ch.vd.infrastructure.model.rest.CollectiviteAdministrative c : collectivitesAdministratives.getCollectiviteAdministrative()) {
				if (isValid(XmlUtils.xmlcal2date(c.getDateFinValidite()))) {
					CollectiviteAdministrative oid = CollectiviteAdministrativeImpl.get(c);
					offices.add((OfficeImpot) oid);
				}
			}

		}
		catch (ServiceInfrastructureClientException e) {
			throw new InfrastructureException("Acces aux collectivites administratives", e);
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
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {

		final List<CollectiviteAdministrative> collectivites = new ArrayList<>();
		try {
			final ListeCollectiviteAdministrative collectivitesAdministratives = client.getCollectivitesAdministratives(InfrastructureConnector.SIGLE_CANTON_VD);
			for (ch.vd.infrastructure.model.rest.CollectiviteAdministrative c : collectivitesAdministratives.getCollectiviteAdministrative()) {
				collectivites.add(CollectiviteAdministrativeImpl.get(c));
			}
		}
		catch (ServiceInfrastructureClientException e) {
			throw new InfrastructureException("Acces aux collectivites administratives", e);
		}
		return Collections.unmodifiableList(collectivites);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings({"unchecked"})
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<ch.vd.unireg.interfaces.infra.data.TypeCollectivite> typesCollectivite)
			throws InfrastructureException {

		final List<CollectiviteAdministrative> collectivites = new ArrayList<>();
		try {

			TypeCollectivite[] tabTypesCollectivite = new TypeCollectivite[typesCollectivite.size()];
			int i = 0;

			for (ch.vd.unireg.interfaces.infra.data.TypeCollectivite typeCollectivite : typesCollectivite) {
				tabTypesCollectivite[i] = new ch.vd.infrastructure.model.rest.TypeCollectivite(typeCollectivite.getCode(), null, null, 0);
				i++;
			}

			final ListeCollectiviteAdministrative listeCollectiviteAdministrative = client.getCollectivitesAdministratives(tabTypesCollectivite);
			for (ch.vd.infrastructure.model.rest.CollectiviteAdministrative c : listeCollectiviteAdministrative.getCollectiviteAdministrative()) {
				collectivites.add(CollectiviteAdministrativeImpl.get(c));
			}
		}
		catch (ServiceInfrastructureClientException e) {
			throw new InfrastructureException("Acces aux collectivites administratives", e);
		}
		return Collections.unmodifiableList(collectivites);

	}

	@Override
	public String getUrl(ApplicationFiscale application, @Nullable Map<String, String> parametres) {
		throw new NotImplementedException("La méthode 'getUrl' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public Logiciel getLogiciel(Long idLogiciel) throws InfrastructureException {
		throw new NotImplementedException("La méthode 'getLogiciel' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() throws InfrastructureException {
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
	public List<CollectiviteAdministrative> findCollectivitesAdministratives(List<Integer> codeCollectivites, boolean inactif) {
		throw new NotImplementedException("La méthode 'findCollectivitesAdministratives' ne doit pas être appelée sur le service host-interface.");
	}

	@Override
	public void ping() throws InfrastructureException {
		try {
			client.ping();
		}
		catch (Exception e) {
			throw new InfrastructureException(e);
		}
	}
}
