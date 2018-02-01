<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="command" type="ch.vd.unireg.entreprise.TiersVisuView"--%>

<unireg:setAuth var="autorisations" tiersId="${command.entreprise.id}"/>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.etats.pm"/></span></legend>

	<c:if test="${autorisations.etatsPM}">
		<table border="0">
			<tr>
				<td>
					<unireg:raccourciModifier link="../entreprise/etats/edit.do?id=${command.entreprise.id}" tooltip="Modifier l'état" display="label.bouton.modifier"/>
				</td>
			</tr>
		</table>
	</c:if>

	<c:if test="${not empty command.entreprise.etats}">
		<display:table name="${command.entreprise.etats}" id="etatPM" requestURI="visu.do" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
			<display:column titleKey="label.date.obtention">
				<unireg:regdate regdate="${etatPM.dateObtention}"/>
			</display:column>
			<display:column titleKey="label.type">
				<fmt:message key="option.etat.entreprise.${etatPM.type}"/>
			</display:column>
			<display:column titleKey="label.etats.type.generation">
				<fmt:message key="option.etat.entreprise.generation.${etatPM.generation}"/>
			</display:column>
			<display:column class="action">
				<unireg:consulterLog entityNature="EtatEntreprise" entityId="${etatPM.id}"/>
			</display:column>
		</display:table>
	</c:if>

</fieldset>
