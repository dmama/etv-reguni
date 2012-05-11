package ch.vd.unireg.interfaces.infra;

import java.util.List;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeEtatPM;
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
		return hostService.getAllCantons();
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
	public Localite getLocaliteByONRP(int onrp) throws ServiceInfrastructureException {
		return hostService.getLocaliteByONRP(onrp);
	}

	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		return hostService.getLocalites();
	}

	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		return fidorService.getPays();
	}

	@Override
	public Pays getPays(int numeroOFS) throws ServiceInfrastructureException {
		return fidorService.getPays(numeroOFS);
	}

	@Override
	public Pays getPays(String codePays) throws ServiceInfrastructureException {
		return fidorService.getPays(codePays);
	}

	@Override
	public Rue getRueByNumero(int numero) throws ServiceInfrastructureException {
		return hostService.getRueByNumero(numero);
	}

	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		return hostService.getRues(localite);
	}

	@Override
	public List<Rue> getRues(Canton canton) throws ServiceInfrastructureException {
		return hostService.getRues(canton);
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		return hostService.getCollectivite(noColAdm);
	}

	@Override
	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws ServiceInfrastructureException {
		return hostService.getOfficeImpotDeCommune(noCommune);
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
		return hostService.getCommuneByLocalite(localite);
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite)
			throws ServiceInfrastructureException {
		return hostService.getCollectivitesAdministratives();
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
	public Localite getLocaliteByNPA(int npa) throws ServiceInfrastructureException {
		return hostService.getLocaliteByNPA(npa);
	}

	@Override
	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws ServiceInfrastructureException {
		return hostService.getTypesRegimesFiscaux();
	}

	@Override
	public TypeRegimeFiscal getTypeRegimeFiscal(String code) throws ServiceInfrastructureException {
		return hostService.getTypeRegimeFiscal(code);
	}

	@Override
	public List<TypeEtatPM> getTypesEtatsPM() throws ServiceInfrastructureException {
		return hostService.getTypesEtatsPM();
	}

	@Override
	public TypeEtatPM getTypeEtatPM(String code) throws ServiceInfrastructureException {
		return hostService.getTypeEtatPM(code);
	}

	@Override
	public String getUrlVers(ApplicationFiscale application, Long tiersId) {
		return fidorService.getUrlVers(application, tiersId);
	}

	@Override
	public Logiciel getLogiciel(Long idLogiciel) throws ServiceInfrastructureException {
		return fidorService.getLogiciel(idLogiciel);
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() throws ServiceInfrastructureException {
		return fidorService.getTousLesLogiciels();
	}
}
