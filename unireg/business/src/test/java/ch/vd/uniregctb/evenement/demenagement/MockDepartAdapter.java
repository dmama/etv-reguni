package ch.vd.uniregctb.evenement.demenagement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.depart.DepartAdapter;
import ch.vd.uniregctb.evenement.depart.DepartHandler;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.model.Individu;

public class MockDepartAdapter extends DepartAdapter {

	private Adresse ancienneAdressePrincipale;
	private CommuneSimple nouvelleCommunePrincipale;
	private CommuneSimple ancienneCommunePrincipale;
	private Adresse ancienneAdresseCourrier;
	private Individu individu;
	private RegDate date;

	protected MockDepartAdapter(EvenementCivilData evenement, EvenementCivilContext context, DepartHandler handler) throws EvenementAdapterException {
		super(evenement, context, handler);
	}

	@Override
	public RegDate getDate() {
		return date;
	}

	public void setDate(RegDate date) {
		this.date = date;
	}

	@Override
	public Individu getIndividu() {
		return individu;
	}

	public void setIndividu(Individu individu) {
		this.individu = individu;
	}

	@Override
	public Adresse getNouvelleAdressePrincipale() {
		return getAdressePrincipale();
	}

	@Override
	public CommuneSimple getNouvelleCommunePrincipale() {
		return nouvelleCommunePrincipale;
	}

	@Override
	public Adresse getNouvelleAdresseCourrier() {
		return getAdresseCourrier();
	}

	@Override
	public Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}

	public CommuneSimple getAncienneCommunePrincipale() {
		return ancienneCommunePrincipale;
	}
	
	public Adresse getAncienneAdresseCourrier() {
		return ancienneAdresseCourrier;
	}
	
	public void setAncienneCommunePrincipale(CommuneSimple amcienneCommunePrincipale) {
		this.ancienneCommunePrincipale = amcienneCommunePrincipale;
	}

	public void setNouvelleCommunePrincipale(CommuneSimple nouvelleCommunePrincipale) {
		this.nouvelleCommunePrincipale = nouvelleCommunePrincipale;
	}

	public void setAncienneAdressePrincipale(Adresse ancienneAdressePrincipale) {
		this.ancienneAdressePrincipale = ancienneAdressePrincipale;
	}
	
	public void setAncienneAdresseCourrier(Adresse ancienneAdresseCourrier) {
		this.ancienneAdresseCourrier = ancienneAdresseCourrier;
	}
}
