package ch.vd.unireg.interfaces.service.mock;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.HistoriqueCommune;
import ch.vd.unireg.common.DonneesCivilesException;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.unireg.interfaces.civil.ServiceCivilServiceWrapper;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.model.AdressesCiviles;
import ch.vd.unireg.interfaces.model.AdressesCivilesHisto;
import ch.vd.unireg.interfaces.service.ServiceCivilImpl;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

/**
 * Proxy du service civil à enregistrer dans l'application context et permettant à chaque test unitaire de spécifier précisemment l'instance
 * du service civil à utiliser.
 */
public class ProxyServiceCivil implements ServiceCivilService, ServiceCivilServiceWrapper {

	private ServiceCivilRaw target;
	private final ServiceCivilImpl service;

	public ProxyServiceCivil() {
		this.service = new ServiceCivilImpl();
	}

	public ProxyServiceCivil(ServiceInfrastructureService infraService) {
		this.target = null;
		this.service = new ServiceCivilImpl(infraService);
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.service.setInfraService(infraService);
	}

	public void setUp(ServiceCivilRaw target) {
		this.target = target;
		this.service.setTarget(target);
	}

	@Override
	public AdressesCiviles getAdresses(long noIndividu, RegDate date, boolean strict) throws DonneesCivilesException {
		assertTargetNotNull();
		return service.getAdresses(noIndividu, date, strict);
	}

	@Override
	public AdressesCivilesHisto getAdressesHisto(long noIndividu, boolean strict) throws DonneesCivilesException {
		assertTargetNotNull();
		return service.getAdressesHisto(noIndividu, strict);
	}

	@Override
	public Individu getIndividu(long noIndividu, @Nullable RegDate date, AttributeIndividu... parties) throws ServiceCivilException {
		assertTargetNotNull();
		return service.getIndividu(noIndividu, date, parties);
	}

	@Override
	public Individu getConjoint(Long noIndividuPrincipal, @Nullable RegDate date) {
		assertTargetNotNull();
		return service.getConjoint(noIndividuPrincipal, date);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Long getNumeroIndividuConjoint(Long noIndividuPrincipal, RegDate date) {
		assertTargetNotNull();
		return service.getNumeroIndividuConjoint(noIndividuPrincipal, date);
	}

	@Override
	public Set<Long> getNumerosIndividusConjoint(Long noIndividuPrincipal) {
		assertTargetNotNull();
		return service.getNumerosIndividusConjoint(noIndividuPrincipal);
	}

	@Override
	public Set<Long> getNumerosIndividusParents(Long noIndividuPrincipal) {
		assertTargetNotNull();
		return service.getNumerosIndividusParents(noIndividuPrincipal);
	}

	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, AttributeIndividu... parties) throws ServiceCivilException {
		assertTargetNotNull();
		return service.getIndividus(nosIndividus, date, parties);
	}

	@Override
	public IndividuApresEvenement getIndividuAfterEvent(long eventId) {
		assertTargetNotNull();
		return service.getIndividuAfterEvent(eventId);
	}

	@Override
	public Individu getIndividuByEvent(long eventId, @Nullable RegDate date, AttributeIndividu... parties) throws ServiceCivilException {
		assertTargetNotNull();
		return service.getIndividuByEvent(eventId, date, parties);
	}

	@Override
	public Collection<Origine> getOrigines(long noTechniqueIndividu, RegDate date) {
		assertTargetNotNull();
		return service.getOrigines(noTechniqueIndividu, date);
	}

	@Override
	public Collection<Permis> getPermis(long noIndividu, @Nullable RegDate date) {
		assertTargetNotNull();
		return service.getPermis(noIndividu, date);
	}

	@Override
	public Collection<Nationalite> getNationalites(long noIndividu, @Nullable RegDate date) {
		assertTargetNotNull();
		return service.getNationalites(noIndividu, date);
	}

	@Override
	public String getNomPrenom(Individu individu) {
		assertTargetNotNull();
		return service.getNomPrenom(individu);
	}

	@Override
	public NomPrenom getDecompositionNomPrenom(Individu individu, boolean tousPrenoms) {
		assertTargetNotNull();
		return service.getDecompositionNomPrenom(individu, tousPrenoms);
	}

	private void assertTargetNotNull() {
		if (target == null) {
			throw new IllegalArgumentException("Le service civil n'a pas été défini !");
		}
	}

	@Override
	public EtatCivil getEtatCivilActif(long noIndividu, RegDate date) {
		assertTargetNotNull();
		return service.getEtatCivilActif(noIndividu, date);
	}

	@Override
	public boolean isWarmable() {
		assertTargetNotNull();
		return service.isWarmable();
	}

	@Override
	public List<HistoriqueCommune> getCommunesDomicileHisto(RegDate depuis, long noIndividu, boolean strict, boolean seulementVaud) throws DonneesCivilesException, ServiceInfrastructureException {
		assertTargetNotNull();
		return service.getCommunesDomicileHisto(depuis, noIndividu, strict, seulementVaud);
	}

	@Override
	public ServiceCivilRaw getTarget() {
		return target;
	}

	@Override
	public ServiceCivilRaw getUltimateTarget() {
		if (target instanceof ServiceCivilServiceWrapper) {
			return ((ServiceCivilServiceWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}
}
