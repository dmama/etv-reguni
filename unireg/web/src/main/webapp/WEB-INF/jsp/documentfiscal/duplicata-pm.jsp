<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}"/>
<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">

    <tiles:put name="body">
        <form:form name="formImpression" id="formImpression">

            <fieldset>
                <legend><span>Veuillez choisir la composition et le nombre de documents à imprimer</span></legend>
                <unireg:nextRowClass reset="0"/>

                <form:hidden path="idDI"/>
                <form:hidden path="selectedTypeDocument"/>

                <c:forEach items="${command.modelesDocumentView}" var="modele" varStatus="statusModele">

                    <form:hidden path="modelesDocumentView[${statusModele.index}].typeDocument"/>
                    <table id="model-for-${command.modelesDocumentView[statusModele.index].typeDocument}" style="display:none" border="0" cellspacing="0">
                        <c:forEach items="${modele.modelesFeuilles}" var="feuille" varStatus="statusFeuille">
                            <c:set var="rowClass"><unireg:nextRowClass/></c:set>
                            <tr class="${rowClass}">
                                <td width="10%">&nbsp;</td>
                                <td width="45%"><c:out value="${feuille.intituleFeuille}"/>&nbsp;:</td>
                                <td width="45%" style="padding-bottom:4px">
                                    <form:input path="modelesDocumentView[${statusModele.index}].modelesFeuilles[${statusFeuille.index}].nombreFeuilles"
                                                cssClass="document-type-${modele.typeDocument}" size="2" maxlength="1"/>
                                    <form:hidden path="modelesDocumentView[${statusModele.index}].modelesFeuilles[${statusFeuille.index}].intituleFeuille"/>
                                    <form:hidden path="modelesDocumentView[${statusModele.index}].modelesFeuilles[${statusFeuille.index}].noCADEV"/>
                                    <form:hidden path="modelesDocumentView[${statusModele.index}].modelesFeuilles[${statusFeuille.index}].noFormulaireACI"/>
                                    <form:hidden path="modelesDocumentView[${statusModele.index}].modelesFeuilles[${statusFeuille.index}].principal"/>
                                    <c:if test="${feuille.principal && fn:length(modele.modelesFeuilles) > 1}">
                                        <span class="info_icon" style="margin-left: 1em;" title="Feuillet principal"></span>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </table>
                </c:forEach>
                <input type="hidden" id="changerType" value="false"/>

            </fieldset>

            <script>
                function show_model_table(typeDocument){
                    $('table[id^="model-for"]').hide();
                    $('#model-for-'+typeDocument).show();
                }

                $(document).ready(function(){
                    show_model_table('${command.selectedTypeDocument}');
                });

            </script>

        </form:form>

    </tiles:put>
</tiles:insert>
