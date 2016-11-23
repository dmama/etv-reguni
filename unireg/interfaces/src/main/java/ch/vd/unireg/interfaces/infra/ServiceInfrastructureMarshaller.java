package ch.vd.unireg.interfaces.infra;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
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
 * Service de transition qui délégue les appels au service host-interfaces ou au service Fidor.
 */
public class ServiceInfrastructureMarshaller implements ServiceInfrastructureRaw {

	private ServiceInfrastructureRaw hostService = null;
	private ServiceInfrastructureRaw fidorService = null;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHostService(ServiceInfrastructureRaw hostService) {
		this.hostService = hostService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setFidorService(ServiceInfrastructureRaw fidorService) {
		this.fidorService = fidorService;
	}

	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		return fidorService.getAllCantons();
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		return fidorService.getCommuneHistoByNumeroOfs(noOfsCommune);
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {
		return fidorService.getNoOfsCommuneByEgid(egid, date);
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
		return fidorService.getListeCommunes(canton);
	}

	@Override
	public List<Localite> getLocalitesByONRP(int onrp) throws ServiceInfrastructureException {
		return fidorService.getLocalitesByONRP(onrp);
	}

	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		return fidorService.getLocalites();
	}

	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		return fidorService.getPays();
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws ServiceInfrastructureException {
		return fidorService.getPaysHisto(numeroOFS);
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws ServiceInfrastructureException {
		return fidorService.getPays(numeroOFS, date);
	}

	@Override
	public Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws ServiceInfrastructureException {
		return fidorService.getPays(codePays, date);
	}

	@Override
	public Rue getRueByNumero(int numero, RegDate date) throws ServiceInfrastructureException {
		return fidorService.getRueByNumero(numero, date);
	}

	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		return fidorService.getRues(localite);
	}

	@Override
	public List<Rue> getRuesHisto(int numero) throws ServiceInfrastructureException {
		return fidorService.getRuesHisto(numero);
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		return hostService.getCollectivite(noColAdm);
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		return hostService.getOfficesImpot();
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		return hostService.getCollectivitesAdministratives();
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
		return fidorService.getCommuneByLocalite(localite);
	}

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws ServiceInfrastructureException {
		return fidorService.findCommuneByNomOfficiel(nomOfficiel, includeFaitieres, includeFractions, date);
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {
		return hostService.getCollectivitesAdministratives(typesCollectivite);
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {
		return hostService.getInstitutionFinanciere(id);
	}

	@Override
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
		return hostService.getInstitutionsFinancieres(noClearing);
	}

	@Override
	public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
		return fidorService.getListeFractionsCommunes();
	}

	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		return fidorService.getCommunes();
	}

	@Override
	public List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws ServiceInfrastructureException {
		return fidorService.getLocalitesByNPA(npa, dateReference);
	}

	@Override
	public String getUrlVers(ApplicationFiscale application, Long tiersId, Integer oid) {
		return fidorService.getUrlVers(application, tiersId, oid);
	}

	@Override
	public Logiciel getLogiciel(Long idLogiciel) throws ServiceInfrastructureException {
		return fidorService.getLogiciel(idLogiciel);
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() throws ServiceInfrastructureException {
		return fidorService.getTousLesLogiciels();
	}

	@Override
	public District getDistrict(int code) {
		return fidorService.getDistrict(code);
	}

	@Override
	public Region getRegion(int code) {
		return fidorService.getRegion(code);
	}

	@Override
	public List<TypeRegimeFiscal> getTousLesRegimesFiscaux() {
		return fidorService.getTousLesRegimesFiscaux();
	}

	@Override
	public List<GenreImpotMandataire> getTousLesGenresImpotMandataires() {
		return fidorService.getTousLesGenresImpotMandataires();
	}

	@Override
	public void ping() throws ServiceInfrastructureException {
		hostService.ping();
		fidorService.ping();
	}
}
