/**
 * @Author: turk
 * @Description: Sintaksni analizator.
 */

package compiler.parser;

import static compiler.lexer.TokenType.*;
import static common.RequireNonNull.requireNonNull;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import common.Report;
import compiler.lexer.Position;
import compiler.lexer.Symbol;
import compiler.lexer.TokenType;
import compiler.parser.ast.*;
import compiler.parser.ast.def.*;
import compiler.parser.ast.type.*;
import compiler.parser.ast.expr.*;
import compiler.parser.ast.expr.Binary.Operator;
import compiler.parser.ast.def.FunDef.Parameter;

public class Parser {
    /**
     * Seznam leksikalnih simbolov.
     */
    private final List<Symbol> symbols;

    /**
     * Ciljni tok, kamor izpisujemo produkcije. Če produkcij ne želimo izpisovati,
     * vrednost opcijske spremenljivke nastavimo na Optional.empty().
     */
    private final Optional<PrintStream> productionsOutputStream;
    private int index;

    public Parser(List<Symbol> symbols, Optional<PrintStream> productionsOutputStream) {
        requireNonNull(symbols, productionsOutputStream);
        this.symbols = symbols;
        this.productionsOutputStream = productionsOutputStream;
        this.index = 0;
    }

    /**
     * Izpiše produkcijo na izhodni tok.
     */
    private void dump(String production) {
        if (productionsOutputStream.isPresent()) {
            productionsOutputStream.get().println(production);
        }
    }

    private boolean check(TokenType token) {
        return symbols.get(index).tokenType == token;
    }

    private Symbol skip() {
        var res = symbols.get(index);
        index++;
        return res;
    }

    private void errorExpected(String expected) {
        if (symbols.get(0).lexeme.equals("$")) {
            Report.error(symbols.get(index).position, "expected " + expected + ", got EOF");
        }
        Report.error(symbols.get(index).position, "expected " + expected + ", got " + symbols.get(index).lexeme);
    }

    /**
     * Izvedi sintaksno analizo.
     */
    public Ast parse() {
        return parseSource();
    }

    private Ast parseSource() {
        dump("source -> definitions");
        return parseDefinitions(new Defs(symbols.get(index).position, new ArrayList<>()));
    }

    /**
     * Implementacija rekurzivnega spuščanja
     */
    private Defs parseDefinitions(Defs defs) {

        dump("definitions -> definition definitions_");
        defs.definitions.add(parseDefinition());
        defs = parseDefinitions_(defs); 

        return new Defs(new Position(defs.position.start, symbols.get(index-1).position.end), defs.definitions);
    }

    private Defs parseDefinitions_(Defs defs) {
        if (check(OP_SEMICOLON)) {
            dump("definitions_ -> ; definitions");
            // skip ';'
            skip();
            return parseDefinitions(defs);
        } else if(check(EOF) || check(OP_RBRACE)) {
            dump( "definitions_ -> .");
            return defs;
        }
        errorExpected("}, ; or EOF");
        return null;
    }

    private Def parseDefinition() {
        if (check(KW_TYP)) {
            dump("definition -> type_definition");
            return parseTypeDef();
        } else if (check(KW_FUN)) {
            dump("definition -> function_definition");
            return parseFunctionDef();
        } else if (check(KW_VAR)) {
            dump("definition -> variable_definition");
            return parseVarDef();
        } else {
            errorExpected("type, function or variable definition");
        }
        return null;
    }

    private TypeDef parseTypeDef() {
        dump("type_definition -> typ identifier : type");
        // skip KW_TYP
        var start = skip().position.start;
        // skip IDENTIFIER
        if (!check(IDENTIFIER)) {
            errorExpected("identifier");
        }
        var id = skip();
        // skip ':'
        if (!check(OP_COLON)) {
            errorExpected(":");
        }
        skip();

        var type = parseType();
        return new TypeDef(new Position(start, type.position.end), id.lexeme, type);
    }

