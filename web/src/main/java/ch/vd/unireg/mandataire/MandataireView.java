package ch.vd.unireg.mandataire;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeMandat;

/**
 * Classe de base des vues représentant des mandats du point de vue du mandant (-> vers les mandataires)
 */
public abstract class MandataireView implements Annulable, DateRange {

	private final long id;
	private final TypeMandat typeMandat;
	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final boolean annule;
	private final String nomRaisonSociale;

	protected <T extends DateRange & Annulable> MandataireView(long id, T mandat, TypeMandat typeMandat, String nomRaisonSociale) {
		this.id = id;
		this.typeMandat = typeMandat;
		this.dateDebut = mandat.getDateDebut();
		this.dateFin = mandat.getDateFin();
		this.annule = mandat.isAnnule();
		this.nomRaisonSociale = nomRaisonSociale;
	}

	protected static String getNomRaisonSociale(long idTiers, TiersService tiersService) {
		final Tiers tiers = tiersService.getTiers(idTiers);
		return tiersService.getNomRaisonSociale(tiers);
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !annule && DateRange.super.isValidAt(date);
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public long getId() {
		return id;
	}

	public TypeMandat getTypeMandat() {
		return typeMandat;
	}

	public String getNomRaisonSociale() {
		return nomRaisonSociale;
	}
}
