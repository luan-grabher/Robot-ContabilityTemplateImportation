package ContabilityTemplateImportation;

import Entity.Executavel;
import JExcel.XLSX;
import Robo.AppRobo;
import TemplateContabil.Control.ControleTemplates;
import TemplateContabil.Model.Entity.Importation;
import fileManager.FileManager;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.ini4j.Ini;

public class Main {

    private static String nomeApp = "";
    private static Ini ini = null;

    public static String testParameters = "";

    public static void main(String[] args) {
        try {
            AppRobo robo = new AppRobo(nomeApp);

            if (args.length > 0 && args[0].equals("test")) {
                robo.definirParametros(testParameters);
            } else {
                robo.definirParametros();
            }

            String iniPath = "\\\\heimerdinger\\docs\\Informatica\\Programas\\Moresco\\Robos\\Contabilidade\\TemplateImportacao\\";
            String iniName = robo.getParametro("ini");

            ini = new Ini(FileManager.getFile(iniPath + iniName + ".ini"));

            String pastaEmpresa = ini.get("Pastas", "empresa");
            String pastaAnual = ini.get("Pastas", "anual");
            String pastaMensal = ini.get("Pastas", "mensal");

            int mes = Integer.valueOf(robo.getParametro("mes"));
            mes = mes >= 1 && mes <= 12 ? mes : 1;
            int ano = Integer.valueOf(robo.getParametro("ano"));

            nomeApp = "Importação " + pastaEmpresa + " - " + ini.get("Config", "nome") + " " + mes + "/" + ano;

            StringBuilder returnExecutions = new StringBuilder();

            String[] templates = ini.get("Config", "templates").split(";");
            //Para cada template pega as informações
            for (String template : templates) {
                template = !template.equals("") ? " " + template : "";

                String comparar = template + (template.equals("") ? "" : " ") + "Comparar";

                Map<String, Object> templateConfig = getTemplateConfig(template);
                Map<String, Object> compararConfig = getTemplateConfig(comparar);

                returnExecutions.append("\n").append(
                        start(mes, ano, pastaEmpresa, pastaAnual, pastaMensal, templateConfig, compararConfig)
                );
            }

            robo.setNome(nomeApp);
            robo.executar(returnExecutions.toString());
        } catch (Exception e) {
            e.printStackTrace();
            FileManager.save(new File(System.getProperty("user.home")) + "\\Desktop\\JavaError.txt", getStackTrace(e));
            System.out.println("Ocorreu um erro na aplicação: " + e);
            System.exit(0);
        }
    }

    private static String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        return sw.toString();
    }

    /**
     * Retorna as configurações do template selecionado
     *
     * @param template Nome do template na seção ini
     */
    private static Map<String, Object> getTemplateConfig(String template) {

        //Se não encontrar a seção do template, retorna null
        if (ini.get("Template" + template, "nome") == null) {
            return null;
        }

        Map<String, Object> templateConfig = new HashMap<>();
        templateConfig.put("nome", ini.get("Template" + template, "nome"));
        templateConfig.put("id", ini.get("Template" + template, "id"));
        templateConfig.put("filtroArquivo", ini.get("Template" + template, "filtroArquivo"));
        templateConfig.put("tipo", ini.get("Template" + template, "tipo"));
        templateConfig.put("colunas", getTemplateColsConfig((String) templateConfig.get("tipo"), template));

        return templateConfig;
    }

    /**
     * Retorna a configuração de colunas da seção "Colunas NOME-TEMPLATE" no
     * arquivo ini
     *
     * @param template Nome do template na seção ini
     * @param tipo Tipo do arquivo, para este metodo funcionar deve ser "excel"
     * @return configuração de colunas da seção "Colunas NOME-TEMPLATE" no
     * arquivo ini
     */
    private static Map<String, Map<String, String>> getTemplateColsConfig(String tipo, String template) {
        Map<String, Map<String, String>> colunas = new HashMap<>();
        if (tipo.equals("excel")) {
            colunas.put("data", getCollumnConfig("data", template));
            colunas.put("documento", getCollumnConfig("documento", template));
            colunas.put("pretexto", getCollumnConfig("pretexto", template));
            colunas.put("historico", getCollumnConfig("historico", template));
            colunas.put("entrada", getCollumnConfig("entrada", template));
            colunas.put("saida", getCollumnConfig("saida", template));
            colunas.put("valor", getCollumnConfig("valor", template));
        }

        return colunas;
    }

    private static Map<String, String> getCollumnConfig(String collumnName, String template) {
        return XLSX.getCollumnConfigFromString(collumnName, ini.get("Colunas" + template, collumnName));
    }

    public static String start(int mes, int ano, String pastaEmpresa, String pastaAnual, String pastaMensal, Map<String, Object> templateConfig, Map<String, Object> compararConfig) {
        Importation importation = new Importation();
        importation.setTIPO(templateConfig.get("tipo").equals("excel") ? Importation.TIPO_EXCEL : Importation.TIPO_OFX);
        importation.setIdTemplateConfig((String) templateConfig.get("id"));
        importation.setNome((String) templateConfig.get("nome"));
        importation.getXlsxCols().putAll((Map<String, Map<String, String>>) templateConfig.get("colunas"));

        Importation importationC = null;
        if (compararConfig != null) {
            importationC = new Importation();
            importationC.setTIPO(compararConfig.get("tipo").equals("excel") ? Importation.TIPO_EXCEL : Importation.TIPO_OFX);
            importationC.setIdTemplateConfig((String) compararConfig.get("id"));
            importationC.setNome((String) compararConfig.get("nome"));
            importationC.getXlsxCols().putAll((Map<String, Map<String, String>>) compararConfig.get("colunas"));
        }

        ControleTemplates controle = new ControleTemplates(mes, ano);
        controle.setPastaEscMensal(pastaEmpresa);
        controle.setPasta(pastaAnual, pastaMensal);

        Map<String, Executavel> execs = new LinkedHashMap<>();
        execs.put("Procurando arquivo " + templateConfig.get("filtroArquivo"), controle.new defineArquivoNaImportacao((String) templateConfig.get("filtroArquivo"), importation));

        if (compararConfig != null) {
            execs.put("Procurando arquivo " + compararConfig.get("filtroArquivo"), controle.new defineArquivoNaImportacao((String) compararConfig.get("filtroArquivo"), importation));
        }

        execs.put("Criando template " + templateConfig.get("nome"), controle.new converterArquivoParaTemplate(importation, importationC));

        return AppRobo.rodarExecutaveis(nomeApp, execs);
    }

}
