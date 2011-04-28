/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.vd.uniregctb.utils;

import java.io.Serializable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.type.Sexe;

/**
 * Generic <b>Code Validation</b> providing format, minimum/maximum length and {@link EAN13CheckDigit} validations.
 * <p>
 * Performs the following validations on a code:
 * <ul>
 * <li>Check the <i>format</i> of the code using a <i>regular expression.</i> (if specified)</li>
 * <li>Check the <i>minimum</i> and <i>maximum</i> length (if specified) of the <i>parsed</i> code (i.e. parsed by the <i>regular
 * expression</i>).</li>
 * <li>Performs {@link EAN13CheckDigit} validation on the parsed code (if specified).</li>
 * </ul>
 * <p>
 * Configure the validator with the appropriate regular expression, minimum/maximum length and {@link EAN13CheckDigit} validator and then
 * call one of the two validation methods provided:
 * </p>
 * <ul>
 * <li><code>boolean isValid(code)</code></li>
 * <li><code>String validate(code)</code></li>
 * </ul>
 * <p>
 * Codes often include <i>format</i> characters - such as hyphens - to make them more easily human readable. These can be removed prior to
 * length and check digit validation by specifying them as a <i>non-capturing</i> group in the regular expression (i.e. use the
 * <code>(?:   )</code> notation).
 *
 * @version $Revision: 493905 $ $Date: 2007-01-07 18:11:38 -0800 (Sun, 07 Jan 2007) $
 * @since Validator 1.4
 */
public final class AVSValidator implements Serializable {

	private static final long serialVersionUID = 4812512956536812418L;

	private RegexValidator regexValidator;
	private int minLength = -1;
	private int maxLength = -1;
	private EAN13CheckDigit checkdigit;

	/**
	 * Default Constructor.
	 */
	public AVSValidator() {
	}

	/**
	 * Construct a code validator with a specified regular expression and {@link EAN13CheckDigit}.
	 *
	 * @param regex
	 *            The format regular expression
	 * @param checkdigit
	 *            The check digit validation routine
	 */
	public AVSValidator(String regex, EAN13CheckDigit checkdigit) {
		this(regex, -1, -1, checkdigit);
	}

	/**
	 * Construct a code validator with a specified regular expression, length and {@link EAN13CheckDigit}.
	 *
	 * @param regex
	 *            The format regular expression.
	 * @param length
	 *            The length of the code (sets the mimimum/maximum to the same)
	 * @param checkdigit
	 *            The check digit validation routine
	 */
	public AVSValidator(String regex, int length, EAN13CheckDigit checkdigit) {
		this(regex, length, length, checkdigit);
	}

	/**
	 * Construct a code validator with a specified regular expression, minimum/maximum length and {@link EAN13CheckDigit} validation.
	 *
	 * @param regex
	 *            The regular expression validator
	 * @param minLength
	 *            The minimum length of the code
	 * @param maxLength
	 *            The maximum length of the code
	 * @param checkdigit
	 *            The check digit validation routine
	 */
	public AVSValidator(String regex, int minLength, int maxLength, EAN13CheckDigit checkdigit) {
		setRegex(regex);
		setMinLength(minLength);
		setMaxLength(maxLength);
		setCheckDigit(checkdigit);
	}

	/**
	 * Construct a code validator with a specified regular expression, validator and {@link EAN13CheckDigit} validation.
	 *
	 * @param regexValidator
	 *            The format regular expression validator
	 * @param checkdigit
	 *            The check digit validation routine.
	 */
	public AVSValidator(RegexValidator regexValidator, EAN13CheckDigit checkdigit) {
		this(regexValidator, -1, -1, checkdigit);
	}

	/**
	 * Construct a code validator with a specified regular expression, validator, length and {@link EAN13CheckDigit} validation.
	 *
	 * @param regexValidator
	 *            The format regular expression validator
	 * @param length
	 *            The length of the code (sets the mimimum/maximum to the same value)
	 * @param checkdigit
	 *            The check digit validation routine
	 */
	public AVSValidator(RegexValidator regexValidator, int length, EAN13CheckDigit checkdigit) {
		this(regexValidator, length, length, checkdigit);
	}

