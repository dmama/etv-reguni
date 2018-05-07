<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="editCommand" type="ch.vd.unireg.etiquette.EditEtiquetteTiersView"--%>
<%--@elvariable id="libelles" type="Map<java.lang.String,java.lang.String>"--%>

<unireg:setAuth var="autorisations" tiersId="${editCommand.tiersId}"/>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edit.etiquette"/>
	</tiles:put>
	<tiles:put name="body">

		<unireg:bandeauTiers numero="${editCommand.tiersId}" showLinks="false" showComplements="false" showEvenementsCivils="false" showValidation="false" showAvatar="false"/>

		<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>

		<form:form id="editEtiquetteForm" commandName="editCommand" action="edit-item.do" method="post">
			<form:hidden path="tiersId"/>
			<form:hidden path="etiquetteTiersId"/>

			<fieldset>
				<legend><span><fmt:message key="label.etiquette"/></span></legend>
				<unireg:nextRowClass reset="0"/>

				<table border="0">
					<tr class="<unireg:nextRowClass/>">
						<td style="width: 20%;"><fmt:message key="label.date.debut"/>&nbsp;:</td>
						<td style="width: 40%;">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDebut" />
								<jsp:param name="id" value="dateDebut" />
								<jsp:param name="mandatory" value="true" />
							</jsp:include>
						</td>
						<td style="width: 20%;"><fmt:message key="label.date.fin" />&nbsp;:</td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateFin" />
								<jsp:param name="id" value="dateFin" />
							</jsp:include>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.type"/>&nbsp;:</td>
						<td>
							<form:select path="codeEtiquette">
								<form:option value=""/>
								<form:options items="${libelles}"/>
							</form:select>
							<span class="mandatory">*</span>
							<form:errors path="codeEtiquette" cssClass="error"/>
						</td>
						<td colspan="2">&nbsp;</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.commentaire"/>&nbsp;:</td>
						<td colspan="3">
							<form:textarea path="commentaire" cols="50" rows="4"/>
						</td>
					</tr>
				</table>

			</fieldset>

			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" value="<fmt:message key="label.bouton.modifier"/>"></td>
					<td width="25%"><unireg:buttonTo method="get" action="/etiquette/edit-list.do" params="{tiersId:${editCommand.tiersId}}" name="label.bouton.retour"/></td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>

		</form:form>

	</tiles:put>
</tiles:insert>
