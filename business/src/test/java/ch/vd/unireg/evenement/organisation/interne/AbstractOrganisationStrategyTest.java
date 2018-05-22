package ch.vd.unireg.evenement.organisation.interne;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.interne.creation.CreateOrganisationStrategy;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementOrganisation;

import static ch.vd.unireg.type.EtatEvenementOrganisation.A_TRAITER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2016-12-19, <raphael.marmier@vd.ch>
 */
public class AbstractOrganisationStrategyTest extends WithoutSpringTest {

	@Test
	public void testNouvellePMauRC() throws Exception {

		final CreateOrganisationStrategy strategy = new CreateOrganisationStrategy(null, null);

		/*
			Créaton entreprise PM VD inscrite au RC
		 */
		{
			final MockOrganisation org = createRcVd(10001L, 90001L, date(2016, 12, 4), FormeLegale.N_0106_SOCIETE_ANONYME, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, MockCommune.Lausanne.getNoOFS());
			final EvenementOrganisation event = createEvent(1L, 10001L, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, date(2016, 12, 4), A_TRAITER);
			final AbstractOrganisationStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateOrganisationStrategy.extraireInformationDeDateEtDeCreation(event, org);

			assertEquals(date(2016, 12, 1), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 12, 2), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertTrue(informationDeDateEtDeCreation.isCreation());
		}

		/*
			Créaton entreprise PM HC inscrite au RC
		 */
		{
			final MockOrganisation org = createRcVd(10001L, 90001L, date(2016, 12, 4), FormeLegale.N_0106_SOCIETE_ANONYME, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, MockCommune.Zurich.getNoOFS());
			final EvenementOrganisation event = createEvent(1L, 10001L, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, date(2016, 12, 4), A_TRAITER);

			final AbstractOrganisationStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateOrganisationStrategy.extraireInformationDeDateEtDeCreation(event, org);

			assertEquals(date(2016, 12, 1), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 12, 2), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertTrue(informationDeDateEtDeCreation.isCreation());
		}
	}

