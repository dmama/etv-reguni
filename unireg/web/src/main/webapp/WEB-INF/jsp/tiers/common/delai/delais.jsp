<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:set var="depuisTache" value="${param.depuisTache}" />
<c:if test="${not empty command.delais}">
<fieldset>
	<legend><span><fmt:message key="label.delais" /></span></legend>
	
	<display:table 	name="command.delais" id="delai" pagesize="10" class="display">
		<display:column titleKey="label.date.demande">
			<c:if test="${delai.annule}"><strike></c:if>
				<unireg:regdate regdate="${delai.dateDemande}" />
			<c:if test="${delai.annule}"></strike></c:if>
		</display:column>
		<display:column titleKey="label.date.delai.accorde">
			<c:if test="${delai.annule}"><strike></c:if>
				<unireg:regdate regdate="${delai.delaiAccordeAu}" />
			<c:if test="${delai.annule}"></strike></c:if>
		</display:column>
		<display:column titleKey="label.confirmation.editee">
			<input type="checkbox" name="decede" value="True"   
			<c:if test="${delai.confirmationEcrite}">checked </c:if> disabled="disabled" />
			<c:if test="${command.allowedDelai}">
				<c:if test="${page == 'edit' }">
					<c:if test="${delai.confirmationEcrite}">
						<a href="#" onClick="javascript:Page_ImprimerDelai(${delai.id});" class="pdf"><img src="${pageContext.request.contextPath}/images/pdf_icon.png" align="top"/></a>
					</c:if>
				</c:if>
			</c:if>
		</display:column>
		<display:column titleKey="label.date.traitement">
			<c:if test="${delai.annule}"><strike></c:if>
				<unireg:regdate regdate="${delai.dateTraitement}" />
			<c:if test="${delai.annule}"></strike></c:if>
		</display:column>
		<display:column style="action">
			<c:if test="${page == 'visu' }">
				<img src="../images/consult_off.gif" title="${delai.logModifUser}-<fmt:formatDate value="${delai.logModifDate}" pattern="dd.MM.yyyy HH:mm:ss"/>" />
			</c:if>
			<c:if test="${depuisTache == null}">
				<c:if test="${command.allowedDelai}">
					<c:if test="${page == 'edit' }">
						<c:if test="${(!delai.annule) && (!delai.first)}">
							<unireg:raccourciAnnuler onClick="javascript:Page_AnnulerDelai(${delai.id});"/>
						</c:if>
					</c:if>
				</c:if>
			</c:if>
		</display:column>

		<display:setProperty name="paging.banner.all_items_found" value=""/>
		<display:setProperty name="paging.banner.one_item_found" value=""/>
	</display:table>

	<script type="text/javascript">
		function Page_AnnulerDelai(idDelai) {
				if(confirm('Voulez-vous vraiment annuler ce delai ?')) {
					var form = F$("theForm");
					form.doPostBack("annulerDelai", idDelai);
			 	}
	 	}
		function Page_ImprimerDelai(idDelai) {
			F$("theForm").doPostBack("imprimerDelai", idDelai);
		 }
	</script>
	
</fieldset>
</c:if>