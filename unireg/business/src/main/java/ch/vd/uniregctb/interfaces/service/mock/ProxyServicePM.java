package ch.vd.uniregctb.interfaces.service.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdressesPM;
import ch.vd.uniregctb.adresse.AdressesPMHisto;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;

public class ProxyServicePM implements ServicePersonneMoraleService {

	private ServicePersonneMoraleService target;

	public void setUp(ServicePersonneMoraleService target) {
		Assert.isNull(this.target);
		this.target = target;
	}

	public void tearDown() {
		this.target = null;
	}

	public AdressesPM getAdresses(long noEntreprise, RegDate date) {
		assertTargetNotNull();
		return target.getAdresses(noEntreprise, date);
	}

	public AdressesPMHisto getAdressesHisto(long noEntreprise) {
		assertTargetNotNull();
		return target.getAdressesHisto(noEntreprise);
	}

	public PersonneMorale getPersonneMorale(Long id) {
		assertTargetNotNull();
		return target.getPersonneMorale(id);
	}

	private void assertTargetNotNull() {
		Assert.notNull(target, "Le service PM n'a pas été défini !");
	}
}
