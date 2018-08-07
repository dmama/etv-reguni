<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Complements -->
<%--@elvariable id="command" type="ch.vd.unireg.tiers.view.TiersVisuView"--%>
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
					<unireg:raccourciModifier link="../complements/coordfinancieres/list.do?tiersId=${command.tiers.numero}" tooltip="Modifier les coordonnées financières" display="label.bouton.modifier"/>
				</c:if>
			</td>
		</tr>
	</table>
</c:if>
<fieldset>
	<legend><span><fmt:message key="label.complement.coordFinancieres" /></span></legend>
	<input class="noprint" name="coordonnees_histo" type="checkbox" <c:if test="${command.coordonneesHisto}">checked</c:if> onClick="window.location.href = App.toggleBooleanParam(window.location, 'coordonneesHisto', true);" id="isCoordonneesHisto" />
	<label class="noprint" for="isCoordonneesHisto"><fmt:message key="label.historique" /></label>
	<unireg:nextRowClass reset="1"/>
	<display:table name="command.complement.coordonneesFinancieres" id="coordoonnees" pagesize="10" class="display" requestURI="visu.do" decorator="ch.vd.unireg.decorator.TableEntityDecorator" sort="list">
		<display:setProperty name="basic.empty.showtable" value="false"/>
		<display:setProperty name="basic.msg.empty_list" value=""/>
		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.some_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
		<display:setProperty name="paging.banner.no_items_found" value=""/>
		<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
			<unireg:regdate regdate="${coordoonnees.dateDebut}"/>
		</display:column>
		<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
			<unireg:regdate regdate="${coordoonnees.dateFin}"/>
		</display:column>
		<display:column sortable="false" titleKey="label.complement.numeroCompteBancaire">
			<c:out value="${coordoonnees.iban}"/>
			<c:if test="${coordoonnees.ibanValidationMessage != null}">
				<span class="global-error"><fmt:message key="error.iban"/>&nbsp;<c:out value="(${coordoonnees.ibanValidationMessage})"/></span>
			</c:if>
		</display:column>
		<display:column sortable="false" titleKey="label.complement.titulaireCompte">
			<c:out value="${coordoonnees.titulaireCompteBancaire}"/>
		</display:column>
		<display:column sortable="false" titleKey="label.complement.bicSwift">
			<c:out value="${coordoonnees.adresseBicSwift}"/>
		</display:column>
		<display:column class="action">
			<unireg:consulterLog entityNature="CoordonneesFinancieres" entityId="${coordoonnees.id}"/>
		</display:column>
	</display:table>
	<table>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.complement.blocageRemboursementAutomatique" />&nbsp;:</td>
			<td><input type="checkbox" name="blocageRemboursementAutomatique" value="true"
				<c:if test="${command.complement.blocageRemboursementAutomatique}">checked </c:if> disabled="disabled"/>
			</td>
		</tr>
	</table>
</fieldset>
<!-- Fin Complements -->
		

		

		