    private Type parseType() {
        if (check(IDENTIFIER)) {
            dump("type -> identifier");
            // skip identifier
            var id = skip();
            return new TypeName(id.position, id.lexeme);
        } else if (check(AT_LOGICAL)) {
            dump("type -> logical");
            // skip logical
            return Atom.LOG(skip().position);
        } else if (check(AT_INTEGER)) {
            dump("type -> integer");
            // skip integer
            return Atom.INT(skip().position);
        } else if (check(AT_STRING)) {
            dump("type -> string");
            // skip string
            return Atom.STR(skip().position);
        } else if (check(KW_ARR)) {
            dump("type -> arr [ int_const ] type");
            // skip 'arr'
            var start = skip().position.start;
            // skip '['
            if (!check(OP_LBRACKET)) {
                errorExpected("[");
            }
            skip();
            // skip 'int_const'
            if (!check(C_INTEGER)) {
                errorExpected("int constant");
            }
            var size = skip().lexeme;
            // skip ']'
            if (!check(OP_RBRACKET)) {
                errorExpected("]");
            }
            skip();

            // parse type
            var type = parseType();
            return new Array(new Position(start, type.position.end), Integer.parseInt(size), type);
        }
        errorExpected("identifier, logical, integer, string or array");
        return null;
    }

    private FunDef parseFunctionDef() {
        dump("function_definition -> fun identifier ( parameters ) : type = expression");
        // skip fun
        var start = skip().position.start;
        // skip identifier
        if (!check(IDENTIFIER)) {
            errorExpected("identifier");
        }
        var name = skip().lexeme;
        // skip (
        if (!check(OP_LPARENT)) {
            errorExpected("(");
        }
        skip();
        var params = parseParameters(new ArrayList<>());
        // skip )
        if (!check(OP_RPARENT)) {
            errorExpected(")");
        }
        skip();
        // skip :
        if (!check(OP_COLON)) {
            errorExpected(":");
        }
        skip();
        var type = parseType();
        // skip =
        if (!check(OP_ASSIGN)) {
            errorExpected("=");
        }
        skip();
        var expr = parseExpression();
        return new FunDef(new Position(start, expr.position.end), name, params, type, expr);
    }

    private List<Parameter> parseParameters(List<Parameter> params) {
        dump("parameters -> parameter parameters_");
        params.add(parseParameter());
        params = parseParameters_(params);
        return params;
    }

    private List<Parameter> parseParameters_(List<Parameter> params) {
        if (check(OP_COMMA)) {
            dump("parameters_ -> , parameters");
            // skip ,
            skip();
            return parseParameters(params);
        } else {
            dump("parameters_ -> .");
            return params;
        }
    }

    private Parameter parseParameter() {
        dump("parameter -> identifier : type");
        // skip identifier
        if (!check(IDENTIFIER)) {
            errorExpected("identifier");
        }
        var name = skip();
        // skip :
        if (!check(OP_COLON)) {
            errorExpected(":");
        }
        skip();
        var type = parseType();
        return new Parameter(new Position(name.position.start, type.position.end), name.lexeme, type);
    }

    private Expr parseExpression() {
        dump("expression -> logical_ior_expression expression_");
        return parseExpression_(parseLogicalIorExpression());
    }

    private Expr parseExpression_(Expr expr) {
        if (check(OP_LBRACE)) {
            dump("expression_ -> { WHERE definitions }");
            // skip {
            skip();
            // skip where
            if (!check(KW_WHERE)) {
                errorExpected("where");
            }
            skip();
            var defs = parseDefinitions(new Defs(symbols.get(index).position, new ArrayList<>()));
            // skip }
            if (!check(OP_RBRACE)) {
                errorExpected("}");
            }
            return new Where(new Position(expr.position.start, skip().position.end), expr, defs);
        } else {
            dump("expression_ -> .");
            return expr;
        }
    }

    private Expr parseLogicalIorExpression() {
        dump("logical_ior_expression -> logical_and_expression logical_ior_expression_");
        return parseLogicalIorExpression_(parseLogicalAndExpression());
    }

    private Expr parseLogicalIorExpression_(Expr left){
        if (check(OP_OR)) {
            dump("logical_ior_expression_ -> | logical_ior_expression");
            // skip |
            skip();
            var right = parseLogicalAndExpression();
            return parseLogicalIorExpression_(new Binary(new Position(left.position.start, right.position.end), left, Binary.Operator.OR, right));
        } else {
            dump("logical_ior_expression_ -> .");
            return left;
        }
    }

