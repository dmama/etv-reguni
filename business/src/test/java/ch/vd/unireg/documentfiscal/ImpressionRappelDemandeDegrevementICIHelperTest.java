package ch.vd.unireg.documentfiscal;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.infrastructure.model.rest.CommuneSimple;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
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
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TexteCasePostale;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.xml.editique.pm.FichierImpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class ImpressionRappelDemandeDegrevementICIHelperTest extends BusinessTest {

	private ImpressionDemandeDegrevementICIHelperImpl impressionDemandeDegrevementICIHelper;

	private ImpressionRappelDemandeDegrevementICIHelperImpl impressionRappelDemandeDegrevementICIHelper;

	private final RegDate dateDebut = date(1990, 4, 2);
	private final RegDate dateObtentionLettreEmis = date(2015, 6, 2);
	private final RegDate dateEnvoiRappel = date(2015, 11, 2);
	private final RegDate dateTraitement = date(2015, 8, 20);
	private static final SimpleDateFormat FORMATER = new SimpleDateFormat("yyyyMMdd");

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		ProxyServiceInfrastructureService infraService = getBean(ProxyServiceInfrastructureService.class, "serviceInfrastructureService");
		RegistreFoncierService rfService = new MockRegistreFoncierService() {
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
		};

		// Impression degrevement
		impressionDemandeDegrevementICIHelper = new ImpressionDemandeDegrevementICIHelperImpl();
		impressionDemandeDegrevementICIHelper.setAdresseService(getBean(AdresseService.class, "adresseService"));
		impressionDemandeDegrevementICIHelper.setInfraService(infraService);
		impressionDemandeDegrevementICIHelper.setTiersService(getBean(TiersService.class, "tiersService"));
		impressionDemandeDegrevementICIHelper.setRegistreFoncierService(rfService);

		// Impression rappel degrèvement
		impressionRappelDemandeDegrevementICIHelper = new ImpressionRappelDemandeDegrevementICIHelperImpl();
		impressionRappelDemandeDegrevementICIHelper.setAdresseService(getBean(AdresseService.class, "adresseService"));
		impressionRappelDemandeDegrevementICIHelper.setInfraService(infraService);
		impressionRappelDemandeDegrevementICIHelper.setTiersService(getBean(TiersService.class, "tiersService"));
		// rappel demande helper
		ImpressionDemandeDegrevementICIHelperImpl helper = new ImpressionDemandeDegrevementICIHelperImpl();
		helper.setRegistreFoncierService(rfService);
		helper.setAdresseService(getBean(AdresseService.class, "adresseService"));
		helper.setInfraService(infraService);
		helper.setTiersService(getBean(TiersService.class, "tiersService"));
		impressionRappelDemandeDegrevementICIHelper.setDemandeHelper(helper);
	}


	/**
	 * [SIFISC-29013] La date inscrite sur les rappels des lettres de dégrèvement (papier) doit tenir compte d'un délai de 3 jours ouvrables.
	 */
	@Test
	public void testDateEnvoiRappelLettreDegrevementDocument () throws Exception { // mode batch

		Date dateAttendue = dateEnvoiRappel.asJavaDate(); // Date retournée par 'DelaisService' = envoi rappel + 3j

		final Long idDegrevement = createDegrevement(dateEnvoiRappel);
		final FichierImpression.Document document = buildRappelDegrevementDocument(idDegrevement);

		// Date d'envoi du rappel attendue, tient compte d'un délai de trois jours ouvrables
		assertNotNull(document);
		assertEquals(FORMATER.format(dateAttendue), document.getInfoEnteteDocument().getExpediteur().getDateExpedition());
	}


	/**
	 * [SIFISC-29013] La date inscrite sur les lettres de dégrèvement (papier) ne tient PAS compte d'un délai de 3 jours ouvrables.
	 */
	@Test
	public void testDateEnvoiLettreDegrevementDocument () throws Exception { // mode batch
		Date dateAttendue = dateObtentionLettreEmis.asJavaDate(); // pas d'ajout de delai pour l'état émis

		final Long idDegrevement = createDegrevement(null);
		final FichierImpression.Document document = buildDegrevementDocument(idDegrevement, false);

		// Date d'envoi du rappel attendue, tient compte d'un délai de trois jours ouvrables
		assertNotNull(document);
		assertEquals(FORMATER.format(dateAttendue), document.getInfoEnteteDocument().getExpediteur().getDateExpedition());
	}

	/**
	 * [SIFISC-29013] La date inscrite sur les duplicatas de lettres de dégrèvement (papier) est ...
	 */
	@Test
	public void testDateEnvoiLettreDegrevementDocumentDuplicata () throws Exception { // mode online
		Date dateAttendue = dateTraitement.asJavaDate();

		final Long idDegrevement = createDegrevement(null);
		final FichierImpression.Document document = buildDegrevementDocument(idDegrevement, true);

		// Date d'envoi du rappel attendue, tient compte d'un délai de trois jours ouvrables
		assertNotNull(document);
		assertEquals(FORMATER.format(dateAttendue), document.getInfoEnteteDocument().getExpediteur().getDateExpedition());
	}


	private FichierImpression.Document buildDegrevementDocument(Long idDegrevement, boolean duplicata) throws Exception {
		return doInNewTransaction(transactionStatus -> {
			final DemandeDegrevementICI dd = hibernateTemplate.get(DemandeDegrevementICI.class, idDegrevement);
			assertNotNull(dd);
			return impressionDemandeDegrevementICIHelper.buildDocument(dd, dateTraitement, duplicata);
		});
	}

	private FichierImpression.Document buildRappelDegrevementDocument(Long idDegrevement) throws Exception {
		return doInNewTransaction(transactionStatus -> {
			final DemandeDegrevementICI dd = hibernateTemplate.get(DemandeDegrevementICI.class, idDegrevement);
			assertNotNull(dd);
			return impressionRappelDemandeDegrevementICIHelper.buildDocument(dd, dateTraitement);
		});
	}

	private Long createDegrevement(@Nullable RegDate dateEnvoiRappel) throws Exception {
		return doInNewTransaction(status -> {
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
				addEstimationFiscale(dateTraitement, dateDebut, null, false, 250000L, "estim01", immeuble);
				addSituationRF(dateDebut, null, 23, immeuble, addCommuneRF(132, "une commune", 456));
				addSurfaceAuSol(dateDebut, null, 200, "type surf", immeuble);
				// Lie les implantations à immeuble et bâtiment
				addImplantationRF(dateDebut, null, 80, immeuble, batiment);
	
				// Demande dégrèvement
				DemandeDegrevementICI dd = addDemandeDegrevementICI(entreprise, 2017, immeuble, "ABC123");
				addEtatAutreDocumentFiscalEmis(dd, dateObtentionLettreEmis);
	
				if(dateEnvoiRappel != null) {
					// Date d'envoi du rappel
					addEtatAutreDocumentFiscalRappele(dd, dateEnvoiRappel);
				}
	
				return dd.getId();
			});
	}
}
