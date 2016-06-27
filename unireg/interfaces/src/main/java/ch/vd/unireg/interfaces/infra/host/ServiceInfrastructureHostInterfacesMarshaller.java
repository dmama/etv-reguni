package ch.vd.unireg.interfaces.infra.host;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;

/**
 * Service de transition qui délégue les appels au service host-interfaces ou au service Fidor.
 */
public class ServiceInfrastructureHostInterfacesMarshaller implements ServiceInfrastructureRaw {

	private ServiceInfrastructureRaw ejbClient = null;
	private ServiceInfrastructureRaw restClient = null;
	private boolean modeRest;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEjbClient(ServiceInfrastructureRaw ejbClient) {
		this.ejbClient = ejbClient;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRestClient(ServiceInfrastructureRaw restClient) {
		this.restClient = restClient;
	}

	public void setModeRest(boolean modeRest) {
		this.modeRest = modeRest;
	}

	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getAllCantons' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getCommuneHistoByNumeroOfs' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getNoOfsCommuneByEgid' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getListeCommunes' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Localite> getLocalitesByONRP(int onrp) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getLocalitesByONRP' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getLocalites' ne doit pas être appelée sur le service host-interfaces.");
	}

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

	@Override
	public Rue getRueByNumero(int numero, RegDate date) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getRueByNumero' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getRues' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Rue> getRuesHisto(int numero) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getRuesHisto' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		if (modeRest) {
			return restClient.getCollectivite(noColAdm);
		}
		return ejbClient.getCollectivite(noColAdm);
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		if (modeRest) {
			return restClient.getOfficesImpot();
		}
		return ejbClient.getOfficesImpot();
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		if (modeRest) {
			return restClient.getCollectivitesAdministratives();
		}
		return ejbClient.getCollectivitesAdministratives();
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getCommuneByLocalite' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {
		if (modeRest) {
			return restClient.getCollectivitesAdministratives(typesCollectivite);
		}
		return ejbClient.getCollectivitesAdministratives(typesCollectivite);
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {
		if (modeRest) {
			return restClient.getInstitutionFinanciere(id);
		}
		return ejbClient.getInstitutionFinanciere(id);
	}

	@Override
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
		if (modeRest) {
			return restClient.getInstitutionsFinancieres(noClearing);
		}
		return ejbClient.getInstitutionsFinancieres(noClearing);
	}

	@Override
	public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getListeFractionsCommunes' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getCommunes' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public Localite getLocaliteByNPA(int npa) throws ServiceInfrastructureException {
		throw new NotImplementedException("La méthode 'getLocaliteByNPA' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public String getUrlVers(ApplicationFiscale application, Long tiersId, Integer oid) {
		return restClient.getUrlVers(application, tiersId, oid);
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
		throw new NotImplementedException("La méthode 'getTousLesRegimesFiscaux' ne doit pas être appelée sur le service host-interfaces.");
	}

	@Override
	public void ping() throws ServiceInfrastructureException {
		if (modeRest) {
			restClient.ping();
		}
		ejbClient.ping();

	}
}
