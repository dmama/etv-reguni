package ch.vd.unireg.interfaces.infra.cache;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;

public class NotImplementedServiceInfrastructure implements ServiceInfrastructureRaw {

	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<Pays> getPaysHisto(int numeroOFS) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public Pays getPays(int numeroOFS, @Nullable RegDate date) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public Pays getPays(@NotNull String codePays, @Nullable RegDate date) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public CollectiviteAdministrative getCollectivite(int noColAdm) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<Commune> getListeCommunes(Canton canton) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<Localite> getLocalitesByONRP(int onrp) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<Rue> getRues(Localite localite) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<Rue> getRuesHisto(int numero) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public Rue getRueByNumero(int numero, RegDate date) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(int noOfsCommune) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public Integer getNoOfsCommuneByEgid(int egid, RegDate date) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public Commune getCommuneByLocalite(Localite localite) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(List<EnumTypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public List<InstitutionFinanciere> getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public Localite getLocaliteByNPA(int npa) throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}

	@Override
	public String getUrlVers(ApplicationFiscale application, Long tiersId, Integer oid) {
		throw new NotImplementedException();
	}

	@Override
	public Logiciel getLogiciel(Long id) {
		throw new NotImplementedException();
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() {
		throw new NotImplementedException();
	}

	@Override
	public District getDistrict(int code) {
		throw new NotImplementedException();
	}

	@Override
	public Region getRegion(int code) {
		throw new NotImplementedException();
	}

	@Override
	public List<TypeRegimeFiscal> getTousLesRegimesFiscaux() {
		throw new NotImplementedException();
	}

	@Override
	public void ping() throws ServiceInfrastructureException {
		throw new NotImplementedException();
	}
}
