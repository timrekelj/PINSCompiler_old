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


    /**
     * Števca za statični nivo in skladovni offset
     */
    private int staticLevel;
    private int stackOffset;

    public FrameEvaluator(
        NodeDescription<Frame> frames, 
        NodeDescription<Access> accesses,
        NodeDescription<Def> definitions,
        NodeDescription<Type> types
    ) {
        requireNonNull(frames, accesses, definitions, types);
        this.frames = frames;
        this.accesses = accesses;
        this.definitions = definitions;
        this.types = types;

        this.staticLevel = 1;
        this.stackOffset = 0;
    }

    @Override
    public void visit(Call call) {
        if (staticLevel == 1)
            accesses.store(new Access.Global(types.valueFor(definitions.valueFor(call).get()).get().sizeInBytes(), frames.valueFor(definitions.valueFor(call).get()).get().label), call);
        else
            accesses.store(new Access.Local(types.valueFor(definitions.valueFor(call).get()).get().sizeInBytes(), stackOffset, staticLevel), call);

    }


    @Override
    public void visit(Binary binary) {
        binary.left.accept(this);
        binary.right.accept(this);
    }


    @Override
    public void visit(Block block) {
        for (Expr expr : block.expressions) {
            expr.accept(this);
        }
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
    public void visit(Name name) {
        if (!accesses.valueFor(definitions.valueFor(name).get()).isPresent())
            definitions.valueFor(name).get().accept(this);
        
        accesses.store(accesses.valueFor(definitions.valueFor(name).get()).get(), name);
    }


    @Override
    public void visit(IfThenElse ifThenElse) {
        ifThenElse.condition.accept(this);
        ifThenElse.thenExpression.accept(this);

        if (ifThenElse.elseExpression.isPresent())
            ifThenElse.elseExpression.get().accept(this);
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
        where.expr.accept(this);
        where.defs.accept(this);
    }


    @Override
    public void visit(Defs defs) {
        for (Def def : defs.definitions) {
            def.accept(this);
        }
    }


    @Override
    public void visit(FunDef funDef) {
        compiler.frm.Frame.Builder Builder = null;

        if (staticLevel == 1)
            Builder = new Frame.Builder(Frame.Label.named(funDef.name), staticLevel);
        else
            Builder = new Frame.Builder(Frame.Label.nextAnonymous(), staticLevel);

        for (Parameter param : funDef.parameters) {
            param.accept(this);
            Builder.addParameter(types.valueFor(param).get().sizeInBytesAsParam());
        }

        staticLevel++;
        funDef.body.accept(this);
        staticLevel--;

        frames.store(Builder.build(), funDef);
    }


    @Override
    public void visit(TypeDef typeDef) { /* do nothing */}


    @Override
    public void visit(VarDef varDef) {
        if (staticLevel == 1) {
            accesses.store(new Access.Global(types.valueFor(varDef).get().sizeInBytes(), Frame.Label.named(varDef.name)), varDef);
            return;
        }

        accesses.store(new Access.Local(types.valueFor(varDef).get().sizeInBytes(), stackOffset, staticLevel), varDef);
        stackOffset += types.valueFor(varDef).get().sizeInBytes();
    }


    @Override
    public void visit(Parameter parameter) {
        accesses.store(new Access.Parameter(types.valueFor(parameter).get().sizeInBytesAsParam(), stackOffset, staticLevel), parameter);
        stackOffset += types.valueFor(parameter).get().sizeInBytesAsParam();
    }


    @Override
    public void visit(Array array) { /* do nothing */}


    @Override
    public void visit(Atom atom) { /* do nothing */}


    @Override
    public void visit(TypeName name) { /* do nothing */}
}
