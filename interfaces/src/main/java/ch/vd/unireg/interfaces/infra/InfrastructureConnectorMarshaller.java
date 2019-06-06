package ch.vd.unireg.interfaces.infra;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
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

/**
 * Service de transition qui délégue les appels au service host-interfaces ou au service Fidor.
 */
public class InfrastructureConnectorMarshaller implements InfrastructureConnector {

	private InfrastructureConnector hostConnector = null;
	private InfrastructureConnector fidorConnector = null;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHostConnector(InfrastructureConnector hostConnector) {
		this.hostConnector = hostConnector;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setFidorConnector(InfrastructureConnector fidorConnector) {
		this.fidorConnector = fidorConnector;
	}

	@Override
	public List<Canton> getAllCantons() throws InfrastructureException {
		return fidorConnector.getAllCantons();
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws InfrastructureException {
		return fidorConnector.getCommuneHistoByNumeroOfs(noOfsCommune);
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws InfrastructureException {
		return fidorConnector.getNoOfsCommuneByEgid(egid, date);
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws InfrastructureException {
		return fidorConnector.getListeCommunes(canton);
	}

	@Override
	public List<Localite> getLocalitesByONRP(int onrp) throws InfrastructureException {
		return fidorConnector.getLocalitesByONRP(onrp);
	}

	@Override
	public Localite getLocaliteByONRP(int onrp, RegDate dateReference) throws InfrastructureException {
		return fidorConnector.getLocaliteByONRP(onrp, dateReference);
	}

	@Override
	public List<Localite> getLocalites() throws InfrastructureException {
		return fidorConnector.getLocalites();
	}

	@Override
	public List<Pays> getPays() throws InfrastructureException {
		return fidorConnector.getPays();
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws InfrastructureException {
		return fidorConnector.getPaysHisto(numeroOFS);
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws InfrastructureException {
		return fidorConnector.getPays(numeroOFS, date);
	}

	@Override
	public Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws InfrastructureException {
		return fidorConnector.getPays(codePays, date);
	}

	@Override
	public Rue getRueByNumero(int numero, RegDate date) throws InfrastructureException {
		return fidorConnector.getRueByNumero(numero, date);
	}

	@Override
	public List<Rue> getRues(Localite localite) throws InfrastructureException {
		return fidorConnector.getRues(localite);
	}

	@Override
	public List<Rue> getRuesHisto(int numero) throws InfrastructureException {
		return fidorConnector.getRuesHisto(numero);
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException {
		return hostConnector.getCollectivite(noColAdm);
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException {
		return hostConnector.getOfficesImpot();
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {
		return hostConnector.getCollectivitesAdministratives();
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException {
		return fidorConnector.getCommuneByLocalite(localite);
	}

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws InfrastructureException {
		return fidorConnector.findCommuneByNomOfficiel(nomOfficiel, includeFaitieres, includeFractions, date);
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws InfrastructureException {
		return hostConnector.getCollectivitesAdministratives(typesCollectivite);
	}

	@Override
	public List<Commune> getCommunesVD() throws InfrastructureException {
		return fidorConnector.getCommunesVD();
	}

	@Override
	public List<Commune> getListeCommunesFaitieres() throws InfrastructureException {
		return fidorConnector.getListeCommunesFaitieres();
	}

	@Override
	public List<Commune> getCommunes() throws InfrastructureException {
		return fidorConnector.getCommunes();
	}

	@Override
	public List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws InfrastructureException {
		return fidorConnector.getLocalitesByNPA(npa, dateReference);
	}

	@Override
	public String getUrl(ApplicationFiscale application, @Nullable Map<String, String> parametres) {
		return fidorConnector.getUrl(application, parametres);
	}

	@Override
	public Logiciel getLogiciel(Long idLogiciel) throws InfrastructureException {
		return fidorConnector.getLogiciel(idLogiciel);
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() throws InfrastructureException {
		return fidorConnector.getTousLesLogiciels();
	}

	@Override
	public District getDistrict(int code) {
		return fidorConnector.getDistrict(code);
	}

	@Override
	public Region getRegion(int code) {
		return fidorConnector.getRegion(code);
	}

	@Override
	public List<TypeRegimeFiscal> getTousLesRegimesFiscaux() {
		return fidorConnector.getTousLesRegimesFiscaux();
	}

	@Override
	public List<GenreImpotMandataire> getTousLesGenresImpotMandataires() {
		return fidorConnector.getTousLesGenresImpotMandataires();
	}

	@Override
	public List<CollectiviteAdministrative> findCollectivitesAdministratives(@NotNull Collection<Integer> codeCollectivites, boolean inactif) {
		return fidorConnector.findCollectivitesAdministratives(codeCollectivites, inactif);
	}

	@Override
	public void ping() throws InfrastructureException {
		hostConnector.ping();
		fidorConnector.ping();
	}
}