	@Test
	public void testNouvelleAPMauRC() throws Exception {

		final CreateOrganisationStrategy strategy = new CreateOrganisationStrategy(null, null);

		/*
			Créaton entreprise APM VD inscrite au RC
		 */
		{
			final MockOrganisation org = createRcVd(10001L, 90001L, date(2016, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeOrganisationRegistreIDE.ASSOCIATION, MockCommune.Lausanne.getNoOFS());

			final EvenementOrganisation event = createEvent(1L, 10001L, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, date(2016, 12, 4), A_TRAITER);

			final AbstractOrganisationStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateOrganisationStrategy.extraireInformationDeDateEtDeCreation(event, org);

			assertEquals(date(2016, 12, 1), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 12, 2), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertTrue(informationDeDateEtDeCreation.isCreation());
		}

		/*
			Créaton entreprise APM HC inscrite au RC
		 */
		{
			final MockOrganisation org = createRcNonVD(10001L, 90001L, date(2016, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeOrganisationRegistreIDE.ASSOCIATION, MockCommune.Zurich.getNoOFS());

			final EvenementOrganisation event = createEvent(1L, 10001L, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, date(2016, 12, 4), A_TRAITER);

			final AbstractOrganisationStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateOrganisationStrategy.extraireInformationDeDateEtDeCreation(event, org);

			assertEquals(date(2016, 12, 1), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 12, 1), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertTrue(informationDeDateEtDeCreation.isCreation());
		}

	}

	@Test
	public void testNouvelleAPMnonRC() throws Exception {

		final CreateOrganisationStrategy strategy = new CreateOrganisationStrategy(null, null);

		/*
			Créaton entreprise APM VD non inscrite au RC
		 */
		{
			final MockOrganisation org = createNonRcVd(10001L, 90001L, date(2016, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeOrganisationRegistreIDE.ASSOCIATION, MockCommune.Lausanne.getNoOFS());

			final EvenementOrganisation event = createEvent(1L, 10001L, TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION, date(2016, 12, 4), A_TRAITER);

			final AbstractOrganisationStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateOrganisationStrategy.extraireInformationDeDateEtDeCreation(event, org);

			assertEquals(date(2016, 12, 4), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 12, 4), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertTrue(informationDeDateEtDeCreation.isCreation());
		}

		/*
			Créaton entreprise APM non VD non inscrite au RC
		 */
		{
			final MockOrganisation org = createNonRcNonVd(10001L, 90001L, date(2016, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeOrganisationRegistreIDE.ASSOCIATION, MockCommune.Zurich.getNoOFS());

			final EvenementOrganisation event = createEvent(1L, 10001L, TypeEvenementOrganisation.IDE_NOUVELLE_INSCRIPTION, date(2016, 12, 4), A_TRAITER);

			final AbstractOrganisationStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateOrganisationStrategy.extraireInformationDeDateEtDeCreation(event, org);

			assertEquals(date(2016, 12, 4), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 12, 4), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertTrue(informationDeDateEtDeCreation.isCreation());
		}
	}

	@Test
	public void testMutationPMauRC() throws Exception {

		final CreateOrganisationStrategy strategy = new CreateOrganisationStrategy(null, null);

		/*
			Arrivee entreprise PM VD inscrite au RC
		 */
		{
			final MockOrganisation org = createRcVd(10001L, 90001L, date(2010, 12, 4), FormeLegale.N_0106_SOCIETE_ANONYME, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, MockCommune.Zurich.getNoOFS());
			final MockSiteOrganisation siteOrganisation = (MockSiteOrganisation) org.getDonneesSites().get(0);
			siteOrganisation.changeDomicile(date(2016, 10, 1), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS());
			final MockDonneesRC donneesRC = siteOrganisation.getDonneesRC();
			donneesRC.addInscription(date(2016, 10, 1), null, new InscriptionRC(StatusInscriptionRC.ACTIF, null, date(2016, 9, 27), null, date(2010, 12, 4), null));

			final EvenementOrganisation event = createEvent(1L, 10001L, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2016, 10, 1), A_TRAITER);

			final AbstractOrganisationStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateOrganisationStrategy.extraireInformationDeDateEtDeCreation(event, org);

			assertEquals(date(2016, 9, 27), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 9, 27), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertFalse(informationDeDateEtDeCreation.isCreation());
		}

		/*
			Mutation entreprise PM HC inscrite au RC
		 */
		{
			final MockOrganisation org = createRcVd(10001L, 90001L, date(2010, 12, 4), FormeLegale.N_0106_SOCIETE_ANONYME, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, MockCommune.Zurich.getNoOFS());
			final EvenementOrganisation event = createEvent(1L, 10001L, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2016, 10, 1), A_TRAITER);

			final AbstractOrganisationStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateOrganisationStrategy.extraireInformationDeDateEtDeCreation(event, org);

			// Note: dans la pratique, ces dates ne sont pas utilisée lors l'ajout en base Unireg d'une tiers PM HC suite à la création d'un établissement secondaire VD. La date de l'événement est utilisée.
			assertEquals(date(2010, 12, 1), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2010, 12, 1), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertFalse(informationDeDateEtDeCreation.isCreation());
		}
	}

	@Test
	public void testMutationAPMauRC() throws Exception {

		final CreateOrganisationStrategy strategy = new CreateOrganisationStrategy(null, null);

		/*
			Arrivee entreprise APM VD inscrite au RC
		 */
		{
			final MockOrganisation org = createRcVd(10001L, 90001L, date(2010, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeOrganisationRegistreIDE.ASSOCIATION, MockCommune.Zurich.getNoOFS());
			final MockSiteOrganisation siteOrganisation = (MockSiteOrganisation) org.getDonneesSites().get(0);
			siteOrganisation.changeDomicile(date(2016, 10, 1), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS());
			final MockDonneesRC donneesRC = siteOrganisation.getDonneesRC();
			donneesRC.addInscription(date(2016, 10, 1), null, new InscriptionRC(StatusInscriptionRC.ACTIF, null, date(2016, 9, 27), null, date(2010, 12, 4), null));

			final EvenementOrganisation event = createEvent(1L, 10001L, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2016, 10, 1), A_TRAITER);

			final AbstractOrganisationStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateOrganisationStrategy.extraireInformationDeDateEtDeCreation(event, org);

			assertEquals(date(2016, 9, 27), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 9, 27), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertFalse(informationDeDateEtDeCreation.isCreation());
		}

		/*
			Mutation entreprise APM HC inscrite au RC
		 */
		{
			final MockOrganisation org = createRcVd(10001L, 90001L, date(2010, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeOrganisationRegistreIDE.ASSOCIATION, MockCommune.Zurich.getNoOFS());
			final EvenementOrganisation event = createEvent(1L, 10001L, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2016, 10, 1), A_TRAITER);

			final AbstractOrganisationStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateOrganisationStrategy.extraireInformationDeDateEtDeCreation(event, org);

			// Note: dans la pratique, ces dates ne sont pas utilisée lors l'ajout en base Unireg d'une tiers PM HC suite à la création d'un établissement secondaire VD. La date de l'événement est utilisée.
			assertEquals(date(2010, 12, 1), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2010, 12, 1), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertFalse(informationDeDateEtDeCreation.isCreation());
		}
	}

	@Test
	public void testMutationAPMnonRC() throws Exception {

		final CreateOrganisationStrategy strategy = new CreateOrganisationStrategy(null, null);

		/*
			Arrivee entreprise APM VD non inscrite au RC
		 */
		{
			final MockOrganisation org = createNonRcVd(10001L, 90001L, date(2010, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeOrganisationRegistreIDE.ASSOCIATION, MockCommune.Zurich.getNoOFS());
			final MockSiteOrganisation siteOrganisation = (MockSiteOrganisation) org.getDonneesSites().get(0);
			siteOrganisation.changeDomicile(date(2016, 10, 1), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS());
			final MockDonneesRC donneesRC = siteOrganisation.getDonneesRC();
			donneesRC.addInscription(date(2016, 10, 1), null, new InscriptionRC(StatusInscriptionRC.ACTIF, null, date(2016, 9, 27), null, date(2010, 12, 4), null));

			final EvenementOrganisation event = createEvent(1L, 10001L, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2016, 10, 1), A_TRAITER);

			final AbstractOrganisationStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateOrganisationStrategy.extraireInformationDeDateEtDeCreation(event, org);

			assertEquals(date(2016, 9, 27), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 9, 27), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertFalse(informationDeDateEtDeCreation.isCreation());
		}

		/*
			Mutation entreprise APM HC non inscrite au RC
		 */
		{
			final MockOrganisation org = createNonRcVd(10001L, 90001L, date(2010, 12, 4), FormeLegale.N_0109_ASSOCIATION, TypeOrganisationRegistreIDE.ASSOCIATION, MockCommune.Zurich.getNoOFS());
			final EvenementOrganisation event = createEvent(1L, 10001L, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2016, 10, 1), A_TRAITER);

			final AbstractOrganisationStrategy.InformationDeDateEtDeCreation informationDeDateEtDeCreation = CreateOrganisationStrategy.extraireInformationDeDateEtDeCreation(event, org);

			// Note: dans la pratique, ces dates ne sont pas utilisée lors l'ajout en base Unireg d'une tiers PM HC suite à la création d'un établissement secondaire VD. La date de l'événement est utilisée.
			assertEquals(date(2016, 10, 1), informationDeDateEtDeCreation.getDateDeCreation());
			assertEquals(date(2016, 10, 1), informationDeDateEtDeCreation.getDateOuvertureFiscale());
			assertFalse(informationDeDateEtDeCreation.isCreation());
		}
	}

	protected MockOrganisation createRcVd(long cantonalId, long cantonalIdSitePrincipal, RegDate dateEvt, FormeLegale formeJuridique, TypeOrganisationRegistreIDE formeJuridiqueIde,
	                                      int noOFSCommune) {
		return MockOrganisationFactory.createOrganisation(cantonalId, cantonalIdSitePrincipal, "Synergy SA", dateEvt, null, formeJuridique,
		                                                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, noOFSCommune, StatusInscriptionRC.ACTIF, dateEvt.addDays(-3),
		                                                  StatusRegistreIDE.DEFINITIF, formeJuridiqueIde, "CHE999999996");
	}

	protected MockOrganisation createRcNonVD(long cantonalId, long cantonalIdSitePrincipal, RegDate dateEvt, FormeLegale formeJuridique, TypeOrganisationRegistreIDE formeJuridiqueIde,
	                                         int noOFSCommune) {
		return MockOrganisationFactory.createOrganisation(cantonalId, cantonalIdSitePrincipal, "Synergy SA", dateEvt, null, formeJuridique,
		                                                  TypeAutoriteFiscale.COMMUNE_HC, noOFSCommune, StatusInscriptionRC.ACTIF, dateEvt.addDays(-3),
		                                                  StatusRegistreIDE.DEFINITIF, formeJuridiqueIde, "CHE999999996");
	}


	protected MockOrganisation createNonRcVd(long cantonalId, long cantonalIdSitePrincipal, RegDate dateEvt, FormeLegale formeJuridique, TypeOrganisationRegistreIDE formeJuridiqueIde,
	                                         int noOFSCommune) {
		return MockOrganisationFactory.createOrganisation(cantonalId, cantonalIdSitePrincipal, "Synergy SA", dateEvt, null, formeJuridique,
		                                                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, noOFSCommune, null, null,
		                                                  StatusRegistreIDE.DEFINITIF, formeJuridiqueIde, "CHE999999996");
	}

	protected MockOrganisation createNonRcNonVd(long cantonalId, long cantonalIdSitePrincipal, RegDate dateEvt, FormeLegale formeJuridique, TypeOrganisationRegistreIDE formeJuridiqueIde,
	                                            int noOFSCommune) {
		return MockOrganisationFactory.createOrganisation(cantonalId, cantonalIdSitePrincipal, "Synergy SA", dateEvt, null, formeJuridique,
		                                                  TypeAutoriteFiscale.COMMUNE_HC, noOFSCommune, null, null,
		                                                  StatusRegistreIDE.DEFINITIF, formeJuridiqueIde, "CHE999999996");
	}

	@NotNull
	protected static EvenementOrganisation createEvent(Long noEvenement, Long noOrganisation, TypeEvenementOrganisation type, RegDate date, EtatEvenementOrganisation etat) {
		final EvenementOrganisation event = new EvenementOrganisation();
		event.setNoEvenement(noEvenement);
		event.setNoOrganisation(noOrganisation);
		event.setType(type);
		event.setDateEvenement(date);
		event.setEtat(etat);
		return event;
	}
}