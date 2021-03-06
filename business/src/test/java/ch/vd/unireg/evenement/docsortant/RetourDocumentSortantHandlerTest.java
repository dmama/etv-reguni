package ch.vd.unireg.evenement.docsortant;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationSommee;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.documentfiscal.LettreBienvenue;
import ch.vd.unireg.editique.ConstantesEditique;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.efacture.DocumentEFacture;
import ch.vd.unireg.efacture.DocumentEFactureDAO;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.entreprise.mock.MockEntrepriseConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeLettreBienvenue;
import ch.vd.unireg.xml.event.docsortant.retour.v3.DocumentQuittance;
import ch.vd.unireg.xml.event.docsortant.retour.v3.FileExtension;
import ch.vd.unireg.xml.event.docsortant.retour.v3.FoldersArchive;
import ch.vd.unireg.xml.event.docsortant.retour.v3.Quittance;

public class RetourDocumentSortantHandlerTest extends BusinessTest {

	private RetourDocumentSortantHandler handler;
	private DocumentEFactureDAO documentEFactureDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		documentEFactureDAO = getBean(DocumentEFactureDAO.class, "documentEFactureDAO");

		final RetourDocumentSortantHandlerImpl impl = new RetourDocumentSortantHandlerImpl();
		impl.setHibernateTemplate(hibernateTemplate);
		impl.setDocumentEFactureDAO(documentEFactureDAO);
		impl.afterPropertiesSet();
		handler = impl;
	}

	private static Quittance buildQuittance(String identifiantRepelecDossier, long idContribuable, TypeDocumentEditique typeDocument, String cleArchivage) {
		final FoldersArchive archive = new FoldersArchive(ConstantesEditique.TYPE_DOSSIER_ARCHIVAGE,
		                                                  FormatNumeroHelper.numeroCTBToDisplay(idContribuable),
		                                                  typeDocument.getCodeDocumentArchivage(),
		                                                  cleArchivage);
		final DocumentQuittance quittance = new DocumentQuittance(identifiantRepelecDossier, UUID.randomUUID().toString(), UUID.randomUUID().toString(), FileExtension.PDF, archive);
		final List<DocumentQuittance> list = Collections.singletonList(quittance);
		final Quittance.DocumentsQuittances documents = new Quittance.DocumentsQuittances(list);
		return new Quittance(XmlUtils.date2xmlcal(DateHelper.getCurrentDate()), documents);
	}

	private static Map<String, String> buildCustomAttributeMap(@Nullable TypeDocumentSortant typeDocument, @Nullable String id) {
		return Stream.of(Pair.of(RetourDocumentSortantHandler.TYPE_DOCUMENT_HEADER_NAME, typeDocument),
		                 Pair.of(RetourDocumentSortantHandler.ID_ENTITE_DOCUMENT_ANNONCE_HEADER_NAME, id))
				.filter(pair -> pair.getRight() != null)
				.collect(Collectors.toMap(Pair::getLeft,
				                          pair -> pair.getRight().toString()));
	}

	@Test
	public void testReceptionSansMetaDonnees() throws Exception {

		final RegDate dateDebut = date(2000, 6, 12);
		final RegDate dateEnvoiDocument = RegDate.get();
		final RegDate delaiRetour = dateEnvoiDocument.addMonths(3);

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				// rien ni personne
			}
		});

		final class Ids {
			long idContribuable;
			long idDocument;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {

			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala SARL");
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);     // chaque année, au 31.12

			final LettreBienvenue lb = addLettreBienvenue(entreprise, TypeLettreBienvenue.VD_RC);
			addDelaiAutreDocumentFiscal(lb, dateEnvoiDocument, delaiRetour, EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(lb, dateEnvoiDocument);
			final Ids res = new Ids();
			res.idContribuable = entreprise.getNumero();
			res.idDocument = lb.getId();
			return res;
		});

		// vérification que la clé de visualisation externe du document est vide
		doInNewTransactionAndSession(status -> {
			final LettreBienvenue lb = hibernateTemplate.get(LettreBienvenue.class, ids.idDocument);
			Assert.assertNotNull(lb);
			Assert.assertNull(lb.getCleDocument());
			Assert.assertNull(lb.getCleDocumentRappel());
			return null;
		});

		// génération de la quittance
		final Quittance quittance = buildQuittance("Mon identifiant bien senti", ids.idContribuable, TypeDocumentEditique.LETTRE_BIENVENUE, "Une clé d'archivage");

		try {
			doInNewTransactionAndSession(status -> {
				handler.onQuittance(quittance, Collections.emptyMap());
				Assert.fail("Le quittancement aurait dû être refusé avec une exception...");
				return null;
			});
		}
		catch (EsbBusinessException e) {
			Assert.assertEquals("Meta-donnée(s) manquante(s) : typeDocumentAnnonce, idDocumentAnnonce", e.getMessage());
		}

		// vérification du résultat en base -> rien
		doInNewTransactionAndSession(status -> {
			final LettreBienvenue lb = hibernateTemplate.get(LettreBienvenue.class, ids.idDocument);
			Assert.assertNotNull(lb);
			Assert.assertNull(lb.getCleDocument());
			Assert.assertNull(lb.getCleDocumentRappel());
			return null;
		});
	}

	@Test
	public void testReceptionPourEnvoiAutreDocumentFiscal() throws Exception {

		final RegDate dateDebut = date(2000, 6, 12);
		final RegDate dateEnvoiDocument = RegDate.get();
		final RegDate delaiRetour = dateEnvoiDocument.addMonths(3);

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				// rien ni personne
			}
		});

		final class Ids {
			long idContribuable;
			long idDocument;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {

			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala SARL");
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);     // chaque année, au 31.12

			final LettreBienvenue lb = addLettreBienvenue(entreprise, TypeLettreBienvenue.VD_RC);
			addDelaiAutreDocumentFiscal(lb, dateEnvoiDocument, delaiRetour, EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(lb, dateEnvoiDocument);
			final Ids res = new Ids();
			res.idContribuable = entreprise.getNumero();
			res.idDocument = lb.getId();
			return res;
		});

		// vérification que la clé de visualisation externe du document est vide
		doInNewTransactionAndSession(status -> {
			final LettreBienvenue lb = hibernateTemplate.get(LettreBienvenue.class, ids.idDocument);
			Assert.assertNotNull(lb);
			Assert.assertNull(lb.getCleDocument());
			Assert.assertNull(lb.getCleDocumentRappel());
			return null;
		});

		// génération de la quittance
		final Quittance quittance = buildQuittance("Mon identifiant bien senti", ids.idContribuable, TypeDocumentEditique.LETTRE_BIENVENUE, "Une clé d'archivage");
		doInNewTransactionAndSession(status -> {
			handler.onQuittance(quittance, buildCustomAttributeMap(TypeDocumentSortant.LETTRE_BIENVENUE_RC_VD, String.valueOf(ids.idDocument)));
			return null;
		});

		// vérification du résultat en base
		doInNewTransactionAndSession(status -> {
			final LettreBienvenue lb = hibernateTemplate.get(LettreBienvenue.class, ids.idDocument);
			Assert.assertNotNull(lb);
			Assert.assertEquals("Mon identifiant bien senti", lb.getCleDocument());
			Assert.assertNull(lb.getCleDocumentRappel());
			return null;
		});
	}

	@Test
	public void testReceptionPourRappelAutreDocumentFiscal() throws Exception {

		final RegDate dateDebut = date(2000, 6, 12);
		final RegDate today = RegDate.get();
		final RegDate dateEnvoiDocument = today.addMonths(-3);
		final RegDate delaiRetour = dateEnvoiDocument.addMonths(2);
		final RegDate dateRappel = today;

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				// rien ni personne
			}
		});

		final class Ids {
			long idContribuable;
			long idDocument;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {

			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala SARL");
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);     // chaque année, au 31.12

			final LettreBienvenue lb = addLettreBienvenue(entreprise, TypeLettreBienvenue.VD_RC);
			addDelaiAutreDocumentFiscal(lb, dateEnvoiDocument, delaiRetour, EtatDelaiDocumentFiscal.ACCORDE);
			addEtatAutreDocumentFiscalEmis(lb, dateEnvoiDocument);
			addEtatAutreDocumentFiscalRappele(lb, dateRappel);
			final Ids res = new Ids();
			res.idContribuable = entreprise.getNumero();
			res.idDocument = lb.getId();
			return res;
		});

		// vérification que la clé de visualisation externe du document est vide
		doInNewTransactionAndSession(status -> {
			final LettreBienvenue lb = hibernateTemplate.get(LettreBienvenue.class, ids.idDocument);
			Assert.assertNotNull(lb);
			Assert.assertNull(lb.getCleDocument());
			Assert.assertNull(lb.getCleDocumentRappel());
			return null;
		});

		// génération de la quittance
		final Quittance quittance = buildQuittance("Mon identifiant bien senti qui fait mal", ids.idContribuable, TypeDocumentEditique.RAPPEL, "Une clé d'archivage");
		doInNewTransactionAndSession(status -> {
			handler.onQuittance(quittance, buildCustomAttributeMap(TypeDocumentSortant.RAPPEL_LETTRE_BIENVENUE, String.valueOf(ids.idDocument)));
			return null;
		});

		// vérification du résultat en base
		doInNewTransactionAndSession(status -> {
			final LettreBienvenue lb = hibernateTemplate.get(LettreBienvenue.class, ids.idDocument);
			Assert.assertNotNull(lb);
			Assert.assertNull(lb.getCleDocument());
			Assert.assertEquals("Mon identifiant bien senti qui fait mal", lb.getCleDocumentRappel());
			return null;
		});
	}

	@Test
	public void testReceptionPourSommationDeclaration() throws Exception {

		final RegDate dateDebut = date(2000, 6, 12);
		final RegDate today = RegDate.get();
		final RegDate dateEnvoiDocument = today.addMonths(-3);
		final RegDate delaiRetour = dateEnvoiDocument.addMonths(2);
		final RegDate dateSommation = today;
		final int anneeDeclaration = 2016;

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				// rien ni personne
			}
		});

		// mise en place fiscale
		final long idContribuable = doInNewTransactionAndSession(status -> {

			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala SARL");
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);     // chaque année, au 31.12
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay);

			final PeriodeFiscale pf = addPeriodeFiscale(anneeDeclaration);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(anneeDeclaration, 1, 1), date(anneeDeclaration, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
			addEtatDeclarationEmise(di, dateEnvoiDocument);
			addDelaiDeclaration(di, dateEnvoiDocument, delaiRetour, EtatDelaiDocumentFiscal.ACCORDE);      // délai initial
			addEtatDeclarationSommee(di, dateSommation, dateSommation, null);

			return entreprise.getNumero();
		});

		// vérification que la clé de visualisation externe du document est vide et récupération de l'identifiant de l'état
		final long idEtatSomme = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(idContribuable);
			Assert.assertNotNull(entreprise);

			final EtatDeclarationSommee etatSomme = entreprise.getDeclarations().stream()
					.map(Declaration::getEtatsDeclaration)
					.flatMap(Collection::stream)
					.filter(EtatDeclarationSommee.class::isInstance)
					.map(EtatDeclarationSommee.class::cast)
					.findFirst()
					.orElse(null);

			Assert.assertNotNull(etatSomme);
			Assert.assertNull(etatSomme.getCleDocument());
			return etatSomme.getId();
		});

		// génération de la quittance
		final Quittance quittance = buildQuittance("IdentificationSommation", idContribuable, TypeDocumentEditique.SOMMATION_DI_PM, "Une clé d'archivage");
		doInNewTransactionAndSession(status -> {
			handler.onQuittance(quittance, buildCustomAttributeMap(TypeDocumentSortant.SOMMATION_DI_ENTREPRISE, String.valueOf(idEtatSomme)));
			return null;
		});

		// vérification du résultat en base
		doInNewTransactionAndSession(status -> {
			final EtatDeclarationSommee etatSomme = hibernateTemplate.get(EtatDeclarationSommee.class, idEtatSomme);
			Assert.assertNotNull(etatSomme);
			Assert.assertEquals("IdentificationSommation", etatSomme.getCleDocument());
			return null;
		});
	}

	@Test
	public void testReceptionPourDelaiDeclaration() throws Exception {

		final RegDate dateDebut = date(2000, 6, 12);
		final RegDate today = RegDate.get();
		final RegDate dateEnvoiDocument = today.addMonths(-3);
		final RegDate delaiRetour = dateEnvoiDocument.addMonths(2);
		final RegDate dateObtentionNouveauDelai = today;
		final int anneeDeclaration = 2016;

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				// rien ni personne
			}
		});

		// mise en place fiscale
		final long idContribuable = doInNewTransactionAndSession(status -> {

			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala SARL");
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);     // chaque année, au 31.12
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay);

			final PeriodeFiscale pf = addPeriodeFiscale(anneeDeclaration);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(anneeDeclaration, 1, 1), date(anneeDeclaration, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
			addEtatDeclarationEmise(di, dateEnvoiDocument);
			addDelaiDeclaration(di, dateEnvoiDocument, delaiRetour, EtatDelaiDocumentFiscal.ACCORDE);      // délai initial
			addDelaiDeclaration(di, dateObtentionNouveauDelai, delaiRetour.addMonths(1), EtatDelaiDocumentFiscal.ACCORDE);     // nouveau délai

			return entreprise.getNumero();
		});

		// vérification que la clé de visualisation externe du document est vide et récupération de l'identifiant du délai
		final long idDelai = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(idContribuable);
			Assert.assertNotNull(entreprise);

			final DelaiDeclaration dernierDelai = entreprise.getDeclarations().stream()
					.map(Declaration::getDelaisDeclaration)
					.flatMap(Collection::stream)
					.max(Comparator.comparing(DelaiDeclaration::getDelaiAccordeAu))
					.orElse(null);

			Assert.assertNotNull(dernierDelai);
			Assert.assertNull(dernierDelai.getCleDocument());
			return dernierDelai.getId();
		});

		// génération de la quittance
		final Quittance quittance = buildQuittance("IdentificationDélai", idContribuable, TypeDocumentEditique.ACCORD_DELAI_PM, "Une clé d'archivage");
		doInNewTransactionAndSession(status -> {
			handler.onQuittance(quittance, buildCustomAttributeMap(TypeDocumentSortant.ACCORD_DELAI_PM, String.valueOf(idDelai)));
			return null;
		});

		// vérification du résultat en base
		doInNewTransactionAndSession(status -> {
			final DelaiDeclaration delai = hibernateTemplate.get(DelaiDeclaration.class, idDelai);
			Assert.assertNotNull(delai);
			Assert.assertEquals("IdentificationDélai", delai.getCleDocument());
			return null;
		});
	}


	@Test
	public void testReceptionPourRefusDelaiDeclarationPM() throws Exception {

		final RegDate dateDebut = date(2000, 6, 12);
		final RegDate today = RegDate.get();
		final RegDate dateEnvoiDocument = today.addMonths(-3);
		final RegDate delaiRetour = dateEnvoiDocument.addMonths(2);
		final RegDate dateObtentionNouveauDelai = today;
		final int anneeDeclaration = 2016;

		// mise en place civile
		serviceEntreprise.setUp(new MockEntrepriseConnector() {
			@Override
			protected void init() {
				// rien ni personne
			}
		});

		// mise en place fiscale
		final long idContribuable = doInNewTransactionAndSession(status -> {

			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateDebut, null, "Tralala SARL");
			addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);     // chaque année, au 31.12
			addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay);

			final PeriodeFiscale pf = addPeriodeFiscale(anneeDeclaration);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_BATCH, pf);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinairePM di = addDeclarationImpot(entreprise, pf, date(anneeDeclaration, 1, 1), date(anneeDeclaration, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
			addEtatDeclarationEmise(di, dateEnvoiDocument);
			addDelaiDeclaration(di, dateEnvoiDocument, delaiRetour, EtatDelaiDocumentFiscal.ACCORDE);      // délai initial
			addDelaiDeclaration(di, dateObtentionNouveauDelai, delaiRetour.addMonths(1), EtatDelaiDocumentFiscal.ACCORDE);     // nouveau délai

			return entreprise.getNumero();
		});

		// vérification que la clé de visualisation externe du document est vide et récupération de l'identifiant du délai
		final long idDelai = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(idContribuable);
			Assert.assertNotNull(entreprise);

			final DelaiDeclaration dernierDelai = entreprise.getDeclarations().stream()
					.map(Declaration::getDelaisDeclaration)
					.flatMap(Collection::stream)
					.max(Comparator.comparing(DelaiDeclaration::getDelaiAccordeAu))
					.orElse(null);

			Assert.assertNotNull(dernierDelai);
			Assert.assertNull(dernierDelai.getCleDocument());
			return dernierDelai.getId();
		});

		// génération du Refus
		final Quittance quittance = buildQuittance("IdentificationRefusDélai", idContribuable, TypeDocumentEditique.REFUS_DELAI_PM, "Une clé d'archivage");
		doInNewTransactionAndSession(status -> {
			handler.onQuittance(quittance, buildCustomAttributeMap(TypeDocumentSortant.REFUS_DELAI_PM, String.valueOf(idDelai)));
			return null;
		});

		// vérification du résultat en base
		doInNewTransactionAndSession(status -> {
			final DelaiDeclaration delai = hibernateTemplate.get(DelaiDeclaration.class, idDelai);
			Assert.assertNotNull(delai);
			Assert.assertEquals("IdentificationRefusDélai", delai.getCleDocument());
			return null;
		});
	}


	@Test
	public void testReceptionPourRefusDelaiDeclarationPP() throws Exception {

		final RegDate dateDebut = date(2000, 6, 12);
		final RegDate today = RegDate.get();
		final RegDate dateEnvoiDocument = today.addMonths(-3);
		final RegDate delaiRetour = dateEnvoiDocument.addMonths(2);
		final RegDate dateObtentionNouveauDelai = today;
		final int anneeDeclaration = 2016;

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				// rien ni personne
			}
		});

		// mise en place fiscale
		final long idContribuable = doInNewTransactionAndSession(status -> {

			final PersonnePhysique nestor = addNonHabitant("Nestor","Burma",date(1980,1,5),Sexe.MASCULIN);
			addForPrincipal(nestor, dateDebut, MotifFor.ARRIVEE_HC, MockCommune.Cossonay);

			final PeriodeFiscale pf = addPeriodeFiscale(anneeDeclaration);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_COSSONAY.getNoColAdm());
			final DeclarationImpotOrdinaire di = addDeclarationImpot(nestor, pf, date(anneeDeclaration, 1, 1), date(anneeDeclaration, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, md);
			addEtatDeclarationEmise(di, dateEnvoiDocument);
			addDelaiDeclaration(di, dateEnvoiDocument, delaiRetour, EtatDelaiDocumentFiscal.ACCORDE);      // délai initial
			addDelaiDeclaration(di, dateObtentionNouveauDelai, delaiRetour.addMonths(1), EtatDelaiDocumentFiscal.ACCORDE);     // nouveau délai

			return nestor.getNumero();
		});

		// vérification que la clé de visualisation externe du document est vide et récupération de l'identifiant du délai
		final long idDelai = doInNewTransactionAndSession(status -> {
			final PersonnePhysique nestor = (PersonnePhysique) tiersDAO.get(idContribuable);
			Assert.assertNotNull(nestor);

			final DelaiDeclaration dernierDelai = nestor.getDeclarations().stream()
					.map(Declaration::getDelaisDeclaration)
					.flatMap(Collection::stream)
					.max(Comparator.comparing(DelaiDeclaration::getDelaiAccordeAu))
					.orElse(null);

			Assert.assertNotNull(dernierDelai);
			Assert.assertNull(dernierDelai.getCleDocument());
			return dernierDelai.getId();
		});

		// génération du Refus
		final Quittance quittance = buildQuittance("IdentificationRefusDélai", idContribuable, TypeDocumentEditique.REFUS_DELAI_PP, "Une clé d'archivage");
		doInNewTransactionAndSession(status -> {
			handler.onQuittance(quittance, buildCustomAttributeMap(TypeDocumentSortant.REFUS_DELAI, String.valueOf(idDelai)));
			return null;
		});

		// vérification du résultat en base
		doInNewTransactionAndSession(status -> {
			final DelaiDeclaration delai = hibernateTemplate.get(DelaiDeclaration.class, idDelai);
			Assert.assertNotNull(delai);
			Assert.assertEquals("IdentificationRefusDélai", delai.getCleDocument());
			return null;
		});
	}

	@Test
	public void testReceptionPourDocumentEFacture() throws Exception {

		final String cleArchivage = "847o35z7343gcfbfdhsjdhg38";

		// mise en place fiscale
		final long idContribuable = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Alfred", "Da Musso", null, Sexe.MASCULIN);
			addDocumentEFacture(pp, cleArchivage, null);
			return pp.getNumero();
		});

		// vérification que la clé de visualisation externe du document est vide et récupération de l'identifiant
		final long idDocumentEFacture = doInNewTransactionAndSession(status -> {
			final List<DocumentEFacture> allDocs = documentEFactureDAO.getAll();
			Assert.assertNotNull(allDocs);
			Assert.assertEquals(1, allDocs.size());
			final DocumentEFacture doc = allDocs.get(0);
			Assert.assertNotNull(doc);
			Assert.assertFalse(doc.isAnnule());
			Assert.assertEquals((Long) idContribuable, doc.getTiers().getNumero());
			Assert.assertEquals(cleArchivage, doc.getCleArchivage());
			Assert.assertNull(doc.getCleDocument());
			return doc.getId();
		});

		// génération de la quittance
		final Quittance quittance = buildQuittance("IdentificationDocEFact", idContribuable, TypeDocumentEditique.ACCORD_DELAI_PM, cleArchivage);
		doInNewTransactionAndSession(status -> {
			handler.onQuittance(quittance, buildCustomAttributeMap(TypeDocumentSortant.E_FACTURE_SIGNATURE, DocumentEFactureHelper.encodeIdentifiant(idContribuable, cleArchivage)));
			return null;
		});

		// vérification du résultat en base
		doInNewTransactionAndSession(status -> {
			final DocumentEFacture doc = hibernateTemplate.get(DocumentEFacture.class, idDocumentEFacture);
			Assert.assertNotNull(doc);
			Assert.assertFalse(doc.isAnnule());
			Assert.assertEquals((Long) idContribuable, doc.getTiers().getNumero());
			Assert.assertEquals(cleArchivage, doc.getCleArchivage());
			Assert.assertEquals("IdentificationDocEFact", doc.getCleDocument());
			return null;
		});
	}
}
