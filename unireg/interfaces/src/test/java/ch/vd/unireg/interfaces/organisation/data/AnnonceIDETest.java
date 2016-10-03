package ch.vd.unireg.interfaces.organisation.data;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.uniregctb.common.WithoutSpringTest;

/**
 * @author Raphaël Marmier, 2016-09-02, <raphael.marmier@vd.ch>
 */
public class AnnonceIDETest extends WithoutSpringTest {

	@Test
	public void testComparaisonAnnonceIDE() throws Exception {
		final Date dateAnnonce = DateHelper.getCurrentDate();

		// l'annonce modèle
		final AdresseAnnonceIDERCEnt adresseAnnonce1 = RCEntAnnonceIDEHelper
				.createAdresseAnnonceIDERCEnt("Longemalle", "1", null, 1020, "Renens", MockPays.Suisse.getNoOfsEtatSouverain(), MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(), null,
				                              null, null);

		final AnnonceIDERCEnt annonce1 =
				RCEntAnnonceIDEHelper.createAnnonceIDERCEnt(1L, TypeAnnonce.CREATION, dateAnnonce, "Robert", null, TypeDeSite.ETABLISSEMENT_PRINCIPAL, null, null, null, null, null, null, null, null,
				                                            "Synergy tour", null, FormeLegale.N_0109_ASSOCIATION, "Tourisme", adresseAnnonce1);


		// l'annonce modèle
		final AdresseAnnonceIDERCEnt adresseAnnonce2 = RCEntAnnonceIDEHelper
				.createAdresseAnnonceIDERCEnt("Longemalle", "1", null, 1020, "Renens", MockPays.Suisse.getNoOfsEtatSouverain(), MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(), null,
				                              null, null);

		final AnnonceIDERCEnt annonce2 =
				RCEntAnnonceIDEHelper.createAnnonceIDERCEnt(1L, TypeAnnonce.CREATION, dateAnnonce, "Robert", null, TypeDeSite.ETABLISSEMENT_PRINCIPAL, null, null, null, null, null, null, null, null,
				                                            "Synergy tour", null, FormeLegale.N_0109_ASSOCIATION, "Tourisme", adresseAnnonce2);

		Assert.assertEquals(annonce2, annonce1);
	}
}