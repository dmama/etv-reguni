package ch.vd.uniregctb.evenement.degrevement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.xml.event.degrevement.v1.Caracteristiques;
import ch.vd.unireg.xml.event.degrevement.v1.CodeSupport;
import ch.vd.unireg.xml.event.degrevement.v1.DonneesMetier;
import ch.vd.unireg.xml.event.degrevement.v1.Message;
import ch.vd.unireg.xml.event.degrevement.v1.SousTypeDocument;
import ch.vd.unireg.xml.event.degrevement.v1.Supervision;
import ch.vd.unireg.xml.event.degrevement.v1.TypDateAttr;
import ch.vd.unireg.xml.event.degrevement.v1.TypEntMax12Attr;
import ch.vd.unireg.xml.event.degrevement.v1.TypPctPosDecMax32Attr;
import ch.vd.unireg.xml.event.degrevement.v1.TypeDocument;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.foncier.AllegementFoncier;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.foncier.DonneesUtilisation;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.TypeRapprochementRF;

public class EvenementDegrevementHandlerV1Test extends BusinessTest {

	private EvenementDegrevementHandlerV1 handler;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		final EvenementDegrevementHandlerV1Impl impl = new EvenementDegrevementHandlerV1Impl();
		impl.setTiersService(tiersService);
		impl.setHibernateTemplate(hibernateTemplate);
		handler = impl;
	}

	private Message buildRetour(RegDate dateReception, DonneesMetier donneesMetier) {
		final Supervision supervision = new Supervision(XmlUtils.regdate2xmlcal(dateReception), null, null, null, "BID378426287", "637946273rwgfs", "rt7862gs");
		final Caracteristiques caracteristiques = new Caracteristiques(TypeDocument.DEM_DEGREV, SousTypeDocument.DEM_DEGREV, CodeSupport.ELECTRONIQUE, "TOTOLEHEROS");
		return new Message(supervision, caracteristiques, donneesMetier);
	}

	private Message buildRetour(int pf, long noCtb, long noSequence, RegDate dateReception) {
		final DonneesMetier donneesMetier = buildDonneesMetier(pf, noCtb, noSequence,
		                                                       100000L, true,
		                                                       1000L, true,
		                                                       350L, true,
		                                                       BigDecimal.valueOf(50), true,
		                                                       100000L, true,
		                                                       1234L, false,
		                                                       200L, true,
		                                                       BigDecimal.valueOf(50), true,
		                                                       false,
		                                                       null, false,
		                                                       null, false);
		return buildRetour(dateReception, donneesMetier);
	}

	private DonneesMetier buildDonneesMetier(int pf, long noCtb, long noSequence,
	                                         @Nullable Long revenuLocatifEncaisse, boolean revenuLocatifEncaisseValide,
	                                         @Nullable Long volumeLocatif, boolean volumeLocatifValide,
	                                         @Nullable Long surfaceLocative, boolean surfaceLocativeValide,
	                                         @Nullable BigDecimal pourcentLocatif, boolean pourcentLocatifValide,
	                                         @Nullable Long revenuEstime, boolean revenuEstimeValide,
	                                         @Nullable Long volumeUsagePropre, boolean volumeUsagePropreValide,
	                                         @Nullable Long surfaceUsagePropre, boolean surfaceUsagePropreValide,
	                                         @Nullable BigDecimal pourcentUsagePropre, boolean pourcentUsagePropreValide,
	                                         boolean controleLoiLogement,
	                                         @Nullable RegDate dateOctroi, boolean dateOctroiValide,
	                                         @Nullable RegDate dateEcheanceOctroi, boolean dateEcheanceOctroiValide) {
		Assert.assertTrue(Long.toString(noCtb), noCtb >= Integer.MIN_VALUE && noCtb <= Integer.MAX_VALUE);
		return new DonneesMetier(pf, (int) noCtb, BigInteger.valueOf(noSequence), null, null,
		                         buildEntier(revenuLocatifEncaisse, revenuLocatifEncaisseValide),
		                         buildEntier(volumeLocatif, volumeLocatifValide),
		                         buildEntier(surfaceLocative, surfaceLocativeValide),
		                         buildPourcent(pourcentLocatif, pourcentLocatifValide),
		                         buildEntier(revenuEstime, revenuEstimeValide),
		                         buildEntier(volumeUsagePropre, volumeUsagePropreValide),
		                         buildEntier(surfaceUsagePropre, surfaceUsagePropreValide),
		                         buildPourcent(pourcentUsagePropre, pourcentUsagePropreValide),
		                         controleLoiLogement,
		                         buildDate(dateOctroi, dateOctroiValide),
		                         buildDate(dateEcheanceOctroi, dateEcheanceOctroiValide), null);
	}

	private static TypEntMax12Attr buildEntier(Long value, boolean valide) {
		return value != null ? new TypEntMax12Attr(BigInteger.valueOf(value), valide, null) : null;
	}

	private static TypPctPosDecMax32Attr buildPourcent(BigDecimal value, boolean valide) {
		return value != null ? new TypPctPosDecMax32Attr(value, valide, null) : null;
	}

	private static TypDateAttr buildDate(RegDate date, boolean valide) {
		return date != null ? new TypDateAttr(XmlUtils.regdate2xmlcal(date), valide, null) : null;
	}

	@Test
	public void testFormulaireDemandeInexistant() throws Exception {

		final RegDate dateDebut = date(2007, 4, 1);
		final RegDate dateChargement = date(2017, 1, 7);
		final RegDate dateAchat = date(2016, 8, 3);
		
		// mise en place fiscale
		final int idpm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Petite Arvine");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
		    addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		    addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

		    final PersonneMoraleRF tiersRF = addPersonneMoraleRF("Petite Arvine", null, "3478t267gfhs", 32561251L, null);
		    addRapprochementRF(entreprise, tiersRF, null, null, TypeRapprochementRF.AUTO);

		    final CommuneRF communeRF = addCommuneRF(22, "Bain bain", 8742);
		    final BienFondRF immeuble = addBienFondRF("478235z32hf", null, communeRF, 1423);
		    addDroitPersonneMoraleRF(dateChargement, dateAchat, null, "Achat", null, "57485ztfgdé",
		                             new IdentifiantAffaireRF(1234, "452"),
		                             new Fraction(1, 1),
		                             GenrePropriete.INDIVIDUELLE,
		                             tiersRF, immeuble, null);

		    return entreprise.getNumero().intValue();
		});

		// réception des données
		final Message retour = buildRetour(2016, idpm, 4321L, RegDate.get());
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				try {
					handler.onRetourDegrevement(retour, null);
					Assert.fail("L'appel aurait dû échouer par manque de formulaire de demande de dégrèvement initial");
				}
				catch (EsbBusinessException e) {
					Assert.assertEquals(EsbBusinessCode.DECLARATION_ABSENTE, e.getCode());
					Assert.assertEquals("Formulaire de demande de dégrèvement introuvable pour la PF 2016 et le numéro de séquence 4321", e.getMessage());

					// on ne relance pas l'exception pour committer la transaction quand-même
					// (c'est ce qui se passe dans le cas d'une EsbBusinessException)
				}
			}
		});

		// vérification en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get((long) idpm);
				Assert.assertNotNull(entreprise);

				final List<DegrevementICI> degrevements = entreprise.getAllegementsFonciers().stream()
						.filter(af -> af instanceof DegrevementICI)
						.map(af -> (DegrevementICI) af)
						.collect(Collectors.toList());
				Assert.assertEquals(0, degrevements.size());
			}
		});
	}

	@Test
	public void testFormulaireDemandeExistant() throws Exception {

		final RegDate dateDebut = date(2007, 4, 1);
		final RegDate dateChargement = date(2017, 1, 7);
		final RegDate dateAchat = date(2016, 8, 3);
		final RegDate dateEnvoiFormulaire = date(2017, 1, 9);
		final int pf = 2016;
		final RegDate dateReception = RegDate.get().addDays(-2);

		final class Ids {
			final long idPM;
			final long idImmeuble;
			final int noSequence;
			public Ids(long idPM, long idImmeuble, int noSequence) {
				this.idPM = idPM;
				this.idImmeuble = idImmeuble;
				this.noSequence = noSequence;
			}
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Petite Arvine");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
		    addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		    addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

		    final PersonneMoraleRF tiersRF = addPersonneMoraleRF("Petite Arvine", null, "3478t267gfhs", 32561251L, null);
		    addRapprochementRF(entreprise, tiersRF, null, null, TypeRapprochementRF.AUTO);

		    final CommuneRF communeRF = addCommuneRF(22, "Bain bain", 8742);
		    final BienFondRF immeuble = addBienFondRF("478235z32hf", null, communeRF, 1423);
		    addDroitPersonneMoraleRF(dateChargement, dateAchat, null, "Achat", null, "57485ztfgdé",
		                             new IdentifiantAffaireRF(1234, "452"),
		                             new Fraction(1, 1),
		                             GenrePropriete.INDIVIDUELLE,
		                             tiersRF, immeuble, null);

			final DemandeDegrevementICI formulaire = addDemandeDegrevementICI(entreprise, dateEnvoiFormulaire, dateEnvoiFormulaire.addMonths(3), null, null, pf, immeuble);
			Assert.assertNotNull(formulaire);
			return new Ids(entreprise.getNumero(), immeuble.getId(), formulaire.getNumeroSequence());
		});

		// réception des données
		final RegDate dateOctroi = date(2016, 4, 2);
		final RegDate dateEcheanceOctroi = date(2017, 12, 1);
		final DonneesMetier donneesMetier = buildDonneesMetier(pf, ids.idPM, ids.noSequence,
		                                                       1L, true,
		                                                       2L, true,
		                                                       3L, true,
		                                                       BigDecimal.TEN, true,
		                                                       4L, true,
		                                                       5L, true,
		                                                       6L, true,
		                                                       BigDecimal.valueOf(20), true,
		                                                       true,
		                                                       dateOctroi, true,
		                                                       dateEcheanceOctroi, true);
		final Message retour = buildRetour(dateReception, donneesMetier);
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				handler.onRetourDegrevement(retour, null);
			}
		});

		// vérification en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idPM);
				Assert.assertNotNull(entreprise);

				final List<DegrevementICI> degrevements = entreprise.getAllegementsFonciers().stream()
						.filter(af -> af instanceof DegrevementICI)
						.map(af -> (DegrevementICI) af)
						.collect(Collectors.toList());
				Assert.assertEquals(1, degrevements.size());

				final DegrevementICI degrevement = degrevements.get(0);
				Assert.assertNotNull(degrevement);
				Assert.assertFalse(degrevement.isAnnule());
				Assert.assertEquals(date(pf, 1, 1), degrevement.getDateDebut());
				Assert.assertNull(degrevement.getDateFin());

				// vérification des valeurs sauvegardées
				Assert.assertNotNull(degrevement.getLocation());
				Assert.assertNotNull(degrevement.getPropreUsage());
				Assert.assertNotNull(degrevement.getLoiLogement());
				Assert.assertEquals((Integer) 1, degrevement.getLocation().getRevenu());
				Assert.assertEquals((Integer) 2, degrevement.getLocation().getVolume());
				Assert.assertEquals((Integer) 3, degrevement.getLocation().getSurface());
				Assert.assertEquals(0, BigDecimal.valueOf(10L).compareTo(degrevement.getLocation().getPourcentage()));
				Assert.assertNull(degrevement.getLocation().getPourcentageArrete());
				Assert.assertEquals((Integer) 4, degrevement.getPropreUsage().getRevenu());
				Assert.assertEquals((Integer) 5, degrevement.getPropreUsage().getVolume());
				Assert.assertEquals((Integer) 6, degrevement.getPropreUsage().getSurface());
				Assert.assertEquals(0, BigDecimal.valueOf(20L).compareTo(degrevement.getPropreUsage().getPourcentage()));
				Assert.assertNull(degrevement.getPropreUsage().getPourcentageArrete());
				Assert.assertEquals(dateOctroi, degrevement.getLoiLogement().getDateOctroi());
				Assert.assertEquals(dateEcheanceOctroi, degrevement.getLoiLogement().getDateEcheance());
				Assert.assertNull(degrevement.getLoiLogement().getPourcentageCaractereSocial());

				// vérification de la quittance du formulaire de demande
				final List<DemandeDegrevementICI> formulaires = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true);
				Assert.assertEquals(1, formulaires.size());

				final DemandeDegrevementICI formulaire = formulaires.get(0);
				Assert.assertNotNull(formulaire);
				Assert.assertEquals((Long) ids.idImmeuble, formulaire.getImmeuble().getId());
				Assert.assertFalse(formulaire.isAnnule());
				Assert.assertEquals(dateReception, formulaire.getDateRetour());
			}
		});
	}

	@Test
	public void testDonneesNonValides() throws Exception {

		final RegDate dateDebut = date(2007, 4, 1);
		final RegDate dateChargement = date(2017, 1, 7);
		final RegDate dateAchat = date(2016, 8, 3);
		final RegDate dateEnvoiFormulaire = date(2017, 1, 9);
		final int pf = 2016;
		final RegDate dateReception = RegDate.get().addDays(-2);

		final class Ids {
			final long idPM;
			final long idImmeuble;
			final int noSequence;
			public Ids(long idPM, long idImmeuble, int noSequence) {
				this.idPM = idPM;
				this.idImmeuble = idImmeuble;
				this.noSequence = noSequence;
			}
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Petite Arvine");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
		    addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		    addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

		    final PersonneMoraleRF tiersRF = addPersonneMoraleRF("Petite Arvine", null, "3478t267gfhs", 32561251L, null);
		    addRapprochementRF(entreprise, tiersRF, null, null, TypeRapprochementRF.AUTO);

		    final CommuneRF communeRF = addCommuneRF(22, "Bain bain", 8742);
		    final BienFondRF immeuble = addBienFondRF("478235z32hf", null, communeRF, 1423);
		    addDroitPersonneMoraleRF(dateChargement, dateAchat, null, "Achat", null, "57485ztfgdé",
		                             new IdentifiantAffaireRF(1234, "452"),
		                             new Fraction(1, 1),
		                             GenrePropriete.INDIVIDUELLE,
		                             tiersRF, immeuble, null);

			final DemandeDegrevementICI formulaire = addDemandeDegrevementICI(entreprise, dateEnvoiFormulaire, dateEnvoiFormulaire.addMonths(3), null, null, pf, immeuble);
			Assert.assertNotNull(formulaire);
			return new Ids(entreprise.getNumero(), immeuble.getId(), formulaire.getNumeroSequence());
		});

		// réception des données (aucune donnée valide)
		final RegDate dateOctroi = date(2016, 4, 2);
		final RegDate dateEcheanceOctroi = date(2017, 12, 1);
		final DonneesMetier donneesMetier = buildDonneesMetier(pf, ids.idPM, ids.noSequence,
		                                                       1L, false,
		                                                       2L, false,
		                                                       3L, false,
		                                                       BigDecimal.TEN, false,
		                                                       4L, false,
		                                                       5L, false,
		                                                       6L, false,
		                                                       BigDecimal.valueOf(20), false,
		                                                       true,
		                                                       dateOctroi, false,
		                                                       dateEcheanceOctroi, false);
		final Message retour = buildRetour(dateReception, donneesMetier);
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				handler.onRetourDegrevement(retour, null);
			}
		});

		// vérification en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idPM);
				Assert.assertNotNull(entreprise);

				final List<DegrevementICI> degrevements = entreprise.getAllegementsFonciers().stream()
						.filter(af -> af instanceof DegrevementICI)
						.map(af -> (DegrevementICI) af)
						.collect(Collectors.toList());
				Assert.assertEquals(1, degrevements.size());

				final DegrevementICI degrevement = degrevements.get(0);
				Assert.assertNotNull(degrevement);
				Assert.assertFalse(degrevement.isAnnule());
				Assert.assertEquals(date(pf, 1, 1), degrevement.getDateDebut());
				Assert.assertNull(degrevement.getDateFin());

				// vérification des valeurs sauvegardées (tout était invalide -> rien n'est présent)
				Assert.assertNull(degrevement.getLocation());
				Assert.assertNull(degrevement.getPropreUsage());
				Assert.assertNull(degrevement.getLoiLogement());

				// vérification de la quittance du formulaire de demande
				final List<DemandeDegrevementICI> formulaires = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true);
				Assert.assertEquals(1, formulaires.size());

				final DemandeDegrevementICI formulaire = formulaires.get(0);
				Assert.assertNotNull(formulaire);
				Assert.assertEquals((Long) ids.idImmeuble, formulaire.getImmeuble().getId());
				Assert.assertFalse(formulaire.isAnnule());
				Assert.assertEquals(dateReception, formulaire.getDateRetour());
			}
		});
	}

	@Test
	public void testRemplacementDonneesExistantes() throws Exception {

		final RegDate dateDebut = date(2007, 4, 1);
		final RegDate dateChargement = date(2017, 1, 7);
		final RegDate dateAchat = date(2016, 8, 3);
		final RegDate dateEnvoiFormulaire = date(2017, 1, 9);
		final int pf = 2016;
		final RegDate dateReception = RegDate.get().addDays(-2);

		final class Ids {
			final long idPM;
			final long idImmeuble;
			final int noSequence;
			public Ids(long idPM, long idImmeuble, int noSequence) {
				this.idPM = idPM;
				this.idImmeuble = idImmeuble;
				this.noSequence = noSequence;
			}
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Petite Arvine");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
		    addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		    addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

		    final PersonneMoraleRF tiersRF = addPersonneMoraleRF("Petite Arvine", null, "3478t267gfhs", 32561251L, null);
		    addRapprochementRF(entreprise, tiersRF, null, null, TypeRapprochementRF.AUTO);

		    final CommuneRF communeRF = addCommuneRF(22, "Bain bain", 8742);
		    final BienFondRF immeuble = addBienFondRF("478235z32hf", null, communeRF, 1423);
		    addDroitPersonneMoraleRF(dateChargement, dateAchat, null, "Achat", null, "57485ztfgdé",
		                             new IdentifiantAffaireRF(1234, "452"),
		                             new Fraction(1, 1),
		                             GenrePropriete.INDIVIDUELLE,
		                             tiersRF, immeuble, null);

		    // quelques dégrèvements pré-existants
			// le premier ne devrait pas être modifié
			addDegrevementICI(entreprise, immeuble, date(pf - 3, 1, 1), date(pf - 3, 12, 31), new DonneesUtilisation(10000, 1000, 100, BigDecimal.valueOf(100), BigDecimal.valueOf(100)), null, null);
			// le second devra être revu pour sa date de fin (qui est après le début de la PF du formulaire)
			addDegrevementICI(entreprise, immeuble, date(pf - 2, 1, 1), date(pf, 4, 12), new DonneesUtilisation(100000, 750, 75, BigDecimal.valueOf(75), BigDecimal.valueOf(75)), null, null);
			// et le troisième devrait se retrouver annulé
			addDegrevementICI(entreprise, immeuble, date(pf, 4, 13), null, new DonneesUtilisation(5, 2, 20, BigDecimal.valueOf(1), BigDecimal.valueOf(1)), null, null);

			final DemandeDegrevementICI formulaire = addDemandeDegrevementICI(entreprise, dateEnvoiFormulaire, dateEnvoiFormulaire.addMonths(3), null, null, pf, immeuble);
			Assert.assertNotNull(formulaire);
			return new Ids(entreprise.getNumero(), immeuble.getId(), formulaire.getNumeroSequence());
		});

		// réception des données
		final RegDate dateOctroi = date(2016, 4, 2);
		final RegDate dateEcheanceOctroi = date(2017, 12, 1);
		final DonneesMetier donneesMetier = buildDonneesMetier(pf, ids.idPM, ids.noSequence,
		                                                       1L, true,
		                                                       2L, true,
		                                                       3L, true,
		                                                       BigDecimal.TEN, true,
		                                                       4L, true,
		                                                       5L, true,
		                                                       6L, true,
		                                                       BigDecimal.valueOf(20), true,
		                                                       true,
		                                                       dateOctroi, true,
		                                                       dateEcheanceOctroi, true);
		final Message retour = buildRetour(dateReception, donneesMetier);
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				handler.onRetourDegrevement(retour, null);
			}
		});

		// vérification en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idPM);
				Assert.assertNotNull(entreprise);

				final List<DegrevementICI> degrevements = entreprise.getAllegementsFonciers().stream()
						.filter(af -> af instanceof DegrevementICI)
						.sorted(Comparator.comparing(AllegementFoncier::getDateDebut).thenComparingLong(AllegementFoncier::getId))
						.map(af -> (DegrevementICI) af)
						.collect(Collectors.toList());
				Assert.assertEquals(5, degrevements.size());

				{
					final DegrevementICI degrevement = degrevements.get(0);
					Assert.assertNotNull(degrevement);
					Assert.assertFalse(degrevement.isAnnule());
					Assert.assertEquals(date(pf - 3, 1, 1), degrevement.getDateDebut());
					Assert.assertEquals(date(pf - 3, 12, 31), degrevement.getDateFin());

					// vérification des valeurs présentes
					Assert.assertNotNull(degrevement.getLocation());
					Assert.assertNull(degrevement.getPropreUsage());
					Assert.assertNull(degrevement.getLoiLogement());
					Assert.assertEquals((Integer) 10000, degrevement.getLocation().getRevenu());
					Assert.assertEquals((Integer) 1000, degrevement.getLocation().getVolume());
					Assert.assertEquals((Integer) 100, degrevement.getLocation().getSurface());
					Assert.assertEquals(0, BigDecimal.valueOf(100L).compareTo(degrevement.getLocation().getPourcentage()));
					Assert.assertEquals(0, BigDecimal.valueOf(100L).compareTo(degrevement.getLocation().getPourcentageArrete()));
				}
				{
					final DegrevementICI degrevement = degrevements.get(1);
					Assert.assertNotNull(degrevement);
					Assert.assertTrue(degrevement.isAnnule());
					Assert.assertEquals(date(pf - 2, 1, 1), degrevement.getDateDebut());
					Assert.assertEquals(date(pf, 4, 12), degrevement.getDateFin());

					// vérification des valeurs présentes
					Assert.assertNotNull(degrevement.getLocation());
					Assert.assertNull(degrevement.getPropreUsage());
					Assert.assertNull(degrevement.getLoiLogement());
					Assert.assertEquals((Integer) 100000, degrevement.getLocation().getRevenu());
					Assert.assertEquals((Integer) 750, degrevement.getLocation().getVolume());
					Assert.assertEquals((Integer) 75, degrevement.getLocation().getSurface());
					Assert.assertEquals(0, BigDecimal.valueOf(75L).compareTo(degrevement.getLocation().getPourcentage()));
					Assert.assertEquals(0, BigDecimal.valueOf(75L).compareTo(degrevement.getLocation().getPourcentageArrete()));
				}
				{
					final DegrevementICI degrevement = degrevements.get(2);
					Assert.assertNotNull(degrevement);
					Assert.assertFalse(degrevement.isAnnule());
					Assert.assertEquals(date(pf - 2, 1, 1), degrevement.getDateDebut());
					Assert.assertEquals(date(pf - 1, 12, 31), degrevement.getDateFin());

					// vérification des valeurs présentes
					Assert.assertNotNull(degrevement.getLocation());
					Assert.assertNull(degrevement.getPropreUsage());
					Assert.assertNull(degrevement.getLoiLogement());
					Assert.assertEquals((Integer) 100000, degrevement.getLocation().getRevenu());
					Assert.assertEquals((Integer) 750, degrevement.getLocation().getVolume());
					Assert.assertEquals((Integer) 75, degrevement.getLocation().getSurface());
					Assert.assertEquals(0, BigDecimal.valueOf(75L).compareTo(degrevement.getLocation().getPourcentage()));
					Assert.assertEquals(0, BigDecimal.valueOf(75L).compareTo(degrevement.getLocation().getPourcentageArrete()));
				}
				{
					final DegrevementICI degrevement = degrevements.get(3);
					Assert.assertNotNull(degrevement);
					Assert.assertFalse(degrevement.isAnnule());
					Assert.assertEquals(date(pf, 1, 1), degrevement.getDateDebut());
					Assert.assertNull(degrevement.getDateFin());

					// nouvelles données
					Assert.assertNotNull(degrevement.getLocation());
					Assert.assertNotNull(degrevement.getPropreUsage());
					Assert.assertNotNull(degrevement.getLoiLogement());
					Assert.assertEquals((Integer) 1, degrevement.getLocation().getRevenu());
					Assert.assertEquals((Integer) 2, degrevement.getLocation().getVolume());
					Assert.assertEquals((Integer) 3, degrevement.getLocation().getSurface());
					Assert.assertEquals(0, BigDecimal.valueOf(10L).compareTo(degrevement.getLocation().getPourcentage()));
					Assert.assertNull(degrevement.getLocation().getPourcentageArrete());
					Assert.assertEquals((Integer) 4, degrevement.getPropreUsage().getRevenu());
					Assert.assertEquals((Integer) 5, degrevement.getPropreUsage().getVolume());
					Assert.assertEquals((Integer) 6, degrevement.getPropreUsage().getSurface());
					Assert.assertEquals(0, BigDecimal.valueOf(20L).compareTo(degrevement.getPropreUsage().getPourcentage()));
					Assert.assertNull(degrevement.getPropreUsage().getPourcentageArrete());
					Assert.assertEquals(dateOctroi, degrevement.getLoiLogement().getDateOctroi());
					Assert.assertEquals(dateEcheanceOctroi, degrevement.getLoiLogement().getDateEcheance());
					Assert.assertNull(degrevement.getLoiLogement().getPourcentageCaractereSocial());
				}
				{
					final DegrevementICI degrevement = degrevements.get(4);
					Assert.assertNotNull(degrevement);
					Assert.assertTrue(degrevement.isAnnule());
					Assert.assertEquals(date(pf, 4, 13), degrevement.getDateDebut());
					Assert.assertNull(degrevement.getDateFin());

					// vérification des valeurs présentes
					Assert.assertNotNull(degrevement.getLocation());
					Assert.assertNull(degrevement.getPropreUsage());
					Assert.assertNull(degrevement.getLoiLogement());
					Assert.assertEquals((Integer) 5, degrevement.getLocation().getRevenu());
					Assert.assertEquals((Integer) 2, degrevement.getLocation().getVolume());
					Assert.assertEquals((Integer) 20, degrevement.getLocation().getSurface());
					Assert.assertEquals(0, BigDecimal.valueOf(1L).compareTo(degrevement.getLocation().getPourcentage()));
					Assert.assertEquals(0, BigDecimal.valueOf(1L).compareTo(degrevement.getLocation().getPourcentageArrete()));
				}

				// vérification de la quittance du formulaire de demande
				final List<DemandeDegrevementICI> formulaires = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true);
				Assert.assertEquals(1, formulaires.size());

				final DemandeDegrevementICI formulaire = formulaires.get(0);
				Assert.assertNotNull(formulaire);
				Assert.assertEquals((Long) ids.idImmeuble, formulaire.getImmeuble().getId());
				Assert.assertFalse(formulaire.isAnnule());
				Assert.assertEquals(dateReception, formulaire.getDateRetour());
			}
		});
	}

	@Test
	public void testAucunChangement() throws Exception {

		final RegDate dateDebut = date(2007, 4, 1);
		final RegDate dateChargement = date(2017, 1, 7);
		final RegDate dateAchat = date(2016, 8, 3);
		final RegDate dateEnvoiFormulaire = date(2017, 1, 9);
		final int pf = 2016;
		final RegDate dateReception = RegDate.get().addDays(-2);

		final class Ids {
			final long idPM;
			final long idImmeuble;
			final int noSequence;
			public Ids(long idPM, long idImmeuble, int noSequence) {
				this.idPM = idPM;
				this.idImmeuble = idImmeuble;
				this.noSequence = noSequence;
			}
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Petite Arvine");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
		    addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
		    addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);

		    final PersonneMoraleRF tiersRF = addPersonneMoraleRF("Petite Arvine", null, "3478t267gfhs", 32561251L, null);
		    addRapprochementRF(entreprise, tiersRF, null, null, TypeRapprochementRF.AUTO);

		    final CommuneRF communeRF = addCommuneRF(22, "Bain bain", 8742);
		    final BienFondRF immeuble = addBienFondRF("478235z32hf", null, communeRF, 1423);
		    addDroitPersonneMoraleRF(dateChargement, dateAchat, null, "Achat", null, "57485ztfgdé",
		                             new IdentifiantAffaireRF(1234, "452"),
		                             new Fraction(1, 1),
		                             GenrePropriete.INDIVIDUELLE,
		                             tiersRF, immeuble, null);

		    // une valeur pré-existante
			addDegrevementICI(entreprise, immeuble, date(pf - 3, 1, 1), null, new DonneesUtilisation(10000, 1000, 100, BigDecimal.valueOf(100), BigDecimal.valueOf(100)), null, null);

			final DemandeDegrevementICI formulaire = addDemandeDegrevementICI(entreprise, dateEnvoiFormulaire, dateEnvoiFormulaire.addMonths(3), null, null, pf, immeuble);
			Assert.assertNotNull(formulaire);
			return new Ids(entreprise.getNumero(), immeuble.getId(), formulaire.getNumeroSequence());
		});

		// réception des données
		final DonneesMetier donneesMetier = buildDonneesMetier(pf, ids.idPM, ids.noSequence,
		                                                       10000L, true,
		                                                       1000L, true,
		                                                       100L, true,
		                                                       BigDecimal.valueOf(100L), true,
		                                                       null, true,
		                                                       null, true,
		                                                       null, true,
		                                                       null, true,
		                                                       false,
		                                                       null, true,
		                                                       null, true);
		final Message retour = buildRetour(dateReception, donneesMetier);
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				handler.onRetourDegrevement(retour, null);
			}
		});

		// vérification en base
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idPM);
				Assert.assertNotNull(entreprise);

				final List<DegrevementICI> degrevements = entreprise.getAllegementsFonciers().stream()
						.filter(af -> af instanceof DegrevementICI)
						.sorted(Comparator.comparing(AllegementFoncier::getDateDebut).thenComparingLong(AllegementFoncier::getId))
						.map(af -> (DegrevementICI) af)
						.collect(Collectors.toList());
				Assert.assertEquals(2, degrevements.size());

				{
					final DegrevementICI degrevement = degrevements.get(0);
					Assert.assertNotNull(degrevement);
					Assert.assertFalse(degrevement.isAnnule());
					Assert.assertEquals(date(pf - 3, 1, 1), degrevement.getDateDebut());
					Assert.assertEquals(date(pf - 1, 12, 31), degrevement.getDateFin());

					// vérification des valeurs présentes
					Assert.assertNotNull(degrevement.getLocation());
					Assert.assertNull(degrevement.getPropreUsage());
					Assert.assertNull(degrevement.getLoiLogement());
					Assert.assertEquals((Integer) 10000, degrevement.getLocation().getRevenu());
					Assert.assertEquals((Integer) 1000, degrevement.getLocation().getVolume());
					Assert.assertEquals((Integer) 100, degrevement.getLocation().getSurface());
					Assert.assertEquals(0, BigDecimal.valueOf(100L).compareTo(degrevement.getLocation().getPourcentage()));
					Assert.assertEquals(0, BigDecimal.valueOf(100L).compareTo(degrevement.getLocation().getPourcentageArrete()));
				}
				{
					final DegrevementICI degrevement = degrevements.get(1);
					Assert.assertNotNull(degrevement);
					Assert.assertFalse(degrevement.isAnnule());
					Assert.assertEquals(date(pf, 1, 1), degrevement.getDateDebut());
					Assert.assertNull(degrevement.getDateFin());

					// vérification des valeurs présentes
					Assert.assertNotNull(degrevement.getLocation());
					Assert.assertNull(degrevement.getPropreUsage());
					Assert.assertNull(degrevement.getLoiLogement());
					Assert.assertEquals((Integer) 10000, degrevement.getLocation().getRevenu());
					Assert.assertEquals((Integer) 1000, degrevement.getLocation().getVolume());
					Assert.assertEquals((Integer) 100, degrevement.getLocation().getSurface());
					Assert.assertEquals(0, BigDecimal.valueOf(100L).compareTo(degrevement.getLocation().getPourcentage()));
					Assert.assertNull(degrevement.getLocation().getPourcentageArrete());
				}

				// vérification de la quittance du formulaire de demande
				final List<DemandeDegrevementICI> formulaires = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true);
				Assert.assertEquals(1, formulaires.size());

				final DemandeDegrevementICI formulaire = formulaires.get(0);
				Assert.assertNotNull(formulaire);
				Assert.assertEquals((Long) ids.idImmeuble, formulaire.getImmeuble().getId());
				Assert.assertFalse(formulaire.isAnnule());
				Assert.assertEquals(dateReception, formulaire.getDateRetour());
			}
		});
	}
}
