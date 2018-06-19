package ch.vd.unireg.interfaces.service.mock;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseException;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseRaw;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseWrapper;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivileEvent;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.model.AdressesCivilesHisto;
import ch.vd.unireg.interfaces.service.ServiceEntreprise;
import ch.vd.unireg.interfaces.service.ServiceEntrepriseImpl;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

/**
 * Proxy du service entreprise civile à enregistrer dans l'application context et permettant à chaque test unitaire de spécifier précisemment l'instance
 * du service entreprise civile à utiliser.
 */
public class ProxyServiceEntreprise implements ServiceEntreprise, ServiceEntrepriseWrapper {

	private ServiceEntrepriseRaw target;
	private final ServiceEntrepriseImpl service;

	public ProxyServiceEntreprise(ServiceInfrastructureService serviceInfra) {
		this.target = null;
		this.service = new ServiceEntrepriseImpl(serviceInfra);
	}

	public void setUp(ServiceEntrepriseRaw target) {
		this.target = target;
		this.service.setTarget(target);
	}

	@Override
	public EntrepriseCivile getEntrepriseHistory(long noEntreprise) throws ServiceEntrepriseException {
		assertTargetNotNull();
		return service.getEntrepriseHistory(noEntreprise);
	}

	@Override
	public Map<Long, EntrepriseCivileEvent> getEntrepriseEvent(long noEvenement) throws ServiceEntrepriseException {
		assertTargetNotNull();
		return service.getEntrepriseEvent(noEvenement);
	}

	@Override
	public ServiceEntrepriseRaw.Identifiers getEntrepriseIdsByNoIde(String noide) throws ServiceEntrepriseException {
		assertTargetNotNull();
		return service.getEntrepriseIdsByNoIde(noide);
	}

	@Override
	public Long getNoEntrepriseCivileFromNoEtablissementCivil(Long noEtablissement) throws ServiceEntrepriseException {
		assertTargetNotNull();
		return service.getNoEntrepriseCivileFromNoEtablissementCivil(noEtablissement);
	}

	@Override
	public AdressesCivilesHisto getAdressesEntrepriseHisto(long noEntrepriseCivile) throws ServiceEntrepriseException {
		assertTargetNotNull();
		return service.getAdressesEntrepriseHisto(noEntrepriseCivile);
	}

	@Nullable
	@Override
	public AdressesCivilesHisto getAdressesEtablissementCivilHisto(long noEtablissementCivil) throws ServiceEntrepriseException {
		assertTargetNotNull();
		return service.getAdressesEtablissementCivilHisto(noEtablissementCivil);
	}

	@Nullable
	@Override
	public AnnonceIDE getAnnonceIDE(long numero, @NotNull String userId) {
		assertTargetNotNull();
		return service.getAnnonceIDE(numero, userId);
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceEntrepriseException {
		assertTargetNotNull();
		return service.findAnnoncesIDE(query, order, pageNumber, resultsPerPage);
	}

	@Override
	public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE annonceIDE) {
		assertTargetNotNull();
		return service.validerAnnonceIDE(annonceIDE);
	}

	@NotNull
	@Override
	public String createEntrepriseDescription(EntrepriseCivile entrepriseCivile, RegDate date) {
		assertTargetNotNull();
		return service.createEntrepriseDescription(entrepriseCivile, date);
	}

	@Override
	public String afficheAttributsEtablissement(@Nullable EtablissementCivil etablissement, @Nullable RegDate date) {
		return service.afficheAttributsEtablissement(etablissement, date);
	}

	private void assertTargetNotNull() {
		if (target == null) {
			throw new IllegalArgumentException("Le service entreprise civile n'a pas été défini !");
		}
	}

	@Override
	public ServiceEntrepriseRaw getTarget() {
		return target;
	}

	@Override
	public ServiceEntrepriseRaw getUltimateTarget() {
		if (target instanceof ServiceEntrepriseWrapper) {
			return ((ServiceEntrepriseWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}
}
