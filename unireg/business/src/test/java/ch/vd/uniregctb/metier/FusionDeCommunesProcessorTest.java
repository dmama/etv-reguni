package ch.vd.uniregctb.metier;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.tiers.*;
import ch.vd.uniregctb.type.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class FusionDeCommunesProcessorTest extends BusinessTest {

	private FusionDeCommunesProcessor processor;
	private Set<Integer> anciensNoOfs;
	private int nouveauNoOfs;
	private RegDate dateFusion;
	private RegDate dateTraitement = RegDate.get();

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final TiersService tiersService = getBean(TiersService.class, "tiersService");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new FusionDeCommunesProcessor(transactionManager, hibernateTemplate, tiersService);

		// Annexion de Croy et Vaulion par Romainmôtier (scénario prophétique et stimulant)
		anciensNoOfs = new HashSet<Integer>();
		anciensNoOfs.add(MockCommune.RomainmotierEnvy.getNoOFS());
		anciensNoOfs.add(MockCommune.Croy.getNoOFS());
		anciensNoOfs.add(MockCommune.Vaulion.getNoOFS());
		nouveauNoOfs = MockCommune.RomainmotierEnvy.getNoOFS();
		dateFusion = date(2000, 1, 1);
	}

	@Test
	public void testTraiteContribuableInvalide() throws Exception {

		final PersonnePhysique bruno = addNonHabitant("Bruno", "Rien", date(1966, 8, 1), Sexe.MASCULIN);
		addForPrincipal(bruno, date(1984, 8, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
		// un second for principal qui chevauche le premier -> invalide
		ForFiscalPrincipal f = new ForFiscalPrincipal();
		f.setDateDebut(date(1997, 10, 29));
		f.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		f.setDateFin(null);
		f.setMotifFermeture(null);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(MockCommune.Croy.getNoOFS());
		f.setMotifRattachement(MotifRattachement.DOMICILE);
		f.setModeImposition(ModeImposition.ORDINAIRE);
		bruno.addForFiscal(f);

		final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement);
		processor.setRapport(rapport);
		processor.traiteTiers(bruno.getNumero(), anciensNoOfs, nouveauNoOfs, dateFusion);

		// Le contribuable ne valide pas -> il ne devrait pas être traité et apparaître en erreur
		final Tiers.ForsParType fors = bruno.getForsParType(true);
		assertNotNull(fors);
		assertEquals(2, fors.principaux.size());
		assertEmpty(fors.secondaires);

		final ForFiscalPrincipal ffp0 = fors.principaux.get(0);
		assertNotNull(ffp0);
		assertForPrincipal(date(1984, 8, 1), MotifFor.MAJORITE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE,
				ffp0);

		final ForFiscalPrincipal ffp1 = fors.principaux.get(1);
		assertNotNull(ffp1);
		assertForPrincipal(date(1997, 10, 29), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Croy.getNoOFS(), MotifRattachement.DOMICILE,
				ModeImposition.ORDINAIRE, ffp1);

		assertEquals(1, rapport.nbTiersTotal);
		assertEquals(0, rapport.tiersTraites.size());
		assertEquals(0, rapport.tiersIgnores.size());
		assertEquals(1, rapport.tiersEnErrors.size());

		FusionDeCommunesResults.Erreur error = rapport.tiersEnErrors.get(0);
		assertNotNull(error);
		assertEquals(FusionDeCommunesResults.ErreurType.VALIDATION, error.raison);
	}

	@Test
	public void testTraiteContribuableSansFor() throws Exception {

		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Rien", date(1966, 8, 1), Sexe.MASCULIN);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement);
		processor.setRapport(rapport);
		processor.traiteTiers(id, anciensNoOfs, nouveauNoOfs, dateFusion);

		// Le contribuable ne possède pas de for -> il ne devrait pas être impacté
		final PersonnePhysique bruno = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, id);
		assertNotNull(bruno);
		assertEmpty(bruno.getForsFiscaux());

		assertEquals(1, rapport.nbTiersTotal);
		assertEquals(0, rapport.tiersTraites.size());
		assertEquals(0, rapport.tiersIgnores.size());
		assertEmpty(rapport.tiersEnErrors);
	}

	@Test
	public void testTraiteContribuableAvecForsSurAutresCommunes() throws Exception {

		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Quelquechose", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				addForSecondaire(bruno, date(1995, 8, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bex.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement);
		processor.setRapport(rapport);
		processor.traiteTiers(id, anciensNoOfs, nouveauNoOfs, dateFusion);

		// Le contribuable possède des fors sur des communes non-concernées pas la fusion -> il ne devrait pas être impacté
		final PersonnePhysique bruno = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, id);
		assertNotNull(bruno);

		final Tiers.ForsParType fors = bruno.getForsParType(true);
		assertNotNull(fors);
		assertEquals(1, fors.principaux.size());
		assertEquals(1, fors.secondaires.size());

		final ForFiscalPrincipal ffp = fors.principaux.get(0);
		assertNotNull(ffp);
		assertForPrincipal(date(1964, 8, 1), MotifFor.MAJORITE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);

		final ForFiscalSecondaire ffs = fors.secondaires.get(0);
		assertNotNull(ffs);
		assertForSecondaire(date(1995, 8, 1), MotifFor.ACHAT_IMMOBILIER, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Bex.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE, ffs);

		assertEquals(1, rapport.nbTiersTotal);
		assertEquals(0, rapport.tiersIgnores.size());
		assertEquals(0, rapport.tiersTraites.size());
		assertEmpty(rapport.tiersEnErrors);
	}

	@Test
	public void testTraiteContribuableAvecAnciensFors() throws Exception {

		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Quelquechose", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, date(1990, 4, 22), MotifFor.DEMENAGEMENT_VD, MockCommune.Croy);
				addForPrincipal(bruno, date(1990, 4, 23), MotifFor.DEMENAGEMENT_VD, MockCommune.Renens);
				addForSecondaire(bruno, date(1995, 8, 1), MotifFor.ACHAT_IMMOBILIER, date(1999, 6, 30), MotifFor.VENTE_IMMOBILIER, MockCommune.Croy.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement);
		processor.setRapport(rapport);
		processor.traiteTiers(id, anciensNoOfs, nouveauNoOfs, dateFusion);

		// Le contribuable possède des fors sur les communes concernées pas la fusion, mais ils sont tous fermés -> il ne devrait pas être impacté
		final PersonnePhysique bruno = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, id);
		assertNotNull(bruno);

		final Tiers.ForsParType fors = bruno.getForsParType(true);
		assertNotNull(fors);
		assertEquals(2, fors.principaux.size());
		assertEquals(1, fors.secondaires.size());

		final ForFiscalPrincipal ffp0 = fors.principaux.get(0);
		assertNotNull(ffp0);
		assertForPrincipal(date(1964, 8, 1), MotifFor.MAJORITE, date(1990, 4, 22), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Croy.getNoOFS(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp0);

		final ForFiscalPrincipal ffp1 = fors.principaux.get(1);
		assertNotNull(ffp1);
		assertForPrincipal(date(1990, 4, 23), MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE,
				ffp1);

		final ForFiscalSecondaire ffs = fors.secondaires.get(0);
		assertNotNull(ffs);
		assertForSecondaire(date(1995, 8, 1), MotifFor.ACHAT_IMMOBILIER, date(1999, 6, 30), MotifFor.VENTE_IMMOBILIER, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Croy.getNoOFS(),
				MotifRattachement.IMMEUBLE_PRIVE, ffs);

		assertEquals(1, rapport.nbTiersTotal);
		assertEquals(0, rapport.tiersIgnores.size());
		assertEquals(0, rapport.tiersTraites.size());
		assertEmpty(rapport.tiersEnErrors);
	}

	@Test
	public void testTraiteContribuableAvecForSecondaire() throws Exception {

		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Propriétaire", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, MockCommune.Renens);
				addForSecondaire(bruno, date(1995, 8, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement);
		processor.setRapport(rapport);
		processor.traiteTiers(id, anciensNoOfs, nouveauNoOfs, dateFusion);

		// Le contribuable possède un immeuble sur une des communes concernées pas la fusion -> ce for devrait être mis-à-jour
		final PersonnePhysique bruno = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, id);
		assertNotNull(bruno);

		final Tiers.ForsParType fors = bruno.getForsParType(true);
		assertNotNull(fors);
		assertEquals(1, fors.principaux.size());
		assertEquals(2, fors.secondaires.size());

		final ForFiscalPrincipal ffp = fors.principaux.get(0);
		assertNotNull(ffp);
		assertForPrincipal(date(1964, 8, 1), MotifFor.MAJORITE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);

		final ForFiscalSecondaire ffs0 = fors.secondaires.get(0);
		assertNotNull(ffs0);
		assertForSecondaire(date(1995, 8, 1), MotifFor.ACHAT_IMMOBILIER, dateFusion.getOneDayBefore(), MotifFor.FUSION_COMMUNES, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
				MockCommune.Croy.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE, ffs0);

		final ForFiscalSecondaire ffs1 = fors.secondaires.get(1);
		assertNotNull(ffs1);
		assertForSecondaire(dateFusion, MotifFor.FUSION_COMMUNES, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE, ffs1);

		assertEquals(1, rapport.nbTiersTotal);
		assertEquals(0, rapport.tiersIgnores.size());
		assertEquals(1, rapport.tiersTraites.size());
		assertEmpty(rapport.tiersEnErrors);
	}

	@Test
	public void testTraiteContribuableAvecForPrincipal() throws Exception {

		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Citoyen", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, MockCommune.Croy);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement);
		processor.setRapport(rapport);
		processor.traiteTiers(id, anciensNoOfs, nouveauNoOfs, dateFusion);

		// Le contribuable habite sur une des communes concernées pas la fusion -> son for principal devrait être mis-à-jour
		final PersonnePhysique bruno = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, id);
		assertNotNull(bruno);

		final Tiers.ForsParType fors = bruno.getForsParType(true);
		assertNotNull(fors);
		assertEquals(2, fors.principaux.size());
		assertEmpty(fors.secondaires);

		final ForFiscalPrincipal ffp0 = fors.principaux.get(0);
		assertNotNull(ffp0);
		assertForPrincipal(date(1964, 8, 1), MotifFor.MAJORITE, dateFusion.getOneDayBefore(), MotifFor.FUSION_COMMUNES, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Croy.getNoOFS(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp0);

		final ForFiscalPrincipal ffp1 = fors.principaux.get(1);
		assertNotNull(ffp1);
		assertForPrincipal(dateFusion, MotifFor.FUSION_COMMUNES, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE,
				ffp1);

		assertEquals(1, rapport.nbTiersTotal);
		assertEquals(0, rapport.tiersIgnores.size());
		assertEquals(1, rapport.tiersTraites.size());
		assertEmpty(rapport.tiersEnErrors);
	}

	@Test
	public void testTraiteContribuableAvecForsExotiques() throws Exception {

		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Citoyen", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, MockCommune.Lausanne);
				addForAutreImpot(bruno, date(1983, 4, 6), null, MockCommune.Vaulion.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, GenreImpot.CHIENS);
				addForAutreElementImposable(bruno, date(1992, 4, 6), null, MockCommune.Vaulion, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.PRESTATION_PREVOYANCE);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement);
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				processor.setRapport(rapport);
				processor.traiteTiers(id, anciensNoOfs, nouveauNoOfs, dateFusion);
				return null;
			}
		});

		// Le contribuable possèdes plusieurs fors sur des communes concernées pas la fusion -> ils devraient être mis à jour
		final PersonnePhysique bruno = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, id);
		assertNotNull(bruno);

		final Tiers.ForsParType fors = bruno.getForsParType(true);
		assertNotNull(fors);
		assertEquals(1, fors.principaux.size());
		assertEmpty(fors.secondaires);
		assertEquals(2, fors.autresImpots.size());
		assertEquals(2, fors.autreElementImpot.size());

		final ForFiscalAutreImpot fai0 = fors.autresImpots.get(0);
		assertNotNull(fai0);
		assertForAutreImpot(date(1983, 4, 6), dateFusion.getOneDayBefore(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Vaulion, GenreImpot.CHIENS, fai0);

		final ForFiscalAutreImpot fai1 = fors.autresImpots.get(1);
		assertNotNull(fai1);
		assertForAutreImpot(dateFusion, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy, GenreImpot.CHIENS, fai1);

		final ForFiscalAutreElementImposable faei0 = fors.autreElementImpot.get(0);
		assertNotNull(faei0);
		assertForAutreElementImposable(date(1992, 4, 6), dateFusion.getOneDayBefore(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Vaulion, MotifRattachement.PRESTATION_PREVOYANCE, faei0);

		final ForFiscalAutreElementImposable faei1 = fors.autreElementImpot.get(1);
		assertNotNull(faei1);
		assertForAutreElementImposable(dateFusion, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy, MotifRattachement.PRESTATION_PREVOYANCE, faei1);

		assertEquals(1, rapport.nbTiersTotal);
		assertEquals(0, rapport.tiersIgnores.size());
		assertEquals(1, rapport.tiersTraites.size());
		assertEmpty(rapport.tiersEnErrors);
	}

	@Test
	public void testTraiteContribuableAvecForsDansLeFutur() throws Exception {

		final RegDate dateFutur = date(2006, 1, 1);
		final RegDate veilleDateFutur = dateFutur.getOneDayBefore();

		// Un contribuable avec tout pleins de fors exotiques dans le futur (= cas à priori pas autorisé aujourd'hui, mais soyons prévoyant)
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Citoyen", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, veilleDateFutur, MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				addForPrincipal(bruno, dateFutur, MotifFor.DEMENAGEMENT_VD, MockCommune.Croy);
				addForSecondaire(bruno, dateFutur, MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);
				addForAutreImpot(bruno, dateFutur, null, MockCommune.Croy.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, GenreImpot.CHIENS);
				addForAutreElementImposable(bruno, dateFutur, null, MockCommune.Croy, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.PRESTATION_PREVOYANCE);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement);
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				processor.setRapport(rapport);
				processor.traiteTiers(id, anciensNoOfs, nouveauNoOfs, dateFusion);
				return null;
			}
		});

		// Le contribuable possède plusieurs fors *dans le futurs* sur des communes concernées pas la fusion -> leurs numéro OFs devraient être mis à jour sans changement de dates
		final PersonnePhysique bruno = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, id);
		assertNotNull(bruno);

		final Tiers.ForsParType fors = bruno.getForsParType(true);
		assertNotNull(fors);
		assertEquals(2, fors.principaux.size());
		assertEquals(1, fors.secondaires.size());
		assertEquals(1, fors.autresImpots.size());
		assertEquals(1, fors.autreElementImpot.size());

		final ForFiscalPrincipal ffp0 = fors.principaux.get(0);
		assertNotNull(ffp0);
		assertForPrincipal(date(1964, 8, 1), MotifFor.MAJORITE, veilleDateFutur, MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp0);

		final ForFiscalPrincipal ffp1 = fors.principaux.get(1);
		assertNotNull(ffp1);
		assertForPrincipal(dateFutur, MotifFor.DEMENAGEMENT_VD, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE,
				ffp1);

		final ForFiscalSecondaire ffs = fors.secondaires.get(0);
		assertNotNull(ffs);
		assertForSecondaire(dateFutur, MotifFor.ACHAT_IMMOBILIER, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE, ffs);

		final ForFiscalAutreImpot fai = fors.autresImpots.get(0);
		assertNotNull(fai);
		assertForAutreImpot(dateFutur, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy, GenreImpot.CHIENS, fai);

		final ForFiscalAutreElementImposable faei = fors.autreElementImpot.get(0);
		assertNotNull(faei);
		assertForAutreElementImposable(dateFutur, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy, MotifRattachement.PRESTATION_PREVOYANCE, faei);

		assertEquals(1, rapport.nbTiersTotal);
		assertEquals(0, rapport.tiersIgnores.size());
		assertEquals(1, rapport.tiersTraites.size());
		assertEmpty(rapport.tiersEnErrors);
	}

	@Test
	public void testTraiteContribuableAvecForDejaSurNouvelleCommune() throws Exception {

		// Le contribuable habite déjà sur la commune résultant de la fusion
		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique bruno = addNonHabitant("Bruno", "Majoritaire", date(1966, 8, 1), Sexe.MASCULIN);
				addForPrincipal(bruno, date(1964, 8, 1), MotifFor.MAJORITE, MockCommune.RomainmotierEnvy);
				return bruno.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement);
		processor.setRapport(rapport);
		processor.traiteTiers(id, anciensNoOfs, nouveauNoOfs, dateFusion);

		// Le contribuable habite déjà sur la commune résultant de la fusion -> son for principal ne doit pas être mis-à-jour
		final PersonnePhysique bruno = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, id);
		assertNotNull(bruno);

		final Tiers.ForsParType fors = bruno.getForsParType(true);
		assertNotNull(fors);
		assertEquals(1, fors.principaux.size());

		final ForFiscalPrincipal ffp = fors.principaux.get(0);
		assertNotNull(ffp);
		assertForPrincipal(date(1964, 8, 1), MotifFor.MAJORITE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);

		assertEquals(1, rapport.nbTiersTotal);
		assertEquals(1, rapport.tiersIgnores.size());
		assertEquals(0, rapport.tiersTraites.size());
		assertEmpty(rapport.tiersEnErrors);
	}

	@Test
	public void testTraiteDebiteurAvecFor() throws Exception {

		final Long id = (Long) doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final DebiteurPrestationImposable dpi = addDebiteur();
				addForDebiteur(dpi, date(1990, 5, 23), null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Croy);
				return dpi.getNumero();
			}
		});

		final FusionDeCommunesResults rapport = new FusionDeCommunesResults(anciensNoOfs, nouveauNoOfs, dateFusion, dateTraitement);
		processor.setRapport(rapport);
		processor.traiteTiers(id, anciensNoOfs, nouveauNoOfs, dateFusion);

		// Le débiteur habite sur une des communes concernées pas la fusion -> son for principal devrait être mis-à-jour
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) hibernateTemplate.get(DebiteurPrestationImposable.class, id);
		assertNotNull(dpi);

		final List<ForFiscal> fors = dpi.getForsFiscauxSorted();
		assertNotNull(fors);
		assertEquals(2, fors.size());

		final ForDebiteurPrestationImposable ffp0 = (ForDebiteurPrestationImposable) fors.get(0);
		assertNotNull(ffp0);
		assertForDebiteur(date(1990, 5, 23), dateFusion.getOneDayBefore(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Croy.getNoOFS(), ffp0);

		final ForDebiteurPrestationImposable ffp1 = (ForDebiteurPrestationImposable) fors.get(1);
		assertNotNull(ffp1);
		assertForDebiteur(dateFusion, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.RomainmotierEnvy.getNoOFS(), ffp1);

		assertEquals(1, rapport.nbTiersTotal);
		assertEquals(0, rapport.tiersIgnores.size());
		assertEquals(1, rapport.tiersTraites.size());
		assertEmpty(rapport.tiersEnErrors);
	}
}
