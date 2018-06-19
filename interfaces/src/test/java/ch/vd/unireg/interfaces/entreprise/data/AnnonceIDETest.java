package ch.vd.unireg.interfaces.entreprise.data;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.interfaces.entreprise.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;

/**
 * @author Raphaël Marmier, 2016-09-02, <raphael.marmier@vd.ch>
 */
public class AnnonceIDETest extends WithoutSpringTest {

	@Test
	public void testComparaisonAnnonceIDE() throws Exception {
		final Date dateAnnonce = DateHelper.getCurrentDate();

		// l'annonce modèle
		final AdresseAnnonceIDERCEnt adresseAnnonce1 = RCEntAnnonceIDEHelper
				.createAdresseAnnonceIDERCEnt("Longemalle", "1", null, MockLocalite.Renens.getNPA(), null, MockLocalite.Renens.getNoOrdre(), "Renens", MockPays.Suisse.getNoOfsEtatSouverain(), MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(), null,
				                              null, null);

		final AnnonceIDE annonce1 =
				RCEntAnnonceIDEHelper.createAnnonceIDE(1L, TypeAnnonce.CREATION, dateAnnonce, "Robert", null, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, null, null, null, null, null, null, null, null,
				                                       "Synergy tour", null, FormeLegale.N_0109_ASSOCIATION, "Tourisme", adresseAnnonce1, null, RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);


		// l'annonce modèle
		final AdresseAnnonceIDERCEnt adresseAnnonce2 = RCEntAnnonceIDEHelper
				.createAdresseAnnonceIDERCEnt("Longemalle", "1", null, MockLocalite.Renens.getNPA(), null, MockLocalite.Renens.getNoOrdre(), "Renens", MockPays.Suisse.getNoOfsEtatSouverain(), MockPays.Suisse.getCodeIso2(), MockPays.Suisse.getNomCourt(), null,
				                              null, null);

		final AnnonceIDE annonce2 =
				RCEntAnnonceIDEHelper.createAnnonceIDE(1L, TypeAnnonce.CREATION, dateAnnonce, "Robert", null, TypeEtablissementCivil.ETABLISSEMENT_PRINCIPAL, null, null, null, null, null, null, null, null,
				                                       "Synergy tour", null, FormeLegale.N_0109_ASSOCIATION, "Tourisme", adresseAnnonce2, null, RCEntAnnonceIDEHelper.SERVICE_IDE_UNIREG);

		Assert.assertEquals(annonce2, annonce1);
	}
}