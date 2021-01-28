package teste;

import ContabilityTemplateImportation.Main;
import java.util.Map;
import java.util.HashMap;

public class teste {

    public static void main(String[] args) {
        int mes = 10;
        int ano = 2020;
        String pastaEmpresa = "Ecofetal  Serviço de Auxílio Diagnóstico e Terapia Ltda";
        String pastaAnual = "Movimentos";
        String pastaMensal = "Bancos";
        String banco = "Template Caixa Fixo (5)";
        String idTemplate = "EcofetalBanco5";
        String filtroArquivo = "CAIXA;FIXO;.xlsx";
        Map<String, String> colunas = new HashMap<>();
        colunas.put("data", "A");
        //colunas.put("documento", "");
        colunas.put("pretexto", "B");
        colunas.put("historico", "F");
        colunas.put("entrada", "G");
        colunas.put("saida", "-H");
        //colunas.put("valor", "");


        System.out.println(Main.principal(
                        mes,
                        ano,
                        pastaEmpresa,
                        pastaAnual,
                        pastaMensal,
                        banco,
                        idTemplate,
                        filtroArquivo,
                        colunas
                ).replaceAll("<br>", "\n")
        );
    }

}
