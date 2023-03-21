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

import javax.crypto.spec.DESKeySpec;
import javax.swing.text.html.HTMLDocument.HTMLReader.ParagraphAction;

import common.Report;
import compiler.lexer.Position;
import compiler.lexer.Symbol;
import compiler.lexer.TokenType;
import compiler.lexer.Position.Location;
import compiler.parser.ast.*;
import compiler.parser.ast.def.*;
import compiler.parser.ast.type.*;
import compiler.parser.ast.expr.*;
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
        if (symbols.get(0).lexeme == "$") {
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
        return parseDefinitions(new Defs(symbols.get(index).position, new ArrayList<Def>()));
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
        } else {
            dump("definitions_ -> .");
        }
        return defs;
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
        var params = parseParameters(new ArrayList<Parameter>());
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
        parseType();
        // skip =
        if (!check(OP_ASSIGN)) {
            errorExpected("=");
        }
        skip();
        parseExpression();
        return new FunDef(new Position(start, null), name, null, null, null);
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
        var ident = skip();
        // skip :
        if (!check(OP_COLON)) {
            errorExpected(":");
        }
        skip();
        var type = parseType();
        return new Parameter(new Position(ident.position.start, type.position.end), ident.lexeme, type);
    }

    private Expr parseExpression(Expr expr) {
        dump("expression -> logical_ior_expression expression_");
        var ior = parseLogicalIorExpression();
        var expr_ = parseExpression_(expr);
    }

    private Expr parseExpression_(Expr expr) {
        if (check(OP_LBRACE)) {
            dump("expression_ -> { WHERE definitions }");
            // skip {
            var start = skip().position.start;
            // skip where
            if (!check(KW_WHERE)) {
                errorExpected("where");
            }
            skip();
            var defs = parseDefinitions(new Defs(symbols.get(index).position, new ArrayList<Def>()));
            // skip }
            if (!check(OP_RBRACE)) {
                errorExpected("}");
            }
            return new Where(new Position(start, skip().position.end), expr, defs);
        } else {
            dump("expression_ -> .");
            return expr;
        }
    }

    private void parseLogicalIorExpression() {
        dump("logical_ior_expression -> logical_and_expression logical_ior_expression_");
        parseLogicalAndExpression();
        parseLogicalIorExpression_();
    }

    private void parseLogicalIorExpression_(){
        if (check(OP_OR)) {
            dump("logical_ior_expression_ -> | logical_ior_expression");
            // skip |
            skip();
            parseLogicalIorExpression();
        } else {
            dump("logical_ior_expression_ -> .");
        }
    }

    private void parseLogicalAndExpression() {
        dump("logical_and_expression -> compare_expression logical_and_expression_");
        parseCompareExpression();
        parseLogicalAndExpression_();
    }

    private void parseLogicalAndExpression_() {
        if (check(OP_AND)) {
            dump("logical_and_expression_ -> & logical_and_expression");
            // skip &
            skip();
            parseLogicalAndExpression();
        } else {
            dump("logical_and_expression_ -> .");
        }
    }

    private void parseCompareExpression() {
        dump("compare_expression -> additive_expression compare_expression_");
        parseAdditiveExpression();
        parseCompareExpression_();
    }

    private void parseCompareExpression_() {
        if (check(OP_EQ)) {
            dump("compare_expression_ -> == additive_expression");
            // skip ==
            skip();
            parseAdditiveExpression();
        } else if (check(OP_NEQ)) {
            dump("compare_expression_ -> != additive_expression");
            // skip !=
            skip();
            parseAdditiveExpression();
        } else if (check(OP_LEQ)) {
            dump("compare_expression_ -> <= additive_expression");
            // skip <=
            skip();
            parseAdditiveExpression();
        } else if (check(OP_GEQ)) {
            dump("compare_expression_ -> >= additive_expression");
            // skip >=
            skip();
            parseAdditiveExpression();
        } else if (check(OP_LT)) {
            dump("compare_expression_ -> < additive_expression");
            // skip <
            skip();
            parseAdditiveExpression();
        } else if (check(OP_GT)) {
            dump("compare_expression_ -> > additive_expression");
            // skip >
            skip();
            parseAdditiveExpression();
        } else {
            dump("compare_expression_ -> .");
        }
    }

    private void parseAdditiveExpression() {
        dump("additive_expression -> multiplicative_expression additive_expression_");
        parseMultiplicativeExpression();
        parseAdditiveExpression_();
    }

    private void parseAdditiveExpression_() {
        if (check(OP_ADD)) {
            dump("additive_expression_ -> + additive_expression");
            // skip +
            skip();
            parseAdditiveExpression();
        } else if (check(OP_SUB)) {
            dump("additive_expression_ -> - additive_expression");
            // skip -
            skip();
            parseAdditiveExpression();
        } else {
            dump("additive_expression_ -> .");
        }
    }

    private void parseMultiplicativeExpression() {
        dump("multiplicative_expression -> prefix_expression multiplicative_expression_");
        parsePrefixExpression();
        parseMultiplicativeExpression_();
    }

    private void parseMultiplicativeExpression_() {
        if (check(OP_MUL)) {
            dump("multiplicative_expression_ -> * multiplicative_expression");
            // skip *
            skip();
            parseMultiplicativeExpression();
        } else if (check(OP_DIV)) {
            dump("multiplicative_expression_ -> / multiplicative_expression");
            // skip /
            skip();
            parseMultiplicativeExpression();
        } else if (check(OP_MOD)) {
            dump("multiplicative_expression_ -> % multiplicative_expression");
            // skip %
            skip();
            parseMultiplicativeExpression();
        } else {
            dump("multiplicative_expression_ -> .");
        }
    }

    private void parsePrefixExpression() {
        if (check(OP_ADD)) {
            dump("prefix_expression -> + prefix_expression");
            // skip +
            skip();
            parsePrefixExpression();
        } else if (check(OP_SUB)) {
            dump("prefix_expression -> - prefix_expression");
            // skip -
            skip();
            parsePrefixExpression();
        } else if (check(OP_NOT)) {
            dump("prefix_expression -> ! prefix_expression");
            // skip !
            skip();
            parsePrefixExpression();
        } else {
            dump("prefix_expression -> postfix_expression");
            parsePostfixExpression();
        }
    }

    private void parsePostfixExpression() {
        dump("postfix_expression -> atom_expression postfix_expression_");
        parseAtomExpression();
        parsePostfixExpression_();
    }

    private void parsePostfixExpression_() {
        if (check(OP_LBRACKET)) {
            dump("postfix_expression -> [ expression ] postfix_expression_");
            // skip [
            skip();
            parseExpression();
            // skip ]
            if (!check(OP_RBRACKET)) {
                errorExpected("]");
            }
            skip();
            parsePostfixExpression_();
        } else {
            dump("postfix_expression_ -> .");
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
            return new Literal(str.position, str.lexeme, Atom.Type.INT);
        } else if (check(IDENTIFIER)) {
            dump("atom_expression -> identifier atom_expression_");
            // skip identifier
            skip();
            parseAtomExpression_();
        } else if (check(OP_LBRACE)) {
            dump("atom_expression -> { atom_expression__");
            // skip {
            skip();
            parseAtomExpression__();
        } else if (check(OP_LPARENT)) {
            dump("atom_expression -> ( expressions )");
            // skip (
            skip();
            parseExpressions();
            // skip )
            if (!check(OP_RPARENT)) {
                errorExpected(")");
            }
            skip();
        } else {
            errorExpected("logical constant, integer constant, string constant, { or (");
        }
    }

    private void parseAtomExpression_() {
        if (check(OP_LPARENT)) {
            dump("atom_expression_ -> ( expressions )");
            // skip (
            skip();
            parseExpressions();
            // skip )
            if (!check(OP_RPARENT)) {
                errorExpected(")");
            }
            skip();
        } else {
            dump("atom_expression_ -> .");
        }
    }

    private void parseAtomExpression__() {
        if (check(KW_WHILE)) {
            dump("atom_expression__ -> while expression : expression }");
            // skip while
            skip();
            parseExpression();
            // skip :
            if (!check(OP_COLON)) {
                errorExpected(":");
            }
            skip();
            parseExpression();
            // skip }
            if (!check(OP_RBRACE)) {
                errorExpected("}");
            }
            skip();
        } else if (check(KW_FOR)) {
            dump("atom_expression__ -> for identifier = expression , expression , expression : expression }");
            // skip for
            skip();
            // skip identifier
            if (!check(IDENTIFIER)) {
                errorExpected("identifier");
            }
            skip();
            // skip =
            if (!check(OP_ASSIGN)) {
                errorExpected("=");
            }
            skip();
            parseExpression();
            // skip ,
            if (!check(OP_COMMA)) {
                errorExpected(",");
            }
            skip();
            parseExpression();
            // skip ,
            if (!check(OP_COMMA)) {
                errorExpected(",");
            }
            skip();
            parseExpression();
            // skip :
            if (!check(OP_COLON)) {
                errorExpected(":");
            }
            skip();
            parseExpression();
            // skip }
            if (!check(OP_RBRACE)) {
                errorExpected("}");
            }
            skip();
        } else if (check(KW_IF)) {
            dump("atom_expression__ -> if expression then expression atom_expression___");
            // skip if
            skip();
            parseExpression();
            // skip then
            if (!check(KW_THEN)) {
                errorExpected("then");
            }
            skip();
            parseExpression();
            parseAtomExpression___();
        } else {
            dump("atom_expression__ -> expression = expression }");
            parseExpression();
            // skip =
            if (!check(OP_ASSIGN)) {
                errorExpected("=");
            }
            skip();
            parseExpression();
            // skip }
            if (!check(OP_RBRACE)) {
                errorExpected("}");
            }
            skip();
        }
    }

    private void parseAtomExpression___() {
        if (check(OP_RBRACE)) {
            dump("atom_expression___ -> }");
            // skip }
            skip();
        } else if (check(KW_ELSE)) {
            dump("atom_expression___ -> else expression }");
            // skip else
            skip();
            parseExpression();
            // skip }
            if (!check(OP_RBRACE)) {
                errorExpected("}");
            }
            skip();
        } else {
            errorExpected("} or else");
        }

    }

    private List<Expr> parseExpressions(List<Expr> expres) {
        dump("expressions -> expression expressions_");
        expres.add(parseExpression());
        expres = parseExpressions_(expres);
    }

    private List<Expr> parseExpressions_(List<Expr> exprs) {
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
