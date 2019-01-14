package ch.vd.unireg.documentfiscal;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TexteCasePostale;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeLettreBienvenue;
import ch.vd.unireg.xml.editique.pm.FichierImpression;


public class ImpressionRappelHelperTest extends BusinessTest {

	private ImpressionRappelHelperImpl impressionRappelHelper;
	private DelaisService delaisService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		impressionRappelHelper = new ImpressionRappelHelperImpl();
		impressionRappelHelper.setAdresseService(getBean(AdresseService.class, "adresseService"));
		ProxyServiceInfrastructureService infraService = getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService");
		impressionRappelHelper.setInfraService(infraService);
		impressionRappelHelper.setTiersService(getBean(TiersService.class, "tiersService"));

		delaisService = getBean(DelaisService.class, "delaisService");
	}

	/**
	 * [SIFISC-29013] La date inscrite sur les rappels de dégrèvement et lettre de bienvenue (papier)
	 * doit tenir compte d'un délai de trois jours ouvrables.
	 *
	 * @throws EditiqueException
	 */
	@Test
	public void testBuildDocument() throws Exception {

		final RegDate dateDebut = date(1990, 4, 2);
		final RegDate dateEnvoiLettre = date(1990, 6, 2);
		final RegDate dateTraitementRappel = date(1990, 8, 20);

		class LbEncapsulation {
			LettreBienvenue lb;
			RegDate dateEnvoiRappel;
		}
		final LbEncapsulation lbEncapsulation = new LbEncapsulation();


		// mise en place fiscale
		final long pmId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER,dateDebut, null, MockRue.Lausanne.AvenueJolimont, new CasePostale(TexteCasePostale.CASE_POSTALE ,1007));
				addAdresseSuisse(entreprise, TypeAdresseTiers.REPRESENTATION,dateDebut, null, MockRue.Lausanne.AvenueJolimont, new CasePostale(TexteCasePostale.CASE_POSTALE ,1007));
				addRaisonSociale(entreprise, dateDebut, null, "Toto SA");
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);
				lbEncapsulation.lb = addLettreBienvenue(entreprise, TypeLettreBienvenue.VD_RC);
				addDelaiAutreDocumentFiscal(lbEncapsulation.lb, dateEnvoiLettre, dateEnvoiLettre.addMonths(2), EtatDelaiDocumentFiscal.ACCORDE);
				addEtatAutreDocumentFiscalEmis(lbEncapsulation.lb, dateEnvoiLettre);
				// La date d'envoi du rappel est calculée à partir du 'delaisService'
				lbEncapsulation.dateEnvoiRappel = delaisService.getDateFinDelaiCadevImpressionLettreBienvenue(dateTraitementRappel);
				addEtatAutreDocumentFiscalRappele(lbEncapsulation.lb, lbEncapsulation.dateEnvoiRappel);

				return entreprise.getNumero();
			}
		});



		FichierImpression.Document document = impressionRappelHelper.buildDocument(lbEncapsulation.lb, dateTraitementRappel, true);

		Assert.assertNotNull(document);
		// Date d'envoi du rappel attendue, tient compte d'un délai de trois jours ouvrables
		Assert.assertEquals("19900823", document.getInfoEnteteDocument().getExpediteur().getDateExpedition());
	}

}
