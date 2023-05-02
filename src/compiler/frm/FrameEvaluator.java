/**
 * @ Author: turk
 * @ Description: Analizator klicnih zapisov.
 */

package compiler.frm;

import static common.RequireNonNull.requireNonNull;

import compiler.common.Visitor;
import compiler.parser.ast.def.*;
import compiler.parser.ast.def.FunDef.Parameter;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.type.Array;
import compiler.parser.ast.type.Atom;
import compiler.parser.ast.type.TypeName;
import compiler.seman.common.NodeDescription;
import compiler.seman.type.type.Type;

import compiler.frm.Frame.Builder;
import compiler.frm.Frame.Label;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import common.Constants;

public class FrameEvaluator implements Visitor {
    /**
     * Opis definicij funkcij in njihovih klicnih zapisov.
     */
    private NodeDescription<Frame> frames;

    /**
     * Opis definicij spremenljivk in njihovih dostopov.
     */
    private NodeDescription<Access> accesses;

    /**
     * Opis vozlišč in njihovih definicij.
     */
    private final NodeDescription<Def> definitions;

    /**
     * Opis vozlišč in njihovih podatkovnih tipov.
     */
    private final NodeDescription<Type> types;

    private int staticLevel = 1;
    private Stack<Frame.Builder> stack = new Stack<Frame.Builder>();

 
    public FrameEvaluator(
            NodeDescription<Frame> frames,
            NodeDescription<Access> accesses,
            NodeDescription<Def> definitions,
            NodeDescription<Type> types) {
        requireNonNull(frames, accesses, definitions, types);
        this.frames = frames;
        this.accesses = accesses;
        this.definitions = definitions;
        this.types = types;
    }

    @Override
    public void visit(Call call) {
        Frame.Builder f = stack.pop();
        int size = Constants.WordSize;
        for (Expr arg : call.arguments) {
            size += types.valueFor(arg).get().sizeInBytesAsParam();
        }
        f.addFunctionCall(size);
        stack.push(f);
    }

    @Override
    public void visit(Binary binary) {
        binary.left.accept(this);
        binary.right.accept(this);
    }

    @Override
    public void visit(Block block) {
        block.expressions.forEach(expr -> {
            expr.accept(this);
        });
    }

    @Override
    public void visit(For forLoop) {
        forLoop.counter.accept(this);
        forLoop.low.accept(this);
        forLoop.high.accept(this);
        forLoop.step.accept(this);
        forLoop.body.accept(this);
    }

    @Override
    public void visit(Name name) { /* do nothing */}

    @Override
    public void visit(IfThenElse ifThenElse) {
        ifThenElse.condition.accept(this);
        ifThenElse.thenExpression.accept(this);
        ifThenElse.elseExpression.ifPresent((expr) -> expr.accept(this));
    }

    @Override
    public void visit(Literal literal) { /* do nothing */}

    @Override
    public void visit(Unary unary) {
        unary.expr.accept(this);
    }

    @Override
    public void visit(While whileLoop) {
        whileLoop.condition.accept(this);
        whileLoop.body.accept(this);
    }

    @Override
    public void visit(Where where) {
        where.defs.accept(this);
        where.expr.accept(this);
    }

    @Override
    public void visit(Defs defs) {
        defs.definitions.forEach(def -> {
            def.accept(this);
        });
    }

    @Override
    public void visit(FunDef funDef) {
        Frame.Builder f;

        if (stack.empty()) {
            f = new Frame.Builder(Frame.Label.named(funDef.name), staticLevel);
            f.addParameter(Constants.WordSize);
            stack.push(f);

            funDef.parameters.forEach((param) -> param.accept(this));

            funDef.body.accept(this);

            f = stack.pop();
            frames.store(f.build(), funDef);
        } else {
            f = new Frame.Builder(Label.nextAnonymous(), ++staticLevel);
            f.addParameter(Constants.WordSize);
            stack.push(f);

            funDef.parameters.forEach((param) -> param.accept(this));

            funDef.body.accept(this);

            f = stack.pop();
            frames.store(f.build(), funDef);
            staticLevel--;
        }
    }

    @Override
    public void visit(TypeDef typeDef) { /* do nothing */}

    @Override
    public void visit(VarDef varDef) {
        if (stack.empty()) {
            accesses.store(new Access.Global(
                types.valueFor(varDef).get().sizeInBytes(),
                Frame.Label.named(varDef.name)
            ),varDef);
        } else {
            Frame.Builder f = stack.pop();
            int size = types.valueFor(varDef).get().sizeInBytes();
            accesses.store(new Access.Local(size, f.addLocalVariable(size), staticLevel), varDef);
            stack.push(f);
        }
    }

    @Override
    public void visit(Parameter parameter) {
        Frame.Builder f = stack.pop();
        int size = types.valueFor(parameter).get().sizeInBytesAsParam();
        accesses.store(new Access.Parameter(size, f.addParameter(size), staticLevel), parameter);
        stack.push(f);
    }

    @Override
    public void visit(Array array) { /* do nothing */ }

    @Override
    public void visit(Atom atom) { /* do nothing */ }

    @Override
    public void visit(TypeName name) { /* do nothing */ }
}