package ch.vd.uniregctb.interfaces.service.mock;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.HistoriqueCommune;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesActives;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceCivilServiceWrapper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureException;

/**
 * Proxy du service civil à enregistrer dans l'application context et permettant à chaque test unitaire de spécifier précisemment l'instance
 * du service civil à utiliser.
 */
public class ProxyServiceCivil implements ServiceCivilService, ServiceCivilServiceWrapper {

	private ServiceCivilService target;

	public ProxyServiceCivil() {
		this.target = null;
	}

	public void setUp(ServiceCivilService target) {
		this.target = target;
	}

	public void tearDown() {
		this.target = null;
	}

	@Override
	public AdressesCivilesActives getAdresses(long noIndividu, RegDate date, boolean strict) throws DonneesCivilesException {
		assertTargetNotNull();
		return target.getAdresses(noIndividu, date, strict);
	}

	@Override
	public AdressesCivilesHistoriques getAdressesHisto(long noIndividu, boolean strict) throws DonneesCivilesException {
		assertTargetNotNull();
		return target.getAdressesHisto(noIndividu, strict);
	}

	@Override
	public Individu getIndividu(long noIndividu, @Nullable RegDate date, AttributeIndividu... parties) {
		assertTargetNotNull();
		return target.getIndividu(noIndividu, date, parties);
	}

	@Override
	public Individu getConjoint(Long noIndividuPrincipal, RegDate date) {
		assertTargetNotNull();
		return target.getConjoint(noIndividuPrincipal, date);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Long getNumeroIndividuConjoint(Long noIndividuPrincipal, RegDate date) {
		assertTargetNotNull();
		return target.getNumeroIndividuConjoint(noIndividuPrincipal, date);
	}

	@Override
	public Set<Long> getNumerosIndividusConjoint(Long noIndividuPrincipal) {
		assertTargetNotNull();
		return target.getNumerosIndividusConjoint(noIndividuPrincipal);
	}

	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, AttributeIndividu... parties) {
		assertTargetNotNull();
		return target.getIndividus(nosIndividus, date, parties);
	}

	@Override
	public Collection<Origine> getOrigines(long noTechniqueIndividu, int anneeValidite) {
		assertTargetNotNull();
		return target.getOrigines(noTechniqueIndividu, anneeValidite);
	}

	@Override
	public Permis getPermis(long noIndividu, @Nullable RegDate date) {
		assertTargetNotNull();
		return target.getPermis(noIndividu, date);
	}

	@Override
	public Tutelle getTutelle(long noTechniqueIndividu, int anneeValidite) {
		assertTargetNotNull();
		return target.getTutelle(noTechniqueIndividu, anneeValidite);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection getNationalites(long noTechniqueIndividu, int anneeValidite) {
		assertTargetNotNull();
		return target.getNationalites(noTechniqueIndividu, anneeValidite);
	}

	@Override
	public String getNomPrenom(Individu individu) {
		assertTargetNotNull();
		return target.getNomPrenom(individu);
	}

	@Override
	public NomPrenom getDecompositionNomPrenom(Individu individu) {
		assertTargetNotNull();
		return target.getDecompositionNomPrenom(individu);
	}

	private void assertTargetNotNull() {
		Assert.notNull(target, "Le service civil n'a pas été défini !");
	}

	@Override
	public EtatCivil getEtatCivilActif(long noIndividu, RegDate date) {
		assertTargetNotNull();
		return target.getEtatCivilActif(noIndividu, date);
	}

	@Override
	public boolean isWarmable() {
		assertTargetNotNull();
		return target.isWarmable();
	}

	@Override
	public List<HistoriqueCommune> getCommunesDomicileHisto(RegDate depuis, long noIndividu, boolean strict, boolean seulementVaud) throws DonneesCivilesException, ServiceInfrastructureException {
		assertTargetNotNull();
		return target.getCommunesDomicileHisto(depuis, noIndividu, strict, seulementVaud);
	}

	@Override
	public ServiceCivilService getTarget() {
		return target;
	}

	@Override
	public ServiceCivilService getUltimateTarget() {
		if (target instanceof ServiceCivilServiceWrapper) {
			return ((ServiceCivilServiceWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}
}
