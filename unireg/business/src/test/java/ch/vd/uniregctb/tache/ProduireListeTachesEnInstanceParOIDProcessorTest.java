package ch.vd.uniregctb.tache;

import java.util.List;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TacheControleDossier;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEtatTache;

public class ProduireListeTachesEnInstanceParOIDProcessorTest extends BusinessTest {

	private ProduireListeTachesEnInstanceParOIDProcessor processor;

	public ProduireListeTachesEnInstanceParOIDProcessorTest() {
		setWantCollectivitesAdministratives(true);
	}

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

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
	public void testDifferenceOidSurTiersDeOidSurTache() throws Exception {

		// construction du tiers associé à l'OID 7 dans la table tiers
		final long ppid = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitantAvecFor("Lausanne", null, MockCommune.Lausanne);
				return pp.getNumero();
			}
		});

		// vérification que l'OID associé au contribuable est bien le 7 (OID Lausanne)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
				Assert.assertNotNull(pp);
				Assert.assertEquals((Integer) MockOfficeImpot.OID_LAUSANNE_OUEST.getNoColAdm(), pp.getOfficeImpotId());
				return null;
			}
		});

		// création d'une tâche sur ce contribuable mais associée à l'OID 1344 (SUCCESSIONS)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppid);
				final CollectiviteAdministrative successions = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noACISuccessions);
				addTacheControle(pp, TypeEtatTache.EN_INSTANCE, successions);
				return null;
			}
		});

		// lancement du processeur
		final ListeTachesEnInstanceParOID res = doInNewTransactionAndSession(new TxCallback<ListeTachesEnInstanceParOID>() {
			@Override
			public ListeTachesEnInstanceParOID execute(TransactionStatus status) throws Exception {
				return processor.run(RegDate.get(), null);
			}
		});

		// dans les statistiques, la tâche doit être placée sur l'OID 22 même si le tiers est lié à l'OID 7...

		Assert.assertNotNull(res);
		Assert.assertEquals(1.0, res.getNombreTacheMoyen());

		final List<ListeTachesEnInstanceParOID.LigneTacheInstance> lignes = res.getLignes();
		Assert.assertNotNull(lignes);
		Assert.assertEquals(1, lignes.size());

		final ListeTachesEnInstanceParOID.LigneTacheInstance ligne = lignes.get(0);
		Assert.assertNotNull(ligne);
		Assert.assertEquals(ServiceInfrastructureRaw.noACISuccessions, ligne.getNumeroOID());
		Assert.assertEquals(TacheControleDossier.class.getSimpleName(), ligne.getTypeTache());
		Assert.assertEquals(1, ligne.getNombreTache());
	}
}
