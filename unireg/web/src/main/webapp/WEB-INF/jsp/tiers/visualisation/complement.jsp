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
			<td>${command.tiers.personneContact}</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement" />&nbsp;:</td>
			<td>${command.tiers.complementNom}</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.numeroTelFixe" />&nbsp;:</td>
			<td>${command.tiers.numeroTelephonePrive}</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.numeroTelPortable" />&nbsp;:</td>
			<td>${command.tiers.numeroTelephonePortable}</td>
		</tr>			
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.numeroTelProfessionnel" />&nbsp;:</td>
			<td>${command.tiers.numeroTelephoneProfessionnel}</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.numeroFax" />&nbsp;:</td>
			<td>${command.tiers.numeroTelecopie}</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.email" />&nbsp;:</td>
			<td>${command.tiers.adresseCourrierElectronique}</td>
		</tr>
	</table>
</fieldset>
<fieldset>
	<legend><span><fmt:message key="label.complement.coordFinancieres" /></span></legend>
	<unireg:nextRowClass reset="1"/>
	<table>
	
		<tr class="<unireg:nextRowClass/>" >
			<td  width="25%"><fmt:message key="label.complement.numeroCompteBancaire" />&nbsp;:</td>
			<td>${command.tiers.numeroCompteBancaire}</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.titulaireCompte" />&nbsp;:</td>
			<td>${command.tiers.titulaireCompteBancaire}</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.bicSwift" />&nbsp;:</td>
			<td>${command.tiers.adresseBicSwift}</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.complement.blocageRemboursementAutomatique" />&nbsp;:</td>
			<td><input type="checkbox" name="blocageRemboursementAutomatique" value="True"   
			<c:if test="${command.tiers.blocageRemboursementAutomatique}">checked "</c:if> disabled="disabled"/>
			</td>
		</tr>
		
	</table>
</fieldset>
<fieldset>
	<legend><span><fmt:message key="label.complement.divers" /></span></legend>
	<unireg:nextRowClass reset="1"/>
	<table>
		<c:if test="{command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant'}">
			<tr class="<unireg:nextRowClass/>" >
				<td  width="25%"><fmt:message key="label.complement.ancienNumSourcier" />&nbsp;:</td>
				<td>
					${command.tiers.ancienNumeroSourcier}
				</td>
			</tr>
		</c:if>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.complement.remarque" />&nbsp;:</td>
			<td>${command.tiers.remarque}</td>
		</tr>
	</table>
</fieldset>
<!-- Fin Complements -->
		

		

		