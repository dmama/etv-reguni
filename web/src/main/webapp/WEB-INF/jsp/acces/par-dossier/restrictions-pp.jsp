<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title"><fmt:message key="title.droits.acces.dossier" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/acces-par-dossier.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<!-- Debut Caracteristiques generales -->
		<jsp:include page="../../general/pp.jsp">
			<jsp:param name="page" value="acces" />
			<jsp:param name="path" value="dossier" />
		</jsp:include>
		<!-- Fin Caracteristiques generales -->
		<!-- Debut Liste des restrictions -->
		<fieldset>
		<legend><span><fmt:message key="label.caracteristiques.acces" /></span></legend>
		<authz:authorize ifAnyGranted="ROLE_SEC_DOS_ECR">
			<table border="0">
			<tr>
				<td>
					<a href="ajouter-restriction.do?numero=${command.dossier.numero}" class="add" title="Ajouter"><fmt:message key="label.bouton.ajouter"/></a>
				</td>
			</tr>
			</table>
		</authz:authorize>
		<c:if test="${not empty command.restrictions}">
			<display:table
					name="command.restrictions" id="restriction" pagesize="10" sort="list"
					requestURI="${url}"
					class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
			
				<display:column sortable ="true" titleKey="label.type.restriction">
						<fmt:message key="option.type.droit.acces.${restriction.type}"  />
				</display:column>
				<display:column sortable ="true" titleKey="label.visa.operateur" property="visaOperateur"/>
				<display:column sortable ="true" titleKey="label.prenom.nom" property="prenomNom"/>
				<display:column sortable ="true" titleKey="label.date.debut" sortProperty="dateDebut" sortName="dateDebut">
					<unireg:regdate regdate="${restriction.dateDebut}" format="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.fin" sortProperty="dateFin" sortName="dateFin">
					<unireg:regdate regdate="${restriction.dateFin}" format="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.office.impot" style="width: 40%;" property="officeImpot"/>
				<display:column sortable ="true" titleKey="label.lecture.seule">
					<input type="checkbox" name="lectureSeule" value="True"   
							<c:if test="${restriction.lectureSeule}">checked </c:if> disabled="disabled" />
				</display:column>
				<display:column style="white-space:nowrap">
					<unireg:consulterLog entityNature="DroitAcces" entityId="${restriction.id}"/>
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
							App.executeAction('post:/acces/par-dossier/annuler-restriction.do?id=' + id + '&ctbId=${command.dossier.numero}');
					    }
				 	} 	
				</script>
			
			</c:if>
			</fieldset>
		<!-- Fin Liste des restrictions -->
		<!-- Debut Bouton -->
		<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='../par-dossier.do';" />
		<!-- Fin Bouton -->
		&nbsp;
	</tiles:put>
</tiles:insert>
