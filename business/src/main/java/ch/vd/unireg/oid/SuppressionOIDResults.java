package ch.vd.unireg.oid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.tiers.TiersService;

public class SuppressionOIDResults extends JobResults<Long, SuppressionOIDResults> {

	public enum ErreurType {
		OID_INCONNU("impossible de calculer l'oid courant du tiers."),
		EXCEPTION("une erreur inattendue s'est produite");

		private final String description;

		ErreurType(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public static class Traite extends Info {

		private final String raison;

		public Traite(long noCtb, Integer officeImpotID, String nomCtb, Set<String> tables) {
			super(noCtb, officeImpotID, null, nomCtb);

			final List<String> list = new ArrayList<>(tables);
			Collections.sort(list);

			final StringBuilder s = new StringBuilder();
			for (int i = 0, listSize = list.size(); i < listSize; i++) {
				final String category = list.get(i);
				s.append(category);
				if (i < listSize - 1) {
					s.append(", ");
				}
			}
			this.raison = "Tables impactées : " + s.toString();
		}

		@Override
		public String getDescriptionRaison() {
			return raison;
		}
	}

	public static class Erreur extends Info {
		public final ErreurType raison;

		public Erreur(long noCtb, Integer officeImpotID, ErreurType raison, String details, String nomCtb) {
			super(noCtb, officeImpotID, details, nomCtb);
			this.raison = raison;
		}

		@Override
		public String getDescriptionRaison() {
			return raison.description;
		}
	}

	public final int oid;
	public final RegDate dateTraitement;
	public int total;
	public final List<Traite> traites = new ArrayList<>();
	public final List<Erreur> errors = new ArrayList<>();
	public boolean interrompu;

	public SuppressionOIDResults(int oid, RegDate dateTraitement, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.oid = oid;
		this.dateTraitement = dateTraitement;
	}

	@Override
	public void addAll(SuppressionOIDResults right) {
		this.total += right.total;
		this.traites.addAll(right.traites);
		this.errors.addAll(right.errors);
	}

	public void addTraite(Long id, Integer officeImpotId, Set<String> tables) {
		traites.add(new Traite(id, officeImpotId, getNom(id), tables));
	}

	public void addOIDInconnu(Long id) {
		errors.add(new Erreur(id, null, ErreurType.OID_INCONNU, null, getNom(id)));
	}

	@Override
	public void addErrorException(Long id, Exception e) {
		errors.add(new Erreur(id, null, ErreurType.EXCEPTION, e.getMessage(), getNom(id)));
	}
}
