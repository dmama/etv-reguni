package ch.vd.unireg.registrefoncier.communaute;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.MockRegistreFoncierService;
import ch.vd.unireg.registrefoncier.RegroupementCommunauteRF;

import static ch.vd.unireg.common.WithoutSpringTest.date;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class RegroupementRFViewTest {

	/**
	 * [IMM-1271] Ce test vérifie qu'une communauté avec plusieurs droits qui pointent vers le même immeuble peut bien s'afficher.
	 */
	@Test
	public void testCommunauteAvecPlusieursDroitsEtUnSeulImmeuble() {

		final MockRegistreFoncierService registreFoncierService = new MockRegistreFoncierService() {
			@Override
			public String getNumeroParcelleComplet(ImmeubleRF immeuble, RegDate dateReference) {
				return "28";
			}

			@Override
			public Commune getCommune(ImmeubleRF immeuble, RegDate dateReference) {
				return MockCommune.Aigle;
			}
		};

		// on crée une communauté avec deux droits qui pointent vers le même immeuble
		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.setId(232212L);

		final DroitProprieteCommunauteRF droit1 = new DroitProprieteCommunauteRF();
		droit1.setDateDebutMetier(date(2000, 1, 1));
		droit1.setDateFin(date(2002, 4, 13));
		droit1.setImmeuble(immeuble);

		final DroitProprieteCommunauteRF droit2 = new DroitProprieteCommunauteRF();
		droit2.setDateDebutMetier(date(2002, 4, 13));
		droit2.setDateFin(date(2004, 12, 31));
		droit2.setImmeuble(immeuble);

		final CommunauteRF comunaute = new CommunauteRF();
		comunaute.setId(8222L);
		comunaute.addDroitPropriete(droit1);
		comunaute.addDroitPropriete(droit2);

		final RegroupementCommunauteRF regroupement = new RegroupementCommunauteRF();
		regroupement.setDateDebut(date(2000, 1, 1));
		regroupement.setDateFin(date(2004, 12, 31));
		regroupement.setCommunaute(comunaute);

		// on créé la vue de ce regroupement (qui devrait être valide)
		final RegroupementRFView view = new RegroupementRFView(regroupement, registreFoncierService);
		assertNotNull(view);
		assertEquals(date(2000, 1, 1), view.getDateDebut());
		assertEquals(date(2004, 12, 31), view.getDateFin());
		assertEquals(8222L, view.getCommunauteId());
	}

	/**
	 * [IMM-1271] Ce test vérifie qu'une communauté avec plusieurs droits qui pointent vers des immeubles différents n'est pas valide.
	 */
	@Test
	public void testCommunauteAvecPlusieursDroitsEtPlsuieursImmeubles() {

		final MockRegistreFoncierService registreFoncierService = new MockRegistreFoncierService() {
			@Override
			public String getNumeroParcelleComplet(ImmeubleRF immeuble, RegDate dateReference) {
				return "28";
			}

			@Override
			public Commune getCommune(ImmeubleRF immeuble, RegDate dateReference) {
				return MockCommune.Aigle;
			}
		};

		// on crée une communauté avec deux droits qui pointent vers des immeubles différents
		final ImmeubleRF immeuble1 = new BienFondsRF();
		immeuble1.setId(232212L);

		final ImmeubleRF immeuble2 = new BienFondsRF();
		immeuble2.setId(20493L);

		final DroitProprieteCommunauteRF droit1 = new DroitProprieteCommunauteRF();
		droit1.setDateDebutMetier(date(2000, 1, 1));
		droit1.setDateFin(date(2002, 4, 13));
		droit1.setImmeuble(immeuble1);

		final DroitProprieteCommunauteRF droit2 = new DroitProprieteCommunauteRF();
		droit2.setDateDebutMetier(date(2002, 4, 13));
		droit2.setDateFin(date(2004, 12, 31));
		droit2.setImmeuble(immeuble2);

		final CommunauteRF comunaute = new CommunauteRF();
		comunaute.setId(8222L);
		comunaute.addDroitPropriete(droit1);
		comunaute.addDroitPropriete(droit2);

		final RegroupementCommunauteRF regroupement = new RegroupementCommunauteRF();
		regroupement.setDateDebut(date(2000, 1, 1));
		regroupement.setDateFin(date(2004, 12, 31));
		regroupement.setCommunaute(comunaute);

		// on créé la vue de ce regroupement (qui devrait être valide)
		try {
			final RegroupementRFView view = new RegroupementRFView(regroupement, registreFoncierService);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("La communauté n°8222 possède plusieurs immeubles (incohérence des données)", e.getMessage());
		}
	}
}