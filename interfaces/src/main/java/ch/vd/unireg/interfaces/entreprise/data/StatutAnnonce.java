package ch.vd.unireg.interfaces.entreprise.data;

/**
 * Les statuts possibles sur une demande d'annonce (entreprises).
 *
 * @author Raphaël Marmier, 2016-08-19, <raphael.marmier@vd.ch>
 */
public enum StatutAnnonce {
	AUTRE,
	A_TRANSMETTRE,
	A_ANALYSER,
	TRANSMIS,
	ACCEPTE_IDE,
	REFUSE_IDE,
	REJET_RCENT,
	VALIDATION_SANS_ERREUR,
	ACCEPTE_REE,
	REFUSE_REE
}
