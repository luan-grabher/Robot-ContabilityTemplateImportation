package ContabilityTemplateImportation;

import Entity.Executavel;
import Robo.AppRobo;
import TemplateContabil.Control.ControleTemplates;
import TemplateContabil.Model.Entity.Importation;
import fileManager.Args;
import fileManager.FileManager;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.ini4j.Ini;

public class Main {

    private static String nomeApp = "";

    public static void main(String[] args) {
        try {
            AppRobo robo = new AppRobo(nomeApp);
            robo.definirParametros();

            String iniPath = "\\\\heimerdinger\\docs\\Informatica\\Programas\\Moresco\\Robos\\Contabilidade\\TemplateImportacao\\";
            String iniName = robo.getParametro("ini").getString();

            Ini ini = new Ini(FileManager.getFile(iniPath + iniName));

            String pastaEmpresa = ini.get("Pastas", "empresa");
            String pastaAnual = ini.get("Pastas", "anual");
            String pastaMensal = ini.get("Pastas", "mensal");

            int mes = robo.getParametro("mes").getMes();
            int ano = robo.getParametro("ano").getInteger();

            nomeApp = "Importação " + pastaEmpresa + " - " + ini.get("Config", "nome") + " " + mes + "/" + ano;

            StringBuilder returnExecutions = new StringBuilder();

            String[] templates = ini.get("Config", "templates").split(";");
            //Para cada template pega as informações
            for (String template : templates) {
                template = !template.equals("") ? " " + template : "";

                String nomeTemplate = ini.get("Template" + template, "nome");
                String idTemplate = ini.get("Template" + template, "id");
                String filtroArquivo = ini.get("Template" + template, "filtroArquivo");
                String tipo = ini.get("Template" + template, "tipo");

                Map<String, Map<String, String>> colunas = new HashMap<>();
                if (tipo.equals("excel")) {
                    colunas.put("data", getCollumnConfig("data", ini.get("Colunas" + template, "data")));
                    colunas.put("documento", getCollumnConfig("documento", ini.get("Colunas" + template, "documento")));
                    colunas.put("pretexto", getCollumnConfig("pretexto", ini.get("Colunas" + template, "pretexto")));
                    colunas.put("historico", getCollumnConfig("historico", ini.get("Colunas" + template, "historico")));
                    colunas.put("entrada", getCollumnConfig("entrada", ini.get("Colunas" + template, "entrada")));
                    colunas.put("saida", getCollumnConfig("saida", ini.get("Colunas" + template, "saida")));
                    colunas.put("valor", getCollumnConfig("valor", ini.get("Colunas" + template, "valor")));
                }

                returnExecutions.append("\n").append(
                        principal(mes, ano, pastaEmpresa, pastaAnual, pastaMensal, nomeTemplate, idTemplate, filtroArquivo, tipo, colunas)
                );
            }

            robo.setNome(nomeApp);
            robo.executar(returnExecutions.toString());
        } catch (Exception e) {
            System.out.println("Ocorreu um erro na aplicação: " + e);
            System.exit(0);
        }
    }

    public static Map<String, String> getCollumnConfig(String collumn, String iniString) {
        Map<String, String> config = new HashMap<>();
        String[] configs = iniString.split("¬", -1);

        config.put("name", collumn);
        config.put("collumn", Args.get(configs, "collumn"));
        config.put("regex", Args.get(configs, "regex"));
        config.put("replace", Args.get(configs, "replace"));
        config.put("type", Args.get(configs, "type"));
        config.put("dateFormat", Args.get(configs, "dateFormat"));
        config.put("required", Args.get(configs, "required"));
        config.put("requiredBlank", Args.get(configs, "requiredBlank"));
        config.put("unifyDown", Args.get(configs, "unifyDown"));
        config.put("forceNegativeIf", Args.get(configs, "forceNegativeIf"));

        return config;
    }

    public static String principal(int mes, int ano, String pastaEmpresa, String pastaAnual, String pastaMensal, String nomeTemplate, String idTemplate, String filtroArquivo, String tipo, Map<String, Map<String, String>> colunas) {
        try {
            Importation importation = new Importation();
            importation.setTIPO(tipo.equals("excel") ? Importation.TIPO_EXCEL : Importation.TIPO_OFX);
            importation.getXlsxCols().putAll(colunas);

            importation.setIdTemplateConfig(idTemplate);
            importation.setNome(nomeTemplate);

            ControleTemplates controle = new ControleTemplates(mes, ano);
            controle.setPastaEscMensal(pastaEmpresa);
            controle.setPasta(pastaAnual, pastaMensal);

            Map<String, Executavel> execs = new LinkedHashMap<>();
            execs.put("Procurando arquivo " + nomeTemplate, controle.new defineArquivoNaImportacao(filtroArquivo, importation));
            execs.put("Criando template " + nomeTemplate, controle.new converterArquivoParaTemplate(importation));

            return AppRobo.rodarExecutaveis(nomeApp, execs);
        } catch (Exception e) {
            return "Ocorreu um erro no Java: " + e;
        }
    }

}
