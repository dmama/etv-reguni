package ch.vd.uniregctb.role;

import java.util.ArrayList;
import java.util.List;

import ch.vd.unireg.common.NomPrenom;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Informations sur un contribuable PP dans le fichier des rôles pour une periode fiscale
 */
public class InfoContribuablePP extends InfoContribuable<InfoContribuablePP> {

	private final List<NomPrenom> nomsPrenoms;
	private final List<String> nosAvs;

	public InfoContribuablePP(ContribuableImpositionPersonnesPhysiques ctb, int annee, AdresseService adresseService, TiersService tiersService) {
		super(ctb, annee, adresseService);

		nomsPrenoms = new ArrayList<>(2);
		nosAvs = new ArrayList<>(2);
		fillNomsPrenomsEtNosAvs(ctb, annee, tiersService, nomsPrenoms, nosAvs);
	}

	private InfoContribuablePP(InfoContribuablePP original) {
		super(original);
		this.nomsPrenoms = original.nomsPrenoms;
		this.nosAvs = original.nosAvs;
	}

	public List<NomPrenom> getNomsPrenoms() {
		return nomsPrenoms;
	}

	public List<String> getNosAvs() {
		return nosAvs;
	}

	@Override
	public InfoContribuablePP duplicate() {
		return new InfoContribuablePP(this);
	}

	private static void fillNomsPrenomsEtNosAvs(ContribuableImpositionPersonnesPhysiques ctb, int annee, TiersService tiersService, List<NomPrenom> nomsPrenoms, List<String> nosAvs) {
		if (ctb instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) ctb;
			final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(pp, false);
			final String noAvs = tiersService.getNumeroAssureSocial(pp);
			nomsPrenoms.add(nomPrenom);
			nosAvs.add(noAvs);
		}
		else if (ctb instanceof MenageCommun) {
			final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple((MenageCommun) ctb, annee);
			final PersonnePhysique principal = ensemble.getPrincipal();
			final PersonnePhysique conjoint = ensemble.getConjoint();
			if (principal != null) {
				nomsPrenoms.add(tiersService.getDecompositionNomPrenom(principal, false));
				nosAvs.add(tiersService.getNumeroAssureSocial(principal));
			}
			if (conjoint != null) {
				nomsPrenoms.add(tiersService.getDecompositionNomPrenom(conjoint, false));
				nosAvs.add(tiersService.getNumeroAssureSocial(conjoint));
			}
		}
	}
}
