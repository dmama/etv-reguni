package ch.vd.uniregctb.interfaces.service.mock;

import java.util.Collection;
import java.util.List;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.HistoriqueCommune;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesActives;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Origine;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.interfaces.model.Tutelle;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

/**
 * Proxy du service civil à enregistrer dans l'application context et permettant à chaque test unitaire de spécifier précisemment l'instance
 * du service civil à utiliser.
 */
public class ProxyServiceCivil implements ServiceCivilService {

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

	public AdressesCivilesActives getAdresses(long noIndividu, RegDate date, boolean strict) throws DonneesCivilesException {
		assertTargetNotNull();
		return target.getAdresses(noIndividu, date, strict);
	}

	public AdressesCivilesHistoriques getAdressesHisto(long noIndividu, boolean strict) throws DonneesCivilesException {
		assertTargetNotNull();
		return target.getAdressesHisto(noIndividu, strict);
	}

	public Collection<Adresse> getAdresses(long noTechniqueIndividu, int anneeValidite) {
		assertTargetNotNull();
		return target.getAdresses(noTechniqueIndividu, anneeValidite);
	}

	public Individu getIndividu(long noTechniqueIndividu, int anneeValidite, AttributeIndividu... attributs) {
		assertTargetNotNull();
		return target.getIndividu(noTechniqueIndividu, anneeValidite, attributs);
	}

	public Individu getIndividu(long noTechniqueIndividu, int anneeValidite) {
		assertTargetNotNull();
		return target.getIndividu(noTechniqueIndividu, anneeValidite);
	}

	public Individu getIndividu(long noIndividu, RegDate date, AttributeIndividu... parties) {
		assertTargetNotNull();
		return target.getIndividu(noIndividu, date, parties);
	}

	public Individu getConjoint(Long noIndividuPrincipal, RegDate date) {
		assertTargetNotNull();
		return target.getConjoint(noIndividuPrincipal,date);
	}

	@SuppressWarnings("unchecked")
	public Long getNumeroIndividuConjoint(Long noIndividuPrincipal, RegDate date) {
		assertTargetNotNull();
		return target.getNumeroIndividuConjoint(noIndividuPrincipal,date);  
	}

	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, AttributeIndividu... parties) {
		assertTargetNotNull();
		return target.getIndividus(nosIndividus, date, parties);
	}

	public List<Individu> getIndividus(Collection<Long> nosIndividus, int annee, AttributeIndividu... parties) {
		assertTargetNotNull();
		return target.getIndividus(nosIndividus, annee, parties);
	}

	public Origine getOrigine(long noTechniqueIndividu, int anneeValidite) {
		assertTargetNotNull();
		return target.getOrigine(noTechniqueIndividu, anneeValidite);
	}

	public Collection<Permis> getPermis(long noTechniqueIndividu, int anneeValidite) {
		assertTargetNotNull();
		return target.getPermis(noTechniqueIndividu, anneeValidite);
	}

	public Tutelle getTutelle(long noTechniqueIndividu, int anneeValidite) {
		assertTargetNotNull();
		return target.getTutelle(noTechniqueIndividu, anneeValidite);
	}

	@SuppressWarnings("unchecked")
	public Collection getNationalites(long noTechniqueIndividu, int anneeValidite) {
		assertTargetNotNull();
		return target.getNationalites(noTechniqueIndividu, anneeValidite);
	}

	public String getNomPrenom(Individu individu) {
		assertTargetNotNull();
		return target.getNomPrenom(individu);
	}

	public NomPrenom getDecompositionNomPrenom(Individu individu) {
		assertTargetNotNull();
		return target.getDecompositionNomPrenom(individu);
	}

	private void assertTargetNotNull() {
		Assert.notNull(target, "Le service civil n'a pas été défini !");
	}

	public EtatCivil getEtatCivilActif(long noIndividu, RegDate date) {
		assertTargetNotNull();
		return target.getEtatCivilActif(noIndividu, date);
	}

	public Permis getPermisActif(long noIndividu, RegDate date) {
		assertTargetNotNull();
		return target.getPermisActif(noIndividu, date);
	}

	public boolean isWarmable() {
		assertTargetNotNull();
		return target.isWarmable();
	}

	public List<HistoriqueCommune> getCommunesDomicileHisto(RegDate depuis, long noIndividu, boolean strict, boolean seulementVaud) throws DonneesCivilesException, InfrastructureException {
		return target.getCommunesDomicileHisto(depuis, noIndividu, strict, seulementVaud);
	}

	public ServiceCivilService getTarget() {
		return target;
	}
}
