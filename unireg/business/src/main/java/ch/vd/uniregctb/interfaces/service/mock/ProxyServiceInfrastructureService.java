package ch.vd.uniregctb.interfaces.service.mock;

import java.util.Collection;
import java.util.List;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.TypeAffranchissement;
import ch.vd.uniregctb.interfaces.model.TypeEtatPM;
import ch.vd.uniregctb.interfaces.model.TypeRegimeFiscal;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;
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

	public void setUp(ServiceInfrastructureService target) {
		this.target = target;
	}

	public void tearDown() {
		this.target = null;
	}

	public List<Canton> getAllCantons() throws InfrastructureException {
		return target.getAllCantons();
	}

	public Canton getCanton(int cantonOFS) throws InfrastructureException {
		return target.getCanton(cantonOFS);
	}

	public Canton getCantonBySigle(String sigle) throws InfrastructureException {
		return target.getCantonBySigle(sigle);
	}

	public Canton getCantonByCommune(int noOfsCommune) throws InfrastructureException {
		return target.getCantonByCommune(noOfsCommune);
	}

	public Commune getCommuneByNumeroOfsEtendu(int noCommune, RegDate date) throws InfrastructureException {
		return target.getCommuneByNumeroOfsEtendu(noCommune, date);
	}

	public Commune getCommuneVaudByNumACI(Integer numeroACI) throws InfrastructureException {
		return target.getCommuneVaudByNumACI(numeroACI);
	}

	public List<Commune> getCommunesDeVaud() throws InfrastructureException {
		return target.getCommunesDeVaud();
	}

	public List<Commune> getCommunesHorsCanton() throws InfrastructureException {
		return target.getCommunesHorsCanton();
	}

	public List<Commune> getListeCommunes(Canton canton) throws InfrastructureException {
		return target.getListeCommunes(canton);
	}

	public List<Commune> getListeCommunes(int cantonOFS) throws InfrastructureException {
		return target.getListeCommunes(cantonOFS);
	}

	public List<Commune> getListeCommunesByOID(int oid) throws InfrastructureException {
		return target.getListeCommunesByOID(oid);
	}

	public CommuneSimple getCommuneFaitiere(CommuneSimple commune, RegDate dateReference) throws InfrastructureException {
		return target.getCommuneFaitiere(commune, dateReference);
	}

	public Localite getLocaliteByONRP(int onrp) throws InfrastructureException {
		return target.getLocaliteByONRP(onrp);
	}

	public List<Localite> getLocaliteByCommune(int commune) throws InfrastructureException {
		return target.getLocaliteByCommune(commune);
	}

	public List<Localite> getLocalites() throws InfrastructureException {
		return target.getLocalites();
	}

	public List<Pays> getPays() throws InfrastructureException {
		return target.getPays();
	}

	public Pays getPays(int numeroOFS) throws InfrastructureException {
		return target.getPays(numeroOFS);
	}

	public Pays getPays(String codePays) throws InfrastructureException {
		return target.getPays(codePays);
	}

	public Rue getRueByNumero(int numero) throws InfrastructureException {
		return target.getRueByNumero(numero);
	}

	public List<Rue> getRues(Localite localite) throws InfrastructureException {
		return target.getRues(localite);
	}

	public List<Rue> getRues(Collection<Localite> localites) throws InfrastructureException {
		return target.getRues(localites);
	}

	public List<Rue> getRues(Canton canton) throws InfrastructureException {
		return target.getRues(canton);
	}

	public Pays getSuisse() throws ServiceInfrastructureException {
		return target.getSuisse();
	}

	public Canton getVaud() throws InfrastructureException {
		return target.getVaud();
	}

	public CollectiviteAdministrative getACI() throws InfrastructureException {
		return target.getACI();
	}

	public CollectiviteAdministrative getACIImpotSource() throws InfrastructureException {
		return target.getACIImpotSource();
	}

	public CollectiviteAdministrative getACISuccessions() throws InfrastructureException {
		return target.getACISuccessions();
	}

	public CollectiviteAdministrative getCEDI() throws InfrastructureException {
		return target.getCEDI();
	}

	public CollectiviteAdministrative getCAT() throws InfrastructureException {
		return target.getCAT();
	}

	public CommuneSimple getCommuneByAdresse(Adresse adresse) throws InfrastructureException {
		return target.getCommuneByAdresse(adresse);
	}

	public CommuneSimple getCommuneByAdresse(AdresseGenerique adresse) throws InfrastructureException {
		return target.getCommuneByAdresse(adresse);
	}

	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException {
		return target.getCollectivite(noColAdm);
	}

	public OfficeImpot getOfficeImpot(int noColAdm) throws InfrastructureException {
		return target.getOfficeImpot(noColAdm);
	}

	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws InfrastructureException {
		return target.getOfficeImpotDeCommune(noCommune);
	}

	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException {
		return target.getOfficesImpot();
	}

	public boolean estDansLeCanton(CommuneSimple commune) throws InfrastructureException {
		return target.estDansLeCanton(commune);
	}

	public boolean estDansLeCanton(Commune commune) throws InfrastructureException {
		return target.estDansLeCanton(commune);
	}

	public boolean estDansLeCanton(Rue rue) throws InfrastructureException {
		return target.estDansLeCanton(rue);
	}

	public boolean estDansLeCanton(AdresseGenerique adresse) throws InfrastructureException {
		return target.estDansLeCanton(adresse);
	}

	public boolean estDansLeCanton(Adresse adresse) throws InfrastructureException {
		return target.estDansLeCanton(adresse);
	}

	public boolean estEnSuisse(AdresseGenerique adresse) throws InfrastructureException {
		return target.estEnSuisse(adresse);
	}

	public boolean estEnSuisse(Adresse adresse) throws InfrastructureException {
		return target.estEnSuisse(adresse);
	}


	public Zone getZone(AdresseGenerique adresse) throws InfrastructureException {
		return target.getZone(adresse);
	}

	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {
		return target.getCollectivitesAdministratives();
	}

	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException {
		return target.getCommuneByLocalite(localite);
	}

	public Pays getPaysInconnu() throws InfrastructureException {
		return target.getPays(8999);
	}

	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite)
			throws InfrastructureException {
		return target.getCollectivitesAdministratives();
	}

	public InstitutionFinanciere getInstitutionFinanciere(int id) throws InfrastructureException {
		return target.getInstitutionFinanciere(id);
	}

	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws InfrastructureException {
		return target.getInstitutionsFinancieres(noClearing);
	}

	public List<Commune> getListeFractionsCommunes() throws InfrastructureException {
		return target.getListeFractionsCommunes();
	}

	public List<Commune> getCommunes() throws InfrastructureException {
		return target.getCommunes();
	}

	public Localite getLocaliteByNPA(int npa) throws InfrastructureException {
		return target.getLocaliteByNPA(npa);
	}

	public TypeAffranchissement getTypeAffranchissement(int noOfsPays) {
		return target.getTypeAffranchissement(noOfsPays);
	}

	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws InfrastructureException {
		return target.getTypesRegimesFiscaux();
	}

	public TypeRegimeFiscal getTypeRegimeFiscal(String code) throws InfrastructureException {
		return target.getTypeRegimeFiscal(code);
	}

	public List<TypeEtatPM> getTypesEtatsPM() throws InfrastructureException {
		return target.getTypesEtatsPM();
	}

	public TypeEtatPM getTypeEtatPM(String code) throws InfrastructureException {
		return target.getTypeEtatPM(code);
	}
}
