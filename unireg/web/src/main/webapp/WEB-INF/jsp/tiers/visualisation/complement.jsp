<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Complements -->
<c:if test="${command.allowedOnglet.CPLT}">
	<table border="0">
		<tr>
			<td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../complement/edit.do?id=${command.tiers.numero}" tooltip="Modifier les compléments" display="label.bouton.modifier"/>
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
			<td><c:out value="${command.tiers.personneContact}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement" />&nbsp;:</td>
			<td><c:out value="${command.tiers.complementNom}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.numeroTelFixe" />&nbsp;:</td>
			<td><c:out value="${command.tiers.numeroTelephonePrive}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.numeroTelPortable" />&nbsp;:</td>
			<td><c:out value="${command.tiers.numeroTelephonePortable}"/></td>
		</tr>			
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.numeroTelProfessionnel" />&nbsp;:</td>
			<td><c:out value="${command.tiers.numeroTelephoneProfessionnel}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.numeroFax" />&nbsp;:</td>
			<td><c:out value="${command.tiers.numeroTelecopie}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.email" />&nbsp;:</td>
			<td><c:out value="${command.tiers.adresseCourrierElectronique}"/></td>
		</tr>
	</table>
</fieldset>
<fieldset>
	<legend><span><fmt:message key="label.complement.coordFinancieres" /></span></legend>
	<unireg:nextRowClass reset="1"/>
	<table>
	
		<tr class="<unireg:nextRowClass/>" >
			<td  width="25%"><fmt:message key="label.complement.numeroCompteBancaire" />&nbsp;:</td>
			<td><c:out value="${command.tiers.numeroCompteBancaire}"/>
				<c:if test="${command.ibanValidationMessage != null}">
					<span class="global-error">
						<fmt:message key="error.iban"/>&nbsp;<c:out value="(${command.ibanValidationMessage})"/>
					</span>
				</c:if>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.titulaireCompte" />&nbsp;:</td>
			<td><c:out value="${command.tiers.titulaireCompteBancaire}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.bicSwift" />&nbsp;:</td>
			<td><c:out value="${command.tiers.adresseBicSwift}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.blocageRemboursementAutomatique" />&nbsp;:</td>
			<td><input type="checkbox" name="blocageRemboursementAutomatique" value="true"
				<c:if test="${command.tiers.blocageRemboursementAutomatique}">checked </c:if> disabled="disabled"/>
			</td>
		</tr>
		
	</table>
</fieldset>
<c:if test="{command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant'}">
<fieldset>
	<legend><span><fmt:message key="label.complement.divers" /></span></legend>
	<unireg:nextRowClass reset="1"/>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td  width="25%"><fmt:message key="label.complement.ancienNumSourcier" />&nbsp;:</td>
			<td>
				<c:out value="${command.tiers.ancienNumeroSourcier}"/>
			</td>
		</tr>
	</table>
</fieldset>
</c:if>
<!-- Fin Complements -->
		

		

		