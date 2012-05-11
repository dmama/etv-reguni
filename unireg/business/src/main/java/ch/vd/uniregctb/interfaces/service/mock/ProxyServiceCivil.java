package ch.vd.uniregctb.interfaces.service.mock;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.unireg.interfaces.civil.ServiceCivilServiceWrapper;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Origine;
import ch.vd.unireg.interfaces.civil.data.Permis;
import ch.vd.unireg.interfaces.civil.data.Tutelle;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.HistoriqueCommune;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesActives;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.interfaces.service.ServiceCivilImpl;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

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
		this.service = new ServiceCivilImpl();
		this.service.setInfraService(infraService);
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.service.setInfraService(infraService);
	}

	public void setUp(ServiceCivilRaw target) {
		this.target = target;
		this.service.setTarget(target);
	}

	public void tearDown() {
		this.target = null;
	}

	@Override
	public AdressesCivilesActives getAdresses(long noIndividu, RegDate date, boolean strict) throws DonneesCivilesException {
		assertTargetNotNull();
		return service.getAdresses(noIndividu, date, strict);
	}

	@Override
	public AdressesCivilesHistoriques getAdressesHisto(long noIndividu, boolean strict) throws DonneesCivilesException {
		assertTargetNotNull();
		return service.getAdressesHisto(noIndividu, strict);
	}

	@Override
	public Individu getIndividu(long noIndividu, @Nullable RegDate date, AttributeIndividu... parties) {
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
	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, AttributeIndividu... parties) {
		assertTargetNotNull();
		return service.getIndividus(nosIndividus, date, parties);
	}

	@Override
	public IndividuApresEvenement getIndividuFromEvent(long eventId) {
		assertTargetNotNull();
		return service.getIndividuFromEvent(eventId);
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
	public Tutelle getTutelle(long noTechniqueIndividu, RegDate date) {
		assertTargetNotNull();
		return service.getTutelle(noTechniqueIndividu, date);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection getNationalites(long noTechniqueIndividu, RegDate date) {
		assertTargetNotNull();
		return service.getNationalites(noTechniqueIndividu, date);
	}

	@Override
	public String getNomPrenom(Individu individu) {
		assertTargetNotNull();
		return service.getNomPrenom(individu);
	}

	@Override
	public NomPrenom getDecompositionNomPrenom(Individu individu) {
		assertTargetNotNull();
		return service.getDecompositionNomPrenom(individu);
	}

	private void assertTargetNotNull() {
		Assert.notNull(target, "Le service civil n'a pas été défini !");
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
	public void setIndividuLogger(boolean value) {
		assertTargetNotNull();
		service.setIndividuLogger(value);
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
