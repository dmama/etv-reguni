<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.taches" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/taches.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="get" id="formRechercheTache">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>
		</form:form>

		<display:table name="taches" id="tache" pagesize="25" requestURI="/tache/list.do" class="display_table"  sort="external" partialList="true" size="resultSize" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncune.tache.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.tache.trouvee" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.taches.trouvees" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.taches.trouvees" /></span></display:setProperty>
	
			<display:column sortable ="true" titleKey="label.type.tache" sortName="class">
				<fmt:message key="option.type.tache.${tache.typeTache}"  />
				<c:if test="${tache.commentaire != null}">
					<div style="float: right;" class="info_icon" title="<c:out value="${tache.commentaire}"/>">&nbsp;</div>
				</c:if>
			</display:column>
			<display:column titleKey="label.type.document">
				<c:choose>
					<c:when test="${tache.typeTache == 'TacheEnvoiDeclarationImpotPP' || tache.typeTache == 'TacheEnvoiDeclarationImpotPM' || tache.typeTache == 'TacheEnvoiQuestionnaireSNC'}">
						<fmt:message key="option.type.document.${tache.typeDocument}"/>
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</display:column>
			<display:column titleKey="label.type.contribuable">
				<c:choose>
					<c:when test="${tache.typeTache == 'TacheEnvoiDeclarationImpotPP' || tache.typeTache == 'TacheEnvoiDeclarationImpotPM'}">
						<fmt:message key="option.type.contribuable.${tache.typeContribuable}"/>
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</display:column>
			<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" sortName="contribuable.numero">
				<c:choose>
					<c:when test="${tache.etatTache == 'EN_INSTANCE' && !tache.annule}">
						<c:choose>
							<c:when test="${tache.typeTache == 'TacheControleDossier' && tache.authDossier.donneesFiscales}">
								<a href="../tiers/visu.do?id=${tache.numero}&idTacheTraite=${tache.id}"><unireg:numCTB numero="${tache.numero}" /></a>
							</c:when>
							<c:when test="${tache.typeTache == 'TacheTransmissionDossier' && tache.authDossier.mouvements}">
								<a href="../mouvement/edit.do?numero=${tache.numero}&depuisTache=true&idTacheTraite=${tache.id}"><unireg:numCTB numero="${tache.numero}" /></a>
							</c:when>
							<c:when test="${tache.typeTache == 'TacheEnvoiDeclarationImpotPP' && tache.authDossier.declarationImpots}">
								<a href="../di/imprimer-pp.do?depuisTache=true&tiersId=${tache.numero}&debut=<unireg:regdate regdate="${tache.dateDebutImposition}" format="yyyyMMdd"/>&fin=<unireg:regdate regdate="${tache.dateFinImposition}" format="yyyyMMdd"/>&typeDocument=${tache.typeDocument}&delaiRetour=${tache.delaiRetourEnJours}" ><unireg:numCTB numero="${tache.numero}" /></a>
							</c:when>
							<c:when test="${tache.typeTache == 'TacheEnvoiDeclarationImpotPM' && tache.authDossier.declarationImpots}">
								<a href="../di/imprimer-pm.do?depuisTache=true&tiersId=${tache.numero}&debut=<unireg:regdate regdate="${tache.dateDebutImposition}" format="yyyyMMdd"/>&fin=<unireg:regdate regdate="${tache.dateFinImposition}" format="yyyyMMdd"/>&typeDocument=${tache.typeDocument}" ><unireg:numCTB numero="${tache.numero}" /></a>
							</c:when>
							<c:when test="${tache.typeTache == 'TacheAnnulationDeclarationImpot' && tache.authDossier.declarationImpots}">
								<a href="../di/editer.do?id=${tache.idDI}&tacheId=${tache.id}"><unireg:numCTB numero="${tache.numero}" /></a>
							</c:when>
							<c:when test="${tache.typeTache == 'TacheEnvoiQuestionnaireSNC' && tache.authDossier.questionnairesSNC}">
								<a href="../qsnc/add.do?depuisTache=true&tiersId=${tache.numero}&pf=${tache.annee}"><unireg:numCTB numero="${tache.numero}" /></a>
							</c:when>
							<c:when test="${tache.typeTache == 'TacheAnnulationQuestionnaireSNC' && tache.authDossier.questionnairesSNC}">
								<a href="../qsnc/editer.do?depuisTache=true&id=${tache.idDI}"><unireg:numCTB numero="${tache.numero}"/></a>
							</c:when>
							<c:otherwise>
								<unireg:numCTB numero="${tache.numero}" />
							</c:otherwise>
						</c:choose>
					</c:when>
					<c:otherwise>
						<unireg:numCTB numero="${tache.numero}" />
					</c:otherwise>
				</c:choose>
			</display:column>
			<display:column titleKey="label.nom.raison" >
				<unireg:multiline lines="${tache.nomCourrier}"/>
			</display:column>
			<display:column titleKey="label.for.gestion" >
				<unireg:commune ofs="${tache.numeroForGestion}" displayProperty="nomOfficiel"/>
			</display:column>
			<display:column titleKey="label.office.impot" >
				${tache.officeImpot}
			</display:column>

			<!-- Spécifique à l'envoi des DIs -->
			<display:column titleKey="label.date.periodeImposition" >
				<c:if test="${tache.dateDebutImposition != null}">
					<fmt:message key="label.date.periodeImpositionDetaillee">
						<fmt:param><unireg:date date="${tache.dateDebutImposition}"/></fmt:param>
						<fmt:param><unireg:date date="${tache.dateFinImposition}"/></fmt:param>
					</fmt:message>
					<c:if test="${tache.longueurPeriodeImposition != null}">
						<c:choose>
							<c:when test="${tache.longueurPeriodeImposition > 1}">
								<fmt:message key="label.x.jours.parentheses">
									<fmt:param>${tache.longueurPeriodeImposition}</fmt:param>
								</fmt:message>
							</c:when>
							<c:when test="${tache.longueurPeriodeImposition == 1}">
								<fmt:message key="label.un.jour.parentheses"/>
							</c:when>
						</c:choose>
					</c:if>
				</c:if>
			</display:column>
			<display:column titleKey="label.date.enregistrement" sortable="true" sortName="logCreationDate">
				<fmt:formatDate value="${tache.dateEnregistrement}" pattern="dd.MM.yyyy HH:mm:ss"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.etat.tache" sortName="etat">
				<fmt:message key="option.etat.tache.${tache.etatTache}"  />
			</display:column>
			<display:column>
				<unireg:consulterLog entityNature="Tache" entityId="${tache.id}"/>
			</display:column>
			<!-- Fin -->
			
		</display:table>
		
	</tiles:put>
</tiles:insert>
