package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.ApplicationFiscale;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.model.LogicielMetier;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.TypeAffranchissement;
import ch.vd.uniregctb.interfaces.model.TypeEtatPM;
import ch.vd.uniregctb.interfaces.model.TypeRegimeFiscal;

/**
 * Service de transition qui délégue les appels au service host-interfaces ou au service Fidor.
 */
public class ServiceInfrastructureMarshaller implements ServiceInfrastructureService {

	private ServiceInfrastructureService hostService = null;
	private ServiceInfrastructureService fidorService = null;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHostService(ServiceInfrastructureService hostService) {
		this.hostService = hostService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setFidorService(ServiceInfrastructureService fidorService) {
		this.fidorService = fidorService;
	}

	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		return hostService.getAllCantons();
	}

	public Canton getCanton(int cantonOFS) throws ServiceInfrastructureException {
		return hostService.getCanton(cantonOFS);
	}

	public Canton getCantonBySigle(String sigle) throws ServiceInfrastructureException {
		return hostService.getCantonBySigle(sigle);
	}

	public Canton getCantonByCommune(int noOfsCommune) throws ServiceInfrastructureException {
		return hostService.getCantonByCommune(noOfsCommune);
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

	@Override
	public Commune getCommuneByEgid(int egid, RegDate date, int hintNoOfsCommune) throws ServiceInfrastructureException {
		// TODO (msi) réactiver cet appel quand fidor sera disponible en intégration
		if (true) {
			return null;
		}
		return fidorService.getCommuneByEgid(egid, date, hintNoOfsCommune);
	}

	public List<Commune> getCommunesDeVaud() throws ServiceInfrastructureException {
		return hostService.getCommunesDeVaud();
	}

	public List<Commune> getCommunesHorsCanton() throws ServiceInfrastructureException {
		return hostService.getCommunesHorsCanton();
	}

	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
		return hostService.getListeCommunes(canton);
	}

	public List<Commune> getListeCommunes(int cantonOFS) throws ServiceInfrastructureException {
		return hostService.getListeCommunes(cantonOFS);
	}

	public List<Commune> getListeCommunesByOID(int oid) throws ServiceInfrastructureException {
		return hostService.getListeCommunesByOID(oid);
	}

	public Commune getCommuneFaitiere(Commune commune, RegDate dateReference) throws ServiceInfrastructureException {
		return hostService.getCommuneFaitiere(commune, dateReference);
	}

	public Localite getLocaliteByONRP(int onrp) throws ServiceInfrastructureException {
		return hostService.getLocaliteByONRP(onrp);
	}

	public List<Localite> getLocaliteByCommune(int commune) throws ServiceInfrastructureException {
		return hostService.getLocaliteByCommune(commune);
	}

	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		return hostService.getLocalites();
	}

	public List<Pays> getPays() throws ServiceInfrastructureException {
		return fidorService.getPays();
	}

	public Pays getPays(int numeroOFS) throws ServiceInfrastructureException {
		return fidorService.getPays(numeroOFS);
	}

	public Pays getPays(String codePays) throws ServiceInfrastructureException {
		return fidorService.getPays(codePays);
	}

	public Rue getRueByNumero(int numero) throws ServiceInfrastructureException {
		return hostService.getRueByNumero(numero);
	}

	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		return hostService.getRues(localite);
	}

	public List<Rue> getRues(Collection<Localite> localites) throws ServiceInfrastructureException {
		return hostService.getRues(localites);
	}

	public List<Rue> getRues(Canton canton) throws ServiceInfrastructureException {
		return hostService.getRues(canton);
	}

	public Pays getSuisse() throws ServiceInfrastructureException {
		return hostService.getSuisse();
	}

	public Canton getVaud() throws ServiceInfrastructureException {
		return hostService.getVaud();
	}

	public CollectiviteAdministrative getACI() throws ServiceInfrastructureException {
		return hostService.getACI();
	}

	public CollectiviteAdministrative getACIImpotSource() throws ServiceInfrastructureException {
		return hostService.getACIImpotSource();
	}

	public CollectiviteAdministrative getACISuccessions() throws ServiceInfrastructureException {
		return hostService.getACISuccessions();
	}

	public CollectiviteAdministrative getCEDI() throws ServiceInfrastructureException {
		return hostService.getCEDI();
	}

	public CollectiviteAdministrative getCAT() throws ServiceInfrastructureException {
		return hostService.getCAT();
	}

	public Commune getCommuneByAdresse(Adresse adresse, RegDate date) throws ServiceInfrastructureException {
		return hostService.getCommuneByAdresse(adresse, date);
	}

	public Commune getCommuneByAdresse(AdresseGenerique adresse, RegDate date) throws ServiceInfrastructureException {
		return hostService.getCommuneByAdresse(adresse, date);
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

	public boolean estDansLeCanton(Commune commune) throws ServiceInfrastructureException {
		return hostService.estDansLeCanton(commune);
	}

	public boolean estDansLeCanton(Rue rue) throws ServiceInfrastructureException {
		return hostService.estDansLeCanton(rue);
	}

	public boolean estDansLeCanton(AdresseGenerique adresse) throws ServiceInfrastructureException {
		return hostService.estDansLeCanton(adresse);
	}

	public boolean estDansLeCanton(Adresse adresse) throws ServiceInfrastructureException {
		return hostService.estDansLeCanton(adresse);
	}

	public boolean estEnSuisse(AdresseGenerique adresse) throws ServiceInfrastructureException {
		return hostService.estEnSuisse(adresse);
	}

	public boolean estEnSuisse(Adresse adresse) throws ServiceInfrastructureException {
		return hostService.estEnSuisse(adresse);
	}


	public Zone getZone(AdresseGenerique adresse) throws ServiceInfrastructureException {
		return hostService.getZone(adresse);
	}

	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		return hostService.getCollectivitesAdministratives();
	}

	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
		return hostService.getCommuneByLocalite(localite);
	}

	public Pays getPaysInconnu() throws ServiceInfrastructureException {
		return hostService.getPays(8999);
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

	public TypeAffranchissement getTypeAffranchissement(int noOfsPays) {
		return hostService.getTypeAffranchissement(noOfsPays);
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

	public List<Logiciel> getLogicielsPour(LogicielMetier metier) {
		return fidorService.getLogicielsPour(metier);
	}
}
