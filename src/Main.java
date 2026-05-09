import AST.ASTGraphvizPrinter;
import AST.ASTNode;
import AST.Program;
import AST.template.TemplateVisitor;
import AST.visitor.ProgramVisitor;
import SymbolTable.FlaskSymbolTable;
import antlr.FlaskLexer;
import antlr.FlaskParser;
import antlr.TemplateLexer;
import antlr.TemplateParser;
import org.antlr.v4.runtime.*;
import java.nio.file.*;
import AST.template.TemplateVisitor;
import SymbolTable.JinjaASTVisitor;
import SymbolTable.TemplateSymbolTable;

public class Main {
    public static void main(String[] args) throws Exception {

        Path p = Paths.get("src/Tests/FinalTests/app.py").toAbsolutePath();
        CharStream input = CharStreams.fromPath(p);

                        //Flask Compiler//

//        FlaskLexer lexer = new FlaskLexer(input);
//        CommonTokenStream tokens = new CommonTokenStream(lexer);
//        FlaskParser parser = new FlaskParser(tokens);
//
//        ProgramVisitor visitor = new ProgramVisitor();
//        Program program = visitor.visit(parser.prog());
//        ASTGraphvizPrinter.print(program, "ast.dot");
//
//        FlaskSymbolTable flaskSymbolTable = new FlaskSymbolTable();
//        SymbolTable.SymbolTableVisitor stVisitor = new SymbolTable.SymbolTableVisitor();
//        stVisitor.visitProgram(program);
//        flaskSymbolTable = stVisitor.getSymbolTable();
//        flaskSymbolTable.printSymbolTable();


                         //Template Compiler//

        TemplateLexer lexer = new TemplateLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TemplateParser parser = new TemplateParser(tokens);

        TemplateVisitor visitor = new TemplateVisitor();
        ASTNode root = visitor.visit(parser.template());
        ASTGraphvizPrinter.print(root, "ast.dot");


        TemplateSymbolTable templateSymbolTable = new TemplateSymbolTable();
        JinjaASTVisitor stVisitor = new JinjaASTVisitor(templateSymbolTable);
        stVisitor.visit(root);
        templateSymbolTable.print();


    }
}

