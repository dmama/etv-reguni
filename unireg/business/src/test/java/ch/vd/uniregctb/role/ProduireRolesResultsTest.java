package ch.vd.uniregctb.role;

import java.util.Arrays;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public class ProduireRolesResultsTest extends BusinessTest {

	private final RegDate dateTraitement = RegDate.get();
	private final int anneeRoles = dateTraitement.year() - 1;

	private AdresseService adresseService;
	private ProduireRolesResults results;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		adresseService = getBean(AdresseService.class, "adresseService");

		results = new ProduireRolesResults(anneeRoles, 1, dateTraitement, tiersService, adresseService) {};
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
			final ProduireRolesResults.InfoCommune infoRenens = results.getOrCreateInfoPourCommune(MockCommune.Renens.getNoOFS());
			final ProduireRolesResults.InfoContribuable infoCtbRenens = infoRenens.getOrCreateInfoPourContribuable(ctb, anneeRoles, adresseService, tiersService);
			infoCtbRenens.addFor(new ProduireRolesResults.InfoFor(ProduireRolesResults.InfoContribuable.TypeContribuable.HORS_CANTON,
					date(1990, 7, 1), MotifFor.ACHAT_IMMOBILIER, date(anneeRoles, 6, 1), MotifFor.VENTE_IMMOBILIER, ProduireRolesResults.InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF,
					false, MotifRattachement.IMMEUBLE_PRIVE, MockCommune.Renens.getNoOFS()));

			final ProduireRolesResults.InfoCommune infoPrilly = results.getOrCreateInfoPourCommune(MockCommune.CheseauxSurLausanne.getNoOFS());
			final ProduireRolesResults.InfoContribuable infoCtbCheseaux = infoPrilly.getOrCreateInfoPourContribuable(ctb, anneeRoles, adresseService, tiersService);
			infoCtbCheseaux.addFor(new ProduireRolesResults.InfoFor(ProduireRolesResults.InfoContribuable.TypeContribuable.HORS_CANTON,
					date(anneeRoles, 10, 15), MotifFor.DEBUT_EXPLOITATION, null, null, ProduireRolesResults.InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF,
					false, MotifRattachement.ACTIVITE_INDEPENDANTE, MockCommune.CheseauxSurLausanne.getNoOFS()));

			Assert.assertNotNull(results.getInfoPourCommune(MockCommune.Renens.getNoOFS()));
			Assert.assertNotNull(results.getInfoPourCommune(MockCommune.CheseauxSurLausanne.getNoOFS()));
		}

		// Renens-Cossonay -> ne doit prendre en compte que le for de Renens
		{
			final Map<Long, ProduireRolesResults.InfoContribuable> map = results.buildInfosPourRegroupementCommunes(Arrays.asList(MockCommune.Renens.getNoOFS(), MockCommune.Cossonay.getNoOFS()));
			Assert.assertNotNull(map);
			Assert.assertEquals(1, map.size());

			final ProduireRolesResults.InfoContribuable infoCtb = map.get(ctb.getNumero());
			Assert.assertNotNull(infoCtb);
			Assert.assertEquals((long) ctb.getNumero(), infoCtb.noCtb);
			Assert.assertEquals(ProduireRolesResults.InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF, infoCtb.getTypeAssujettissementAgrege());
			Assert.assertEquals(ProduireRolesResults.InfoContribuable.TypeContribuable.HORS_CANTON, infoCtb.getTypeCtb());

			final Pair<RegDate, MotifFor> infoOuverture = infoCtb.getInfosOuverture();
			Assert.assertNotNull(infoOuverture);
			Assert.assertEquals(date(1990, 7, 1), infoOuverture.getFirst());
			Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, infoOuverture.getSecond());

			final Pair<RegDate, MotifFor> infoFermeture = infoCtb.getInfosFermeture();
			Assert.assertNotNull(infoFermeture);
			Assert.assertEquals(date(anneeRoles, 6, 1), infoFermeture.getFirst());
			Assert.assertEquals(MotifFor.VENTE_IMMOBILIER, infoFermeture.getSecond());
		}

		// Renens-Cheseaux -> doit prendre tous les fors ajoutés
		{
			final Map<Long, ProduireRolesResults.InfoContribuable> map = results.buildInfosPourRegroupementCommunes(Arrays.asList(MockCommune.Renens.getNoOFS(), MockCommune.CheseauxSurLausanne.getNoOFS()));
			Assert.assertNotNull(map);
			Assert.assertEquals(1, map.size());

			final ProduireRolesResults.InfoContribuable infoCtb = map.get(ctb.getNumero());
			Assert.assertNotNull(infoCtb);
			Assert.assertEquals((long) ctb.getNumero(), infoCtb.noCtb);
			Assert.assertEquals(ProduireRolesResults.InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, infoCtb.getTypeAssujettissementAgrege());
			Assert.assertEquals(ProduireRolesResults.InfoContribuable.TypeContribuable.HORS_CANTON, infoCtb.getTypeCtb());

			final Pair<RegDate, MotifFor> infoOuverture = infoCtb.getInfosOuverture();
			Assert.assertNotNull(infoOuverture);
			Assert.assertEquals(date(1990, 7, 1), infoOuverture.getFirst());
			Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, infoOuverture.getSecond());

			final Pair<RegDate, MotifFor> infoFermeture = infoCtb.getInfosFermeture();
			Assert.assertNull(infoFermeture);
		}

		// Cheseaux-Renens-Aubonne -> doit prendre tous les fors ajoutés
		{
			final Map<Long, ProduireRolesResults.InfoContribuable> map = results.buildInfosPourRegroupementCommunes(Arrays.asList(MockCommune.CheseauxSurLausanne.getNoOFS(), MockCommune.Renens.getNoOFS(), MockCommune.Aubonne.getNoOFS()));
			Assert.assertNotNull(map);
			Assert.assertEquals(1, map.size());

			final ProduireRolesResults.InfoContribuable infoCtb = map.get(ctb.getNumero());
			Assert.assertNotNull(infoCtb);
			Assert.assertEquals((long) ctb.getNumero(), infoCtb.noCtb);
			Assert.assertEquals(ProduireRolesResults.InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, infoCtb.getTypeAssujettissementAgrege());
			Assert.assertEquals(ProduireRolesResults.InfoContribuable.TypeContribuable.HORS_CANTON, infoCtb.getTypeCtb());

			final Pair<RegDate, MotifFor> infoOuverture = infoCtb.getInfosOuverture();
			Assert.assertNotNull(infoOuverture);
			Assert.assertEquals(date(1990, 7, 1), infoOuverture.getFirst());
			Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, infoOuverture.getSecond());

			final Pair<RegDate, MotifFor> infoFermeture = infoCtb.getInfosFermeture();
			Assert.assertNull(infoFermeture);
		}

		// Cheseaux-Lausanne -> seulement le for de Cheseaux
		{
			final Map<Long, ProduireRolesResults.InfoContribuable> map = results.buildInfosPourRegroupementCommunes(Arrays.asList(MockCommune.CheseauxSurLausanne.getNoOFS(), MockCommune.Lausanne.getNoOFS()));
			Assert.assertNotNull(map);
			Assert.assertEquals(1, map.size());

			final ProduireRolesResults.InfoContribuable infoCtb = map.get(ctb.getNumero());
			Assert.assertNotNull(infoCtb);
			Assert.assertEquals((long) ctb.getNumero(), infoCtb.noCtb);
			Assert.assertEquals(ProduireRolesResults.InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF, infoCtb.getTypeAssujettissementAgrege());
			Assert.assertEquals(ProduireRolesResults.InfoContribuable.TypeContribuable.HORS_CANTON, infoCtb.getTypeCtb());

			final Pair<RegDate, MotifFor> infoOuverture = infoCtb.getInfosOuverture();
			Assert.assertNotNull(infoOuverture);
			Assert.assertEquals(date(anneeRoles, 10, 15), infoOuverture.getFirst());
			Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, infoOuverture.getSecond());

			final Pair<RegDate, MotifFor> infoFermeture = infoCtb.getInfosFermeture();
			Assert.assertNull(infoFermeture);
		}
	}
}
