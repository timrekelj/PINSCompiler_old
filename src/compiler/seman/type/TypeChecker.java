/**
 * @ Author: turk
 * @ Description: Preverjanje tipov.
 */

package compiler.seman.type;

import static common.RequireNonNull.requireNonNull;

import common.Report;
import compiler.common.Visitor;
import compiler.parser.ast.def.*;
import compiler.parser.ast.def.FunDef.Parameter;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.type.*;
import compiler.seman.common.NodeDescription;
import compiler.seman.type.type.Type;
import compiler.seman.type.type.Type.Atom.Kind;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TypeChecker implements Visitor {
    /**
     * Opis vozlišč in njihovih definicij.
     */
    private final NodeDescription<Def> definitions;

    /**
     * Opis vozlišč, ki jim priredimo podatkovne tipe.
     */
    private NodeDescription<Type> types;
    private Set<Def> visited = new HashSet<Def>();

    public TypeChecker(NodeDescription<Def> definitions, NodeDescription<Type> types) {
        requireNonNull(definitions, types);
        this.definitions = definitions;
        this.types = types;
    }

    @Override
    public void visit(Call call) {
        if (call.arguments.size() != ((FunDef) definitions.valueFor(call).get()).parameters.size()) {
            Report.error(call.position, "Number of arguments is not the same as the number of parameters.");
        }

        // check called function if it is not already checked
        if (!types.valueFor(definitions.valueFor(call).get()).isPresent())
            definitions.valueFor(call).get().accept(this);

        for (int i = 0; i < call.arguments.size(); i++) {
            call.arguments.get(i).accept(this);
            if (!types.valueFor(call.arguments.get(i)).get().equals(((Type.Function) types.valueFor(definitions.valueFor(call).get()).get()).parameters.get(i)))
                Report.error(call.arguments.get(i).position, i + ". argument is not of the same type as the function parameter.");
        }

        types.store(types.valueFor(((FunDef)definitions.valueFor(call).get()).type).get(), call);
    }

    @Override
    public void visit(Binary binary) {
        binary.left.accept(this);
        binary.right.accept(this);

        switch (binary.operator) {
            case ADD, SUB, MUL, DIV, MOD:
                if (types.valueFor(binary.left).get().equals(new Type.Atom(Kind.INT)) &&
                    types.valueFor(binary.right).get().equals(new Type.Atom(Kind.INT)))
                    types.store(new Type.Atom(Kind.INT) , binary);
                else Report.error(binary.position, "Arithmetic operator used on non-integer types.");
                break;
            case AND, OR:
                if (types.valueFor(binary.left).get().equals(new Type.Atom(Kind.LOG)) &&
                    types.valueFor(binary.right).get().equals(new Type.Atom(Kind.LOG)))
                    types.store(new Type.Atom(Kind.LOG) , binary);
                else Report.error(binary.position, "Logical operator used on non-logical types.");
                break;
            case EQ, NEQ, GEQ, LEQ, GT, LT:
                if (types.valueFor(binary.left).get().equals(types.valueFor(binary.right).get()) && (
                    types.valueFor(binary.left).get().equals(new Type.Atom(Kind.INT)) ||
                    types.valueFor(binary.left).get().equals(new Type.Atom(Kind.LOG))))
                    types.store(new Type.Atom(Kind.LOG) , binary);
                else Report.error(binary.position, "Comparison of different types.");
                break;
            case ASSIGN:
                if (types.valueFor(binary.left).get().equals(types.valueFor(binary.right).get()))
                    types.store(types.valueFor(binary.left).get(), binary);
                else Report.error(binary.position, "Assignment of different types.");
                break;
            case ARR:
                if (types.valueFor(binary.right).get().equals(new Type.Atom(Kind.INT)) &&
                    types.valueFor(binary.left).get().isArray())
                    types.store(types.valueFor(binary.left).get().asArray().get().type, binary);
                else Report.error(binary.position, "Array is not called right.");
                break;
            default:
                Report.error("u dumb.");
                break;
        }
    }

    @Override
    public void visit(Block block) {
        block.expressions.forEach(expr -> expr.accept(this));
        types.store(types.valueFor(block.expressions.get(block.expressions.size() - 1)).get(), block);
    }

    @Override
    public void visit(For forLoop) {
        forLoop.counter.accept(this);
        if (!types.valueFor(forLoop.counter).get().equals(new Type.Atom(Kind.INT)))
            Report.error(forLoop.counter.position, "Counter is not integer.");

        forLoop.low.accept(this);
        if (!types.valueFor(forLoop.low).get().equals(new Type.Atom(Kind.INT)))
            Report.error(forLoop.low.position, "Low is not integer.");

        forLoop.high.accept(this);
        if (!types.valueFor(forLoop.high).get().equals(new Type.Atom(Kind.INT)))
            Report.error(forLoop.high.position, "High is not integer.");

        forLoop.step.accept(this);
        if (!types.valueFor(forLoop.step).get().equals(new Type.Atom(Kind.INT)))
            Report.error(forLoop.step.position, "Step is not integer.");

        forLoop.body.accept(this);
        types.store(new Type.Atom(Kind.VOID), forLoop);
    }

    @Override
    public void visit(Name name) {
        if (!types.valueFor(definitions.valueFor(name).get()).isPresent())
            definitions.valueFor(name).get().accept(this);
        types.store(types.valueFor(definitions.valueFor(name).get()).get(), name);
    }

    @Override
    public void visit(IfThenElse ifThenElse) {
        ifThenElse.condition.accept(this);
        if (!types.valueFor(ifThenElse.condition).get().equals(new Type.Atom(Kind.LOG)))
            Report.error(ifThenElse.position, "Condition is not logical.");
        ifThenElse.thenExpression.accept(this);
        ifThenElse.elseExpression.ifPresent((expr) -> expr.accept(this));
        types.store(new Type.Atom(Kind.VOID), ifThenElse);
    }

    @Override
    public void visit(Literal literal) {
        switch (literal.type) {
            case INT -> types.store(new Type.Atom(Kind.INT), literal);
            case LOG -> types.store(new Type.Atom(Kind.LOG), literal);
            case STR -> types.store(new Type.Atom(Kind.STR), literal);
        }
    }

    @Override
    public void visit(Unary unary) {
        unary.expr.accept(this);
        if (types.valueFor(unary.expr).get().equals(new Type.Atom(Kind.INT)) && (unary.operator.equals(Unary.Operator.SUB) || unary.operator.equals(Unary.Operator.ADD)))
            types.store(new Type.Atom(Kind.INT), unary);
        else if (types.valueFor(unary.expr).get().equals(new Type.Atom(Kind.LOG)) && unary.operator == Unary.Operator.NOT)
            types.store(new Type.Atom(Kind.LOG), unary);
        else
            Report.error(unary.position, "Expression is wrong.");
    }

    @Override
    public void visit(While whileLoop) {
        whileLoop.condition.accept(this);
        if (!types.valueFor(whileLoop.condition).get().equals(new Type.Atom(Kind.LOG)))
            Report.error(whileLoop.position, "Condition is not logical.");
        types.store(new Type.Atom(Kind.VOID), whileLoop);
        whileLoop.body.accept(this);
    }

    @Override
    public void visit(Where where) {
        where.defs.accept(this);
        where.expr.accept(this);
        types.store(types.valueFor(where.expr).get(), where);
    }

    @Override
    public void visit(Defs defs) {
        for (Def def : defs.definitions) {
            def.accept(this);
        }
    }

    @Override
    public void visit(FunDef funDef) {
        funDef.type.accept(this);

        List<Type> params = new ArrayList<Type>(); 
        funDef.parameters.forEach(parameter -> parameter.accept(this));
        funDef.parameters.forEach(parameter -> params.add(types.valueFor(parameter.type).get()));
        types.store(new Type.Function(params, types.valueFor(funDef.type).get()), funDef);
        funDef.body.accept(this);
        if (!types.valueFor(funDef.type).get().equals(types.valueFor(funDef.body).get()))
            Report.error(funDef.position, "Function body does not match return type.");
    }

    @Override
    public void visit(TypeDef typeDef) {
        if (visited.contains(typeDef))
            Report.error(typeDef.position, "Cycle detected in type definition.");
        visited.add(typeDef);

        typeDef.type.accept(this);
        types.store(types.valueFor(typeDef.type).get(), typeDef);

        visited.remove(typeDef);

    }

    @Override
    public void visit(VarDef varDef) {
        varDef.type.accept(this);
        types.store(types.valueFor(varDef.type).get(), varDef);
    }

    @Override
    public void visit(Parameter parameter) {
        parameter.type.accept(this);
        types.store(types.valueFor(parameter.type).get(), parameter);
    }

    @Override
    public void visit(Array array) {
        array.type.accept(this);
        types.store(new Type.Array(array.size, types.valueFor(array.type).get()), array);
    }

    @Override
    public void visit(Atom atom) {
        switch (atom.type) {
            case INT -> types.store(new Type.Atom(Kind.INT), atom);
            case LOG -> types.store(new Type.Atom(Kind.LOG), atom);
            case STR -> types.store(new Type.Atom(Kind.STR), atom);
        }
    }

    @Override
    public void visit(TypeName name) {
        if (!types.valueFor(definitions.valueFor(name).get()).isPresent())
            definitions.valueFor(name).get().accept(this);
        types.store(types.valueFor(definitions.valueFor(name).get()).get(), name);
    }
}
