<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Complements -->
<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>
<c:if test="${autorisations.complementsCommunications}">
	<table border="0">
		<tr>
			<td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../complements/communications/edit.do?id=${command.tiers.numero}" tooltip="Modifier les points de communications" display="label.bouton.modifier"/>
				</c:if>
			</td>
		</tr>
	</table>
</c:if>
<fieldset>
	<legend><span><fmt:message key="label.complement.pointCommunication" /></span></legend>
	<unireg:nextRowClass reset="1"/>
	<table>
	
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.complement.contact" />&nbsp;:</td>
			<td><c:out value="${command.complement.personneContact}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement" />&nbsp;:</td>
			<td><c:out value="${command.complement.complementNom}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.numeroTelFixe" />&nbsp;:</td>
			<td><c:out value="${command.complement.numeroTelephonePrive}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.numeroTelPortable" />&nbsp;:</td>
			<td><c:out value="${command.complement.numeroTelephonePortable}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.numeroTelProfessionnel" />&nbsp;:</td>
			<td><c:out value="${command.complement.numeroTelephoneProfessionnel}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.numeroFax" />&nbsp;:</td>
			<td><c:out value="${command.complement.numeroTelecopie}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.email" />&nbsp;:</td>
			<td><c:out value="${command.complement.adresseCourrierElectronique}"/></td>
		</tr>
	</table>
</fieldset>

<c:if test="${autorisations.complementsCoordonneesFinancieres}">
	<table border="0" style="margin-top: 0.5em;">
		<tr>
			<td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../complements/coordfinancieres/edit.do?id=${command.tiers.numero}" tooltip="Modifier les coordonnées financières" display="label.bouton.modifier"/>
				</c:if>
			</td>
		</tr>
	</table>
</c:if>
<fieldset>
	<legend><span><fmt:message key="label.complement.coordFinancieres" /></span></legend>
	<unireg:nextRowClass reset="1"/>
	<c:if test="${command.complement.compteBancaire != null}">
		<table>

			<tr class="<unireg:nextRowClass/>" >
				<td  width="25%"><fmt:message key="label.complement.numeroCompteBancaire" />&nbsp;:</td>
				<td><c:out value="${command.complement.compteBancaire.iban}"/>
					<c:if test="${command.complement.compteBancaire.ibanValidationMessage != null}">
						<span class="global-error">
							<fmt:message key="error.iban"/>&nbsp;<c:out value="(${command.complement.compteBancaire.ibanValidationMessage})"/>
						</span>
					</c:if>
				</td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.complement.titulaireCompte" />&nbsp;:</td>
				<td><c:out value="${command.complement.compteBancaire.titulaireCompteBancaire}"/></td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.complement.bicSwift" />&nbsp;:</td>
				<td><c:out value="${command.complement.compteBancaire.adresseBicSwift}"/></td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.complement.blocageRemboursementAutomatique" />&nbsp;:</td>
				<td><input type="checkbox" name="blocageRemboursementAutomatique" value="true"
					<c:if test="${command.complement.blocageRemboursementAutomatique}">checked </c:if> disabled="disabled"/>
				</td>
			</tr>

		</table>
	</c:if>
</fieldset>
<!-- Fin Complements -->
		

		

		