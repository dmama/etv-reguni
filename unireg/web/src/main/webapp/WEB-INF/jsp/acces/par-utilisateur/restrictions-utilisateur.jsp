<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.droits.acces.utilisateur" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/acces-par-utilisateur.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
	<tiles:put name="body">
	<form:form method="post" id="formEditRestriction"  name="theForm">
		<input type="hidden"  name="__TARGET__" value="">
		<input type="hidden"  name="__EVENT_ARGUMENT__" value="">
		<unireg:nextRowClass reset="1"/>
		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../../general/utilisateur.jsp">
			<jsp:param name="path" value="utilisateur" />
			<jsp:param name="titleKey" value="title.droits.operateur" />
		</jsp:include>
		<!-- Fin Caracteristiques generales -->
		<!-- Debut Liste des restrictions -->
		<fieldset>
		<legend><span><fmt:message key="label.caracteristiques.acces" /></span></legend>
		<authz:authorize ifAnyGranted="ROLE_SEC_DOS_ECR">
			<table border="0">
			<tr>
				<td>
					<a href="list-pp-utilisateur.do?noIndividuOperateur=${command.utilisateur.numeroIndividu}" 
					class="add" title="Ajouter">&nbsp;<fmt:message key="label.bouton.ajouter" /></a>
				</td>
			</tr>
			</table>
		</authz:authorize>
		<c:if test="${not empty command.restrictions}">
			<display:table
					name="command.restrictions" id="restriction" pagesize="10" 
					requestURI="${url}"
					class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			
				<display:column sortable ="true" titleKey="label.type.restriction">
						<fmt:message key="option.type.droit.acces.${restriction.type}"  />
				</display:column>
				<display:column sortable ="true" titleKey="label.numero.contribuable" >
						<unireg:numCTB numero="${restriction.numeroCTB}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.prenom.nom">
						<c:out value="${restriction.prenomNom}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.localite">
						<c:out value="${restriction.localite}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.date.naissance">
						<unireg:date date="${restriction.dateNaissance}"></unireg:date>
				</display:column>
				<display:column sortable ="true" titleKey="label.lecture.seule">
					<input type="checkbox" name="lectureSeule" value="True"   
							<c:if test="${restriction.lectureSeule}">checked </c:if> disabled="disabled" />
				</display:column>
				<display:column style="action">
					<unireg:consulterLog entityNature="DroitAcces"  entityId="${restriction.id}"/>
					<authz:authorize ifAnyGranted="ROLE_SEC_DOS_ECR">
						<c:if test="${!restriction.annule}">
							<unireg:raccourciAnnuler onClick="javascript:Page_AnnulerRestriction(${restriction.id});" tooltip="Annulation de la restriction"/>
						</c:if>
					</authz:authorize>
				</display:column>
				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
				
			</display:table>
			
				<script type="text/javascript">
					function Page_AnnulerRestriction(id) {
							if(confirm('Voulez-vous vraiment annuler ce droit d\'acces ?')) {
								Form.doPostBack("theForm", "annulerRestriction", id);
						 	}
				 	} 	
				</script>
			
			</c:if>
			</fieldset>
		<!-- Fin Liste des restrictions -->
		<!-- Debut Bouton -->
		<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:document.location.href='select-utilisateur.do';" />
		<c:if test="${not empty command.restrictions}">
			<input type="submit" value="<fmt:message key="label.bouton.exporter"/>" name="exporter"/>
		</c:if>
		<!-- Fin Bouton -->
		&nbsp;
	</form:form>
	</tiles:put>
</tiles:insert>