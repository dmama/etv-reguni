package ch.vd.unireg.interfaces.infra.cache;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.InfrastructureConnector;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
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

public class NotImplementedInfrastructureConnector implements InfrastructureConnector {

	@Override
	public List<Pays> getPays() throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<Canton> getAllCantons() throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<Commune> getCommunesVD() throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<Commune> getListeCommunesFaitieres() throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<Commune> getCommunes() throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<Localite> getLocalites() throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<Localite> getLocalitesByONRP(int onrp) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public Localite getLocaliteByONRP(int onrp, RegDate dateReference) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<Rue> getRues(Localite localite) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<Rue> getRuesHisto(int numero) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public Rue getRueByNumero(int numero, RegDate date) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<TypeCollectivite> typesCollectivite) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public List<Localite> getLocalitesByNPA(int npa, RegDate dateReference) throws InfrastructureException {
		throw new NotImplementedException("");
	}

	@Override
	public String getUrl(ApplicationFiscale application, @Nullable Map<String, String> parametres) {
		throw new NotImplementedException("");
	}

	@Override
	public Logiciel getLogiciel(Long id) {
		throw new NotImplementedException("");
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() {
		throw new NotImplementedException("");
	}

	@Override
	public District getDistrict(int code) {
		throw new NotImplementedException("");
	}

	@Override
	public Region getRegion(int code) {
		throw new NotImplementedException("");
	}

	@Override
	public List<TypeRegimeFiscal> getTousLesRegimesFiscaux() {
		throw new NotImplementedException("");
	}

	@Override
	public List<GenreImpotMandataire> getTousLesGenresImpotMandataires() {
		throw new NotImplementedException("");
	}

	@Override
	public List<CollectiviteAdministrative> findCollectivitesAdministratives(@NotNull Collection<Integer> codeCollectivites, boolean inactif) {
		throw new NotImplementedException("");
	}

	@Override
	public void ping() throws InfrastructureException {
		throw new NotImplementedException("");
	}
}
