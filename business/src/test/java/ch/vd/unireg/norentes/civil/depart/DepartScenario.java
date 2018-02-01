package ch.vd.unireg.norentes.civil.depart;

import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.type.TypeAdresseCivil;

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
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Zurich.GloriaStrasse, null, dateOuverture, null));
	}

	protected void ouvrirAdresseEnney(MockIndividu individu, RegDate dateOuverture) {
		individu.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Enney.CheminDAfflon, null, dateOuverture, null));
	}

	protected void ouvrirAdresseEtranger(MockIndividu individu, RegDate dateOuverture, Pays paysDepart) {
		final MockAdresse adresse = new MockAdresse();
		adresse.setTypeAdresse(TypeAdresseCivil.PRINCIPALE);
		adresse.setPays(paysDepart);
		adresse.setDateDebutValidite(dateOuverture);
		individu.addAdresse(adresse);
	}

}
