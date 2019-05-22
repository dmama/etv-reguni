package ch.vd.unireg.interfaces.infra;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.load.DetailedLoadMeter;
import ch.vd.unireg.load.MethodCallDescriptor;
import ch.vd.unireg.stats.DetailedLoadMonitorable;
import ch.vd.unireg.stats.LoadDetail;

public class InfrastructureConnectorEndPoint implements InfrastructureConnector, DetailedLoadMonitorable {

	private InfrastructureConnector target;

	private final DetailedLoadMeter<MethodCallDescriptor> loadMeter = new DetailedLoadMeter<>();

	public void setTarget(InfrastructureConnector target) {
		this.target = target;
	}

	@Override
	public List<Pays> getPays() throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getPays"));
		try {
			return target.getPays();
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getPaysHisto", "numeroOfs", numeroOFS));
		try {
			return target.getPaysHisto(numeroOFS);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getPays", "numeroOfs", numeroOFS, "date", date));
		try {
			return target.getPays(numeroOFS, date);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getPays", "codePays", codePays, "date", date));
		try {
			return target.getPays(codePays, date);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getCollectivite", "noColAdm", noColAdm));
		try {
			return target.getCollectivite(noColAdm);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Canton> getAllCantons() throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getAllCantons"));
		try {
			return target.getAllCantons();
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getListeCommunes", "canton", canton != null ? canton.getSigleOFS() : null));
		try {
			return target.getListeCommunes(canton);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Commune> getCommunesVD() throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getCommunesVD"));
		try {
			return target.getCommunesVD();
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Commune> getListeCommunesFaitieres() throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getListeCommunesFaitieres"));
		try {
			return target.getListeCommunesFaitieres();
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Commune> getCommunes() throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getCommunes"));
		try {
			return target.getCommunes();
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Localite> getLocalites() throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getLocalites"));
		try {
			return target.getLocalites();
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Localite> getLocalitesByONRP(int onrp) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getLocaliteByONRP", "onrp", onrp));
		try {
			return target.getLocalitesByONRP(onrp);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Localite getLocaliteByONRP(int onrp, RegDate dateReference) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getLocaliteByONRP", "onrp", onrp, "dateReference", dateReference));
		try {
			return target.getLocaliteByONRP(onrp, dateReference);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Rue> getRues(Localite localite) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getRues", "localite", localite == null ? null : localite.getNom()));
		try {
			return target.getRues(localite);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Rue> getRuesHisto(int numero) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getRuesHisto", "numero", numero));
		try {
			return target.getRuesHisto(numero);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Rue getRueByNumero(int numero, RegDate date) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getRueByNumero", "numero", numero, "date", date));
		try {
			return target.getRueByNumero(numero, date);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getCommuneHistoByNumeroOfs", "noOfsCommune", noOfsCommune));
		try {
			return target.getCommuneHistoByNumeroOfs(noOfsCommune);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getNoOfsCommuneByEgid", "egid", egid, "date", date));
		try {
			return target.getNoOfsCommuneByEgid(egid, date);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getCommuneByLocalite", "localite", localite == null ? null : localite.getNom()));
		try {
			return target.getCommuneByLocalite(localite);
		}
		finally {
			loadMeter.end();
		}
	}

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("findCommuneByNomOfficiel", new String[]{"nomOfficiel", "includeFaitieres", "includeFractions", "date"}, new Object[]{nomOfficiel, includeFaitieres, includeFractions, date}));
		try {
			return target.findCommuneByNomOfficiel(nomOfficiel, includeFaitieres, includeFractions, date);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getOfficesImpot"));
		try {
			return target.getOfficesImpot();
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getCollectivitesAdministratives"));
		try {
			return target.getCollectivitesAdministratives();
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getCollectivitesAdministratives", "typesCollectivites", typesCollectivite));
		try {
			return target.getCollectivitesAdministratives(typesCollectivite);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("getLocaliteByNPA", "npa", npa, "dateReference", dateReference));
		try {
			return target.getLocalitesByNPA(npa, dateReference);
		}
		finally {
			loadMeter.end();
		}
	}

	private static String buildMapString(Map<String, String> map) {
		return CollectionsUtils.toString(map,
		                                 StringRenderer.DEFAULT,
		                                 str -> str != null ? String.format("'%s'", str) : StringUtils.EMPTY,
		                                 ", ",
		                                 "[",
		                                 "]",
		                                 "null");
	}

	@Override
	public String getUrl(ApplicationFiscale application, @Nullable Map<String, String> parametres) {
		loadMeter.start(new MethodCallDescriptor("getUrl", "application", application, "parametres", buildMapString(parametres)));
		try {
			return target.getUrl(application, parametres);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Logiciel getLogiciel(Long id) {
		loadMeter.start(new MethodCallDescriptor("getLogiciel", "id", id));
		try {
			return target.getLogiciel(id);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() {
		loadMeter.start(new MethodCallDescriptor("getTousLesLogiciels"));
		try {
			return target.getTousLesLogiciels();
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public District getDistrict(int code) {
		loadMeter.start(new MethodCallDescriptor("getDistrict", "code", code));
		try {
			return target.getDistrict(code);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Region getRegion(int code) {
		loadMeter.start(new MethodCallDescriptor("getRegion", "code", code));
		try {
			return target.getRegion(code);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<TypeRegimeFiscal> getTousLesRegimesFiscaux() {
		loadMeter.start(new MethodCallDescriptor("getTousLesRegimesFiscaux"));
		try {
			return target.getTousLesRegimesFiscaux();
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<GenreImpotMandataire> getTousLesGenresImpotMandataires() {
		loadMeter.start(new MethodCallDescriptor("getTousLesGenresImpotMandataires"));
		try {
			return target.getTousLesGenresImpotMandataires();
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<CollectiviteAdministrative> findCollectivitesAdministratives(List<Integer> codeCollectivites, boolean inactif) {
		loadMeter.start(new MethodCallDescriptor("findCollectivitesAdministratives"));
		try {
			return target.findCollectivitesAdministratives(codeCollectivites, inactif);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<LoadDetail> getLoadDetails() {
		return loadMeter.getLoadDetails();
	}

	@Override
	public int getLoad() {
		return loadMeter.getLoad();
	}

	@Override
	public void ping() throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("ping"));
		try {
			target.ping();
		}
		finally {
			loadMeter.end();
		}
	}
}
