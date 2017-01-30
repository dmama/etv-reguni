<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
    <tiles:put name="title"><fmt:message key="label.impression.fourre.neutre" /></tiles:put>
    <tiles:put name="fichierAide"><li><a href="#" onClick="ouvrirAide('<c:url value="/docs/maj-di.pdf"/>');" title="AccessKey: a" accesskey="e">Aide</a></li></tiles:put>
    <tiles:put name="body">

        <unireg:nextRowClass reset="1"/>
        <unireg:bandeauTiers numero="${command.tiersId}" showAvatar="true" showValidation="false" showEvenementsCivils="false" showLinks="false" />

        <form:form name="theForm" method="post" action="imprimer.do">

            <input type="hidden" name="tiersId" value="${command.tiersId}"/>
            <input type="hidden" name="periodes" value="${periodes}"/>


                <!-- Debut fourre -->
                <fieldset class="information">
                    <legend><span><fmt:message key="label.caracteristiques.fourre.neutre" /></span></legend>


                    <table border="0">
                        <tr class="<unireg:nextRowClass/>" >
                            <td width="25%"><fmt:message key="label.periode.fiscale" />&nbsp;:</td>
                            <td width="25%">
                                <form:select path="periodeFiscale">
                                    <form:option value="null" ><fmt:message key="option.selectionner" /></form:option>
                                    <form:options items="${periodes}" />
                                </form:select><span style="color: red;">*</span>
                                <form:errors path="periodeFiscale" cssClass="error"/>
                            <td width="25%"></td>
                            <td width="25%"></td>
                        </tr>

                    </table>

                </fieldset>
                <!-- Fin fourre -->

            <!-- Debut Boutons -->
            <input type="button" name="retourFourreNeutre" value="<fmt:message key="label.bouton.retour" />" onclick="Page_RetourVisualisation(${command.tiersId});" />

            <input type="button" name="imprimerFourreNeutre" value="<fmt:message key="label.bouton.imprimer"/>" onclick="return Page_ImprimerFourreNeutre(this);"/>

            <!-- Fin Boutons -->

        </form:form>

        <script type="text/javascript">
            /**
             * Initialisation de l'observeur du flag 'modifier'
             */
            Modifier.attachObserver( "theForm", false);
            Modifier.messageSaveSubmitConfirmation = 'Voulez-vous vraiment imprimer cette fourre neutre?';
            Modifier.messageOverConfirmation = "Voulez-vous vraiment quitter cette page sans imprimer la fourre neutre ?";

            function Page_RetourVisualisation(numero) {
                if (Modifier.isModified) {
                    if (!confirm(Modifier.messageOverConfirmation)) {
                        return false;
                    }
                }
                document.location.href='../tiers/visu.do?id=' + numero ;
                return true;
            }

            function Page_ImprimerFourreNeutre(button) {
                $('span.error').hide(); // on cache d'éventuelles erreurs datant d'un ancien submit
                Modifier.isModified = false; // du moment qu'on imprie la fourre, les valeurs saisies ont été prises en compte
                $(button).closest("form").submit();
                button.disabled = true;
                return true;
            }

        </script>

    </tiles:put>
</tiles:insert>