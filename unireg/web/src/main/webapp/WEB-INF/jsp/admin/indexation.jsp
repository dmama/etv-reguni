<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title"><fmt:message key="label.gestion.indexation" /></tiles:put>
  	
  	<tiles:put name="body">

	    <form:form method="post" servletRelativeAction="/admin/indexation/reindexTiers.do" >
			<fieldset>
				<legend><span><fmt:message key="label.force.reindexation" /></span></legend>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td><fmt:message key="label.numero.tiers"/>&nbsp;:</td>
						<td>
							<form:input path="id" size ="15" cssErrorClass="input-with-errors"/>
							<form:errors path="id" cssClass="error"/>
						</td>
						<td>
							<input type="submit" value="<fmt:message key="label.bouton.forcer.reindexation"/>"/>
						</td>
					</tr>
				</table>
			</fieldset>
	    </form:form>

	    <form:form method="post" servletRelativeAction="/admin/indexation/reloadIndividu.do">
		    <fieldset>
				<legend><span>Rafraichissement du cache des individus</span></legend>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td>Numéro d'individu&nbsp;:</td>
						<td>
							<form:input path="indNo" size ="15" cssErrorClass="input-with-errors"/>
							<form:errors path="indNo" cssClass="error"/>
							<form:checkbox path="logIndividu" id="logIndividuCheckBox"/>
							<label for="logIndividuCheckBox">Avec log de rechargement</label>
						</td>
						<td>
							<input type="submit" value="Recharger"/>
						</td>
					</tr>
				</table>
			</fieldset>
	    </form:form>

	    <%--@elvariable id="cheminIndex" type="java.lang.String"--%>
	    <%--@elvariable id="nombreDocumentsIndexes" type="java.lang.Integer"--%>
		<fieldset>
			<legend><span><fmt:message key="label.effacement.index" /></span></legend>
			<table>
				<tr class="<unireg:nextRowClass/>" >
					<td width="50%"><fmt:message key="label.chemin.index" />&nbsp;:</td>
					<td width="50%">${cheminIndex}</td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="50%"><fmt:message key="label.nombre.documents.indexes" />&nbsp;:</td>
					<td width="50%">${nombreDocumentsIndexes}</td>
				</tr>
			</table>
		</fieldset>

	    <form:form method="get" servletRelativeAction="/admin/indexation/show.do">
		    <fieldset>
				<legend><span><fmt:message key="label.requete.lucene" /></span></legend>
			    <unireg:nextRowClass reset="1"/>

			    <table>
					<tr class="<unireg:nextRowClass/>" >
						<td width="30%"><fmt:message key="label.requete.lucene" />&nbsp;:</td>
						<td>
							<form:input path="requete" id="requete" size ="100" cssErrorClass="input-with-errors"/>
							<form:errors path="requete" cssClass="error"/>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>" >
						<td></td>
						<td>
							<input type="submit" value="<fmt:message key="label.bouton.rechercher"/>"/>
						</td>
					</tr>
				</table>

				<display:table name="docs" id="docs" pagesize="30" defaultsort="1" requestURI="/admin/indexation/show.do" sort="list">
					<display:column property="entityId" titleKey="label.index.entityId" />
					<display:column property="nomCourrier1" titleKey="label.index.nomCourrier1" />
					<display:column property="nomCourrier2" titleKey="label.index.nomCourrier2" />
					<display:column property="nomFor" titleKey="label.index.nomFor" />
					<display:column property="npa" titleKey="label.index.npa" />
					<display:column property="localite" titleKey="label.index.localite" />
					<display:column property="dateNaissance" titleKey="label.index.dateNaissance" />
					<display:column property="numeroAvs" titleKey="label.index.numeroAvs" />
				</display:table>

			</fieldset>
		</form:form>

  	</tiles:put>
</tiles:insert>
