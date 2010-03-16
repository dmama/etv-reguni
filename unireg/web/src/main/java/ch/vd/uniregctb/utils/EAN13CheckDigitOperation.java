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

/**
 * Modulus 10 <b>EAN-13</b> Check Digit calculation/validation.
 * <p>
 * Check digit calculation is based on <i>modulus 10</i> with digits in
 * an <i>odd</i> position (from right to left) being weighted 1 and <i>even</i>
 * position digits being weighted 3.
 * <p>
 * For further information see:
 *
 */
public final class EAN13CheckDigitOperation extends EAN13ModulusCheckDigit implements Serializable {

	private static final long serialVersionUID = 3771978906131872773L;

	/** Singleton EAN-13 Check Digit instance */
    public static final EAN13CheckDigit INSTANCE = new EAN13CheckDigitOperation();

    /** weighting given to digits depending on their right position */
    private static final int[] POSITION_WEIGHT = new int[] {3, 1};

    /**
     * Construct a modulus 10 Check Digit routine for EAN/UPC.
     */
    public EAN13CheckDigitOperation() {
        super(10);
    }

    /**
     * <p>Calculates the <i>weighted</i> value of a character in the
     * code at a specified position.</p>
     *
     * <p>For EAN-13 (from right to left) <b>odd</b> digits are weighted
     * with a factor of <b>one</b> and <b>even</b> digits with a factor
     * of <b>three</b>.</p>
     *
     * @param charValue The numeric value of the character.
     * @param leftPos The position of the character in the code, counting from left to right
     * @param rightPos The positionof the character in the code, counting from right to left
     * @return The weighted value of the character.
     */
    @Override
	protected int weightedValue(int charValue, int leftPos, int rightPos) {
        int weight = POSITION_WEIGHT[rightPos % 2];
        return (charValue * weight);
    }
}
