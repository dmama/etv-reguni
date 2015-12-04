<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<table>
	<tr>
		<td width="50%" valign="top">
			<fieldset>
				<legend><span><fmt:message key="label.exercices.commerciaux"/></span></legend>

				<c:if test="${empty command.exercicesCommerciaux}">
					<fmt:message key="no.data" />
				</c:if>

				<c:if test="${not empty command.exercicesCommerciaux}">
					<display:table name="${command.exercicesCommerciaux}" id="exercices" requestURI="visu.do" class="display">
						<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
							<unireg:regdate regdate="${exercices.dateDebut}"/>
						</display:column>
						<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
							<unireg:regdate regdate="${exercices.dateFin}"/>
						</display:column>
					</display:table>
				</c:if>
			</fieldset>
		</td>
		<td valign="top">
			<fieldset>
				<legend><span><fmt:message key="label.date.bouclement.futur"/></span></legend>
				<unireg:nextRowClass reset="1"/>
				<table>
					<tr class="<unireg:nextRowClass/>">
						<td width="50%"><fmt:message key="label.date.bouclement.futur"/>&nbsp;:</td>
						<td><unireg:regdate regdate="${command.dateBouclementFutur}"/></td>
					</tr>
				</table>
			</fieldset>
		</td>
	</tr>
</table>

