package ch.vd.uniregctb.role.before2016;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.metier.assujettissement.MotifAssujettissement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ProduireRolesResultsTest extends BusinessTest {

	private final RegDate dateTraitement = RegDate.get();
	private final int anneeRoles = dateTraitement.year() - 1;

	private AdresseService adresseService;
	private ProduireRolesOIDsResults results;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		adresseService = getBean(AdresseService.class, "adresseService");
		results = new ProduireRolesOIDsResults(anneeRoles, 1, dateTraitement, tiersService, adresseService);
	}

	private PersonnePhysique addNonHabitantAvecAdresseCourierALausanne() {
		final PersonnePhysique pp = addNonHabitant("Marie", "Tatouille", date(1970, 4, 1), Sexe.FEMININ);
		addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(1990, 7, 1), null, MockRue.Lausanne.AvenueDeBeaulieu);
		return pp;
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRegrouppementCommunes() throws Exception {

		// ctb a un immeuble à Renens jusqu'au 1 juin
		// ctb a une activité indépendante à Cheseaux depuis le 15 octobre

		final Contribuable ctb = addNonHabitantAvecAdresseCourierALausanne();
		{
			final ForFiscalSecondaire ffsRenens = new ForFiscalSecondaire(date(1990, 7, 1), MotifFor.ACHAT_IMMOBILIER, date(anneeRoles, 6, 1), MotifFor.VENTE_IMMOBILIER, MockCommune.Renens.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE);
			final InfoFor infoForRenens = new InfoFor(InfoContribuable.TypeContribuable.HORS_CANTON,
			                                          ffsRenens.getDateDebut(), MotifAssujettissement.of(ffsRenens.getMotifOuverture()), ffsRenens.getDateFin(), MotifAssujettissement.of(ffsRenens.getMotifFermeture()), InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, ffsRenens);
			results.digestInfoFor(infoForRenens, ctb, null, date(anneeRoles - 1, 12, 31), anneeRoles, MockCommune.Renens.getNoOFS(), adresseService, tiersService);

			final ForFiscalSecondaire ffsCheseaux = new ForFiscalSecondaire(date(anneeRoles, 10, 15), MotifFor.DEBUT_EXPLOITATION, null, null, MockCommune.CheseauxSurLausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.ACTIVITE_INDEPENDANTE);
			final InfoFor infoForCheseaux = new InfoFor(InfoContribuable.TypeContribuable.HORS_CANTON,
			                                            ffsCheseaux.getDateDebut(), MotifAssujettissement.of(ffsCheseaux.getMotifOuverture()), ffsCheseaux.getDateFin(), MotifAssujettissement.of(ffsCheseaux.getMotifFermeture()), InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, ffsCheseaux);
			results.digestInfoFor(infoForCheseaux, ctb, null, date(anneeRoles - 1, 12, 31), anneeRoles, MockCommune.CheseauxSurLausanne.getNoOFS(), adresseService, tiersService);

			Assert.assertTrue(results.getNoOfsCommunesTraitees().contains(MockCommune.Renens.getNoOFS()));
			Assert.assertTrue(results.getNoOfsCommunesTraitees().contains(MockCommune.CheseauxSurLausanne.getNoOFS()));
		}

		// Renens-Cossonay -> ne doit prendre en compte que le for de Renens
		{
			final Collection<InfoContribuablePP> infos = results.buildInfoPourRegroupementCommunes(Arrays.asList(MockCommune.Renens.getNoOFS(), MockCommune.Cossonay.getNoOFS()));
			Assert.assertNotNull(infos);
			Assert.assertEquals(1, infos.size());

			final InfoContribuablePP infoCtb = infos.iterator().next();
			Assert.assertNotNull(infoCtb);
			Assert.assertEquals((long) ctb.getNumero(), infoCtb.noCtb);
			Assert.assertEquals(InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, infoCtb.getTypeAssujettissementAgrege());
			Assert.assertEquals(InfoContribuable.TypeContribuable.HORS_CANTON, infoCtb.getTypeCtb());

			final Pair<RegDate, MotifAssujettissement> infoOuverture = infoCtb.getInfosOuverture();
			Assert.assertNotNull(infoOuverture);
			Assert.assertEquals(date(1990, 7, 1), infoOuverture.getLeft());
			Assert.assertEquals(MotifAssujettissement.ACHAT_IMMOBILIER, infoOuverture.getRight());

			final Pair<RegDate, MotifAssujettissement> infoFermeture = infoCtb.getInfosFermeture();
			Assert.assertNotNull(infoFermeture);
			Assert.assertEquals(date(anneeRoles, 6, 1), infoFermeture.getLeft());
			Assert.assertEquals(MotifAssujettissement.VENTE_IMMOBILIER, infoFermeture.getRight());
		}

		// Renens-Cheseaux -> doit prendre tous les fors ajoutés
		{
			final Collection<InfoContribuablePP> infos = results.buildInfoPourRegroupementCommunes(Arrays.asList(MockCommune.Renens.getNoOFS(), MockCommune.CheseauxSurLausanne.getNoOFS()));
			Assert.assertNotNull(infos);
			Assert.assertEquals(1, infos.size());

			final InfoContribuablePP infoCtb = infos.iterator().next();
			Assert.assertNotNull(infoCtb);
			Assert.assertEquals((long) ctb.getNumero(), infoCtb.noCtb);
			Assert.assertEquals(InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, infoCtb.getTypeAssujettissementAgrege());
			Assert.assertEquals(InfoContribuable.TypeContribuable.HORS_CANTON, infoCtb.getTypeCtb());

			final Pair<RegDate, MotifAssujettissement> infoOuverture = infoCtb.getInfosOuverture();
			Assert.assertNotNull(infoOuverture);
			Assert.assertEquals(date(1990, 7, 1), infoOuverture.getLeft());
			Assert.assertEquals(MotifAssujettissement.ACHAT_IMMOBILIER, infoOuverture.getRight());

			final Pair<RegDate, MotifAssujettissement> infoFermeture = infoCtb.getInfosFermeture();
			Assert.assertNull(infoFermeture);
		}

		// Cheseaux-Renens-Aubonne -> doit prendre tous les fors ajoutés
		{
			final Collection<InfoContribuablePP> infos = results.buildInfoPourRegroupementCommunes(Arrays.asList(MockCommune.CheseauxSurLausanne.getNoOFS(), MockCommune.Renens.getNoOFS(), MockCommune.Aubonne.getNoOFS()));
			Assert.assertNotNull(infos);
			Assert.assertEquals(1, infos.size());

			final InfoContribuablePP infoCtb = infos.iterator().next();
			Assert.assertNotNull(infoCtb);
			Assert.assertEquals((long) ctb.getNumero(), infoCtb.noCtb);
			Assert.assertEquals(InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, infoCtb.getTypeAssujettissementAgrege());
			Assert.assertEquals(InfoContribuable.TypeContribuable.HORS_CANTON, infoCtb.getTypeCtb());

			final Pair<RegDate, MotifAssujettissement> infoOuverture = infoCtb.getInfosOuverture();
			Assert.assertNotNull(infoOuverture);
			Assert.assertEquals(date(1990, 7, 1), infoOuverture.getLeft());
			Assert.assertEquals(MotifAssujettissement.ACHAT_IMMOBILIER, infoOuverture.getRight());

			final Pair<RegDate, MotifAssujettissement> infoFermeture = infoCtb.getInfosFermeture();
			Assert.assertNull(infoFermeture);
		}

		// Cheseaux-Lausanne -> seulement le for de Cheseaux
		{
			final Collection<InfoContribuablePP> infos = results.buildInfoPourRegroupementCommunes(Arrays.asList(MockCommune.CheseauxSurLausanne.getNoOFS(), MockCommune.Lausanne.getNoOFS()));
			Assert.assertNotNull(infos);
			Assert.assertEquals(1, infos.size());

			final InfoContribuablePP infoCtb = infos.iterator().next();
			Assert.assertNotNull(infoCtb);
			Assert.assertEquals((long) ctb.getNumero(), infoCtb.noCtb);
			Assert.assertEquals(InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, infoCtb.getTypeAssujettissementAgrege());
			Assert.assertEquals(InfoContribuable.TypeContribuable.HORS_CANTON, infoCtb.getTypeCtb());

			final Pair<RegDate, MotifAssujettissement> infoOuverture = infoCtb.getInfosOuverture();
			Assert.assertNotNull(infoOuverture);
			Assert.assertEquals(date(anneeRoles, 10, 15), infoOuverture.getLeft());
			Assert.assertEquals(MotifAssujettissement.DEBUT_EXPLOITATION, infoOuverture.getRight());

			final Pair<RegDate, MotifAssujettissement> infoFermeture = infoCtb.getInfosFermeture();
			Assert.assertNull(infoFermeture);
		}
	}
}
