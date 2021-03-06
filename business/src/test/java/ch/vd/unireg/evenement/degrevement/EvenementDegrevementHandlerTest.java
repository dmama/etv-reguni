package ch.vd.unireg.evenement.degrevement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.dperm.xml.common.v1.TypImmeuble;
import ch.vd.dperm.xml.common.v1.TypeImposition;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.foncier.AllegementFoncier;
import ch.vd.unireg.foncier.DegrevementICI;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.foncier.DonneesLoiLogement;
import ch.vd.unireg.foncier.DonneesUtilisation;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.MineRF;
import ch.vd.unireg.registrefoncier.PartCoproprieteRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.ProprieteParEtageRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.TypeRapprochementRF;
import ch.vd.unireg.xml.degrevement.quittance.v1.QuittanceIntegrationMetierImmDetails;
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

public class EvenementDegrevementHandlerTest extends BusinessTest {

	private EvenementDegrevementHandler handler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final EvenementDegrevementHandlerImpl impl = new EvenementDegrevementHandlerImpl();
		impl.setTiersService(tiersService);
		impl.setRegistreFoncierService(getBean(RegistreFoncierService.class, "serviceRF"));
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

	/**
	 * Cas d'une identification de formulaire de demande de dégrèvement qui ne trouve rien
	 */
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

