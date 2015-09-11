package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.FonctionOrganisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;

/**
 * Représente un object mock pour un site d'organisation. Le mock fait plusieurs choses:
 *
 * - Il rend modifiables les champs de l'entité.
 * - Il implémente éventuellement des mutations spécifiques, nécessaires dans un
 *   contexte de test.
 */
public class MockSiteOrganisation extends SiteOrganisation {
	public MockSiteOrganisation(long no, @NotNull List<DateRanged<String>> nom, DonneesRC rc,
	                            DonneesRegistreIDE ide,
	                            Map<String, List<DateRanged<String>>> identifiants,
	                            List<DateRanged<String>> nomsAdditionnels,
	                            List<DateRanged<TypeDeSite>> typeDeSite,
	                            List<DateRanged<Integer>> siege,
	                            List<DateRanged<FonctionOrganisation>> fonction) {
		super(no, nom, rc, ide, identifiants, nomsAdditionnels, typeDeSite, siege, fonction);
	}

	@Override
	public void setFonction(List<DateRanged<FonctionOrganisation>> fonction) {
		super.setFonction(fonction);
	}

	@Override
	public void setIde(@NotNull DonneesRegistreIDE ide) {
		super.setIde(ide);
	}

	@Override
	public void setIdentifiants(Map<String, List<DateRanged<String>>> identifiants) {
		super.setIdentifiants(identifiants);
	}

	@Override
	public void setNom(@NotNull List<DateRanged<String>> nom) {
		super.setNom(nom);
	}

	@Override
	public void setNomsAdditionnels(List<DateRanged<String>> nomsAdditionnels) {
		super.setNomsAdditionnels(nomsAdditionnels);
	}

	@Override
	public void setRc(DonneesRC rc) {
		super.setRc(rc);
	}

	@Override
	public void setSiege(List<DateRanged<Integer>> siege) {
		super.setSiege(siege);
	}

	@Override
	public void setTypeDeSite(List<DateRanged<TypeDeSite>> typeDeSite) {
		super.setTypeDeSite(typeDeSite);
	}
}
