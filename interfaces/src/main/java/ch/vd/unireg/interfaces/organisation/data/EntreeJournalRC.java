package ch.vd.unireg.interfaces.organisation.data;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;

/**
 * @author Raphaël Marmier, 2016-06-13, <raphael.marmier@vd.ch>
 */
public class EntreeJournalRC implements Serializable {

	private static final long serialVersionUID = -8712830699880613304L;

	private final TypeEntree type;
	private final RegDate date;
	private final Long numero;
	private final PublicationFOSC publicationFOSC;

	public EntreeJournalRC(TypeEntree type, @NotNull RegDate date, @NotNull Long numero, @NotNull PublicationFOSC publicationFOSC) {
		this.type = type;
		this.date = date;
		this.numero = numero;
		this.publicationFOSC = publicationFOSC;
	}

	public enum TypeEntree {
		AUTRE,
		NORMAL,
		RECTIFICATION,
		COMPLEMENT
	}

	public TypeEntree getType() {
		return type;
	}

	public RegDate getDate() {
		return date;
	}

	public Long getNumero() {
		return numero;
	}

	public PublicationFOSC getPublicationFOSC() {
		return publicationFOSC;
	}
}
