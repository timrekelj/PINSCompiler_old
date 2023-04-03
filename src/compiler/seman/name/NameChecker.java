/**
 * @ Author: turk
 * @ Description: Preverjanje in razreševanje imen.
 */

package compiler.seman.name;

import static common.RequireNonNull.requireNonNull;

import common.Report;
import compiler.common.Visitor;
import compiler.lexer.Position;
import compiler.parser.ast.def.*;
import compiler.parser.ast.def.FunDef.Parameter;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.type.*;
import compiler.seman.common.NodeDescription;
import compiler.seman.name.env.SymbolTable;
import compiler.seman.name.env.SymbolTable.DefinitionAlreadyExistsException;

public class NameChecker implements Visitor {
    /**
     * Opis vozlišč, ki jih povežemo z njihovimi
     * definicijami.
     */
    private NodeDescription<Def> definitions;

    /**
     * Simbolna tabela.
     */
    private SymbolTable symbolTable;

    /**
     * Ustvari nov razreševalnik imen.
     */
    public NameChecker(
        NodeDescription<Def> definitions,
        SymbolTable symbolTable
    ) {
        requireNonNull(definitions, symbolTable);
        this.definitions = definitions;
        this.symbolTable = symbolTable;
    }

    @Override
    public void visit(Call call) {
        if (symbolTable.definitionFor(call.name).isPresent()) {
            if (!symbolTable.definitionFor(call.name).get().getClass().getSimpleName().equals("FunDef"))
                Report.error(call.position, call.name + " is not defined as function");
            definitions.store(symbolTable.definitionFor(call.name).get(), call);
        } else Report.error(call.position, "The function " + call.name + " is not declared.");
    }

    @Override
    public void visit(Binary binary) {
        binary.left.accept(this);
        binary.right.accept(this);
    }

    @Override
    public void visit(Block block) {
        for (Expr expr : block.expressions)
            expr.accept(this);
    }

    @Override
    public void visit(For forLoop) {
        visit(forLoop.counter);
        forLoop.low.accept(this);
        forLoop.high.accept(this);
        forLoop.step.accept(this);
        forLoop.body.accept(this);
    }

    @Override
    public void visit(Name name) {
            if (symbolTable.definitionFor(name.name).isPresent()) {
                if (!symbolTable.definitionFor(name.name).get().getClass().getSimpleName().equals("VarDef") &&
                    !symbolTable.definitionFor(name.name).get().getClass().getSimpleName().equals("Parameter"))
                    Report.error(name.position, name.name + " is not defined as variable or parameter");
                definitions.store(symbolTable.definitionFor(name.name).get(), name);
            } else Report.error(name.position, "The variable " + name.name + " is not declared.");
    }

    @Override
    public void visit(IfThenElse ifThenElse) {
        ifThenElse.condition.accept(this);
        ifThenElse.thenExpression.accept(this);
    }

    @Override
    public void visit(Literal literal) { /* Do nothing. */ }

    @Override
    public void visit(Unary unary) { unary.expr.accept(this); }

    @Override
    public void visit(While whileLoop) {
        whileLoop.condition.accept(this);
        whileLoop.body.accept(this);
    }

    @Override
    public void visit(Where where) {
        symbolTable.inNewScope(() -> {
            visit(where.defs);
            where.expr.accept(this);
        });
    }

    @Override
    public void visit(Defs defs) {
        for (Def def : defs.definitions) {
            try {
                symbolTable.insert(def);
            } catch (DefinitionAlreadyExistsException e) {
                Report.error("Definition already exists");
            }
        }

        for (Def def : defs.definitions) {
            switch (def.getClass().getSimpleName()) {
                case "FunDef" -> visit((FunDef) def);
                case "TypeDef" -> visit((TypeDef) def);
                case "VarDef" -> visit((VarDef) def);
            }
        }
    }

    @Override
    public void visit(FunDef funDef) {
        symbolTable.inNewScope(() -> {
            for (Parameter parameter : funDef.parameters)
                parameter.type.accept(this);
            for (Parameter parameter : funDef.parameters)
                parameter.accept(this);
            funDef.type.accept(this);
            funDef.body.accept(this);
        });
    }

    @Override
    public void visit(TypeDef typeDef) { typeDef.type.accept(this); }

    @Override
    public void visit(VarDef varDef) { varDef.type.accept(this); }

    @Override
    public void visit(Parameter parameter) {
        try {
            symbolTable.insert(parameter);
        } catch (Exception e) {
            Report.error(parameter.position, "Parameter already exists");
        } 
    }

    @Override
    public void visit(Array array) { array.type.accept(this); }

    @Override
    public void visit(Atom atom) { /* Do nothing. */ }

    @Override
    public void visit(TypeName name) {
        if (symbolTable.definitionFor(name.identifier).isPresent()) {
            if (!symbolTable.definitionFor(name.identifier).get().getClass().getSimpleName().equals("TypeDef"))
                Report.error(name.position, name.identifier + " is not defined as type");
            definitions.store(symbolTable.definitionFor(name.identifier).get(), name);
        } else Report.error(name.position, "The type " + name.identifier + " is not declared.");
    }
}