	/**
	 * Construct a code validator with a specified regular expression validator, minimum/maximum length and {@link EAN13CheckDigit}
	 * validation.
	 *
	 * @param regexValidator
	 *            The format regular expression validator
	 * @param minLength
	 *            The minimum length of the code
	 * @param maxLength
	 *            The maximum length of the code
	 * @param checkdigit
	 *            The check digit validation routine
	 */
	public AVSValidator(RegexValidator regexValidator, int minLength, int maxLength, EAN13CheckDigit checkdigit) {
		setRegexValidator(regexValidator);
		setMinLength(minLength);
		setMaxLength(maxLength);
		setCheckDigit(checkdigit);
	}

	/**
	 * Return the check digit validation routine.
	 * <p>
	 * <b>N.B.</b> Optional, if not set no Check Digit validation will be performed on the code.
	 *
	 * @return The check digit validation routine
	 */
	public EAN13CheckDigit getCheckDigit() {
		return checkdigit;
	}

	/**
	 * Set the check digit validation routine.
	 * <p>
	 * <b>N.B.</b> Optional, if not set no Check Digit validation will be performed on the code.
	 *
	 * @param checkdigit
	 *            The check digit validation routine
	 */
	public void setCheckDigit(EAN13CheckDigit checkdigit) {
		this.checkdigit = checkdigit;
	}

	/**
	 * Convenience method that sets the minimum and maximum length to the same value.
	 *
	 * @param length
	 *            The length of the code
	 */
	public void setLength(int length) {
		setMinLength(length);
		setMaxLength(length);
	}

	/**
	 * Return the minimum length of the code.
	 * <p>
	 * <b>N.B.</b> Optional, if less than zero the minimum length will not be checked.
	 *
	 * @return The minimum length of the code or <code>-1</code> if the code has no minimum length
	 */
	public int getMinLength() {
		return minLength;
	}

	/**
	 * Set the minimum length of the code.
	 * <p>
	 * <b>N.B.</b> Optional, if less than zero the minimum length will not be checked.
	 *
	 * @param minLength
	 *            The minimum length of the code or <code>-1</code> if the code has no minimum length
	 */
	public void setMinLength(int minLength) {
		this.minLength = minLength;
	}

	/**
	 * Return the maximum length of the code.
	 * <p>
	 * <b>N.B.</b> Optional, if less than zero the maximum length will not be checked.
	 *
	 * @return The maximum length of the code or <code>-1</code> if the code has no maximum length
	 */
	public int getMaxLength() {
		return maxLength;
	}

	/**
	 * Set the maximum length of the code.
	 * <p>
	 * <b>N.B.</b> Optional, if less than zero the maximum length will not be checked.
	 *
	 * @param maxLength
	 *            The maximum length of the code or <code>-1</code> if the code has no maximum length
	 */
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	/**
	 * Return the <i>regular expression</i> validator.
	 * <p>
	 * <b>N.B.</b> Optional, if not set no regular expression validation will be performed on the code.
	 *
	 * @return The regular expression validator
	 */
	public RegexValidator getRegexValidator() {
		return regexValidator;
	}

	/**
	 * Set the <i>regular expression</i> used to validate the <i>format</i> of the code.
	 * <p>
	 * This is a convenience method which creates a {@link RegexValidator} with the specified regular expression.
	 * <p>
	 * <b>N.B.</b> Optional, if not set no regular expression validation will be performed on the code.
	 *
	 * @param regex
	 *            The format regular expression.
	 */
	public void setRegex(String regex) {
		if (regex != null && regex.length() > 0) {
			setRegexValidator(new RegexValidator(regex));
		}
		else {
			setRegexValidator(null);
		}
	}

	/**
	 * Set the <i>regular expression</i> validator.
	 * <p>
	 * <b>N.B.</b> Optional, if not set no regular expression validation will be performed on the code.
	 *
	 * @param regexValidator
	 *            The regular expression validator
	 */
	public void setRegexValidator(RegexValidator regexValidator) {
		this.regexValidator = regexValidator;
	}

	/* *//**
			 * Validate the code returning either <code>true</code> or <code>false</code>.
			 *
			 * @param input
			 *            The code to validate
			 * @return <code>true</code> if valid, otherwise <code>false</code>
			 */

	public boolean isValidNouveauNumAVS(String input) {
		return (validateNouveauNumAVS(input) != null);
	}