    private Expr parseLogicalAndExpression() {
        dump("logical_and_expression -> compare_expression logical_and_expression_");
        return parseLogicalAndExpression_(parseCompareExpression());
    }

    private Expr parseLogicalAndExpression_(Expr left) {
        if (check(OP_AND)) {
            dump("logical_and_expression_ -> & logical_and_expression");
            // skip &
            skip();
            var right = parseCompareExpression();
            return parseLogicalAndExpression_(new Binary(new Position(left.position.start, right.position.end), left, Binary.Operator.AND, right));
        } else {
            dump("logical_and_expression_ -> .");
            return left;
        }
    }

    private Expr parseCompareExpression() {
        dump("compare_expression -> additive_expression compare_expression_");
        var left = parseAdditiveExpression();
        var right = parseCompareExpression_(left);
        if (check(OP_EQ) || check(OP_NEQ) || check(OP_GEQ) || check(OP_LEQ) || check(OP_LT) || check(OP_GT))
            errorExpected("end of something");
        return right;
    }

    private Expr parseCompareExpression_(Expr left) {
        if (check(OP_EQ)) {
            dump("compare_expression_ -> == additive_expression");
            // skip ==
            skip();
            var right = parseAdditiveExpression();
            return new Binary(new Position(left.position.start, right.position.end), left, Binary.Operator.EQ, right);
        } else if (check(OP_NEQ)) {
            dump("compare_expression_ -> != additive_expression");
            // skip !=
            skip();
            var right = parseAdditiveExpression();
            return new Binary(new Position(left.position.start, right.position.end), left, Binary.Operator.NEQ, right);
        } else if (check(OP_LEQ)) {
            dump("compare_expression_ -> <= additive_expression");
            // skip <=
            skip();
            var right = parseAdditiveExpression();
            return new Binary(new Position(left.position.start, right.position.end), left, Binary.Operator.LEQ, right);
        } else if (check(OP_GEQ)) {
            dump("compare_expression_ -> >= additive_expression");
            // skip >=
            skip();
            var right = parseAdditiveExpression();
            return new Binary(new Position(left.position.start, right.position.end), left, Binary.Operator.GEQ, right);
        } else if (check(OP_LT)) {
            dump("compare_expression_ -> < additive_expression");
            // skip <
            skip();
            var right = parseAdditiveExpression();
            return new Binary(new Position(left.position.start, right.position.end), left, Binary.Operator.LT, right);
        } else if (check(OP_GT)) {
            dump("compare_expression_ -> > additive_expression");
            // skip >
            skip();
            var right = parseAdditiveExpression();
            return new Binary(new Position(left.position.start, right.position.end), left, Binary.Operator.GT, right);
        } else {
            dump("compare_expression_ -> .");
            return left;
        }
    }

    private Expr parseAdditiveExpression() {
        dump("additive_expression -> multiplicative_expression additive_expression_");
        return parseAdditiveExpression_(parseMultiplicativeExpression());
    }

    private Expr parseAdditiveExpression_(Expr left) {
        if (check(OP_ADD)) {
            dump("additive_expression_ -> + additive_expression");
            // skip +
            skip();
            var right = parseMultiplicativeExpression();
            return parseAdditiveExpression_(new Binary(new Position(left.position.start, right.position.end), left, Binary.Operator.ADD, right));
        } else if (check(OP_SUB)) {
            dump("additive_expression_ -> - additive_expression");
            // skip -
            skip();
            var right = parseMultiplicativeExpression();
            return parseAdditiveExpression_(new Binary(new Position(left.position.start, right.position.end), left, Binary.Operator.SUB, right));
        } else {
            dump("additive_expression_ -> .");
            return left;
        }
    }

    private Expr parseMultiplicativeExpression() {
        dump("multiplicative_expression -> prefix_expression multiplicative_expression_");
        return parseMultiplicativeExpression_(parsePrefixExpression());
    }

