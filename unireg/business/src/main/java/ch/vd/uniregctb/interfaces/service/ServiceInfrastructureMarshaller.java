package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.ApplicationFiscale;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.CommuneId;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
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

	public List<Canton> getAllCantons() throws InfrastructureException {
		return hostService.getAllCantons();
	}

	public Canton getCanton(int cantonOFS) throws InfrastructureException {
		return hostService.getCanton(cantonOFS);
	}

	public Canton getCantonBySigle(String sigle) throws InfrastructureException {
		return hostService.getCantonBySigle(sigle);
	}

	public Canton getCantonByCommune(int noOfsCommune) throws InfrastructureException {
		return hostService.getCantonByCommune(noOfsCommune);
	}

	public Commune getCommuneByNumeroOfsEtendu(int noCommune, RegDate date) throws InfrastructureException {
		return hostService.getCommuneByNumeroOfsEtendu(noCommune, date);
	}

	@Override
	public CommuneId getCommuneIdByEgid(int egid, RegDate date, Integer hintNoOfsCommune) throws InfrastructureException {
		return fidorService.getCommuneIdByEgid(egid, date, hintNoOfsCommune);
	}

	@Override
	public CommuneSimple getCommuneById(CommuneId id) throws InfrastructureException {
		return hostService.getCommuneById(id);
	}

	@Override
	public CommuneSimple getCommuneByEgid(int egid, RegDate date, Integer hintNoOfsCommune) throws InfrastructureException {
		return fidorService.getCommuneByEgid(egid, date, hintNoOfsCommune);
	}

	public Commune getCommuneVaudByNumACI(Integer numeroACI) throws InfrastructureException {
		return hostService.getCommuneVaudByNumACI(numeroACI);
	}

	public List<Commune> getCommunesDeVaud() throws InfrastructureException {
		return hostService.getCommunesDeVaud();
	}

	public List<Commune> getCommunesHorsCanton() throws InfrastructureException {
		return hostService.getCommunesHorsCanton();
	}

	public List<Commune> getListeCommunes(Canton canton) throws InfrastructureException {
		return hostService.getListeCommunes(canton);
	}

	public List<Commune> getListeCommunes(int cantonOFS) throws InfrastructureException {
		return hostService.getListeCommunes(cantonOFS);
	}

	public List<Commune> getListeCommunesByOID(int oid) throws InfrastructureException {
		return hostService.getListeCommunesByOID(oid);
	}

	public CommuneSimple getCommuneFaitiere(CommuneSimple commune, RegDate dateReference) throws InfrastructureException {
		return hostService.getCommuneFaitiere(commune, dateReference);
	}

	public Localite getLocaliteByONRP(int onrp) throws InfrastructureException {
		return hostService.getLocaliteByONRP(onrp);
	}

	public List<Localite> getLocaliteByCommune(int commune) throws InfrastructureException {
		return hostService.getLocaliteByCommune(commune);
	}

	public List<Localite> getLocalites() throws InfrastructureException {
		return hostService.getLocalites();
	}

	public List<Pays> getPays() throws InfrastructureException {
		return fidorService.getPays();
	}

	public Pays getPays(int numeroOFS) throws InfrastructureException {
		return fidorService.getPays(numeroOFS);
	}

	public Pays getPays(String codePays) throws InfrastructureException {
		return fidorService.getPays(codePays);
	}

	public Rue getRueByNumero(int numero) throws InfrastructureException {
		return hostService.getRueByNumero(numero);
	}

	public List<Rue> getRues(Localite localite) throws InfrastructureException {
		return hostService.getRues(localite);
	}

	public List<Rue> getRues(Collection<Localite> localites) throws InfrastructureException {
		return hostService.getRues(localites);
	}

	public List<Rue> getRues(Canton canton) throws InfrastructureException {
		return hostService.getRues(canton);
	}

	public Pays getSuisse() throws ServiceInfrastructureException {
		return hostService.getSuisse();
	}

	public Canton getVaud() throws InfrastructureException {
		return hostService.getVaud();
	}

	public CollectiviteAdministrative getACI() throws InfrastructureException {
		return hostService.getACI();
	}

	public CollectiviteAdministrative getACIImpotSource() throws InfrastructureException {
		return hostService.getACIImpotSource();
	}

	public CollectiviteAdministrative getACISuccessions() throws InfrastructureException {
		return hostService.getACISuccessions();
	}

	public CollectiviteAdministrative getCEDI() throws InfrastructureException {
		return hostService.getCEDI();
	}

	public CollectiviteAdministrative getCAT() throws InfrastructureException {
		return hostService.getCAT();
	}

	public CommuneSimple getCommuneByAdresse(Adresse adresse) throws InfrastructureException {
		return hostService.getCommuneByAdresse(adresse);
	}

	public CommuneSimple getCommuneByAdresse(AdresseGenerique adresse) throws InfrastructureException {
		return hostService.getCommuneByAdresse(adresse);
	}

	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException {
		return hostService.getCollectivite(noColAdm);
	}

	public OfficeImpot getOfficeImpot(int noColAdm) throws InfrastructureException {
		return hostService.getOfficeImpot(noColAdm);
	}

	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws InfrastructureException {
		return hostService.getOfficeImpotDeCommune(noCommune);
	}

	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException {
		return hostService.getOfficesImpot();
	}

	public boolean estDansLeCanton(CommuneSimple commune) throws InfrastructureException {
		return hostService.estDansLeCanton(commune);
	}

	public boolean estDansLeCanton(Commune commune) throws InfrastructureException {
		return hostService.estDansLeCanton(commune);
	}

	public boolean estDansLeCanton(Rue rue) throws InfrastructureException {
		return hostService.estDansLeCanton(rue);
	}

	public boolean estDansLeCanton(AdresseGenerique adresse) throws InfrastructureException {
		return hostService.estDansLeCanton(adresse);
	}

	public boolean estDansLeCanton(Adresse adresse) throws InfrastructureException {
		return hostService.estDansLeCanton(adresse);
	}

	public boolean estEnSuisse(AdresseGenerique adresse) throws InfrastructureException {
		return hostService.estEnSuisse(adresse);
	}

	public boolean estEnSuisse(Adresse adresse) throws InfrastructureException {
		return hostService.estEnSuisse(adresse);
	}


	public Zone getZone(AdresseGenerique adresse) throws InfrastructureException {
		return hostService.getZone(adresse);
	}

	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {
		return hostService.getCollectivitesAdministratives();
	}

	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException {
		return hostService.getCommuneByLocalite(localite);
	}

	public Pays getPaysInconnu() throws InfrastructureException {
		return hostService.getPays(8999);
	}

	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite)
			throws InfrastructureException {
		return hostService.getCollectivitesAdministratives();
	}

	public InstitutionFinanciere getInstitutionFinanciere(int id) throws InfrastructureException {
		return hostService.getInstitutionFinanciere(id);
	}

	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws InfrastructureException {
		return hostService.getInstitutionsFinancieres(noClearing);
	}

	public List<Commune> getListeFractionsCommunes() throws InfrastructureException {
		return hostService.getListeFractionsCommunes();
	}

	public List<Commune> getCommunes() throws InfrastructureException {
		return hostService.getCommunes();
	}

	public Localite getLocaliteByNPA(int npa) throws InfrastructureException {
		return hostService.getLocaliteByNPA(npa);
	}

	public TypeAffranchissement getTypeAffranchissement(int noOfsPays) {
		return hostService.getTypeAffranchissement(noOfsPays);
	}

	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws InfrastructureException {
		return hostService.getTypesRegimesFiscaux();
	}

	public TypeRegimeFiscal getTypeRegimeFiscal(String code) throws InfrastructureException {
		return hostService.getTypeRegimeFiscal(code);
	}

	public List<TypeEtatPM> getTypesEtatsPM() throws InfrastructureException {
		return hostService.getTypesEtatsPM();
	}

	public TypeEtatPM getTypeEtatPM(String code) throws InfrastructureException {
		return hostService.getTypeEtatPM(code);
	}

	public String getUrlVers(ApplicationFiscale application, Long tiersId) {
		return fidorService.getUrlVers(application, tiersId);
	}

	public Logiciel getLogiciel(Long idLogiciel) {
		return fidorService.getLogiciel(idLogiciel);
	}

	public List<Logiciel> getTousLesLogiciels() {
		return fidorService.getTousLesLogiciels();
	}

	public List<Logiciel> getLogicielsPour(LogicielMetier metier) {
		return fidorService.getLogicielsPour(metier);
	}
}
