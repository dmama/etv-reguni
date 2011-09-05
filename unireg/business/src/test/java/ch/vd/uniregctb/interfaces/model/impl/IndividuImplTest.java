package ch.vd.uniregctb.interfaces.model.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.registre.civil.model.impl.EtatCivilImpl;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.interfaces.model.Adresse;
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
		assertNull(etatCivil.getDateDebutValidite());
		assertEquals(TypeEtatCivil.DIVORCE, etatCivil.getTypeEtatCivil());
	}

	/**
	 * [SIFISC-35] problématique des adresses civiles dans le futur
	 */
	@Test
	public void testAdresseCivileDebutDansLeFutur() throws Exception {

		final ch.vd.registre.civil.model.impl.IndividuImpl individu = new ch.vd.registre.civil.model.impl.IndividuImpl();
		individu.setNoTechnique(536824);

		final ch.vd.common.model.impl.AdresseImpl adresse = new ch.vd.common.model.impl.AdresseImpl();
		adresse.setDateDebutValidite(DateHelper.getFirstDayOfNextMonth(DateHelper.getCurrentDate()));
		individu.setAdresses(Arrays.asList(adresse));

		final IndividuImpl wrapper = new IndividuImpl(individu);
		assertEquals(0, wrapper.getAdresses().size());
	}

	/**
	 * [SIFISC-35] problématique des adresses civiles dans le futur
	 */
	@Test
	public void testAdresseCivileFinDansLeFutur() throws Exception {

		final ch.vd.registre.civil.model.impl.IndividuImpl individu = new ch.vd.registre.civil.model.impl.IndividuImpl();
		individu.setNoTechnique(536824);

		final ch.vd.common.model.impl.AdresseImpl adresse = new ch.vd.common.model.impl.AdresseImpl();
		adresse.setDateDebutValidite(DateHelper.getDate(2001, 9, 12));
		adresse.setDateFinValidite(DateHelper.getFirstDayOfNextMonth(DateHelper.getCurrentDate()));
		individu.setAdresses(Arrays.asList(adresse));

		final IndividuImpl wrapper = new IndividuImpl(individu);
		assertEquals(1, wrapper.getAdresses().size());

		final Adresse adresseWrapper = wrapper.getAdresses().iterator().next();
		assertEquals(RegDate.get(2001, 9, 12), adresseWrapper.getDateDebut());
		assertNull(adresseWrapper.getDateFin());
	}
}