    private Expr parseMultiplicativeExpression_(Expr left) {
        if (check(OP_MUL)) {
            dump("multiplicative_expression_ -> * multiplicative_expression");
            // skip *
            skip();
            var right = parsePrefixExpression();
            return parseMultiplicativeExpression_(new Binary(new Position(left.position.start, right.position.end), left, Binary.Operator.MUL, right));
        } else if (check(OP_DIV)) {
            dump("multiplicative_expression_ -> / multiplicative_expression");
            // skip /
            skip();
            var right = parsePrefixExpression();
            return parseMultiplicativeExpression_(new Binary(new Position(left.position.start, right.position.end), left, Binary.Operator.DIV, right));
        } else if (check(OP_MOD)) {
            dump("multiplicative_expression_ -> % multiplicative_expression");
            // skip %
            skip();
            var right = parsePrefixExpression();
            return parseMultiplicativeExpression_(new Binary(new Position(left.position.start, right.position.end), left, Binary.Operator.MOD, right));
        } else {
            dump("multiplicative_expression_ -> .");
            return left; 
        }
    }

    private Expr parsePrefixExpression() {
        if (check(OP_ADD)) {
            dump("prefix_expression -> + prefix_expression");
            // skip +
            var start = skip().position.start;
            var right = parsePrefixExpression();
            return new Unary(new Position(start, right.position.end), right, Unary.Operator.ADD);
        } else if (check(OP_SUB)) {
            dump("prefix_expression -> - prefix_expression");
            // skip -
            var start = skip().position.start;
            var right = parsePrefixExpression();
            return new Unary(new Position(start, right.position.end), right, Unary.Operator.SUB);
        } else if (check(OP_NOT)) {
            dump("prefix_expression -> ! prefix_expression");
            // skip !
            var start = skip().position.start;
            var right = parsePrefixExpression();
            return new Unary(new Position(start, right.position.end), right, Unary.Operator.NOT);
        } else {
            dump("prefix_expression -> postfix_expression");
            return parsePostfixExpression();
        }
    }

    private Expr parsePostfixExpression() {
        dump("postfix_expression -> atom_expression postfix_expression_");
        return parsePostfixExpression_(parseAtomExpression());
    }

    private Expr parsePostfixExpression_(Expr left) {
        if (check(OP_LBRACKET)) {
            dump("postfix_expression -> [ expression ] postfix_expression_");
            // skip [
            skip();
            var right = parseExpression();
            // skip ]
            if (!check(OP_RBRACKET)) {
                errorExpected("]");
            }
            return parsePostfixExpression_(new Binary(new Position(left.position.start, skip().position.end), left, Operator.ARR, right));
        } else {
            dump("postfix_expression_ -> .");
            return left;
        }
    }

    private Expr parseAtomExpression() {
        if (check(C_LOGICAL)) {
            dump("atom_expression -> log_constant");
            // skip log_constant
            var log = skip();
            return new Literal(log.position, log.lexeme, Atom.Type.LOG);
        } else if (check(C_INTEGER)) {
            dump("atom_expression -> int_constant");
            // skip int_constant
            var inte = skip();
            return new Literal(inte.position, inte.lexeme, Atom.Type.INT);
        } else if (check(C_STRING)) {
            dump("atom_expression -> str_constant");
            // skip str_constant
            var str = skip();
            return new Literal(str.position, str.lexeme, Atom.Type.STR);
        } else if (check(IDENTIFIER)) {
            dump("atom_expression -> identifier atom_expression_");
            // skip identifier
            var name = skip();
            var atom = parseAtomExpression_();
            if (atom == null) return new Name(name.position, name.lexeme);
            return new Call(new Position(name.position.start, atom.position.end), atom.expressions, name.lexeme);
        } else if (check(OP_LBRACE)) {
            dump("atom_expression -> { atom_expression__");
            return parseAtomExpression__();
        } else if (check(OP_LPARENT)) {
            dump("atom_expression -> ( expressions )");
            // skip (
            var start = skip().position.start;
            var expr = parseExpressions(new Block(null, new ArrayList<>()));
            // skip )
            if (!check(OP_RPARENT)) {
                errorExpected(")");
            }
            return new Block(new Position(start, skip().position.end), expr.expressions);
        } else {
            errorExpected("logical constant, integer constant, string constant, { or (");
            return null;
        }
    }

    private Block parseAtomExpression_() {
        if (check(OP_LPARENT)) {
            dump("atom_expression_ -> ( expressions )");
            // skip (
            var start = skip().position.start;
            var exprs = parseExpressions(new Block(null, new ArrayList<>()));
            // skip )
            if (!check(OP_RPARENT)) {
                errorExpected(")");
            }
            var end = skip().position.end;
            return new Block(new Position(start, end), exprs.expressions);
        } else {
            dump("atom_expression_ -> .");
            return null;
        }
    }

