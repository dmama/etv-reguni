package ch.vd.uniregctb.norentes.civil.depart;

import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.type.TypeAdresseCivil;

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
			if (adresse.getDateFin() == null) {
				adresse.setDateFinValidite(dateFermeture);
			}
		}
	}

	protected void ouvrirAdresseZurich(MockIndividu individu, RegDate dateOuverture) {
		final Collection<Adresse> adrs = individu.getAdresses();
		final Adresse aa = MockServiceCivil.newAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, dateOuverture, null);
		adrs.add(aa);
	}



	protected void ouvrirAdresseEnney(MockIndividu individu, RegDate dateOuverture) {
		final Collection<Adresse> adrs = individu.getAdresses();
		final Adresse aa = MockServiceCivil.newAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Enney.chemin, null, dateOuverture, null);
		adrs.add(aa);
	}

	protected void ouvrirAdresseEtranger(MockIndividu individu, RegDate dateOuverture, Pays paysDepart) {
		final Collection<Adresse> adrs = individu.getAdresses();

		final MockAdresse adresse = new MockAdresse();
		adresse.setTypeAdresse(TypeAdresseCivil.PRINCIPALE);
		adresse.setPays(paysDepart);
		adresse.setDateDebutValidite(dateOuverture);

		adrs.add(adresse);
	}

}
