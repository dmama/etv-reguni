package ch.vd.uniregctb.evenement.docsortant;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.editique.unireg.CTypeInfoArchivage;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.xml.event.docsortant.v1.CodeSupport;
import ch.vd.unireg.xml.event.docsortant.v1.Document;
import ch.vd.unireg.xml.event.docsortant.v1.Documents;
import ch.vd.unireg.xml.event.docsortant.v1.Population;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.documentfiscal.LettreTypeInformationLiquidation;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeRapprochementRF;

public class EvenementDocumentSortantServiceTest extends BusinessTest {

	private CollectingSender collectingSender;
	private EvenementDocumentSortantService service;

	/**
	 * Une implémentation bidon du sender pour voir ce qui aurait dû partir sur le fil
	 */
	private final class CollectingSender implements EvenementDocumentSortantSender {

		private final Map<String, Documents> collected = new HashMap<>();

		@Override
		public void sendEvenementDocumentSortant(String businessId, Documents docs) throws EvenementDocumentSortantException {
			collected.put(businessId, docs);
		}

		public Map<String, Documents> getCollected() {
			return collected;
		}
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		collectingSender = new CollectingSender();
		final EvenementDocumentSortantServiceImpl implementation = new EvenementDocumentSortantServiceImpl();
		implementation.setSender(collectingSender);
		this.service = implementation;
	}

