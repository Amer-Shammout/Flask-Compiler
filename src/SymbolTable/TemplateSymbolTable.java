//package SymbolTable;
//
//import java.util.*;
//
//public class TemplateSymbolTable {
//
//    // اسم البلوك → الـ JinjaBlockStmt
//    private final Map<String, Object> blocks = new LinkedHashMap<>();
//
//    // المتغيرات المحلية (مثلاً من جملة for)
//    private final Set<String> variables = new LinkedHashSet<>();
//
//    // القوالب المضمّنة
//    private final List<String> includes = new ArrayList<>();
//
//    // القالب الذي يتم تمديده
//    private String extendsTemplate = null;
//
//    public void addBlock(String name, Object block) {
//        blocks.put(name, block);
//    }
//
//    public void addVariable(String varName) {
//        variables.add(varName);
//    }
//
//    public void addInclude(String templateName) {
//        includes.add(templateName);
//    }
//
//    public void setExtendsTemplate(String templateName) {
//        extendsTemplate = templateName;
//    }
//
//    public Map<String, Object> getBlocks() {
//        return blocks;
//    }
//
////    public Set<String> getVariables() {
////        return variables;
////    }
//Set<String> globalVariables;
//    Set<String> loopVariables;
//    Set<String> functions;
//    Set<String> filters;
//    Set<String> attributes;
//
//
//    public List<String> getIncludes() {
//        return includes;
//    }
//
//    public String getExtendsTemplate() {
//        return extendsTemplate;
//    }
//
//    @Override
//    public String toString() {
//        return "TemplateSymbolTable{" +
//                "blocks=" + blocks.keySet() +
//                ", variables=" + variables +
//                ", includes=" + includes +
//                ", extendsTemplate='" + extendsTemplate + '\'' +
//                '}';
//    }
//    public void printTable() {
//        System.out.println("\n================ TEMPLATE SYMBOL TABLE ================\n");
//
//        printBlocks();
//        printVariables();
//        printIncludes();
//        printExtends();
//
//        System.out.println("=======================================================\n");
//    }
//    private void printBlocks() {
//        System.out.println("Blocks:");
//        System.out.println("------------------------------------------------------");
//        System.out.printf("| %-15s | %-30s |\n", "Name", "Type");
//        System.out.println("------------------------------------------------------");
//
//        if (blocks.isEmpty()) {
//            System.out.printf("| %-48s |\n", "(none)");
//        } else {
//            for (var entry : blocks.entrySet()) {
//                System.out.printf(
//                        "| %-15s | %-30s |\n",
//                        entry.getKey(),
//                        entry.getValue().getClass().getSimpleName()
//                );
//            }
//        }
//        System.out.println("------------------------------------------------------\n");
//    }
//    private void printVariables() {
//        System.out.println("Variables:");
//        System.out.println("------------------------------------------------------");
//
//        if (variables.isEmpty()) {
//            System.out.printf("| %-48s |\n", "(none)");
//        } else {
//            for (String var : variables) {
//                System.out.printf("| %-48s |\n", var);
//            }
//        }
//        System.out.println("------------------------------------------------------\n");
//    }
//    private void printIncludes() {
//        System.out.println("Includes:");
//        System.out.println("------------------------------------------------------");
//
//        if (includes.isEmpty()) {
//            System.out.printf("| %-48s |\n", "(none)");
//        } else {
//            for (String inc : includes) {
//                System.out.printf("| %-48s |\n", inc);
//            }
//        }
//        System.out.println("------------------------------------------------------\n");
//    }
//    private void printExtends() {
//        System.out.println("Extends:");
//        System.out.println("------------------------------------------------------");
//
//        if (extendsTemplate == null) {
//            System.out.printf("| %-48s |\n", "(none)");
//        } else {
//            System.out.printf("| %-48s |\n", extendsTemplate);
//        }
//        System.out.println("------------------------------------------------------\n");
//    }
//
//}
package SymbolTable;

import java.util.*;

public class TemplateSymbolTable {

    private final Map<String, Object> blocks = new LinkedHashMap<>();

    private final Set<String> globalVariables = new LinkedHashSet<>();
    private final Set<String> loopVariables   = new LinkedHashSet<>();

    private final Set<String> functions = new LinkedHashSet<>();
    private final Set<String> filters   = new LinkedHashSet<>();


    private final Set<String> attributes = new LinkedHashSet<>();


    private final List<String> includes = new ArrayList<>();
    private String extendsTemplate = null;



    public void addBlock(String name, Object block) {
        blocks.put(name, block);
    }

    public void addGlobalVariable(String name) {
        globalVariables.add(name);
    }

    public void addLoopVariable(String name) {
        loopVariables.add(name);
    }

    public void addFunction(String name) {
        functions.add(name);
    }

    public void addFilter(String name) {
        filters.add(name);
    }

    public void addAttribute(String attr) {
        attributes.add(attr);
    }

    public void addInclude(String templateName) {
        includes.add(templateName);
    }

    public void setExtendsTemplate(String templateName) {
        extendsTemplate = templateName;
    }



    public Map<String, Object> getBlocks() { return blocks; }
    public Set<String> getGlobalVariables() { return globalVariables; }
    public Set<String> getLoopVariables() { return loopVariables; }
    public Set<String> getFunctions() { return functions; }
    public Set<String> getFilters() { return filters; }
    public Set<String> getAttributes() { return attributes; }
    public List<String> getIncludes() { return includes; }
    public String getExtendsTemplate() { return extendsTemplate; }



    public void print() {
        System.out.println("\n================ TEMPLATE SYMBOL TABLE ================\n");

        System.out.println("Blocks:");
        blocks.forEach((k, v) ->
                System.out.println("  - " + k + " : " + v.getClass().getSimpleName()));

        System.out.println("\nGlobal Variables:");
        globalVariables.forEach(v -> System.out.println("  - " + v));

        System.out.println("\nLoop Variables:");
        loopVariables.forEach(v -> System.out.println("  - " + v));

        System.out.println("\nFunctions:");
        functions.forEach(f -> System.out.println("  - " + f + "()"));

        System.out.println("\nFilters:");
        filters.forEach(f -> System.out.println("  - " + f));

        System.out.println("\nAttributes:");
        attributes.forEach(a -> System.out.println("  - " + a));

        System.out.println("\nIncludes:");
        System.out.println(includes.isEmpty() ? "  (none)" : includes);

        System.out.println("\nExtends:");
        System.out.println(extendsTemplate == null ? "  (none)" : extendsTemplate);

        System.out.println("\n=======================================================\n");
    }
}

