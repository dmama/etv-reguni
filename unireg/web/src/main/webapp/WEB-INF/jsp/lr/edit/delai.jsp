<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
  		<fmt:message key="title.ajout.delai.lr">
  			<fmt:param>${command.declarationPeriode}</fmt:param>
  			<fmt:param><unireg:date date="${command.declarationRange.dateDebut}"/></fmt:param>
  			<fmt:param><unireg:date date="${command.declarationRange.dateFin}"/></fmt:param>
  			<fmt:param><unireg:numCTB numero="${command.tiersId}"/></fmt:param>
  		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<form:form name="formAddDelai" id="formAddDelai">
		<fieldset><legend><span><fmt:message key="label.delais" /></span></legend>
		<table border="0">
			<unireg:nextRowClass reset="0"/>
			<tr class="<unireg:nextRowClass/>" >
				<td/>
				<td/>
				<td><fmt:message key="label.date.ancien.delai"/>&nbsp;:</td>
				<td><unireg:date date="${command.oldDelaiAccorde}"/></td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.date.demande"/>&nbsp;:</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="dateDemande" />
						<jsp:param name="id" value="dateDemande" />
					</jsp:include>
					<FONT COLOR="#FF0000">*</FONT>
				</td>
				<td><fmt:message key="label.date.delai.accorde"/>&nbsp;:</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="delaiAccordeAu" />
						<jsp:param name="id" value="delaiAccordeAu" />
					</jsp:include>
					<FONT COLOR="#FF0000">*</FONT>
				</td>
			</tr>
			<unireg:nextRowClass reset="0"/>
			<tr class="<unireg:nextRowClass/>" >
				<td><fmt:message key="label.confirmation.ecrite"/>&nbsp;:</td>
				<td>
					<form:checkbox path="confirmationEcrite" />
				</td>
				<td>&nbsp;</td>
				<td>&nbsp;</td>
			</tr>
		</table>
		</fieldset>
		<table>
			<tr>
				<td width="25%">&nbsp;</td>
				<td width="25%">
					<input type="submit" id="ajouter" value="Ajouter"/>
				</td>				
				<td width="25%">
					<input type="button" id="annuler" value="Annuler" onclick="document.location.href='../lr/edit.do?action=editdi&id=' + ${command.idDeclaration}" />
				</td>
				<td width="25%">&nbsp;</td>
			</tr>
		</table>
	</form:form>
	</tiles:put>
</tiles:insert>
