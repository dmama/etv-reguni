package ch.vd.uniregctb.interfaces.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.registre.civil.model.impl.EtatCivilImpl;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class IndividuImplTest extends WithoutSpringTest {

	/**
	 * [UNIREG-1194] teste qu'on retrouve un état-civil avec une date de début nulle
	 */
	@Test
	public void testGetEtatCivilDateDebutNulle() {

		// construit un individu qui possède un seul état civil avec une date de début nulle

		final EtatCivilImpl e = new EtatCivilImpl();
		e.setDateDebutValidite(null);
		e.setNoSequence(0);
		e.setTypeEtatCivil(EnumTypeEtatCivil.DIVORCE);

		final List<ch.vd.registre.civil.model.EtatCivil> etats = new ArrayList<ch.vd.registre.civil.model.EtatCivil>();
		etats.add(e);

		final ch.vd.registre.civil.model.impl.IndividuImpl individu = new ch.vd.registre.civil.model.impl.IndividuImpl();
		individu.setNoTechnique(536824);
		individu.setEtatsCivils(etats);

		final IndividuImpl wrapper = new IndividuImpl(individu);

		// vérifie que l'état civil est bien trouvé

		final EtatCivil etatCivil = wrapper.getEtatCivil(RegDate.get(2007,3,9));
		assertNotNull(etatCivil);
		assertEquals(0, etatCivil.getNoSequence());
		assertNull(etatCivil.getDateDebutValidite());
		assertEquals(TypeEtatCivil.DIVORCE, etatCivil.getTypeEtatCivil());
	}
}
