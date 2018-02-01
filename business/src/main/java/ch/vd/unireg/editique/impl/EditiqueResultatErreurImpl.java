package ch.vd.uniregctb.editique.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.technical.esb.ErrorType;
import ch.vd.uniregctb.editique.EditiqueResultatErreur;

public final class EditiqueResultatErreurImpl extends BaseEditiqueResultatImpl implements EditiqueResultatErreur {

	private static final Logger LOGGER = LoggerFactory.getLogger(EditiqueResultatErreurImpl.class);

	private final String errorMessage;
	private final ErrorType errorType;
	private final Integer errorCode;

	public EditiqueResultatErreurImpl(String idDocument, String errorMessage, ErrorType errorType, String errorCode) {
		super(idDocument);
		this.errorMessage = errorMessage;
		this.errorType = errorType;
		this.errorCode = parseErrorCode(errorCode);
	}

	private static Integer parseErrorCode(String code) {
		if (StringUtils.isBlank(code)) {
			return null;
		}
		try {
			return Integer.parseInt(code);
		}
		catch (NumberFormatException e) {
			LOGGER.warn("Code d'erreur Editique inattendu (et donc ignoré) : " + code);
			return null;
		}
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	public ErrorType getErrorType() {
		return errorType;
	}

	public Integer getErrorCode() {
		return errorCode;
	}

	@Override
	public String getToStringComplement() {
		return String.format("error='%s' (%s/%s)", errorMessage, errorType, errorCode);
	}
}
