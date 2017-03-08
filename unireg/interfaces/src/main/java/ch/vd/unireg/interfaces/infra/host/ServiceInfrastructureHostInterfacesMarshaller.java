package ch.vd.unireg.interfaces.infra.host;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
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
 * Service de transition qui délégue les appels au service host-interfaces EJB ou REST
 */
public class ServiceInfrastructureHostInterfacesMarshaller implements ServiceInfrastructureRaw {

	private ServiceInfrastructureRaw ejbClient;
	private ServiceInfrastructureRaw restClient;
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

	/**
	 * @return le client effectivement à utiliser
	 */
	private ServiceInfrastructureRaw getClient() {
		return modeRest ? restClient : ejbClient;
	}

	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		return getClient().getAllCantons();
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		return getClient().getCommuneHistoByNumeroOfs(noOfsCommune);
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {
		return getClient().getNoOfsCommuneByEgid(egid, date);
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
		return getClient().getListeCommunes(canton);
	}

	@Override
	public List<Localite> getLocalitesByONRP(int onrp) throws ServiceInfrastructureException {
		return getClient().getLocalitesByONRP(onrp);
	}

	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		return getClient().getLocalites();
	}

	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		return getClient().getPays();
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws ServiceInfrastructureException {
		return getClient().getPaysHisto(numeroOFS);
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws ServiceInfrastructureException {
		return getClient().getPays(numeroOFS, date);
	}

	@Override
	public Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws ServiceInfrastructureException {
		return getClient().getPays(codePays, date);
	}

	@Override
	public Rue getRueByNumero(int numero, RegDate date) throws ServiceInfrastructureException {
		return getClient().getRueByNumero(numero, date);
	}

	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		return getClient().getRues(localite);
	}

	@Override
	public List<Rue> getRuesHisto(int numero) throws ServiceInfrastructureException {
		return getClient().getRuesHisto(numero);
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		return getClient().getCollectivite(noColAdm);
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		return getClient().getOfficesImpot();
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		return getClient().getCollectivitesAdministratives();
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
		return getClient().getCommuneByLocalite(localite);
	}

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws ServiceInfrastructureException {
		return getClient().findCommuneByNomOfficiel(nomOfficiel, includeFaitieres, includeFractions, date);
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {
		return getClient().getCollectivitesAdministratives(typesCollectivite);
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {
		return getClient().getInstitutionFinanciere(id);
	}

	@Override
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
		return getClient().getInstitutionsFinancieres(noClearing);
	}

	@Override
	public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
		return getClient().getListeFractionsCommunes();
	}

	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		return getClient().getCommunes();
	}

	@Override
	public List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws ServiceInfrastructureException {
		return getClient().getLocalitesByNPA(npa, dateReference);
	}

	@Override
	public String getUrlVers(ApplicationFiscale application, Long tiersId, Integer oid) {
		return getClient().getUrlVers(application, tiersId, oid);
	}

	@Override
	public String getUrlVisualisationDocument(Long tiersId, @Nullable Integer pf, Integer oid, String cleDocument) {
		return getClient().getUrlVisualisationDocument(tiersId, pf, oid, cleDocument);
	}

	@Override
	public Logiciel getLogiciel(Long idLogiciel) throws ServiceInfrastructureException {
		return getClient().getLogiciel(idLogiciel);
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() throws ServiceInfrastructureException {
		return getClient().getTousLesLogiciels();
	}

	@Override
	public District getDistrict(int code) {
		return getClient().getDistrict(code);
	}

	@Override
	public Region getRegion(int code) {
		return getClient().getRegion(code);
	}

	@Override
	public List<TypeRegimeFiscal> getTousLesRegimesFiscaux() {
		return getClient().getTousLesRegimesFiscaux();
	}

	@Override
	public List<GenreImpotMandataire> getTousLesGenresImpotMandataires() {
		return getClient().getTousLesGenresImpotMandataires();
	}

	@Override
	public void ping() throws ServiceInfrastructureException {
		getClient().ping();
	}
}