	/**
	 * Validate the code returning either the valid code or <code>null</code> if invalid.
	 *
	 * @param input
	 *            The code to validate
	 * @return The code if valid, otherwise <code>null</code> if invalid
	 */
	public Object validateNouveauNumAVS(String input) {

		String code = FormatNumeroHelper.removeSpaceAndDash(input);

		// validate/reformat using regular expression

		boolean isVal = RegexValidator.isValid(code, "756[0-9]*");
		if (!isVal) {
			return null;
		}

		// check the length
		if ((minLength >= 0 && code.length() < minLength) || (maxLength >= 0 && code.length() > maxLength)) {
			return null;
		}

		// validate the check digit
		if (checkdigit != null && !checkdigit.isValid(code)) {
			return null;
		}

		return code;

	}

	/**
	 * Validate the code returning either the valid code or <code>null</code> if invalid.
	 *
	 * @param input
	 *            The code to validate
	 * @return The code if valid, otherwise <code>null</code> if invalid
	 */
	public String validateAncienNumAVS(String input, RegDate dateNaissance,Sexe sexe) {

		String numAVS = FormatNumeroHelper.removeSpaceAndDash(input);

		if (numAVS == null || numAVS.length() == 0) {
			return null;
		}

		// validate/reformat using regular expression
		boolean isVal = RegexValidator.isValid(numAVS, "^[0-9]*$");
		if (!isVal) {
			return null;
		}

		// check the length
		if (numAVS.length() != 8 && numAVS.length() != 11) {
			return null;
		}

		// validate the check Date Naissance
		if (dateNaissance != null) {
			if (!checkDateNaissance(numAVS, dateNaissance.year())) {
				return null;
			}
		}
		// validate the sexe
		if (!checkSexe(numAVS, sexe)){
			return null;
		}
		// validate check Modulo 11

		if (!checkModulo11(numAVS)) {
			return null;
		}

		return numAVS;

	}

	private boolean checkDateNaissance(String input, int anneeNaissance) {
		final String chiffreAVS = input.substring(3, 5);
		return String.valueOf(anneeNaissance).endsWith(chiffreAVS);
	}

	/**
	 * Vérifie si le sexe codé dans le numéro avs est conforme à celui du contribuable
	 * @param num
	 * @param sexe
	 * @return boolean
	 */
	public boolean checkSexe(String num, Sexe sexe) {
		boolean isValide = false;
		String homme ="1234";
		String femme ="5678";
		String codeSexe= num.substring(5,6);
		if(sexe == Sexe.FEMININ && femme.contains(codeSexe)){
			isValide= true;
		}
		else if(sexe == Sexe.MASCULIN && homme.contains(codeSexe)){
			isValide= true;
		}

		return isValide;
	}

	public boolean checkModulo11(String num) {

		boolean isValide = false;

		if (num.length() == 11) {
			if (num.endsWith("000")) {
				// UNIREG-605: les anciens numéro AVS sur 8 positions (qui ont complétés avec 3 zéros) doivent être acceptés.
				isValide = true;
			}
			else {
				char[] sTab = num.toCharArray();
				int[] intArray = new int[sTab.length];

				for (int i = 0; i < sTab.length; i++) {
					// intArray[i] = sTab[i] - 48;
					intArray[i] = Character.getNumericValue((sTab[i]));
				}

				int somme = (intArray[0] * 5) + (intArray[1] * 4) + (intArray[2] * 3) + (intArray[3] * 2) + (intArray[4] * 7)
						+ (intArray[5] * 6) + (intArray[6] * 5) + (intArray[7] * 4) + (intArray[8] * 3) + (intArray[9] * 2);

				int modulo = somme % 11;
				int codeControleCalcule = (11 - modulo) % 11;
				// int codeControleAVS = num.charAt(10) - 48;
				int codeControleAVS = Character.getNumericValue(num.charAt(10));

				if (codeControleCalcule == codeControleAVS) {
					isValide = true;
				}
			}
		}

		return isValide;
	}

	/**
	 * Validate the code returning either <code>true</code> or <code>false</code>.
	 *
	 * @param input
	 *            The code to validate
	 * @return <code>true</code> if valid, otherwise <code>false</code>
	 */
	public boolean isValidAncienNumAVS(String input, RegDate dateNaissance, Sexe sexe) {
		return (validateAncienNumAVS(input, dateNaissance,sexe) != null);
	}

}
