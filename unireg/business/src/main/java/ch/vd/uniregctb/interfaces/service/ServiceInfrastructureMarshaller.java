package ch.vd.uniregctb.interfaces.service;

import java.util.List;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.ApplicationFiscale;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.TypeEtatPM;
import ch.vd.uniregctb.interfaces.model.TypeRegimeFiscal;

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

	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		return hostService.getAllCantons();
	}

	public Commune getCommuneByNumeroOfsEtendu(int noCommune, RegDate date) throws ServiceInfrastructureException {
		return hostService.getCommuneByNumeroOfsEtendu(noCommune, date);
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date, int hintNoOfsCommune) throws ServiceInfrastructureException {
		// TODO (msi) réactiver cet appel quand fidor sera disponible en intégration
		if (true) {
			return null;
		}
		return fidorService.getNoOfsCommuneByEgid(egid, date, hintNoOfsCommune);
	}

	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
		return hostService.getListeCommunes(canton);
	}

	public Localite getLocaliteByONRP(int onrp) throws ServiceInfrastructureException {
		return hostService.getLocaliteByONRP(onrp);
	}

	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		return hostService.getLocalites();
	}

	public List<Pays> getPays() throws ServiceInfrastructureException {
		return fidorService.getPays();
	}

	public Rue getRueByNumero(int numero) throws ServiceInfrastructureException {
		return hostService.getRueByNumero(numero);
	}

	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		return hostService.getRues(localite);
	}

	public List<Rue> getRues(Canton canton) throws ServiceInfrastructureException {
		return hostService.getRues(canton);
	}

	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		return hostService.getCollectivite(noColAdm);
	}

	public OfficeImpot getOfficeImpot(int noColAdm) throws ServiceInfrastructureException {
		return hostService.getOfficeImpot(noColAdm);
	}

	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws ServiceInfrastructureException {
		return hostService.getOfficeImpotDeCommune(noCommune);
	}

	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		return hostService.getOfficesImpot();
	}

	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		return hostService.getCollectivitesAdministratives();
	}

	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
		return hostService.getCommuneByLocalite(localite);
	}

	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite)
			throws ServiceInfrastructureException {
		return hostService.getCollectivitesAdministratives();
	}

	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {
		return hostService.getInstitutionFinanciere(id);
	}

	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
		return hostService.getInstitutionsFinancieres(noClearing);
	}

	public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
		return hostService.getListeFractionsCommunes();
	}

	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		return hostService.getCommunes();
	}

	public Localite getLocaliteByNPA(int npa) throws ServiceInfrastructureException {
		return hostService.getLocaliteByNPA(npa);
	}

	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws ServiceInfrastructureException {
		return hostService.getTypesRegimesFiscaux();
	}

	public TypeRegimeFiscal getTypeRegimeFiscal(String code) throws ServiceInfrastructureException {
		return hostService.getTypeRegimeFiscal(code);
	}

	public List<TypeEtatPM> getTypesEtatsPM() throws ServiceInfrastructureException {
		return hostService.getTypesEtatsPM();
	}

	public TypeEtatPM getTypeEtatPM(String code) throws ServiceInfrastructureException {
		return hostService.getTypeEtatPM(code);
	}

	public String getUrlVers(ApplicationFiscale application, Long tiersId) {
		return fidorService.getUrlVers(application, tiersId);
	}

	public Logiciel getLogiciel(Long idLogiciel) throws ServiceInfrastructureException {
		return fidorService.getLogiciel(idLogiciel);
	}

	public List<Logiciel> getTousLesLogiciels() throws ServiceInfrastructureException {
		return fidorService.getTousLesLogiciels();
	}
}