    private Expr parseAtomExpression__() {
        // skip {
        var start = skip().position.start;
        if (check(KW_WHILE)) {
            dump("atom_expression__ -> while expression : expression }");
            // skip while
            skip();
            var condition = parseExpression();
            // skip :
            if (!check(OP_COLON)) {
                errorExpected(":");
            }
            skip();
            var body = parseExpression();
            // skip }
            if (!check(OP_RBRACE)) {
                errorExpected("}");
            }
            return new While(new Position(start, skip().position.end), condition, body);
        } else if (check(KW_FOR)) {
            dump("atom_expression__ -> for identifier = expression , expression , expression : expression }");
            // skip for
            skip();
            // skip identifier
            if (!check(IDENTIFIER)) {
                errorExpected("identifier");
            }
            var counter = skip();
            // skip =
            if (!check(OP_ASSIGN)) {
                errorExpected("=");
            }
            skip();
            var low = parseExpression();
            // skip ,
            if (!check(OP_COMMA)) {
                errorExpected(",");
            }
            skip();
            var high = parseExpression();
            // skip ,
            if (!check(OP_COMMA)) {
                errorExpected(",");
            }
            skip();
            var step = parseExpression();
            // skip :
            if (!check(OP_COLON)) {
                errorExpected(":");
            }
            skip();
            var body = parseExpression();
            // skip }
            if (!check(OP_RBRACE)) {
                errorExpected("}");
            }
            return new For(new Position(start, skip().position.end), new Name(counter.position, counter.lexeme), low, high, step, body);
        } else if (check(KW_IF)) {
            dump("atom_expression__ -> if expression then expression atom_expression___");
            // skip if
            skip();
            var condition = parseExpression();
            // skip then
            if (!check(KW_THEN)) {
                errorExpected("then");
            }
            skip();
            var then = parseExpression();
            return parseAtomExpression___(new IfThenElse(new Position(start, then.position.end), condition, then));
        } else {
            dump("atom_expression__ -> expression = expression }");
            var left = parseExpression();
            // skip =
            if (!check(OP_ASSIGN)) {
                errorExpected("=");
            }
            skip();
            var right = parseExpression();
            // skip }
            if (!check(OP_RBRACE)) {
                errorExpected("}");
            }
            return new Binary(new Position(start, skip().position.end), left, Operator.ASSIGN, right);
        }
    }

    private Expr parseAtomExpression___(IfThenElse expr) {
        if (check(OP_RBRACE)) {
            dump("atom_expression___ -> }");
            // skip }
            return new IfThenElse(new Position(expr.position.start, skip().position.end), expr.condition, expr.thenExpression);
        } else if (check(KW_ELSE)) {
            dump("atom_expression___ -> else expression }");
            // skip else
            skip();
            var els = parseExpression();
            // skip }
            if (!check(OP_RBRACE)) {
                errorExpected("}");
            }
            return new IfThenElse(new Position(expr.position.start, skip().position.end), expr.condition, expr.thenExpression, els);
        } else {
            errorExpected("} or else");
            return null;
        }

    }

    private Block parseExpressions(Block exprs) {
        dump("expressions -> expression expressions_");
        exprs.expressions.add(parseExpression());
        return parseExpressions_(exprs);
    }

    private Block parseExpressions_(Block exprs) {
        if (check(OP_COMMA)) {
            dump("expressions_ -> , expressions");
            // skip ,
            skip();
            return parseExpressions(exprs);
        } else {
            dump("expressions_ -> .");
            return exprs;
        }
    }

    private VarDef parseVarDef() {
        dump("variable_definition -> var identifier : type");
        // skip var
        var start = skip().position.start;
        // skip identifier
        if (!check(IDENTIFIER)) {
            errorExpected("identifier");
        }
        var name = skip().lexeme;
        // skip :
        if (!check(OP_COLON)) {
            errorExpected(":");
        }
        skip();
        // parse type
        var type = parseType();
        return new VarDef(new Position(start, type.position.end), name, type);
    }
}
