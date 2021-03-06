package ch.vd.unireg.tiers.rattrapage.appariement;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.tiers.Entreprise;

/**
 * Service qui traite des appariements entre Unireg et RCEnt
 * (au niveau des établissements seulement pour le moment)
 */
public interface AppariementService {

	@NotNull
	List<CandidatAppariement> rechercheAppariementsEtablissementsSecondaires(Entreprise entreprise);
}
