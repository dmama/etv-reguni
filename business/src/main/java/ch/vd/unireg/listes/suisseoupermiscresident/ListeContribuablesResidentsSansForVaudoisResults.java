package ch.vd.unireg.listes.suisseoupermiscresident;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.ListesResults;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;

public class ListeContribuablesResidentsSansForVaudoisResults extends ListesResults<ListeContribuablesResidentsSansForVaudoisResults> {

	private final List<Long> contribuablesIdentifies = new LinkedList<>();

	private final List<InfoContribuableIgnore> contribuablesIgnores = new LinkedList<>();

	public enum CauseIgnorance {
		MENAGE_FERME("Ménage commun non actif"),
		DECEDE("Décédé"),
		MINEUR("Mineur"),
		DOMICILE_NON_VAUDOIS("Domicile non vaudois"),
		ETRANGER_SANS_PERMIS_C("Etranger sans permis C");

		private final String description;

		CauseIgnorance(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	public static class InfoContribuableIgnore {

		public final long ctbId;
		public final CauseIgnorance cause;

		public InfoContribuableIgnore(long ctbId, CauseIgnorance cause) {
			this.ctbId = ctbId;
			this.cause = cause;
		}
	}

	public ListeContribuablesResidentsSansForVaudoisResults(RegDate dateTraitement, int nombreThreads, TiersService tiersService, AdresseService adresseService) {
		super(dateTraitement, nombreThreads, tiersService, adresseService);
	}

	@Override
	public void addContribuable(Contribuable ctb) throws Exception {
		contribuablesIdentifies.add(ctb.getNumero());
	}

	@Override
	public void addTiersEnErreur(Tiers tiers) {
		// rien à faire, l'erreur a déjà été logguée par ailleurs
	}

	public void addContribuableIgnore(Contribuable ctb, CauseIgnorance cause) {
		contribuablesIgnores.add(new InfoContribuableIgnore(ctb.getNumero(), cause));
	}

	@Override
	public void addAll(ListeContribuablesResidentsSansForVaudoisResults sources) {
		super.addAll(sources);
		contribuablesIdentifies.addAll(sources.contribuablesIdentifies);
		contribuablesIgnores.addAll(sources.contribuablesIgnores);
	}

	public List<Long> getContribuablesIdentifies() {
		return contribuablesIdentifies;
	}

	public List<InfoContribuableIgnore> getContribuablesIgnores() {
		return contribuablesIgnores;
	}

	public int getNombreContribuablesInspectes() {
		return contribuablesIdentifies.size() + contribuablesIgnores.size() + getListeErreurs().size();
	}

	@Override
	public void sort() {
		super.sort();

		Collections.sort(contribuablesIdentifies);
		contribuablesIgnores.sort(Comparator.comparingLong(c -> c.ctbId));
	}
}
