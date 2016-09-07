package ch.vd.uniregctb.role;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

public class InfoFor implements DateRange {

	public final InfoContribuable.TypeContribuable typeCtb;
	public final RegDate dateOuverture;
	public final RegDate dateFermeture;
	public final MotifFor motifOuverture;
	public final MotifFor motifFermeture;
	public final InfoContribuable.TypeAssujettissement typeAssujettissement;
	public final InfoContribuable.TypeContribuable ancienTypeCtb;
	public final boolean forPrincipal;
	public final MotifRattachement motifRattachement;
	public final int ofsCommune;

	/**
	 * Constructeur pour les assujettissements sur la période
	 */
	public InfoFor(InfoContribuable.TypeContribuable typeCtb, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture,
	               InfoContribuable.TypeAssujettissement typeAssujettissement, boolean forPrincipal, MotifRattachement motifRattachement, int ofsCommune) {
		this(typeCtb, dateOuverture, motifOuverture, dateFermeture, motifFermeture, typeAssujettissement, null, forPrincipal, motifRattachement, ofsCommune);
		Assert.isTrue(typeAssujettissement == InfoContribuable.TypeAssujettissement.POURSUIVI_APRES_PF || typeAssujettissement == InfoContribuable.TypeAssujettissement.TERMINE_DANS_PF);
	}

	/**
	 * Constructeur pour les fors fermés dans la période fiscale, et qui ont donné lieu à une fin d'assujettissement
	 * à la fin de la période fiscale précédente
	 * @param dateOuverture date d'ouverture du for déterminant pour l'assujettissement maintenant terminé
	 * @param motifOuverture motif d'ouverture de ce même for
	 * @param dateFermeture date de fermeture du for déterminant (ne devrait pas être nulle)
	 * @param motifFermeture motif de fermeture du for déterminant (ne devrait pas être nul)
	 * @param ancienTypeCtb type du contribuable dans la période fiscale précédente (il est maintenant non-assujetti)
	 * @param ofsCommune numéro OFS de la commune du for (vaudois!)
	 */
	public InfoFor(RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, InfoContribuable.TypeContribuable ancienTypeCtb, boolean forPrincipal,
	               MotifRattachement motifRattachement, int ofsCommune) {
		this(InfoContribuable.TypeContribuable.NON_ASSUJETTI, dateOuverture, motifOuverture, dateFermeture, motifFermeture, InfoContribuable.TypeAssujettissement.NON_ASSUJETTI, ancienTypeCtb, forPrincipal, motifRattachement, ofsCommune);
	}

	/**
	 * Constructeur interne factorisé
	 */
	private InfoFor(InfoContribuable.TypeContribuable typeCtb, RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture,
	                InfoContribuable.TypeAssujettissement typeAssujettissement, InfoContribuable.TypeContribuable ancienTypeCtb, boolean forPrincipal,
	                MotifRattachement motifRattachement, int ofsCommune) {

		Assert.notNull(dateOuverture);
		Assert.notNull(motifOuverture);

		this.typeCtb = typeCtb;
		this.dateOuverture = dateOuverture;
		this.dateFermeture = dateFermeture;
		this.motifOuverture = motifOuverture;
		this.motifFermeture = motifFermeture;
		this.typeAssujettissement = typeAssujettissement;
		this.ancienTypeCtb = ancienTypeCtb;
		this.forPrincipal = forPrincipal;
		this.motifRattachement = motifRattachement;
		this.ofsCommune = ofsCommune;
	}

	@Override
	public RegDate getDateDebut() {
		return dateOuverture;
	}

	@Override
	public RegDate getDateFin() {
		return dateFermeture;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateOuverture, dateFermeture, NullDateBehavior.LATEST);
	}
}
