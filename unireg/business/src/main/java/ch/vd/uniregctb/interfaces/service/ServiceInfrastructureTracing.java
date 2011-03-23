package ch.vd.uniregctb.interfaces.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.infrastructure.service.InfrastructureException;
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
import ch.vd.uniregctb.stats.StatsService;

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

	public boolean estDansLeCanton(final Rue rue) throws InfrastructureException {
		boolean result;
		long time = tracing.start();
		try {
			result = target.estDansLeCanton(rue);
		}
		finally {
			tracing.end(time, "estDansLeCanton", new Object() {
				@Override
				public String toString() {
					return String.format("rue=%s", rue != null ? rue.getNoRue() : null);
				}
			});
		}
		return result;
	}

	public boolean estDansLeCanton(final Commune commune) throws InfrastructureException {
		boolean result;
		long time = tracing.start();
		try {
			result = target.estDansLeCanton(commune);
		}
		finally {
			tracing.end(time, "estDansLeCanton", new Object() {
				@Override
				public String toString() {
					return String.format("commune=%s", commune != null ? commune.getNoOFSEtendu() : null);
				}
			});
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
			tracing.end(time, "estDansLeCanton", "adresseGenerique");
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
			tracing.end(time, "estDansLeCanton", "adresse");
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
			tracing.end(time, "estEnSuisse", "adresseGenerique");
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
			tracing.end(time, "estEnSuisse", "adresse");
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
			tracing.end(time, "getACI", null);
		}

		return result;
	}

	public CollectiviteAdministrative getACIImpotSource() throws InfrastructureException {
	CollectiviteAdministrative result;
		long time = tracing.start();
		try {
			result = target.getACIImpotSource();
		}
		finally {
			tracing.end(time, "getACIImpotSource", null);
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
			tracing.end(time, "getACISuccessions", null);
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
			tracing.end(time, "getCEDI", null);
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
			tracing.end(time, "getCAT", null);
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
			tracing.end(time, "getAllCantons", null);
		}

		return result;
	}

	public Canton getCanton(final int cantonOFS) throws InfrastructureException {
		Canton result;
		long time = tracing.start();
		try {
			result = target.getCanton(cantonOFS);
		}
		finally {
			tracing.end(time, "getCanton", new Object() {
				@Override
				public String toString() {
					return String.format("cantonOFS=%d", cantonOFS);
				}
			});
		}

		return result;
	}

	public Canton getCantonByCommune(final int noOfsCommune) throws InfrastructureException {
		Canton result;
		long time = tracing.start();
		try {
			result = target.getCantonByCommune(noOfsCommune);
		}
		finally {
			tracing.end(time, "getCantonByCommune", new Object() {
				@Override
				public String toString() {
					return String.format("noOfsCommune=%d", noOfsCommune);
				}
			});
		}

		return result;
	}

	public Canton getCantonBySigle(final String sigle) throws InfrastructureException {
		Canton result;
		long time = tracing.start();
		try {
			result = target.getCantonBySigle(sigle);
		}
		finally {
			tracing.end(time, "getCantonBySigle", new Object() {
				@Override
				public String toString() {
					return String.format("sigle=%s", sigle);
				}
			});
		}

		return result;
	}

	public CollectiviteAdministrative getCollectivite(final int noColAdm) throws InfrastructureException {
		CollectiviteAdministrative result;
		long time = tracing.start();
		try {
			result = target.getCollectivite(noColAdm);
		}
		finally {
			tracing.end(time, "getCollectivite", new Object() {
				@Override
				public String toString() {
					return String.format("noColAdm=%d", noColAdm);
				}
			});
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
			tracing.end(time, "getCollectivitesAdministratives", null);
		}

		return result;
	}

	public List<CollectiviteAdministrative> getCollectivitesAdministratives(final List<EnumTypeCollectivite> typesCollectivite)
			throws InfrastructureException {
		List<CollectiviteAdministrative> result;
		long time = tracing.start();
		try {
			result = target.getCollectivitesAdministratives(typesCollectivite);
		}
		finally {
			tracing.end(time, "getCollectivitesAdministratives", new Object() {
				@Override
				public String toString() {
					return String.format("typesCollectivite=%s", ServiceTracing.toString(typesCollectivite));
				}
			});
		}

		return result;
	}

	public Commune getCommuneByAdresse(Adresse adresse, RegDate date) throws InfrastructureException {
		Commune result;
		long time = tracing.start();
		try {
			result = target.getCommuneByAdresse(adresse, date);
		}
		finally {
			tracing.end(time, "getCommuneByAdresse", "adresse");
		}

		return result;
	}

	public Commune getCommuneByAdresse(AdresseGenerique adresse, RegDate date) throws InfrastructureException {
		Commune result;
		long time = tracing.start();
		try {
			result = target.getCommuneByAdresse(adresse, date);
		}
		finally {
			tracing.end(time, "getCommuneByAdresse", "adresseGenerique");
		}

		return result;
	}

	@Override
	public Integer getNoOfsCommuneByEgid(final int egid, final RegDate date, final int hintNoOfsCommune) throws InfrastructureException {
		Integer result;
		long time = tracing.start();
		try {
			result = target.getNoOfsCommuneByEgid(egid, date, hintNoOfsCommune);
		}
		finally {
			tracing.end(time, "getNoOfsCommuneByEgid", new Object() {
				@Override
				public String toString() {
					return String.format("egid=%d, date=%s, hintNoOfsCommune=%d", egid, ServiceTracing.toString(date), hintNoOfsCommune);
				}
			});
		}

		return result;
	}

	@Override
	public Commune getCommuneByEgid(final int egid, final RegDate date, final int hintNoOfsCommune) throws InfrastructureException {
		Commune result;
		long time = tracing.start();
		try {
			result = target.getCommuneByEgid(egid, date, hintNoOfsCommune);
		}
		finally {
			tracing.end(time, "getCommuneByEgid", new Object() {
				@Override
				public String toString() {
					return String.format("egid=%d, date=%s, hintNoOfsCommune=%d", egid, ServiceTracing.toString(date), hintNoOfsCommune);
				}
			});
		}

		return result;
	}

	public Commune getCommuneByLocalite(final Localite localite) throws InfrastructureException {
		Commune result;
		long time = tracing.start();
		try {
			result = target.getCommuneByLocalite(localite);
		}
		finally {
			tracing.end(time, "getCommuneByLocalite", new Object() {
				@Override
				public String toString() {
					return String.format("localite=%s", localite != null ? localite.getNoOrdre() : null);
				}
			});
		}

		return result;
	}

	public Commune getCommuneByNumeroOfsEtendu(final int noCommune, final RegDate date) throws InfrastructureException {
		Commune result;
		long time = tracing.start();
		try {
			result = target.getCommuneByNumeroOfsEtendu(noCommune, date);
		}
		finally {
			tracing.end(time, "getCommuneByNumeroOfsEtendu", new Object() {
				@Override
				public String toString() {
					return String.format("noCommune=%d, date=%s", noCommune, ServiceTracing.toString(date));
				}
			});
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
			tracing.end(time, "getCommunesDeVaud", null);
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
			tracing.end(time, "getCommunesHorsCanton", null);
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
			tracing.end(time, "getCommunes", null);
		}

		return result;
	}

	public List<Commune> getListeCommunes(final Canton canton) throws InfrastructureException {
		List<Commune> result;
		long time = tracing.start();
		try {
			result = target.getListeCommunes(canton);
		}
		finally {
			tracing.end(time, "getListeCommunes", new Object() {
				@Override
				public String toString() {
					return String.format("canton=%s", canton != null ? canton.getSigleOFS() : null);
				}
			});
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
			tracing.end(time, "getListeFractionsCommunes", null);
		}

		return result;
	}

	public List<Commune> getListeCommunes(final int cantonOFS) throws InfrastructureException {
		List<Commune> result;
		long time = tracing.start();
		try {
			result = target.getListeCommunes(cantonOFS);
		}
		finally {
			tracing.end(time, "getListeCommunes", new Object() {
				@Override
				public String toString() {
					return String.format("cantonOFS=%d", cantonOFS);
				}
			});
		}

		return result;
	}

	public List<Commune> getListeCommunesByOID(final int oid) throws InfrastructureException {
		List<Commune> result;
		long time = tracing.start();
		try {
			result = target.getListeCommunesByOID(oid);
		}
		finally {
			tracing.end(time, "getListeCommunesByOID", new Object() {
				@Override
				public String toString() {
					return String.format("oid=%d", oid);
				}
			});
		}

		return result;
	}

	public Commune getCommuneFaitiere(final Commune commune, final RegDate dateReference) throws InfrastructureException {
		Commune result;
		long time = tracing.start();
		try {
			result = target.getCommuneFaitiere(commune, dateReference);
		}
		finally {
			tracing.end(time, "getCommuneFaitiere", new Object() {
				@Override
				public String toString() {
					return String.format("commune=%s, dateReference=%s", commune != null ? commune.getNoOFSEtendu() : null, ServiceTracing.toString(dateReference));
				}
			});
		}

		return result;
	}

	public List<Localite> getLocaliteByCommune(final int commune) throws InfrastructureException {
		List<Localite> result;
		long time = tracing.start();
		try {
			result = target.getLocaliteByCommune(commune);
		}
		finally {
			tracing.end(time, "getLocaliteByCommune", new Object() {
				@Override
				public String toString() {
					return String.format("commune=%d", commune);
				}
			});
		}

		return result;
	}

	public Localite getLocaliteByONRP(final int onrp) throws InfrastructureException {
		Localite result;
		long time = tracing.start();
		try {
			result = target.getLocaliteByONRP(onrp);
		}
		finally {
			tracing.end(time, "getLocaliteByONRP", new Object() {
				@Override
				public String toString() {
					return String.format("onrp=%d", onrp);
				}
			});
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
			tracing.end(time, "getLocalites", null);
		}

		return result;
	}

	public OfficeImpot getOfficeImpot(final int noColAdm) throws InfrastructureException {
		OfficeImpot result;
		long time = tracing.start();
		try {
			result = target.getOfficeImpot(noColAdm);
		}
		finally {
			tracing.end(time, "getOfficeImpot", new Object() {
				@Override
				public String toString() {
					return String.format("noColAdm=%d", noColAdm);
				}
			});
		}

		return result;
	}

	public OfficeImpot getOfficeImpotDeCommune(final int noCommune) throws InfrastructureException {
		OfficeImpot result;
		long time = tracing.start();
		try {
			result = target.getOfficeImpotDeCommune(noCommune);
		}
		finally {
			tracing.end(time, "getOfficeImpotDeCommune", new Object() {
				@Override
				public String toString() {
					return String.format("noCommune=%d", noCommune);
				}
			});
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
			tracing.end(time, "getOfficesImpot", null);
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
			tracing.end(time, "getPays", null);
		}

		return result;
	}

	public Pays getPays(final int numeroOFS) throws InfrastructureException {
		Pays result;
		long time = tracing.start();
		try {
			result = target.getPays(numeroOFS);
		}
		finally {
			tracing.end(time, "getPays", new Object() {
				@Override
				public String toString() {
					return String.format("numeroOFS=%d", numeroOFS);
				}
			});
		}

		return result;
	}


	public Pays getPays(final String codePays) throws InfrastructureException {
		Pays result;
		long time = tracing.start();
		try {
			result = target.getPays(codePays);
		}
		finally {
			tracing.end(time, "getPays", new Object() {
				@Override
				public String toString() {
					return String.format("codePays=%s", codePays);
				}
			});
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
			tracing.end(time, "getPaysInconnu", null);
		}

		return result;
	}

	public Rue getRueByNumero(final int numero) throws InfrastructureException {
		Rue result;
		long time = tracing.start();
		try {
			result = target.getRueByNumero(numero);
		}
		finally {
			tracing.end(time, "getRueByNumero", new Object() {
				@Override
				public String toString() {
					return String.format("numero=%d", numero);
				}
			});
		}

		return result;
	}

	public List<Rue> getRues(final Localite localite) throws InfrastructureException {
		List<Rue> result;
		long time = tracing.start();
		try {
			result = target.getRues(localite);
		}
		finally {
			tracing.end(time, "getRues", new Object() {
				@Override
				public String toString() {
					return String.format("localite=%s", localite != null ? localite.getNoOrdre() : null);
				}
			});
		}

		return result;
	}

	public List<Rue> getRues(final Collection<Localite> localites) throws InfrastructureException {
		List<Rue> result;
		long time = tracing.start();
		try {
			result = target.getRues(localites);
		}
		finally {
			tracing.end(time, "getRues", new Object() {
				@Override
				public String toString() {
					final String parms;
					if (localites != null) {
						final List<Integer> norp = new ArrayList<Integer>(localites.size());
						for (Localite localite : localites) {
							norp.add(localite.getNoOrdre());
						}
						parms = ServiceTracing.toString(norp);
					}
					else {
						parms = null;
					}
					return String.format("localites=%s", parms);
				}
			});
		}

		return result;
	}

	public List<Rue> getRues(final Canton canton) throws InfrastructureException {
		List<Rue> result;
		long time = tracing.start();
		try {
			result = target.getRues(canton);
		}
		finally {
			tracing.end(time, "getRues", new Object() {
				@Override
				public String toString() {
					return String.format("canton=%s", canton != null ? canton.getSigleOFS() : null);
				}
			});
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
			tracing.end(time, "getSuisse", null);
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
			tracing.end(time, "getVaud", null);
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
			tracing.end(time, "getZone", "adresseGenerique");
		}

		return result;
	}

	public InstitutionFinanciere getInstitutionFinanciere(final int id) throws InfrastructureException {
		InstitutionFinanciere result;
		long time = tracing.start();
		try {
			result = target.getInstitutionFinanciere(id);
		}
		finally {
			tracing.end(time, "getInstitutionFinanciere", new Object() {
				@Override
				public String toString() {
					return String.format("id=%d", id);
				}
			});
		}

		return result;
	}

	public List<InstitutionFinanciere> getInstitutionsFinancieres(final String noClearing) throws InfrastructureException {
		List<InstitutionFinanciere> result;
		long time = tracing.start();
		try {
			result = target.getInstitutionsFinancieres(noClearing);
		}
		finally {
			tracing.end(time, "getInstitutionsFinancieres", new Object() {
				@Override
				public String toString() {
					return String.format("noClearing=%s", noClearing);
				}
			});
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

	public Localite getLocaliteByNPA(final int npa) throws InfrastructureException {
		Localite result;
		long time = tracing.start();
		try {
			result = target.getLocaliteByNPA(npa);
		}
		finally {
			tracing.end(time, "getLocaliteByNPA", new Object() {
				@Override
				public String toString() {
					return String.format("npa=%d", npa);
				}
			});
		}

		return result;
	}

	public TypeAffranchissement getTypeAffranchissement(final int noOfsPays) {
		TypeAffranchissement result;
		long time = tracing.start();
		try {
			result = target.getTypeAffranchissement(noOfsPays);
		}
		finally {
			tracing.end(time, "getTypeAffranchissement", new Object() {
				@Override
				public String toString() {
					return String.format("noOfsPays=%d", noOfsPays);
				}
			});
		}

		return result;
	}

	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws InfrastructureException {
		List<TypeRegimeFiscal> result;
		long time = tracing.start();
		try {
			result = target.getTypesRegimesFiscaux();
		}
		finally {
			tracing.end(time, "getTypesRegimesFiscaux", null);
		}

		return result;
	}

	public TypeRegimeFiscal getTypeRegimeFiscal(final String code) throws InfrastructureException {
		TypeRegimeFiscal result;
		long time = tracing.start();
		try {
			result = target.getTypeRegimeFiscal(code);
		}
		finally {
			tracing.end(time, "getTypeRegimeFiscal", new Object() {
				@Override
				public String toString() {
					return String.format("code=%s", code);
				}
			});
		}

		return result;
	}

	public List<TypeEtatPM> getTypesEtatsPM() throws InfrastructureException {
		List<TypeEtatPM> result;
		long time = tracing.start();
		try {
			result = target.getTypesEtatsPM();
		}
		finally {
			tracing.end(time, "getTypesEtatsPM", null);
		}

		return result;
	}

	public TypeEtatPM getTypeEtatPM(final String code) throws InfrastructureException {
		TypeEtatPM result;
		long time = tracing.start();
		try {
			result = target.getTypeEtatPM(code);
		}
		finally {
			tracing.end(time, "getTypeEtatPM", new Object() {
				@Override
				public String toString() {
					return String.format("code=%s", code);
				}
			});
		}

		return result;
	}

	public String getUrlVers(final ApplicationFiscale application, final Long tiersId) {
		String result;
		final long time = tracing.start();
		try {
			result = target.getUrlVers(application, tiersId);
		}
		finally {
			tracing.end(time, "getUrlVers", new Object() {
				@Override
				public String toString() {
					return String.format("application=%s, tiersId=%d", application.name(), tiersId);
				}
			});
		}

		return result;
	}

	public Logiciel getLogiciel(final Long idLogiciel) {
		Logiciel result;
		long time = tracing.start();
		try {
			result = target.getLogiciel(idLogiciel);
		}
		finally {
			tracing.end(time, "getLogiciel", new Object() {
				@Override
				public String toString() {
					return String.format("id=%d", idLogiciel);
				}
			});
		}

		return result;
	}

	public List<Logiciel> getTousLesLogiciels() {
		List<Logiciel> result;
		long time = tracing.start();
		try {
			result = target.getTousLesLogiciels();
		}
		finally {
			tracing.end(time, "getTousLesLogiciels", null);
		}

		return result;
	}

	public List<Logiciel> getLogicielsPour(final LogicielMetier metier) {
		List<Logiciel> result;
		long time = tracing.start();
		try {
			result = target.getLogicielsPour(metier);
		}
		finally {
			tracing.end(time, "getLogicielsPour", new Object() {
				@Override
				public String toString() {
					return String.format("metier=%s", metier);
				}
			});
		}

		return result;
	}
}

