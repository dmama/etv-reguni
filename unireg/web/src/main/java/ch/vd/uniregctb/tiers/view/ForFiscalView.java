package ch.vd.uniregctb.tiers.view;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ForFiscalView implements Comparable<ForFiscalView> {

	private Long id;

	private GenreImpot genreImpot;

	private MotifRattachement motifRattachement;

	private TypeAutoriteFiscale typeAutoriteFiscale;

	private Integer numeroForFiscalCommune;

	private Integer numeroForFiscalCommuneHorsCanton;

	private Integer numeroForFiscalPays;

	private RegDate dateOuverture;

	private RegDate dateFermeture;

	private RegDate dateEvenement;

	private Long numeroCtb;

	private ModeImposition modeImposition;

	private RegDate dateChangement;

	private MotifFor motifOuverture;

	private MotifFor motifFermeture;

	private MotifFor motifImposition;

	private String libFractionCommune;

	private String libCommuneHorsCanton;

	private String libPays;

	private boolean annule;

	private String natureForFiscal;

	private String natureTiers;

	private boolean dernierForPrincipal;

	private String changementModeImposition;

	private Boolean forGestion;

	/**
	 * @return the genreImpot
	 */
	public GenreImpot getGenreImpot() {
		return genreImpot;
	}

	/**
	 * @param genreImpot the genreImpot to set
	 */
	public void setGenreImpot(GenreImpot genreImpot) {
		this.genreImpot = genreImpot;
	}

	/**
	 * @return the rattachement
	 */
	public MotifRattachement getMotifRattachement() {
		return motifRattachement;
	}

	/**
	 * @param rattachement the rattachement to set
	 */
	public void setMotifRattachement(MotifRattachement rattachement) {
		this.motifRattachement = rattachement;
	}

	/**
	 * @return the typeForFiscal
	 */
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	/**
	 * @param typeForFiscal the typeForFiscal to set
	 */
	public void setTypeAutoriteFiscale(TypeAutoriteFiscale typeAutoriteFiscale) {
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}

	/**
	 * @return the numeroForFiscalCommune
	 */
	public Integer getNumeroForFiscalCommune() {
		return numeroForFiscalCommune;
	}

	/**
	 * @param numeroForFiscalCommune the numeroForFiscalCommune to set
	 */
	public void setNumeroForFiscalCommune(Integer numeroForFiscalCommune) {
		this.numeroForFiscalCommune = numeroForFiscalCommune;
	}

	/**
	 * @return the numeroForFiscalCommuneHorsCanton
	 */
	public Integer getNumeroForFiscalCommuneHorsCanton() {
		return numeroForFiscalCommuneHorsCanton;
	}

	/**
	 * @param numeroForFiscalCommuneHorsCanton the numeroForFiscalCommuneHorsCanton to set
	 */
	public void setNumeroForFiscalCommuneHorsCanton(
			Integer numeroForFiscalCommuneHorsCanton) {
		this.numeroForFiscalCommuneHorsCanton = numeroForFiscalCommuneHorsCanton;
	}

	/**
	 * @return the numeroForFiscalPays
	 */
	public Integer getNumeroForFiscalPays() {
		return numeroForFiscalPays;
	}

	/**
	 * @param numeroForFiscalPays the numeroForFiscalPays to set
	 */
	public void setNumeroForFiscalPays(Integer numeroForFiscalPays) {
		this.numeroForFiscalPays = numeroForFiscalPays;
	}

	/**
	 * @return the dateOuverture
	 */
	public RegDate getRegDateOuverture() {
		return dateOuverture;
	}

	/**
	 * @param dateOuverture the dateOuverture to set
	 */
	public void setDateOuverture(RegDate dateOuverture) {
		this.dateOuverture = dateOuverture;
	}

	/**
	 * @return the dateFermeture
	 */
	public RegDate getRegDateFermeture() {
		return dateFermeture;
	}

	/**
	 * @param dateFermeture the dateFermeture to set
	 */
	public void setDateFermeture(RegDate dateFermeture) {
		this.dateFermeture = dateFermeture;
	}

	/**
	 * @return the dateEvenement
	 */
	public RegDate getRegDateEvenement() {
		return dateEvenement;
	}

	/**
	 * @param dateEvenement the dateEvenement to set
	 */
	public void setDateEvenement(RegDate dateEvenement) {
		this.dateEvenement = dateEvenement;
	}

	/**
	 * @return the dateOuverture
	 */
	public Date getDateOuverture() {
		return "ForFiscalAutreImpot".equals(getNatureForFiscal()) ? 
				RegDate.asJavaDate(dateEvenement) :
				RegDate.asJavaDate(dateOuverture);
	}

	/**
	 * @param dateOuverture the dateOuverture to set
	 */
	public void setDateOuverture(Date dateOuverture) {
		this.dateOuverture = RegDate.get(dateOuverture);
	}

	/**
	 * @return the dateFermeture
	 */
	public Date getDateFermeture() {
		return "ForFiscalAutreImpot".equals(getNatureForFiscal()) ? 
				RegDate.asJavaDate(dateEvenement) :
				RegDate.asJavaDate(dateFermeture);
	}

	/**
	 * @param dateFermeture the dateFermeture to set
	 */
	public void setDateFermeture(Date dateFermeture) {
		this.dateFermeture = RegDate.get(dateFermeture);
	}

	/**
	 * @return the dateEvenement
	 */
	public Date getDateEvenement() {
		return RegDate.asJavaDate(dateEvenement);
	}

	/**
	 * @param dateEvenement the dateEvenement to set
	 */
	public void setDateEvenement(Date dateEvenement) {
		this.dateEvenement = RegDate.get(dateEvenement);
	}

	public ModeImposition getModeImposition() {
		return modeImposition;
	}

	public void setModeImposition(ModeImposition mode) {
		this.modeImposition = mode;
	}

	public Date getDateChangement() {
		return RegDate.asJavaDate(dateChangement);
	}

	public RegDate getRegDateChangement() {
		return dateChangement;
	}

	public void setDateChangement(Date dateChangement) {
		this.dateChangement = RegDate.get(dateChangement);
	}


	public void setDateChangement(RegDate dateChangement) {
		this.dateChangement = dateChangement;
	}

	public Long getNumeroCtb() {
		return numeroCtb;
	}

	public void setNumeroCtb(Long numeroCtb) {
		this.numeroCtb = numeroCtb;
	}

	public MotifFor getMotifOuverture() {
		return motifOuverture;
	}

	public void setMotifOuverture(MotifFor motifOuverture) {
		this.motifOuverture = motifOuverture;
	}

	public MotifFor getMotifFermeture() {
		return motifFermeture;
	}

	public void setMotifFermeture(MotifFor motifFermeture) {
		this.motifFermeture = motifFermeture;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLibFractionCommune() {
		return libFractionCommune;
	}

	public void setLibFractionCommune(String libFractionCommune) {
		this.libFractionCommune = libFractionCommune;
	}

	public String getLibCommuneHorsCanton() {
		return libCommuneHorsCanton;
	}

	public void setLibCommuneHorsCanton(String libCommuneHorsCanton) {
		this.libCommuneHorsCanton = libCommuneHorsCanton;
	}

	public String getLibPays() {
		return libPays;
	}

	/**
	 * Compare d'apres la date de ForFiscalView
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(ForFiscalView forFiscalView) {
		Date dateDebut = getDateOuverture();
		if (dateDebut == null) {
			dateDebut = getDateEvenement();
		}
		Date autreDateDebut = forFiscalView.getDateOuverture();
		if (autreDateDebut == null) {
			autreDateDebut = getDateEvenement();
		}
		int value = - dateDebut.compareTo(autreDateDebut);
		return value;
	}

	public void setLibPays(String libPays) {
		this.libPays = libPays;
	}

	public String getNatureForFiscal() {
		return natureForFiscal;
	}

	public void setNatureForFiscal(String natureForFiscal) {
		this.natureForFiscal = natureForFiscal;
	}

	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public String getNatureTiers() {
		return natureTiers;
	}

	public void setNatureTiers(String natureTiers) {
		this.natureTiers = natureTiers;
	}

	public boolean isDernierForPrincipal() {
		return dernierForPrincipal;
	}

	public void setDernierForPrincipal(boolean dernierForPrincipal) {
		this.dernierForPrincipal = dernierForPrincipal;
	}

	public String getChangementModeImposition() {
		return changementModeImposition;
	}

	public void setChangementModeImposition(String changementModeImposition) {
		this.changementModeImposition = changementModeImposition;
	}

	public Boolean getForGestion() {
		return forGestion;
	}

	public void setForGestion(Boolean forGestion) {
		this.forGestion = forGestion;
	}

	public MotifFor getMotifImposition() {
		return motifImposition;
	}

	public void setMotifImposition(MotifFor motifImposition) {
		this.motifImposition = motifImposition;
	}

}
