package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.TypeAffranchissement;
import ch.vd.uniregctb.stats.StatsService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.service.InfrastructureException;
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

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServiceInfrastructureTracing implements ServiceInfrastructureService, InitializingBean, DisposableBean {

	private ServiceInfrastructureService target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(ServiceInfrastructureService target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public boolean estDansLeCanton(Rue rue) throws InfrastructureException {
		boolean result;
		long time = tracing.start();
		try {
			result = target.estDansLeCanton(rue);
		}
		finally {
			tracing.end(time, "estDansLeCanton(rue)");
		}
		return result;
	}

	public boolean estDansLeCanton(CommuneSimple commune) throws InfrastructureException {
		boolean result;
		long time = tracing.start();
		try {
			result = target.estDansLeCanton(commune);
		}
		finally {
			tracing.end(time, "estDansLeCanton(communeSimple)");
		}

		return result;
	}

	public boolean estDansLeCanton(Commune commune) throws InfrastructureException {
		boolean result;
		long time = tracing.start();
		try {
			result = target.estDansLeCanton(commune);
		}
		finally {
			tracing.end(time, "estDansLeCanton(commune)");
		}

		return result;
	}

	public boolean estDansLeCanton(AdresseGenerique adresse) throws InfrastructureException {
		boolean result;
		long time = tracing.start();
		try {
			result = target.estDansLeCanton(adresse);
		}
		finally {
			tracing.end(time, "estDansLeCanton(adresseGenerique)");
		}

		return result;
	}

	public boolean estDansLeCanton(Adresse adresse) throws InfrastructureException {
		boolean result;
		long time = tracing.start();
		try {
			result = target.estDansLeCanton(adresse);
		}
		finally {
			tracing.end(time, "estDansLeCanton(adresse)");
		}

		return result;
	}

	public boolean estEnSuisse(AdresseGenerique adresse) throws InfrastructureException {
		boolean result;
		long time = tracing.start();
		try {
			result = target.estEnSuisse(adresse);
		}
		finally {
			tracing.end(time, "estEnSuisse(adresseGenerique)");
		}

		return result;
	}

	public boolean estEnSuisse(Adresse adresse) throws InfrastructureException {
		boolean result;
		long time = tracing.start();
		try {
			result = target.estEnSuisse(adresse);
		}
		finally {
			tracing.end(time, "estEnSuisse(adresse)");
		}

		return result;
	}

	public CollectiviteAdministrative getACI() throws InfrastructureException {
		CollectiviteAdministrative result;
		long time = tracing.start();
		try {
			result = target.getACI();
		}
		finally {
			tracing.end(time, "getACI");
		}

		return result;
	}

		public CollectiviteAdministrative getACISuccessions() throws InfrastructureException {
		CollectiviteAdministrative result;
		long time = tracing.start();
		try {
			result = target.getACISuccessions();
		}
		finally {
			tracing.end(time, "getACISuccessions");
		}

		return result;
	}

	public CollectiviteAdministrative getCEDI() throws InfrastructureException {
		CollectiviteAdministrative result;
		long time = tracing.start();
		try {
			result = target.getCEDI();
		}
		finally {
			tracing.end(time, "getCEDI");
		}

		return result;
	}

	public CollectiviteAdministrative getCAT() throws InfrastructureException {
		CollectiviteAdministrative result;
		long time = tracing.start();
		try {
			result = target.getCAT();
		}
		finally {
			tracing.end(time, "getCAT");
		}

		return result;
	}

	public List<Canton> getAllCantons() throws InfrastructureException {
		List<Canton> result;
		long time = tracing.start();
		try {
			result = target.getAllCantons();
		}
		finally {
			tracing.end(time, "getAllCantons");
		}

		return result;
	}

	public Canton getCanton(int cantonOFS) throws InfrastructureException {
		Canton result;
		long time = tracing.start();
		try {
			result = target.getCanton(cantonOFS);
		}
		finally {
			tracing.end(time, "getCanton");
		}

		return result;
	}

	public Canton getCantonByCommune(int noOfsCommune) throws InfrastructureException {
		Canton result;
		long time = tracing.start();
		try {
			result = target.getCantonByCommune(noOfsCommune);
		}
		finally {
			tracing.end(time, "getCantonByCommune");
		}

		return result;
	}

	public Canton getCantonBySigle(String sigle) throws InfrastructureException {
		Canton result;
		long time = tracing.start();
		try {
			result = target.getCantonBySigle(sigle);
		}
		finally {
			tracing.end(time, "getCantonBySigle");
		}

		return result;
	}

	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException {
		CollectiviteAdministrative result;
		long time = tracing.start();
		try {
			result = target.getCollectivite(noColAdm);
		}
		finally {
			tracing.end(time, "getCollectivite");
		}

		return result;
	}

	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {
		List<CollectiviteAdministrative> result;
		long time = tracing.start();
		try {
			result = target.getCollectivitesAdministratives();
		}
		finally {
			tracing.end(time, "getCollectivitesAdministratives");
		}

		return result;
	}

	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite)
			throws InfrastructureException {
		List<CollectiviteAdministrative> result;
		long time = tracing.start();
		try {
			result = target.getCollectivitesAdministratives(typesCollectivite);
		}
		finally {
			tracing.end(time, "getCollectivitesAdministratives(types)");
		}

		return result;
	}

	public CommuneSimple getCommuneByAdresse(Adresse adresse) throws InfrastructureException {
		CommuneSimple result;
		long time = tracing.start();
		try {
			result = target.getCommuneByAdresse(adresse);
		}
		finally {
			tracing.end(time, "getCommuneByAdresse(adresse)");
		}

		return result;
	}

	public CommuneSimple getCommuneByAdresse(AdresseGenerique adresse) throws InfrastructureException {
		CommuneSimple result;
		long time = tracing.start();
		try {
			result = target.getCommuneByAdresse(adresse);
		}
		finally {
			tracing.end(time, "getCommuneByAdresse(adresseGenerique)");
		}

		return result;
	}

	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException {
		Commune result;
		long time = tracing.start();
		try {
			result = target.getCommuneByLocalite(localite);
		}
		finally {
			tracing.end(time, "getCommuneByLocalite");
		}

		return result;
	}

	public Commune getCommuneByNumeroOfsEtendu(int noCommune, RegDate date) throws InfrastructureException {
		Commune result;
		long time = tracing.start();
		try {
			result = target.getCommuneByNumeroOfsEtendu(noCommune, date);
		}
		finally {
			tracing.end(time, "getCommuneByNumeroOfsEtendu");
		}

		return result;
	}

	public Commune getCommuneVaudByNumACI(Integer numeroACI) throws InfrastructureException {
		Commune result;
		long time = tracing.start();
		try {
			result = target.getCommuneVaudByNumACI(numeroACI);
		}
		finally {
			tracing.end(time, "getCommuneVaudByNumACI");
		}

		return result;
	}

	public List<Commune> getCommunesDeVaud() throws InfrastructureException {
		List<Commune> result;
		long time = tracing.start();
		try {
			result = target.getCommunesDeVaud();
		}
		finally {
			tracing.end(time, "getCommunesDeVaud");
		}

		return result;
	}

	public List<Commune> getCommunesHorsCanton() throws InfrastructureException {
		List<Commune> result;
		long time = tracing.start();
		try {
			result = target.getCommunesHorsCanton();
		}
		finally {
			tracing.end(time, "getCommunesHorsCanton");
		}

		return result;
	}

	public List<Commune> getCommunes() throws InfrastructureException {
		List<Commune> result;
		long time = tracing.start();
		try {
			result = target.getCommunes();
		}
		finally {
			tracing.end(time, "getCommunes");
		}

		return result;
	}

	public List<Commune> getListeCommunes(Canton canton) throws InfrastructureException {
		List<Commune> result;
		long time = tracing.start();
		try {
			result = target.getListeCommunes(canton);
		}
		finally {
			tracing.end(time, "getListeCommunes(canton)");
		}

		return result;
	}

	public List<Commune> getListeFractionsCommunes() throws InfrastructureException {
		List<Commune> result;
		long time = tracing.start();
		try {
			result = target.getListeFractionsCommunes();
		}
		finally {
			tracing.end(time, "getListeFractionsCommunes");
		}

		return result;
	}

	public List<Commune> getListeCommunes(int cantonOFS) throws InfrastructureException {
		List<Commune> result;
		long time = tracing.start();
		try {
			result = target.getListeCommunes(cantonOFS);
		}
		finally {
			tracing.end(time, "getListeCommunes(int)");
		}

		return result;
	}

	public List<Commune> getListeCommunesByOID(int oid) throws InfrastructureException {
		List<Commune> result;
		long time = tracing.start();
		try {
			result = target.getListeCommunesByOID(oid);
		}
		finally {
			tracing.end(time, "getListeCommunesByOID");
		}

		return result;
	}

	public CommuneSimple getCommuneFaitiere(CommuneSimple commune, RegDate dateReference) throws InfrastructureException {
		CommuneSimple result;
		long time = tracing.start();
		try {
			result = target.getCommuneFaitiere(commune, dateReference);
		}
		finally {
			tracing.end(time, "getCommuneFaitiere");
		}

		return result;
	}

	public List<Localite> getLocaliteByCommune(int commune) throws InfrastructureException {
		List<Localite> result;
		long time = tracing.start();
		try {
			result = target.getLocaliteByCommune(commune);
		}
		finally {
			tracing.end(time, "getLocaliteByCommune");
		}

		return result;
	}

	public Localite getLocaliteByONRP(int onrp) throws InfrastructureException {
		Localite result;
		long time = tracing.start();
		try {
			result = target.getLocaliteByONRP(onrp);
		}
		finally {
			tracing.end(time, "getLocaliteByONRP");
		}

		return result;
	}

	public List<Localite> getLocalites() throws InfrastructureException {
		List<Localite> result;
		long time = tracing.start();
		try {
			result = target.getLocalites();
		}
		finally {
			tracing.end(time, "getLocalites");
		}

		return result;
	}

	public OfficeImpot getOfficeImpot(int noColAdm) throws InfrastructureException {
		OfficeImpot result;
		long time = tracing.start();
		try {
			result = target.getOfficeImpot(noColAdm);
		}
		finally {
			tracing.end(time, "getOfficeImpot");
		}

		return result;
	}

	public OfficeImpot getOfficeImpotDeCommune(int noCommune) throws InfrastructureException {
		OfficeImpot result;
		long time = tracing.start();
		try {
			result = target.getOfficeImpotDeCommune(noCommune);
		}
		finally {
			tracing.end(time, "getOfficeImpotDeCommune");
		}

		return result;
	}

	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException {
		List<OfficeImpot> result;
		long time = tracing.start();
		try {
			result = target.getOfficesImpot();
		}
		finally {
			tracing.end(time, "getOfficesImpot");
		}

		return result;
	}

	public List<Pays> getPays() throws InfrastructureException {
		List<Pays> result;
		long time = tracing.start();
		try {
			result = target.getPays();
		}
		finally {
			tracing.end(time, "getPays");
		}

		return result;
	}

	public Pays getPays(int numeroOFS) throws InfrastructureException {
		Pays result;
		long time = tracing.start();
		try {
			result = target.getPays(numeroOFS);
		}
		finally {
			tracing.end(time, "getPays(int)");
		}

		return result;
	}


	public Pays getPays(String codePays) throws InfrastructureException {
		Pays result;
		long time = tracing.start();
		try {
			result = target.getPays(codePays);
		}
		finally {
			tracing.end(time, "getPays(String)");
		}

		return result;
	}

	public Pays getPaysInconnu() throws InfrastructureException {
		Pays result;
		long time = tracing.start();
		try {
			result = target.getPaysInconnu();
		}
		finally {
			tracing.end(time, "getPaysInconnu");
		}

		return result;
	}

	public Rue getRueByNumero(int numero) throws InfrastructureException {
		Rue result;
		long time = tracing.start();
		try {
			result = target.getRueByNumero(numero);
		}
		finally {
			tracing.end(time, "getRueByNumero");
		}

		return result;
	}

	public List<Rue> getRues(Localite localite) throws InfrastructureException {
		List<Rue> result;
		long time = tracing.start();
		try {
			result = target.getRues(localite);
		}
		finally {
			tracing.end(time, "getRues(localite)");
		}

		return result;
	}

	public List<Rue> getRues(Collection<Localite> localites) throws InfrastructureException {
		List<Rue> result;
		long time = tracing.start();
		try {
			result = target.getRues(localites);
		}
		finally {
			tracing.end(time, "getRues(localites)");
		}

		return result;
	}

	public List<Rue> getRues(Canton canton) throws InfrastructureException {
		List<Rue> result;
		long time = tracing.start();
		try {
			result = target.getRues(canton);
		}
		finally {
			tracing.end(time, "getRues(canton)");
		}

		return result;
	}

	public Pays getSuisse() throws ServiceInfrastructureException {
		Pays result;
		long time = tracing.start();
		try {
			result = target.getSuisse();
		}
		finally {
			tracing.end(time, "getSuisse");
		}

		return result;
	}

	public Canton getVaud() throws InfrastructureException {
		Canton result;
		long time = tracing.start();
		try {
			result = target.getVaud();
		}
		finally {
			tracing.end(time, "getVaud");
		}

		return result;
	}

	public Zone getZone(AdresseGenerique adresse) throws InfrastructureException {
		Zone result;
		long time = tracing.start();
		try {
			result = target.getZone(adresse);
		}
		finally {
			tracing.end(time, "getZone");
		}

		return result;
	}

	public InstitutionFinanciere getInstitutionFinanciere(int id) throws InfrastructureException {
		InstitutionFinanciere result;
		long time = tracing.start();
		try {
			result = target.getInstitutionFinanciere(id);
		}
		finally {
			tracing.end(time, "getInstitutionFinanciere");
		}

		return result;
	}

	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws InfrastructureException {
		List<InstitutionFinanciere> result;
		long time = tracing.start();
		try {
			result = target.getInstitutionsFinancieres(noClearing);
		}
		finally {
			tracing.end(time, "getInstitutionsFinancieres");
		}

		return result;
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, tracing);
		}
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(SERVICE_NAME);
		}
	}

	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		return null;
	}

	public Localite getLocaliteByNPA(int npa) throws InfrastructureException {
		Localite result;
		long time = tracing.start();
		try {
			result = target.getLocaliteByNPA(npa);
		}
		finally {
			tracing.end(time, "getLocaliteByNPA");
		}

		return result;
	}

	public TypeAffranchissement getTypeAffranchissement(int noOfsPays) {
		TypeAffranchissement result;
		long time = tracing.start();
		try {
			result = target.getTypeAffranchissement(noOfsPays);
		}
		finally {
			tracing.end(time, "getTypeAffranchissement");
		}

		return result;
	}
}

