package ch.vd.uniregctb.interfaces.service.mock;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.LogicielMetier;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.TiersDAO;

/**
 * Proxy du service host-infrastructure à enregistrer dans l'application context et permettant à chaque test unitaire de spécifier
 * précisemment l'instance du service civil à utiliser.
 * <p>
 * Ce proxy est initialisé par défaut sur une instance de DefaultMockServiceInfrastructureService.
 */
public class ProxyServiceInfrastructureService implements ServiceInfrastructureService, InitializingBean {

	private ServiceInfrastructureService target = null;
	private TiersDAO tiersDAO;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		setUpDefault();
	}

	public void setUpDefault() {
		setUp(new DefaultMockServiceInfrastructureService());
	}

	public void setUp(ServiceInfrastructureRaw target) {
		this.target = new ServiceInfrastructureImpl(target, tiersDAO);
	}

	public void setUp(ServiceInfrastructureService target) {
		this.target = target;
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
	public Commune getCommuneByNumeroOfs(int noCommune, RegDate date) throws ServiceInfrastructureException {
		return target.getCommuneByNumeroOfs(noCommune, date);
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
	public List<Commune> getListeCommunesByOID(int oid) throws ServiceInfrastructureException {
		return target.getListeCommunesByOID(oid);
	}

	@Override
	public Commune getCommuneFaitiere(Commune commune, RegDate dateReference) throws ServiceInfrastructureException {
		return target.getCommuneFaitiere(commune, dateReference);
	}

	@Override
	public Localite getLocaliteByONRP(int onrp, RegDate dateReference) throws ServiceInfrastructureException {
		return target.getLocaliteByONRP(onrp, dateReference);
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
	public List<Pays> getPaysHisto(int numeroOFS) throws ServiceInfrastructureException {
		return target.getPaysHisto(numeroOFS);
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws ServiceInfrastructureException {
		return target.getPays(numeroOFS, date);
	}

	@Override
	public Pays getPays(String codePays, @Nullable RegDate date) throws ServiceInfrastructureException {
		return target.getPays(codePays, date);
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
	public CollectiviteAdministrative getACIOIPM() throws ServiceInfrastructureException {
		return target.getACIOIPM();
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
	public CollectiviteAdministrative getRC() throws ServiceInfrastructureException {
		return target.getRC();
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

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws ServiceInfrastructureException {
		return target.findCommuneByNomOfficiel(nomOfficiel, includeFaitieres, includeFractions, date);
	}

	@Override
	public Pays getPaysInconnu() throws ServiceInfrastructureException {
		return target.getPays(noPaysInconnu, null);
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite)
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
	public List<TypeRegimeFiscal> getRegimesFiscaux() throws ServiceInfrastructureException {
		return target.getRegimesFiscaux();
	}

	@Override
	public List<GenreImpotMandataire> getGenresImpotMandataires() throws ServiceInfrastructureException {
		return target.getGenresImpotMandataires();
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
	public List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws ServiceInfrastructureException {
		return target.getLocalitesByNPA(npa, dateReference);
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

	@Override
	public List<Logiciel> getLogicielsCertifiesPour(LogicielMetier metier) {
		return target.getLogicielsCertifiesPour(metier);
	}
}
