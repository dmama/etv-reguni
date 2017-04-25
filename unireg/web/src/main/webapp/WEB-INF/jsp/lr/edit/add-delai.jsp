<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="idDebiteur" type="java.lang.Long"--%>
<%--@elvariable id="pf" type="java.lang.Integer"--%>
<%--@elvariable id="dateDebut" type="ch.vd.registre.base.date.RegDate"--%>
<%--@elvariable id="dateFin" type="ch.vd.registre.base.date.RegDate"--%>
<%--@elvariable id="idListe" type="java.lang.Long"--%>
<%--@elvariable id="ancienDelai" type="ch.vd.registre.base.date.RegDate"--%>
<%--@elvariable id="addDelaiCommand" type="ch.vd.uniregctb.lr.view.DelaiAddView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<tiles:put name="title">
  		<fmt:message key="title.ajout.delai.lr">
  			<fmt:param>${pf}</fmt:param>
  			<fmt:param><unireg:date date="${dateDebut}"/></fmt:param>
  			<fmt:param><unireg:date date="${dateFin}"/></fmt:param>
  			<fmt:param><unireg:numCTB numero="${idDebiteur}"/></fmt:param>
  		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<form:form name="formAddDelai" id="formAddDelai" commandName="addDelaiCommand">
		<fieldset><legend><span><fmt:message key="label.delais" /></span></legend>
		<table border="0">
			<unireg:nextRowClass reset="0"/>
			<tr class="<unireg:nextRowClass/>" >
				<td colspan="2" width="50%">&nbsp;</td>
				<td width="25%"><fmt:message key="label.date.ancien.delai"/>&nbsp;:</td>
				<td width="25%"><unireg:date date="${ancienDelai}"/></td>
			</tr>
			<tr class="<unireg:nextRowClass/>" >
				<td width="25%"><fmt:message key="label.date.demande"/>&nbsp;:</td>
				<td width="25%">
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="dateDemande" />
						<jsp:param name="id" value="dateDemande" />
						<jsp:param name="mandatory" value="true" />
					</jsp:include>
				</td>
				<td><fmt:message key="label.date.delai.accorde"/>&nbsp;:</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="delaiAccorde" />
						<jsp:param name="id" value="delaiAccorde" />
						<jsp:param name="mandatory" value="true" />
					</jsp:include>
				</td>
			</tr>
		</table>
		</fieldset>
		<table>
			<tr>
				<td width="25%">&nbsp;</td>
				<td width="25%">
					<input type="submit" id="ajouter" value="<fmt:message key="label.bouton.ajouter"/>"/>
				</td>				
				<td width="25%">
					<input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler"/>" onclick="document.location.href='edit-lr.do?action=editdi&id=${idListe}'" />
				</td>
				<td width="25%">&nbsp;</td>
			</tr>
		</table>
	</form:form>
	</tiles:put>
</tiles:insert>