			final CommuneRF communeRF = addCommuneRF(22, MockCommune.Aigle.getNomOfficiel(), MockCommune.Aigle.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("478235z32hf", null, communeRF, 1423);
			addDroitPersonneMoraleRF(dateChargement, dateAchat, null, null, "Achat", null, "57485ztfgdé",
			                         "57485ztfgdd", new IdentifiantAffaireRF(1234, "452"),
			                         new Fraction(1, 1),
			                         GenrePropriete.INDIVIDUELLE,
			                         tiersRF, immeuble, null);
			addEstimationFiscale(null, dateAchat, null, false, 1234L, String.valueOf(dateAchat.year()), immeuble);
			addSurfaceAuSol(null, null, 100, "Chemin", immeuble);

			return entreprise.getNumero().intValue();
		});

		// réception des données
		final Message retour = buildRetour(2016, idpm, 4321L, RegDate.get());
		doInNewTransactionAndSession(status -> {
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
			return null;
		});

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get((long) idpm);
			Assert.assertNotNull(entreprise);

			final List<DegrevementICI> degrevements = entreprise.getAllegementsFonciers().stream()
					.filter(DegrevementICI.class::isInstance)
					.map(DegrevementICI.class::cast)
					.collect(Collectors.toList());
			Assert.assertEquals(0, degrevements.size());
			return null;
		});
	}

	/**
	 * Cas simple avec identification correcte du formulaire de demande de dégrèvement
	 */
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

			final CommuneRF communeRF = addCommuneRF(22, MockCommune.Aigle.getNomOfficiel(), MockCommune.Aigle.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("478235z32hf", null, communeRF, 1423);
			addDroitPersonneMoraleRF(dateChargement, dateAchat, null, null, "Achat", null, "57485ztfgdé",
			                         "57485ztfgdd", new IdentifiantAffaireRF(1234, "452"),
			                         new Fraction(1, 1),
			                         GenrePropriete.INDIVIDUELLE,
			                         tiersRF, immeuble, null);
			addEstimationFiscale(dateChargement, dateAchat, null, false, 1234L, String.valueOf(dateAchat.year()), immeuble);
			addSurfaceAuSol(null, null, 100, "Chemin", immeuble);

			final DemandeDegrevementICI formulaire = addDemandeDegrevementICI(entreprise, pf, immeuble);
			addDelaiAutreDocumentFiscal(formulaire, dateEnvoiFormulaire, dateEnvoiFormulaire.addMonths(3), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(formulaire, dateEnvoiFormulaire);
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
		final QuittanceIntegrationMetierImmDetails quittance = doInNewTransactionAndSession(status -> handler.onRetourDegrevement(retour, null));

		// vérification des données retournées
		Assert.assertNotNull(quittance);
		Assert.assertEquals(BigDecimal.valueOf(1234L), quittance.getEstimationFiscale());
		Assert.assertEquals("Chemin", quittance.getNatureImmeuble());
		Assert.assertNotNull(quittance.getCommune());
		Assert.assertEquals(MockCommune.Aigle.getNomOfficiel(), quittance.getCommune().getLibelleCommune());
		Assert.assertEquals(BigInteger.valueOf(MockCommune.Aigle.getNoOFS()), quittance.getCommune().getNumeroOfsCommune());
		Assert.assertEquals((int) ids.idPM, quittance.getNumeroContribuable());
		Assert.assertEquals("1423", quittance.getNumeroParcelle());
		Assert.assertEquals(TypImmeuble.B_F, quittance.getTypeImmeuble());
		Assert.assertEquals(TypeImposition.IMPOT_COMPLEMENTAIRE_IMMEUBLE, quittance.getTypeImpot());
		Assert.assertTrue(quittance.isTraitementMetier());

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idPM);
			Assert.assertNotNull(entreprise);

			final List<DegrevementICI> degrevements = entreprise.getAllegementsFonciers().stream()
					.filter(DegrevementICI.class::isInstance)
					.map(DegrevementICI.class::cast)
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
			Assert.assertEquals((Long) 1L, degrevement.getLocation().getRevenu());
			Assert.assertEquals((Long) 2L, degrevement.getLocation().getVolume());
			Assert.assertEquals((Long) 3L, degrevement.getLocation().getSurface());
			Assert.assertEquals(0, BigDecimal.valueOf(10L).compareTo(degrevement.getLocation().getPourcentage()));
			Assert.assertNull(degrevement.getLocation().getPourcentageArrete());
			Assert.assertEquals((Long) 4L, degrevement.getPropreUsage().getRevenu());
			Assert.assertEquals((Long) 5L, degrevement.getPropreUsage().getVolume());
			Assert.assertEquals((Long) 6L, degrevement.getPropreUsage().getSurface());
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
			return null;
		});
	}

	/**
	 * Cas d'un formulaire bien identifié mais avec des données qui sortent de leur domaine de validité
	 */
	@Test
	public void testFormulaireDemandeExistantEtDonneesHorsPlage() throws Exception {

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

			final CommuneRF communeRF = addCommuneRF(22, MockCommune.Aigle.getNomOfficiel(), MockCommune.Aigle.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("478235z32hf", null, communeRF, 1423);
			addDroitPersonneMoraleRF(dateChargement, dateAchat, null, null, "Achat", null, "57485ztfgdé",
			                         "57485ztfgdd", new IdentifiantAffaireRF(1234, "452"),
			                         new Fraction(1, 1),
			                         GenrePropriete.INDIVIDUELLE,
			                         tiersRF, immeuble, null);
			addEstimationFiscale(dateChargement, dateAchat, null, false, 1234L, String.valueOf(dateAchat.year()), immeuble);
			addSurfaceAuSol(null, null, 100, "Chemin", immeuble);

			final DemandeDegrevementICI formulaire = addDemandeDegrevementICI(entreprise, pf, immeuble);
			addDelaiAutreDocumentFiscal(formulaire, dateEnvoiFormulaire, dateEnvoiFormulaire.addMonths(3), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(formulaire, dateEnvoiFormulaire);
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
		                                                       BigDecimal.valueOf(-20), true,       // pourcentage négatif -> devrait être refusé !
		                                                       true,
		                                                       dateOctroi, true,
		                                                       dateEcheanceOctroi, true);
		final Message retour = buildRetour(dateReception, donneesMetier);
		doInNewTransactionAndSession(status -> {
			handler.onRetourDegrevement(retour, null);
			return null;
		});

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idPM);
			Assert.assertNotNull(entreprise);

			// vérification de l'absence de dégrèvement entré
			final List<DegrevementICI> degrevements = entreprise.getAllegementsFonciers().stream()
					.filter(DegrevementICI.class::isInstance)
					.map(DegrevementICI.class::cast)
					.collect(Collectors.toList());
			Assert.assertEquals(1, degrevements.size());

			final DegrevementICI degrevement = degrevements.get(0);
			Assert.assertNotNull(degrevement);
			Assert.assertFalse(degrevement.isAnnule());
			Assert.assertEquals(date(pf, 1, 1), degrevement.getDateDebut());
			Assert.assertNull(degrevement.getDateFin());

			// vérification des valeurs sauvegardées (valeur invalide -> rien n'a été conservé)
			Assert.assertNull(degrevement.getLocation());
			Assert.assertNull(degrevement.getPropreUsage());
			Assert.assertNotNull(degrevement.getLoiLogement());
			Assert.assertFalse(degrevement.getLoiLogement().getControleOfficeLogement());
			Assert.assertNull(degrevement.getLoiLogement().getDateEcheance());
			Assert.assertNull(degrevement.getLoiLogement().getDateOctroi());
			Assert.assertNull(degrevement.getLoiLogement().getPourcentageCaractereSocial());
			Assert.assertTrue(degrevement.getNonIntegrable());

			// vérification de la quittance du formulaire de demande (= doit avoir eu lieu quand-même...)
			final List<DemandeDegrevementICI> formulaires = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true);
			Assert.assertEquals(1, formulaires.size());

			final DemandeDegrevementICI formulaire = formulaires.get(0);
			Assert.assertNotNull(formulaire);
			Assert.assertEquals((Long) ids.idImmeuble, formulaire.getImmeuble().getId());
			Assert.assertFalse(formulaire.isAnnule());
			Assert.assertEquals(dateReception, formulaire.getDateRetour());
			return null;
		});
	}

	/**
	 * Cas des données toutes indiquées comme "invalides" au vidéo-codage
	 */
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

			final CommuneRF communeRF = addCommuneRF(22, MockCommune.Aigle.getNomOfficiel(), MockCommune.Aigle.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("478235z32hf", null, communeRF, 1423);
			addDroitPersonneMoraleRF(dateChargement, dateAchat, null, null, "Achat", null, "57485ztfgdé",
			                         "57485ztfgdd", new IdentifiantAffaireRF(1234, "452"),
			                         new Fraction(1, 1),
			                         GenrePropriete.INDIVIDUELLE,
			                         tiersRF, immeuble, null);
			addEstimationFiscale(dateChargement, dateAchat, null, false, 1234L, String.valueOf(dateAchat.year()), immeuble);
			addSurfaceAuSol(null, null, 100, "Chemin", immeuble);

			final DemandeDegrevementICI formulaire = addDemandeDegrevementICI(entreprise, pf, immeuble);
			addDelaiAutreDocumentFiscal(formulaire, dateEnvoiFormulaire, dateEnvoiFormulaire.addMonths(3), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(formulaire, dateEnvoiFormulaire);
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
		final QuittanceIntegrationMetierImmDetails quittance = doInNewTransactionAndSession(status -> handler.onRetourDegrevement(retour, null));

		// vérification des données retournées
		Assert.assertNotNull(quittance);
		Assert.assertEquals(BigDecimal.valueOf(1234L), quittance.getEstimationFiscale());
		Assert.assertEquals("Chemin", quittance.getNatureImmeuble());
		Assert.assertNotNull(quittance.getCommune());
		Assert.assertEquals(MockCommune.Aigle.getNomOfficiel(), quittance.getCommune().getLibelleCommune());
		Assert.assertEquals(BigInteger.valueOf(MockCommune.Aigle.getNoOFS()), quittance.getCommune().getNumeroOfsCommune());
		Assert.assertEquals((int) ids.idPM, quittance.getNumeroContribuable());
		Assert.assertEquals("1423", quittance.getNumeroParcelle());
		Assert.assertEquals(TypImmeuble.B_F, quittance.getTypeImmeuble());
		Assert.assertEquals(TypeImposition.IMPOT_COMPLEMENTAIRE_IMMEUBLE, quittance.getTypeImpot());
		Assert.assertTrue(quittance.isTraitementMetier());

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idPM);
			Assert.assertNotNull(entreprise);

			final List<DegrevementICI> degrevements = entreprise.getAllegementsFonciers().stream()
					.filter(DegrevementICI.class::isInstance)
					.map(DegrevementICI.class::cast)
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
			Assert.assertNotNull(degrevement.getLoiLogement());
			Assert.assertFalse(degrevement.getLoiLogement().getControleOfficeLogement());
			Assert.assertNull(degrevement.getLoiLogement().getDateEcheance());
			Assert.assertNull(degrevement.getLoiLogement().getDateOctroi());
			Assert.assertNull(degrevement.getLoiLogement().getPourcentageCaractereSocial());
			Assert.assertTrue(degrevement.getNonIntegrable());

			// vérification de la quittance du formulaire de demande
			final List<DemandeDegrevementICI> formulaires = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true);
			Assert.assertEquals(1, formulaires.size());

			final DemandeDegrevementICI formulaire = formulaires.get(0);
			Assert.assertNotNull(formulaire);
			Assert.assertEquals((Long) ids.idImmeuble, formulaire.getImmeuble().getId());
			Assert.assertFalse(formulaire.isAnnule());
			Assert.assertEquals(dateReception, formulaire.getDateRetour());
			return null;
		});
	}

	/**
	 * Cas d'une date complètement hors de la plage de validité (1291 - 2400)
	 */
	@Test
	public void testDateHorsPlageValidite() throws Exception {

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

			final CommuneRF communeRF = addCommuneRF(22, MockCommune.Aigle.getNomOfficiel(), MockCommune.Aigle.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("478235z32hf", null, communeRF, 1423);
			addDroitPersonneMoraleRF(dateChargement, dateAchat, null, null, "Achat", null, "57485ztfgdé",
			                         "57485ztfgdd", new IdentifiantAffaireRF(1234, "452"),
			                         new Fraction(1, 1),
			                         GenrePropriete.INDIVIDUELLE,
			                         tiersRF, immeuble, null);
			addEstimationFiscale(dateChargement, dateAchat, null, false, 1234L, String.valueOf(dateAchat.year()), immeuble);
			addSurfaceAuSol(null, null, 100, "Chemin", immeuble);

			final DemandeDegrevementICI formulaire = addDemandeDegrevementICI(entreprise, pf, immeuble);
			addDelaiAutreDocumentFiscal(formulaire, dateEnvoiFormulaire, dateEnvoiFormulaire.addMonths(3), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(formulaire, dateEnvoiFormulaire);
			Assert.assertNotNull(formulaire);
			return new Ids(entreprise.getNumero(), immeuble.getId(), formulaire.getNumeroSequence());
		});

		// réception des données (aucune donnée valide)
		final RegDate dateOctroi = date(16, 4, 2);                  // 16 au lieu de 2016....
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
		final QuittanceIntegrationMetierImmDetails quittance = doInNewTransactionAndSession(status -> handler.onRetourDegrevement(retour, null));

		// vérification des données retournées
		Assert.assertNotNull(quittance);
		Assert.assertEquals(BigDecimal.valueOf(1234L), quittance.getEstimationFiscale());
		Assert.assertEquals("Chemin", quittance.getNatureImmeuble());
		Assert.assertNotNull(quittance.getCommune());
		Assert.assertEquals(MockCommune.Aigle.getNomOfficiel(), quittance.getCommune().getLibelleCommune());
		Assert.assertEquals(BigInteger.valueOf(MockCommune.Aigle.getNoOFS()), quittance.getCommune().getNumeroOfsCommune());
		Assert.assertEquals((int) ids.idPM, quittance.getNumeroContribuable());
		Assert.assertEquals("1423", quittance.getNumeroParcelle());
		Assert.assertEquals(TypImmeuble.B_F, quittance.getTypeImmeuble());
		Assert.assertEquals(TypeImposition.IMPOT_COMPLEMENTAIRE_IMMEUBLE, quittance.getTypeImpot());
		Assert.assertTrue(quittance.isTraitementMetier());

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idPM);
			Assert.assertNotNull(entreprise);

			final List<DegrevementICI> degrevements = entreprise.getAllegementsFonciers().stream()
					.filter(DegrevementICI.class::isInstance)
					.map(DegrevementICI.class::cast)
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
			Assert.assertNotNull(degrevement.getLoiLogement());
			Assert.assertFalse(degrevement.getLoiLogement().getControleOfficeLogement());
			Assert.assertNull(degrevement.getLoiLogement().getDateEcheance());
			Assert.assertNull(degrevement.getLoiLogement().getDateOctroi());
			Assert.assertNull(degrevement.getLoiLogement().getPourcentageCaractereSocial());
			Assert.assertTrue(degrevement.getNonIntegrable());

			// vérification de la quittance du formulaire de demande
			final List<DemandeDegrevementICI> formulaires = entreprise.getAutresDocumentsFiscaux(DemandeDegrevementICI.class, false, true);
			Assert.assertEquals(1, formulaires.size());

			final DemandeDegrevementICI formulaire = formulaires.get(0);
			Assert.assertNotNull(formulaire);
			Assert.assertEquals((Long) ids.idImmeuble, formulaire.getImmeuble().getId());
			Assert.assertFalse(formulaire.isAnnule());
			Assert.assertEquals(dateReception, formulaire.getDateRetour());
			return null;
		});
	}

	/**
	 * Etude de l'impact sur les données de dégrèvement déjà existantes
	 */
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

			final CommuneRF communeRF = addCommuneRF(22, MockCommune.Aigle.getNomOfficiel(), MockCommune.Aigle.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("478235z32hf", null, communeRF, 1423);
			addDroitPersonneMoraleRF(dateChargement, dateAchat, null, null, "Achat", null, "57485ztfgdé",
			                         "57485ztfgdd", new IdentifiantAffaireRF(1234, "452"),
			                         new Fraction(1, 1),
			                         GenrePropriete.INDIVIDUELLE,
			                         tiersRF, immeuble, null);
			addEstimationFiscale(dateChargement, dateAchat, null, false, 1234L, String.valueOf(dateAchat.year()), immeuble);
			addSurfaceAuSol(null, null, 100, "Chemin", immeuble);

			// quelques dégrèvements pré-existants
			// le premier ne devrait pas être modifié
			addDegrevementICI(entreprise, immeuble, pf - 3, pf - 3, new DonneesUtilisation(10000L, 1000L, 100L, BigDecimal.valueOf(100), BigDecimal.valueOf(100)), null, new DonneesLoiLogement(false, null, null, null));
			// le second devra être revu pour sa date de fin (qui est après le début de la PF du formulaire)
			addDegrevementICI(entreprise, immeuble, pf - 2, pf, new DonneesUtilisation(100000L, 750L, 75L, BigDecimal.valueOf(75), BigDecimal.valueOf(75)), null, new DonneesLoiLogement(false, null, null, null));
			// et le troisième devrait se retrouver annulé
			addDegrevementICI(entreprise, immeuble, pf + 1, null, new DonneesUtilisation(5L, 2L, 20L, BigDecimal.valueOf(1), BigDecimal.valueOf(1)), null, new DonneesLoiLogement(false, null, null, null));

			final DemandeDegrevementICI formulaire = addDemandeDegrevementICI(entreprise, pf, immeuble);
			addDelaiAutreDocumentFiscal(formulaire, dateEnvoiFormulaire, dateEnvoiFormulaire.addMonths(3), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(formulaire, dateEnvoiFormulaire);
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
		final QuittanceIntegrationMetierImmDetails quittance = doInNewTransactionAndSession(status -> handler.onRetourDegrevement(retour, null));

		// vérification des données retournées
		Assert.assertNotNull(quittance);
		Assert.assertEquals(BigDecimal.valueOf(1234L), quittance.getEstimationFiscale());
		Assert.assertEquals("Chemin", quittance.getNatureImmeuble());
		Assert.assertNotNull(quittance.getCommune());
		Assert.assertEquals(MockCommune.Aigle.getNomOfficiel(), quittance.getCommune().getLibelleCommune());
		Assert.assertEquals(BigInteger.valueOf(MockCommune.Aigle.getNoOFS()), quittance.getCommune().getNumeroOfsCommune());
		Assert.assertEquals((int) ids.idPM, quittance.getNumeroContribuable());
		Assert.assertEquals("1423", quittance.getNumeroParcelle());
		Assert.assertEquals(TypImmeuble.B_F, quittance.getTypeImmeuble());
		Assert.assertEquals(TypeImposition.IMPOT_COMPLEMENTAIRE_IMMEUBLE, quittance.getTypeImpot());
		Assert.assertTrue(quittance.isTraitementMetier());

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idPM);
			Assert.assertNotNull(entreprise);

			final List<DegrevementICI> degrevements = entreprise.getAllegementsFonciers().stream()
					.filter(DegrevementICI.class::isInstance)
					.sorted(Comparator.comparing(AllegementFoncier::getDateDebut).thenComparingLong(AllegementFoncier::getId))
					.map(DegrevementICI.class::cast)
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
				Assert.assertNotNull(degrevement.getLoiLogement());
				Assert.assertFalse(degrevement.getLoiLogement().getControleOfficeLogement());
				Assert.assertNull(degrevement.getLoiLogement().getDateEcheance());
				Assert.assertNull(degrevement.getLoiLogement().getDateOctroi());
				Assert.assertNull(degrevement.getLoiLogement().getPourcentageCaractereSocial());
				Assert.assertFalse(degrevement.getNonIntegrable());
				Assert.assertEquals((Long) 10000L, degrevement.getLocation().getRevenu());
				Assert.assertEquals((Long) 1000L, degrevement.getLocation().getVolume());
				Assert.assertEquals((Long) 100L, degrevement.getLocation().getSurface());
				Assert.assertEquals(0, BigDecimal.valueOf(100L).compareTo(degrevement.getLocation().getPourcentage()));
				Assert.assertEquals(0, BigDecimal.valueOf(100L).compareTo(degrevement.getLocation().getPourcentageArrete()));
			}
			{
				final DegrevementICI degrevement = degrevements.get(1);
				Assert.assertNotNull(degrevement);
				Assert.assertTrue(degrevement.isAnnule());
				Assert.assertEquals(date(pf - 2, 1, 1), degrevement.getDateDebut());
				Assert.assertEquals(date(pf, 12, 31), degrevement.getDateFin());

				// vérification des valeurs présentes
				Assert.assertNotNull(degrevement.getLocation());
				Assert.assertNull(degrevement.getPropreUsage());
				Assert.assertNotNull(degrevement.getLoiLogement());
				Assert.assertFalse(degrevement.getLoiLogement().getControleOfficeLogement());
				Assert.assertNull(degrevement.getLoiLogement().getDateEcheance());
				Assert.assertNull(degrevement.getLoiLogement().getDateOctroi());
				Assert.assertNull(degrevement.getLoiLogement().getPourcentageCaractereSocial());
				Assert.assertFalse(degrevement.getNonIntegrable());
				Assert.assertEquals((Long) 100000L, degrevement.getLocation().getRevenu());
				Assert.assertEquals((Long) 750L, degrevement.getLocation().getVolume());
				Assert.assertEquals((Long) 75L, degrevement.getLocation().getSurface());
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
				Assert.assertNotNull(degrevement.getLoiLogement());
				Assert.assertFalse(degrevement.getLoiLogement().getControleOfficeLogement());
				Assert.assertNull(degrevement.getLoiLogement().getDateEcheance());
				Assert.assertNull(degrevement.getLoiLogement().getDateOctroi());
				Assert.assertNull(degrevement.getLoiLogement().getPourcentageCaractereSocial());
				Assert.assertFalse(degrevement.getNonIntegrable());
				Assert.assertEquals((Long) 100000L, degrevement.getLocation().getRevenu());
				Assert.assertEquals((Long) 750L, degrevement.getLocation().getVolume());
				Assert.assertEquals((Long) 75L, degrevement.getLocation().getSurface());
				Assert.assertEquals(0, BigDecimal.valueOf(75L).compareTo(degrevement.getLocation().getPourcentage()));
				Assert.assertEquals(0, BigDecimal.valueOf(75L).compareTo(degrevement.getLocation().getPourcentageArrete()));
			}
			{
				final DegrevementICI degrevement = degrevements.get(3);
				Assert.assertNotNull(degrevement);
				Assert.assertFalse(degrevement.isAnnule());
				Assert.assertEquals(date(pf, 1, 1), degrevement.getDateDebut());
				Assert.assertEquals(date(pf, 12, 31), degrevement.getDateFin());

				// nouvelles données
				Assert.assertNotNull(degrevement.getLocation());
				Assert.assertNotNull(degrevement.getPropreUsage());
				Assert.assertNotNull(degrevement.getLoiLogement());
				Assert.assertEquals((Long) 1L, degrevement.getLocation().getRevenu());
				Assert.assertEquals((Long) 2L, degrevement.getLocation().getVolume());
				Assert.assertEquals((Long) 3L, degrevement.getLocation().getSurface());
				Assert.assertEquals(0, BigDecimal.valueOf(10L).compareTo(degrevement.getLocation().getPourcentage()));
				Assert.assertNull(degrevement.getLocation().getPourcentageArrete());
				Assert.assertEquals((Long) 4L, degrevement.getPropreUsage().getRevenu());
				Assert.assertEquals((Long) 5L, degrevement.getPropreUsage().getVolume());
				Assert.assertEquals((Long) 6L, degrevement.getPropreUsage().getSurface());
				Assert.assertEquals(0, BigDecimal.valueOf(20L).compareTo(degrevement.getPropreUsage().getPourcentage()));
				Assert.assertNull(degrevement.getPropreUsage().getPourcentageArrete());
				Assert.assertTrue(degrevement.getLoiLogement().getControleOfficeLogement());
				Assert.assertEquals(dateOctroi, degrevement.getLoiLogement().getDateOctroi());
				Assert.assertEquals(dateEcheanceOctroi, degrevement.getLoiLogement().getDateEcheance());
				Assert.assertNull(degrevement.getLoiLogement().getPourcentageCaractereSocial());
				Assert.assertFalse(degrevement.getNonIntegrable());
			}
			{
				final DegrevementICI degrevement = degrevements.get(4);
				Assert.assertNotNull(degrevement);
				Assert.assertFalse(degrevement.isAnnule());
				Assert.assertEquals(date(pf + 1, 1, 1), degrevement.getDateDebut());
				Assert.assertNull(degrevement.getDateFin());

				// vérification des valeurs présentes
				Assert.assertNotNull(degrevement.getLocation());
				Assert.assertNull(degrevement.getPropreUsage());
				Assert.assertNotNull(degrevement.getLoiLogement());
				Assert.assertFalse(degrevement.getLoiLogement().getControleOfficeLogement());
				Assert.assertNull(degrevement.getLoiLogement().getDateEcheance());
				Assert.assertNull(degrevement.getLoiLogement().getDateOctroi());
				Assert.assertNull(degrevement.getLoiLogement().getPourcentageCaractereSocial());
				Assert.assertFalse(degrevement.getNonIntegrable());
				Assert.assertEquals((Long) 5L, degrevement.getLocation().getRevenu());
				Assert.assertEquals((Long) 2L, degrevement.getLocation().getVolume());
				Assert.assertEquals((Long) 20L, degrevement.getLocation().getSurface());
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
			return null;
		});
	}

	/**
	 * Cas sans aucun changement de valeur... pour l'instant, on crée tout de même une nouvelle instance
	 */
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

			final CommuneRF communeRF = addCommuneRF(22, MockCommune.Aigle.getNomOfficiel(), MockCommune.Aigle.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("478235z32hf", null, communeRF, 1423);
			addDroitPersonneMoraleRF(dateChargement, dateAchat, null, null, "Achat", null, "57485ztfgdé",
			                         "57485ztfgdd", new IdentifiantAffaireRF(1234, "452"),
			                         new Fraction(1, 1),
			                         GenrePropriete.INDIVIDUELLE,
			                         tiersRF, immeuble, null);
			addEstimationFiscale(dateChargement, dateAchat, null, false, 1234L, String.valueOf(dateAchat.year()), immeuble);
			addSurfaceAuSol(null, null, 100, "Chemin", immeuble);

			// une valeur pré-existante
			addDegrevementICI(entreprise, immeuble, pf - 3, null, new DonneesUtilisation(10000L, 1000L, 100L, BigDecimal.valueOf(100), BigDecimal.valueOf(100)), null, null);

			final DemandeDegrevementICI formulaire = addDemandeDegrevementICI(entreprise, pf, immeuble);
			addDelaiAutreDocumentFiscal(formulaire, dateEnvoiFormulaire, dateEnvoiFormulaire.addMonths(3), EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(formulaire, dateEnvoiFormulaire);
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
		final QuittanceIntegrationMetierImmDetails quittance = doInNewTransactionAndSession(status -> handler.onRetourDegrevement(retour, null));

		// vérification des données retournées
		Assert.assertNotNull(quittance);
		Assert.assertEquals(BigDecimal.valueOf(1234L), quittance.getEstimationFiscale());
		Assert.assertEquals("Chemin", quittance.getNatureImmeuble());
		Assert.assertNotNull(quittance.getCommune());
		Assert.assertEquals(MockCommune.Aigle.getNomOfficiel(), quittance.getCommune().getLibelleCommune());
		Assert.assertEquals(BigInteger.valueOf(MockCommune.Aigle.getNoOFS()), quittance.getCommune().getNumeroOfsCommune());
		Assert.assertEquals((int) ids.idPM, quittance.getNumeroContribuable());
		Assert.assertEquals("1423", quittance.getNumeroParcelle());
		Assert.assertEquals(TypImmeuble.B_F, quittance.getTypeImmeuble());
		Assert.assertEquals(TypeImposition.IMPOT_COMPLEMENTAIRE_IMMEUBLE, quittance.getTypeImpot());
		Assert.assertTrue(quittance.isTraitementMetier());

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.idPM);
			Assert.assertNotNull(entreprise);

			final List<DegrevementICI> degrevements = entreprise.getAllegementsFonciers().stream()
					.filter(DegrevementICI.class::isInstance)
					.sorted(Comparator.comparing(AllegementFoncier::getDateDebut).thenComparingLong(AllegementFoncier::getId))
					.map(DegrevementICI.class::cast)
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
				Assert.assertEquals((Long) 10000L, degrevement.getLocation().getRevenu());
				Assert.assertEquals((Long) 1000L, degrevement.getLocation().getVolume());
				Assert.assertEquals((Long) 100L, degrevement.getLocation().getSurface());
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
				Assert.assertNotNull(degrevement.getLoiLogement());
				Assert.assertFalse(degrevement.getLoiLogement().getControleOfficeLogement());
				Assert.assertNull(degrevement.getLoiLogement().getDateEcheance());
				Assert.assertNull(degrevement.getLoiLogement().getDateOctroi());
				Assert.assertNull(degrevement.getLoiLogement().getPourcentageCaractereSocial());
				Assert.assertFalse(degrevement.getNonIntegrable());
				Assert.assertEquals((Long) 10000L, degrevement.getLocation().getRevenu());
				Assert.assertEquals((Long) 1000L, degrevement.getLocation().getVolume());
				Assert.assertEquals((Long) 100L, degrevement.getLocation().getSurface());
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
			return null;
		});
	}

	@Test
	public void testGetTypeImmeuble() throws Exception {
		Assert.assertEquals(TypImmeuble.PPE, EvenementDegrevementHandlerImpl.getTypeImmeuble(new ProprieteParEtageRF()));
		Assert.assertEquals(TypImmeuble.DDP, EvenementDegrevementHandlerImpl.getTypeImmeuble(new DroitDistinctEtPermanentRF()));
		Assert.assertEquals(TypImmeuble.MINE, EvenementDegrevementHandlerImpl.getTypeImmeuble(new MineRF()));
		Assert.assertEquals(TypImmeuble.B_F, EvenementDegrevementHandlerImpl.getTypeImmeuble(new BienFondsRF()));
		Assert.assertEquals(TypImmeuble.COP, EvenementDegrevementHandlerImpl.getTypeImmeuble(new PartCoproprieteRF()));
	}
}
