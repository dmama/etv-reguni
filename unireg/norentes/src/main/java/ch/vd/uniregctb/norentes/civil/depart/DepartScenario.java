package ch.vd.uniregctb.norentes.civil.depart;

import java.util.Collection;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;

public abstract class DepartScenario extends EvenementCivilScenario {

	public DepartScenario() {
		super();
	}

	protected void fermerAdresse(MockIndividu ind, RegDate dateFermeture) {
		final Collection<Adresse> adrs = ind.getAdresses();
		MockAdresse last = null;
		for (Adresse a : adrs) {
			last = (MockAdresse) a;
		}
		last.setDateFinValidite(dateFermeture);
	
	}

	protected void fermerAdresses(MockIndividu ind, RegDate dateFermeture) {
		for (Adresse adr : ind.getAdresses()) {
			MockAdresse adresse = (MockAdresse) adr;
			if (adresse.getDateFinValidite() == null) {
				adresse.setDateFinValidite(dateFermeture);
			}
		}
	}
	
	protected void ouvrirAdresseZurich(MockIndividu individu, RegDate dateOuverture) {
		final Collection<Adresse> adrs = individu.getAdresses();
		final Adresse aa = MockServiceCivil.newAdresse(EnumTypeAdresse.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, MockLocalite.Zurich, dateOuverture, null);
		adrs.add(aa);
	}
	
	protected void ouvrirAdresseEtranger(MockIndividu individu, RegDate dateOuverture, Pays paysDepart) {
		final Collection<Adresse> adrs = individu.getAdresses();
		
		final MockAdresse adresse = new MockAdresse();
		adresse.setTypeAdresse(EnumTypeAdresse.PRINCIPALE);
		adresse.setPays(paysDepart);
		adresse.setDateDebutValidite(dateOuverture);
		
		adrs.add(adresse);
	}

}