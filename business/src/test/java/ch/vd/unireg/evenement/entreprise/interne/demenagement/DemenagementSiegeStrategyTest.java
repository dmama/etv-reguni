package ch.vd.unireg.evenement.entreprise.interne.demenagement;

import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.interne.TraitementManuel;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.Domicile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.entreprise.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.service.mock.MockServiceEntreprise;
import ch.vd.unireg.tiers.DomicileHisto;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementEntreprise;

import static ch.vd.unireg.type.EtatEvenementEntreprise.A_TRAITER;
import static ch.vd.unireg.type.TypeEvenementEntreprise.REE_MUTATION;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

public class DemenagementSiegeStrategyTest extends WithoutSpringTest {

	private final MockServiceEntreprise serviceEntreprise = new MockServiceEntreprise() {

		@Override
		public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE annonceIDE) {
			return null;
		}

		@NotNull
		@Override
		public String createEntrepriseDescription(EntrepriseCivile entrepriseCivile, RegDate date) {
			return "UT: Org no 1L.";
		}
	};
	private final EvenementEntrepriseContext spyContext = Mockito.spy(new EvenementEntrepriseContext(serviceEntreprise, null, null, null));

	private final EvenementEntrepriseOptions options = new EvenementEntrepriseOptions();

	private final DemenagementSiegeStrategy strategy = new DemenagementSiegeStrategy(spyContext, options);

	MockEntrepriseCivile entrepriseCivile = MockEntrepriseFactory
			.createEntreprise(1L, 1L, "Synergy SA", RegDate.get(2010, 6, 26), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITEE,
			                  TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
			                  StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996");


	@Test
	public void testRecuperationEtablissementPasseParTiersService() throws Exception {
		final EvenementEntreprise event = createEvent(1000000L, 1L, REE_MUTATION, RegDate.get(2015, 6, 24), A_TRAITER);

		final MockEntrepriseCivile spyEntrepriseCivile = Mockito.spy(entrepriseCivile);
		doReturn(null).when(spyEntrepriseCivile).getSiegePrincipal(event.getDateEvenement().getOneDayBefore());
		doReturn(null).when(spyEntrepriseCivile).getNom(event.getDateEvenement().getOneDayBefore());

		final Entreprise entreprise = Mockito.mock(Entreprise.class);

		final DemenagementSiegeStrategy spyStrategy = Mockito.spy(this.strategy);
		final TiersService mockTiersService = Mockito.mock(TiersService.class);
		doReturn(mockTiersService).when(spyContext).getTiersService();
		doReturn(Collections.singletonList(new DateRanged(RegDate.get(2014, 6, 24), null, new Etablissement()))).when(mockTiersService).getEtablissementsPrincipauxEntreprise(entreprise);
		final Domicile domicile = new Domicile(RegDate.get(2014, 6, 24), null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, 1234);
		doReturn(Collections.singletonList(new DomicileHisto(domicile))).when(mockTiersService).getDomiciles(any(Etablissement.class), any(Boolean.class));

		final TraitementManuel eventAndMatch = (TraitementManuel) spyStrategy.matchAndCreate(event, spyEntrepriseCivile, entreprise);

		Assert.assertNotNull(eventAndMatch);
		Mockito.verify(mockTiersService, Mockito.atLeast(1)).getEtablissementsPrincipauxEntreprise(entreprise);
	}


	@NotNull
	private static EvenementEntreprise createEvent(Long noEvenement, Long noEntrepriseCivile, TypeEvenementEntreprise type, RegDate date, EtatEvenementEntreprise etat) {
		return getEvenementEntreprise(noEvenement, noEntrepriseCivile, type, date, etat);
	}

	@NotNull
	public static EvenementEntreprise getEvenementEntreprise(Long noEvenement, Long noEntrepriseCivile, TypeEvenementEntreprise type, RegDate date, EtatEvenementEntreprise etat) {
		final EvenementEntreprise event = new EvenementEntreprise();
		event.setId(noEvenement);
		event.setNoEntrepriseCivile(noEntrepriseCivile);
		event.setType(type);
		event.setDateEvenement(date);
		event.setEtat(etat);
		return event;
	}

}