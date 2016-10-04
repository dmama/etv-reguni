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
import ch.vd.unireg.xml.event.docsortant.v1.CodeSupport;
import ch.vd.unireg.xml.event.docsortant.v1.Document;
import ch.vd.unireg.xml.event.docsortant.v1.Documents;
import ch.vd.unireg.xml.event.docsortant.v1.Population;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.documentfiscal.LettreTypeInformationLiquidation;
import ch.vd.uniregctb.editique.ConstantesEditique;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;

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
	public void testEnvoi() throws Exception {

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
}
