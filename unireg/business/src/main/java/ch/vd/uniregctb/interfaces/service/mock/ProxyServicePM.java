package ch.vd.uniregctb.interfaces.service.mock;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdressesPM;
import ch.vd.uniregctb.adresse.AdressesPMHisto;
import ch.vd.uniregctb.interfaces.model.Etablissement;
import ch.vd.uniregctb.interfaces.model.EvenementPM;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;

public class ProxyServicePM implements ServicePersonneMoraleService {

	private ServicePersonneMoraleService target;

	public void setUp(ServicePersonneMoraleService target) {
		this.target = target;
	}

	@Override
	public List<Long> getAllIds() {
		assertTargetNotNull();
		return target.getAllIds();
	}

	@Override
	public PersonneMorale getPersonneMorale(Long id, PartPM... parts) {
		assertTargetNotNull();
		return target.getPersonneMorale(id, parts);
	}

	@Override
	public List<PersonneMorale> getPersonnesMorales(List<Long> ids, PartPM... parts) {
		assertTargetNotNull();
		return target.getPersonnesMorales(ids, parts);
	}

	@Override
	public Etablissement getEtablissement(long id) {
		assertTargetNotNull();
		return target.getEtablissement(id);
	}

	@Override
	public List<Etablissement> getEtablissements(List<Long> ids) {
		assertTargetNotNull();
		return target.getEtablissements(ids);
	}

	@Override
	public AdressesPM getAdresses(long noEntreprise, RegDate date) {
		assertTargetNotNull();
		return target.getAdresses(noEntreprise, date);
	}

	@Override
	public AdressesPMHisto getAdressesHisto(long noEntreprise) {
		assertTargetNotNull();
		return target.getAdressesHisto(noEntreprise);
	}

	@Override
	public List<EvenementPM> findEvenements(long numeroEntreprise, String code, RegDate minDate, RegDate maxDate) {
		assertTargetNotNull();
		return target.findEvenements(numeroEntreprise, code, minDate, maxDate);
	}

	private void assertTargetNotNull() {
		Assert.notNull(target, "Le service PM n'a pas été défini !");
	}
}
