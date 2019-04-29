package ch.vd.unireg.role;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;

public class RoleHelperTest extends BusinessTest {

	private RoleHelper processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		processor = new RoleHelper(transactionManager, hibernateTemplate, tiersService);
	}

	@Test
	public void testGetIdsContribuables() throws Exception {

		final class Ids {
			long pp;
			long mc;
			long pm;
		}

		// mise en place des données
		final Ids ids = doInNewTransactionAndSession(status -> {
			final RegDate dateNaissance = date(1985, 8, 26);
			final RegDate dateAchat = date(2009, 4, 23);
			final RegDate dateArriveeVD = date(2011, 8, 1);
			final RegDate dateMariage = date(2012, 7, 14);
			final PersonnePhysique pp = addNonHabitant("Mélusine", "Chapuis", dateNaissance, Sexe.FEMININ);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateArriveeVD.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern);
			addForPrincipal(pp, dateArriveeVD, MotifFor.ARRIVEE_HC, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
			addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
			addForSecondaire(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE);

			final RegDate dateFondation = date(2000, 6, 1);
			final RegDate dateRadiation = date(2014, 10, 4);
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateFondation, null, FormeJuridiqueEntreprise.ASSOCIATION);
			addRaisonSociale(entreprise, dateFondation, null, "Les joyeux lurons");
			addRegimeFiscalCH(entreprise, dateFondation, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addRegimeFiscalVD(entreprise, dateFondation, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addForPrincipal(entreprise, dateFondation, MotifFor.DEBUT_EXPLOITATION, dateRadiation, MotifFor.FIN_EXPLOITATION, MockCommune.Renens);

			final Ids ids1 = new Ids();
			ids1.pp = pp.getNumero();
			ids1.mc = mc.getNumero();
			ids1.pm = entreprise.getNumero();
			return ids1;
		});

		// population PM
		doInNewTransactionAndSession(status -> {
			for (int annee = 1980; annee <= RegDate.get().year(); ++annee) {
				final List<Long> found = processor.getIdsContribuablesPM(annee, null);

				// l'entreprise n'apparaît qu'à partir de 2000, jusqu'à 2014
				if (annee >= 2000 && annee <= 2014) {
					Assert.assertEquals(String.valueOf(annee), Collections.singletonList(ids.pm), found);
				}
				else {
					Assert.assertEquals(String.valueOf(annee), Collections.emptyList(), found);
				}
			}
			return null;
		});

		// population PP
		doInNewTransactionAndSession(status -> {
			for (int annee = 1980; annee <= RegDate.get().year(); ++annee) {
				final List<Long> found = processor.getIdsContribuablesPP(annee, null);

				// de 2009 à 2011, on a juste la personne physique
				// en 2012, on a la personne physique et le ménage commun
				// depuis 2013, on a juste le ménage commun
				if (annee >= 2009 && annee <= 2011) {
					Assert.assertEquals(String.valueOf(annee), Collections.singletonList(ids.pp), found);
				}
				else if (annee == 2012) {
					Assert.assertEquals(String.valueOf(annee), Arrays.asList(ids.pp, ids.mc), found);
				}
				else if (annee >= 2013) {
					Assert.assertEquals(String.valueOf(annee), Collections.singletonList(ids.mc), found);
				}
				else {
					Assert.assertEquals(String.valueOf(annee), Collections.emptyList(), found);
				}
			}
			return null;
		});
	}

	@Test
	public void testGetIdsContribuablesAvecCommunes() throws Exception {

		final class Ids {
			long pp;
			long mc;
			long pm;
		}

		// mise en place des données
		final Ids ids = doInNewTransactionAndSession(status -> {
			final RegDate dateNaissance = date(1985, 8, 26);
			final RegDate dateAchat = date(2009, 4, 23);
			final RegDate dateArriveeVD = date(2011, 8, 1);
			final RegDate dateMariage = date(2012, 7, 14);
			final PersonnePhysique pp = addNonHabitant("Mélusine", "Chapuis", dateNaissance, Sexe.FEMININ);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateArriveeVD.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern);
			addForPrincipal(pp, dateArriveeVD, MotifFor.ARRIVEE_HC, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
			addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
			addForSecondaire(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE);

			final RegDate dateFondation = date(2000, 6, 1);
			final RegDate dateRadiation = date(2014, 10, 4);
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateFondation, null, FormeJuridiqueEntreprise.ASSOCIATION);
			addRaisonSociale(entreprise, dateFondation, null, "Les joyeux lurons");
			addRegimeFiscalCH(entreprise, dateFondation, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addRegimeFiscalVD(entreprise, dateFondation, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addForPrincipal(entreprise, dateFondation, MotifFor.DEBUT_EXPLOITATION, dateRadiation, MotifFor.FIN_EXPLOITATION, MockCommune.Renens);

			final Ids ids1 = new Ids();
			ids1.pp = pp.getNumero();
			ids1.mc = mc.getNumero();
			ids1.pm = entreprise.getNumero();
			return ids1;
		});

		// population PM (avec bonne commune)
		doInNewTransactionAndSession(status -> {
			for (int annee = 1980; annee <= RegDate.get().year(); ++annee) {
				final List<Long> found = processor.getIdsContribuablesPM(annee, Collections.singleton(MockCommune.Renens.getNoOFS()));

				// l'entreprise n'apparaît qu'à partir de 2000, jusqu'à 2014
				if (annee >= 2000 && annee <= 2014) {
					Assert.assertEquals(String.valueOf(annee), Collections.singletonList(ids.pm), found);
				}
				else {
					Assert.assertEquals(String.valueOf(annee), Collections.emptyList(), found);
				}
			}
			return null;
		});

		// population PM (avec mauvaise commune)
		doInNewTransactionAndSession(status -> {
			for (int annee = 1980; annee <= RegDate.get().year(); ++annee) {
				final List<Long> found = processor.getIdsContribuablesPM(annee, Collections.singleton(MockCommune.Lausanne.getNoOFS()));
				Assert.assertEquals(String.valueOf(annee), Collections.emptyList(), found);
			}
			return null;
		});

		// population PP (avec bonnes communes)
		doInNewTransactionAndSession(status -> {
			for (int annee = 1980; annee <= RegDate.get().year(); ++annee) {
				final List<Long> found = processor.getIdsContribuablesPP(annee, Stream.of(MockCommune.Cossonay, MockCommune.Bussigny).map(MockCommune::getNoOFS).collect(Collectors.toSet()));

				// de 2009 à 2011, on a juste la personne physique
				// en 2012, on a la personne physique et le ménage commun
				// depuis 2013, on a juste le ménage commun
				if (annee >= 2009 && annee <= 2011) {
					Assert.assertEquals(String.valueOf(annee), Collections.singletonList(ids.pp), found);
				}
				else if (annee == 2012) {
					Assert.assertEquals(String.valueOf(annee), Arrays.asList(ids.pp, ids.mc), found);
				}
				else if (annee >= 2013) {
					Assert.assertEquals(String.valueOf(annee), Collections.singletonList(ids.mc), found);
				}
				else {
					Assert.assertEquals(String.valueOf(annee), Collections.emptyList(), found);
				}
			}
			return null;
		});

		// population PP (avec mauvaises communes)
		doInNewTransactionAndSession(status -> {
			for (int annee = 1980; annee <= RegDate.get().year(); ++annee) {
				final List<Long> found = processor.getIdsContribuablesPP(annee, Stream.of(MockCommune.Lausanne, MockCommune.Echallens).map(MockCommune::getNoOFS).collect(Collectors.toSet()));
				Assert.assertEquals(String.valueOf(annee), Collections.emptyList(), found);
			}
			return null;
		});
	}

	@Test
	public void testDispatch() throws Exception {

		final class Ids {
			long pp;
			long mc;
			long pm;
		}

		// mise en place des données
		final Ids ids = doInNewTransactionAndSession(status -> {
			final RegDate dateNaissance = date(1985, 8, 26);
			final RegDate dateAchat = date(2009, 4, 23);
			final RegDate dateArriveeVD = date(2011, 8, 1);
			final RegDate dateMariage = date(2012, 7, 14);
			final PersonnePhysique pp = addNonHabitant("Mélusine", "Chapuis", dateNaissance, Sexe.FEMININ);
			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateArriveeVD.getOneDayBefore(), MotifFor.ARRIVEE_HC, MockCommune.Bern);
			addForPrincipal(pp, dateArriveeVD, MotifFor.ARRIVEE_HC, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
			addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE);

			final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
			addForSecondaire(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE);

			final RegDate dateFondation = date(2000, 6, 1);
			final RegDate dateRadiation = date(2014, 10, 4);
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateFondation, null, FormeJuridiqueEntreprise.ASSOCIATION);
			addRaisonSociale(entreprise, dateFondation, null, "Les joyeux lurons");
			addRegimeFiscalCH(entreprise, dateFondation, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addRegimeFiscalVD(entreprise, dateFondation, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addForPrincipal(entreprise, dateFondation, MotifFor.DEBUT_EXPLOITATION, dateRadiation, MotifFor.FIN_EXPLOITATION, MockCommune.Renens);

			final Ids ids1 = new Ids();
			ids1.pp = pp.getNumero();
			ids1.mc = mc.getNumero();
			ids1.pm = entreprise.getNumero();
			return ids1;
		});

		// dispatch PM
		doInNewTransactionAndSession(status -> {
			final List<Entreprise> entreprises = Stream.of(ids.pm)
					.map(tiersService::getTiers)
					.filter(Objects::nonNull)
					.map(Entreprise.class::cast)
					.collect(Collectors.toList());
			Assert.assertEquals(1, entreprises.size());

			for (int annee = 1980; annee <= RegDate.get().year(); ++annee) {
				final Map<Integer, List<Entreprise>> map = processor.dispatchPM(annee, entreprises);
				Assert.assertNotNull(map);
				Assert.assertEquals(String.valueOf(annee), 1, map.size());

				// l'entreprise n'apparaît qu'à partir de 2000, jusqu'à 2014
				if (annee >= 2000 && annee <= 2014) {
					Assert.assertEquals(String.valueOf(annee), Collections.singleton(MockCommune.Renens.getNoOFS()), map.keySet());
				}
				else {
					Assert.assertEquals(String.valueOf(annee), Collections.singleton(null), map.keySet());
				}

				final List<Entreprise> found = map.values().iterator().next();
				Assert.assertNotNull(String.valueOf(annee), found);
				Assert.assertEquals(String.valueOf(annee), entreprises, found);
			}
			return null;
		});

		// dispatch PP
		doInNewTransactionAndSession(status -> {
			final List<ContribuableImpositionPersonnesPhysiques> contribuables = Stream.of(ids.pp, ids.mc)
					.map(tiersService::getTiers)
					.filter(Objects::nonNull)
					.map(ContribuableImpositionPersonnesPhysiques.class::cast)
					.collect(Collectors.toList());
			Assert.assertEquals(2, contribuables.size());

			for (int annee = 1980; annee <= RegDate.get().year(); ++annee) {
				// de 2009 à 2011, on a juste la personne physique (sur Cossonay jusqu'à 2010, puis Bussigny)
				// dès 2012, on a la personne physique sans rôle et et le ménage commun sur Bussigny

				final Map<Integer, List<ContribuableImpositionPersonnesPhysiques>> map = processor.dispatchPP(annee, contribuables);
				Assert.assertNotNull(map);

				if (annee == 2009 || annee == 2010) {
					// pp seule sur Cossonay
					Assert.assertEquals(String.valueOf(annee), 2, map.size());
					Assert.assertTrue(String.valueOf(annee), map.keySet().contains(MockCommune.Cossonay.getNoOFS()));
					Assert.assertTrue(String.valueOf(annee), map.keySet().contains(null));

					// Cossonay
					{
						final List<ContribuableImpositionPersonnesPhysiques> found = map.get(MockCommune.Cossonay.getNoOFS());
						Assert.assertNotNull(String.valueOf(annee), found);
						Assert.assertEquals(String.valueOf(annee), Collections.singletonList(ids.pp), found.stream().map(Contribuable::getNumero).collect(Collectors.toList()));
					}

					// Sans attachement de commune
					{
						final List<ContribuableImpositionPersonnesPhysiques> found = map.get(null);
						Assert.assertNotNull(String.valueOf(annee), found);
						Assert.assertEquals(String.valueOf(annee), Collections.singletonList(ids.mc), found.stream().map(Contribuable::getNumero).collect(Collectors.toList()));
					}
				}
				else if (annee == 2011) {
					// pp seule sur Bussigny
					Assert.assertEquals(String.valueOf(annee), 2, map.size());
					Assert.assertTrue(String.valueOf(annee), map.keySet().contains(MockCommune.Bussigny.getNoOFS()));
					Assert.assertTrue(String.valueOf(annee), map.keySet().contains(null));

					// Bussigny
					{
						final List<ContribuableImpositionPersonnesPhysiques> found = map.get(MockCommune.Bussigny.getNoOFS());
						Assert.assertNotNull(String.valueOf(annee), found);
						Assert.assertEquals(String.valueOf(annee), Collections.singletonList(ids.pp), found.stream().map(Contribuable::getNumero).collect(Collectors.toList()));
					}

					// Sans attachement de commune
					{
						final List<ContribuableImpositionPersonnesPhysiques> found = map.get(null);
						Assert.assertNotNull(String.valueOf(annee), found);
						Assert.assertEquals(String.valueOf(annee), Collections.singletonList(ids.mc), found.stream().map(Contribuable::getNumero).collect(Collectors.toList()));
					}
				}
				else if (annee >= 2012) {
					// pp sans rôle, et mc sur Bussigny
					Assert.assertEquals(String.valueOf(annee), 2, map.size());
					Assert.assertTrue(String.valueOf(annee), map.keySet().contains(MockCommune.Bussigny.getNoOFS()));
					Assert.assertTrue(String.valueOf(annee), map.keySet().contains(null));

					// Bussigny
					{
						final List<ContribuableImpositionPersonnesPhysiques> found = map.get(MockCommune.Bussigny.getNoOFS());
						Assert.assertNotNull(String.valueOf(annee), found);
						Assert.assertEquals(String.valueOf(annee), Collections.singletonList(ids.mc), found.stream().map(Contribuable::getNumero).collect(Collectors.toList()));
					}

					// Sans attachement de commune
					{
						final List<ContribuableImpositionPersonnesPhysiques> found = map.get(null);
						Assert.assertNotNull(String.valueOf(annee), found);
						Assert.assertEquals(String.valueOf(annee), Collections.singletonList(ids.pp), found.stream().map(Contribuable::getNumero).collect(Collectors.toList()));
					}
				}
				else {
					// les deux sans rôle
					Assert.assertEquals(String.valueOf(annee), 1, map.size());
					Assert.assertTrue(String.valueOf(annee), map.keySet().contains(null));

					// Sans attachement de commune
					{
						final List<ContribuableImpositionPersonnesPhysiques> found = map.get(null);
						Assert.assertNotNull(String.valueOf(annee), found);
						Assert.assertEquals(String.valueOf(annee), contribuables, found);
					}
				}
			}
			return null;
		});
	}
}
