
package ch.vd.uniregctb.webservices.batch;

import javax.xml.bind.annotation.XmlEnum;


/**
 * <p>Java class for jobSynchronousMode.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="jobSynchronousMode">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="ASYNCHRONOUS"/>
 *     &lt;enumeration value="SYNCHRONOUS"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlEnum
public enum JobSynchronousMode {

    ASYNCHRONOUS,
    SYNCHRONOUS;

    public String value() {
        return name();
    }

    public static JobSynchronousMode fromValue(String v) {
        return valueOf(v);
    }

}
