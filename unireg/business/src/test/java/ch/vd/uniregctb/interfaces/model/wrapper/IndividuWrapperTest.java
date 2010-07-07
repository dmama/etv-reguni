package ch.vd.uniregctb.interfaces.model.wrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.registre.civil.model.impl.EtatCivilImpl;
import ch.vd.registre.civil.model.impl.IndividuImpl;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.interfaces.model.EtatCivil;

public class IndividuWrapperTest extends WithoutSpringTest {

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

		final IndividuImpl individu = new IndividuImpl();
		individu.setNoTechnique(536824);
		individu.setEtatsCivils(etats);

		final IndividuWrapper wrapper = new IndividuWrapper(individu);

		// vérifie que l'état civil est bien trouvé

		final EtatCivil etatCivil = wrapper.getEtatCivil(RegDate.get(2007,3,9));
		assertNotNull(etatCivil);
		assertEquals(0, etatCivil.getNoSequence());
		assertNull(etatCivil.getDateDebutValidite());
		assertEquals(EnumTypeEtatCivil.DIVORCE, etatCivil.getTypeEtatCivil());
	}
}
