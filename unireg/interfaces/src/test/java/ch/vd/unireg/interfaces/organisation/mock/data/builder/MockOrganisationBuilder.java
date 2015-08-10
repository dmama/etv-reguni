package ch.vd.unireg.interfaces.organisation.mock.data.builder;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRCBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.DonneesRegistreIDEBuilder;
import ch.vd.unireg.interfaces.organisation.data.builder.OrganisationBuilder;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;

/**
 * Construit un objet mock pour une organisation
 */
public class MockOrganisationBuilder extends OrganisationBuilder {
	public MockOrganisationBuilder(long cantonalId) {
		super(cantonalId);
	}

	public MockOrganisationBuilder(long cantonalId, @NotNull List<DateRanged<String>> nom) {
		super(cantonalId, nom);
	}

	@Override
	public Organisation build() {
		return new MockOrganisation(
				getCantonalId(),
				getIdentifiants(),
				getNom(),
				getNomsAdditionnels(),
				getFormeLegale(),
				getSites(),
				getDonneesSites(),
				getTransfereA(),
				getTransferDe(),
				getRemplacePar(),
				getEnRemplacementDe()
		);
	}

	public static MockOrganisation createDummySA(long cantonalId, String nom, RegDate dateDebut) {
		long siteCantonalId = cantonalId + 1111;
		return (MockOrganisation) new MockOrganisationBuilder(cantonalId)
				.addNom(dateDebut, null, nom)
				.addIdentifiant("CT.VD.PARTY", dateDebut, null, String.valueOf(cantonalId))
				.addSite(dateDebut, null, siteCantonalId)
				.addFormeLegale(dateDebut, null, FormeLegale.N_0106_SOCIETE_ANONYME)
				.addDonneesSite(
						new MockSiteOrganisationBuilder(siteCantonalId)
								.addNom(dateDebut, null, nom)
								.addIdentifiant("CT.VD.PARTY", dateDebut, null, String.valueOf(siteCantonalId))
								.addTypeDeSite(dateDebut, null, TypeDeSite.ETABLISSEMENT_PRINCIPAL)
								.withRC(
										new DonneesRCBuilder()
												.addNom(dateDebut, null, nom)
												.addStatus(dateDebut, null, StatusRC.INSCRIT)
												.addStatusInscription(dateDebut, null, StatusInscriptionRC.ACTIF)
												.build()
								)
								.withIde(
										new DonneesRegistreIDEBuilder()
												.addTypeOrganisation(dateDebut, null, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE)
												.addStatus(dateDebut, null, StatusRegistreIDE.DEFINITIF)
												.build()

								)
								.build()
				)
				.build();
	}
}
