/**
 * @Author: turk
 * @Description: Sintaksni analizator.
 */

package compiler.parser;

import static compiler.lexer.TokenType.*;
import static common.RequireNonNull.requireNonNull;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

import common.Report;
import compiler.lexer.Position;
import compiler.lexer.Symbol;
import compiler.lexer.TokenType;

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

    public Parser(List<Symbol> symbols, Optional<PrintStream> productionsOutputStream) {
        requireNonNull(symbols, productionsOutputStream);
        this.symbols = symbols;
        this.productionsOutputStream = productionsOutputStream;
    }

    /**
     * Izvedi sintaksno analizo.
     */
    public void parse() {
        parseSource();
    }

    private void parseSource() {
        dump("source -> definitions");
        parseDefinitions();
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
        return symbols.get(0).tokenType == token;
    }

    private void skip() {
        symbols.remove(0);
    }

    private void errorExpected(String expected) {
        if (symbols.get(0).lexeme == "$") {
            Report.error(symbols.get(0).position, "expected " + expected + ", got EOF");
        }
        Report.error(symbols.get(0).position, "expected " + expected + ", got " + symbols.get(0).lexeme);
    }

    /**
     * Implementacija rekurzivnega spuščanja
     */

    private void parseDefinitions() {
        dump("definitions -> definition definitions_");
        parseDefinition();
        parseDefinitions_();
    }

    private void parseDefinitions_() {
        if (check(OP_SEMICOLON)) {
            dump("definitions_ -> ; definitions");
            // skip ';'
            skip();
            parseDefinitions();
        } else {
            dump("definitions_ -> .");
        }
    }

    private void parseDefinition() {
        if (check(KW_TYP)) {
            dump("definition -> type_definition");
            parseTypeDef();
        } else if (check(KW_FUN)) {
            dump("definition -> function_definition");
            parseFunctionDef();
        } else if (check(KW_VAR)) {
            dump("definition -> variable_definition");
            parseVarDef();
        } else {
            errorExpected("type, function or variable definition");
        }
    }

    private void parseTypeDef() {
        dump("type_definition -> typ identifier : type");
        // skip KW_TYP
        skip();
        // skip IDENTIFIER
        if (!check(IDENTIFIER)) {
            errorExpected("identifier");
        }
        skip();
        // skip ':'
        if (!check(OP_COLON)) {
            errorExpected(":");
        }
        skip();
        parseType();
    }

    private void parseType() {
        if (check(IDENTIFIER)) {
            dump("type -> identifier");
            // skip identifier
            skip();
        } else if (check(AT_LOGICAL)) {
            dump("type -> logical");
            // skip logical
            skip();
        } else if (check(AT_INTEGER)) {
            dump("type -> integer");
            // skip integer
            skip();
        } else if (check(AT_STRING)) {
            dump("type -> string");
            // skip string
            skip();
        } else if (check(KW_ARR)) {
            dump("type -> arr [ int_const ] type");
            // skip 'arr'
            if (!check(KW_ARR)) {
                errorExpected("arr");
            }
            skip();
            // skip '['
            if (!check(OP_LBRACKET)) {
                errorExpected("[");
            }
            skip();
            // skip 'int_const'
            if (!check(C_INTEGER)) {
                errorExpected("int constant");
            }
            skip();
            // skip ']'
            if (!check(OP_RBRACKET)) {
                errorExpected("]");
            }
            skip();
            parseType();
        } else {
            errorExpected("identifier, logical, integer, string or array");
        }
    }

    private void parseFunctionDef() {
        // TODO: throw error for wrong function definition
        dump("function_definition -> 'fun' 'identifier' '(' parameters ')' ':' type '=' expression");
        // skip fun
        skip();
        // skip identifier
        skip();
        // skip (
        skip();
        parseParameters();
        // skip )
        skip();
        // skip :
        skip();
        parseType();
        // skip =
        skip();
        parseExpression();
    }

    private void parseParameters() {
        dump("parameters -> parameter parameters_");
        parseParameter();
        parseParameters_();
    }

    private void parseParameters_() {
        if (check(OP_COMMA)) {
            dump("parameters_ -> , parameters");
            // skip ,
            skip();
            parseParameters();
        } else {
            dump("parameters_ -> .");
        }
    }

    private void parseParameter() {
        // TODO: check for wrong parameter use
        dump("parameter -> identifier : type");
        // skip identifier
        skip();
        // skip :
        skip();
        parseType();
    }

    private void parseExpression() {
        dump("expression -> logical_ior_expression expression_");
        parseLogicalIorExpression();
        parseExpression_();
    }

    private void parseExpression_() {
        // TODO: check for errors in expression
        if (check(OP_LBRACE)) {
            dump("expression_ -> { WHERE definitions }");
            // skip {
            skip();
            // skip where
            skip();
            parseDefinitions();
            // skip }
            skip();
        } else {
            dump("expression_ -> .");
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
            // TODO: check for error
            dump("postfix_expression -> [ expression ] postfix_expression");
            // skip [
            skip();
            parseExpression();
            // skip ]
            skip();
            parsePostfixExpression();
        } else {
            dump("postfix_expression_ -> .");
        }
    }

    private void parseAtomExpression() {
        if (check(C_LOGICAL)) {
            dump("atom_expression -> log_constant");
            // skip log_constant
            skip();
        } else if (check(C_INTEGER)) {
            dump("atom_expression -> int_constant");
            // skip int_constant
            skip();
        } else if (check(C_STRING)) {
            dump("atom_expression -> str_constant");
            // skip str_constant
            skip();
        } else if (check(IDENTIFIER)) {
            // skip identifier
            skip();
            if (check(OP_LBRACE)) {
                // TODO: check for wrong atom expression
                dump("atom_expression -> identifier { expression }");
                // skip {
                skip();
                parseExpression();
                // skip }
                skip();
            } else {
                dump("atom_expression -> identifier");
            }
        } else if (check(OP_LBRACE)) {
            // skip {
            skip();
            if (check(KW_IF)) {
                // skip if
                skip();
                parseExpression();
                //skip then
                skip();
                parseExpression();
                if (check(KW_ELSE)) {
                    dump("atom_expression -> { if expression then expression else expression }");
                    // skip else
                    skip();
                    parseExpression();
                    // skip }
                    skip();
                } else {
                    dump("atom_expression -> { if expression then expression }");
                    // skip }
                    skip();
                }
            } else if (check(KW_WHILE)) {
                dump("atom_expression -> { while expression : expression }");
                // skip while
                skip();
                parseExpression();
                // skip :
                skip();
                parseExpression();
                // skip }
                skip();
            } else if (check(KW_FOR)) {
                dump("atom_expression -> { for identifier = expression , expression , expression : expression }");
                // skip for
                skip();
                // skip identifier
                skip();
                // skip =
                skip();
                parseExpression();
                // skip ,
                skip();
                parseExpression();
                // skip ,
                skip();
                parseExpression();
                // skip :
                skip();
                parseExpression();
                // skip }
                skip();

            } else {
                dump("");
            }
        } else if (check(OP_LPARENT)) {
            // TODO: check for errors
            dump("atom_expression -> ( expressions )");
            // skip (
            skip();
            parseExpressions();
            //skip )
            skip();
        }
    }

    private void parseExpressions() {
        dump("expressions -> expression expressions_");
        parseExpression();
        parseExpressions_();
    }

    private void parseExpressions_() {
        if (check(OP_COMMA)) {
            dump("expressions_ -> , expressions");
            // skip ,
            skip();
            parseExpressions();
        } else {
            dump("expressions_ -> .");
        }
    }

    private void parseVarDef() {
        // TODO: check for errors in var def
        // skip var
        skip();
        // skip identifier
        skip();
        // skip :
        skip();
        parseType();
    }
}
