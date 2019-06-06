package ch.vd.unireg.interfaces.service.mock;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.InfrastructureConnector;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.LogicielMetier;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockInfrastructureConnector;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureImpl;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.TiersDAO;

/**
 * Proxy du service host-infrastructure à enregistrer dans l'application context et permettant à chaque test unitaire de spécifier précisemment l'instance du service civil à utiliser.
 * <p>
 * Ce proxy est initialisé par défaut sur une instance de DefaultMockInfrastructureConnector.
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
		setUp(new DefaultMockInfrastructureConnector());
	}

	public void setUp(InfrastructureConnector target) {
		this.target = new ServiceInfrastructureImpl(target, tiersDAO);
	}

	public void setUp(ServiceInfrastructureService target) {
		this.target = target;
	}

	@Override
	public List<Canton> getAllCantons() throws InfrastructureException {
		return target.getAllCantons();
	}

	@Override
	public Canton getCanton(int cantonOFS) throws InfrastructureException {
		return target.getCanton(cantonOFS);
	}

	@Override
	public Canton getCantonBySigle(String sigle) throws InfrastructureException {
		return target.getCantonBySigle(sigle);
	}

	@Override
	public Canton getCantonByCommune(int noOfsCommune) throws InfrastructureException {
		return target.getCantonByCommune(noOfsCommune);
	}

	@Override
	public Commune getCommuneByNumeroOfs(int noCommune, RegDate date) throws InfrastructureException {
		return target.getCommuneByNumeroOfs(noCommune, date);
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws InfrastructureException {
		return target.getCommuneHistoByNumeroOfs(noOfsCommune);
	}

	@Override
	public List<Commune> getCommunesDeVaud() throws InfrastructureException {
		return target.getCommunesDeVaud();
	}

	@Override
	public List<Commune> getCommunesHorsCanton() throws InfrastructureException {
		return target.getCommunesHorsCanton();
	}

	@Override
	public List<Commune> getListeCommunesByOID(int oid) throws InfrastructureException {
		return target.getListeCommunesByOID(oid);
	}

	@Override
	public Commune getCommuneFaitiere(Commune commune, RegDate dateReference) throws InfrastructureException {
		return target.getCommuneFaitiere(commune, dateReference);
	}

	@Override
	public Localite getLocaliteByONRP(int onrp, RegDate dateReference) throws InfrastructureException {
		return target.getLocaliteByONRP(onrp, dateReference);
	}

	@Override
	public List<Localite> getLocaliteByCommune(int commune) throws InfrastructureException {
		return target.getLocaliteByCommune(commune);
	}

	@Override
	public List<Localite> getLocalites() throws InfrastructureException {
		return target.getLocalites();
	}

	@Override
	public List<Pays> getPays() throws InfrastructureException {
		return target.getPays();
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws InfrastructureException {
		return target.getPaysHisto(numeroOFS);
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws InfrastructureException {
		return target.getPays(numeroOFS, date);
	}

	@Override
	public Pays getPays(String codePays, @Nullable RegDate date) throws InfrastructureException {
		return target.getPays(codePays, date);
	}

	@Override
	public Rue getRueByNumero(int numero) throws InfrastructureException {
		return target.getRueByNumero(numero);
	}

	@Override
	public List<Rue> getRues(Localite localite) throws InfrastructureException {
		return target.getRues(localite);
	}

	@Override
	public List<Rue> getRues(Collection<Localite> localites) throws InfrastructureException {
		return target.getRues(localites);
	}

	@Override
	public Pays getSuisse() throws InfrastructureException {
		return target.getSuisse();
	}

	@Override
	public Canton getVaud() throws InfrastructureException {
		return target.getVaud();
	}

	@Override
	public CollectiviteAdministrative getACI() throws InfrastructureException {
		return target.getACI();
	}

	@Override
	public CollectiviteAdministrative getACIImpotSource() throws InfrastructureException {
		return target.getACIImpotSource();
	}

	@Override
	public CollectiviteAdministrative getACIOIPM() throws InfrastructureException {
		return target.getACIOIPM();
	}

	@Override
	public CollectiviteAdministrative getCEDI() throws InfrastructureException {
		return target.getCEDI();
	}

	@Override
	public CollectiviteAdministrative getCAT() throws InfrastructureException {
		return target.getCAT();
	}

	@Override
	public CollectiviteAdministrative getRC() throws InfrastructureException {
		return target.getRC();
	}

	@Override
	public List<CollectiviteAdministrative> findCollectivitesAdministratives(@NotNull Collection<Integer> codeCollectivites, boolean inactif) {
		return target.findCollectivitesAdministratives(codeCollectivites, inactif);
	}

	@Override
	public Commune getCommuneByAdresse(Adresse adresse, RegDate date) throws InfrastructureException {
		return target.getCommuneByAdresse(adresse, date);
	}

	@Override
	public Commune getCommuneByAdresse(AdresseGenerique adresse, RegDate date) throws InfrastructureException {
		return target.getCommuneByAdresse(adresse, date);
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws InfrastructureException {
		return target.getNoOfsCommuneByEgid(egid, date);
	}

	@Override
	public Commune getCommuneByEgid(int egid, RegDate date) throws InfrastructureException {
		return target.getCommuneByEgid(egid, date);
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException {
		return target.getCollectivite(noColAdm);
	}

	@Override
	public OfficeImpot getOfficeImpot(int noColAdm) throws InfrastructureException {
		return target.getOfficeImpot(noColAdm);
	}

	@Override
	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws InfrastructureException {
		return target.getOfficeImpotDeCommune(noCommune);
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException {
		return target.getOfficesImpot();
	}

	@Override
	public boolean estDansLeCanton(Commune commune) throws InfrastructureException {
		return target.estDansLeCanton(commune);
	}

	@Override
	public boolean estDansLeCanton(Rue rue) throws InfrastructureException {
		return target.estDansLeCanton(rue);
	}

	@Override
	public boolean estDansLeCanton(AdresseGenerique adresse) throws InfrastructureException {
		return target.estDansLeCanton(adresse);
	}

	@Override
	public boolean estDansLeCanton(Adresse adresse) throws InfrastructureException {
		return target.estDansLeCanton(adresse);
	}

	@Override
	public boolean estEnSuisse(AdresseGenerique adresse) throws InfrastructureException {
		return target.estEnSuisse(adresse);
	}

	@Override
	public boolean estEnSuisse(Adresse adresse) throws InfrastructureException {
		return target.estEnSuisse(adresse);
	}


	@Override
	public Zone getZone(AdresseGenerique adresse) throws InfrastructureException {
		return target.getZone(adresse);
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {
		return target.getCollectivitesAdministratives();
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException {
		return target.getCommuneByLocalite(localite);
	}

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws InfrastructureException {
		return target.findCommuneByNomOfficiel(nomOfficiel, includeFaitieres, includeFractions, date);
	}

	@Override
	public Pays getPaysInconnu() throws InfrastructureException {
		return target.getPays(noPaysInconnu, null);
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite)
			throws InfrastructureException {
		return target.getCollectivitesAdministratives();
	}

	@Override
	public List<TypeRegimeFiscal> getRegimesFiscaux() throws InfrastructureException {
		return target.getRegimesFiscaux();
	}

	@Override
	@Nullable
	public TypeRegimeFiscal getRegimeFiscal(@NotNull String code) throws InfrastructureException {
		return target.getRegimeFiscal(code);
	}

	@Override
	public List<GenreImpotMandataire> getGenresImpotMandataires() throws InfrastructureException {
		return target.getGenresImpotMandataires();
	}

	@Override
	public List<Commune> getCommunesVD() throws InfrastructureException {
		return target.getCommunesVD();
	}

	@Override
	public List<Commune> getListeCommunesFaitieres() throws InfrastructureException {
		return target.getListeCommunesFaitieres();
	}

	@Override
	public List<Commune> getCommunes() throws InfrastructureException {
		return target.getCommunes();
	}

	@Override
	public List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws InfrastructureException {
		return target.getLocalitesByNPA(npa, dateReference);
	}

	@Override
	public String getUrlInteroperabilite(ApplicationFiscale application, Long tiersId) {
		return target.getUrlInteroperabilite(application, tiersId);
	}

	@Override
	public String getUrlVisualisationDocument(Long tiersId, @Nullable Integer pf, String cleDocument) {
		return target.getUrlVisualisationDocument(tiersId, pf, cleDocument);
	}

	@Override
	public String getUrlBrutte(ApplicationFiscale application) {
		return target.getUrlBrutte(application);
	}

	@Override
	public Logiciel getLogiciel(Long idLogiciel) throws InfrastructureException {
		return target.getLogiciel(idLogiciel);
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() throws InfrastructureException {
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
