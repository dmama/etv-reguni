package ch.vd.uniregctb.evenement.civil.engine.ech;

import junit.framework.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.DataHolder;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;
import ch.vd.uniregctb.type.TypePermis;

public class PermisComparisonStrategyTest extends BusinessTest {

	private PermisComparisonStrategy strategy;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		strategy = new PermisComparisonStrategy();
	}

	private void setupCivil(final long noIndividu, final long noEvt1, final DateRange range1, final TypePermis type1,
	                        final long noEvt2, final DateRange range2, final TypePermis type2) {
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, null, "Leblanc", "Juste", true);
				if (type1 != null) {
					addPermis(individu, type1, range1.getDateDebut(), range1.getDateFin(), false);
				}
				addIndividuFromEvent(noEvt1, individu, RegDate.get(), TypeEvenementCivilEch.ARRIVEE);

				final MockIndividu individuCorrige = createIndividu(noIndividu, null, "Leblenc", "Justin", true);
				if (type2!= null) {
					addPermis(individuCorrige, type2, range2.getDateDebut(), range2.getDateFin(), false);
				}
				addIndividuFromEvent(noEvt2, individuCorrige, RegDate.get(), TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, noEvt2);
			}
		});
	}

	@Test
	public void testMemePermis() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypePermis type = TypePermis.ANNUEL;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), type, noEvt2, new DateRangeHelper.Range(debut, fin), type);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean neutre = strategy.isFiscalementNeutre(iae1, iae2, dh);
		Assert.assertTrue(neutre);
		Assert.assertNull(dh.get());
	}

	@Test
	public void testSansPermis() throws Exception {

		final long noIndividu = 367315L;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, null, null, noEvt2, null, null);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean neutre = strategy.isFiscalementNeutre(iae1, iae2, dh);
		Assert.assertTrue(neutre);
		Assert.assertNull(dh.get());
	}

	@Test
	public void testTypesDifferentsDontC() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypePermis type1 = TypePermis.DIPLOMATE;
		final TypePermis type2 = TypePermis.ETABLISSEMENT;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), type1, noEvt2, new DateRangeHelper.Range(debut, fin), type2);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean neutre = strategy.isFiscalementNeutre(iae1, iae2, dh);
		Assert.assertFalse(neutre);
		Assert.assertEquals("permis", dh.get());
	}

	@Test
	public void testTypesDifferentsSansC() throws Exception {
		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 1, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypePermis type1 = TypePermis.COURTE_DUREE;
		final TypePermis type2 = TypePermis.ANNUEL;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), type1, noEvt2, new DateRangeHelper.Range(debut, fin), type2);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean neutre = strategy.isFiscalementNeutre(iae1, iae2, dh);
		Assert.assertTrue(neutre);
		Assert.assertNull(dh.get());
	}

	@Test
	public void testDatesDebutDifferentesPermisC() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut1 = date(2000, 1, 1);
		final RegDate debut2 = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypePermis type = TypePermis.ETABLISSEMENT;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut1, fin), type, noEvt2, new DateRangeHelper.Range(debut2, fin), type);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean neutre = strategy.isFiscalementNeutre(iae1, iae2, dh);
		Assert.assertFalse(neutre);
		Assert.assertEquals("permis", dh.get());
	}

	@Test
	public void testDatesDebutDifferentesPermisNonC() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut1 = date(2000, 1, 1);
		final RegDate debut2 = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypePermis type = TypePermis.SAISONNIER;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut1, fin), type, noEvt2, new DateRangeHelper.Range(debut2, fin), type);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean neutre = strategy.isFiscalementNeutre(iae1, iae2, dh);
		Assert.assertTrue(neutre);
		Assert.assertNull(dh.get());
	}

	@Test
	public void testDatesFinDifferentesPermisC() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 2, 1);
		final RegDate fin1 = date(2012, 8, 3);
		final RegDate fin2 = null;
		final TypePermis type = TypePermis.ETABLISSEMENT;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin1), type, noEvt2, new DateRangeHelper.Range(debut, fin2), type);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean neutre = strategy.isFiscalementNeutre(iae1, iae2, dh);
		Assert.assertFalse(neutre);
		Assert.assertEquals("permis", dh.get());
	}

	@Test
	public void testDatesFinDifferentesPermisNonC() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 2, 1);
		final RegDate fin1 = date(2012, 8, 3);
		final RegDate fin2 = null;
		final TypePermis type = TypePermis.ETRANGER_ADMIS_PROVISOIREMENT;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin1), type, noEvt2, new DateRangeHelper.Range(debut, fin2), type);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean neutre = strategy.isFiscalementNeutre(iae1, iae2, dh);
		Assert.assertTrue(neutre);
		Assert.assertNull(dh.get());
	}

	@Test
	public void testApparition() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypePermis type = TypePermis.DIPLOMATE;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, null, null, noEvt2, new DateRangeHelper.Range(debut, fin), type);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean neutre = strategy.isFiscalementNeutre(iae1, iae2, dh);
		Assert.assertFalse(neutre);
		Assert.assertEquals("permis", dh.get());
	}

	@Test
	public void testDisparition() throws Exception {

		final long noIndividu = 367315L;
		final RegDate debut = date(2000, 2, 1);
		final RegDate fin = date(2012, 8, 3);
		final TypePermis type = TypePermis.COURTE_DUREE;
		final long noEvt1 = 4326784234L;
		final long noEvt2 = 54378436574L;

		setupCivil(noIndividu, noEvt1, new DateRangeHelper.Range(debut, fin), type, noEvt2, null, null);

		final IndividuApresEvenement iae1 = serviceCivil.getIndividuFromEvent(noEvt1);
		Assert.assertNotNull(iae1);

		final IndividuApresEvenement iae2 = serviceCivil.getIndividuFromEvent(noEvt2);
		Assert.assertNotNull(iae1);

		final DataHolder<String> dh = new DataHolder<String>();
		final boolean neutre = strategy.isFiscalementNeutre(iae1, iae2, dh);
		Assert.assertFalse(neutre);
		Assert.assertEquals("permis", dh.get());
	}
}