	@Test
	public void testEnvoiLettreInformationLiquidation() throws Exception {

		final RegDate dateDebut = date(2005, 3, 14);
		final RegDate dateEnvoiDocument = RegDate.get().addDays(-3);
		final String cleArchivage = "56782433289024328";

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Machin truc");
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.ASSOCIATION);
				return entreprise.getNumero();
			}
		});

		// envoi d'un nouveau document sortant
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				final LettreTypeInformationLiquidation lettre = new LettreTypeInformationLiquidation();
				lettre.setDateEnvoi(dateEnvoiDocument);
				lettre.setCleArchivage(cleArchivage);
				entreprise.addAutreDocumentFiscal(lettre);

				final CTypeInfoArchivage infoArchivage = new CTypeInfoArchivage();
				infoArchivage.setDatTravail(String.valueOf(dateEnvoiDocument.index()));
				infoArchivage.setIdDocument(cleArchivage);
				infoArchivage.setNomApplication(ConstantesEditique.APPLICATION_ARCHIVAGE);
				infoArchivage.setNomDossier(FormatNumeroHelper.numeroCTBToDisplay(id));
				infoArchivage.setTypDocument(TypeDocumentEditique.LETTRE_TYPE_INFO_LIQUIDATION.getCodeDocumentArchivage());
				infoArchivage.setTypDossier(ConstantesEditique.TYPE_DOSSIER_ARCHIVAGE);

				service.signaleLettreTypeInformationLiquidation(lettre, infoArchivage, true);
			}
		});

		// vérification du contenu collecté
		final Map<String, Documents> collectes = collectingSender.getCollected();
		Assert.assertNotNull(collectes);
		Assert.assertEquals(1, collectes.size());
		final Map.Entry<String, Documents> entry = collectes.entrySet().iterator().next();
		Assert.assertNotNull(entry);
		Assert.assertNotNull(entry.getKey());
		Assert.assertTrue(entry.getKey(), entry.getKey().startsWith("LTIL " + id + " "));
		final Documents docs = entry.getValue();
		Assert.assertNotNull(docs);
		Assert.assertNotNull(docs.getHorodatage());
		Assert.assertNotNull(docs.getDocumentSortant());
		Assert.assertEquals(1, docs.getDocumentSortant().size());
		final Document doc = docs.getDocumentSortant().get(0);
		Assert.assertNotNull(doc);
		Assert.assertNotNull(doc.getDonneesMetier());
		Assert.assertNotNull(doc.getArchive());
		Assert.assertNotNull(doc.getCaracteristiques());
		Assert.assertNotNull(doc.getIdentifiantSupervision());
		Assert.assertNull(doc.getIdentifiantRepelec());
		Assert.assertNull(doc.getUrl());

		Assert.assertTrue(doc.getIdentifiantSupervision(), doc.getIdentifiantSupervision().startsWith("UNIREG-LTIL " + id + " "));

		Assert.assertEquals(Population.PM, doc.getDonneesMetier().getAxe());
		Assert.assertEquals((int) id, doc.getDonneesMetier().getNumeroContribuable());
		Assert.assertNull(doc.getDonneesMetier().getMontant());
		Assert.assertNotNull(doc.getDonneesMetier().getPeriodesFiscales());
		Assert.assertNotNull(doc.getDonneesMetier().getPeriodesFiscales().getPeriodeFiscale());
		Assert.assertEquals(Collections.singletonList(BigInteger.valueOf(dateEnvoiDocument.year())), doc.getDonneesMetier().getPeriodesFiscales().getPeriodeFiscale());
		Assert.assertNull(doc.getDonneesMetier().isPeriodeFiscalePerenne());

		Assert.assertEquals("UNIREG", doc.getCaracteristiques().getEmetteur());
		Assert.assertEquals("Lettre type information sur liquidation", doc.getCaracteristiques().getNomDocument());
		Assert.assertNull(doc.getCaracteristiques().getSousTypeDocument());
		Assert.assertEquals(TypeDocumentSortant.LETTRE_TYPE_INFO_LIQUIDATION.getCodeTypeDocumentSortant().getCode(), doc.getCaracteristiques().getTypeDocument());
		Assert.assertEquals(CodeSupport.LOCAL, doc.getCaracteristiques().getSupport());
		Assert.assertNotNull(doc.getCaracteristiques().getArchivage());
		Assert.assertEquals(false, doc.getCaracteristiques().getArchivage().isValeurProbante());
		Assert.assertNull(doc.getCaracteristiques().getArchivage().getNombreAnneesValeurProbante());

		Assert.assertEquals(cleArchivage, doc.getArchive().getIdDocument());
		Assert.assertEquals(FormatNumeroHelper.numeroCTBToDisplay(id), doc.getArchive().getNomDossier());
		Assert.assertEquals(TypeDocumentEditique.LETTRE_TYPE_INFO_LIQUIDATION.getCodeDocumentArchivage(), doc.getArchive().getTypDocument());
		Assert.assertEquals(ConstantesEditique.TYPE_DOSSIER_ARCHIVAGE, doc.getArchive().getTypDossier());
	}

	@Test
	public void testEnvoiFormulaireDemandeDegrevement() throws Exception {

		final RegDate dateDebut = date(2005, 3, 14);
		final RegDate dateEnvoiDocument = RegDate.get().addDays(-3);
		final String cleArchivage = "56782433289024328";

		// mise en place fiscale
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Machin truc");
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.ASSOCIATION);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);

				final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
				final BienFondRF immeuble = addBienFondRF("85343fldfg", null, commune, 42);

				final PersonneMoraleRF pmRF = addPersonneMoraleRF("Machin truc", null, "87553zhgfsjh", 35623, null);
				addDroitPersonneMoraleRF(null, dateDebut, null, "Achat", null, "578567fdbdfbsd", new IdentifiantAffaireRF(484, null, null, null), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, pmRF, immeuble, null);
				addRapprochementRF(entreprise, pmRF, null, null, TypeRapprochementRF.AUTO);

				return entreprise.getNumero();
			}
		});

		// envoi d'un nouveau document sortant
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
				Assert.assertNotNull(entreprise);

				final DemandeDegrevementICI demande = new DemandeDegrevementICI();
				demande.setCodeControle("H12345");
				demande.setNumeroSequence(1);
				demande.setCleArchivage(cleArchivage);
				demande.setDateEnvoi(dateEnvoiDocument);
				demande.setPeriodeFiscale(dateEnvoiDocument.year() + 1);
				demande.setDelaiRetour(dateEnvoiDocument.addMonths(4));
				entreprise.addAutreDocumentFiscal(demande);

				final CTypeInfoArchivage infoArchivage = new CTypeInfoArchivage();
				infoArchivage.setDatTravail(String.valueOf(dateEnvoiDocument.index()));
				infoArchivage.setIdDocument(cleArchivage);
				infoArchivage.setNomApplication(ConstantesEditique.APPLICATION_ARCHIVAGE);
				infoArchivage.setNomDossier(FormatNumeroHelper.numeroCTBToDisplay(id));
				infoArchivage.setTypDocument(TypeDocumentEditique.DEMANDE_DEGREVEMENT_ICI.getCodeDocumentArchivage());
				infoArchivage.setTypDossier(ConstantesEditique.TYPE_DOSSIER_ARCHIVAGE);

				service.signaleDemandeDegrevementICI(demande, "Ma super commune", "42-12-4", infoArchivage, true);
			}
		});

		// vérification du contenu collecté
		final Map<String, Documents> collectes = collectingSender.getCollected();
		Assert.assertNotNull(collectes);
		Assert.assertEquals(1, collectes.size());
		final Map.Entry<String, Documents> entry = collectes.entrySet().iterator().next();
		Assert.assertNotNull(entry);
		Assert.assertNotNull(entry.getKey());
		Assert.assertTrue(entry.getKey(), entry.getKey().startsWith("DDICI " + id + " "));
		final Documents docs = entry.getValue();
		Assert.assertNotNull(docs);
		Assert.assertNotNull(docs.getHorodatage());
		Assert.assertNotNull(docs.getDocumentSortant());
		Assert.assertEquals(1, docs.getDocumentSortant().size());
		final Document doc = docs.getDocumentSortant().get(0);
		Assert.assertNotNull(doc);
		Assert.assertNotNull(doc.getDonneesMetier());
		Assert.assertNotNull(doc.getArchive());
		Assert.assertNotNull(doc.getCaracteristiques());
		Assert.assertNotNull(doc.getIdentifiantSupervision());
		Assert.assertNull(doc.getIdentifiantRepelec());
		Assert.assertNull(doc.getUrl());

		Assert.assertTrue(doc.getIdentifiantSupervision(), doc.getIdentifiantSupervision().startsWith("UNIREG-DDICI " + id + " "));

		Assert.assertEquals(Population.PM, doc.getDonneesMetier().getAxe());
		Assert.assertEquals((int) id, doc.getDonneesMetier().getNumeroContribuable());
		Assert.assertNull(doc.getDonneesMetier().getMontant());
		Assert.assertNotNull(doc.getDonneesMetier().getPeriodesFiscales());
		Assert.assertNotNull(doc.getDonneesMetier().getPeriodesFiscales().getPeriodeFiscale());
		Assert.assertEquals(Collections.singletonList(BigInteger.valueOf(dateEnvoiDocument.year() + 1)), doc.getDonneesMetier().getPeriodesFiscales().getPeriodeFiscale());
		Assert.assertNull(doc.getDonneesMetier().isPeriodeFiscalePerenne());

		Assert.assertEquals("UNIREG", doc.getCaracteristiques().getEmetteur());
		Assert.assertEquals("Formulaire de demande de dégrèvement ICI Ma super commune 42-12-4", doc.getCaracteristiques().getNomDocument());
		Assert.assertNull(doc.getCaracteristiques().getSousTypeDocument());
		Assert.assertEquals(TypeDocumentSortant.DEMANDE_DEGREVEMENT_ICI.getCodeTypeDocumentSortant().getCode(), doc.getCaracteristiques().getTypeDocument());
		Assert.assertEquals(CodeSupport.LOCAL, doc.getCaracteristiques().getSupport());
		Assert.assertNotNull(doc.getCaracteristiques().getArchivage());
		Assert.assertEquals(false, doc.getCaracteristiques().getArchivage().isValeurProbante());
		Assert.assertNull(doc.getCaracteristiques().getArchivage().getNombreAnneesValeurProbante());

		Assert.assertEquals(cleArchivage, doc.getArchive().getIdDocument());
		Assert.assertEquals(FormatNumeroHelper.numeroCTBToDisplay(id), doc.getArchive().getNomDossier());
		Assert.assertEquals(TypeDocumentEditique.DEMANDE_DEGREVEMENT_ICI.getCodeDocumentArchivage(), doc.getArchive().getTypDocument());
		Assert.assertEquals(ConstantesEditique.TYPE_DOSSIER_ARCHIVAGE, doc.getArchive().getTypDossier());
	}
}
