package ch.vd.unireg.documentfiscal;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.infrastructure.model.rest.CommuneSimple;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.CommuneImpl;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.service.mock.ProxyServiceInfrastructureService;
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.EstimationRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.MockRegistreFoncierService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TexteCasePostale;
import ch.vd.unireg.type.TypeAdresseTiers;

import static org.junit.Assert.assertNotNull;


public class ImpressionRappelDemandeDegrevementICIHelperTest extends BusinessTest {

	private ImpressionDemandeDegrevementICIHelperImpl impressionDemandeDegrevementICIHelper;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		impressionDemandeDegrevementICIHelper = new ImpressionDemandeDegrevementICIHelperImpl();
		impressionDemandeDegrevementICIHelper.setAdresseService(getBean(AdresseService.class, "adresseService"));
		ProxyServiceInfrastructureService infraService = getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService");
		impressionDemandeDegrevementICIHelper.setInfraService(infraService);
		impressionDemandeDegrevementICIHelper.setTiersService(getBean(TiersService.class, "tiersService"));
//		impressionDemandeDegrevementICIHelper.setRegistreFoncierService(getBean(RegistreFoncierService.class, "serviceRF"));
		impressionDemandeDegrevementICIHelper.setRegistreFoncierService(new MockRegistreFoncierService() {
			@Override
			public @Nullable Commune getCommune(ImmeubleRF immeuble, RegDate dateReference) {
				CommuneSimple commune = new CommuneSimple();
				commune.setNomMinuscule("mock commune");
				return CommuneImpl.get(commune);
			}

			@Override
			public @Nullable EstimationRF getEstimationFiscale(ImmeubleRF immeuble, RegDate dateReference) {
				EstimationRF estimation = new EstimationRF();
				estimation.setMontant(500000L);
				estimation.setImmeuble(immeuble);
				return estimation;
			}

			@Override
			public @Nullable String getNumeroParcelleComplet(ImmeubleRF immeuble, RegDate dateReference) {
				return "456";
			}
		});
	}


	/**
	 * [SIFISC-29013] La date inscrite sur les rappels de dégrèvement et lettre de bienvenue (papier) doit tenir compte d'un délai de trois jours ouvrables.
	 *
	 * @throws EditiqueException
	 */
	@Test
	public void testDateEnvoiRappelLettreDegrevementDocument() throws Exception {

		final RegDate dateDebut = date(1990, 4, 2);
		final RegDate dateEnvoiLettre = date(2015, 6, 2);
		final RegDate dateEnvoiRappel = date(2015, 11, 2);
		final RegDate dateTraitementRappel = date(2015, 8, 20);

		class Ids {
			Long dd;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				// entreprise
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, dateDebut, null, MockRue.Lausanne.AvenueJolimont, new CasePostale(TexteCasePostale.CASE_POSTALE, 1007));
				addAdresseSuisse(entreprise, TypeAdresseTiers.REPRESENTATION, dateDebut, null, MockRue.Lausanne.AvenueJolimont, new CasePostale(TexteCasePostale.CASE_POSTALE, 1007));
				addRaisonSociale(entreprise, dateDebut, null, "Toto SA");
				addRegimeFiscalVD(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);

				BatimentRF batiment = addBatimentRF("masterId");
				ImmeubleRF immeuble = addImmeubleRF("rf123");
				addEstimationFiscale(dateTraitementRappel, dateDebut, null, false, 250000L, "estim01", immeuble);
				addSituationRF(dateDebut, null, 23, immeuble, addCommuneRF(132, "une commune", 456));
				addSurfaceAuSol(dateDebut, null, 200, "type surf", immeuble);
				// Lie les implantations à immeuble et bâtiment
				addImplantationRF(dateDebut, null, 80, immeuble, batiment);

				// Demande dégrèvement
				DemandeDegrevementICI dd = addDemandeDegrevementICI(entreprise, 2017, immeuble, "ABC123");
				addEtatAutreDocumentFiscalEmis(dd, dateEnvoiLettre);
				ids.dd = dd.getId();

				// Date d'envoi du rappel
				addEtatAutreDocumentFiscalRappele(dd, dateEnvoiRappel);

				return null;
			}
		});

		doInNewTransaction(transactionStatus -> {
			final DemandeDegrevementICI dd = hibernateTemplate.get(DemandeDegrevementICI.class, ids.dd);
			assertNotNull(dd);

			FichierImpression.Document document;
			try {
				document = impressionDemandeDegrevementICIHelper.buildDocument(dd, dateTraitementRappel, true);
			}
			catch (EditiqueException e) {
				throw new RuntimeException(e);
			}
			assertNotNull(document);

			// Date d'envoi du rappel attendue, tient compte d'un délai de trois jours ouvrables
			Date dateAttendue = dateEnvoiRappel.asJavaDate();
			SimpleDateFormat formater = new SimpleDateFormat("yyyyMMdd");
			Assert.assertEquals(formater.format(dateAttendue), document.getInfoEnteteDocument().getExpediteur().getDateExpedition());
			return null;
		});

	}
}
