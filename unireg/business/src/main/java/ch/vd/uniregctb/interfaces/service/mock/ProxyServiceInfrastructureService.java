package ch.vd.uniregctb.interfaces.service.mock;

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
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureRaw;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Proxy du service host-infrastructure à enregistrer dans l'application context et permettant à chaque test unitaire de spécifier
 * précisemment l'instance du service civil à utiliser.
 * <p>
 * Ce proxy est initialisé par défaut sur une instance de DefaultMockServiceInfrastructureService.
 */
public class ProxyServiceInfrastructureService implements ServiceInfrastructureService {

	private ServiceInfrastructureService target = null;

	public ProxyServiceInfrastructureService() {
		setUpDefault();
	}

	public void setUpDefault() {
		setUp(new DefaultMockServiceInfrastructureService());
	}

	public void setUp(ServiceInfrastructureRaw target) {
		this.target = new ServiceInfrastructureImpl(target);
	}

	public void setUp(ServiceInfrastructureService target) {
		this.target = target;
	}

	public void tearDown() {
		this.target = null;
	}

	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		return target.getAllCantons();
	}

	@Override
	public Canton getCanton(int cantonOFS) throws ServiceInfrastructureException {
		return target.getCanton(cantonOFS);
	}

	@Override
	public Canton getCantonBySigle(String sigle) throws ServiceInfrastructureException {
		return target.getCantonBySigle(sigle);
	}

	@Override
	public Canton getCantonByCommune(int noOfsCommune) throws ServiceInfrastructureException {
		return target.getCantonByCommune(noOfsCommune);
	}

	@Override
	public Commune getCommuneByNumeroOfsEtendu(int noCommune, RegDate date) throws ServiceInfrastructureException {
		return target.getCommuneByNumeroOfsEtendu(noCommune, date);
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		return target.getCommuneHistoByNumeroOfs(noOfsCommune);
	}

	@Override
	public List<Commune> getCommunesDeVaud() throws ServiceInfrastructureException {
		return target.getCommunesDeVaud();
	}

	@Override
	public List<Commune> getCommunesHorsCanton() throws ServiceInfrastructureException {
		return target.getCommunesHorsCanton();
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
		return target.getListeCommunes(canton);
	}

	@Override
	public List<Commune> getListeCommunes(int cantonOFS) throws ServiceInfrastructureException {
		return target.getListeCommunes(cantonOFS);
	}

	@Override
	public List<Commune> getListeCommunesByOID(int oid) throws ServiceInfrastructureException {
		return target.getListeCommunesByOID(oid);
	}

	@Override
	public Commune getCommuneFaitiere(Commune commune, RegDate dateReference) throws ServiceInfrastructureException {
		return target.getCommuneFaitiere(commune, dateReference);
	}

	@Override
	public Localite getLocaliteByONRP(int onrp) throws ServiceInfrastructureException {
		return target.getLocaliteByONRP(onrp);
	}

	@Override
	public List<Localite> getLocaliteByCommune(int commune) throws ServiceInfrastructureException {
		return target.getLocaliteByCommune(commune);
	}

	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		return target.getLocalites();
	}

	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		return target.getPays();
	}

	@Override
	public Pays getPays(int numeroOFS) throws ServiceInfrastructureException {
		return target.getPays(numeroOFS);
	}

	@Override
	public Pays getPays(String codePays) throws ServiceInfrastructureException {
		return target.getPays(codePays);
	}

	@Override
	public Rue getRueByNumero(int numero) throws ServiceInfrastructureException {
		return target.getRueByNumero(numero);
	}

	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		return target.getRues(localite);
	}

	@Override
	public List<Rue> getRues(Collection<Localite> localites) throws ServiceInfrastructureException {
		return target.getRues(localites);
	}

	@Override
	public List<Rue> getRues(Canton canton) throws ServiceInfrastructureException {
		return target.getRues(canton);
	}

	@Override
	public Pays getSuisse() throws ServiceInfrastructureException {
		return target.getSuisse();
	}

	@Override
	public Canton getVaud() throws ServiceInfrastructureException {
		return target.getVaud();
	}

	@Override
	public CollectiviteAdministrative getACI() throws ServiceInfrastructureException {
		return target.getACI();
	}

	@Override
	public CollectiviteAdministrative getACIImpotSource() throws ServiceInfrastructureException {
		return target.getACIImpotSource();
	}

	@Override
	public CollectiviteAdministrative getACISuccessions() throws ServiceInfrastructureException {
		return target.getACISuccessions();
	}

	@Override
	public CollectiviteAdministrative getCEDI() throws ServiceInfrastructureException {
		return target.getCEDI();
	}

	@Override
	public CollectiviteAdministrative getCAT() throws ServiceInfrastructureException {
		return target.getCAT();
	}

	@Override
	public Commune getCommuneByAdresse(Adresse adresse, RegDate date) throws ServiceInfrastructureException {
		return target.getCommuneByAdresse(adresse, date);
	}

	@Override
	public Commune getCommuneByAdresse(AdresseGenerique adresse, RegDate date) throws ServiceInfrastructureException {
		return target.getCommuneByAdresse(adresse, date);
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {
		return target.getNoOfsCommuneByEgid(egid, date);
	}

	@Override
	public Commune getCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {
		return target.getCommuneByEgid(egid, date);
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		return target.getCollectivite(noColAdm);
	}

	@Override
	public OfficeImpot getOfficeImpot(int noColAdm) throws ServiceInfrastructureException {
		return target.getOfficeImpot(noColAdm);
	}

	@Override
	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws ServiceInfrastructureException {
		return target.getOfficeImpotDeCommune(noCommune);
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		return target.getOfficesImpot();
	}

	@Override
	public boolean estDansLeCanton(Commune commune) throws ServiceInfrastructureException {
		return target.estDansLeCanton(commune);
	}

	@Override
	public boolean estDansLeCanton(Rue rue) throws ServiceInfrastructureException {
		return target.estDansLeCanton(rue);
	}

	@Override
	public boolean estDansLeCanton(AdresseGenerique adresse) throws ServiceInfrastructureException {
		return target.estDansLeCanton(adresse);
	}

	@Override
	public boolean estDansLeCanton(Adresse adresse) throws ServiceInfrastructureException {
		return target.estDansLeCanton(adresse);
	}

	@Override
	public boolean estEnSuisse(AdresseGenerique adresse) throws ServiceInfrastructureException {
		return target.estEnSuisse(adresse);
	}

	@Override
	public boolean estEnSuisse(Adresse adresse) throws ServiceInfrastructureException {
		return target.estEnSuisse(adresse);
	}


	@Override
	public Zone getZone(AdresseGenerique adresse) throws ServiceInfrastructureException {
		return target.getZone(adresse);
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		return target.getCollectivitesAdministratives();
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
		return target.getCommuneByLocalite(localite);
	}

	@Override
	public Pays getPaysInconnu() throws ServiceInfrastructureException {
		return target.getPays(8999);
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite)
			throws ServiceInfrastructureException {
		return target.getCollectivitesAdministratives();
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {
		return target.getInstitutionFinanciere(id);
	}

	@Override
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
		return target.getInstitutionsFinancieres(noClearing);
	}

	@Override
	public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
		return target.getListeFractionsCommunes();
	}

	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		return target.getCommunes();
	}

	@Override
	public Localite getLocaliteByNPA(int npa) throws ServiceInfrastructureException {
		return target.getLocaliteByNPA(npa);
	}

	@Override
	public TypeAffranchissement getTypeAffranchissement(int noOfsPays) {
		return target.getTypeAffranchissement(noOfsPays);
	}

	@Override
	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws ServiceInfrastructureException {
		return target.getTypesRegimesFiscaux();
	}

	@Override
	public TypeRegimeFiscal getTypeRegimeFiscal(String code) throws ServiceInfrastructureException {
		return target.getTypeRegimeFiscal(code);
	}

	@Override
	public List<TypeEtatPM> getTypesEtatsPM() throws ServiceInfrastructureException {
		return target.getTypesEtatsPM();
	}

	@Override
	public TypeEtatPM getTypeEtatPM(String code) throws ServiceInfrastructureException {
		return target.getTypeEtatPM(code);
	}

	@Override
	public String getUrlVers(ApplicationFiscale application, Long tiersId) {
		return target.getUrlVers(application, tiersId);
	}

	@Override
	public Logiciel getLogiciel(Long idLogiciel) throws ServiceInfrastructureException {
		return target.getLogiciel(idLogiciel);
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() throws ServiceInfrastructureException {
		return target.getTousLesLogiciels();
	}

	@Override
	public List<Logiciel> getLogicielsPour(LogicielMetier metier) {
		return target.getLogicielsPour(metier);
	}
}
