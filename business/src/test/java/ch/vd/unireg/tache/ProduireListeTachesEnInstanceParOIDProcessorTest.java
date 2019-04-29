package ch.vd.unireg.tache;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TacheControleDossier;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

public class ProduireListeTachesEnInstanceParOIDProcessorTest extends BusinessTest {

	private ProduireListeTachesEnInstanceParOIDProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		processor = new ProduireListeTachesEnInstanceParOIDProcessor(hibernateTemplate, tiersService, adresseService);
	}

	private PersonnePhysique addNonHabitantAvecFor(String nom, String prenom, MockCommune communeFor) {
		final PersonnePhysique pp = addNonHabitant(prenom, nom, null, null);
		final int year = RegDate.get().year();
		addForPrincipal(pp, date(year, 1, 1), MotifFor.ARRIVEE_HS, communeFor);
		return pp;
	}

	@Test
	public void testTousTypesDeTacheSupportes() throws Exception {
		final Map<String, TypeTache> mapping = ProduireListeTachesEnInstanceParOIDProcessor.buildTypesTacheMapping();
		Assert.assertNotNull(mapping);

		final Set<TypeTache> allTypes = EnumSet.allOf(TypeTache.class);
		Assert.assertEquals(allTypes.size(), mapping.size());

		allTypes.removeAll(mapping.values());
		if (!allTypes.isEmpty()) {
			Assert.fail("Le mapping des types de tâches ne comprend pas les types " + CollectionsUtils.toString(allTypes, TypeTache::name, ", "));
		}
	}

	@Test
	public void testDifferenceOidSurTiersDeOidSurTache() throws Exception {

		// construction du tiers associé à l'OID 7 dans la table tiers
		final long ppid = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitantAvecFor("Lausanne", null, MockCommune.Lausanne);
			return pp.getNumero();
		});

		// vérification que l'OID associé au contribuable est bien le 7 (OID Lausanne)
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
			Assert.assertNotNull(pp);
			Assert.assertEquals((Integer) MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), pp.getOfficeImpotId());
			return null;
		});

		// création d'une tâche sur ce contribuable mais associée à la nouvelle entité
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
			final CollectiviteAdministrative ca = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.noNouvelleEntite);
			addTacheControle(pp, TypeEtatTache.EN_INSTANCE, ca);
			return null;
		});

		// lancement du processeur
		final ListeTachesEnInstanceParOID res = doInNewTransactionAndSession(new TxCallback<ListeTachesEnInstanceParOID>() {
			@Override
			public ListeTachesEnInstanceParOID execute(TransactionStatus status) throws Exception {
				return processor.run(RegDate.get(), null);
			}
		});

		// dans les statistiques, la tâche doit être placée sur la nouvelle entité même si le tiers est lié à l'OID 7...

		Assert.assertNotNull(res);
		Assert.assertEquals(1.0, res.getNombreTacheMoyen(), 1e-12);

		final List<ListeTachesEnInstanceParOID.LigneTacheInstance> lignes = res.getLignes();
		Assert.assertNotNull(lignes);
		Assert.assertEquals(1, lignes.size());

		final ListeTachesEnInstanceParOID.LigneTacheInstance ligne = lignes.get(0);
		Assert.assertNotNull(ligne);
		Assert.assertEquals(MockCollectiviteAdministrative.noNouvelleEntite, ligne.getNumeroOID());
		Assert.assertEquals(TacheControleDossier.class.getSimpleName(), ligne.getTypeTache());
		Assert.assertEquals(1, ligne.getNombreTache());
	}
}
